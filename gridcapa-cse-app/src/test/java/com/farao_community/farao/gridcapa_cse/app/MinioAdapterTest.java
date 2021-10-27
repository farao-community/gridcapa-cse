/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse.app;

import com.farao_community.farao.gridcapa_cse.app.services.MinioAdapter;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class MinioAdapterTest {

    @Autowired
    private MinioAdapter minioAdapter;

    @MockBean
    private MinioClient minioClient;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(minioClient);
    }

    @Test
    void checkThatUploadFileActuallyPutObject() throws Exception {
        minioAdapter.uploadFile("file/path", new ByteArrayInputStream("File content".getBytes()));
        Mockito.verify(minioClient, Mockito.times(1)).putObject(Mockito.any());
    }

    @Test
    void checkGetPresignedUrl() throws Exception {
        Mockito.when(minioClient.getPresignedObjectUrl(Mockito.any())).thenReturn("http://url");
        String url = minioAdapter.generatePreSignedUrl("file/path");
        Mockito.verify(minioClient, Mockito.times(1)).getPresignedObjectUrl(Mockito.any());
        assertEquals("http://url", url);
    }
}
