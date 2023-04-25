/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.export_runner.app.FileUtil;
import com.farao_community.farao.cse.export_runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.cse.network_processing.busbar_change.BusBarChangePostProcessor;
import com.farao_community.farao.cse.network_processing.busbar_change.BusBarChangePreProcessor;
import com.farao_community.farao.cse.network_processing.ucte_pst_change.PstInitializer;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseExportRequest;
import com.farao_community.farao.cse.runner.api.resource.CseExportResponse;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.BusBarChangeSwitches;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.CseCracCreationParameters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class CseExportRunner {
    private static final String NETWORK_PRE_PROCESSED_FILE_NAME = "network_pre_processed";
    private static final DateTimeFormatter OUTPUTS_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final PiSaService pisaService;
    private final RaoRunnerService raoRunnerService;
    private final TtcRaoService ttcRaoService;
    private final Logger businessLogger;
    private final ProcessConfiguration processConfiguration;

    public CseExportRunner(FileImporter fileImporter, FileExporter fileExporter, PiSaService pisaService, RaoRunnerService raoRunnerService, TtcRaoService ttcRaoService, Logger businessLogger, ProcessConfiguration processConfiguration) {
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.pisaService = pisaService;
        this.raoRunnerService = raoRunnerService;
        this.ttcRaoService = ttcRaoService;
        this.businessLogger = businessLogger;
        this.processConfiguration = processConfiguration;
    }

    public CseExportResponse run(CseExportRequest cseExportRequest) {
        String logsFileUrl = ""; //TODO

        // Check on cgm file name
        FileUtil.checkCgmFileName(cseExportRequest.getCgmUrl(), cseExportRequest.getProcessType());

        // Import and pre-treatment on Network
        Network network = fileImporter.importNetwork(cseExportRequest.getCgmUrl());
        pisaService.alignGenerators(network);

        // Create CRAC creation context
        CseCrac nativeCseCrac = fileImporter.importCseCrac(cseExportRequest.getMergedCracUrl());
        Set<BusBarChangeSwitches> busBarChangeSwitchesSet = BusBarChangePreProcessor.process(network, nativeCseCrac); // Pre-treatment on network
        CracCreationParameters cracCreationParameters = getCracCreationParameters(busBarChangeSwitchesSet);
        CseCracCreationContext cseCracCreationContext = (CseCracCreationContext) CracCreators.createCrac(nativeCseCrac,
            network, cseExportRequest.getTargetProcessDateTime(), cracCreationParameters);

        // Put all PSTs within their ranges to be able to optimize them
        Map<String, Integer> preprocessedPsts = PstInitializer.withLogger(businessLogger).initializePsts(network, cseCracCreationContext.getCrac());

        String initialNetworkUrl = saveInitialNetwork(cseExportRequest, network);
        String cracInJsonFormatUrl = fileExporter.saveCracInJsonFormat(cseCracCreationContext.getCrac(),
            cseExportRequest.getProcessType(), cseExportRequest.getTargetProcessDateTime());
        String raoParametersUrl = fileExporter.saveRaoParameters(cseExportRequest.getProcessType(),
            cseExportRequest.getTargetProcessDateTime());

        try {
            RaoResponse raoResponse = raoRunnerService.run(cseExportRequest.getId(), initialNetworkUrl, cracInJsonFormatUrl, raoParametersUrl);

            Network networkWithPra = fileImporter.importNetwork(raoResponse.getNetworkWithPraFileUrl());
            BusBarChangePostProcessor.process(networkWithPra, busBarChangeSwitchesSet);
            // Save again on MinIO to proper process location and naming
            String networkWithPraUrl = saveNetworkWithPra(cseExportRequest, networkWithPra);
            RaoResult raoResult = fileImporter.importRaoResult(raoResponse.getRaoResultFileUrl(), cseCracCreationContext.getCrac());
            String ttcResultUrl = ttcRaoService.saveTtcRao(cseExportRequest, cseCracCreationContext, raoResult, preprocessedPsts);
            return new CseExportResponse(cseExportRequest.getId(), ttcResultUrl, networkWithPraUrl, logsFileUrl);
        } catch (CseInternalException e) {
            // Temporary return of an empty string for ttc logs file and cgm file
            return new CseExportResponse(cseExportRequest.getId(), ttcRaoService.saveFailedTtcRao(cseExportRequest), "", logsFileUrl);
        }
    }

    private CracCreationParameters getCracCreationParameters(Set<BusBarChangeSwitches> busBarChangeSwitches) {
        CseCracCreationParameters cseCracCreationParameters = new CseCracCreationParameters();
        cseCracCreationParameters.setBusBarChangeSwitchesSet(busBarChangeSwitches);
        CracCreationParameters cracCreationParameters = CracCreationParameters.load();
        cracCreationParameters.addExtension(CseCracCreationParameters.class, cseCracCreationParameters);
        return cracCreationParameters;
    }

    private String saveInitialNetwork(CseExportRequest cseExportRequest, Network initialNetwork) {
        return fileExporter.saveNetwork(initialNetwork, "XIIDM", GridcapaFileGroup.ARTIFACT,
            cseExportRequest.getProcessType(), NETWORK_PRE_PROCESSED_FILE_NAME, cseExportRequest.getTargetProcessDateTime());
    }

    private String saveNetworkWithPra(CseExportRequest cseExportRequest, Network networkWithPra) {
        return fileExporter.saveNetwork(networkWithPra, "UCTE", GridcapaFileGroup.OUTPUT,
            cseExportRequest.getProcessType(), getFinalNetworkFilenameWithoutExtension(cseExportRequest.getTargetProcessDateTime(), FileUtil.getFilenameFromUrl(cseExportRequest.getCgmUrl()), cseExportRequest.getProcessType()), cseExportRequest.getTargetProcessDateTime());
    }

    String getFinalNetworkFilenameWithoutExtension(OffsetDateTime processTargetDate, String initialCgmFilename, ProcessType processType) {
        ZonedDateTime targetDateInEuropeZone = processTargetDate.atZoneSameInstant(ZoneId.of(processConfiguration.getZoneId()));
        int dayOfWeek = targetDateInEuropeZone.getDayOfWeek().getValue();
        String dateAndTime = targetDateInEuropeZone.format(OUTPUTS_DATE_TIME_FORMATTER);
        return dateAndTime + "_2D" + dayOfWeek + "_ce_Transit_RAO_CSE" + FileUtil.getFileVersion(initialCgmFilename, processType);
    }


}
