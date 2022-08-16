/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class DichotomyRunnerTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Autowired
    private FileImporter fileImporter;

    @Autowired
    private DichotomyRunner dichotomyRunner;

    @Test
    void testHandleLskD2CC() throws IOException {
        Network network = fileImporter.importNetwork(Objects.requireNonNull(getClass().getResource("CSE_no_normal_glsk_variation.uct")).toString());
        ZonalData<Scalable> zonalScalable = dichotomyRunner.getZonalScalable(Objects.requireNonNull(getClass().getResource("EmptyGlsk.xml")).toString(), network, ProcessType.D2CC);
        assertEquals(500, zonalScalable.getData("10YAT-APG------L").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(500, zonalScalable.getData("10YFR-RTE------C").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(1000, zonalScalable.getData("10YCH-SWISSGRIDZ").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(0, zonalScalable.getData("10YSI-ELES-----O").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(6000, zonalScalable.getData("10YIT-GRTN-----B").scale(network, 10000), DOUBLE_TOLERANCE);
        assertEquals(-10000, zonalScalable.getData("10YIT-GRTN-----B").scale(network, -10000), DOUBLE_TOLERANCE);
    }

    @Test
    void testHandleLskIDCC() throws IOException {
        Network network = fileImporter.importNetwork(Objects.requireNonNull(getClass().getResource("CSE_no_normal_glsk_variation.uct")).toString());
        ZonalData<Scalable> zonalScalable = dichotomyRunner.getZonalScalable(Objects.requireNonNull(getClass().getResource("EmptyGlsk.xml")).toString(), network, ProcessType.IDCC);
        assertEquals(500, zonalScalable.getData("10YAT-APG------L").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(500, zonalScalable.getData("10YFR-RTE------C").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(1000, zonalScalable.getData("10YCH-SWISSGRIDZ").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(0, zonalScalable.getData("10YSI-ELES-----O").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(0, zonalScalable.getData("10YIT-GRTN-----B").scale(network, 10000), DOUBLE_TOLERANCE);
        assertEquals(0, zonalScalable.getData("10YIT-GRTN-----B").scale(network, -10000), DOUBLE_TOLERANCE);
    }

    @Test
    void testSplittingFactorsConversion() {
        Map<String, Double> splittingFactors = Map.of(
            "AT", 0.1,
            "CH", 0.2,
            "FR", 0.3,
            "SI", 0.4
        );
        Map<String, Double> convertedSplittingFactors = DichotomyRunner.convertSplittingFactors(splittingFactors);
        assertEquals(0.1, convertedSplittingFactors.get(CseCountry.AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(0.2, convertedSplittingFactors.get(CseCountry.CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(0.3, convertedSplittingFactors.get(CseCountry.FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(0.4, convertedSplittingFactors.get(CseCountry.SI.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-1.0, convertedSplittingFactors.get(CseCountry.IT.getEiCode()), DOUBLE_TOLERANCE);
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
        assertEquals(-100, convertedBorderExchanges.get(CseCountry.AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-200, convertedBorderExchanges.get(CseCountry.CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-300, convertedBorderExchanges.get(CseCountry.FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(-400, convertedBorderExchanges.get(CseCountry.SI.getEiCode()), DOUBLE_TOLERANCE);
    }

    @Test
    void testFlowOnmerchantLinesConversion() {
        Map<String, Double> flowOnMerchantLinesPerCountry = Map.of(
            "AT", 100.,
            "FR", 300.,
            "SI", 400.
        );
        Map<String, Double> convertedFlowOnMerchantLinesPerCountry = DichotomyRunner.convertFlowsOnMerchantLines(flowOnMerchantLinesPerCountry);
        assertEquals(100, convertedFlowOnMerchantLinesPerCountry.get(CseCountry.AT.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(0, convertedFlowOnMerchantLinesPerCountry.get(CseCountry.CH.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(300, convertedFlowOnMerchantLinesPerCountry.get(CseCountry.FR.getEiCode()), DOUBLE_TOLERANCE);
        assertEquals(400, convertedFlowOnMerchantLinesPerCountry.get(CseCountry.SI.getEiCode()), DOUBLE_TOLERANCE);
    }
}
