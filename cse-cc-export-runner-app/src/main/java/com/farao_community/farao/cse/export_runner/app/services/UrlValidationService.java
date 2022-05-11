/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.export_runner.app.configurations.UrlWhitelistConfiguration;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@Component
public class UrlValidationService {
    private final UrlWhitelistConfiguration urlWhitelistConfiguration;

    public UrlValidationService(UrlWhitelistConfiguration urlWhitelistConfiguration) {
        this.urlWhitelistConfiguration = urlWhitelistConfiguration;
    }

    public InputStream openUrlStream(String urlString) throws IOException {
        if (urlWhitelistConfiguration.getWhitelist().stream().noneMatch(urlString::startsWith)) {
            throw new CseInvalidDataException(String.format("URL '%s' is not part of application's whitelisted url's.", urlString));
        }
        URL url = new URL(urlString);
        return url.openStream(); // NOSONAR Usage of whitelist not triggered by Sonar quality assessment, even if listed as a solution to the vulnerability
    }
}
