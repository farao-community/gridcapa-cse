/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.network_processing.busbar_change.BusBarChangeProcessor;
import com.farao_community.farao.cse.runner.app.CseData;
import com.farao_community.farao.cse.runner.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import com.farao_community.farao.cse.runner.app.util.ItalianImport;
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
        CseData cseData = new CseData(cseRequest, fileImporter);

        Network network = fileImporter.importNetwork(cseRequest.getCgmUrl());
        MerchantLine.activateMerchantLine(cseRequest.getProcessType(), network);
        cseData.setPreProcesedNetworkUrl(fileExporter.saveNetwork(network, cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType()).getUrl());
        double initialItalianImportFromNetwork = ItalianImport.compute(network);
        checkNetworkAndReferenceExchangesDifference(cseData, initialItalianImportFromNetwork);

        Crac crac = preProcessNetworkAndImportCrac(cseRequest.getMergedCracUrl(), network, cseRequest.getTargetProcessDateTime());
        cseData.setJsonCracUrl(fileExporter.saveCracInJsonFormat(crac, cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType()));

        DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.runDichotomy(cseRequest, cseData, network, initialItalianImportFromNetwork);
        String baseCaseFilePath = fileExporter.getBaseCaseFilePath(cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());
        String baseCaseFileUrl = fileExporter.saveNetworkInUcteFormat(network, baseCaseFilePath);
        String finalCgmPath = fileExporter.getFinalNetworkFilePath(cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());
        String finalCgmUrl = fileExporter.saveNetworkInUcteFormat(
                fileImporter.importNetwork(dichotomyResult.getHighestValidStep().getValidationData().getNetworkWithPraFileUrl()),
                finalCgmPath);
        String ttcResultUrl = ttcResultService.saveTtcResult(cseRequest, cseData, dichotomyResult, baseCaseFileUrl,  finalCgmUrl);
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
}
