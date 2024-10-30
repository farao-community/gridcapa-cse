/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.api;

import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseExportRequest;
import com.farao_community.farao.cse.runner.api.resource.CseExportResponse;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
class JsonApiConverterTest {

    @Test
    void checkCseRequestJsonConversion() throws IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        byte[] requestBytes = getClass().getResourceAsStream("/cseRequestMessage.json").readAllBytes();
        CseRequest request = jsonApiConverter.fromJsonMessage(requestBytes, CseRequest.class);

        assertEquals("id", request.getId());
        assertEquals("networkFileUrl", request.getCgmUrl());
    }

    @Test
    void checkCseResponseJsonConversion() throws IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        byte[] responseBytes = getClass().getResourceAsStream("/cseResponseMessage.json").readAllBytes();
        CseResponse response = jsonApiConverter.fromJsonMessage(responseBytes, CseResponse.class);

        assertEquals("id", response.getId());
        assertEquals("ttcFileUrl", response.getTtcFileUrl());

    }

    @Test
    void checkExceptionJsonConversion() throws URISyntaxException, IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        CseInternalException exception = new CseInternalException("Something really bad happened");
        String expectedExceptionMessage = Files.readString(Paths.get(getClass().getResource("/errorMessage.json").toURI()));
        assertEquals(expectedExceptionMessage, new String(jsonApiConverter.toJsonMessage(exception)));

    }

    @Test
    void checkCseExportRequestJsonConversion() throws IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        byte[] requestBytes = getClass().getResourceAsStream("/cseExportRequestMessage.json").readAllBytes();
        CseExportRequest request = jsonApiConverter.fromJsonMessage(requestBytes, CseExportRequest.class);

        assertEquals("id", request.getId());
        assertEquals("mergedCracUrl", request.getMergedCracUrl());
    }

    @Test
    void checkCseExportResponseJsonConversion() throws IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        byte[] responseBytes = getClass().getResourceAsStream("/cseExportResponseMessage.json").readAllBytes();
        CseExportResponse response = jsonApiConverter.fromJsonMessage(responseBytes, CseExportResponse.class);

        assertEquals("id", response.getId());
    }
}
