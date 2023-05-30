/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.farao_community.farao.cse.import_runner.app.dichotomy.CseCountry.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class CseIdccShiftDispatcherTest {
    private static final double DOUBLE_TOLERANCE = 1;

    private Map<String, Double> splittingFactors1;
    private Map<String, Double> referenceExchanges;
    private Map<String, Double> ntcs2;

    @BeforeEach
    public void setUp() {
        splittingFactors1 = ImmutableMap.of(
            FR.getEiCode(), 0.4,
            CH.getEiCode(), 0.3,
            AT.getEiCode(), 0.1,
            SI.getEiCode(), 0.2
        );

        referenceExchanges = ImmutableMap.of(
            FR.getEiCode(), 2000.,
            CH.getEiCode(), 1000.,
            AT.getEiCode(), 500.,
            SI.getEiCode(), 500.
        );

        ntcs2 = ImmutableMap.of(
            FR.getEiCode(), 3000.,
            CH.getEiCode(), 2500.,
            AT.getEiCode(), 650.,
            SI.getEiCode(), 350.
        );

    }

    @Test
    void testDispatchForValueLowerThanNtc2ButGreaterThanReferenceExchanges() {
        CseIdccShiftDispatcher dispatcher = new CseIdccShiftDispatcher(LoggerFactory.getLogger(""), ntcs2, splittingFactors1, referenceExchanges);

        Map<String, Double> shifts = dispatcher.dispatch(6250);
        assertEquals(5, shifts.keySet().size());
        assertEquals(-100., shifts.get(FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-150., shifts.get(CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-15., shifts.get(AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(15., shifts.get(SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(250., shifts.get(IT.getEiCode()), DOUBLE_TOLERANCE);
    }

    @Test
    void testDispatchForValueLowerThanReferenceExchanges() {
        CseIdccShiftDispatcher dispatcher = new CseIdccShiftDispatcher(LoggerFactory.getLogger(""), ntcs2, splittingFactors1, referenceExchanges);

        Map<String, Double> shifts = dispatcher.dispatch(3000);
        assertEquals(5, shifts.keySet().size());
        assertEquals(-1461.53, shifts.get(FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-1884.61, shifts.get(CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-250., shifts.get(AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(96.15, shifts.get(SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(3500., shifts.get(IT.getEiCode()), DOUBLE_TOLERANCE);
    }

    @Test
    void testDispatchForValueAboveReferenceExchangeAndNtc2() {
        CseIdccShiftDispatcher dispatcher = new CseIdccShiftDispatcher(LoggerFactory.getLogger(""), ntcs2, splittingFactors1, referenceExchanges);

        Map<String, Double> shifts = dispatcher.dispatch(7000);
        assertEquals(5, shifts.keySet().size());
        assertEquals(200., shifts.get(FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(150.0, shifts.get(CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(50, shifts.get(AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(100., shifts.get(SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-500, shifts.get(IT.getEiCode()), DOUBLE_TOLERANCE);
    }

}
