/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_cse.app.services;

import com.farao_community.farao.dichotomy_runner.api.resource.*;
import com.farao_community.farao.dichotomy_runner.starter.DichotomyClient;
import com.farao_community.farao.gridcapa_cse.api.resource.CseRequest;
import com.farao_community.farao.gridcapa_cse.api.resource.ProcessType;
import com.farao_community.farao.gridcapa_cse.app.CseData;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Random;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class DichotomyRunner {
    private static final double MAX_IMPORT_VALUE = 19999;
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters.json";
    private static final Random RANDOM = new SecureRandom();

    private final UrlValidationService urlValidationService;
    private final MinioAdapter minioAdapter;
    private final DichotomyClient dichotomyClient;

    public DichotomyRunner(UrlValidationService urlValidationService, MinioAdapter minioAdapter, DichotomyClient dichotomyClient) {
        this.urlValidationService = urlValidationService;
        this.minioAdapter = minioAdapter;
        this.dichotomyClient = dichotomyClient;
    }

    public DichotomyResponse runDichotomy(CseRequest cseRequest, CseData cseData, double initialItalianImportFromNetwork) {
        String raoParametersUrl = saveRaoParameters();
        DichotomyParameters dichotomyParameters = new DichotomyParameters(
            Optional.ofNullable(cseRequest.getInitialDichotomyIndex()).orElse(initialItalianImportFromNetwork),
            MAX_IMPORT_VALUE,
            cseRequest.getDichotomyPrecision(),
            getShiftDispatcherConfiguration(cseRequest.getProcessType(), cseData),
            new StepsIndexStrategyConfiguration(true, cseRequest.getInitialDichotomyStep())
        );

        DichotomyRequest dichotomyRequest = new DichotomyRequest(String.valueOf(RANDOM.nextLong()),
            new DichotomyFileResource(urlValidationService.getFileNameFromUrl(cseData.getPreProcesedNetworkUrl()), cseData.getPreProcesedNetworkUrl()),
            new DichotomyFileResource(urlValidationService.getFileNameFromUrl(cseData.getJsonCracUrl()), cseData.getJsonCracUrl()),
            new DichotomyFileResource(urlValidationService.getFileNameFromUrl(cseRequest.getMergedGlskUrl()), cseRequest.getMergedGlskUrl()),
            new DichotomyFileResource(urlValidationService.getFileNameFromUrl(raoParametersUrl), raoParametersUrl),
            dichotomyParameters);
        return dichotomyClient.runDichotomy(dichotomyRequest);
    }

    private String saveRaoParameters() {
        RaoParameters raoParameters = RaoParameters.load();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        String raoParametersDestinationPath = RAO_PARAMETERS_FILE_NAME;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadFile(raoParametersDestinationPath, bais);
        return minioAdapter.generatePreSignedUrl(raoParametersDestinationPath);
    }

    private ShiftDispatcherConfiguration getShiftDispatcherConfiguration(ProcessType processType, CseData cseData) {
        if (processType == ProcessType.D2CC) {
            return new SplittingFactorsConfiguration(cseData.getReducedSplittingFactors());
        } else {
            return new CseIdccShiftDispatcherConfiguration(
                cseData.getReducedSplittingFactors(),
                cseData.getCseReferenceExchanges().getExchanges(),
                cseData.getNtc2().getExchanges()
            );
        }
    }
}
