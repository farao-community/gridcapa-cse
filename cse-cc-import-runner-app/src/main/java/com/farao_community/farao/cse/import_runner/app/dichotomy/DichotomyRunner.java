/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.services.FileExporter;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.import_runner.app.services.ForcedPrasHandler;
import com.farao_community.farao.cse.import_runner.app.services.InterruptionService;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.index.BiDirectionalStepsWithReferenceIndexStrategy;
import com.farao_community.farao.dichotomy.api.index.Index;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class DichotomyRunner {
    private static final double MIN_IMPORT_VALUE = 0;
    private static final double MAX_IMPORT_VALUE = 19999;
    private static final String DICHOTOMY_PARAMETERS_MSG = "Starting dichotomy index: {}, Minimum dichotomy index: {}, Maximum dichotomy index: {}, Initial dichotomy step: {}, Dichotomy precision: {}";

    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final NetworkShifterProvider networkShifterProvider;
    private final ForcedPrasHandler forcedPrasHandler;
    private final RaoRunnerClient raoRunnerClient;
    private final Logger businessLogger;
    private final InterruptionService interruptionService;

    public DichotomyRunner(FileExporter fileExporter, FileImporter fileImporter, NetworkShifterProvider networkShifterProvider, ForcedPrasHandler forcedPrasHandler, RaoRunnerClient raoRunnerClient, Logger businessLogger, InterruptionService interruptionService) {
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.networkShifterProvider = networkShifterProvider;
        this.forcedPrasHandler = forcedPrasHandler;
        this.raoRunnerClient = raoRunnerClient;
        this.businessLogger = businessLogger;
        this.interruptionService = interruptionService;
    }

    public DichotomyResult<DichotomyRaoResponse> runDichotomy(final CseRequest cseRequest,
                                                              final CseData cseData,
                                                              final Network network,
                                                              final double initialIndexValue,
                                                              final Map<String, Double> referenceExchanges,
                                                              final Map<String, Double> ntcsByEic,
                                                              final Set<String> forcedPrasIds) {
        return runDichotomy(cseRequest, cseData, network, initialIndexValue, MIN_IMPORT_VALUE, referenceExchanges, ntcsByEic, forcedPrasIds);
    }

    public DichotomyResult<DichotomyRaoResponse> runDichotomy(final CseRequest cseRequest,
                                                              final CseData cseData,
                                                              final Network network,
                                                              final double initialIndexValue,
                                                              final double minImportValue,
                                                              final Map<String, Double> referenceExchanges,
                                                              final Map<String, Double> ntcsByEic,
                                                              final Set<String> forcedPrasIds) {
        final double initialDichotomyStep = cseRequest.getInitialDichotomyStep();
        final double dichotomyPrecision = cseRequest.getDichotomyPrecision();
        businessLogger.info(DICHOTOMY_PARAMETERS_MSG, (int) initialIndexValue, (int) minImportValue, (int) MAX_IMPORT_VALUE, (int) initialDichotomyStep, (int) dichotomyPrecision);
        final Index<DichotomyRaoResponse> index = new Index<>(minImportValue, MAX_IMPORT_VALUE, dichotomyPrecision);
        final DichotomyEngine<DichotomyRaoResponse> engine = DichotomyEngine.<DichotomyRaoResponse>builder()
                .withIndex(index)
                .withIndexStrategy(new BiDirectionalStepsWithReferenceIndexStrategy<>(initialIndexValue, initialDichotomyStep, NetworkShifterUtil.getReferenceItalianImport(referenceExchanges)))
                .withInterruptionStrategy(interruptionService)
                .withNetworkShifter(networkShifterProvider.get(cseRequest, cseData, network, referenceExchanges, ntcsByEic))
                .withNetworkValidator(getNetworkValidator(cseRequest, cseData, forcedPrasIds))
                .withNetworkExporter(new CseNetworkExporter(cseRequest, fileExporter))
                .withRunId(cseRequest.getCurrentRunId())
                .build();
        return engine.run(network);
    }

    private NetworkValidator<DichotomyRaoResponse> getNetworkValidator(CseRequest request, CseData cseData, Set<String> forcedPrasIds) {
        final boolean isImportEcProcess = request.isImportEcProcess();
        return new RaoRunnerValidator(
                request,
                cseData.getJsonCracUrl(),
                fileExporter.saveRaoParameters(request.getTargetProcessDateTime(), request.getProcessType(), isImportEcProcess),
                raoRunnerClient,
                fileExporter,
                fileImporter,
                forcedPrasHandler,
                forcedPrasIds,
                isImportEcProcess,
                businessLogger);
    }
}
