/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.data.xsd.ttc_rao.CseRaoResult;
import com.farao_community.farao.cse.export_runner.app.FileUtil;
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
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
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
    private static final String PROCESS_TYPE_PREFIX = "CSE_EXPORT_";
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters.json";
    private static final String REGION = "CSE";
    private static final String DIRECTION = "EXPORT";
    private static final String UCTE_EXTENSION = "uct";
    private static final String UCTE_FORMAT = "UCTE";
    private static final String XIIDM_FORMAT = "XIIDM";
    private static final String XIIDM_EXTENSION = "xiidm";

    private final MinioAdapter minioAdapter;
    private final ProcessConfiguration processConfiguration;

    public FileExporter(MinioAdapter minioAdapter, ProcessConfiguration processConfiguration) {
        this.minioAdapter = minioAdapter;
        this.processConfiguration = processConfiguration;
    }

    String saveCracInJsonFormat(Crac crac, ProcessType processType, OffsetDateTime processTargetDate) {
        MemDataSource memDataSource = new MemDataSource();
        try (OutputStream os = memDataSource.newOutputStream(JSON_CRAC_FILE_NAME, false)) {
            CracExporters.exportCrac(crac, "Json", os);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to save converted CRAC file.", e);
        }
        String cracPath = getDestinationPath(processTargetDate, processType, GridcapaFileGroup.ARTIFACT) + JSON_CRAC_FILE_NAME;
        try (InputStream is = memDataSource.newInputStream(JSON_CRAC_FILE_NAME)) {
            minioAdapter.uploadArtifactForTimestamp(cracPath, is, adaptTargetProcessName(processType), "", processTargetDate);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }

    String saveNetwork(Network network, String format, GridcapaFileGroup fileGroup, ProcessType processType, String networkFilename, OffsetDateTime processTargetDate) {
        String networkPath = getDestinationPath(processTargetDate, processType, fileGroup) + getNetworkFilenameWithExtension(networkFilename, format);
        try (InputStream is = getNetworkInputStream(network, format)) {
            switch (fileGroup) {
                case ARTIFACT:
                    minioAdapter.uploadArtifactForTimestamp(networkPath, is, adaptTargetProcessName(processType), "", processTargetDate);
                    break;
                case OUTPUT:
                    minioAdapter.uploadOutputForTimestamp(networkPath, is, adaptTargetProcessName(processType), processConfiguration.getFinalCgm(), processTargetDate);
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("File group %s not supported", fileGroup));
            }
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to save network", e);
        }
        return minioAdapter.generatePreSignedUrl(networkPath);
    }

    private static String getNetworkFilenameWithExtension(String networkFileName, String format) {
        switch (format) {
            case UCTE_FORMAT:
                return networkFileName + "." + UCTE_EXTENSION;
            case XIIDM_FORMAT:
                return networkFileName + "." + XIIDM_EXTENSION;
            default:
                throw new NotImplementedException("Network format %s is not recognized");
        }
    }

    String saveRaoParameters(ProcessType processType, OffsetDateTime processTargetDate) {
        RaoParameters raoParameters = RaoParameters.load();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        String raoParametersDestinationPath = getDestinationPath(processTargetDate, processType, GridcapaFileGroup.ARTIFACT) + RAO_PARAMETERS_FILE_NAME;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadArtifactForTimestamp(raoParametersDestinationPath, bais, adaptTargetProcessName(processType), "", processTargetDate);
        return minioAdapter.generatePreSignedUrl(raoParametersDestinationPath);
    }

    String saveTtcRao(CseRaoResult cseRaoResult, ProcessType processType, OffsetDateTime processTargetDate, String initialCgmFilename) {
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
            throw new CseInternalException("XSD matching error", e);
        }
        ByteArrayInputStream is = new ByteArrayInputStream(stringWriter.toString().getBytes());
        String ttcPath =  getDestinationPath(processTargetDate, processType, GridcapaFileGroup.OUTPUT) + getTtcRaoResultOutputFilename(processTargetDate, initialCgmFilename, processType);
        minioAdapter.uploadOutputForTimestamp(ttcPath, is, adaptTargetProcessName(processType), processConfiguration.getTtcRao(), processTargetDate);
        return minioAdapter.generatePreSignedUrl(ttcPath);
    }

    private String getTtcRaoResultOutputFilename(OffsetDateTime processTargetDate, String initialCgmFilename, ProcessType processType) {
        ZonedDateTime targetDateInEuropeZone = processTargetDate.atZoneSameInstant(ZoneId.of(processConfiguration.getZoneId()));
        String dateAndTime = targetDateInEuropeZone.format(OUTPUTS_DATE_TIME_FORMATTER);
        return  "TTC_Calculation_" + dateAndTime + "_2D0_CO_RAO_Transit_CSE" + FileUtil.getFileVersion(initialCgmFilename, processType) + ".xml";
    }

    InputStream getNetworkInputStream(Network network, String format) throws IOException {
        MemDataSource memDataSource = new MemDataSource();
        switch (format) {
            case UCTE_FORMAT:
                network.write(UCTE_FORMAT, new Properties(), memDataSource);
                return memDataSource.newInputStream("", UCTE_EXTENSION);
            case XIIDM_FORMAT:
                network.write(XIIDM_FORMAT, new Properties(), memDataSource);
                return memDataSource.newInputStream("", XIIDM_EXTENSION);
            default:
                throw new UnsupportedOperationException(String.format("Network format %s not supported.", format));
        }
    }

    protected String getDestinationPath(OffsetDateTime offsetDateTime, ProcessType processType, GridcapaFileGroup gridcapaFileGroup) {
        ZonedDateTime targetDateTime = offsetDateTime.atZoneSameInstant(ZoneId.of(processConfiguration.getZoneId()));
        return REGION + MINIO_SEPARATOR
            + DIRECTION + MINIO_SEPARATOR
            + processType + MINIO_SEPARATOR
            + targetDateTime.getYear() + MINIO_SEPARATOR
            + String.format("%02d", targetDateTime.getMonthValue()) + MINIO_SEPARATOR
            + String.format("%02d", targetDateTime.getDayOfMonth()) + MINIO_SEPARATOR
            + String.format("%02d", targetDateTime.getHour()) + "_30" + MINIO_SEPARATOR
            + gridcapaFileGroup + MINIO_SEPARATOR;
    }

    private String adaptTargetProcessName(ProcessType processType) {
        return PROCESS_TYPE_PREFIX + processType;
    }

}
