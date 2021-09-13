/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
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

import static org.junit.jupiter.api.Assertions.*;

class NtcFilesTest {
    private static final double DOUBLE_PRECISION = 0.001;

    private Ntc ntc;

    @BeforeEach
    public void setUp() throws JAXBException {
        OffsetDateTime targetDateTime = OffsetDateTime.parse("2021-06-24T16:30Z");
        InputStream yearlyData = getClass().getResourceAsStream("2021_2Dp_NTC_annual_CSE1.xml");
        InputStream dailyData = getClass().getResourceAsStream("20210624_2D4_NTC_reductions_CSE1.xml");
        ntc = Ntc.create(targetDateTime, yearlyData, dailyData);
    }

    @Test
    void getFixedFlow() {
        Map<String, Double> fixedFlowLines = ntc.getFlowOnFixedFlowLines();
        assertEquals(1, fixedFlowLines.size());
        assertEquals(150, fixedFlowLines.get("ml_mendrisio-cagno"), DOUBLE_PRECISION);
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
}
