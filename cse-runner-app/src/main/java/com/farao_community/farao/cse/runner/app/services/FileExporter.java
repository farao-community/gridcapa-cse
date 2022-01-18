/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.cse.runner.api.resource.FileResource;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class FileExporter {

    private static final String PRE_PROCESSED_NETWORK_FILE_NAME = "network_pre_processed.xiidm";
    private static final String JSON_CRAC_FILE_NAME = "crac.json";
    public static final String ARTIFACTS_S = "artifacts/%s";
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters.json";

    private final MinioAdapter minioAdapter;
    private final String zoneId;

    private String raoParametersUrl;

    public FileExporter(MinioAdapter minioAdapter, ProcessConfiguration processConfiguration) {
        this.minioAdapter = minioAdapter;
        zoneId = processConfiguration.getZoneId();
    }

    public String saveCracInJsonFormat(OffsetDateTime processTargetDate, Crac crac) {
        MemDataSource memDataSource = new MemDataSource();
        try (OutputStream os = memDataSource.newOutputStream(JSON_CRAC_FILE_NAME, false)) {
            CracExporters.exportCrac(crac, "Json", os);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to save converted CRAC file.", e);
        }
        String cracPath = String.format(ARTIFACTS_S, JSON_CRAC_FILE_NAME);
        try (InputStream is = memDataSource.newInputStream(JSON_CRAC_FILE_NAME)) {
            return minioAdapter.uploadFile(processTargetDate, cracPath, is).getUrl();
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to upload converted CRAC file.", e);
        }
    }

    public FileResource saveNetwork(OffsetDateTime processTargetDate, Network network) {
        return saveNetwork(processTargetDate, network, PRE_PROCESSED_NETWORK_FILE_NAME);
    }

    public FileResource saveNetwork(OffsetDateTime processTargetDate, Network network, String networkFilePath) {
        MemDataSource memDataSource = new MemDataSource();
        Exporters.export("XIIDM", network, new Properties(), memDataSource);
        try (InputStream is = memDataSource.newInputStream("", "xiidm")) {
            return minioAdapter.uploadFile(processTargetDate, networkFilePath, is);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to save pre-processed network", e);
        }
    }

    public void saveRaoParameters() {
        RaoParameters raoParameters = RaoParameters.load();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        raoParametersUrl = minioAdapter.uploadFile(RAO_PARAMETERS_FILE_NAME, bais).getUrl();
    }

    public String getRaoParametersUrl() {
        if (raoParametersUrl == null) {
            saveRaoParameters();
        }
        return raoParametersUrl;
    }

    String saveTtcResult(Timestamp timestamp, OffsetDateTime processTargetDate, ProcessType processType) throws IOException {
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
        return minioAdapter.uploadFile(processTargetDate, getTtcResFilePath(processTargetDate, processType), is).getUrl();
    }

    String getTtcResFilePath(OffsetDateTime processTargetDate, ProcessType processType) {
        String filename;
        ZonedDateTime targetDateInEuropeZone = processTargetDate.atZoneSameInstant(ZoneId.of(zoneId));
        if (processType == ProcessType.D2CC) {
            String dateAndTime = targetDateInEuropeZone.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            filename = "TTC_Calculation_" + dateAndTime + "_2D0_CO_CSE1.xml";
        } else {
            String date = targetDateInEuropeZone.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            filename = date + "_XBID2_TTCRes_CSE1.xml";
        }
        return "outputs/" + filename;
    }

    public String getMinioBaseProcessTargetDatePath(OffsetDateTime processTargetDate) {
        return minioAdapter.getMinioBaseProcessTargetDatePath(processTargetDate);
    }
}
