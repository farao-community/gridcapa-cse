/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Configuration
public class ProcessConfiguration {

    @Value("${cse-cc-runner.zone-id}")
    private String zoneId;

    @Value("${cse-cc-runner.trm}")
    private Double trm;

    @Value("${cse-cc-runner.outputs.initial-cgm}")
    private String initialCgm;
    @Value("${cse-cc-runner.outputs.final-cgm}")
    private String finalCgm;
    @Value("${cse-cc-runner.outputs.ttc-res}")
    private String ttcRes;

    public String getZoneId() {
        return zoneId;
    }

    public Double getTrm() {
        return trm;
    }

    public String getInitialCgm() {
        return initialCgm;
    }

    public String getFinalCgm() {
        return finalCgm;
    }

    public String getTtcRes() {
        return ttcRes;
    }
}
