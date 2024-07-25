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
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class RequestServiceTest {

    @MockBean
    private CseRunner cseServer;

    @MockBean
    private StreamBridge streamBridge;

    @Autowired
    private RequestService requestService;

    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    @Test
    void testRequestService() throws IOException {
        String id = UUID.randomUUID().toString();
        String runId = UUID.randomUUID().toString();
        CseRequest cseRequest = new CseRequest(id, runId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 0.0, 0.0, null, false);
        CseResponse cseResponse = new CseResponse(cseRequest.getId(), "null", "null", false);
        byte[] req = jsonApiConverter.toJsonMessage(cseRequest, CseRequest.class);
        when(cseServer.run(any())).thenReturn(cseResponse);

        requestService.launchCseRequest(req);

        ArgumentCaptor<TaskStatusUpdate> captor = ArgumentCaptor.forClass(TaskStatusUpdate.class);
        verify(streamBridge, times(2)).send(any(), captor.capture());
        assertEquals(TaskStatus.RUNNING, captor.getAllValues().get(0).getTaskStatus());
        assertEquals(TaskStatus.SUCCESS, captor.getAllValues().get(1).getTaskStatus());
    }

    @Test
    void testInterruptedRequestService() throws IOException {
        String id = UUID.randomUUID().toString();
        String runId = UUID.randomUUID().toString();
        CseRequest cseRequest = new CseRequest(id, runId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 0.0, 0.0, null, false);
        CseResponse cseResponse = new CseResponse(cseRequest.getId(), "null", "null", true);
        byte[] req = jsonApiConverter.toJsonMessage(cseRequest, CseRequest.class);
        when(cseServer.run(any())).thenReturn(cseResponse);

        requestService.launchCseRequest(req);

        ArgumentCaptor<TaskStatusUpdate> captor = ArgumentCaptor.forClass(TaskStatusUpdate.class);
        verify(streamBridge, times(2)).send(any(), captor.capture());
        assertEquals(TaskStatus.RUNNING, captor.getAllValues().get(0).getTaskStatus());
        assertEquals(TaskStatus.INTERRUPTED, captor.getAllValues().get(1).getTaskStatus());
    }

    @Test
    void testErrorRequestService() throws IOException {
        String id = UUID.randomUUID().toString();
        String runId = UUID.randomUUID().toString();
        Exception except = new IOException("Mocked exception");
        CseRequest cseRequest = new CseRequest(id, runId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 0.0, 0.0, null, false);
        byte[] req = jsonApiConverter.toJsonMessage(cseRequest, CseRequest.class);
        when(cseServer.run(any())).thenThrow(except);
        jsonApiConverter.toJsonMessage(new CseInternalException("CSE run failed", except));

        requestService.launchCseRequest(req);

        ArgumentCaptor<TaskStatusUpdate> captor = ArgumentCaptor.forClass(TaskStatusUpdate.class);
        verify(streamBridge, times(2)).send(any(), captor.capture());
        assertEquals(TaskStatus.RUNNING, captor.getAllValues().get(0).getTaskStatus());
        assertEquals(TaskStatus.ERROR, captor.getAllValues().get(1).getTaskStatus());
    }
}
