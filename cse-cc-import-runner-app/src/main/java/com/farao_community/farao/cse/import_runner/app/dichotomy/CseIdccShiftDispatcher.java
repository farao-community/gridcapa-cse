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
    private final Map<String, Double> ntcs;

    private final double referenceItalianImport;
    private final double ntcSum;
    private int shiftCounter = 0;

    public CseIdccShiftDispatcher(Logger businessLogger,
                                  Map<String, Double> ntcs,
                                  Map<String, Double> splittingFactors,
                                  Map<String, Double> referenceExchanges) {
        this.businessLogger = businessLogger;
        this.ntcs = ntcs;
        this.splittingFactors = splittingFactors;
        this.referenceExchanges = referenceExchanges;

        referenceItalianImport = NetworkShifterUtil.getReferenceItalianImport(referenceExchanges);
        ntcSum = ntcs.values().stream().reduce(0., Double::sum);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Italian import reference: %.2f", referenceItalianImport));
            LOGGER.info(String.format("Italian import in D2: %.2f", ntcSum));
        }
    }

    @Override
    public Map<String, Double> dispatch(double value) {
        Map<String, Double> shifts;
        if (value <= ntcSum && value > referenceItalianImport) {
            LOGGER.debug("Computing a shift proportional to D2 ATC.");
            shifts = getShiftsProportionallyToAtcCapacity(value);
        } else if (value <= referenceItalianImport) {
            LOGGER.debug("Computing a shift proportional to D2 capacity.");
            shifts = getShiftsProportionallyToNtcCapacity(value);
        } else {
            LOGGER.debug("Computing a shift proportional to splitting factors above D2 capacity.");
            shifts = getShiftsAboveAtcProportionallyToSplittingFactors(value);
        }
        logShifts(value, shifts);
        shiftCounter++;
        return shifts;
    }

    private Map<String, Double> getShiftsProportionallyToNtcCapacity(double targetValue) {
        Map<String, Double> shifts = new HashMap<>();
        double exportingCountriesNtcs = ntcs.values().stream().filter(ntc2 -> ntc2 > 0).reduce(0., Double::sum);
        BORDER_COUNTRIES.forEach(country -> {
            if (ntcs.get(country) <= 0) {
                shifts.put(country, -(ntcs.get(country) - referenceExchanges.get(country)));
            } else {
                shifts.put(country,
                    (((targetValue - referenceItalianImport) / exportingCountriesNtcs) * ntcs.get(country)) - (ntcs.get(country) - referenceExchanges.get(country))
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
                (targetValue - ntcSum) *
                    ((ntcs.get(country) - referenceExchanges.get(country)) / (ntcSum - referenceItalianImport))

            ));

        shifts.put(CseCountry.IT.getEiCode(),
            -shifts.values().stream().mapToDouble(Double::doubleValue).sum());
        return shifts;
    }

    private Map<String, Double> getShiftsAboveAtcProportionallyToSplittingFactors(double targetValue) {
        Map<String, Double> shifts = new HashMap<>();
        // Each country shifts to NTC2 value from reference and then completes proportionally to splitting factors
        BORDER_COUNTRIES.forEach(country ->
            shifts.put(country, splittingFactors.get(country) * (targetValue - ntcs.values().stream().mapToDouble(Double::doubleValue).sum())));
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
