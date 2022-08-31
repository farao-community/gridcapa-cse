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
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.index.BiDirectionalStepsIndexStrategy;
import com.farao_community.farao.dichotomy.api.index.Index;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

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
    private final Logger logger;

    public DichotomyRunner(FileExporter fileExporter, FileImporter fileImporter, NetworkShifterProvider networkShifterProvider, ForcedPrasHandler forcedPrasHandler, RaoRunnerClient raoRunnerClient, Logger logger) {
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.networkShifterProvider = networkShifterProvider;
        this.forcedPrasHandler = forcedPrasHandler;
        this.raoRunnerClient = raoRunnerClient;
        this.logger = logger;
    }

    public DichotomyResult<RaoResponse> runDichotomy(CseRequest cseRequest,
                                                     CseData cseData,
                                                     Network network,
                                                     double initialItalianImport,
                                                     Set<String> forcedPrasIds) throws IOException {
        return runDichotomy(cseRequest, cseData, network, initialItalianImport, MIN_IMPORT_VALUE, forcedPrasIds);
    }

    public DichotomyResult<RaoResponse> runDichotomy(CseRequest cseRequest,
                                                     CseData cseData,
                                                     Network network,
                                                     double initialItalianImport,
                                                     double minImportValue,
                                                     Set<String> forcedPrasIds) throws IOException {
        double initialIndexValue = Optional.ofNullable(cseRequest.getInitialDichotomyIndex()).orElse(initialItalianImport);
        double initialDichotomyStep = cseRequest.getInitialDichotomyStep();
        double dichotomyPrecision = cseRequest.getDichotomyPrecision();
        logger.info(DICHOTOMY_PARAMETERS_MSG, (int) initialIndexValue, minImportValue, MAX_IMPORT_VALUE, (int) initialDichotomyStep, (int) dichotomyPrecision);
        Index<RaoResponse> index = new Index<>(minImportValue, MAX_IMPORT_VALUE, dichotomyPrecision);
        DichotomyEngine<RaoResponse> engine = new DichotomyEngine<>(
            index,
            new BiDirectionalStepsIndexStrategy(initialIndexValue, initialDichotomyStep),
            networkShifterProvider.get(cseRequest, cseData, network),
            getNetworkValidator(cseRequest, cseData, forcedPrasIds));
        return engine.run(network);
    }

    private NetworkValidator<RaoResponse> getNetworkValidator(CseRequest request, CseData cseData, Set<String> forcedPrasIds) {
        return new RaoRunnerValidator(
            request.getProcessType(),
            request.getId(),
            request.getTargetProcessDateTime(),
            cseData.getJsonCracUrl(),
            fileExporter.saveRaoParameters(request.getTargetProcessDateTime(), request.getProcessType()),
            raoRunnerClient,
            fileExporter,
            fileImporter,
            forcedPrasHandler,
            forcedPrasIds);
    }
}
