/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class CseD2ccShiftDispatcherTest {
    private static final double DOUBLE_TOLERANCE = 1;

    @Test
    void testShiftForD2cc() {

        Map<String, Double> ntcs = Map.of(
            CseCountry.FR.getEiCode(), 100.,
            CseCountry.CH.getEiCode(), 100.,
            CseCountry.AT.getEiCode(), 100.,
            CseCountry.SI.getEiCode(), 100.
        );

        Map<String, Double> splittingFactors = Map.of(
            CseCountry.FR.getEiCode(), 0.428,
            CseCountry.CH.getEiCode(), 0.477,
            CseCountry.AT.getEiCode(), 0.030,
            CseCountry.SI.getEiCode(), 0.065
        );

        Map<String, Double> referenceExchanges = Map.of(
            CseCountry.FR.getEiCode(), 2000.,
            CseCountry.CH.getEiCode(), 500.,
            CseCountry.AT.getEiCode(), 1000.,
            CseCountry.SI.getEiCode(), 2500.
        );

        Map<String, Double> flowOnNotModelledLinesPerCountry = Map.of(
            CseCountry.FR.getEiCode(), 0.,
            CseCountry.CH.getEiCode(), 250.,
            CseCountry.AT.getEiCode(), 80.,
            CseCountry.SI.getEiCode(), 0.
        );

        CseD2ccShiftDispatcher dispatcher = new CseD2ccShiftDispatcher(LoggerFactory.getLogger("anyLogger"), ntcs, splittingFactors, referenceExchanges, flowOnNotModelledLinesPerCountry);

        Map<String, Double> initialShifts = dispatcher.dispatch(3000);
        assertEquals(-1900, initialShifts.get(CseCountry.FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-650, initialShifts.get(CseCountry.CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-980, initialShifts.get(CseCountry.AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-2400, initialShifts.get(CseCountry.SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(5930, initialShifts.get(CseCountry.IT.getEiCode()), DOUBLE_TOLERANCE);

        // Second shift up by 650
        Map<String, Double> secondShifts = dispatcher.dispatch(3650);
        assertEquals(-1621.8, secondShifts.get(CseCountry.FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-339.95, secondShifts.get(CseCountry.CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-960.5, secondShifts.get(CseCountry.AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-2357.75, secondShifts.get(CseCountry.SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(5280.0, secondShifts.get(CseCountry.IT.getEiCode()), DOUBLE_TOLERANCE);

        // Third shift down by 325
        Map<String, Double> thirdShifts = dispatcher.dispatch(3325);
        assertEquals(-1760.9, thirdShifts.get(CseCountry.FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-494.975, thirdShifts.get(CseCountry.CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-970.25, thirdShifts.get(CseCountry.AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-2378.875, thirdShifts.get(CseCountry.SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(5605.0, thirdShifts.get(CseCountry.IT.getEiCode()), DOUBLE_TOLERANCE);
    }

}
