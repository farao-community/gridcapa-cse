/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CseIdccShiftDispatcher implements ShiftDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CseIdccShiftDispatcher.class);
    private static final Set<String> BORDER_COUNTRIES = Set.of(CseCountry.FR.getEiCode(), CseCountry.CH.getEiCode(), CseCountry.AT.getEiCode(), CseCountry.SI.getEiCode());

    private final Map<String, Double> splittingFactors;
    private final Map<String, Double> referenceExchanges;
    private final Map<String, Double> ntcs2;
    private final double referenceItalianImport;
    private final double ntc2ItalianImport;

    public CseIdccShiftDispatcher(Map<String, Double> splittingFactors, Map<String, Double> referenceExchanges, Map<String, Double> ntcs2) {
        this.splittingFactors = splittingFactors;
        this.referenceExchanges = referenceExchanges;
        this.ntcs2 = ntcs2;
        referenceItalianImport = NetworkShifterUtil.getReferenceItalianImport(referenceExchanges);
        ntc2ItalianImport = ntcs2.values().stream().reduce(0., Double::sum);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Italian import reference: %.2f", referenceItalianImport));
            LOGGER.info(String.format("Italian import in D2: %.2f", ntc2ItalianImport));
        }
    }

    @Override
    public Map<String, Double> dispatch(double value) throws ShiftingException {
        if (value < referenceItalianImport) {
            LOGGER.debug("Computing a shift proportional to D2 capacity.");
            return getShiftsProportionallyToNtc2Capacity(value);
        } else if (value < ntc2ItalianImport) {
            LOGGER.debug("Computing a shift proportional to D2 ATC.");
            return getShiftsProportionallyToAtcCapacity(value);
        } else {
            LOGGER.debug("Computing a shift proportional to splitting factors above D2 capacity.");
            return getShiftsAboveAtcProportionallyToSplittingFactors(value);
        }
    }

    private Map<String, Double> initWithItalianShift(double targetValue) {
        Map<String, Double> shifts = new HashMap<>();
        shifts.put(CseCountry.IT.getEiCode(), referenceItalianImport - targetValue);
        return shifts;
    }

    private Map<String, Double> getShiftsProportionallyToNtc2Capacity(double targetValue) throws ShiftingException {
        if (ntcs2.values().stream().allMatch(v -> v < 0)) {
            throw new ShiftingException("Negative NTC2 for all countries of the area and target import inferior to reference exchanges.");
        }
        Map<String, Double> shifts = initWithItalianShift(targetValue);

        // Just making sure to take only exporting countries for D-2 prevision but it should be the case for all
        double exportingCountriesNtc2 = ntcs2.values().stream().filter(ntc2 -> ntc2 > 0).reduce(0., Double::sum);
        BORDER_COUNTRIES.forEach(country -> {
            if (ntcs2.get(country) <= 0) {
                shifts.put(country, 0.);
            } else {
                shifts.put(country,
                        (targetValue - referenceItalianImport) * ntcs2.get(country) / exportingCountriesNtc2
                );
            }
        });
        return shifts;
    }

    private Map<String, Double> getShiftsProportionallyToAtcCapacity(double targetValue) {
        Map<String, Double> shifts = initWithItalianShift(targetValue);

        // Each country shifts proportionally to its capacity between reference and NTC2 value
        BORDER_COUNTRIES.forEach(country ->
                shifts.put(country,
                        (targetValue - referenceItalianImport) *
                                (ntcs2.get(country) - referenceExchanges.get(country)) /
                                (ntc2ItalianImport - referenceItalianImport)
                ));
        return shifts;
    }

    private Map<String, Double> getShiftsAboveAtcProportionallyToSplittingFactors(double targetValue) {
        Map<String, Double> shifts = initWithItalianShift(targetValue);

        // Each country shifts to NTC2 value from reference and then completes proportionally to splitting factors
        BORDER_COUNTRIES.forEach(country ->
            shifts.put(country,
                    ntcs2.get(country) - referenceExchanges.get(country) +
                            splittingFactors.get(country) * (targetValue - ntc2ItalianImport)
            ));
        return shifts;
    }
}
