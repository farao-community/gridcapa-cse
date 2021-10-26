/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_cse.app.util;

import com.farao_community.farao.gridcapa_cse.api.exception.CseInternalException;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class ItalianImport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItalianImport.class);
    private static final List<Country> NORTHERN_ITALIAN_BORDERS = Arrays.asList(Country.FR, Country.AT, Country.CH, Country.SI);
    private static final double MAX_VALUE = 19999;

    private ItalianImport() {
        // Should not be instantiated
    }

    public static double compute(Network network) {
        LoadFlowResult result = LoadFlow.run(network, LoadFlowParameters.load());
        if (!result.isOk()) {
            LOGGER.error("Loadflow computation diverged on network '{}'", network.getId());
            throw new CseInternalException(String.format("Loadflow computation diverged on network %s", network.getId()));
        }
        double itBorderFlow = 0.;
        List<Branch> crossBordersOfCountryBranchList = network.getBranchStream().filter(ItalianImport::isBorderOfItaly).collect(Collectors.toList());
        for (Branch<?> branch : crossBordersOfCountryBranchList) {
            itBorderFlow += getItalyBorderFlow(branch);
        }
        return Math.min(itBorderFlow, MAX_VALUE);
    }

    public static double getItalyBorderFlow(Branch<?> branch) {
        double leavingFlow = 0;
        for (Branch.Side side : Branch.Side.values()) {
            Optional<Country> countrySide = branch.getTerminal(side).getVoltageLevel().getSubstation().get().getCountry();
            double flow = countrySide.isPresent() && countrySide.get().equals(Country.IT) ? -branch.getTerminal(side).getP() : branch.getTerminal(side).getP();
            if (!Double.isNaN(flow)) {
                leavingFlow += branch.getTerminal(side).isConnected() ? flow : 0; //in this branch one of the terminals is connected but P = Nan
            }
        }
        return leavingFlow / 2;
    }

    public static boolean isBorderOfItaly(Branch<?> branch) {
        Country side1Country = getCountrySide(branch, Branch.Side.ONE);
        Country side2Country = getCountrySide(branch, Branch.Side.TWO);
        return (side1Country == Country.IT && isCountrySideNotItButOneOfNorthernItalianBorder(side2Country)) ||
            (side2Country == Country.IT && isCountrySideNotItButOneOfNorthernItalianBorder(side1Country));

    }

    private static boolean isCountrySideNotItButOneOfNorthernItalianBorder(Country sideCountry) {
        return NORTHERN_ITALIAN_BORDERS.contains(sideCountry);
    }

    public static Country getCountrySide(Branch<?> branch, Branch.Side side) {
        return branch.getTerminal(side)
            .getVoltageLevel()
            .getSubstation()
            .orElseThrow(() -> new CseInternalException(String.format("Could not find country in side %s of branch : %s", side.name(), branch.getId())))
            .getNullableCountry();

    }
}
