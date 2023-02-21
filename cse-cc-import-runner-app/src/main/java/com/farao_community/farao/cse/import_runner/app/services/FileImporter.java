/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.CseReferenceExchanges;
import com.farao_community.farao.cse.data.ntc.Ntc;
import com.farao_community.farao.cse.data.ntc2.Ntc2;
import com.farao_community.farao.cse.data.target_ch.LineFixedFlows;
import com.farao_community.farao.cse.import_runner.app.configurations.UrlWhitelistConfiguration;
import com.farao_community.farao.cse.import_runner.app.util.FileUtil;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracImporter;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class FileImporter {

    private final UrlWhitelistConfiguration urlWhitelistConfiguration;
    private final Logger businessLogger;

    public FileImporter(UrlWhitelistConfiguration urlWhitelistConfiguration, Logger businessLogger) {
        this.urlWhitelistConfiguration = urlWhitelistConfiguration;
        this.businessLogger = businessLogger;
    }

    public Network importNetwork(String cgmUrl) {
        return Network.read(FileUtil.getFilenameFromUrl(cgmUrl), openUrlStream(cgmUrl));
    }

    public CseCrac importCseCrac(String cracUrl) {
        InputStream cracInputStream = openUrlStream(cracUrl);
        CseCracImporter cseCracImporter = new CseCracImporter();
        return cseCracImporter.importNativeCrac(cracInputStream);
    }

    public Crac importCracFromJson(String cracUrl) {
        InputStream cracResultStream = openUrlStream(cracUrl);
        return CracImporters.importCrac(FileUtil.getFilenameFromUrl(cracUrl), cracResultStream);
    }

    public ZonalData<Scalable> importGlsk(String glskUrl, Network network) {
        return GlskDocumentImporters.importGlsk(openUrlStream(glskUrl)).getZonalScalable(network);
    }

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) {
        return new RaoResultImporter().importRaoResult(openUrlStream(raoResultUrl), crac);
    }

    public Ntc importNtc(OffsetDateTime targetProcessDateTime, String yearlyNtcUrl, String dailyNtcUrl) {
        try (InputStream yearlyNtcStream = openUrlStream(yearlyNtcUrl);
             InputStream dailyNtcStream = openUrlStream(dailyNtcUrl)) {
            return Ntc.create(targetProcessDateTime, yearlyNtcStream, dailyNtcStream, false); // Temporarily we do not manage the adapted import
        } catch (IOException | JAXBException e) {
            throw new CseInvalidDataException("Impossible to create NTC", e);
        }
    }

    public Ntc2 importNtc2(OffsetDateTime targetProcessDateTime, String ntc2AtItUrl, String ntc2ChItUrl, String ntc2FrItUrl, String ntc2SiItUrl) {
        try (InputStream ntc2AtItStream = openUrlStream(ntc2AtItUrl);
             InputStream ntc2ChItStream = openUrlStream(ntc2ChItUrl);
             InputStream ntc2FrItStream = openUrlStream(ntc2FrItUrl);
             InputStream ntc2SiItStream = openUrlStream(ntc2SiItUrl)) {
            Map<String, InputStream> ntc2Streams = Map.of(
                FileUtil.getFilenameFromUrl(ntc2AtItUrl), ntc2AtItStream,
                FileUtil.getFilenameFromUrl(ntc2ChItUrl), ntc2ChItStream,
                FileUtil.getFilenameFromUrl(ntc2FrItUrl), ntc2FrItStream,
                FileUtil.getFilenameFromUrl(ntc2SiItUrl), ntc2SiItStream
            );
            return Ntc2.create(targetProcessDateTime, ntc2Streams);
        } catch (IOException e) {
            throw new CseInvalidDataException("Impossible to create NTC2", e);
        }
    }

    public CseReferenceExchanges importCseReferenceExchanges(OffsetDateTime targetProcessDateTime, String vulcanusUrl) {
        try (InputStream vulcanusStream = openUrlStream(vulcanusUrl)) {
            return CseReferenceExchanges.fromVulcanusFile(targetProcessDateTime, vulcanusStream, FileUtil.getFilenameFromUrl(vulcanusUrl));
        } catch (IOException e) {
            throw new CseInvalidDataException("Impossible to create CseReferenceExchanges", e);
        }
    }

    public LineFixedFlows importLineFixedFlowFromTargetChFile(OffsetDateTime targetProcessDateTime, String targetChUrl, boolean isAdaptedProcess) {
        try (InputStream targetChStream = openUrlStream(targetChUrl)) {
            return LineFixedFlows.create(targetProcessDateTime, targetChStream, isAdaptedProcess);
        } catch (Exception e) {
            throw new CseInvalidDataException("Impossible to import LineFixedFlow from Target ch file", e);
        }
    }

    private InputStream openUrlStream(String urlString) {
        try {
            if (urlWhitelistConfiguration.getWhitelist().stream().noneMatch(urlString::startsWith)) {
                throw new CseInvalidDataException(String.format("URL '%s' is not part of application's whitelisted url's.", urlString));
            }
            URL url = new URL(urlString);
            return url.openStream(); // NOSONAR Usage of whitelist not triggered by Sonar quality assessment, even if listed as a solution to the vulnerability
        } catch (IOException e) {
            businessLogger.error("Error while retrieving content of file : {}, Link may have expired.", getFileNameFromUrl(urlString));
            throw new CseDataException(String.format("Exception occurred while retrieving file content from : %s Cause: %s ", urlString, e.getMessage()));
        }
    }

    private String getFileNameFromUrl(String stringUrl) {
        try {
            URL url = new URL(stringUrl);
            return FilenameUtils.getName(url.getPath());
        } catch (IOException e) {
            throw new CseDataException(String.format("Exception occurred while retrieving file name from : %s Cause: %s ", stringUrl, e.getMessage()));
        }
    }
}
