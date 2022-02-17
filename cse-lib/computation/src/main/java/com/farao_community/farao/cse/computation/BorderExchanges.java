/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.computation;

import com.powsybl.balances_adjustment.util.CountryArea;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class BorderExchanges {
    private static final Logger LOGGER = LoggerFactory.getLogger(BorderExchanges.class);

    private BorderExchanges() {
        // Should not be instantiated
    }

    public static double computeItalianImport(Network network) {
        runLoadFlow(network);
        CountryArea itArea = new CountryAreaFactory(Country.IT).create(network);
        return Stream.of(Country.FR, Country.AT, Country.CH, Country.SI)
            .map(country -> new CountryAreaFactory(country).create(network).getLeavingFlowToCountry(itArea))
            .reduce(0., Double::sum);
    }

    public static Map<String, Double> computeCseBordersExchanges(Network network) {
        runLoadFlow(network);
        Map<String, Double> borderExchanges = new HashMap<>();
        Map<Country, CountryArea> countryAreaPerCountry = Stream.of(Country.FR, Country.AT, Country.CH, Country.SI, Country.IT, Country.DE)
            .collect(Collectors.toMap(
                Function.identity(),
                country -> new CountryAreaFactory(country).create(network)
            ));
        borderExchanges.put("IT-CH", getBorderExchange(Country.IT, Country.CH, countryAreaPerCountry));
        borderExchanges.put("IT-FR", getBorderExchange(Country.IT, Country.FR, countryAreaPerCountry));
        borderExchanges.put("IT-AT", getBorderExchange(Country.IT, Country.AT, countryAreaPerCountry));
        borderExchanges.put("IT-SI", getBorderExchange(Country.IT, Country.SI, countryAreaPerCountry));
        borderExchanges.put("CH-FR", getBorderExchange(Country.CH, Country.FR, countryAreaPerCountry));
        borderExchanges.put("FR-DE", getBorderExchange(Country.FR, Country.DE, countryAreaPerCountry));
        borderExchanges.put("CH-DE", getBorderExchange(Country.CH, Country.DE, countryAreaPerCountry));
        return borderExchanges;
    }

    private static double getBorderExchange(Country fromCountry, Country toCountry, Map<Country, CountryArea> countryAreaPerCountry) {
        return countryAreaPerCountry.get(fromCountry).getLeavingFlowToCountry(countryAreaPerCountry.get(toCountry));
    }

    public static Map<String, Double> computeCseCountriesBalances(Network network) {
        runLoadFlow(network);
        Map<String, Double> countriesBalances = new HashMap<>();
        countriesBalances.put("AT", new CountryAreaFactory(Country.AT).create(network).getNetPosition());
        countriesBalances.put("CH", new CountryAreaFactory(Country.CH).create(network).getNetPosition());
        countriesBalances.put("FR", new CountryAreaFactory(Country.FR).create(network).getNetPosition());
        countriesBalances.put("SI", new CountryAreaFactory(Country.SI).create(network).getNetPosition());
        countriesBalances.put("IT", new CountryAreaFactory(Country.IT).create(network).getNetPosition());
        countriesBalances.put("DE", new CountryAreaFactory(Country.DE).create(network).getNetPosition());
        return countriesBalances;
    }

    private static void runLoadFlow(Network network) {
        LoadFlowResult result = LoadFlow.run(network, LoadFlowParameters.load());
        if (!result.isOk()) {
            LOGGER.error("Loadflow computation diverged on network '{}'", network.getId());
            throw new CseComputationException(String.format("Loadflow computation diverged on network %s", network.getId()));
        }
    }
}
