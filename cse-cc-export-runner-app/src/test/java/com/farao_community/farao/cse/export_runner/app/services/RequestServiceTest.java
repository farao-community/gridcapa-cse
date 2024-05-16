/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.runner.api.JsonApiConverter;
import com.farao_community.farao.cse.runner.api.resource.CseExportRequest;
import com.farao_community.farao.cse.runner.api.resource.CseExportResponse;

import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class RequestServiceTest {
    @MockBean
    private CseExportRunner cseExportRunner;

    @MockBean
    private StreamBridge streamBridge;

    @Autowired
    private RequestService requestService;

    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    @Test
    void testSuccessRequestService() {
        CseExportRequest cseRequest = new CseExportRequest(UUID.randomUUID().toString(), null, null, "", "");
        CseExportResponse cseResponse = new CseExportResponse(cseRequest.getId(), "", "", "",  false);
        byte[] req = jsonApiConverter.toJsonMessage(cseRequest, CseExportRequest.class);
        byte[] resp = jsonApiConverter.toJsonMessage(cseResponse, CseExportResponse.class);

        when(cseExportRunner.run(any())).thenReturn(cseResponse);

        byte[] result = requestService.launchCseRequest(req);

        ArgumentCaptor<TaskStatusUpdate> captor = ArgumentCaptor.forClass(TaskStatusUpdate.class);
        verify(streamBridge, times(2)).send(any(), captor.capture());
        assertEquals(TaskStatus.RUNNING, captor.getAllValues().get(0).getTaskStatus());
        assertEquals(TaskStatus.SUCCESS, captor.getAllValues().get(1).getTaskStatus());

        assertArrayEquals(resp, result);
    }

    @Test
    void testInterruptedRequestService() {
        CseExportRequest cseRequest = new CseExportRequest(UUID.randomUUID().toString(), null, null, "", "");
        CseExportResponse cseResponse = new CseExportResponse(cseRequest.getId(), "", "", "", true);
        byte[] request = jsonApiConverter.toJsonMessage(cseRequest, CseExportRequest.class);
        byte[] expectedResponse = jsonApiConverter.toJsonMessage(cseResponse, CseExportResponse.class);
        when(cseExportRunner.run(any())).thenReturn(cseResponse);

        byte[] actualResponse = requestService.launchCseRequest(request);

        ArgumentCaptor<TaskStatusUpdate> captor = ArgumentCaptor.forClass(TaskStatusUpdate.class);
        verify(streamBridge, times(2)).send(any(), captor.capture());
        assertEquals(TaskStatus.RUNNING, captor.getAllValues().get(0).getTaskStatus());
        assertEquals(TaskStatus.INTERRUPTED, captor.getAllValues().get(1).getTaskStatus());

        assertArrayEquals(expectedResponse, actualResponse);
    }
}
