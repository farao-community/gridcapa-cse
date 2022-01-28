/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.app.dichotomy;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.runner.app.CseData;
import com.farao_community.farao.cse.runner.app.services.FileExporter;
import com.farao_community.farao.cse.runner.app.services.FileImporter;
import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.index.Index;
import com.farao_community.farao.dichotomy.api.index.StepsIndexStrategy;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.dichotomy.shift.SplittingFactors;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

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

    public DichotomyResult<RaoResponse> runDichotomy(CseRequest cseRequest,
                                                     CseData cseData,
                                                     Network network,
                                                     double initialItalianImportFromNetwork) throws IOException {
        Index<RaoResponse> index = new Index<>(
            Optional.ofNullable(cseRequest.getInitialDichotomyIndex()).orElse(initialItalianImportFromNetwork),
            MAX_IMPORT_VALUE,
            cseRequest.getDichotomyPrecision());
        DichotomyEngine<RaoResponse> engine = new DichotomyEngine<>(
            index,
            new StepsIndexStrategy(true, cseRequest.getInitialDichotomyStep()),
            getNetworkShifter(cseRequest, cseData, network),
            getNetworkValidator(cseRequest, cseData));
        return engine.run(network);
    }

    private NetworkShifter getNetworkShifter(CseRequest request,
                                             CseData cseData,
                                             Network network) throws IOException {
        return new LinearScaler(
            fileImporter.importGlsk(request.getMergedGlskUrl(), network),
            getShiftDispatcher(request.getProcessType(), cseData));
    }

    private ShiftDispatcher getShiftDispatcher(ProcessType processType, CseData cseData) {
        if (processType == ProcessType.D2CC) {
            return new SplittingFactors(cseData.getReducedSplittingFactors());
        } else {
            return new CseIdccShiftDispatcher(
                convertSplittingFactors(cseData.getReducedSplittingFactors()),
                cseData.getCseReferenceExchanges().getExchanges(),
                cseData.getNtc2().getExchanges()
            );
        }
    }

    private NetworkValidator<RaoResponse> getNetworkValidator(CseRequest request, CseData cseData) {
        return new RaoRunnerValidator(
            request.getProcessType(),
            request.getId(),
            request.getTargetProcessDateTime(),
            cseData.getJsonCracUrl(),
            fileExporter.getRaoParametersUrl(request.getProcessType()),
            raoRunnerClient,
            fileExporter,
            fileImporter);
    }

    Map<String, Double> convertSplittingFactors(Map<String, Double> tSplittingFactors) {
        Map<String, Double> splittingFactors = new TreeMap<>();
        tSplittingFactors.forEach((key, value) -> splittingFactors.put(toEic(key), value));
        splittingFactors.put(toEic("IT"), -splittingFactors.values().stream().reduce(0., Double::sum));
        return splittingFactors;
    }

    private String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }
}
