/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.app.util;

import com.farao_community.farao.cse.runner.app.dichotomy.CseIdccShiftDispatcher;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.farao_community.farao.cse.runner.app.dichotomy.CseCountry.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class CseIdccShiftDispatcherTest {
    private static final double DOUBLE_TOLERANCE = 1;

    private Map<String, Double> splittingFactors1;
    private Map<String, Double> splittingFactors2;
    private Map<String, Double> referenceExchanges;
    private Map<String, Double> ntcs2;
    private Map<String, Double> negativeNtcs2Map;

    @BeforeEach
    public void setUp() {
        splittingFactors1 = ImmutableMap.of(
                FR.getEiCode(), 0.4,
                AT.getEiCode(), 0.2,
                CH.getEiCode(), 0.1,
                SI.getEiCode(), 0.3,
                IT.getEiCode(), -1.0
        );

        splittingFactors2 = ImmutableMap.of(
                FR.getEiCode(), 0.6,
                AT.getEiCode(), 0.05,
                CH.getEiCode(), 0.2,
                SI.getEiCode(), 0.15,
                IT.getEiCode(), -1.0
        );

        referenceExchanges = ImmutableMap.of(
                FR.getEiCode(), 2000.,
                AT.getEiCode(), 1000.,
                CH.getEiCode(), 500.,
                SI.getEiCode(), 2500.
        );

        ntcs2 = ImmutableMap.of(
                FR.getEiCode(), 3000.,
                AT.getEiCode(), 2000.,
                CH.getEiCode(), 1500.,
                SI.getEiCode(), 4500.
        );

        negativeNtcs2Map = ImmutableMap.of(
                FR.getEiCode(), -3000.,
                AT.getEiCode(), -2000.,
                CH.getEiCode(), -1500.,
                SI.getEiCode(), -4500.
        );
    }

    @Test
    void testDispatchForValueInferiorToReferenceExchanges() throws ShiftingException {
        CseIdccShiftDispatcher dispatcher = new CseIdccShiftDispatcher(splittingFactors1, referenceExchanges, ntcs2);

        Map<String, Double> shifts = dispatcher.dispatch(3000);
        assertEquals(5, shifts.keySet().size());
        assertEquals(-818, shifts.get(FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-409, shifts.get(CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-545, shifts.get(AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-1227, shifts.get(SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(3000, shifts.get(IT.getEiCode()), DOUBLE_TOLERANCE);
    }

    @Test
    void testDispatchForValueInferiorToReferenceExchanges2() throws ShiftingException {
        // No changes compared to splittingFactors1
        CseIdccShiftDispatcher dispatcher = new CseIdccShiftDispatcher(splittingFactors2, referenceExchanges, ntcs2);

        Map<String, Double> shifts = dispatcher.dispatch(3000);
        assertEquals(5, shifts.keySet().size());
        assertEquals(-818, shifts.get(FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-409, shifts.get(CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-545, shifts.get(AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-1227, shifts.get(SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(3000, shifts.get(IT.getEiCode()), DOUBLE_TOLERANCE);
    }

    @Test
    void testDispatchForValueBetweenReferenceExchangeAndNtc2() throws ShiftingException {
        CseIdccShiftDispatcher dispatcher = new CseIdccShiftDispatcher(splittingFactors1, referenceExchanges, ntcs2);

        Map<String, Double> shifts = dispatcher.dispatch(7000);
        assertEquals(5, shifts.keySet().size());
        assertEquals(200, shifts.get(FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(200, shifts.get(CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(200, shifts.get(AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(400., shifts.get(SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-1000, shifts.get(IT.getEiCode()), DOUBLE_TOLERANCE);
    }

    @Test
    void testDispatchForValueBetweenReferenceExchangeAndNtc2WithSplittingFactors2() throws ShiftingException {
        // No changes compared to splittingFactors1
        CseIdccShiftDispatcher dispatcher = new CseIdccShiftDispatcher(splittingFactors2, referenceExchanges, ntcs2);

        Map<String, Double> shifts = dispatcher.dispatch(7000);
        assertEquals(5, shifts.keySet().size());
        assertEquals(200, shifts.get(FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(200, shifts.get(CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(200, shifts.get(AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(400., shifts.get(SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-1000, shifts.get(IT.getEiCode()), DOUBLE_TOLERANCE);
    }

    @Test
    void testDispatchForValueSuperiorToNtc2() throws ShiftingException {
        CseIdccShiftDispatcher dispatcher = new CseIdccShiftDispatcher(splittingFactors1, referenceExchanges, ntcs2);

        Map<String, Double> shifts = dispatcher.dispatch(15000);
        assertEquals(5, shifts.keySet().size());
        assertEquals(2600, shifts.get(FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(1400, shifts.get(CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(1800, shifts.get(AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(3200, shifts.get(SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-9000, shifts.get(IT.getEiCode()), DOUBLE_TOLERANCE);
    }

    @Test
    void testDispatchForValueSuperiorToNtc2WithDifferentSplittingFactors() throws ShiftingException {
        CseIdccShiftDispatcher dispatcher = new CseIdccShiftDispatcher(splittingFactors2, referenceExchanges, ntcs2);

        Map<String, Double> shifts = dispatcher.dispatch(15000);
        assertEquals(5, shifts.keySet().size());
        assertEquals(3400, shifts.get(FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(1800, shifts.get(CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(1200, shifts.get(AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(2600, shifts.get(SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-9000, shifts.get(IT.getEiCode()), DOUBLE_TOLERANCE);
    }

    @Test
    void testThrowsExceptionWhenValueInferiorToReferenceExchangesAndNtc2ForAllCountriesAreNegative() {
        CseIdccShiftDispatcher dispatcher = new CseIdccShiftDispatcher(splittingFactors1, referenceExchanges, negativeNtcs2Map);
        assertThrows(ShiftingException.class, () -> dispatcher.dispatch(3000));
    }

    @Test
    void testDispatchForValueSuperiorToReferenceExchangesAndNtc2ForAllCountriesAreNegative() throws ShiftingException {
        CseIdccShiftDispatcher dispatcher = new CseIdccShiftDispatcher(splittingFactors1, referenceExchanges, negativeNtcs2Map);
        Map<String, Double> shifts = dispatcher.dispatch(7000);
        assertEquals(2200, shifts.get(FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-200, shifts.get(CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(600, shifts.get(AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-1600, shifts.get(SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-1000, shifts.get(IT.getEiCode()), DOUBLE_TOLERANCE);
    }
}
