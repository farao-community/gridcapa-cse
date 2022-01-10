/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
class BorderExchangesTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private BorderExchanges bordersExchanges;

    @Test
    void getBordersFlowsfromVulcanusFileTest() throws IOException {
        Map<String, Double> exchanges = bordersExchanges.fromVulcanusFile(OffsetDateTime.parse("2019-12-28T14:30Z"),
                getClass().getResourceAsStream("vulcanus_28122019_96.xls"));

        assertEquals(7, exchanges.size());
        assertEquals(633, exchanges.get("CH - IT"), DOUBLE_TOLERANCE);
        assertEquals(3021, exchanges.get("FR - IT"), DOUBLE_TOLERANCE);
        assertEquals(0, exchanges.get("AT - IT"), DOUBLE_TOLERANCE);
        assertEquals(-44, exchanges.get("SI - IT"), DOUBLE_TOLERANCE);
        assertEquals(2694, exchanges.get("FR - DE"), DOUBLE_TOLERANCE);
        assertEquals(2208, exchanges.get("CH - DE"), DOUBLE_TOLERANCE);
        assertEquals(-3000, exchanges.get("CH - FR"), DOUBLE_TOLERANCE);

    }
}
