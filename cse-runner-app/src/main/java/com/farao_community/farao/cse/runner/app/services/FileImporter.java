/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.data.CseReferenceExchanges;
import com.farao_community.farao.cse.data.ntc.Ntc;
import com.farao_community.farao.cse.data.ntc2.Ntc2;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;

import static com.farao_community.farao.cse.runner.app.util.FileUtil.getFilenameFromUrl;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class FileImporter {

    private final UrlValidationService urlValidationService;

    public FileImporter(UrlValidationService urlValidationService) {
        this.urlValidationService = urlValidationService;
    }

    public Network importNetwork(String cgmUrl) throws IOException {
        return Importers.loadNetwork(getFilenameFromUrl(cgmUrl), urlValidationService.openUrlStream(cgmUrl));
    }

    public Crac importCrac(String cracUrl, OffsetDateTime targetProcessDateTime, Network network) throws IOException {
        String cracFilename = getFilenameFromUrl(cracUrl);
        InputStream cracInputStream = urlValidationService.openUrlStream(cracUrl);
        return CracCreators.importAndCreateCrac(cracFilename, cracInputStream, network, targetProcessDateTime).getCrac();
    }

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) throws IOException {
        return new RaoResultImporter().importRaoResult(urlValidationService.openUrlStream(raoResultUrl), crac);
    }

    public Ntc importNtc(OffsetDateTime targetProcessDateTime, String yearlyNtcUrl, String dailyNtcUrl) {
        try (InputStream yearlyNtcStream = urlValidationService.openUrlStream(yearlyNtcUrl);
             InputStream dailyNtcStream = urlValidationService.openUrlStream(dailyNtcUrl)) {
            return Ntc.create(targetProcessDateTime, yearlyNtcStream, dailyNtcStream);
        } catch (IOException | JAXBException e) {
            throw new CseInvalidDataException("Impossible to create NTC", e);
        }
    }

    public Ntc2 importNtc2(OffsetDateTime targetProcessDateTime, String ntc2AtItUrl, String ntc2ChItUrl, String ntc2FrItUrl, String ntc2SiItUrl) {
        try (InputStream ntc2AtItStream = urlValidationService.openUrlStream(ntc2AtItUrl);
             InputStream ntc2ChItStream = urlValidationService.openUrlStream(ntc2ChItUrl);
             InputStream ntc2FrItStream = urlValidationService.openUrlStream(ntc2FrItUrl);
             InputStream ntc2SiItStream = urlValidationService.openUrlStream(ntc2SiItUrl)) {
            Map<String, InputStream> ntc2Streams = Map.of(
                getFilenameFromUrl(ntc2AtItUrl), ntc2AtItStream,
                getFilenameFromUrl(ntc2ChItUrl), ntc2ChItStream,
                getFilenameFromUrl(ntc2FrItUrl), ntc2FrItStream,
                getFilenameFromUrl(ntc2SiItUrl), ntc2SiItStream
            );
            return Ntc2.create(targetProcessDateTime, ntc2Streams);
        } catch (IOException e) {
            throw new CseInvalidDataException("Impossible to create NTC2", e);
        }
    }

    public CseReferenceExchanges importCseReferenceExchanges(OffsetDateTime targetProcessDateTime, String vulcanusUrl) {
        try (InputStream vulcanusStream = urlValidationService.openUrlStream(vulcanusUrl)) {
            return CseReferenceExchanges.fromVulcanusFile(targetProcessDateTime, vulcanusStream);
        } catch (IOException e) {
            throw new CseInvalidDataException("Impossible to create NTC", e);
        }
    }
}
