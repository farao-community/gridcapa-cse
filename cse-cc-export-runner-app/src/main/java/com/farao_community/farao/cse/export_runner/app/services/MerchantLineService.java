/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.export_runner.app.configurations.MendrisioConfiguration;
import com.farao_community.farao.cse.network_processing.ucte_pst_change.UctePstProcessor;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.PhaseTapChangerHolder;
import com.powsybl.iidm.network.VoltageLevel;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class MerchantLineService {

    private final UctePstProcessor uctePstProcessor;
    private final Logger businessLogger;
    private final String mendrisioVoltageLevel;

    public MerchantLineService(MendrisioConfiguration mendrisioConfiguration, Logger businessLogger) {
        this.businessLogger = businessLogger;
        this.mendrisioVoltageLevel = mendrisioConfiguration.getMendrisioVoltageLevel();
        this.uctePstProcessor = new UctePstProcessor(businessLogger,
                mendrisioConfiguration.getMendrisioVoltageLevel(),
                mendrisioConfiguration.getMendrisioNodeId());
    }

    public void setTransformerInActivePowerRegulation(Network network) {
        final Optional<VoltageLevel> optionalVoltageLevel = Optional.ofNullable(network.getVoltageLevel(mendrisioVoltageLevel));
        final Optional<Double> optionalRegulationValue = optionalVoltageLevel.flatMap(level -> level
                .getTwoWindingsTransformerStream()
                .filter(PhaseTapChangerHolder::hasPhaseTapChanger)
                .findAny()
                .map(PhaseTapChangerHolder::getPhaseTapChanger)
                .map(PhaseTapChanger::getRegulationValue));
        if (optionalRegulationValue.isEmpty() || Double.isNaN(optionalRegulationValue.get())) {
            businessLogger.warn("PST at voltage level {} cannot be put in flow regulation mode, because no target flow was available in input CGM", mendrisioVoltageLevel);
        } else {
            uctePstProcessor.setTransformerInActivePowerRegulation(network);
        }
    }
}
