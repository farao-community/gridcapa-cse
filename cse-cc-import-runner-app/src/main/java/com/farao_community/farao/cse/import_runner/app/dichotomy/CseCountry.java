/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.iidm.network.Country;

import java.util.Arrays;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public enum CseCountry {
    IT(Country.IT, "IT"),
    FR(Country.FR, "FR"),
    AT(Country.AT, "AT"),
    CH(Country.CH, "CH"),
    SI(Country.SI, "SI");

    private final String eiCode;
    private final String name;

    CseCountry(Country country, String name) {
        this.eiCode = new CountryEICode(country).getCode();
        this.name = name;
    }

    public String getEiCode() {
        return eiCode;
    }

    public String getName() {
        return name;
    }

    public static boolean contains(Country country) {
        return Arrays.stream(values())
            .map(CseCountry::getName)
            .anyMatch(name -> name.equals(country.toString()));
    }

    public static CseCountry byCountry(Country country) {
        if (contains(country)) {
            return valueOf(country.toString());
        } else {
            throw new IllegalArgumentException(String.format("CseCountry does not contain %s", country));
        }
    }

    public static CseCountry byEiCode(String eiCode) {
        return Arrays.stream(values())
            .filter(cseCountry -> cseCountry.getEiCode().equals(eiCode))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("CseCountry does not contain %s", eiCode)));
    }

    @Override
    public String toString() {
        return eiCode;
    }
}
