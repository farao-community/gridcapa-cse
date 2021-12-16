/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.app.dichotomy;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public enum CseCountry {
    IT("10YIT-GRTN-----B"),
    FR("10YFR-RTE------C"),
    AT("10YAT-APG------L"),
    CH("10YCH-SWISSGRIDZ"),
    SI("10YSI-ELES-----O");

    private final String eiCode;

    CseCountry(String eiCode) {
        this.eiCode = eiCode;
    }

    public String getEiCode() {
        return eiCode;
    }

    @Override
    public String toString() {
        return eiCode;
    }
}
