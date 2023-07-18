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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
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
}
