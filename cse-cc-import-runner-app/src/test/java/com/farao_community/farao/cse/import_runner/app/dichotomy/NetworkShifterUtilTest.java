/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class NetworkShifterUtilTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    void testSplittingFactorsConversion() {
        Map<String, Double> splittingFactors = Map.of(
            "AT", 0.1,
            "CH", 0.2,
            "FR", 0.3,
            "SI", 0.4
        );
        Map<String, Double> convertedSplittingFactors = NetworkShifterUtil.convertMapByCountryToMapByEic(splittingFactors);
        assertEquals(0.1, convertedSplittingFactors.get(CseCountry.AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(0.2, convertedSplittingFactors.get(CseCountry.CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(0.3, convertedSplittingFactors.get(CseCountry.FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(0.4, convertedSplittingFactors.get(CseCountry.SI.getEiCode()), DOUBLE_TOLERANCE);
    }

}
