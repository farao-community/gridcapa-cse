/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.data.xsd.ttc_rao.CseRaoResult;
import com.farao_community.farao.cse.export_runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.powsybl.openrao.data.crac.api.Crac;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class FileExporterTest {

    @MockitoBean
    MinioAdapter minioAdapter;

    private MinioMock minioMock;

    @SpyBean
    FileExporter fileExporter;

    @Autowired
    ProcessConfiguration processConfiguration;

    @BeforeEach
    void setUp() throws IOException {
        minioMock = new MinioMock();
        Mockito.doAnswer(invocation -> {
            String path = (String) invocation.getArguments()[0];
            String processTag = (String) invocation.getArguments()[2];
            String fileType = (String) invocation.getArguments()[3];
            minioMock.addEntry(path, new MinioMock.MinioEntry(FilenameUtils.getName(path), GridcapaFileGroup.ARTIFACT, processTag, fileType));
            return null;
        }).when(minioAdapter).uploadArtifactForTimestamp(any(), any(), any(), any(), any());

        Mockito.doAnswer(invocation -> {
            String path = (String) invocation.getArguments()[0];
            String processTag = (String) invocation.getArguments()[2];
            String fileType = (String) invocation.getArguments()[3];
            minioMock.addEntry(path, new MinioMock.MinioEntry(FilenameUtils.getName(path), GridcapaFileGroup.OUTPUT, processTag, fileType));
            return null;
        }).when(minioAdapter).uploadOutputForTimestamp(any(), any(), any(), any(), any());

        Mockito.doReturn(new ByteArrayInputStream("test data".getBytes())).when(fileExporter).getNetworkInputStream(any(), anyString());
    }

    @Test
    void testSaveFinalCgmForIdccProcess() {
        fileExporter.saveNetwork(
            Mockito.mock(Network.class),
            "XIIDM",
            GridcapaFileGroup.OUTPUT,
            ProcessType.IDCC,
            "network-test",
            OffsetDateTime.parse("2021-01-01T15:30Z")
        );

        MinioMock.MinioEntry minioEntry = minioMock.getEntry("CSE/EXPORT/IDCC/2021/01/01/16_30/OUTPUT/network-test.xiidm");
        assertNotNull(minioEntry);
        assertEquals("network-test.xiidm", minioEntry.getFilename());
        assertEquals(processConfiguration.getFinalCgm(), minioEntry.getFileType());
        assertEquals("CSE_EXPORT_IDCC", minioEntry.getProcessTag());
        assertEquals(GridcapaFileGroup.OUTPUT, minioEntry.getGridcapaFileGroup());
    }

    @Test
    void testSaveFinalCgmForIdccProcessInUcteFormat() {
        fileExporter.saveNetwork(
            Mockito.mock(Network.class),
            "UCTE",
            GridcapaFileGroup.OUTPUT,
            ProcessType.IDCC,
            "network-test",
            OffsetDateTime.parse("2021-01-01T15:30Z")
        );

        MinioMock.MinioEntry minioEntry = minioMock.getEntry("CSE/EXPORT/IDCC/2021/01/01/16_30/OUTPUT/network-test.uct");
        assertNotNull(minioEntry);
        assertEquals("network-test.uct", minioEntry.getFilename());
        assertEquals(processConfiguration.getFinalCgm(), minioEntry.getFileType());
        assertEquals("CSE_EXPORT_IDCC", minioEntry.getProcessTag());
        assertEquals(GridcapaFileGroup.OUTPUT, minioEntry.getGridcapaFileGroup());
    }

    @Test
    void testSavePreProcessedCgmForIdccProcess() {
        fileExporter.saveNetwork(
            Mockito.mock(Network.class),
            "XIIDM",
            GridcapaFileGroup.ARTIFACT,
            ProcessType.IDCC,
            "network-test",
            OffsetDateTime.parse("2021-01-01T15:30Z")
        );

        MinioMock.MinioEntry minioEntry = minioMock.getEntry("CSE/EXPORT/IDCC/2021/01/01/16_30/ARTIFACT/network-test.xiidm");
        assertNotNull(minioEntry);
        assertEquals("network-test.xiidm", minioEntry.getFilename());
        assertEquals("", minioEntry.getFileType());
        assertEquals("CSE_EXPORT_IDCC", minioEntry.getProcessTag());
        assertEquals(GridcapaFileGroup.ARTIFACT, minioEntry.getGridcapaFileGroup());
    }

    @Test
    void testSaveCracInJsonFormatForD2ccProcess() {
        fileExporter.saveCracInJsonFormat(
            Mockito.mock(Crac.class),
            ProcessType.D2CC,
            OffsetDateTime.parse("2021-01-01T15:30Z")
        );

        MinioMock.MinioEntry minioEntry = minioMock.getEntry("CSE/EXPORT/D2CC/2021/01/01/16_30/ARTIFACT/crac.json");
        assertNotNull(minioEntry);
        assertEquals("crac.json", minioEntry.getFilename());
        assertEquals("", minioEntry.getFileType());
        assertEquals("CSE_EXPORT_D2CC", minioEntry.getProcessTag());
        assertEquals(GridcapaFileGroup.ARTIFACT, minioEntry.getGridcapaFileGroup());
    }

    @Test
    void testSaveCracInJsonFormatForIdccProcess() {
        fileExporter.saveCracInJsonFormat(
            Mockito.mock(Crac.class),
            ProcessType.IDCC,
            OffsetDateTime.parse("2021-01-01T15:30Z")
        );

        MinioMock.MinioEntry minioEntry = minioMock.getEntry("CSE/EXPORT/IDCC/2021/01/01/16_30/ARTIFACT/crac.json");
        assertNotNull(minioEntry);
        assertEquals("crac.json", minioEntry.getFilename());
        assertEquals("", minioEntry.getFileType());
        assertEquals("CSE_EXPORT_IDCC", minioEntry.getProcessTag());
        assertEquals(GridcapaFileGroup.ARTIFACT, minioEntry.getGridcapaFileGroup());
    }

    @Test
    void testSaveRaoParametersForD2ccProcess() {
        fileExporter.saveRaoParameters(
            ProcessType.D2CC,
            OffsetDateTime.parse("2021-01-01T15:30Z")
        );

        MinioMock.MinioEntry minioEntry = minioMock.getEntry("CSE/EXPORT/D2CC/2021/01/01/16_30/ARTIFACT/raoParameters.json");
        assertNotNull(minioEntry);
        assertEquals("raoParameters.json", minioEntry.getFilename());
        assertEquals("", minioEntry.getFileType());
        assertEquals("CSE_EXPORT_D2CC", minioEntry.getProcessTag());
        assertEquals(GridcapaFileGroup.ARTIFACT, minioEntry.getGridcapaFileGroup());
    }

    @Test
    void testSaveRaoParametersForIdccProcess() {
        fileExporter.saveRaoParameters(
            ProcessType.IDCC,
            OffsetDateTime.parse("2021-01-01T15:30Z")
        );

        MinioMock.MinioEntry minioEntry = minioMock.getEntry("CSE/EXPORT/IDCC/2021/01/01/16_30/ARTIFACT/raoParameters.json");
        assertNotNull(minioEntry);
        assertEquals("raoParameters.json", minioEntry.getFilename());
        assertEquals("", minioEntry.getFileType());
        assertEquals("CSE_EXPORT_IDCC", minioEntry.getProcessTag());
        assertEquals(GridcapaFileGroup.ARTIFACT, minioEntry.getGridcapaFileGroup());
    }

    @Test
    void testSaveTtcRaoForD2ccProcess() {
        fileExporter.saveTtcRao(
            Mockito.mock(CseRaoResult.class),
            ProcessType.D2CC,
            OffsetDateTime.parse("2021-01-01T15:30Z"),
            "20210101_1630_2D1_CO_Transit_CSE1.uct"
        );

        MinioMock.MinioEntry minioEntry = minioMock.getEntry("CSE/EXPORT/D2CC/2021/01/01/16_30/OUTPUT/TTC_Calculation_20210101_1630_2D0_CO_RAO_Transit_CSE1.xml");
        assertNotNull(minioEntry);
        assertEquals("TTC_Calculation_20210101_1630_2D0_CO_RAO_Transit_CSE1.xml", minioEntry.getFilename());
        assertEquals(processConfiguration.getTtcRao(), minioEntry.getFileType());
        assertEquals("CSE_EXPORT_D2CC", minioEntry.getProcessTag());
        assertEquals(GridcapaFileGroup.OUTPUT, minioEntry.getGridcapaFileGroup());
    }

    @Test
    void testSaveTtcRaoForIdccProcess() {
        fileExporter.saveTtcRao(
            Mockito.mock(CseRaoResult.class),
            ProcessType.IDCC,
            OffsetDateTime.parse("2021-01-01T15:30Z"),
            "20210101_1630_155_Transit_CSE0.uct"
        );

        MinioMock.MinioEntry minioEntry = minioMock.getEntry("CSE/EXPORT/IDCC/2021/01/01/16_30/OUTPUT/TTC_Calculation_20210101_1630_2D0_CO_RAO_Transit_CSE0.xml");
        assertNotNull(minioEntry);
        assertEquals("TTC_Calculation_20210101_1630_2D0_CO_RAO_Transit_CSE0.xml", minioEntry.getFilename());
        assertEquals(processConfiguration.getTtcRao(), minioEntry.getFileType());
        assertEquals("CSE_EXPORT_IDCC", minioEntry.getProcessTag());
        assertEquals(GridcapaFileGroup.OUTPUT, minioEntry.getGridcapaFileGroup());
    }
}
