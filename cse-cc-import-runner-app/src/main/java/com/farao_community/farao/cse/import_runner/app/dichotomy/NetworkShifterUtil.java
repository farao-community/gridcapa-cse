/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.powsybl.openrao.commons.EICode;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.powsybl.iidm.network.Country;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class NetworkShifterUtil {
    private NetworkShifterUtil() {
    }

    public static Map<String, Double> convertMapByCountryToMapByEic(Map<String, Double> mapByCountry) {
        Map<String, Double> mapByEic = new TreeMap<>();
        // initialize map to cse borders
        mapByEic.put(toEic("FR"), 0.);
        mapByEic.put(toEic("CH"), 0.);
        mapByEic.put(toEic("SI"), 0.);
        mapByEic.put(toEic("AT"), 0.);
        mapByCountry.forEach((key, value) -> mapByEic.put(toEic(key), value));
        return mapByEic;
    }

    private static String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }

    public static Map<String, Double> getReferenceExchanges(CseData cseData) {
        return cseData.getCseReferenceExchanges().getExchanges();
    }

    public static double getReferenceItalianImport(Map<String, Double> referenceExchanges) {
        return referenceExchanges.values().stream().reduce(0., Double::sum);
    }

}
