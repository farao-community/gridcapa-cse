/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse.app.services;

import com.farao_community.farao.gridcapa_cse.api.exception.CseInternalException;
import com.farao_community.farao.gridcapa_cse.api.exception.CseInvalidDataException;
import com.farao_community.farao.gridcapa_cse.app.configurations.UrlWhitelistConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@Component
public class UrlValidationService {
    private UrlWhitelistConfiguration urlWhitelistConfiguration;

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

    public String getFileNameFromUrl(String stringUrl) {
        try {
            URL url = new URL(stringUrl);
            return FilenameUtils.getName(url.getPath());
        } catch (IOException e) {
            throw new CseInternalException(String.format("Exception occurred while retrieving file name from : %s Cause: %s ", stringUrl, e.getMessage()));
        }
    }
}
