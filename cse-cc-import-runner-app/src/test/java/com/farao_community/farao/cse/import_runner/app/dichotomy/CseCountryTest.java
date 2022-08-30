/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class CseCountryTest {

    @Test
    void testCreationByPowsyblCountry() {
        assertEquals(CseCountry.IT, CseCountry.byCountry(Country.IT));
        assertEquals(CseCountry.FR, CseCountry.byCountry(Country.FR));
        assertEquals(CseCountry.AT, CseCountry.byCountry(Country.AT));
        assertEquals(CseCountry.SI, CseCountry.byCountry(Country.SI));
        assertEquals(CseCountry.CH, CseCountry.byCountry(Country.CH));
    }

    @Test
    void testCreationByPowsyblCountryFails() {
        assertThrows(IllegalArgumentException.class, () -> CseCountry.byCountry(Country.ME));
    }

    @Test
    void testCreationByEiCode() {
        assertEquals(CseCountry.IT, CseCountry.byEiCode(new CountryEICode(Country.IT).getCode()));
        assertEquals(CseCountry.FR, CseCountry.byEiCode(new CountryEICode(Country.FR).getCode()));
        assertEquals(CseCountry.AT, CseCountry.byEiCode(new CountryEICode(Country.AT).getCode()));
        assertEquals(CseCountry.SI, CseCountry.byEiCode(new CountryEICode(Country.SI).getCode()));
        assertEquals(CseCountry.CH, CseCountry.byEiCode(new CountryEICode(Country.CH).getCode()));
    }

    @Test
    void testCreationByEiCodeFails() {
        String eiCode = new CountryEICode(Country.ME).getCode();
        assertThrows(IllegalArgumentException.class, () -> CseCountry.byEiCode(eiCode));
    }

    @Test
    void testContains() {
        assertTrue(CseCountry.contains(Country.IT));
        assertTrue(CseCountry.contains(Country.FR));
        assertTrue(CseCountry.contains(Country.AT));
        assertTrue(CseCountry.contains(Country.SI));
        assertTrue(CseCountry.contains(Country.CH));
    }

    @Test
    void testDoesNotContain() {
        assertFalse(CseCountry.contains(Country.ME));
        assertFalse(CseCountry.contains(Country.DE));
        assertFalse(CseCountry.contains(Country.AF));
        assertFalse(CseCountry.contains(Country.BJ));
    }
}
