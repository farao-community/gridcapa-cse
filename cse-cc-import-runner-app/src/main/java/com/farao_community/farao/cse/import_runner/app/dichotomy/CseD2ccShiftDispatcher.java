/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CseD2ccShiftDispatcher implements ShiftDispatcher {
    private static final Set<String> BORDER_COUNTRIES = Set.of(CseCountry.FR.getEiCode(), CseCountry.CH.getEiCode(), CseCountry.AT.getEiCode(), CseCountry.SI.getEiCode());

    private final Logger businessLogger;
    private final Map<String, Double> reducedSplittingFactors;
    private final Map<String, Double> ntcsByEic;
    private final Map<String, Double> referenceExchanges;
    private final Map<String, Double> flowOnNotModelledLinesPerCountry;

    private int shiftCounter = 0;
    Map<String, Double> initialShifts = new HashMap<>();
    private double previousTarget = 0.;
    private double sumOfPreviousSteps = 0.;

    public CseD2ccShiftDispatcher(Logger businessLogger, Map<String, Double> ntcsByEic, Map<String, Double> reducedSplittingFactors, Map<String, Double> referenceExchanges, Map<String, Double> flowOnNotModelledLinesPerCountry) {
        this.businessLogger = businessLogger;
        this.ntcsByEic = ntcsByEic;
        this.reducedSplittingFactors = reducedSplittingFactors;
        this.referenceExchanges = referenceExchanges;
        this.flowOnNotModelledLinesPerCountry = flowOnNotModelledLinesPerCountry;
    }

    @Override
    public Map<String, Double> dispatch(double value) {
        if (shiftCounter == 0) {
            // Calculate initial shifts for IT borders
            BORDER_COUNTRIES.forEach(country -> {
                double initialShift = ntcsByEic.get(country)
                    - referenceExchanges.get(country)
                    - flowOnNotModelledLinesPerCountry.get(country);
                initialShifts.put(country, initialShift);
            });

            // Calculate initial shift for IT
            double itShift = -initialShifts.values().stream().mapToDouble(Double::doubleValue).sum();
            initialShifts.put(CseCountry.IT.getEiCode(), itShift);
            shiftCounter++;
            businessLogger.info("Initial shift: Step value is {}", value);
            for (Map.Entry<String, Double> entry : initialShifts.entrySet()) {
                businessLogger.info("Initial shift for {} is {}.", entry.getKey(), entry.getValue());
            }

            previousTarget = value;
            return initialShifts;
        }

        sumOfPreviousSteps = sumOfPreviousSteps + value - previousTarget;
        // Calculate shifts for IT borders
        Map<String, Double> shifts = new HashMap<>();
        BORDER_COUNTRIES.forEach(country -> {
            double newShift = initialShifts.get(country)
                + reducedSplittingFactors.get(country) * sumOfPreviousSteps;
            shifts.put(country, newShift);
        });
        // Calculate shift for IT
        double itShift = -shifts.values().stream().mapToDouble(Double::doubleValue).sum();
        shifts.put(CseCountry.IT.getEiCode(), itShift);

        businessLogger.info("Shift iteration number: {}, step value is {}", shiftCounter, value);
        for (Map.Entry<String, Double> entry : shifts.entrySet()) {
            businessLogger.info("Shift for {} is {}.", entry.getKey(), entry.getValue());
        }

        shiftCounter++;
        previousTarget = value;
        return shifts;
    }

}

