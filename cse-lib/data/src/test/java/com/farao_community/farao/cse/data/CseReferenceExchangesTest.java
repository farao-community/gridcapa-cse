/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data;

import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class CseReferenceExchangesTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private CseReferenceExchanges cseReferenceExchanges;

    @Test
    void creationThrowsExceptionWhenMinutesNotAMultipleOf15() {
        assertThrows(
            CseDataException.class,
            () -> cseReferenceExchanges = CseReferenceExchanges.fromVulcanusFile(
                OffsetDateTime.parse("2019-12-28T14:05Z"),
                getClass().getResourceAsStream("vulcanus_28122019_96.xls"),
                    "vulcanus_28122019_96.xls",
                ProcessType.IDCC
            )
        );
    }

    @Test
    void creationThrowsExceptionWhenTargetDateTimeDifferentFromFileDate() {
        assertThrows(
            CseDataException.class,
            () -> cseReferenceExchanges = CseReferenceExchanges.fromVulcanusFile(
                OffsetDateTime.parse("2019-12-29T14:30Z"),
                getClass().getResourceAsStream("vulcanus_28122019_96.xls"),
                    "vulcanus_28122019_96.xls",
                ProcessType.IDCC
            )
        );
    }

    @Test
    void getExchangedFlow1() throws IOException {
        cseReferenceExchanges = CseReferenceExchanges.fromVulcanusFile(
            OffsetDateTime.parse("2019-12-28T14:30Z"),
            getClass().getResourceAsStream("vulcanus_28122019_96.xls"),
                "vulcanus_28122019_96.xls",
            ProcessType.IDCC
        );
        assertEquals(4, cseReferenceExchanges.getExchanges().size());
        assertEquals(633, cseReferenceExchanges.getExchange(Country.CH), DOUBLE_TOLERANCE);
        assertEquals(3021, cseReferenceExchanges.getExchange(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(0, cseReferenceExchanges.getExchange(Country.AT), DOUBLE_TOLERANCE);
        assertEquals(-44, cseReferenceExchanges.getExchange(Country.SI), DOUBLE_TOLERANCE);
    }

    @Test
    void getExchangedFlow2() throws IOException {
        cseReferenceExchanges = CseReferenceExchanges.fromVulcanusFile(
            OffsetDateTime.parse("2019-12-28T07:00Z"),
            getClass().getResourceAsStream("vulcanus_28122019_96.xls"),
                "vulcanus_28122019_96.xls",
            ProcessType.IDCC
        );
        assertEquals(4, cseReferenceExchanges.getExchanges().size());
        assertEquals(2129, cseReferenceExchanges.getExchange(Country.CH), DOUBLE_TOLERANCE);
        assertEquals(3098, cseReferenceExchanges.getExchange(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(311, cseReferenceExchanges.getExchange(Country.AT), DOUBLE_TOLERANCE);
        assertEquals(718, cseReferenceExchanges.getExchange(Country.SI), DOUBLE_TOLERANCE);
    }

    @Test
    void testOnRealData1() throws IOException {
        LocalDateTime localDateTime = LocalDateTime.parse("2021-01-01T00:00");
        ZoneOffset zoneOffset = ZoneId.of("CET").getRules().getOffset(localDateTime);
        cseReferenceExchanges = CseReferenceExchanges.fromVulcanusFile(
            OffsetDateTime.of(localDateTime, zoneOffset),
            getClass().getResourceAsStream("vulcanus_01012021_96.xls"),
                "vulcanus_01012021_96.xls",
            ProcessType.IDCC
        );
        assertEquals(880, cseReferenceExchanges.getExchange(Country.CH), DOUBLE_TOLERANCE);
        assertEquals(-502, cseReferenceExchanges.getExchange(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(0, cseReferenceExchanges.getExchange(Country.AT), DOUBLE_TOLERANCE);
        assertEquals(157, cseReferenceExchanges.getExchange(Country.SI), DOUBLE_TOLERANCE);
    }

    @Test
    void testOnRealData2() throws IOException {
        LocalDateTime localDateTime = LocalDateTime.parse("2021-01-01T01:00");
        ZoneOffset zoneOffset = ZoneId.of("CET").getRules().getOffset(localDateTime);
        cseReferenceExchanges = CseReferenceExchanges.fromVulcanusFile(
            OffsetDateTime.of(localDateTime, zoneOffset),
            getClass().getResourceAsStream("vulcanus_01012021_96.xls"),
                "vulcanus_01012021_96.xls",
            ProcessType.IDCC
        );
        assertEquals(756, cseReferenceExchanges.getExchange(Country.CH), DOUBLE_TOLERANCE);
        assertEquals(-611, cseReferenceExchanges.getExchange(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(0, cseReferenceExchanges.getExchange(Country.AT), DOUBLE_TOLERANCE);
        assertEquals(174, cseReferenceExchanges.getExchange(Country.SI), DOUBLE_TOLERANCE);
    }

    @Test
    void testOnRealData3() throws IOException {
        LocalDateTime localDateTime = LocalDateTime.parse("2021-03-01T00:00");
        ZoneOffset zoneOffset = ZoneId.of("CET").getRules().getOffset(localDateTime);
        cseReferenceExchanges = CseReferenceExchanges.fromVulcanusFile(
            OffsetDateTime.of(localDateTime, zoneOffset),
            getClass().getResourceAsStream("vulcanus_01032021_96.xls"),
                "vulcanus_01032021_96.xls",
            ProcessType.IDCC
        );
        assertEquals(-1000, cseReferenceExchanges.getExchange(Country.CH), DOUBLE_TOLERANCE);
        assertEquals(-50, cseReferenceExchanges.getExchange(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(-100, cseReferenceExchanges.getExchange(Country.AT), DOUBLE_TOLERANCE);
        assertEquals(-300, cseReferenceExchanges.getExchange(Country.SI), DOUBLE_TOLERANCE);
    }

    @Test
    void testOnRealData4() throws IOException {
        LocalDateTime localDateTime = LocalDateTime.parse("2021-03-01T01:00");
        ZoneOffset zoneOffset = ZoneId.of("CET").getRules().getOffset(localDateTime);
        cseReferenceExchanges = CseReferenceExchanges.fromVulcanusFile(
            OffsetDateTime.of(localDateTime, zoneOffset),
            getClass().getResourceAsStream("vulcanus_01032021_96.xls"),
                "vulcanus_01032021_96.xls",
            ProcessType.IDCC
        );
        assertEquals(-500, cseReferenceExchanges.getExchange(Country.CH), DOUBLE_TOLERANCE);
        assertEquals(-2000, cseReferenceExchanges.getExchange(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(-100, cseReferenceExchanges.getExchange(Country.AT), DOUBLE_TOLERANCE);
        assertEquals(-300, cseReferenceExchanges.getExchange(Country.SI), DOUBLE_TOLERANCE);
    }
}
