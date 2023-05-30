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
    void testD2ccShift() {
        Map<String, Double> splittingFactors = Map.of(
            CseCountry.FR.getEiCode(), 0.428,
            CseCountry.CH.getEiCode(), 0.477,
            CseCountry.AT.getEiCode(), 0.030,
            CseCountry.SI.getEiCode(), 0.065
        );

        Map<String, Double> ntcs = Map.of(
            CseCountry.FR.getEiCode(), 2000.,
            CseCountry.CH.getEiCode(), 1500.,
            CseCountry.AT.getEiCode(), 100.,
            CseCountry.SI.getEiCode(), 300.
        );

        CseD2ccShiftDispatcher dispatcher = new CseD2ccShiftDispatcher(LoggerFactory.getLogger(""), splittingFactors, ntcs);
        Map<String, Double> shifts = dispatcher.dispatch(3000);

        assertEquals(-385.2, shifts.get(CseCountry.FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-429.29, shifts.get(CseCountry.CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-27.0, shifts.get(CseCountry.AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-58.5, shifts.get(CseCountry.SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(900.0, shifts.get(CseCountry.IT.getEiCode()), DOUBLE_TOLERANCE);
    }
}
