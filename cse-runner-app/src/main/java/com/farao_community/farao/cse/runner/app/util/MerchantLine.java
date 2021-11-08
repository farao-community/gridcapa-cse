/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.util;

import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class MerchantLine {
    private static final Logger LOGGER = LoggerFactory.getLogger(MerchantLine.class);
    public static final String MENDRISIO_ID = "SMENDR3T SMENDR32 1";

    private MerchantLine() {
        // Should not be instantiated
    }

    public static void activateMerchantLine(ProcessType processType, Network network) {
        if (processType == ProcessType.IDCC) {
            activateMerchantLineForIdcc(network);
        } else if (processType == ProcessType.D2CC) {
            activateMerchantLineForD2cc(network);
        } else {
            throw new CseInternalException(String.format("Process type %s is not handled", processType));
        }
    }

    private static void activateMerchantLineForIdcc(Network network) {
        TwoWindingsTransformer mendrisioTransformer = network.getTwoWindingsTransformer(MENDRISIO_ID);
        if (mendrisioTransformer == null) {
            throw new CseInvalidDataException(String.format(
                "Mendrisio transformer is not present in the network with the following ID : %s", MENDRISIO_ID));
        }
        PhaseTapChanger phaseTapChanger = mendrisioTransformer.getPhaseTapChanger();
        if (phaseTapChanger == null) {
            throw new CseInvalidDataException(String.format(
                "Mendrisio transformer (%s) has no phase tap changer", MENDRISIO_ID));
        }
        // In UCTE format regulated terminal is node 2 but PowSyBl inverts transformer nodes in UCTE import
        phaseTapChanger.setRegulationTerminal(mendrisioTransformer.getTerminal1());
        phaseTapChanger.setRegulationValue(-phaseTapChanger.getRegulationValue());

        phaseTapChanger.setRegulationMode(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL);
        phaseTapChanger.setTargetDeadband(5);
        phaseTapChanger.setRegulating(true);
        LOGGER.info(String.format("Mendrisio PST (%s) has been set in active power control to %.0f MW",
            MENDRISIO_ID, phaseTapChanger.getRegulationValue()));
    }

    private static void activateMerchantLineForD2cc(Network network) {
        // TODO: Implement merchant line handling for D2CC
    }
}
