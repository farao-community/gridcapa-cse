/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class ZonalScalableProviderTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Autowired
    private FileImporter fileImporter;

    @Autowired
    private ZonalScalableProvider zonalScalableProvider;

    @Test
    void testHandleLskD2CC() {
        Network network = fileImporter.importNetwork(Objects.requireNonNull(getClass().getResource("CSE_no_normal_glsk_variation.uct")).toString());
        ZonalData<Scalable> zonalScalable = zonalScalableProvider.get(Objects.requireNonNull(getClass().getResource("EmptyGlsk.xml")).toString(), network, ProcessType.D2CC);
        assertEquals(500, zonalScalable.getData("10YAT-APG------L").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(500, zonalScalable.getData("10YFR-RTE------C").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(1000, zonalScalable.getData("10YCH-SWISSGRIDZ").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(0, zonalScalable.getData("10YSI-ELES-----O").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(6000, zonalScalable.getData("10YIT-GRTN-----B").scale(network, 10000), DOUBLE_TOLERANCE);
        assertEquals(-10000, zonalScalable.getData("10YIT-GRTN-----B").scale(network, -10000), DOUBLE_TOLERANCE);
    }

    @Test
    void testHandleLskIDCC() {
        Network network = fileImporter.importNetwork(Objects.requireNonNull(getClass().getResource("CSE_no_normal_glsk_variation.uct")).toString());
        ZonalData<Scalable> zonalScalable = zonalScalableProvider.get(Objects.requireNonNull(getClass().getResource("EmptyGlsk.xml")).toString(), network, ProcessType.IDCC);
        assertEquals(500, zonalScalable.getData("10YAT-APG------L").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(500, zonalScalable.getData("10YFR-RTE------C").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(1000, zonalScalable.getData("10YCH-SWISSGRIDZ").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(0, zonalScalable.getData("10YSI-ELES-----O").scale(network, 1000), DOUBLE_TOLERANCE);
        assertEquals(0, zonalScalable.getData("10YIT-GRTN-----B").scale(network, 10000), DOUBLE_TOLERANCE);
        assertEquals(0, zonalScalable.getData("10YIT-GRTN-----B").scale(network, -10000), DOUBLE_TOLERANCE);
    }
}
