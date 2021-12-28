/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.dichotomy_runner.api.resource.*;
import com.farao_community.farao.dichotomy_runner.starter.DichotomyClient;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.runner.app.CseData;
import com.powsybl.iidm.network.Country;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;

import static com.farao_community.farao.cse.runner.app.util.FileUtil.getFilenameFromUrl;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class DichotomyRunner {
    private static final double MAX_IMPORT_VALUE = 19999;
    private static final Random RANDOM = new SecureRandom();

    private final FileExporter fileExporter;
    private final DichotomyClient dichotomyClient;

    public DichotomyRunner(FileExporter fileExporter, DichotomyClient dichotomyClient) {
        this.fileExporter = fileExporter;
        this.dichotomyClient = dichotomyClient;
    }

    public DichotomyResponse runDichotomy(CseRequest cseRequest, CseData cseData, double initialItalianImportFromNetwork) {
        DichotomyRequest dichotomyRequest = getDichotomyRequest(cseRequest, cseData, initialItalianImportFromNetwork);
        return dichotomyClient.runDichotomy(dichotomyRequest);
    }

    DichotomyRequest getDichotomyRequest(CseRequest cseRequest, CseData cseData, double initialItalianImportFromNetwork) {
        String raoParametersUrl = fileExporter.saveRaoParameters();
        DichotomyParameters dichotomyParameters = new DichotomyParameters(
            Optional.ofNullable(cseRequest.getInitialDichotomyIndex()).orElse(initialItalianImportFromNetwork),
            MAX_IMPORT_VALUE,
            cseRequest.getDichotomyPrecision(),
            getShiftDispatcherConfiguration(cseRequest.getProcessType(), cseData),
            new StepsIndexStrategyConfiguration(true, cseRequest.getInitialDichotomyStep())
        );

        return new DichotomyRequest(String.valueOf(RANDOM.nextLong()),
            new DichotomyFileResource(getFilenameFromUrl(cseData.getPreProcesedNetworkUrl()), cseData.getPreProcesedNetworkUrl()),
            new DichotomyFileResource(getFilenameFromUrl(cseData.getJsonCracUrl()), cseData.getJsonCracUrl()),
            new DichotomyFileResource(getFilenameFromUrl(cseRequest.getMergedGlskUrl()), cseRequest.getMergedGlskUrl()),
            new DichotomyFileResource(getFilenameFromUrl(raoParametersUrl), raoParametersUrl),
            dichotomyParameters);
    }

    private ShiftDispatcherConfiguration getShiftDispatcherConfiguration(ProcessType processType, CseData cseData) {
        if (processType == ProcessType.D2CC) {
            return new SplittingFactorsConfiguration(convertSplittingFactors(cseData.getReducedSplittingFactors()));
        } else {
            return new CseIdccShiftDispatcherConfiguration(
                convertSplittingFactors(cseData.getReducedSplittingFactors()),
                cseData.getCseReferenceExchanges().getExchanges(),
                cseData.getNtc2().getExchanges()
            );
        }
    }

    private Map<String, Double> convertSplittingFactors(Map<String, Double> tSplittingFactors) {
        Map<String, Double> splittingFactors = new TreeMap<>();
        tSplittingFactors.forEach((key, value) -> splittingFactors.put(toEic(key), value));
        splittingFactors.put(toEic("IT"), -splittingFactors.values().stream().reduce(0., Double::sum));
        return splittingFactors;
    }

    private String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }
}
