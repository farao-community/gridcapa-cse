/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.export_runner.app.FileUtil;
import com.farao_community.farao.cse.export_runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.cse.export_runner.app.configurations.UrlConfiguration;
import com.farao_community.farao.cse.network_processing.CracCreationParametersService;
import com.farao_community.farao.cse.network_processing.busbar_change.BusBarChangePostProcessor;
import com.farao_community.farao.cse.network_processing.busbar_change.BusBarChangePreProcessor;
import com.farao_community.farao.cse.network_processing.ucte_pst_change.PstInitializer;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseExportRequest;
import com.farao_community.farao.cse.runner.api.resource.CseExportResponse;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.dichotomy.api.exceptions.RaoFailureException;
import com.farao_community.farao.dichotomy.api.exceptions.RaoInterruptionException;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.cse.CseCracCreationContext;
import com.powsybl.openrao.data.crac.io.cse.parameters.BusBarChangeSwitches;
import com.powsybl.openrao.data.crac.io.cse.xsd.CRACDocumentType;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.dichotomy.api.logging.DichotomyLoggerProvider.BUSINESS_WARNS;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class CseExportRunner {
    private static final String NETWORK_PRE_PROCESSED_FILE_NAME = "network_pre_processed";
    private static final DateTimeFormatter OUTPUTS_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final Logger LOGGER = LoggerFactory.getLogger(CseExportRunner.class);
    private static final String CRAC_CREATION_PARAMETERS_JSON = "/crac/cseCracCreationParameters.json";
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final PiSaService pisaService;
    private final RaoRunnerService raoRunnerService;
    private final TtcRaoService ttcRaoService;
    private final Logger businessLogger;
    private final ProcessConfiguration processConfiguration;
    private final MerchantLineService merchantLineService;
    private final RestTemplateBuilder restTemplateBuilder;
    private final UrlConfiguration urlConfiguration;

    public CseExportRunner(FileImporter fileImporter, FileExporter fileExporter, PiSaService pisaService, RaoRunnerService raoRunnerService, TtcRaoService ttcRaoService, Logger businessLogger, ProcessConfiguration processConfiguration, MerchantLineService merchantLineService, RestTemplateBuilder restTemplateBuilder, UrlConfiguration urlConfiguration) {
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.pisaService = pisaService;
        this.raoRunnerService = raoRunnerService;
        this.ttcRaoService = ttcRaoService;
        this.businessLogger = businessLogger;
        this.processConfiguration = processConfiguration;
        this.merchantLineService = merchantLineService;
        this.restTemplateBuilder = restTemplateBuilder;
        this.urlConfiguration = urlConfiguration;
    }

    public CseExportResponse run(CseExportRequest cseExportRequest) throws IOException {
        if (checkIsInterrupted(cseExportRequest)) {
            businessLogger.warn("Computation has been interrupted for timestamp {}", cseExportRequest.getTargetProcessDateTime());
            LOGGER.info("Response sent for timestamp {} : run has been interrupted", cseExportRequest.getTargetProcessDateTime());
            return new CseExportResponse(cseExportRequest.getId(), ttcRaoService.saveFailedTtcRao(cseExportRequest), "", true);
        }

        // Check on cgm file name
        FileUtil.checkCgmFileName(cseExportRequest.getCgmUrl(), cseExportRequest.getProcessType());

        // Import and pre-treatment on Network
        Network network = fileImporter.importNetwork(cseExportRequest.getCgmUrl());
        merchantLineService.setTransformerInActivePowerRegulation(network);
        pisaService.alignGenerators(network);

        // Create CRAC creation context
        CRACDocumentType nativeCseCrac = fileImporter.importCseCrac(cseExportRequest.getMergedCracUrl());
        Set<BusBarChangeSwitches> busBarChangeSwitchesSet = BusBarChangePreProcessor.process(network, nativeCseCrac); // Pre-treatment on network
        LOGGER.info("Importing Crac Creation Parameters file: {}", CRAC_CREATION_PARAMETERS_JSON);
        InputStream cracCreationParametersInputStream = getClass().getResourceAsStream(CRAC_CREATION_PARAMETERS_JSON);
        CracCreationParameters cracCreationParameters = CracCreationParametersService.getCracCreationParameters(cracCreationParametersInputStream, busBarChangeSwitchesSet);

        CseCracCreationContext cseCracCreationContext = (CseCracCreationContext) Crac.readWithContext(FileUtil.getFilenameFromUrl(cseExportRequest.getMergedCracUrl()), fileImporter.openUrlStream(cseExportRequest.getMergedCracUrl()), network, cseExportRequest.getTargetProcessDateTime(), cracCreationParameters);

        // Put all PSTs within their ranges to be able to optimize them
        Map<String, Integer> preprocessedPsts = PstInitializer.withLogger(businessLogger).initializePsts(network, cseCracCreationContext.getCrac());

        String initialNetworkUrl = saveInitialNetwork(cseExportRequest, network);
        String cracInJsonFormatUrl = fileExporter.saveCracInJsonFormat(cseCracCreationContext.getCrac(),
                cseExportRequest.getProcessType(), cseExportRequest.getTargetProcessDateTime());
        String raoParametersUrl = fileExporter.saveRaoParameters(cseExportRequest.getProcessType(),
                cseExportRequest.getTargetProcessDateTime());
        String artefactDestinationPath = fileExporter.getDestinationPath(cseExportRequest.getTargetProcessDateTime(), cseExportRequest.getProcessType(), GridcapaFileGroup.ARTIFACT);
        try {
            RaoSuccessResponse raoResponse = raoRunnerService.run(cseExportRequest.getId(), cseExportRequest.getCurrentRunId(), initialNetworkUrl, cracInJsonFormatUrl, raoParametersUrl, artefactDestinationPath);

            Network networkWithPra = fileImporter.importNetwork(raoResponse.getNetworkWithPraFileUrl());
            BusBarChangePostProcessor.process(networkWithPra, busBarChangeSwitchesSet);
            runLoadFlow(networkWithPra);
            // Save again on MinIO to proper process location and naming
            String networkWithPraUrl = saveNetworkWithPra(cseExportRequest, networkWithPra);
            RaoResult raoResult = fileImporter.importRaoResult(raoResponse.getRaoResultFileUrl(), cseCracCreationContext.getCrac());
            Network networkForTtc = fileImporter.importNetwork(raoResponse.getNetworkWithPraFileUrl());
            String ttcResultUrl = ttcRaoService.saveTtcRao(cseExportRequest, cseCracCreationContext, raoResult, networkForTtc, preprocessedPsts);
            return new CseExportResponse(cseExportRequest.getId(), ttcResultUrl, networkWithPraUrl, false);
        } catch (RaoInterruptionException e) {
            BUSINESS_WARNS.warn("RAO interrupted");
            return new CseExportResponse(cseExportRequest.getId(), ttcRaoService.saveFailedTtcRao(cseExportRequest), "", true);
        } catch (CseInternalException | RaoFailureException e) {
            ttcRaoService.saveFailedTtcRao(cseExportRequest);
            throw new CseInternalException("RAO run failed", e);
        }
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

    private void runLoadFlow(Network network) {
        LoadFlowResult result = LoadFlow.run(network, LoadFlowParameters.load());
        if (result.isFailed()) {
            LOGGER.error("Loadflow computation diverged on network '{}'", network.getId());
            throw new CseInternalException(String.format("Loadflow computation diverged on network %s", network.getId()));
        }
    }

    private boolean checkIsInterrupted(CseExportRequest cseExportRequest) {
        ResponseEntity<Boolean> responseEntity = restTemplateBuilder.build().getForEntity(getInterruptedUrl(cseExportRequest.getCurrentRunId()), Boolean.class);
        return responseEntity.getBody() != null && responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody();
    }

    private String getInterruptedUrl(String runId) {
        return urlConfiguration.getInterruptServerUrl() + runId;
    }
}
