/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.network_processing.ucte_pst_change;

import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utility class prevent an issue that comes from PowSyBl UCTE importer, because regulation in active power
 * is not correctly set.
 * In CSE computations this should only apply to mendridio PST, but it can theoretically be used on other PSTs.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class UctePstProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(UctePstProcessor.class);

    private final String pstId;
    private final String nodeId;

    public UctePstProcessor(String pstId, String nodeId) {
        this.pstId = pstId;
        this.nodeId = nodeId;
    }

    public void forcePhaseTapChangerInActivePowerRegulation(Network network) {
        PhaseTapChanger phaseTapChanger = forceAndGetPhaseTapChangerInActivePowerRegulation(network);
        // PowSyBl transformer is inverted compared to UCTE transformer so we have to set opposite sign
        phaseTapChanger.setRegulationValue(-phaseTapChanger.getRegulationValue());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("PST (%s) has been set in active power control to %.0f MW",
                pstId, phaseTapChanger.getRegulationValue()));
        }
    }

    public void forcePhaseTapChangerInActivePowerRegulationIdcc(Network network, double defaultRegulationValue) {
        PhaseTapChanger phaseTapChanger = forceAndGetPhaseTapChangerInActivePowerRegulation(network);
        // PowSyBl transformer is inverted compared to UCTE transformer so we have to set opposite sign
        double regulationValue = -phaseTapChanger.getRegulationValue();
        if (phaseTapChanger.getRegulationValue() == 0.0) {
            regulationValue = defaultRegulationValue;
        }
        phaseTapChanger.setRegulationValue(regulationValue);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("PST (%s) has been set in active power control to %.0f MW",
                    pstId, phaseTapChanger.getRegulationValue()));
        }
    }

    public void forcePhaseTapChangerInActivePowerRegulation(Network network, double regulationValue) {
        PhaseTapChanger phaseTapChanger = forceAndGetPhaseTapChangerInActivePowerRegulation(network);
        // PowSyBl transformer is inverted compared to UCTE transformer so we have to set opposite sign
        phaseTapChanger.setRegulationValue(regulationValue);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("PST (%s) has been set in active power control to %.0f MW",
                pstId, phaseTapChanger.getRegulationValue()));
        }
    }

    private PhaseTapChanger forceAndGetPhaseTapChangerInActivePowerRegulation(Network network) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(pstId);
        if (transformer == null) {
            throw new UctePstException(String.format(
                "Transformer is not present in the network with the following ID : %s", pstId));
        }
        PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
        if (phaseTapChanger == null) {
            throw new UctePstException(String.format(
                "Transformer (%s) has no phase tap changer", pstId));
        }

        phaseTapChanger.setRegulationTerminal(getRegulatedTerminal(transformer));
        phaseTapChanger.setRegulationMode(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL);
        phaseTapChanger.setTargetDeadband(5);
        phaseTapChanger.setRegulating(true);
        return phaseTapChanger;
    }

    /**
     * We want to make sure to set regulation on the good terminal with the following convention:
     * positive set-point on PST will cause a positive flow on from the specified {@code nodeId}.
     *
     * @param transformer: Transformer on which to find proper terminal.
     * @return The terminal of the PST corresponding to the specified node.
     */
    private Terminal getRegulatedTerminal(TwoWindingsTransformer transformer) {
        if (transformer.getTerminal1().getBusBreakerView().getConnectableBus().getId().equals(nodeId)) {
            return transformer.getTerminal2();
        } else {
            return transformer.getTerminal1();
        }
    }
}
