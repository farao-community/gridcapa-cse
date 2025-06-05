/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.runner.api.resource.CseExportRequest;
import com.farao_community.farao.cse.runner.api.resource.CseExportResponse;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class CseExportRunnerTest {

    @Autowired
    private CseExportRunner cseExportRunner;

    @MockitoBean
    private RestTemplateBuilder restTemplateBuilder;

    @MockitoBean
    private TtcRaoService ttcRaoService;

    @Test
    void getFinalNetworkFilenameTest() {
        OffsetDateTime processTargetDate = OffsetDateTime.parse("2022-10-20T16:30Z");
        String initialCgmFilename = "20221020_1830_155_Transit_CSE1.uct";
        String actualFilenameWithoutExtension = cseExportRunner.getFinalNetworkFilenameWithoutExtension(processTargetDate, initialCgmFilename, ProcessType.IDCC);
        String expectedFilenameWithoutExtension = "20221020_1830_2D4_ce_Transit_RAO_CSE1";
        assertEquals(expectedFilenameWithoutExtension, actualFilenameWithoutExtension);

    }

    @Test
    void runInterruptPendingCase() throws IOException {
        CseExportRequest request = Mockito.mock(CseExportRequest.class);
        Mockito.when(request.getId()).thenReturn("ID");
        Mockito.when(request.getCurrentRunId()).thenReturn("RUNID");
        Mockito.when(request.getProcessType()).thenReturn(ProcessType.D2CC);
        Mockito.when(request.getTargetProcessDateTime()).thenReturn(OffsetDateTime.now());
        Mockito.when(request.getCgmUrl()).thenReturn("testCgmUrl");

        RestTemplate restTemplate = mock(RestTemplate.class);
        ResponseEntity<Boolean> responseEntity = mock(ResponseEntity.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(restTemplate.getForEntity(anyString(), eq(Boolean.class))).thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        when(responseEntity.getBody()).thenReturn(true);

        CseExportResponse cseExportResponse = cseExportRunner.run(request);
        assertTrue(cseExportResponse.isInterrupted());
        assertEquals("ID", cseExportResponse.getId());
    }
}
