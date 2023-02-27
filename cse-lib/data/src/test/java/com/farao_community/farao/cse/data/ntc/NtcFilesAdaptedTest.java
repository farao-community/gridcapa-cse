/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ntc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
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
        InputStream yearlyData = getClass().getResourceAsStream("2021_2Dp_NTC_annual_CSE1_Adapted_v8_8.xml");
        InputStream dailyData = getClass().getResourceAsStream("20210624_2D4_NTC_reductions_CSE1_Adapted_v8_8.xml");
        ntc = Ntc.create(targetDateTime, yearlyData, dailyData, true);
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
    void computeReducedSplittingFactors() {
        Map<String, Double> splittingFactors = ntc.computeReducedSplittingFactors();
        assertEquals(4, splittingFactors.size());
        assertEquals(0.466, splittingFactors.get("FR"), DOUBLE_PRECISION);
        assertEquals(0.409, splittingFactors.get("CH"), DOUBLE_PRECISION);
        assertEquals(0.035, splittingFactors.get("AT"), DOUBLE_PRECISION);
        assertEquals(0.089, splittingFactors.get("SI"), DOUBLE_PRECISION);
    }

    @Test
    void checkDefaultFlowForMendrisioCagno() throws JAXBException {
        Map<String, Double> fixedFlowLines = Ntc.create(OffsetDateTime.parse("2021-09-13T12:30Z"),
                                                        getClass().getResourceAsStream("2021_2Dp_NTC_annual_CSE1_Adapted_v8_8.xml"),
                                                        getClass().getResourceAsStream("20210913_2D1_NTC_reductions_CSE1_Adapted_v8_8.xml"),
                                     true
                                                        ).getFlowOnFixedFlowLines();
        assertEquals(75, fixedFlowLines.get(MENDRISIO_CAGNO_ID), DOUBLE_PRECISION);
    }
}
