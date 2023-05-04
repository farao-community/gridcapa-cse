/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.ntc.Ntc;
import com.farao_community.farao.cse.data.ntc2.Ntc2;
import com.farao_community.farao.cse.data.target_ch.LineFixedFlows;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class FileImporterTest {

    @Autowired
    private FileImporter fileImporter;

    @Test
    void testCseCracImport() throws IOException {
        CseCrac cseCrac = fileImporter.importCseCrac(Objects.requireNonNull(getClass().getResource("20210901_2230_213_CRAC_CO_CSE1.xml")).toString());
        assertEquals(2, cseCrac.getCracDocument().getCRACSeries().get(0).getCriticalBranches().getBaseCaseBranches().getBranch().size());
    }

    @Test
    void testTargetChImport() {
        LineFixedFlows lineFixedFlows = fileImporter.importLineFixedFlowFromTargetChFile(
            OffsetDateTime.parse("2021-01-01T00:00Z"),
            Objects.requireNonNull(getClass().getResource("20210901_2230_213_CRAC_CO_CSE1.xml")).toString(),
                false);
        assertNotNull(lineFixedFlows);
    }

    @Test
    void testImportNtcAdapted() {
        Ntc ntc = fileImporter.importNtc(
                OffsetDateTime.parse("2021-01-01T00:00Z"),
                Objects.requireNonNull(getClass().getResource("20210901_2230_213_CRAC_CO_CSE1.xml")).toString(),
                Objects.requireNonNull(getClass().getResource("20210901_2230_213_CRAC_CO_CSE1.xml")).toString(),
                true
        );
        assertNotNull(ntc);
    }

    @Test
    void testImportNtc() {
        Ntc ntc = fileImporter.importNtc(
                OffsetDateTime.parse("2021-01-01T00:00Z"),
                Objects.requireNonNull(getClass().getResource("20210901_2230_213_CRAC_CO_CSE1.xml")).toString(),
                Objects.requireNonNull(getClass().getResource("20210901_2230_213_CRAC_CO_CSE1.xml")).toString(),
                false
        );
        assertNotNull(ntc);
    }

    @Test
    void testImportNtc2AllAbsent() {
        Ntc2 ntc2 = fileImporter.importNtc2(OffsetDateTime.parse("2021-06-23T00:00Z"), null, null, "", "   ");
        assertNotNull(ntc2);
        assertNotNull(ntc2.getExchanges());
        assertTrue(ntc2.getExchanges().isEmpty());

    }

    @Test
    void testImportNtc2OnlyAtPresent() {
        Ntc2 ntc2 = fileImporter.importNtc2(OffsetDateTime.parse("2021-06-24T00:00Z"), getClass().getResource("NTC2_20210624_2D5_AT-IT1-test.xml").toString(), null, "", "   ");
        assertNotNull(ntc2);
        assertNotNull(ntc2.getExchanges());
        assertEquals(1, ntc2.getExchanges().size());
        assertNotNull(ntc2.getExchanges().get("10YAT-APG------L"));
        assertEquals(1024, ntc2.getExchanges().get("10YAT-APG------L"));
    }

    @Test
    void testImportNtc2OnlyChPresent() {
        Ntc2 ntc2 = fileImporter.importNtc2(OffsetDateTime.parse("2021-06-24T00:00Z"), null, getClass().getResource("NTC2_20210624_2D5_CH-IT1-test.xml").toString(), "", "   ");
        assertNotNull(ntc2);
        assertNotNull(ntc2.getExchanges());
        assertEquals(1, ntc2.getExchanges().size());
        assertNotNull(ntc2.getExchanges().get("10YCH-SWISSGRIDZ"));
        assertEquals(2048, ntc2.getExchanges().get("10YCH-SWISSGRIDZ"));
    }

    @Test
    void testImportNtc2OnlyFrPresent() {
        Ntc2 ntc2 = fileImporter.importNtc2(OffsetDateTime.parse("2021-06-24T00:00Z"), null, "", getClass().getResource("NTC2_20210624_2D5_FR-IT1-test.xml").toString(), "   ");
        assertNotNull(ntc2);
        assertNotNull(ntc2.getExchanges());
        assertEquals(1, ntc2.getExchanges().size());
        assertNotNull(ntc2.getExchanges().get("10YFR-RTE------C"));
        assertEquals(4096, ntc2.getExchanges().get("10YFR-RTE------C"));
    }

    @Test
    void testImportNtc2OnlySiPresent() {
        Ntc2 ntc2 = fileImporter.importNtc2(OffsetDateTime.parse("2021-06-24T00:00Z"), null, "", "   ", getClass().getResource("NTC2_20210624_2D5_SI-IT1-test.xml").toString());
        assertNotNull(ntc2);
        assertNotNull(ntc2.getExchanges());
        assertEquals(1, ntc2.getExchanges().size());
        assertNotNull(ntc2.getExchanges().get("10YSI-ELES-----O"));
        assertEquals(8192, ntc2.getExchanges().get("10YSI-ELES-----O"));
    }

    @Test
    void testImportNtc2AllPresent() {
        Ntc2 ntc2 = fileImporter.importNtc2(OffsetDateTime.parse("2021-06-24T00:00Z"),
                getClass().getResource("NTC2_20210624_2D5_AT-IT1-test.xml").toString(),
                getClass().getResource("NTC2_20210624_2D5_CH-IT1-test.xml").toString(),
                getClass().getResource("NTC2_20210624_2D5_FR-IT1-test.xml").toString(),
                getClass().getResource("NTC2_20210624_2D5_SI-IT1-test.xml").toString());
        assertNotNull(ntc2);
        assertNotNull(ntc2.getExchanges());
        assertEquals(4, ntc2.getExchanges().size());
        assertNotNull(ntc2.getExchanges().get("10YAT-APG------L"));
        assertEquals(1024, ntc2.getExchanges().get("10YAT-APG------L"));
        assertNotNull(ntc2.getExchanges().get("10YCH-SWISSGRIDZ"));
        assertEquals(2048, ntc2.getExchanges().get("10YCH-SWISSGRIDZ"));
        assertNotNull(ntc2.getExchanges().get("10YFR-RTE------C"));
        assertEquals(4096, ntc2.getExchanges().get("10YFR-RTE------C"));
        assertNotNull(ntc2.getExchanges().get("10YSI-ELES-----O"));
        assertEquals(8192, ntc2.getExchanges().get("10YSI-ELES-----O"));
    }

    @Test
    void assertThrowsWhenDataIsMissing() {
        OffsetDateTime time = OffsetDateTime.parse("2021-06-23T22:00Z");
        String atFileUrl = getClass().getResource("NTC2_20210624_2D5_AT-IT1-test.xml").toString();
        String chFileUrl = getClass().getResource("NTC2_20210624_2D5_CH-IT1-test.xml").toString();
        String frFileUrl = getClass().getResource("NTC2_20210624_2D5_FR-IT1-test.xml").toString();
        String siFileUrl = getClass().getResource("NTC2_20210624_2D5_SI-IT1-test.xml").toString();
        assertThrows(CseDataException.class, () ->  fileImporter.importNtc2(time, atFileUrl, chFileUrl, frFileUrl, siFileUrl));
    }

    @Test
    void assertThrowsWhenTargetDateTimeOutOfBound() {
        OffsetDateTime time = OffsetDateTime.parse("2021-06-24T23:00Z");
        String atFileUrl = getClass().getResource("NTC2_20210624_2D5_AT-IT1-test.xml").toString();
        String chFileUrl = getClass().getResource("NTC2_20210624_2D5_CH-IT1-test.xml").toString();
        String frFileUrl = getClass().getResource("NTC2_20210624_2D5_FR-IT1-test.xml").toString();
        String siFileUrl = getClass().getResource("NTC2_20210624_2D5_SI-IT1-test.xml").toString();
        assertThrows(CseDataException.class, () ->  fileImporter.importNtc2(time, atFileUrl, chFileUrl, frFileUrl, siFileUrl));
    }

    @Test
    void testImportFailsWithMissingPositions() {
        OffsetDateTime time = OffsetDateTime.parse("2021-06-24T15:00Z");
        String atFileUrl = getClass().getResource("NTC2_20210624_2D5_AT-IT1-test.xml").toString();
        String chFileUrl = getClass().getResource("NTC2_20210624_2D5_CH-IT1-test.xml").toString();
        String frFileUrl = getClass().getResource("NTC2_20210624_2D5_FR-IT1-test.xml").toString();
        String siFileUrl = getClass().getResource("NTC2_20210624_2D5_SI-IT1-test.xml").toString();
        Throwable e =  assertThrows(CseDataException.class, () ->  fileImporter.importNtc2(time, atFileUrl, chFileUrl, frFileUrl, siFileUrl));
        assertEquals("Impossible to import NTC2 file for area: 10YAT-APG------L", e.getMessage());
        Throwable nestedE = e.getCause();
        assertEquals("Impossible to retrieve NTC2 position 18. It does not exist", nestedE.getMessage());
    }

    @Test
    void testImportNtcAdaptedWithMissingNtcRed() {
        Ntc ntc = fileImporter.importNtc(
                OffsetDateTime.parse("2021-01-01T00:00Z"),
                Objects.requireNonNull(getClass().getResource("2021_2Dp_NTC_annual_CSE1_Adapted_v8_8.xml")).toString(),
                null,
                true
        );
        assertNotNull(ntc);
    }

    @Test
    void testImportNtcWithMissingNtcRed() {
        assertThrows(NullPointerException.class, () -> fileImporter.importNtc(
                OffsetDateTime.parse("2021-01-01T00:00Z"),
                null,
                "",
                false
        ));
    }
}
