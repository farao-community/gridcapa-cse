/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.dichotomy.api.exceptions.RaoInterruptionException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class RaoRunnerServiceTest {

    @Mock
    private RaoRunnerClient raoRunnerClient;

    @InjectMocks
    private RaoRunnerService raoRunnerService;

    private final String id = "testId";
    private final String runId = "testRunId";
    private final String networkPresignedUrl = "http://network.url";
    private final String cracInJsonFormatUrl = "http://crac.url";
    private final String raoParametersUrl = "http://parameters.url";
    private final String artifactDestinationPath = "/path/to/artifact";

    @Test
    void testRunSuccessful() throws CseInternalException, RaoInterruptionException {
        RaoResponse expectedResponse = new RaoResponse.RaoResponseBuilder().withId("id").build(); // Assuming RaoResponse is a valid response type

        when(raoRunnerClient.runRao(any(RaoRequest.class))).thenReturn(expectedResponse);

        RaoResponse actualResponse = raoRunnerService.run(id, runId, networkPresignedUrl, cracInJsonFormatUrl, raoParametersUrl, artifactDestinationPath);

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void testRunThrowsCseInternalException() {
        when(raoRunnerClient.runRao(any())).thenThrow(new RuntimeException("Test exception"));

        Exception exception = assertThrows(CseInternalException.class, () -> {
            raoRunnerService.run(id, runId, networkPresignedUrl, cracInJsonFormatUrl, raoParametersUrl, artifactDestinationPath);
        });

        String expectedMessage = "RAO run failed";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testRaoRequestValues() {
        RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId(id)
                .withRunId(runId)
                .withNetworkFileUrl(networkPresignedUrl)
                .withCracFileUrl(cracInJsonFormatUrl)
                .withRaoParametersFileUrl(raoParametersUrl)
                .withResultsDestination(artifactDestinationPath)
                .build();

        assertEquals("testId", raoRequest.getId());
        assertEquals("testRunId", raoRequest.getRunId());
        assertEquals("http://network.url", raoRequest.getNetworkFileUrl());
        assertEquals("http://crac.url", raoRequest.getCracFileUrl());
        assertEquals("http://parameters.url", raoRequest.getRaoParametersFileUrl());
        assertEquals("/path/to/artifact", raoRequest.getResultsDestination().get());
    }
}
