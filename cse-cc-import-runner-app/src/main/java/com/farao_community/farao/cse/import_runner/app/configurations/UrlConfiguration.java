/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@ConfigurationProperties("cse-cc-runner")
public class UrlConfiguration {
    private final List<String> whitelist;
    private final String interruptServerUrl;

    public UrlConfiguration(List<String> whitelist, String interruptServerUrl) {
        this.whitelist = whitelist == null ? new ArrayList<>() : whitelist;
        this.interruptServerUrl = interruptServerUrl;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public String getInterruptServerUrl() {
        return interruptServerUrl;
    }
}