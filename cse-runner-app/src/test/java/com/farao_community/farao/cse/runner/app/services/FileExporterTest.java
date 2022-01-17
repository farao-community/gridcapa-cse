/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class FileExporterTest {

    @Autowired
    FileExporter fileExporter;

    @Test
    void getTTcFilePathForD2ccProcessTest() {
        String actualFilePath = fileExporter.getFilePath(OffsetDateTime.parse("2021-09-13T12:30Z"), ProcessType.D2CC);
        String expectedFilePath = "outputs/TTC_Calculation_20210913_1430_2D0_CO_CSE1.xml";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getTTcFilePathForIdccProcessTest() {
        String actualFilePath = fileExporter.getFilePath(OffsetDateTime.parse("2021-09-13T23:30Z"), ProcessType.IDCC);
        String expectedFilePath = "outputs/20210914_XBID2_TTCRes_CSE1.xml";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getFinalNetworkFilePathForD2ccProcesTest() {
        String actualFilePath = fileExporter.getFinalNetworkFilePath(OffsetDateTime.parse("2021-09-13T12:30Z"), ProcessType.D2CC);
        String expectedFilePath = "outputs/20210913_1430_2D1_CO_Final_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }

    @Test
    void getFinalNetworkFilePathForIdccProcesTest() {
        String actualFilePath = fileExporter.getFinalNetworkFilePath(OffsetDateTime.parse("2021-09-13T23:30Z"), ProcessType.IDCC);
        String expectedFilePath = "outputs/20210914_0130_122_CSE1.uct";
        assertEquals(expectedFilePath, actualFilePath);
    }
}
