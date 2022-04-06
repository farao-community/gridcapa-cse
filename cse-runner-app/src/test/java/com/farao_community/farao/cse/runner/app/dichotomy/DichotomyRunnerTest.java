/*
 *
 *  * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.cse.runner.app.dichotomy;

import com.farao_community.farao.cse.computation.BorderExchanges;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class DichotomyRunnerTest {

    @Test
    void testSplittingFactorsConversion() {
        Map<String, Double> splittingFactors = Map.of(
            "AT", 0.1,
            "CH", 0.2,
            "FR", 0.3,
            "SI", 0.4
        );
        Map<String, Double> convertedSplittingFactors = DichotomyRunner.convertSplittingFactors(splittingFactors);
        assertEquals(0.1, convertedSplittingFactors.get(CseCountry.AT.getEiCode()), 0.01);
        assertEquals(0.2, convertedSplittingFactors.get(CseCountry.CH.getEiCode()), 0.01);
        assertEquals(0.3, convertedSplittingFactors.get(CseCountry.FR.getEiCode()), 0.01);
        assertEquals(0.4, convertedSplittingFactors.get(CseCountry.SI.getEiCode()), 0.01);
        assertEquals(-1.0, convertedSplittingFactors.get(CseCountry.IT.getEiCode()), 0.01);
    }

    @Test
    void testBorderExchangesConversion() {
        Map<String, Double> borderExchanges = Map.of(
            BorderExchanges.IT_AT, 100.,
            BorderExchanges.IT_CH, 200.,
            BorderExchanges.IT_FR, 300.,
            BorderExchanges.IT_SI, 400.
        );
        Map<String, Double> convertedBorderExchanges = DichotomyRunner.convertBorderExchanges(borderExchanges);
        assertEquals(-100, convertedBorderExchanges.get(CseCountry.AT.getEiCode()), 0.01);
        assertEquals(-200, convertedBorderExchanges.get(CseCountry.CH.getEiCode()), 0.01);
        assertEquals(-300, convertedBorderExchanges.get(CseCountry.FR.getEiCode()), 0.01);
        assertEquals(-400, convertedBorderExchanges.get(CseCountry.SI.getEiCode()), 0.01);
    }

    @Test
    void testFlowOnmerchantLinesConversion() {
        Map<String, Double> flowOnMerchantLinesPerCountry = Map.of(
            "AT", 100.,
            "FR", 300.,
            "SI", 400.
        );
        Map<String, Double> convertedFlowOnMerchantLinesPerCountry = DichotomyRunner.convertFlowsOnMerchantLines(flowOnMerchantLinesPerCountry);
        assertEquals(100, convertedFlowOnMerchantLinesPerCountry.get(CseCountry.AT.getEiCode()), 0.01);
        assertEquals(0, convertedFlowOnMerchantLinesPerCountry.get(CseCountry.CH.getEiCode()), 0.01);
        assertEquals(300, convertedFlowOnMerchantLinesPerCountry.get(CseCountry.FR.getEiCode()), 0.01);
        assertEquals(400, convertedFlowOnMerchantLinesPerCountry.get(CseCountry.SI.getEiCode()), 0.01);
    }
}
