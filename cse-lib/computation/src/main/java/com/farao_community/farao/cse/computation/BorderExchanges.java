/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.computation;

import com.farao_community.farao.cse.network_processing.pisa_change.PiSaLinkProcessor;
import com.powsybl.balances_adjustment.util.CountryArea;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class BorderExchanges {
    private static final Logger LOGGER = LoggerFactory.getLogger(BorderExchanges.class);
    public static final String IT_CH = "IT-CH";
    public static final String IT_FR = "IT-FR";
    public static final String IT_AT = "IT-AT";
    public static final String IT_SI = "IT-SI";
    public static final String CH_FR = "CH-FR";
    public static final String FR_DE = "FR-DE";
    public static final String CH_DE = "CH-DE";

    private BorderExchanges() {
        // Should not be instantiated
    }

    public static double computeItalianImport(Network network, Collection<PiSaLinkProcessor> piSaLinkProcessors) {
        runLoadFlow(network);
        CountryArea itArea = new CountryAreaFactory(Country.IT).create(network);
        return Stream.of(Country.FR, Country.AT, Country.CH, Country.SI)
            .map(country -> {
                double flow = new CountryAreaFactory(country).create(network).getLeavingFlowToCountry(itArea);
                if (country == Country.FR) {
                    flow += getPiSaFlowToItaly(network, piSaLinkProcessors);
                }
                return flow;
            })
            .reduce(0., Double::sum);
    }

    private static double getPiSaFlowToItaly(Network network, Collection<PiSaLinkProcessor> piSaLinkProcessors) {
        return piSaLinkProcessors.stream().mapToDouble(piSaLinkProcessor -> getFlowToItalyForOnePiSaLink(network, piSaLinkProcessor)).sum();
    }

    private static double getFlowToItalyForOnePiSaLink(Network network, PiSaLinkProcessor piSaLinkProcessor) {
        // As it is on French side, positive P means consumption, means flow directed towards Italy
        return Optional.ofNullable(piSaLinkProcessor.getFrFictiveGenerator(network))
            .map(g -> g.getTerminal().getP())
            .filter(d -> !d.isNaN())
            .orElse(0.);
    }

    public static Map<String, Double> computeCseBordersExchanges(Network network, Collection<PiSaLinkProcessor> piSaLinkProcessors) {
        return computeCseBordersExchanges(network, piSaLinkProcessors, true);
    }

    public static Map<String, Double> computeCseBordersExchanges(Network network, Collection<PiSaLinkProcessor> piSaLinkProcessors, boolean withLoadflow) {
        if (withLoadflow) {
            runLoadFlow(network);
        }
        Map<String, Double> borderExchanges = new HashMap<>();
        Map<Country, CountryArea> countryAreaPerCountry = Stream.of(Country.FR, Country.AT, Country.CH, Country.SI, Country.IT, Country.DE)
            .collect(Collectors.toMap(
                Function.identity(),
                country -> new CountryAreaFactory(country).create(network)
            ));
        borderExchanges.put(IT_CH, getBorderExchange(Country.IT, Country.CH, countryAreaPerCountry));
        borderExchanges.put(IT_FR, getBorderExchange(Country.IT, Country.FR, countryAreaPerCountry) - getPiSaFlowToItaly(network, piSaLinkProcessors));
        borderExchanges.put(IT_AT, getBorderExchange(Country.IT, Country.AT, countryAreaPerCountry));
        borderExchanges.put(IT_SI, getBorderExchange(Country.IT, Country.SI, countryAreaPerCountry));
        borderExchanges.put(CH_FR, getBorderExchange(Country.CH, Country.FR, countryAreaPerCountry));
        borderExchanges.put(FR_DE, getBorderExchange(Country.FR, Country.DE, countryAreaPerCountry));
        borderExchanges.put(CH_DE, getBorderExchange(Country.CH, Country.DE, countryAreaPerCountry));
        return borderExchanges;
    }

    private static double getBorderExchange(Country fromCountry, Country toCountry, Map<Country, CountryArea> countryAreaPerCountry) {
        return countryAreaPerCountry.get(fromCountry).getLeavingFlowToCountry(countryAreaPerCountry.get(toCountry));
    }

    public static Map<String, Double> computeCseCountriesBalances(Network network, Collection<PiSaLinkProcessor> piSaLinkProcessors) {
        runLoadFlow(network);
        Map<String, Double> countriesBalances = new HashMap<>();
        countriesBalances.put("AT", new CountryAreaFactory(Country.AT).create(network).getNetPosition());
        countriesBalances.put("CH", new CountryAreaFactory(Country.CH).create(network).getNetPosition());
        countriesBalances.put("FR", new CountryAreaFactory(Country.FR).create(network).getNetPosition() + getPiSaFlowToItaly(network, piSaLinkProcessors));
        countriesBalances.put("SI", new CountryAreaFactory(Country.SI).create(network).getNetPosition());
        countriesBalances.put("IT", new CountryAreaFactory(Country.IT).create(network).getNetPosition() - getPiSaFlowToItaly(network, piSaLinkProcessors));
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
