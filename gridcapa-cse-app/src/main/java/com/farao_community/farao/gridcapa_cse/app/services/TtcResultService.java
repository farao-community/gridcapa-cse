/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_cse.app.services;

import com.farao_community.farao.cse.data.ttc_res.CracResultsHelper;
import com.farao_community.farao.cse.data.ttc_res.TtcResult;
import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.dichotomy_runner.api.resource.DichotomyResponse;
import com.farao_community.farao.gridcapa_cse.api.exception.CseInternalException;
import com.farao_community.farao.gridcapa_cse.api.resource.CseRequest;
import com.farao_community.farao.gridcapa_cse.app.CseData;
import com.farao_community.farao.gridcapa_cse.app.util.ItalianImport;
import com.farao_community.farao.gridcapa_cse.app.configurations.XNodesConfiguration;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.*;
import java.util.Collections;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class TtcResultService {

    private final MinioAdapter minioAdapter;
    private final UrlValidationService urlValidationService;
    private final XNodesConfiguration xNodesConfiguration;

    public TtcResultService(MinioAdapter minioAdapter, UrlValidationService urlValidationService, XNodesConfiguration xNodesConfiguration) {
        this.minioAdapter = minioAdapter;
        this.urlValidationService = urlValidationService;
        this.xNodesConfiguration = xNodesConfiguration;
    }

    public String saveTtcResult(CseRequest cseRequest, CseData cseData, DichotomyResponse dichotomyResponse, Crac crac) throws IOException {
        TtcResult.TtcFiles ttcFiles = new TtcResult.TtcFiles(
            cseRequest.getCgmUrl(),
            cseData.getJsonCracUrl(),
            cseRequest.getMergedGlskUrl(),
            urlValidationService.getFileNameFromUrl(cseRequest.getNtcReductionsUrl()),
            "ntcReductionCreationDatetime",
            dichotomyResponse.getHighestValidStep().getNetworkWithPra().getUrl()
        );

        Network networkAfterDichotomy = Importers.loadNetwork(
            urlValidationService.getFileNameFromUrl(dichotomyResponse.getHighestValidStep().getNetworkWithPra().getUrl()),
            urlValidationService.openUrlStream(dichotomyResponse.getHighestValidStep().getNetworkWithPra().getUrl())
        );
        double finalItalianImport = ItalianImport.compute(networkAfterDichotomy);
        TtcResult.ProcessData processData = new TtcResult.ProcessData(
            cseData.getCseReferenceExchanges().getExchanges(),
            cseData.getReducedSplittingFactors(),
            Collections.emptyMap(),
            dichotomyResponse.getLimitingCause().toString(),
            finalItalianImport,
            cseData.getMniiOffset(),
            cseRequest.getTargetProcessDateTime().toString()
        );

        RaoResult raoResult = new RaoResultImporter().importRaoResult(
            urlValidationService.openUrlStream(dichotomyResponse.getHighestValidStep().getRaoResult().getUrl()),
            crac
        );

        Timestamp timestamp = TtcResult.generate(ttcFiles, processData, new CracResultsHelper(crac, raoResult, xNodesConfiguration.getXNodes()));
        return saveTtcResult(timestamp);
    }

    private String saveTtcResult(Timestamp timestamp) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        StringWriter stringWriter = new StringWriter();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Timestamp.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // format the XML output
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            QName qName = new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "Timestamp");
            JAXBElement<Timestamp> root = new JAXBElement<>(qName, Timestamp.class, timestamp);

            jaxbMarshaller.marshal(root, stringWriter);
            String result = stringWriter.toString()
                .replace("xsi:Timestamp", "Timestamp");

            bos.write(result.getBytes());

        } catch (JAXBException e) {
            throw new CseInternalException("XSD matching error");
        }

        byte[] bytes = bos.toByteArray();
        InputStream is = new ByteArrayInputStream(bytes);
        minioAdapter.uploadFile("outputs/ttc_res.xml", is);
        return minioAdapter.generatePreSignedUrl("outputs/ttc_res.xml");
    }
}
