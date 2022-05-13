/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.data.xsd.ttc_rao.CseRaoResult;
import com.farao_community.farao.cse.export_runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

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
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class FileExporter {
    private static final String JSON_CRAC_FILE_NAME = "crac.json";
    private static final String MINIO_SEPARATOR = "/";
    private static final DateTimeFormatter OUTPUTS_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final String PROCESS_TYPE_PREFIX = "CSE_";
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters.json";
    private static final String REGION = "CSE";
    private static final String UCTE_EXTENSION = "uct";
    private static final String UCTE_FORMAT = "UCTE";
    private static final String IIDM_FORMAT = "XIIDM";
    private static final String IIDM_EXTENSION = "xiidm";

    private final MinioAdapter minioAdapter;
    private final ProcessConfiguration processConfiguration;

    public FileExporter(MinioAdapter minioAdapter, ProcessConfiguration processConfiguration) {
        this.minioAdapter = minioAdapter;
        this.processConfiguration = processConfiguration;
    }

    String saveCracInJsonFormat(Crac crac, ProcessType processType) {
        MemDataSource memDataSource = new MemDataSource();
        try (OutputStream os = memDataSource.newOutputStream(JSON_CRAC_FILE_NAME, false)) {
            CracExporters.exportCrac(crac, "Json", os);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to save converted CRAC file.", e);
        }
        String cracPath = getDestinationPath(processType, GridcapaFileGroup.ARTIFACT) + JSON_CRAC_FILE_NAME;
        try (InputStream is = memDataSource.newInputStream(JSON_CRAC_FILE_NAME)) {
            minioAdapter.uploadArtifact(cracPath, is);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }

    String saveNetwork(Network network, String format, GridcapaFileGroup fileGroup, ProcessType processType, String networkFilename) {
        String networkPath;
        try (InputStream is = getNetworkInputStream(network, format)) {
            switch (fileGroup) {
                case ARTIFACT:
                    networkPath = getDestinationPath(processType, GridcapaFileGroup.ARTIFACT) + networkFilename;
                    minioAdapter.uploadArtifact(networkPath, is);
                    break;
                case OUTPUT:
                    networkPath = getDestinationPath(processType, GridcapaFileGroup.OUTPUT) + networkFilename + "." + UCTE_EXTENSION;
                    minioAdapter.uploadOutput(networkPath, is);
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("File group %s not supported", fileGroup));

            }
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to save network", e);
        }
        return minioAdapter.generatePreSignedUrl(networkPath);
    }

    String saveRaoParameters(ProcessType processType) {
        RaoParameters raoParameters = RaoParameters.load();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        String raoParametersDestinationPath = getDestinationPath(processType, GridcapaFileGroup.ARTIFACT) + RAO_PARAMETERS_FILE_NAME;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadArtifact(raoParametersDestinationPath, bais);
        return minioAdapter.generatePreSignedUrl(raoParametersDestinationPath);
    }

    String saveTtcRao(CseRaoResult cseRaoResult, ProcessType processType, OffsetDateTime processTargetDate) {
        StringWriter stringWriter = new StringWriter();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(CseRaoResult.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // format the XML output
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            QName qName = new QName("CseRaoResult");
            JAXBElement<CseRaoResult> root = new JAXBElement<>(qName, CseRaoResult.class, cseRaoResult);

            jaxbMarshaller.marshal(root, stringWriter);

        } catch (JAXBException e) {
            throw new CseInternalException("XSD matching error");
        }
        ByteArrayInputStream is = new ByteArrayInputStream(stringWriter.toString().getBytes());
        String ttcPath =  getDestinationPath(processType, GridcapaFileGroup.OUTPUT) + getTtcRaoResultOutputFilename(processTargetDate);
        minioAdapter.uploadOutput(ttcPath, is, adaptTargetProcessName(processType), processConfiguration.getTtcRao(), generateHourlyValidityInterval(processTargetDate));
        return minioAdapter.generatePreSignedUrl(ttcPath);
    }

    private String getTtcRaoResultOutputFilename(OffsetDateTime processTargetDate) {
        ZonedDateTime targetDateInEuropeZone = processTargetDate.atZoneSameInstant(ZoneId.of(processConfiguration.getZoneId()));
        String dateAndTime = targetDateInEuropeZone.format(OUTPUTS_DATE_TIME_FORMATTER);
        return  "TTC_Calculation_" + dateAndTime + "_2D0_CO_RAO_Transit_CSE0.xml";
    }

    private InputStream getNetworkInputStream(Network network, String format) throws IOException {
        MemDataSource memDataSource = new MemDataSource();
        switch (format) {
            case UCTE_FORMAT:
                Exporters.export(UCTE_FORMAT, network, new Properties(), memDataSource);
                return memDataSource.newInputStream("", UCTE_EXTENSION);
            case IIDM_FORMAT:
                Exporters.export(IIDM_FORMAT, network, new Properties(), memDataSource);
                return memDataSource.newInputStream("", IIDM_EXTENSION);
            default:
                throw new UnsupportedOperationException(String.format("Network format %s not supported.", format));
        }
    }

    private String getDestinationPath(ProcessType processType, GridcapaFileGroup gridcapaFileGroup) {
        return REGION + MINIO_SEPARATOR
                + processType + MINIO_SEPARATOR
                + gridcapaFileGroup + MINIO_SEPARATOR;
    }

    private String adaptTargetProcessName(ProcessType processType) {
        return PROCESS_TYPE_PREFIX + processType;
    }

    private String generateHourlyValidityInterval(OffsetDateTime timestamp) {
        return timestamp != null ? timestamp + "/" + timestamp.plusHours(1L) : null;
    }
}
