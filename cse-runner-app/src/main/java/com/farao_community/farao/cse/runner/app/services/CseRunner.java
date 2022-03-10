/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.computation.CseComputationException;
import com.farao_community.farao.cse.data.ttc_res.TtcResult;
import com.farao_community.farao.cse.network_processing.busbar_change.BusBarChangeProcessor;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.runner.app.CseData;
import com.farao_community.farao.cse.runner.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import com.farao_community.farao.cse.runner.app.util.MerchantLine;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.BusBarChangeSwitches;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final DichotomyRunner dichotomyRunner;
    private final TtcResultService ttcResultService;

    public CseRunner(FileImporter fileImporter, FileExporter fileExporter, DichotomyRunner dichotomyRunner, TtcResultService ttcResultService) {
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.dichotomyRunner = dichotomyRunner;
        this.ttcResultService = ttcResultService;
    }

    public CseResponse run(CseRequest cseRequest) throws IOException {
        List<String> missingFiles = checkMissingFiles(cseRequest);
        if (!missingFiles.isEmpty()) {
            String ttcResultUrl = ttcResultService.saveFailedTtcResult(
                cseRequest,
                cseRequest.getCgmUrl(),
                TtcResult.FailedProcessData.FailedProcessReason.MISSING_FILES,
                String.join(", ", missingFiles));
            return new CseResponse(cseRequest.getId(), ttcResultUrl, cseRequest.getCgmUrl());
        }
        CseData cseData = new CseData(cseRequest, fileImporter);

        Network network = fileImporter.importNetwork(cseRequest.getCgmUrl());
        MerchantLine.activateMerchantLine(cseRequest.getProcessType(), network);
        cseData.setPreProcesedNetworkUrl(fileExporter.saveNetwork(network, cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType()).getUrl());

        double initialItalianImportFromNetwork;
        try {
            initialItalianImportFromNetwork = BorderExchanges.computeItalianImport(network);
        } catch (CseComputationException e) {
            String ttcResultUrl = ttcResultService.saveFailedTtcResult(
                cseRequest,
                cseRequest.getCgmUrl(),
                TtcResult.FailedProcessData.FailedProcessReason.LOAD_FLOW_FAILURE);
            return new CseResponse(cseRequest.getId(), ttcResultUrl, cseRequest.getCgmUrl());
        }

        checkNetworkAndReferenceExchangesDifference(cseData, initialItalianImportFromNetwork);

        Crac crac = preProcessNetworkAndImportCrac(cseRequest.getMergedCracUrl(), network, cseRequest.getTargetProcessDateTime());
        cseData.setJsonCracUrl(fileExporter.saveCracInJsonFormat(crac, cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType()));

        String baseCaseFilePath = fileExporter.getBaseCaseFilePath(cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());
        String baseCaseFileUrl = fileExporter.saveNetworkInUcteFormat(network, baseCaseFilePath);

        DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.runDichotomy(cseRequest, cseData, network, initialItalianImportFromNetwork);
        String ttcResultUrl;
        String finalCgmUrl;
        if (dichotomyResult.hasValidStep()) {
            String finalCgmPath = fileExporter.getFinalNetworkFilePath(cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());
            finalCgmUrl = fileExporter.saveNetworkInUcteFormat(
                fileImporter.importNetwork(dichotomyResult.getHighestValidStep().getValidationData().getNetworkWithPraFileUrl()),
                finalCgmPath);
            ttcResultUrl = ttcResultService.saveTtcResult(cseRequest, cseData,
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

    Crac preProcessNetworkAndImportCrac(String mergedCracUrl, Network initialNetwork, OffsetDateTime targetProcessDateTime) throws IOException {
        CseCrac cseCrac = fileImporter.importCseCrac(mergedCracUrl);
        Set<BusBarChangeSwitches> busBarChangeSwitchesSet = BusBarChangeProcessor.process(initialNetwork, cseCrac);
        return fileImporter.importCrac(cseCrac, busBarChangeSwitchesSet, targetProcessDateTime, initialNetwork);
    }

    private void checkNetworkAndReferenceExchangesDifference(CseData cseData, double initialItalianImportFromNetwork) {
        double referenceItalianImport = cseData.getCseReferenceExchanges().getExchanges().values().stream().reduce(0., Double::sum);
        if (Math.abs(referenceItalianImport - initialItalianImportFromNetwork) / Math.abs(referenceItalianImport) > NETWORK_AND_REFERENCE_EXCHANGES_DIFFERENCE_THRESHOLD) {
            LOGGER.warn("Difference between vulcanus exchanges and network exchanges too high.");
        }
    }

    private List<String> checkMissingFiles(CseRequest cseRequest) {
        List<String> missingFiles = new ArrayList<>(10);
        if (cseRequest.getCgmUrl() == null || cseRequest.getCgmUrl().isEmpty()) {
            missingFiles.add("CGM");
        }
        if (cseRequest.getMergedCracUrl() == null || cseRequest.getMergedCracUrl().isEmpty()) {
            missingFiles.add("CRAC");
        }
        if (cseRequest.getMergedGlskUrl() == null || cseRequest.getMergedGlskUrl().isEmpty()) {
            missingFiles.add("GLSK");
        }
        if (cseRequest.getYearlyNtcUrl() == null || cseRequest.getYearlyNtcUrl().isEmpty()) {
            missingFiles.add("YEARLY NTC");
        }
        if (cseRequest.getNtcReductionsUrl() == null || cseRequest.getNtcReductionsUrl().isEmpty()) {
            missingFiles.add("NTC REDUCTIONS");
        }
        if (cseRequest.getVulcanusUrl() == null || cseRequest.getVulcanusUrl().isEmpty()) {
            missingFiles.add("VULCANUS");
        }
        if (cseRequest.getProcessType() == ProcessType.IDCC) {
            if (cseRequest.getNtc2AtItUrl() == null || cseRequest.getNtc2AtItUrl().isEmpty()) {
                missingFiles.add("NTC2-AT");
            }
            if (cseRequest.getNtc2ChItUrl() == null || cseRequest.getNtc2ChItUrl().isEmpty()) {
                missingFiles.add("NTC2-CH");
            }
            if (cseRequest.getNtc2FrItUrl() == null || cseRequest.getNtc2FrItUrl().isEmpty()) {
                missingFiles.add("NTC2-FR");
            }
            if (cseRequest.getNtc2SiItUrl() == null || cseRequest.getNtc2SiItUrl().isEmpty()) {
                missingFiles.add("NTC2-SI");
            }
        }
        return missingFiles;
    }
}
