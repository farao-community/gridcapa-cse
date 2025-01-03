/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.import_runner.app.configurations.MendrisioConfiguration;
import com.farao_community.farao.cse.network_processing.ucte_pst_change.UctePstProcessor;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class MerchantLineService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MerchantLineService.class);

    private final MendrisioConfiguration mendrisioConfiguration;
    private final UctePstProcessor uctePstProcessor;

    public MerchantLineService(MendrisioConfiguration mendrisioConfiguration, Logger businessLogger) {
        this.mendrisioConfiguration = mendrisioConfiguration;
        this.uctePstProcessor = new UctePstProcessor(businessLogger,
                mendrisioConfiguration.getMendrisioVoltageLevel(),
                mendrisioConfiguration.getMendrisioNodeId());
    }

    public void activateMerchantLine(ProcessType processType, Network network, CseData cseData) {
        if (processType == ProcessType.IDCC) {
            activateMerchantLineForIdcc(network, cseData);
        } else if (processType == ProcessType.D2CC) {
            activateMerchantLineForD2cc(network, cseData);
        } else {
            throw new CseInternalException(String.format("Process type %s is not handled", processType));
        }
    }

    private void activateMerchantLineForIdcc(Network network, CseData cseData) {
        double defaultFlow = getMendrisioTargetFlowForIdcc(cseData);
        uctePstProcessor.forcePhaseTapChangerInActivePowerRegulationForIdcc(network, defaultFlow);
    }

    private void activateMerchantLineForD2cc(Network network, CseData cseData) {
        double offset = Optional.ofNullable(network.getLoad(mendrisioConfiguration.getMendrisioNodeId() + "_load"))
            .map(Load::getP0)
            .orElse(0.);

        double mendrisioCagnoTargetFlow = getMendrisioTargetFlowForD2cc(network, cseData);
        double pstSetPoint = mendrisioCagnoTargetFlow + offset;

        uctePstProcessor.forcePhaseTapChangerInActivePowerRegulationForD2cc(network, pstSetPoint);
    }

    private double getMendrisioTargetFlowForD2cc(Network network, CseData cseData) {
        double defaultFlow = cseData.getNtc().getFlowOnFixedFlowLines().get(mendrisioConfiguration.getMendrisioCagnoNtcId());
        UcteNetworkAnalyzer ucteNetworkHelper = new UcteNetworkAnalyzer(
            network,
            new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        Optional<Double> reducedFlow = cseData.getLineFixedFlows().getFixedFlow(
            mendrisioConfiguration.getMendrisioCagnoTargetChId(),
            network,
            ucteNetworkHelper);
        double mendrisioCagnoTargetFlow = reducedFlow.isEmpty() ? defaultFlow : Math.min(defaultFlow, reducedFlow.get());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Target flow for Mendrisio-Cagno is %.0f MW", mendrisioCagnoTargetFlow));
        }
        return mendrisioCagnoTargetFlow;
    }

    private double getMendrisioTargetFlowForIdcc(CseData cseData) {
        double defaultFlow = cseData.getNtc().getFlowOnFixedFlowLines().get(mendrisioConfiguration.getMendrisioCagnoNtcId());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Target default flow for Mendrisio-Cagno is %.0f MW", defaultFlow));
        }
        return defaultFlow;
    }

}
