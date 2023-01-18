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

    public void forcePhaseTapChangerInActivePowerRegulationForIdcc(Network network, double defaultRegulationValue) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(pstId);
        PhaseTapChanger phaseTapChanger = getPhaseTapChanger(transformer);
        // PowSyBl transformer is inverted compared to UCTE transformer so we have to set opposite sign
        double regulationValue = -phaseTapChanger.getRegulationValue();
        if (Double.isNaN(phaseTapChanger.getRegulationValue())) {
            regulationValue = defaultRegulationValue;
        }
        setTransformerInActivePowerRegulation(transformer, phaseTapChanger, regulationValue);
    }

    public void forcePhaseTapChangerInActivePowerRegulationForD2cc(Network network, double regulationValue) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(pstId);
        PhaseTapChanger phaseTapChanger = getPhaseTapChanger(transformer);
        setTransformerInActivePowerRegulation(transformer, phaseTapChanger, regulationValue);
    }

    private void setTransformerInActivePowerRegulation(TwoWindingsTransformer transformer, PhaseTapChanger phaseTapChanger, double regulationValue) {
        phaseTapChanger.setRegulationValue(regulationValue); // Powsybl iidm model requirement: regulationValue must be not empty in order to activate regulation mode
        phaseTapChanger.setRegulationTerminal(getRegulatedTerminal(transformer));
        phaseTapChanger.setRegulationMode(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL);
        phaseTapChanger.setTargetDeadband(5);
        phaseTapChanger.setRegulating(true);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("PST (%s) has been set in active power control to %.0f MW",
                pstId, phaseTapChanger.getRegulationValue()));
        }
    }

    private PhaseTapChanger getPhaseTapChanger(TwoWindingsTransformer transformer) {
        if (transformer == null) {
            throw new UctePstException(String.format(
                "Transformer is not present in the network with the following ID : %s", pstId));
        }
        PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
        if (phaseTapChanger == null) {
            throw new UctePstException(String.format(
                "Transformer (%s) has no phase tap changer", pstId));
        }
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

    /**
     * Regulation value initially inverted in preprocessing for IDCC process to prevent an issue that comes from PowSyBl UCTE importer
     * should be re-inverted when exporting UCTE network
     */
    public void invertRegulationValueForIdcc(Network network) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(pstId);
        PhaseTapChanger phaseTapChanger = getPhaseTapChanger(transformer);
        phaseTapChanger.setRegulationValue(-phaseTapChanger.getRegulationValue());
    }
}
