/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.powsybl.openrao.data.cracimpl.CracImpl;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class FileExporterTest {

    @MockBean
    MinioAdapter minioAdapter;

    @Autowired
    FileExporter fileExporter;

    @Test
    void getTTcFilePathForD2ccProcessTest() {
        String actualFilePath = fileExporter.getFilePath(OffsetDateTime.parse("2021-09-13T12:30Z"), ProcessType.D2CC, false);
        String expectedFilePath = "CSE/IMPORT/D2CC/2021/09/13/14_30/OUTPUTS/TTC_Calculation_20210913_1430_2D0_CO_CSE1.xml";
        assertEquals(expectedFilePath, actualFilePath);
        FilenameUtils.getPathNoEndSeparator("CSE/D2CC/2021/");
    }

    @Test
    void getTTcFilePathForIdccProcessTest() {
        String actualFilePath = fileExporter.getFilePath(OffsetDateTime.parse("2021-09-13T23:30Z"), ProcessType.IDCC, false);
        String expectedFilePath = "CSE/IMPORT/IDCC/2021/09/14/01_30/OUTPUTS/20210914_XBID2_TTCRes_CSE1.xml";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getFinalNetworkFilePathForD2ccProcesTest() {
        String actualFilePath = fileExporter.getFinalNetworkFilePath(OffsetDateTime.parse("2021-09-13T12:30Z"), ProcessType.D2CC, false);
        String expectedFilePath = "CSE/IMPORT/D2CC/2021/09/13/14_30/OUTPUTS/20210913_1430_2D1_CO_Final_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getFinalNetworkFilePathForIdccProcesTest() {
        String actualFilePath = fileExporter.getFinalNetworkFilePath(OffsetDateTime.parse("2021-09-01T17:30Z"), ProcessType.IDCC, false);
        String expectedFilePath = "CSE/IMPORT/IDCC/2021/09/01/19_30/OUTPUTS/20210901_1930_173_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getBaseCaseFilePathForIdccProcesTest() {
        String actualFilePath = fileExporter.getFirstShiftNetworkPath(OffsetDateTime.parse("2021-01-01T15:30Z"), ProcessType.IDCC, false);
        String expectedFilePath = "CSE/IMPORT/IDCC/2021/01/01/16_30/OUTPUTS/20210101_1630_155_Initial_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getBaseCaseFilePathForD2ccProcesTest() {
        String actualFilePath = fileExporter.getFirstShiftNetworkPath(OffsetDateTime.parse("2021-01-01T12:30Z"), ProcessType.D2CC, false);
        String expectedFilePath = "CSE/IMPORT/D2CC/2021/01/01/13_30/OUTPUTS/20210101_1330_2D5_CO_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getRaoParametersTest() {
        RaoParameters raoParameters = fileExporter.getRaoParameters(Collections.emptyList());
        assertEquals(1, raoParameters.getTopoOptimizationParameters().getPredefinedCombinations().size());
        List<String> raCombination = raoParameters.getTopoOptimizationParameters().getPredefinedCombinations().get(0);
        assertEquals(2, raCombination.size());
        assertTrue(raCombination.contains("PRA_2N_VALPELLINE"));
        assertTrue(raCombination.contains("PRA_2N_AVISE"));
    }

    @Test
    void getRaoParametersTestWhenRemedialActionsAppliedInPreviousStepIsNotEmpty() {
        List<String> networkActionIds = List.of("PRA_XX", "PRA_YY");
        RaoParameters raoParameters = fileExporter.getRaoParameters(networkActionIds);
        assertEquals(2, raoParameters.getTopoOptimizationParameters().getPredefinedCombinations().size());
    }

    @Test
    void getRaoParametersFilePathTest() {
        String expectedFilePathWhenBasePathIsEmptyOrNull = "CSE/IMPORT/IDCC/2021/09/14/01_30/ARTIFACTS/raoParameters.json";
        String expectedFilePathWhenBasePathIsNotEmpty = "FAKE_PATH/raoParameters.json";
        assertEquals(expectedFilePathWhenBasePathIsEmptyOrNull, fileExporter.getRaoParametersDestinationPath("", ProcessType.IDCC, OffsetDateTime.parse("2021-09-13T23:30Z"), false));
        assertEquals(expectedFilePathWhenBasePathIsEmptyOrNull, fileExporter.getRaoParametersDestinationPath(null, ProcessType.IDCC, OffsetDateTime.parse("2021-09-13T23:30Z"), false));
        assertEquals(expectedFilePathWhenBasePathIsNotEmpty, fileExporter.getRaoParametersDestinationPath("FAKE_PATH/", ProcessType.IDCC, OffsetDateTime.parse("2021-09-13T23:30Z"), false));
    }

    @Test
    void getTTcFilePathForD2ccAdaptedProcessTest() {
        String actualFilePath = fileExporter.getFilePath(OffsetDateTime.parse("2021-09-13T12:30Z"), ProcessType.D2CC, true);
        String expectedFilePath = "CSE/IMPORT_EC/D2CC/2021/09/13/14_30/OUTPUTS/TTC_Calculation_20210913_1430_2D0_CO_CSE1.xml";
        assertEquals(expectedFilePath, actualFilePath);
        FilenameUtils.getPathNoEndSeparator("CSE/D2CC/2021/");
    }

    @Test
    void getTTcFilePathForIdccAdaptedProcessTest() {
        String actualFilePath = fileExporter.getFilePath(OffsetDateTime.parse("2021-09-13T23:30Z"), ProcessType.IDCC, true);
        String expectedFilePath = "CSE/IMPORT_EC/IDCC/2021/09/14/01_30/OUTPUTS/20210914_XBID2_TTCRes_CSE1.xml";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getFinalNetworkFilePathForD2ccAdaptedProcesTest() {
        String actualFilePath = fileExporter.getFinalNetworkFilePath(OffsetDateTime.parse("2021-09-13T12:30Z"), ProcessType.D2CC, true);
        String expectedFilePath = "CSE/IMPORT_EC/D2CC/2021/09/13/14_30/OUTPUTS/20210913_1430_2D1_CO_Final_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getFinalNetworkFilePathForIdccAdaptedProcesTest() {
        String actualFilePath = fileExporter.getFinalNetworkFilePath(OffsetDateTime.parse("2021-09-01T17:30Z"), ProcessType.IDCC, true);
        String expectedFilePath = "CSE/IMPORT_EC/IDCC/2021/09/01/19_30/OUTPUTS/20210901_1930_173_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getBaseCaseFilePathForIdccAdaptedProcesTest() {
        String actualFilePath = fileExporter.getFirstShiftNetworkPath(OffsetDateTime.parse("2021-01-01T15:30Z"), ProcessType.IDCC, true);
        String expectedFilePath = "CSE/IMPORT_EC/IDCC/2021/01/01/16_30/OUTPUTS/20210101_1630_155_Initial_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getBaseCaseFilePathForD2ccAdaptedProcesTest() {
        String actualFilePath = fileExporter.getFirstShiftNetworkPath(OffsetDateTime.parse("2021-01-01T12:30Z"), ProcessType.D2CC, true);
        String expectedFilePath = "CSE/IMPORT_EC/D2CC/2021/01/01/13_30/OUTPUTS/20210101_1330_2D5_CO_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getRaoParametersFileAdaptedPathTest() {
        String expectedFilePathWhenBasePathIsEmptyOrNull = "CSE/IMPORT_EC/IDCC/2021/09/14/01_30/ARTIFACTS/raoParameters.json";
        String expectedFilePathWhenBasePathIsNotEmpty = "FAKE_PATH/raoParameters.json";
        assertEquals(expectedFilePathWhenBasePathIsEmptyOrNull, fileExporter.getRaoParametersDestinationPath("", ProcessType.IDCC, OffsetDateTime.parse("2021-09-13T23:30Z"), true));
        assertEquals(expectedFilePathWhenBasePathIsEmptyOrNull, fileExporter.getRaoParametersDestinationPath(null, ProcessType.IDCC, OffsetDateTime.parse("2021-09-13T23:30Z"), true));
        assertEquals(expectedFilePathWhenBasePathIsNotEmpty, fileExporter.getRaoParametersDestinationPath("FAKE_PATH/", ProcessType.IDCC, OffsetDateTime.parse("2021-09-13T23:30Z"), true));
    }

    @Test
    void saveCracInJsonFormatTest() {
        String expectedCracFilePath = "CSE/IMPORT_EC/IDCC/1999/01/01/13_30/ARTIFACTS/crac.json";
        Mockito.when(minioAdapter.generatePreSignedUrl(expectedCracFilePath)).thenReturn("SUCCESS");
        String result = fileExporter.saveCracInJsonFormat(new CracImpl("testCrac"), OffsetDateTime.parse("1999-01-01T12:30Z"), ProcessType.IDCC, true);
        assertNotNull(result);
    }

    @Test
    void saveCracInJsonFormatNotAdaptedTest() {
        String expectedCracFilePath = "CSE/IMPORT/D2CC/1999/01/01/13_30/ARTIFACTS/crac.json";
        Mockito.when(minioAdapter.generatePreSignedUrl(expectedCracFilePath)).thenReturn("SUCCESS");
        String result = fileExporter.saveCracInJsonFormat(new CracImpl("testCrac"), OffsetDateTime.parse("1999-01-01T12:30Z"), ProcessType.D2CC, false);
        assertNotNull(result);
    }

    @Test
    void saveNetworkInArtifactTest() {
        Network network = NetworkFactory.findDefault().createNetwork("test", "TEST");
        String expectedCracFilePath = "CSE/IMPORT_EC/D2CC/1999/01/01/13_30/ARTIFACTS/network_pre_processed.xiidm";
        Mockito.when(minioAdapter.generatePreSignedUrl(expectedCracFilePath)).thenReturn("SUCCESS");
        String result = fileExporter.saveNetworkInArtifact(network, OffsetDateTime.parse("1999-01-01T12:30Z"), "", ProcessType.D2CC, true);
        assertNotNull(result);
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadArtifactForTimestamp(Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyString(), Mockito.anyString(), Mockito.any(OffsetDateTime.class));
    }

    @Test
    void saveRaoParametersTest() {
        String raoParametersDestinationPath = "CSE/IMPORT_EC/IDCC/1999/01/01/13_30/ARTIFACTS/raoParameters.json";
        Mockito.when(minioAdapter.generatePreSignedUrl(raoParametersDestinationPath)).thenReturn("SUCCESS");
        String result = fileExporter.saveRaoParameters(OffsetDateTime.parse("1999-01-01T12:30Z"), ProcessType.IDCC, true);
        assertNotNull(result);
    }

    @Test
    void saveTtcResultD2ccTest() {
        String ttcFilePath = "CSE/IMPORT_EC/D2CC/1999/01/01/13_30/OUTPUTS/TTC_Calculation_19990101_1330_2D0_CO_CSE1.xml";
        Mockito.when(minioAdapter.generatePreSignedUrl(ttcFilePath)).thenReturn("SUCCESS");
        String result = fileExporter.saveTtcResult(new Timestamp(), OffsetDateTime.parse("1999-01-01T12:30Z"), ProcessType.D2CC, true);
        assertNotNull(result);
    }

    @Test
    void saveTtcResultIdccTest() {
        String ttcFilePath = "CSE/IMPORT_EC/IDCC/1999/12/31/13_30/OUTPUTS/19991231_XBID2_TTCRes_CSE1.xml";
        Mockito.when(minioAdapter.generatePreSignedUrl(ttcFilePath)).thenReturn("SUCCESS");
        String result = fileExporter.saveTtcResult(new Timestamp(), OffsetDateTime.parse("1999-12-31T12:30Z"), ProcessType.IDCC, true);
        assertNotNull(result);
    }

    @Test
    void exportAndUploadNetworkAsOutputTest() {
        Network network = NetworkFactory.findDefault().createNetwork("test", "TEST");
        String expectedCracFilePath = "CSE/IMPORT_EC/D2CC/1999/01/01/13_30/ARTIFACTS/network_pre_processed.xiidm";
        Mockito.when(minioAdapter.generatePreSignedUrl(expectedCracFilePath)).thenReturn("SUCCESS");
        String result = fileExporter.exportAndUploadNetwork(network, "XIIDM", GridcapaFileGroup.OUTPUT, expectedCracFilePath, "", OffsetDateTime.parse("1999-01-01T12:30Z"), ProcessType.D2CC, true);
        assertNotNull(result);
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyString(), Mockito.anyString(), Mockito.any(OffsetDateTime.class));
    }
}
