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
public class CseD2ccShiftDispatcher implements ShiftDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CseD2ccShiftDispatcher.class);
    private static final Set<String> BORDER_COUNTRIES = Set.of(CseCountry.FR.getEiCode(), CseCountry.CH.getEiCode(), CseCountry.AT.getEiCode(), CseCountry.SI.getEiCode());

    private final Map<String, Double> reducedSplittingFactors;
    private final Map<String, Double> referenceExchanges;
    private final Map<String, Double> flowOnMerchantLinesPerCountry;

    public CseD2ccShiftDispatcher(Map<String, Double> reducedSplittingFactors, Map<String, Double> referenceExchanges, Map<String, Double> flowOnMerchantLinesPerCountry) {
        this.reducedSplittingFactors = reducedSplittingFactors;
        this.referenceExchanges = referenceExchanges;
        this.flowOnMerchantLinesPerCountry = flowOnMerchantLinesPerCountry;
    }

    @Override
    public Map<String, Double> dispatch(double value) {
        Map<String, Double> shifts = new HashMap<>();
        double referenceItalianImport = referenceExchanges.values().stream().reduce(0., Double::sum);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Italian import reference: %.2f", referenceItalianImport));
        }
        shifts.put(CseCountry.IT.getEiCode(), referenceItalianImport - value);
        Double reducedTarget = value - flowOnMerchantLinesPerCountry.values().stream().reduce(0., Double::sum);
        BORDER_COUNTRIES.forEach(borderCountry ->
            shifts.put(borderCountry, reducedSplittingFactors.get(borderCountry) * reducedTarget
                + flowOnMerchantLinesPerCountry.get(borderCountry)
                - referenceExchanges.get(borderCountry))
        );
        return shifts;
    }
}
