/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ntc2;

import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class Ntc2Test {
    private Map<String, Double> test1Nt2Files;
    private Map<String, Double> test2Nt2Files;
    private Map<String, Double> test3Nt2Files;

    @BeforeEach
    void setUp() {
        test1Nt2Files = new HashMap<>();
        test1Nt2Files.put("10YSI-ELES-----O", 111.);
        test1Nt2Files.put("10YFR-RTE------C", 222.);
        test1Nt2Files.put("10YCH-SWISSGRIDZ", 333.);
        test1Nt2Files.put("10YAT-APG------L", 444.);

        test2Nt2Files = new HashMap<>();
        test2Nt2Files.put("10YAT-APG------L", 88.);
        test2Nt2Files.put("10YCH-SWISSGRIDZ", 77.);
        test2Nt2Files.put("10YFR-RTE------C", 66.);
        test2Nt2Files.put("10YSI-ELES-----O", 55.);

        test3Nt2Files = new HashMap<>();
        test3Nt2Files.put("10YCH-SWISSGRIDZ", 99.);
        test3Nt2Files.put("10YFR-RTE------C", 121.);
    }

    @Test
    void testConstrutor() {
        Ntc2 ntc2 = new Ntc2(test1Nt2Files);
        assertEquals(444, ntc2.getExchange(Country.AT));
        assertEquals(222, ntc2.getExchange(Country.FR));
        assertEquals(111, ntc2.getExchange(Country.SI));
        assertEquals(333, ntc2.getExchange(Country.CH));

        ntc2 = new Ntc2(test3Nt2Files);
        assertEquals(121, ntc2.getExchange(Country.FR));
        assertEquals(99, ntc2.getExchange(Country.CH));
        assertNull(ntc2.getExchange(Country.SI));
        assertNull(ntc2.getExchange(Country.AT));

    }

    @Test
    void testGetEchanges() {

        Ntc2 ntc2 = new Ntc2(test1Nt2Files);
        Map<String, Double> exchanges = ntc2.getExchanges();
        assertNotNull(exchanges);
        assertEquals(4, exchanges.size());

        assertEquals(111, exchanges.get("10YSI-ELES-----O"));
        assertEquals(222, exchanges.get("10YFR-RTE------C"));
        assertEquals(333, exchanges.get("10YCH-SWISSGRIDZ"));
        assertEquals(444, exchanges.get("10YAT-APG------L"));

        ntc2 = new Ntc2(test2Nt2Files);
        exchanges = ntc2.getExchanges();
        assertNotNull(exchanges);
        assertEquals(4, exchanges.size());
        assertEquals(55, exchanges.get("10YSI-ELES-----O"));
        assertEquals(66, exchanges.get("10YFR-RTE------C"));
        assertEquals(77, exchanges.get("10YCH-SWISSGRIDZ"));
        assertEquals(88, exchanges.get("10YAT-APG------L"));

        ntc2 = new Ntc2(test3Nt2Files);
        exchanges = ntc2.getExchanges();
        assertNotNull(exchanges);
        assertEquals(2, exchanges.size());
        assertEquals(99, exchanges.get("10YCH-SWISSGRIDZ"));
        assertEquals(121, exchanges.get("10YFR-RTE------C"));

    }
}
