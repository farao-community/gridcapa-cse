/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class FileExporterTest {

    @TestConfiguration
    public static class MockCombinedRasConfiguration {

        @Bean("combinedRasFilePath")
        @Primary
        public String getMockCombinedRasFilePath() {
            return getClass().getResource("/combinedRAs.json").getPath();
        }
    }

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
        String actualFilePath = fileExporter.getBaseCaseFilePath(OffsetDateTime.parse("2021-01-01T15:30Z"), ProcessType.IDCC, false);
        String expectedFilePath = "CSE/IMPORT/IDCC/2021/01/01/16_30/OUTPUTS/20210101_1630_155_Initial_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getBaseCaseFilePathForD2ccProcesTest() {
        String actualFilePath = fileExporter.getBaseCaseFilePath(OffsetDateTime.parse("2021-01-01T12:30Z"), ProcessType.D2CC, false);
        String expectedFilePath = "CSE/IMPORT/D2CC/2021/01/01/13_30/OUTPUTS/20210101_1330_2D5_CO_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getRaoParametersTest() {
        RaoParameters raoParameters = fileExporter.getRaoParameters(Collections.emptyList());
        assertEquals(1, raoParameters.getExtension(SearchTreeRaoParameters.class).getNetworkActionIdCombinations().size());
        List<String> raCombination = raoParameters.getExtension(SearchTreeRaoParameters.class).getNetworkActionIdCombinations().get(0);
        Map<String, Integer> maxCurativeRaPerTso = raoParameters.getExtension(SearchTreeRaoParameters.class).getMaxCurativeRaPerTso();
        assertEquals(2, raCombination.size());
        assertTrue(raCombination.contains("PRA_2N_VALPELLINE"));
        assertTrue(raCombination.contains("PRA_2N_AVISE"));
        assertEquals(5, maxCurativeRaPerTso.size());
        assertEquals(1, maxCurativeRaPerTso.get("IT"));
        assertEquals(5, maxCurativeRaPerTso.get("FR"));
        assertEquals(0, maxCurativeRaPerTso.get("CH"));
        assertEquals(3, maxCurativeRaPerTso.get("SI"));
        assertEquals(3, maxCurativeRaPerTso.get("AT"));
    }

    @Test
    void getRaoParametersTestWhenRemedialActionsAppliedInPreviousStepIsNotEmpty() {
        List<String> networkActionIds = List.of("PRA_XX", "PRA_YY");
        RaoParameters raoParameters = fileExporter.getRaoParameters(networkActionIds);
        assertEquals(2, raoParameters.getExtension(SearchTreeRaoParameters.class).getNetworkActionIdCombinations().size());
    }

    @Test
    void getRaoParametersFilePathTest() {
        String expectedFilePathWhenBasePathIsEmptyOrNull = "CSE/IMPORT/IDCC/2021/09/14/01_30/ARTIFACTS/raoParameters.json";
        String expectedFilePathWhenBasePathIsNotEmpty = "FAKE_PATH/raoParameters.json";
        assertEquals(expectedFilePathWhenBasePathIsEmptyOrNull, fileExporter.getRaoParametersDestinationPath("", ProcessType.IDCC, OffsetDateTime.parse("2021-09-13T23:30Z"), false));
        assertEquals(expectedFilePathWhenBasePathIsEmptyOrNull, fileExporter.getRaoParametersDestinationPath(null, ProcessType.IDCC, OffsetDateTime.parse("2021-09-13T23:30Z"), false));
        assertEquals(expectedFilePathWhenBasePathIsNotEmpty, fileExporter.getRaoParametersDestinationPath("FAKE_PATH/", ProcessType.IDCC, OffsetDateTime.parse("2021-09-13T23:30Z"), false));
    }

    //
    @Test
    void getTTcFilePathForD2ccAdaptedProcessTest() {
        String actualFilePath = fileExporter.getFilePath(OffsetDateTime.parse("2021-09-13T12:30Z"), ProcessType.D2CC, true);
        String expectedFilePath = "CSE/IMPORT-ADAPTED/D2CC/2021/09/13/14_30/OUTPUTS/TTC_Calculation_20210913_1430_2D0_CO_CSE1.xml";
        assertEquals(expectedFilePath, actualFilePath);
        FilenameUtils.getPathNoEndSeparator("CSE/D2CC/2021/");
    }

    @Test
    void getTTcFilePathForIdccAdaptedProcessTest() {
        String actualFilePath = fileExporter.getFilePath(OffsetDateTime.parse("2021-09-13T23:30Z"), ProcessType.IDCC, true);
        String expectedFilePath = "CSE/IMPORT-ADAPTED/IDCC/2021/09/14/01_30/OUTPUTS/20210914_XBID2_TTCRes_CSE1.xml";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getFinalNetworkFilePathForD2ccAdaptedProcesTest() {
        String actualFilePath = fileExporter.getFinalNetworkFilePath(OffsetDateTime.parse("2021-09-13T12:30Z"), ProcessType.D2CC, true);
        String expectedFilePath = "CSE/IMPORT-ADAPTED/D2CC/2021/09/13/14_30/OUTPUTS/20210913_1430_2D1_CO_Final_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getFinalNetworkFilePathForIdccAdaptedProcesTest() {
        String actualFilePath = fileExporter.getFinalNetworkFilePath(OffsetDateTime.parse("2021-09-01T17:30Z"), ProcessType.IDCC, true);
        String expectedFilePath = "CSE/IMPORT-ADAPTED/IDCC/2021/09/01/19_30/OUTPUTS/20210901_1930_173_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getBaseCaseFilePathForIdccAdaptedProcesTest() {
        String actualFilePath = fileExporter.getBaseCaseFilePath(OffsetDateTime.parse("2021-01-01T15:30Z"), ProcessType.IDCC, true);
        String expectedFilePath = "CSE/IMPORT-ADAPTED/IDCC/2021/01/01/16_30/OUTPUTS/20210101_1630_155_Initial_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getBaseCaseFilePathForD2ccAdaptedProcesTest() {
        String actualFilePath = fileExporter.getBaseCaseFilePath(OffsetDateTime.parse("2021-01-01T12:30Z"), ProcessType.D2CC, true);
        String expectedFilePath = "CSE/IMPORT-ADAPTED/D2CC/2021/01/01/13_30/OUTPUTS/20210101_1330_2D5_CO_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getRaoParametersFileAdaptedPathTest() {
        String expectedFilePathWhenBasePathIsEmptyOrNull = "CSE/IMPORT-ADAPTED/IDCC/2021/09/14/01_30/ARTIFACTS/raoParameters.json";
        String expectedFilePathWhenBasePathIsNotEmpty = "FAKE_PATH/raoParameters.json";
        assertEquals(expectedFilePathWhenBasePathIsEmptyOrNull, fileExporter.getRaoParametersDestinationPath("", ProcessType.IDCC, OffsetDateTime.parse("2021-09-13T23:30Z"), true));
        assertEquals(expectedFilePathWhenBasePathIsEmptyOrNull, fileExporter.getRaoParametersDestinationPath(null, ProcessType.IDCC, OffsetDateTime.parse("2021-09-13T23:30Z"), true));
        assertEquals(expectedFilePathWhenBasePathIsNotEmpty, fileExporter.getRaoParametersDestinationPath("FAKE_PATH/", ProcessType.IDCC, OffsetDateTime.parse("2021-09-13T23:30Z"), true));
    }
}
