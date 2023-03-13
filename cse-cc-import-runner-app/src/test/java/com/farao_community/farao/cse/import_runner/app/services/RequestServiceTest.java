/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.runner.api.JsonApiConverter;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class RequestServiceTest {

    @MockBean
    private CseRunner cseServer;

    @MockBean
    private StreamBridge streamBridge;

    @Autowired
    private RequestService requestService;

    @Test
    void testRequestService() throws IOException {
        String id = UUID.randomUUID().toString();
        CseRequest cseRequest = new CseRequest(id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 0.0, 0.0, null, false);
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        CseResponse cseResponse = new CseResponse(cseRequest.getId(), "null", "null");
        byte[] req = jsonApiConverter.toJsonMessage(cseRequest, CseRequest.class);
        byte[] resp = jsonApiConverter.toJsonMessage(cseResponse, CseResponse.class);
        when(cseServer.run(any())).thenReturn(cseResponse);
        when(streamBridge.send(any(), any())).thenReturn(true);
        byte[] result = requestService.launchCseRequest(req);
        assertArrayEquals(resp, result);
    }

    @Test
    void testInterruptionRequestService() throws IOException {
        String id = UUID.randomUUID().toString();
        Exception except = new InterruptedIOException("interrupted");
        CseRequest cseRequest = new CseRequest(id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 0.0, 0.0, null, false);
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        CseResponse cseResponse = new CseResponse(cseRequest.getId(), null, null);
        byte[] req = jsonApiConverter.toJsonMessage(cseRequest, CseRequest.class);
        byte[] resp = jsonApiConverter.toJsonMessage(cseResponse, CseResponse.class);
        when(cseServer.run(any())).thenThrow(except);
        when(streamBridge.send(any(), any())).thenReturn(true);
        byte[] result = requestService.launchCseRequest(req);
        assertArrayEquals(resp, result);
    }

    @Test
    void testErrorRequestService() throws IOException {
        String id = UUID.randomUUID().toString();
        Exception except = new IOException("Mocked exception");
        CseRequest cseRequest = new CseRequest(id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 0.0, 0.0, null, false);
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        byte[] req = jsonApiConverter.toJsonMessage(cseRequest, CseRequest.class);
        when(cseServer.run(any())).thenThrow(except);
        when(streamBridge.send(any(), any())).thenReturn(true);
        byte[] expectedResult = jsonApiConverter.toJsonMessage(new CseInternalException("CSE run failed", new InvocationTargetException(except)));

        byte[] result = requestService.launchCseRequest(req);

        assertArrayEquals(expectedResult, result);
    }
}
