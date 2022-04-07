/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.util;

import com.farao_community.farao.cse.computation.CseComputationException;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.runner.app.CseData;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzerProperties;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public final class MerchantLine {
    private static final Logger LOGGER = LoggerFactory.getLogger(MerchantLine.class);
    static final String MENDRISIO_ID = "SMENDR3T SMENDR32 1";
    static final String MENDRISIO_CAGNO_CODE_IN_NTC_FILE = "ml_mendrisio-cagno";
    private static final String MENDRISIO_CAGNO_ID_IN_NETWORK = "SMENDR11 XME_CA11 1";
    private static final String MENDRISIO_CAGNO_CODE_IN_TARGET_CH_FILE = "ml_0001";

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

    private static void activateMerchantLineForD2cc(Network network, CseData cseData) {
        runLoadFlow(network);
        double defaultFlow = cseData.getNtc().getFlowOnFixedFlowLines().get(MENDRISIO_CAGNO_CODE_IN_NTC_FILE);
        UcteNetworkAnalyzer ucteNetworkHelper = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        Optional<Double> reducedFlow = cseData.getLineFixedFlows().getFixedFlow(MENDRISIO_CAGNO_CODE_IN_TARGET_CH_FILE, network, ucteNetworkHelper);
        double mendrisioCagnoTargetFlow = reducedFlow.isEmpty() ? defaultFlow : Math.min(defaultFlow, reducedFlow.get());

        LOGGER.info(String.format("Target flow for Mendrisio-Cagno is %.0f MW", mendrisioCagnoTargetFlow));

        // Offset Calculation
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

        double mendrisioPstFlow = phaseTapChanger.getRegulationValue();
        Optional<Line> mendrisioCagnoLine = network.getLineStream().filter(line -> line.getId().contains(MENDRISIO_CAGNO_ID_IN_NETWORK)).findFirst();
        if (mendrisioCagnoLine.isEmpty()) {
            throw new CseInvalidDataException(String.format(
                    "Mendrisio Cagno line is not present in the network with the following ID : %s", MENDRISIO_CAGNO_ID_IN_NETWORK));
        }
        double mendrisioCagnoFlow = mendrisioCagnoLine.get().getTerminal1().getP();
        double offset = mendrisioCagnoFlow - mendrisioPstFlow;

        double pstSetPoint = mendrisioCagnoTargetFlow - offset;

        phaseTapChanger.setRegulationTerminal(mendrisioTransformer.getTerminal1());
        phaseTapChanger.setRegulationValue(pstSetPoint);
        phaseTapChanger.setRegulationMode(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL);
        phaseTapChanger.setTargetDeadband(5);
        phaseTapChanger.setRegulating(true);
        LOGGER.info(String.format("Mendrisio PST (%s) has been set in active power control to %.0f MW",
                MENDRISIO_ID, pstSetPoint));
    }

    private static void runLoadFlow(Network network) {
        LoadFlowResult result = LoadFlow.run(network, LoadFlowParameters.load());
        if (!result.isOk()) {
            LOGGER.error("Loadflow computation diverged on network '{}'", network.getId());
            throw new CseComputationException(String.format("Loadflow computation diverged on network %s", network.getId()));
        }
    }
}
