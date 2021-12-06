/*
 *
 *  * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.cse.runner.app.dichotomy;

import com.farao_community.farao.cse.runner.app.services.FileExporter;
import com.farao_community.farao.cse.runner.app.services.FileImporter;
import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.Index;
import com.farao_community.farao.dichotomy.api.StepsIndexStrategy;
import com.farao_community.farao.dichotomy.api.ValidationStrategy;
import com.farao_community.farao.dichotomy.network.NetworkDichotomyResult;
import com.farao_community.farao.dichotomy.network.NetworkDichotomyStepResult;
import com.farao_community.farao.dichotomy.network.NetworkValidator;
import com.farao_community.farao.dichotomy.network.scaling.ScalingNetworkValidationStrategy;
import com.farao_community.farao.dichotomy.network.scaling.ShiftDispatcher;
import com.farao_community.farao.dichotomy.network.scaling.SplittingFactors;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.runner.app.CseData;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class DichotomyRunner {
    private static final double MAX_IMPORT_VALUE = 19999;

    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final RaoRunnerClient raoRunnerClient;

    public DichotomyRunner(FileExporter fileExporter, FileImporter fileImporter, RaoRunnerClient raoRunnerClient) {
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.raoRunnerClient = raoRunnerClient;
    }

    public NetworkDichotomyResult<RaoResponse> runDichotomy(CseRequest cseRequest,
                                                            CseData cseData,
                                                            Network network,
                                                            double initialItalianImportFromNetwork) throws IOException {
        Index<NetworkDichotomyStepResult<RaoResponse>> index = new Index<>(
            Optional.ofNullable(cseRequest.getInitialDichotomyIndex()).orElse(initialItalianImportFromNetwork),
            MAX_IMPORT_VALUE,
            cseRequest.getDichotomyPrecision());
        DichotomyEngine<NetworkDichotomyStepResult<RaoResponse>> engine = new DichotomyEngine<>(
            index,
            new StepsIndexStrategy(true, cseRequest.getInitialDichotomyStep()),
            buildValidationStrategy(cseRequest, cseData, network));
        engine.run();
        return NetworkDichotomyResult.buildFromIndex(index);
    }

    private ValidationStrategy<NetworkDichotomyStepResult<RaoResponse>> buildValidationStrategy(CseRequest request,
                                                                                                CseData cseData,
                                                                                                Network network) throws IOException {
        NetworkValidator<RaoResponse> networkValidator = new RaoRunnerValidator(
            request.getId(),
            cseData.getJsonCracUrl(),
            fileExporter.saveRaoParameters(),
            raoRunnerClient,
            fileExporter,
            fileImporter);

        return new ScalingNetworkValidationStrategy<>(
            network,
            networkValidator,
            fileImporter.importGlsk(request.getMergedGlskUrl(), network),
            getShiftDispatcher(request.getProcessType(), cseData));
    }

    private ShiftDispatcher getShiftDispatcher(ProcessType processType, CseData cseData) {
        if (processType == ProcessType.D2CC) {
            return new SplittingFactors(cseData.getReducedSplittingFactors());
        } else {
            return new CseIdccShiftDispatcher(
                cseData.getReducedSplittingFactors(),
                cseData.getCseReferenceExchanges().getExchanges(),
                cseData.getNtc2().getExchanges()
            );
        }
    }
}
