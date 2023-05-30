/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

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

    private final Logger businessLogger;
    private final Map<String, Double> splittingFactors;
    private final Map<String, Double> referenceExchanges;
    private final Map<String, Double> ntcs2;

    private final double referenceItalianImport;
    private final double ntc2ItalianImport;
    private int shiftCounter = 0;

    public CseIdccShiftDispatcher(Logger businessLogger,
                                  Map<String, Double> ntcs2,
                                  Map<String, Double> splittingFactors,
                                  Map<String, Double> referenceExchanges) {
        this.businessLogger = businessLogger;
        this.ntcs2 = ntcs2;
        this.splittingFactors = splittingFactors;
        this.referenceExchanges = referenceExchanges;

        referenceItalianImport = NetworkShifterUtil.getReferenceItalianImport(referenceExchanges);
        ntc2ItalianImport = ntcs2.values().stream().reduce(0., Double::sum);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Italian import reference: %.2f", referenceItalianImport));
            LOGGER.info(String.format("Italian import in D2: %.2f", ntc2ItalianImport));
        }
    }

    @Override
    public Map<String, Double> dispatch(double value) {
        Map<String, Double> shifts;
        if (value <= ntc2ItalianImport && value > referenceItalianImport) {
            LOGGER.debug("Computing a shift proportional to D2 ATC.");
            shifts = getShiftsProportionallyToAtcCapacity(value);
        } else if (value <= referenceItalianImport) {   // stepvalue 4951;  ntc2 = 5601; vulcanus = 5488
            LOGGER.debug("Computing a shift proportional to D2 capacity.");
            shifts = getShiftsProportionallyToNtc2Capacity(value);
        } else {
            LOGGER.debug("Computing a shift proportional to splitting factors above D2 capacity.");
            shifts = getShiftsAboveAtcProportionallyToSplittingFactors(value);
        }
        logShifts(value, shifts);
        shiftCounter++;
        return shifts;
    }

    private Map<String, Double> getShiftsProportionallyToNtc2Capacity(double targetValue) {
        Map<String, Double> shifts = new HashMap<>();
        double exportingCountriesNtc2 = ntcs2.values().stream().filter(ntc2 -> ntc2 > 0).reduce(0., Double::sum);
        BORDER_COUNTRIES.forEach(country -> {
            if (ntcs2.get(country) <= 0) {
                shifts.put(country, -(ntcs2.get(country) - referenceExchanges.get(country)));
            } else {
                shifts.put(country,
                    (((targetValue - referenceItalianImport) / exportingCountriesNtc2) * ntcs2.get(country)) - (ntcs2.get(country) - referenceExchanges.get(country))
                );
            }
        });
        shifts.put(CseCountry.IT.getEiCode(),
            -shifts.values().stream().mapToDouble(Double::doubleValue).sum());
        return shifts;
    }

    private Map<String, Double> getShiftsProportionallyToAtcCapacity(double targetValue) {
        // ATC = diff between ntc2 and ref exchange(vulcanus)
        // Each country shifts proportionally to its capacity between reference and NTC2 value
        Map<String, Double> shifts = new HashMap<>();
        BORDER_COUNTRIES.forEach(country ->
            shifts.put(country,
                (targetValue - ntc2ItalianImport) *
                    ((ntcs2.get(country) - referenceExchanges.get(country)) / (ntc2ItalianImport - referenceItalianImport))
                //
            ));

        shifts.put(CseCountry.IT.getEiCode(),
            -shifts.values().stream().mapToDouble(Double::doubleValue).sum());
        return shifts;
    }

    private Map<String, Double> getShiftsAboveAtcProportionallyToSplittingFactors(double targetValue) {
        Map<String, Double> shifts = new HashMap<>();
        // Each country shifts to NTC2 value from reference and then completes proportionally to splitting factors
        BORDER_COUNTRIES.forEach(country ->
            shifts.put(country, splittingFactors.get(country) * (targetValue - ntcs2.values().stream().mapToDouble(Double::doubleValue).sum())));
        shifts.put(CseCountry.IT.getEiCode(),
            -shifts.values().stream().mapToDouble(Double::doubleValue).sum());
        return shifts;
    }

    private void logShifts(double value, Map<String, Double> shifts) {
        businessLogger.info("Summary : Shift iteration: {}, step value is {}", shiftCounter + 1, value);
        for (Map.Entry<String, Double> entry : shifts.entrySet()) {
            businessLogger.info("Summary : Shift iteration: {}, for area {} : {}.", shiftCounter + 1, entry.getKey(), entry.getValue());
        }
    }
}
