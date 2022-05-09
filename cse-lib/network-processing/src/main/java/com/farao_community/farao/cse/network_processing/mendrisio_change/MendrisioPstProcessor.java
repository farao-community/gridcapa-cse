/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.network_processing.mendrisio_change;

import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class MendrisioPstProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MendrisioPstProcessor.class);

    private final String mendrisioPstId;
    private final String mendrisioNodeId;

    public MendrisioPstProcessor(String mendrisioPstId, String mendrisioNodeId) {
        this.mendrisioPstId = mendrisioPstId;
        this.mendrisioNodeId = mendrisioNodeId;
    }

    public void forcePhaseTapChangerInActivePowerRegulation(Network network) {
        PhaseTapChanger phaseTapChanger = forceAndGetPhaseTapChangerInActivePowerRegulation(network);
        // PowSyBl transformer is inverted compared to UCTE transformer so we have to set opposite sign
        phaseTapChanger.setRegulationValue(-phaseTapChanger.getRegulationValue());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Mendrisio PST (%s) has been set in active power control to %.0f MW",
                mendrisioPstId, phaseTapChanger.getRegulationValue()));
        }
    }

    public void forcePhaseTapChangerInActivePowerRegulation(Network network, double regulationValue) {
        PhaseTapChanger phaseTapChanger = forceAndGetPhaseTapChangerInActivePowerRegulation(network);
        // PowSyBl transformer is inverted compared to UCTE transformer so we have to set opposite sign
        phaseTapChanger.setRegulationValue(regulationValue);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Mendrisio PST (%s) has been set in active power control to %.0f MW",
                mendrisioPstId, phaseTapChanger.getRegulationValue()));
        }
    }

    private PhaseTapChanger forceAndGetPhaseTapChangerInActivePowerRegulation(Network network) {
        TwoWindingsTransformer mendrisioTransformer = network.getTwoWindingsTransformer(mendrisioPstId);
        if (mendrisioTransformer == null) {
            throw new MendrisioException(String.format(
                "Mendrisio transformer is not present in the network with the following ID : %s", mendrisioPstId));
        }
        PhaseTapChanger phaseTapChanger = mendrisioTransformer.getPhaseTapChanger();
        if (phaseTapChanger == null) {
            throw new MendrisioException(String.format(
                "Mendrisio transformer (%s) has no phase tap changer", mendrisioPstId));
        }

        phaseTapChanger.setRegulationTerminal(getRegulatedTerminal(mendrisioTransformer));
        phaseTapChanger.setRegulationMode(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL);
        phaseTapChanger.setTargetDeadband(5);
        phaseTapChanger.setRegulating(true);
        return phaseTapChanger;
    }

    /**
     * We want to make sure to set regulation on the good terminal with the following convention:
     * positive set-point on mendrisio PST will cause a positive flow on mendrisio-cagno line.
     * As mendrisio node is in-between mendrisio-cagno line and mendrisio PST, we don't want to use this terminal,
     * because sign of set-point would be inverted.
     *
     * @param mendrisioTransformer: Mendrisio tranformer on which to find proper terminal.
     * @return The terminal of Mendrisio PST that on the opposite side of the mendrisio-cagno line.
     */
    private Terminal getRegulatedTerminal(TwoWindingsTransformer mendrisioTransformer) {
        if (mendrisioTransformer.getTerminal1().getBusBreakerView().getConnectableBus().getId().equals(mendrisioNodeId)) {
            return mendrisioTransformer.getTerminal2();
        } else {
            return mendrisioTransformer.getTerminal1();
        }
    }
}
