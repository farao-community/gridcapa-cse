/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.app.dichotomy;

import com.farao_community.farao.cse.runner.api.resource.FileResource;
import com.farao_community.farao.cse.runner.app.services.FileExporter;
import com.farao_community.farao.cse.runner.app.services.FileImporter;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoRunnerValidator implements NetworkValidator<RaoResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaoRunnerValidator.class);

    private final String requestId;
    private final String cracUrl;
    private final String raoParametersUrl;
    private final RaoRunnerClient raoRunnerClient;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;

    public RaoRunnerValidator(String requestId,
                              String cracUrl,
                              String raoParametersUrl,
                              RaoRunnerClient raoRunnerClient,
                              FileExporter fileExporter,
                              FileImporter fileImporter) {
        this.requestId = requestId;
        this.cracUrl = cracUrl;
        this.raoParametersUrl = raoParametersUrl;
        this.raoRunnerClient = raoRunnerClient;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
    }

    @Override
    public DichotomyStepResult<RaoResponse> validateNetwork(Network network) throws ValidationException {
        FileResource networkFile = fileExporter.saveNetwork(network, networkScaledFilePath(network));
        RaoRequest raoRequest = buildRaoRequest(networkFile);
        try {
            LOGGER.info("RAO request sent: {}", raoRequest);
            RaoResponse raoResponse = raoRunnerClient.runRao(raoRequest);
            LOGGER.info("RAO response received: {}", raoResponse);
            Crac crac = fileImporter.importCracFromJson(raoResponse.getCracFileUrl());
            RaoResult raoResult = fileImporter.importRaoResult(raoResponse.getRaoResultFileUrl(), crac);
            return DichotomyStepResult.fromNetworkValidationResult(raoResult, raoResponse);
        } catch (RuntimeException | IOException e) {
            throw new ValidationException("RAO run failed. Nested exception: " + e.getMessage());
        }
    }

    private RaoRequest buildRaoRequest(FileResource networkFile) {
        String raoRequestId = String.format("%s-%s", requestId, networkFile.getFilename());
        return new RaoRequest(raoRequestId, networkFile.getUrl(), cracUrl, raoParametersUrl);
    }

    private String networkScaledFilePath(Network network) {
        String variantName = network.getVariantManager().getWorkingVariantId();
        String networkFilename = String.format("%s-%s.xiidm", network.getNameOrId(), variantName);
        return String.format("%s/%s", requestId, networkFilename);
    }
}
