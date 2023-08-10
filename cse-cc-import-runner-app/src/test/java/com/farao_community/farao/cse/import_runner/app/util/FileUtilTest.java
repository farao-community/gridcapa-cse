/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.util;

import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.import_runner.app.dichotomy.CseD2ccShiftDispatcher;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.cse.CseGlskDocumentImporter;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import groovy.util.logging.Log4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class FileUtilTest {

    @Test
    void testGetFilename() {
        assertEquals("test_file.xml", FileUtil.getFilenameFromUrl("file://fake_folder/test_file.xml?variableId=4"));
        assertEquals("20210901_2230_test_network.uct", FileUtil.getFilenameFromUrl("http://minio:9000/gridcapa/CSE/D2CC/IDCC-1/20210901_2230_test_network.uct?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=gridcapa%2F20211223%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20211223T092947Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=4d24c286f490fcb18eb078cf21c1503d5e4eb557337469dda3c86d7b9998bf09"));
    }


    @Test
    void testWalid() {
        String testDirectory = "/testWalid/";
        String networkFileName = "20230719_1330_113_CSE1.uct";
        Network network = Network.read(getClass().getResource(testDirectory + networkFileName).getPath());
        GlskDocument glskDocument = null;
        try {
            InputStream inputStream = new FileInputStream(getClass().getResource(testDirectory + "20230719_1330_113_CO_GSK_CSE1.xml").getPath());
            glskDocument = new CseGlskDocumentImporter().importGlsk(inputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Map<Country, Double> exports = BorderExchanges.computeExports(network);
        ZonalData<Scalable> scalableZonalData = glskDocument.getZonalScalable(network);
        Logger logger = Mockito.mock(Logger.class);
        ShiftDispatcher shiftDispatcher = new CseD2ccShiftDispatcher(logger,
            Map.of(
            "10YFR-RTE------C", 0.407642,
            "10YCH-SWISSGRIDZ", 0.493676,
            "10YIT-GRTN-----B", -1.0,
            "10YSI-ELES-----O", 0.070356,
            "10YAT-APG------L", 0.028327),
            Map.of(
                "10YFR-RTE------C", 2497.7567649572125,
                "10YCH-SWISSGRIDZ", 4156.703573152423,
                "10YSI-ELES-----O", 745.1815822721377,
                "10YAT-APG------L", 195.07532837545722));
        LinearScaler linearScaler = new LinearScaler(scalableZonalData, shiftDispatcher);
        try {
            linearScaler.shiftNetwork(8097, network);
            Map<Country, Double> exportsFinal = BorderExchanges.computeExports(network);
            System.out.println("zz");
        } catch (GlskLimitationException e) {
            throw new RuntimeException(e);
        } catch (ShiftingException e) {
            throw new RuntimeException(e);
        }

        String requestId = "Test request";
        OffsetDateTime dateTime = OffsetDateTime.parse("2023-07-03T10:30Z");




    }
}
