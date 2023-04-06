/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.powsybl.iidm.network.Country;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class NetworkShifterUtil {
    private NetworkShifterUtil() {
    }

    static Map<String, Double> convertSplittingFactors(Map<String, Double> tSplittingFactors) {
        Map<String, Double> splittingFactors = new TreeMap<>();
        tSplittingFactors.forEach((key, value) -> splittingFactors.put(toEic(key), value));
        splittingFactors.put(toEic("IT"), -splittingFactors.values().stream().reduce(0., Double::sum));
        return splittingFactors;
    }

    static Map<String, Double> convertBorderExchanges(Map<String, Double> borderExchanges) {
        Map<String, Double> convertedBorderExchanges = new HashMap<>();
        borderExchanges.forEach((key, value) -> {
            // We take -value because we want flow towards Italy
            switch (key) {
                case BorderExchanges.IT_AT:
                    convertedBorderExchanges.put(CseCountry.AT.getEiCode(), -value);
                    break;
                case BorderExchanges.IT_CH:
                    convertedBorderExchanges.put(CseCountry.CH.getEiCode(), -value);
                    break;
                case BorderExchanges.IT_FR:
                    convertedBorderExchanges.put(CseCountry.FR.getEiCode(), -value);
                    break;
                case BorderExchanges.IT_SI:
                    convertedBorderExchanges.put(CseCountry.SI.getEiCode(), -value);
                    break;
                default:
                    break;
            }
        });
        return convertedBorderExchanges;
    }

    static Map<String, Double> convertFlowsOnMerchantLines(Map<String, Double> flowOnMerchantLinesPerCountry) {
        Map<String, Double> convertedFlowOnMerchantLinesPerCountry = new HashMap<>();
        Set.of(CseCountry.FR, CseCountry.CH, CseCountry.AT, CseCountry.SI).forEach(country -> {
            double exchange = flowOnMerchantLinesPerCountry.getOrDefault(country.getName(), 0.);
            convertedFlowOnMerchantLinesPerCountry.put(country.getEiCode(), exchange);
        });
        return convertedFlowOnMerchantLinesPerCountry;
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
