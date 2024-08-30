/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.data.ttc_res.TtcResult;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.cse.import_runner.app.configurations.UrlConfiguration;
import com.farao_community.farao.cse.import_runner.app.dichotomy.DichotomyRaoResponse;
import com.farao_community.farao.cse.import_runner.app.dichotomy.MultipleDichotomyResult;
import com.farao_community.farao.cse.import_runner.app.dichotomy.MultipleDichotomyRunner;
import com.farao_community.farao.cse.import_runner.app.dichotomy.NetworkShifterUtil;
import com.farao_community.farao.cse.import_runner.app.util.FileUtil;
import com.farao_community.farao.cse.network_processing.CracCreationParametersService;
import com.farao_community.farao.cse.network_processing.busbar_change.BusBarChangePostProcessor;
import com.farao_community.farao.cse.network_processing.busbar_change.BusBarChangePreProcessor;
import com.farao_community.farao.cse.network_processing.ucte_pst_change.PstInitializer;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.powsybl.openrao.data.cracapi.Crac;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cse.CseCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.BusBarChangeSwitches;
import com.powsybl.openrao.data.craccreation.creator.cse.xsd.CRACDocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
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
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final MultipleDichotomyRunner multipleDichotomyRunner;
    private final TtcResultService ttcResultService;
    private final PiSaService piSaService;
    private final MerchantLineService merchantLineService;
    private final ProcessConfiguration processConfiguration;
    private final Logger businessLogger;
    private final InitialShiftService initialShiftService;
    private final RestTemplateBuilder restTemplateBuilder;
    private final UrlConfiguration urlConfiguration;
    private static final String CRAC_CREATION_PARAMETERS_JSON = "/crac/cseCracCreationParameters.json";

    public CseRunner(FileImporter fileImporter, FileExporter fileExporter, MultipleDichotomyRunner multipleDichotomyRunner,
                     TtcResultService ttcResultService, PiSaService piSaService, MerchantLineService merchantLineService,
                     ProcessConfiguration processConfiguration, Logger businessLogger, InitialShiftService initialShiftService,
                     RestTemplateBuilder restTemplateBuilder, UrlConfiguration urlConfiguration) {
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.multipleDichotomyRunner = multipleDichotomyRunner;
        this.ttcResultService = ttcResultService;
        this.piSaService = piSaService;
        this.merchantLineService = merchantLineService;
        this.processConfiguration = processConfiguration;
        this.businessLogger = businessLogger;
        this.initialShiftService = initialShiftService;
        this.restTemplateBuilder = restTemplateBuilder;
        this.urlConfiguration = urlConfiguration;
    }

    public CseResponse run(CseRequest cseRequest) throws IOException {
        String firstShiftNetworkName = fileExporter.getFirstShiftNetworkName(cseRequest.getTargetProcessDateTime(), FileUtil.getFilenameFromUrl(cseRequest.getCgmUrl()), cseRequest.getProcessType());

        if (checkIsInterrupted(cseRequest)) {
            businessLogger.warn("Computation has been interrupted for timestamp {}", cseRequest.getTargetProcessDateTime());
            LOGGER.info("Response sent for timestamp {} : run has been interrupted", cseRequest.getTargetProcessDateTime());
            String interruptedTtc = ttcResultService.saveFailedTtcResult(
                    cseRequest,
                    firstShiftNetworkName,
                    TtcResult.FailedProcessData.FailedProcessReason.OTHER);
            String interruptedCgm = firstShiftNetworkName;

            return new CseResponse(cseRequest.getId(), interruptedTtc, interruptedCgm, true);
        }

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

        Map<String, Double> ntcsByEic = cseRequest.getProcessType().equals(ProcessType.IDCC) ?
            cseData.getNtc2().getExchanges() :
            NetworkShifterUtil.convertMapByCountryToMapByEic(cseData.getNtcPerCountry());

        double initialIndexValue = Optional.ofNullable(cseRequest.getInitialDichotomyIndex()).orElse(ntcsByEic.values().stream().mapToDouble(Double::doubleValue).sum());
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        // input cgm corresponds to vulcanus file but we want to start calculation from ntc values
        initialShiftService.performInitialShiftFromVulcanusLevelToNtcLevel(network, cseData, cseRequest, cseData.getCseReferenceExchanges().getExchanges(), ntcsByEic);
        if (cseRequest.getProcessType().equals(ProcessType.IDCC)) {
            network.getVariantManager().setWorkingVariant(initialVariantId);
        }

        MultipleDichotomyResult<DichotomyRaoResponse> multipleDichotomyResult = multipleDichotomyRunner.runMultipleDichotomy(
            cseRequest,
            cseData,
            network,
            crac,
            initialIndexValue,
            NetworkShifterUtil.getReferenceExchanges(cseData),
            ntcsByEic);

        String ttcResultUrl;
        String finalCgmUrl;
        DichotomyResult<DichotomyRaoResponse> dichotomyResult;
        try {
            dichotomyResult = multipleDichotomyResult.getBestDichotomyResult();
        } catch (IndexOutOfBoundsException ioobe) {
            dichotomyResult = null;
        }

        if (dichotomyResult != null && dichotomyResult.hasValidStep() && dichotomyResult.getHighestValidStep().getValidationData() != null) {
            String finalCgmPath = fileExporter.getFinalNetworkFilePath(cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType(), FileUtil.getFilenameFromUrl(cseRequest.getCgmUrl()), importEcProcess);
            Network finalNetwork = fileImporter.importNetwork(dichotomyResult.getHighestValidStep().getValidationData()
                .getRaoResponse().getNetworkWithPraFileUrl());
            BusBarChangePostProcessor.process(finalNetwork, cracImportData.busBarChangeSwitchesSet);

            finalCgmUrl = fileExporter.exportAndUploadNetwork(finalNetwork, "UCTE", GridcapaFileGroup.OUTPUT, finalCgmPath, processConfiguration.getFinalCgm(), cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType(), importEcProcess);
            ttcResultUrl = ttcResultService.saveTtcResult(cseRequest, cseData, cracImportData.cseCracCreationContext,
                dichotomyResult.getHighestValidStep().getValidationData(), dichotomyResult.getLimitingCause(),
                firstShiftNetworkName, FileUtil.getFilenameFromUrl(finalCgmUrl), preprocessedPsts, preprocessedPisaLinks);
        } else {
            TtcResult.FailedProcessData.FailedProcessReason  failedProcessReason = multipleDichotomyResult.isInterrupted() ? TtcResult.FailedProcessData.FailedProcessReason.OTHER : TtcResult.FailedProcessData.FailedProcessReason.NO_SECURE_TTC;
            ttcResultUrl = ttcResultService.saveFailedTtcResult(
                cseRequest,
                firstShiftNetworkName,
                failedProcessReason);
            finalCgmUrl = firstShiftNetworkName;
        }

        return new CseResponse(cseRequest.getId(), ttcResultUrl, finalCgmUrl, multipleDichotomyResult.isInterrupted());
    }

    CracImportData importCracAndModifyNetworkForBusBars(String cracUrl, OffsetDateTime targetProcessDateTime, Network network) throws IOException {
        // Create CRAC creation context
        CRACDocumentType nativeCseCrac = fileImporter.importCseCrac(cracUrl);
        // Pre-treatment on network
        Set<BusBarChangeSwitches> busBarChangeSwitchesSet = BusBarChangePreProcessor.process(network, nativeCseCrac);
        LOGGER.info("Importing Crac Creation Parameters file: {}", CRAC_CREATION_PARAMETERS_JSON);
        InputStream cracCreationParametersIs = getClass().getResourceAsStream(CRAC_CREATION_PARAMETERS_JSON);
        CracCreationParameters cracCreationParameters = CracCreationParametersService.getCracCreationParameters(cracCreationParametersIs, busBarChangeSwitchesSet);
        // input stream null and file name
        CseCracCreationContext cracCreationContext = (CseCracCreationContext) Crac.readWithContext(FileUtil.getFilenameFromUrl(cracUrl), fileImporter.openUrlStream(cracUrl),
                network, targetProcessDateTime, cracCreationParameters);

        return new CracImportData(cracCreationContext, busBarChangeSwitchesSet);
    }

    static final class CracImportData {
        final CseCracCreationContext cseCracCreationContext;
        final Set<BusBarChangeSwitches> busBarChangeSwitchesSet;

        private CracImportData(CseCracCreationContext cseCracCreationContext, Set<BusBarChangeSwitches> busBarChangeSwitchesSet) {
            this.cseCracCreationContext = cseCracCreationContext;
            this.busBarChangeSwitchesSet = busBarChangeSwitchesSet;
        }
    }

    private boolean checkIsInterrupted(CseRequest cseRequest) {
        ResponseEntity<Boolean> responseEntity = restTemplateBuilder.build().getForEntity(getInterruptedUrl(cseRequest.getCurrentRunId()), Boolean.class);
        return responseEntity.getBody() != null && responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody();
    }

    private String getInterruptedUrl(String runId) {
        return urlConfiguration.getInterruptServerUrl() + runId;
    }
}
