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
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.cse.import_runner.app.dichotomy.DichotomyRaoResponse;
import com.farao_community.farao.cse.import_runner.app.dichotomy.MultipleDichotomyResult;
import com.farao_community.farao.cse.import_runner.app.dichotomy.MultipleDichotomyRunner;
import com.farao_community.farao.cse.import_runner.app.dichotomy.NetworkShifterUtil;
import com.farao_community.farao.cse.import_runner.app.util.FileUtil;
import com.farao_community.farao.cse.network_processing.busbar_change.BusBarChangePostProcessor;
import com.farao_community.farao.cse.network_processing.busbar_change.BusBarChangePreProcessor;
import com.farao_community.farao.cse.network_processing.ucte_pst_change.PstInitializer;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import com.farao_community.farao.data.crac_api.Crac;
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

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
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
    private final Logger businessLogger;

    public CseRunner(FileImporter fileImporter, FileExporter fileExporter, MultipleDichotomyRunner multipleDichotomyRunner,
                     TtcResultService ttcResultService, PiSaService piSaService, MerchantLineService merchantLineService,
                     ProcessConfiguration processConfiguration, Logger businessLogger) {
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.multipleDichotomyRunner = multipleDichotomyRunner;
        this.ttcResultService = ttcResultService;
        this.piSaService = piSaService;
        this.merchantLineService = merchantLineService;
        this.processConfiguration = processConfiguration;
        this.businessLogger = businessLogger;
    }

    public CseResponse run(CseRequest cseRequest) throws IOException {
        final boolean importEcProcess = cseRequest.isImportEcProcess();
        CseData cseData = new CseData(cseRequest, fileImporter);
        // CRAC import and network pre-processing
        Network network = fileImporter.importNetwork(cseRequest.getCgmUrl());
        merchantLineService.activateMerchantLine(cseRequest.getProcessType(), network, cseData);
        piSaService.alignGenerators(network);

        CracImportData cracImportData = importCracAndModifyNetworkForBusBars(
            cseRequest.getMergedCracUrl(), cseRequest.getTargetProcessDateTime(), network);

        Crac crac = cracImportData.cseCracCreationContext.getCrac();

        Map<String, Double> preprocessedPisaLinks = piSaService.forceSetPoint(cseRequest.getProcessType(), network, crac);
        // Put all PSTs within their ranges to be able to optimize them
        Map<String, Integer> preprocessedPsts = PstInitializer.withLogger(businessLogger).initializePsts(network, crac);

        // Saving pre-processed network in IIDM and CRAC in JSON format
        cseData.setPreProcesedNetworkUrl(fileExporter.saveNetworkInArtifact(network, cseRequest.getTargetProcessDateTime(), "", cseRequest.getProcessType(), importEcProcess));
        cseData.setJsonCracUrl(fileExporter.saveCracInJsonFormat(crac, cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType(), importEcProcess));

        double initialIndexValueForProcess;
        try {
            // We compute real Italian import on network just to compare with Vulcanus reference and log an event
            // if the difference is too high (> 5%)
            double initialItalianImportOnNetwork = BorderExchanges.computeItalianImport(network);
            // Then we set initial index from Vulcanus reference value
            initialIndexValueForProcess = getInitialIndexValue(cseData);
            checkNetworkAndReferenceExchangesDifference(initialIndexValueForProcess, initialItalianImportOnNetwork);
        } catch (CseComputationException e) {
            LOGGER.warn("A problem occurred while calculating initial italian import from reference exchanges. First shift operation is not performed yet, base case cgm will be added to outputs. Nested message: {}", e.getMessage());
            String ttcResultUrl = ttcResultService.saveFailedTtcResult(
                cseRequest,
                FileUtil.getFilenameFromUrl(cseRequest.getCgmUrl()),
                TtcResult.FailedProcessData.FailedProcessReason.LOAD_FLOW_FAILURE);
            return new CseResponse(cseRequest.getId(), ttcResultUrl, cseRequest.getCgmUrl(), false);
        }

        double initialIndexValue = Optional.ofNullable(cseRequest.getInitialDichotomyIndex()).orElse(initialIndexValueForProcess);

        MultipleDichotomyResult<DichotomyRaoResponse> multipleDichotomyResult = multipleDichotomyRunner.runMultipleDichotomy(
            cseRequest,
            cseData,
            network,
            crac,
            initialIndexValue,
            NetworkShifterUtil.getReferenceExchanges(cseData));

        if (multipleDichotomyResult.isInterrupted()) {
            String ttcResultUrl = ttcResultService.saveFailedTtcResult(
                cseRequest,
                FileUtil.getFilenameFromUrl(cseRequest.getCgmUrl()),
                TtcResult.FailedProcessData.FailedProcessReason.OTHER);
            return new CseResponse(cseRequest.getId(), ttcResultUrl, cseRequest.getCgmUrl(), true);
        }

        DichotomyResult<DichotomyRaoResponse> dichotomyResult = multipleDichotomyResult.getBestDichotomyResult();
        String firstShiftNetworkName = fileExporter.getBaseCaseName(cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());
        String ttcResultUrl;
        String finalCgmUrl;
        if (dichotomyResult.hasValidStep()) {
            String finalCgmPath = fileExporter.getFinalNetworkFilePath(cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType(), importEcProcess);
            Network finalNetwork = fileImporter.importNetwork(dichotomyResult.getHighestValidStep().getValidationData()
                .getRaoResponse().getNetworkWithPraFileUrl());
            BusBarChangePostProcessor.process(finalNetwork, cracImportData.busBarChangeSwitchesSet);

            finalCgmUrl = fileExporter.exportAndUploadNetwork(finalNetwork, "UCTE", GridcapaFileGroup.OUTPUT, finalCgmPath, processConfiguration.getFinalCgm(), cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType(), importEcProcess);
            ttcResultUrl = ttcResultService.saveTtcResult(cseRequest, cseData, cracImportData.cseCracCreationContext,
                dichotomyResult.getHighestValidStep().getValidationData(), dichotomyResult.getLimitingCause(),
                firstShiftNetworkName, FileUtil.getFilenameFromUrl(finalCgmUrl), preprocessedPsts, preprocessedPisaLinks);
        } else {
            ttcResultUrl = ttcResultService.saveFailedTtcResult(
                cseRequest,
                firstShiftNetworkName,
                TtcResult.FailedProcessData.FailedProcessReason.NO_SECURE_TTC);
            finalCgmUrl = firstShiftNetworkName;
        }

        return new CseResponse(cseRequest.getId(), ttcResultUrl, finalCgmUrl, multipleDichotomyResult.isInterrupted());
    }

    double getInitialIndexValue(CseData cseData) {
        return cseData.getCseReferenceExchanges().getExchanges().values().stream().reduce(0., Double::sum);
    }

    CracImportData importCracAndModifyNetworkForBusBars(String cracUrl, OffsetDateTime targetProcessDateTime, Network network) {
        // Create CRAC creation context
        CseCrac nativeCseCrac = fileImporter.importCseCrac(cracUrl);
        // Pre-treatment on network
        Set<BusBarChangeSwitches> busBarChangeSwitchesSet = BusBarChangePreProcessor.process(network, nativeCseCrac);
        CracCreationParameters cracCreationParameters = getCracCreationParameters(busBarChangeSwitchesSet);
        CseCracCreationContext cracCreationContext = (CseCracCreationContext) CracCreators.createCrac(
            nativeCseCrac, network, targetProcessDateTime, cracCreationParameters);
        return new CracImportData(cracCreationContext, busBarChangeSwitchesSet);
    }

    private CracCreationParameters getCracCreationParameters(Set<BusBarChangeSwitches> busBarChangeSwitches) {
        CseCracCreationParameters cseCracCreationParameters = new CseCracCreationParameters();
        cseCracCreationParameters.setBusBarChangeSwitchesSet(busBarChangeSwitches);
        CracCreationParameters cracCreationParameters = CracCreationParameters.load();
        cracCreationParameters.addExtension(CseCracCreationParameters.class, cseCracCreationParameters);
        return cracCreationParameters;
    }

    private void checkNetworkAndReferenceExchangesDifference(double initialItalianImportFromReference, double initialItalianImportFromNetwork) {
        double relativeDifference = Math.abs(initialItalianImportFromReference - initialItalianImportFromNetwork)
            / Math.abs(initialItalianImportFromReference);
        if (relativeDifference > NETWORK_AND_REFERENCE_EXCHANGES_DIFFERENCE_THRESHOLD) {
            LOGGER.warn("Difference between vulcanus exchanges and network exchanges too high.");
        }
    }

    static final class CracImportData {
        final CseCracCreationContext cseCracCreationContext;
        final Set<BusBarChangeSwitches> busBarChangeSwitchesSet;

        private CracImportData(CseCracCreationContext cseCracCreationContext, Set<BusBarChangeSwitches> busBarChangeSwitchesSet) {
            this.cseCracCreationContext = cseCracCreationContext;
            this.busBarChangeSwitchesSet = busBarChangeSwitchesSet;
        }
    }
}
