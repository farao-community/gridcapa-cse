/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.dichotomy.api.exceptions.RaoFailureException;
import com.farao_community.farao.dichotomy.api.exceptions.RaoInterruptionException;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class RaoRunnerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaoRunnerService.class);

    private final Logger businessLogger;
    private final RaoRunnerClient raoRunnerClient;

    public RaoRunnerService(RaoRunnerClient raoRunnerClient, Logger businessLogger) {
        this.raoRunnerClient = raoRunnerClient;
        this.businessLogger = businessLogger;
    }

    public RaoSuccessResponse run(String id, String runId, String networkPresignedUrl, String cracInJsonFormatUrl, String raoParametersUrl, String artifactDestinationPath) throws CseInternalException, RaoInterruptionException, RaoFailureException {
        RaoRequest raoRequest = buildRaoRequest(id, runId, networkPresignedUrl, cracInJsonFormatUrl, raoParametersUrl, artifactDestinationPath);
        try {
            LOGGER.info("RAO request sent: {}", raoRequest);
            AbstractRaoResponse abstractRaoResponse = raoRunnerClient.runRao(raoRequest);
            LOGGER.info("RAO response received: {}", abstractRaoResponse);
            if (abstractRaoResponse.isRaoFailed()) {
                RaoFailureResponse failureResponse = (RaoFailureResponse) abstractRaoResponse;
                businessLogger.error("RAO computation failed: {}", failureResponse.getErrorMessage());
                throw new RaoFailureException(failureResponse.getErrorMessage());
            }

            RaoSuccessResponse raoResponse = (RaoSuccessResponse) abstractRaoResponse;
            if (raoResponse.isInterrupted()) {
                LOGGER.info("RAO has been interrupted");
                throw new RaoInterruptionException("RAO has been interrupted");
            }
            return raoResponse;
        } catch (RuntimeException e) {
            throw new CseInternalException("RAO run failed", e);
        }
    }

    private RaoRequest buildRaoRequest(String id, String runId, String networkPresignedUrl, String cracUrl, String raoParametersUrl, String artifactDestinationPath) {
        return new RaoRequest.RaoRequestBuilder()
                .withId(id)
                .withRunId(runId)
                .withNetworkFileUrl(networkPresignedUrl)
                .withCracFileUrl(cracUrl)
                .withRaoParametersFileUrl(raoParametersUrl)
                .withResultsDestination(artifactDestinationPath)
                .build();
    }
}
