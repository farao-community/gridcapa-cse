/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
public class MendrisioConfiguration {
    @Value("${cse-cc-runner.mendrisio.mendrisio-pst-id}")
    private String mendrisioPstId;
    @Value("${cse-cc-runner.mendrisio.mendrisio-node-id}")
    private String mendrisioNodeId;

    public String getMendrisioPstId() {
        return mendrisioPstId;
    }

    public String getMendrisioNodeId() {
        return mendrisioNodeId;
    }
}
