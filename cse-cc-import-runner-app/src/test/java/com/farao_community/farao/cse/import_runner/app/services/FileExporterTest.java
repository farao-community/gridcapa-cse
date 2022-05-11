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
import java.util.List;

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
        String actualFilePath = fileExporter.getFilePath(OffsetDateTime.parse("2021-09-13T12:30Z"), ProcessType.D2CC);
        String expectedFilePath = "CSE/D2CC/2021/09/13/14_30/OUTPUTS/TTC_Calculation_20210913_1430_2D0_CO_CSE1.xml";
        assertEquals(expectedFilePath, actualFilePath);
        FilenameUtils.getPathNoEndSeparator("CSE/D2CC/2021/");
    }

    @Test
    void getTTcFilePathForIdccProcessTest() {
        String actualFilePath = fileExporter.getFilePath(OffsetDateTime.parse("2021-09-13T23:30Z"), ProcessType.IDCC);
        String expectedFilePath = "CSE/IDCC/2021/09/14/01_30/OUTPUTS/20210914_XBID2_TTCRes_CSE1.xml";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getFinalNetworkFilePathForD2ccProcesTest() {
        String actualFilePath = fileExporter.getFinalNetworkFilePath(OffsetDateTime.parse("2021-09-13T12:30Z"), ProcessType.D2CC);
        String expectedFilePath = "CSE/D2CC/2021/09/13/14_30/OUTPUTS/20210913_1430_2D1_CO_Final_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getFinalNetworkFilePathForIdccProcesTest() {
        String actualFilePath = fileExporter.getFinalNetworkFilePath(OffsetDateTime.parse("2021-09-01T17:30Z"), ProcessType.IDCC);
        String expectedFilePath = "CSE/IDCC/2021/09/01/19_30/OUTPUTS/20210901_1930_173_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getBaseCaseFilePathForIdccProcesTest() {
        String actualFilePath = fileExporter.getBaseCaseFilePath(OffsetDateTime.parse("2021-01-01T15:30Z"), ProcessType.IDCC);
        String expectedFilePath = "CSE/IDCC/2021/01/01/16_30/OUTPUTS/20210101_1630_155_Initial_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getBaseCaseFilePathForD2ccProcesTest() {
        String actualFilePath = fileExporter.getBaseCaseFilePath(OffsetDateTime.parse("2021-01-01T12:30Z"), ProcessType.D2CC);
        String expectedFilePath = "CSE/D2CC/2021/01/01/13_30/OUTPUTS/20210101_1330_2D5_CO_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getRaoParametersTest() {
        RaoParameters raoParameters = fileExporter.getRaoParameters();
        assertEquals(1, raoParameters.getExtension(SearchTreeRaoParameters.class).getNetworkActionIdCombinations().size());
        List<String> raCombination = raoParameters.getExtension(SearchTreeRaoParameters.class).getNetworkActionIdCombinations().get(0);
        assertEquals(2, raCombination.size());
        assertTrue(raCombination.contains("PRA_2N_VALPELLINE"));
        assertTrue(raCombination.contains("PRA_2N_AVISE"));
    }
}
