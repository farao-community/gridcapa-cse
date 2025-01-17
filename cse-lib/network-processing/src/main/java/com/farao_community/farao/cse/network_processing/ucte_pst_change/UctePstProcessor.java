/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.network_processing.ucte_pst_change;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.PhaseTapChangerHolder;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * In CSE computations this should only apply to mendrisio PST, but it can theoretically be used on other PSTs.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class UctePstProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(UctePstProcessor.class);
    private final Logger businessLogger;
    private final String voltageLevel;
    private final String nodeId;
    private final String warnMessageNoPstAtVoltageLevel;

    public UctePstProcessor(Logger businessLogger, String voltageLevel, String nodeId) {
        this.businessLogger = businessLogger;
        this.voltageLevel = voltageLevel;
        this.nodeId = nodeId;
        this.warnMessageNoPstAtVoltageLevel = String.format("No PST at voltage level (%s) has been found.", voltageLevel);
    }

    public void forcePhaseTapChangerInActivePowerRegulationForIdcc(Network network, double defaultRegulationValue) {
        TwoWindingsTransformer transformer = getTwoWindingsTransformerAtVoltageLevelWithPhaseTapChanger(network);
        if (transformer != null) {
            PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
            double regulationValue = phaseTapChanger.getRegulationValue();
            if (Double.isNaN(regulationValue)) {
                phaseTapChanger.setRegulationValue(defaultRegulationValue); // Powsybl iidm model requirement: regulationValue must be not empty in order to activate regulation mode
            }
            setTransformerInActivePowerRegulation(transformer, phaseTapChanger);
        } else {
            businessLogger.warn(warnMessageNoPstAtVoltageLevel);
        }
    }

    public void forcePhaseTapChangerInActivePowerRegulationForD2cc(Network network, double regulationValue) {
        TwoWindingsTransformer transformer = getTwoWindingsTransformerAtVoltageLevelWithPhaseTapChanger(network);
        if (transformer != null) {
            PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
            phaseTapChanger.setRegulationValue(regulationValue);
            setTransformerInActivePowerRegulation(transformer, phaseTapChanger);
        } else {
            businessLogger.warn(warnMessageNoPstAtVoltageLevel);
        }
    }

    private TwoWindingsTransformer getTwoWindingsTransformerAtVoltageLevelWithPhaseTapChanger(final Network network) {
        final Optional<VoltageLevel> optionalVoltageLevel = Optional.ofNullable(network.getVoltageLevel(voltageLevel));
        return optionalVoltageLevel.flatMap(a -> a.getTwoWindingsTransformerStream()
                .filter(PhaseTapChangerHolder::hasPhaseTapChanger)
                .findAny()).orElse(null);
    }

    private void setTransformerInActivePowerRegulation(TwoWindingsTransformer transformer, PhaseTapChanger phaseTapChanger) {
        phaseTapChanger.setRegulationTerminal(getRegulatedTerminal(transformer));
        phaseTapChanger.setRegulationMode(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL);
        phaseTapChanger.setTargetDeadband(5);
        phaseTapChanger.setRegulating(true);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("PST at voltage level (%s) has been set in active power control to %.0f MW",
                    voltageLevel, phaseTapChanger.getRegulationValue()));
        }
    }

    public void setTransformerInActivePowerRegulation(Network network) {
        TwoWindingsTransformer transformer = getTwoWindingsTransformerAtVoltageLevelWithPhaseTapChanger(network);
        if (transformer != null) {
            PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
            setTransformerInActivePowerRegulation(transformer, phaseTapChanger);
        } else {
            businessLogger.warn(warnMessageNoPstAtVoltageLevel);
        }
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
