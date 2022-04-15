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
import com.farao_community.farao.cse.runner.app.CseData;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzerProperties;
import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public final class MerchantLine {
    private static final Logger LOGGER = LoggerFactory.getLogger(MerchantLine.class);

    static final String MENDRISIO_CAGNO_CODE_IN_TARGET_CH_FILE = "ml_0001";
    static final String MENDRISIO_ID = "SMENDR3T SMENDR32 1";
    static final String MENDRISIO_NODE = "SMENDR3T";
    static final String MENDRISIO_CAGNO_CODE_IN_NTC_FILE = "ml_mendrisio-cagno";

    private MerchantLine() {
        // Should not be instantiated
    }

    public static void activateMerchantLine(ProcessType processType, Network network, CseData cseData) {
        if (processType == ProcessType.IDCC) {
            activateMerchantLineForIdcc(network);
        } else if (processType == ProcessType.D2CC) {
            activateMerchantLineForD2cc(network, cseData);
        } else {
            throw new CseInternalException(String.format("Process type %s is not handled", processType));
        }
    }

    private static void activateMerchantLineForIdcc(Network network) {
        PhaseTapChanger phaseTapChanger = forcePhaseTapChangerInActivePowerRegulation(network);
        // PowSyBl transformer is inverted compared to UCTE transformer so we have to set opposite sign
        phaseTapChanger.setRegulationValue(-phaseTapChanger.getRegulationValue());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Mendrisio PST (%s) has been set in active power control to %.0f MW",
                MENDRISIO_ID, phaseTapChanger.getRegulationValue()));
        }
    }

    private static void activateMerchantLineForD2cc(Network network, CseData cseData) {
        PhaseTapChanger phaseTapChanger = forcePhaseTapChangerInActivePowerRegulation(network);

        double offset = Optional.ofNullable(network.getLoad(MENDRISIO_NODE + "_load"))
            .map(Load::getP0)
            .orElse(0.);

        double mendrisioCagnoTargetFlow = getMendrisioTargetFlowForD2cc(network, cseData);
        double pstSetPoint = mendrisioCagnoTargetFlow + offset;

        phaseTapChanger.setRegulationValue(pstSetPoint);
        LOGGER.info(String.format("Mendrisio PST (%s) has been set in active power control to %.0f MW",
                MENDRISIO_ID, pstSetPoint));
    }

    private static double getMendrisioTargetFlowForD2cc(Network network, CseData cseData) {
        double defaultFlow = cseData.getNtc().getFlowOnFixedFlowLines().get(MENDRISIO_CAGNO_CODE_IN_NTC_FILE);
        UcteNetworkAnalyzer ucteNetworkHelper = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        Optional<Double> reducedFlow = cseData.getLineFixedFlows().getFixedFlow(MENDRISIO_CAGNO_CODE_IN_TARGET_CH_FILE, network, ucteNetworkHelper);
        double mendrisioCagnoTargetFlow = reducedFlow.isEmpty() ? defaultFlow : Math.min(defaultFlow, reducedFlow.get());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Target flow for Mendrisio-Cagno is %.0f MW", mendrisioCagnoTargetFlow));
        }
        return mendrisioCagnoTargetFlow;
    }

    private static PhaseTapChanger forcePhaseTapChangerInActivePowerRegulation(Network network) {
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
    private static Terminal getRegulatedTerminal(TwoWindingsTransformer mendrisioTransformer) {
        if (mendrisioTransformer.getTerminal1().getBusBreakerView().getBus().getId().equals(MENDRISIO_NODE)) {
            return mendrisioTransformer.getTerminal2();
        } else {
            return mendrisioTransformer.getTerminal1();
        }
    }
}
