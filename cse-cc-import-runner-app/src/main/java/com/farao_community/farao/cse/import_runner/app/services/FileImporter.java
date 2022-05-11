/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.data.CseReferenceExchanges;
import com.farao_community.farao.cse.data.ntc.Ntc;
import com.farao_community.farao.cse.data.ntc2.Ntc2;
import com.farao_community.farao.cse.data.target_ch.LineFixedFlows;
import com.farao_community.farao.cse.import_runner.app.util.FileUtil;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracImporter;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.BusBarChangeSwitches;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.CseCracCreationParameters;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.powsybl.action.util.Scalable;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

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
        return Importers.loadNetwork(FileUtil.getFilenameFromUrl(cgmUrl), urlValidationService.openUrlStream(cgmUrl));
    }

    public CseCrac importCseCrac(String cracUrl) throws IOException {
        InputStream cracInputStream = urlValidationService.openUrlStream(cracUrl);
        CseCracImporter cseCracImporter = new CseCracImporter();
        return cseCracImporter.importNativeCrac(cracInputStream);
    }

    public Crac importCrac(CseCrac cseCrac, Set<BusBarChangeSwitches> busBarChangeSwitchesSet, OffsetDateTime targetProcessDateTime, Network network) {
        CracCreationParameters cracCreationParameters = CracCreationParameters.load();
        CseCracCreationParameters cseCracCreationParameters = new CseCracCreationParameters();
        cseCracCreationParameters.setBusBarChangeSwitchesSet(busBarChangeSwitchesSet);
        cracCreationParameters.addExtension(CseCracCreationParameters.class, cseCracCreationParameters);
        return CracCreators.createCrac(cseCrac, network, targetProcessDateTime, cracCreationParameters).getCrac();
    }

    public Crac importCracFromJson(String cracUrl) throws IOException {
        InputStream cracResultStream = urlValidationService.openUrlStream(cracUrl);
        return CracImporters.importCrac(FileUtil.getFilenameFromUrl(cracUrl), cracResultStream);
    }

    public ZonalData<Scalable> importGlsk(String glskUrl, Network network) throws IOException {
        return GlskDocumentImporters.importGlsk(urlValidationService.openUrlStream(glskUrl)).getZonalScalable(network);
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
        try (InputStream vulcanusStream = urlValidationService.openUrlStream(vulcanusUrl)) {
            return CseReferenceExchanges.fromVulcanusFile(targetProcessDateTime, vulcanusStream, FileUtil.getFilenameFromUrl(vulcanusUrl));
        } catch (IOException e) {
            throw new CseInvalidDataException("Impossible to create CseReferenceExchanges", e);
        }
    }

    public LineFixedFlows importLineFixedFlowFromTargetChFile(OffsetDateTime targetProcessDateTime, String targetChUrl) {
        try (InputStream targetChStream = urlValidationService.openUrlStream(targetChUrl)) {
            return LineFixedFlows.create(targetProcessDateTime, targetChStream);
        } catch (Exception e) {
            throw new CseInvalidDataException("Impossible to import LineFixedFlow from Target ch file", e);
        }
    }
}
