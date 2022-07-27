/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracImporter;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class FileImporter {
    private final UrlValidationService urlValidationService;

    public FileImporter(UrlValidationService urlValidationService) {
        this.urlValidationService = urlValidationService;
    }

    Network importNetwork(String cgmUrl) throws IOException {
        return Importers.loadNetwork(getFilenameFromUrl(cgmUrl), urlValidationService.openUrlStream(cgmUrl));
    }

    CseCrac importCseCrac(String cracUrl) throws IOException {
        InputStream cracInputStream = urlValidationService.openUrlStream(cracUrl);
        CseCracImporter cseCracImporter = new CseCracImporter();
        return cseCracImporter.importNativeCrac(cracInputStream);
    }

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) throws IOException {
        return new RaoResultImporter().importRaoResult(urlValidationService.openUrlStream(raoResultUrl), crac);
    }

    public Crac importCracFromJson(String cracUrl) throws IOException {
        InputStream cracResultStream = urlValidationService.openUrlStream(cracUrl);
        return CracImporters.importCrac(getFilenameFromUrl(cracUrl), cracResultStream);
    }

    private String getFilenameFromUrl(String url) {
        try {
            return FilenameUtils.getName(new URL(url).getPath());
        } catch (MalformedURLException e) {
            throw new CseInvalidDataException(String.format("URL is invalid: %s", url));
        }
    }
}
