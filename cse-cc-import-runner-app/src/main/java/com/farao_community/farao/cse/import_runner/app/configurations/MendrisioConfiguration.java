/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@ConfigurationProperties(prefix = "cse-cc-runner.mendrisio")
public class MendrisioConfiguration {
    private final MendrisioCagnoLineProperties mendrisioCagnoLine;
    private final String mendrisioVoltageLevel;
    private final String mendrisioNodeId;

    public MendrisioConfiguration(MendrisioCagnoLineProperties mendrisioCagnoLine, String mendrisioVoltageLevel, String mendrisioNodeId) {
        this.mendrisioCagnoLine = mendrisioCagnoLine;
        this.mendrisioVoltageLevel = mendrisioVoltageLevel;
        this.mendrisioNodeId = mendrisioNodeId;
    }

    public MendrisioCagnoLineProperties getMendrisioCagnoLine() {
        return mendrisioCagnoLine;
    }

    public String getMendrisioVoltageLevel() {
        return mendrisioVoltageLevel;
    }

    public String getMendrisioNodeId() {
        return mendrisioNodeId;
    }

    public String getMendrisioCagnoTargetChId() {
        return mendrisioCagnoLine.getTargetChId();
    }

    public String getMendrisioCagnoNtcId() {
        return mendrisioCagnoLine.getNtcId();
    }
}
