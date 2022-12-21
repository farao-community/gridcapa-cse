/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.export_runner.app.FileUtil;
import com.farao_community.farao.cse.export_runner.app.configurations.UrlWhitelistConfiguration;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracImporter;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class FileImporter {
    private final UrlWhitelistConfiguration urlWhitelistConfiguration;
    private final Logger businessLogger;

    public FileImporter(UrlWhitelistConfiguration urlWhitelistConfiguration, Logger businessLogger) {
        this.urlWhitelistConfiguration = urlWhitelistConfiguration;
        this.businessLogger = businessLogger;
    }

    Network importNetwork(String cgmUrl) {
        return Network.read(FileUtil.getFilenameFromUrl(cgmUrl), openUrlStream(cgmUrl));
    }

    CseCrac importCseCrac(String cracUrl) {
        InputStream cracInputStream = openUrlStream(cracUrl);
        CseCracImporter cseCracImporter = new CseCracImporter();
        return cseCracImporter.importNativeCrac(cracInputStream);
    }

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) {
        return new RaoResultImporter().importRaoResult(openUrlStream(raoResultUrl), crac);
    }

    private InputStream openUrlStream(String urlString) {
        try {
            if (urlWhitelistConfiguration.getWhitelist().stream().noneMatch(urlString::startsWith)) {
                throw new CseInvalidDataException(String.format("URL '%s' is not part of application's whitelisted url's.", urlString));
            }
            URL url = new URL(urlString);
            return url.openStream(); // NOSONAR Usage of whitelist not triggered by Sonar quality assessment, even if listed as a solution to the vulnerability
        } catch (IOException e) {
            businessLogger.error("Error while retrieving content of file : {}, Link may have expired.", FileUtil.getFilenameFromUrl(urlString));
            throw new CseDataException(String.format("Exception occurred while retrieving file content from : %s Cause: %s ", urlString, e.getMessage()));
        }
    }

}
