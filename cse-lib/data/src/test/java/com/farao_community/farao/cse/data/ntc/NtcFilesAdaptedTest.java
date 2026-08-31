/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ntc;

import com.farao_community.farao.cse.data.DataUtil;
import com.farao_community.farao.cse.data.xsd.ntc_adapted.NTCAnnualDocument;
import com.farao_community.farao.cse.data.xsd.ntc_adapted.NTCReductionsDocument;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NtcFilesAdaptedTest {
    private static final double DOUBLE_PRECISION = 0.001;
    private static final String MENDRISIO_CAGNO_ID = "ml_mendrisio-cagno";

    private Ntc ntc;

    @BeforeEach
    void setUp() throws JAXBException {
        OffsetDateTime targetDateTime = OffsetDateTime.parse("2021-06-24T16:30Z");
        try (InputStream yearlyData = getClass().getResourceAsStream("2021_2Dp_NTC_annual_CSE1_Adapted_v8_8.xml");
            InputStream dailyData = getClass().getResourceAsStream("20210624_2D4_NTC_reductions_CSE1_Adapted_v8_8.xml")
        ) {
            ntc = new Ntc(new YearlyNtcDocumentAdapted(targetDateTime, DataUtil.unmarshalFromInputStream(yearlyData, NTCAnnualDocument.class)),
                    new DailyNtcDocumentAdapted(targetDateTime, DataUtil.unmarshalFromInputStream(dailyData, NTCReductionsDocument.class)), true);
        } catch (IOException | JAXBException e) {
            throw new CseInvalidDataException("Impossible to create NTC", e);
        }
    }

    @Test
    void getFixedFlow() {
        Map<String, Double> fixedFlowLines = ntc.getFlowOnFixedFlowLines();
        assertEquals(1, fixedFlowLines.size());
        assertEquals(150, fixedFlowLines.get(MENDRISIO_CAGNO_ID), DOUBLE_PRECISION);
    }

    @Test
    void computeMniiOffset() {
        double mniiOffset = ntc.computeMniiOffset();
        assertEquals(255, mniiOffset, DOUBLE_PRECISION);
    }

    @Test
    void checkDefaultFlowForMendrisioCagno() throws JAXBException {

        OffsetDateTime targetDateTime = OffsetDateTime.parse("2021-09-13T12:30Z");
        try (InputStream yearlyData = getClass().getResourceAsStream("2021_2Dp_NTC_annual_CSE1_Adapted_v8_8.xml");
             InputStream dailyData = getClass().getResourceAsStream("20210913_2D1_NTC_reductions_CSE1_Adapted_v8_8.xml")
        ) {
            Map<String, Double> fixedFlowLines =  new Ntc(new YearlyNtcDocumentAdapted(targetDateTime, DataUtil.unmarshalFromInputStream(yearlyData, NTCAnnualDocument.class)),
                    new DailyNtcDocumentAdapted(targetDateTime, DataUtil.unmarshalFromInputStream(dailyData, NTCReductionsDocument.class)), true)
                    .getFlowOnFixedFlowLines();
            assertEquals(75, fixedFlowLines.get(MENDRISIO_CAGNO_ID), DOUBLE_PRECISION);
        } catch (IOException | JAXBException e) {
            throw new CseInvalidDataException("Impossible to create NTC", e);
        }
    }

    @ParameterizedTest
    @CsvSource({"2025-10-25T21:30Z, 67.0, 63.0"})
    @CsvSource({"2025-10-25T22:30Z, 87.0, 93.0"})
    @CsvSource({"2025-10-25T23:30Z, 87.0, 93.0"})
    @CsvSource({"2025-10-26T22:30Z, 107.0, 123.0"})
    @CsvSource({"2025-10-26T23:30Z, 127.0, 153.0"})
    void testGetFlowByCountryOnLongClockChange(final String target, final double expectedAt, final double expectedCh) { // SpecialLines
        final OffsetDateTime targetDateTime = OffsetDateTime.parse(target);
        try (final InputStream yearlyData = getClass().getResourceAsStream("TEST_2025_2Dp_NTC_annual_CSE1.xml");
             final InputStream dailyData = getClass().getResourceAsStream("TEST_20251026_2D7_NTC_reductions_CSE1.xml")
        ) {
            final Ntc longClockChangeNtc = new Ntc(
                new YearlyNtcDocumentAdapted(targetDateTime, DataUtil.unmarshalFromInputStream(yearlyData, NTCAnnualDocument.class)),
                new DailyNtcDocumentAdapted(targetDateTime, DataUtil.unmarshalFromInputStream(dailyData, NTCReductionsDocument.class)),
                true
            );

            final Map<String, Double> flowByCountry = longClockChangeNtc.getFlowPerCountryAdapted(l -> true);
            assertEquals(expectedAt, flowByCountry.get("AT"));
            assertEquals(expectedCh, flowByCountry.get("CH"));
        } catch (IOException | JAXBException e) {
            throw new CseInvalidDataException("Impossible to create NTC", e);
        }
    }

    @ParameterizedTest
    @CsvSource({"2025-10-25T21:30Z, 11.0, 12.0, 13.0, 14.0"})
    @CsvSource({"2025-10-25T22:30Z, 21.0, 22.0, 23.0, 24.0"})
    @CsvSource({"2025-10-25T23:30Z, 21.0, 22.0, 23.0, 24.0"})
    @CsvSource({"2025-10-26T22:30Z, 31.0, 32.0, 33.0, 34.0"})
    @CsvSource({"2025-10-26T23:30Z, 41.0, 42.0, 43.0, 44.0"})
    void testGetNtcByCountryOnLongClockChange(final String target, final double expectedAt, final double expectedCh, final double expectedFr, final double expectedSi) { // BasicDays
        final OffsetDateTime targetDateTime = OffsetDateTime.parse(target);
        try (final InputStream yearlyData = getClass().getResourceAsStream("TEST_2025_2Dp_NTC_annual_CSE1.xml");
             final InputStream dailyData = getClass().getResourceAsStream("TEST_20251026_2D7_NTC_reductions_CSE1.xml")
        ) {
            final Ntc longClockChangeNtc = new Ntc(
                new YearlyNtcDocumentAdapted(targetDateTime, DataUtil.unmarshalFromInputStream(yearlyData, NTCAnnualDocument.class)),
                new DailyNtcDocumentAdapted(targetDateTime, DataUtil.unmarshalFromInputStream(dailyData, NTCReductionsDocument.class)),
                true
            );

            final Map<String, Double> ntcByCountry = longClockChangeNtc.getNtcPerCountry();
            assertEquals(expectedAt, ntcByCountry.get("AT"));
            assertEquals(expectedCh, ntcByCountry.get("CH"));
            assertEquals(expectedFr, ntcByCountry.get("FR"));
            assertEquals(expectedSi, ntcByCountry.get("SI"));
        } catch (IOException | JAXBException e) {
            throw new CseInvalidDataException("Impossible to create NTC", e);
        }
    }
}
