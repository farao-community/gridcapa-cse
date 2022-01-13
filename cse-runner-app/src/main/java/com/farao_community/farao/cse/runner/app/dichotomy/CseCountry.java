/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.app.dichotomy;

import com.farao_community.farao.commons.CountryEICode;
import com.powsybl.iidm.network.Country;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public enum CseCountry {
    IT(Country.IT),
    FR(Country.FR),
    AT(Country.AT),
    CH(Country.CH),
    SI(Country.SI);

    private final String eiCode;

    CseCountry(Country country) {
        this.eiCode = new CountryEICode(country).getCode();
    }

    public String getEiCode() {
        return eiCode;
    }

    @Override
    public String toString() {
        return eiCode;
    }
}
