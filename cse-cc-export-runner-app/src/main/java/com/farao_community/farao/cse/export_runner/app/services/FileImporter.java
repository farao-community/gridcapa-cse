/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.export_runner.app.FileUtil;
import com.farao_community.farao.cse.export_runner.app.configurations.UrlConfiguration;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.io.cse.xsd.CRACDocumentType;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonImporter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class FileImporter {
    private final UrlConfiguration urlConfiguration;
    private final Logger businessLogger;

    public FileImporter(UrlConfiguration urlConfiguration, Logger businessLogger) {
        this.urlConfiguration = urlConfiguration;
        this.businessLogger = businessLogger;
    }

    Network importNetwork(String cgmUrl) {
        return Network.read(FileUtil.getFilenameFromUrl(cgmUrl), openUrlStream(cgmUrl));
    }

    CRACDocumentType importCseCrac(String cracUrl) {
        InputStream cracInputStream = openUrlStream(cracUrl);
        return importNativeCrac(cracInputStream);
    }

    CRACDocumentType importNativeCrac(InputStream inputStream) {
        try {
            return JAXBContext.newInstance(CRACDocumentType.class)
                    .createUnmarshaller()
                    .unmarshal(new StreamSource(inputStream), CRACDocumentType.class)
                    .getValue();
        } catch (JAXBException e) {
            throw new CseDataException("Exception occurred during import of native crac", e);
        }
    }

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) {
        return new RaoResultJsonImporter().importData(openUrlStream(raoResultUrl), crac);
    }

    InputStream openUrlStream(String urlString) {
        try {
            if (urlConfiguration.getWhitelist().stream().noneMatch(urlString::startsWith)) {
                throw new CseInvalidDataException(String.format("URL '%s' is not part of application's whitelisted url's.", urlString));
            }
            URL url = new URI(urlString).toURL();
            return url.openStream(); // NOSONAR Usage of whitelist not triggered by Sonar quality assessment, even if listed as a solution to the vulnerability
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            businessLogger.error("Error while retrieving content of file \"{}\", link may have expired.", FileUtil.getFilenameFromUrl(urlString));
            throw new CseDataException(String.format("Exception occurred while retrieving file content from %s", urlString), e);
        }
    }

}
