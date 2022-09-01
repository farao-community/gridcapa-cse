/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.computation.CseComputationException;
import com.farao_community.farao.cse.data.ttc_res.TtcResult;
import com.farao_community.farao.cse.import_runner.app.dichotomy.DichotomyRaoResponse;
import com.farao_community.farao.cse.import_runner.app.dichotomy.MultipleDichotomyResult;
import com.farao_community.farao.cse.import_runner.app.dichotomy.MultipleDichotomyRunner;
import com.farao_community.farao.cse.import_runner.app.util.Threadable;
import com.farao_community.farao.cse.network_processing.busbar_change.BusBarChangeProcessor;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.BusBarChangeSwitches;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.CseCracCreationParameters;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class CseRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CseRunner.class);
    private static final double NETWORK_AND_REFERENCE_EXCHANGES_DIFFERENCE_THRESHOLD = 0.05;

    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final MultipleDichotomyRunner multipleDichotomyRunner;
    private final TtcResultService ttcResultService;
    private final PiSaService piSaService;
    private final MerchantLineService merchantLineService;
    private final ProcessConfiguration processConfiguration;

    public CseRunner(FileImporter fileImporter, FileExporter fileExporter, MultipleDichotomyRunner multipleDichotomyRunner, TtcResultService ttcResultService, PiSaService piSaService, MerchantLineService merchantLineService, ProcessConfiguration processConfiguration) {
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.multipleDichotomyRunner = multipleDichotomyRunner;
        this.ttcResultService = ttcResultService;
        this.piSaService = piSaService;
        this.merchantLineService = merchantLineService;
        this.processConfiguration = processConfiguration;

    }

    @Threadable
    public CseResponse run(CseRequest cseRequest) throws IOException {
        CseData cseData = new CseData(cseRequest, fileImporter);
        // CRAC import and network pre-processing
        Network network = fileImporter.importNetwork(cseRequest.getCgmUrl());
        merchantLineService.activateMerchantLine(cseRequest.getProcessType(), network, cseData);
        piSaService.alignGenerators(network);

        CseCracCreationContext cseCracCreationContext = importCracAndModifyNetworkForBusBars(
            cseRequest.getMergedCracUrl(), cseRequest.getTargetProcessDateTime(), network);

        piSaService.forceSetPoint(cseRequest.getProcessType(), network, cseCracCreationContext.getCrac());

        // Saving pre-processed network in IIDM and CRAC in JSON format
        cseData.setPreProcesedNetworkUrl(fileExporter.saveNetworkInArtifact(network, cseRequest.getTargetProcessDateTime(), "", cseRequest.getProcessType()));
        cseData.setJsonCracUrl(fileExporter.saveCracInJsonFormat(cseCracCreationContext.getCrac(), cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType()));
        String baseCaseFilePath = fileExporter.getBaseCaseFilePath(cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());
        String baseCaseFileUrl = fileExporter.exportAndUploadNetwork(network, "UCTE", GridcapaFileGroup.OUTPUT, baseCaseFilePath, processConfiguration.getInitialCgm(), cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());

        double initialItalianImport;
        if (cseRequest.getProcessType() == ProcessType.IDCC) {
            try {
                initialItalianImport = BorderExchanges.computeItalianImport(network);
            } catch (CseComputationException e) {
                String ttcResultUrl = ttcResultService.saveFailedTtcResult(
                    cseRequest,
                    cseRequest.getCgmUrl(),
                    TtcResult.FailedProcessData.FailedProcessReason.LOAD_FLOW_FAILURE);
                return new CseResponse(cseRequest.getId(), ttcResultUrl, cseRequest.getCgmUrl());
            }
            checkNetworkAndReferenceExchangesDifference(cseData, initialItalianImport);
        } else if (cseRequest.getProcessType() == ProcessType.D2CC) {
            initialItalianImport = getInitialItalianImportForD2ccProcess(cseData);
        } else {
            throw new CseInternalException(String.format("Process type %s is not handled", cseRequest.getProcessType()));
        }

        MultipleDichotomyResult<DichotomyRaoResponse> multipleDichotomyResult = multipleDichotomyRunner.runMultipleDichotomy(
            cseRequest,
            cseData,
            network,
            cseCracCreationContext.getCrac(),
            initialItalianImport);

        DichotomyResult<DichotomyRaoResponse> dichotomyResult = multipleDichotomyResult.getBestDichotomyResult();

        String ttcResultUrl;
        String finalCgmUrl;
        if (dichotomyResult.hasValidStep()) {
            String finalCgmPath = fileExporter.getFinalNetworkFilePath(cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());
            Network finalNetwork = fileImporter.importNetwork(dichotomyResult.getHighestValidStep().getValidationData()
                .getRaoResponse().getNetworkWithPraFileUrl());
            finalCgmUrl = fileExporter.exportAndUploadNetwork(finalNetwork, "UCTE", GridcapaFileGroup.OUTPUT, finalCgmPath, processConfiguration.getFinalCgm(), cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());
            ttcResultUrl = ttcResultService.saveTtcResult(cseRequest, cseData, cseCracCreationContext,
                dichotomyResult.getHighestValidStep().getValidationData(), dichotomyResult.getLimitingCause(),
                baseCaseFileUrl, finalCgmUrl);
        } else {
            ttcResultUrl = ttcResultService.saveFailedTtcResult(
                cseRequest,
                baseCaseFileUrl,
                TtcResult.FailedProcessData.FailedProcessReason.NO_SECURE_TTC);
            finalCgmUrl = baseCaseFileUrl;
        }

        return new CseResponse(cseRequest.getId(), ttcResultUrl, finalCgmUrl);
    }

    double getInitialItalianImportForD2ccProcess(CseData cseData) {
        return cseData.getNtcPerCountry().values().stream().reduce(0., Double::sum) + processConfiguration.getTrm();
    }

    CseCracCreationContext importCracAndModifyNetworkForBusBars(String cracUrl, OffsetDateTime targetProcessDateTime, Network network) throws IOException {
        // Create CRAC creation context
        CseCrac nativeCseCrac = fileImporter.importCseCrac(cracUrl);
        // Pre-treatment on network
        Set<BusBarChangeSwitches> busBarChangeSwitchesSet = BusBarChangeProcessor.process(network, nativeCseCrac);
        CracCreationParameters cracCreationParameters = getCracCreationParameters(busBarChangeSwitchesSet);
        return (CseCracCreationContext) CracCreators.createCrac(nativeCseCrac, network, targetProcessDateTime, cracCreationParameters);
    }

    private CracCreationParameters getCracCreationParameters(Set<BusBarChangeSwitches> busBarChangeSwitches) {
        CseCracCreationParameters cseCracCreationParameters = new CseCracCreationParameters();
        cseCracCreationParameters.setBusBarChangeSwitchesSet(busBarChangeSwitches);
        CracCreationParameters cracCreationParameters = CracCreationParameters.load();
        cracCreationParameters.addExtension(CseCracCreationParameters.class, cseCracCreationParameters);
        return cracCreationParameters;
    }

    private void checkNetworkAndReferenceExchangesDifference(CseData cseData, double initialItalianImportFromNetwork) {
        double referenceItalianImport = cseData.getCseReferenceExchanges().getExchanges().values().stream().reduce(0., Double::sum);
        if (Math.abs(referenceItalianImport - initialItalianImportFromNetwork) / Math.abs(referenceItalianImport) > NETWORK_AND_REFERENCE_EXCHANGES_DIFFERENCE_THRESHOLD) {
            LOGGER.warn("Difference between vulcanus exchanges and network exchanges too high.");
        }
    }
}
