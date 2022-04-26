/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.runner.app.configurations.OutputsConfiguration;
import com.farao_community.farao.cse.runner.app.util.MinioStorageHelper;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.network.Network;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.Properties;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class FileExporter {

    private static final String NETWORK_FILE_NAME = "network_pre_processed.xiidm";
    private static final String JSON_CRAC_FILE_NAME = "crac.json";
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters.json";
    private static final DateTimeFormatter OUTPUTS_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private final MinioAdapter minioAdapter;
    private final OutputsConfiguration outputsConfiguration;

    private final String combinedRasFilePath;

    private static final String PROCESS_TYPE_PREFIX = "CSE_";

    @Value("${cse-cc-runner.zone-id}")
    private String zoneId;

    public FileExporter(MinioAdapter minioAdapter, OutputsConfiguration outputsConfiguration, String combinedRasFilePath) {
        this.minioAdapter = minioAdapter;
        this.outputsConfiguration = outputsConfiguration;
        this.combinedRasFilePath = combinedRasFilePath;
    }

    public String saveCracInJsonFormat(Crac crac, OffsetDateTime processTargetDateTime, ProcessType processType) {
        MemDataSource memDataSource = new MemDataSource();
        try (OutputStream os = memDataSource.newOutputStream(JSON_CRAC_FILE_NAME, false)) {
            CracExporters.exportCrac(crac, "Json", os);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to save converted CRAC file.", e);
        }
        String cracPath = MinioStorageHelper.makeDestinationMinioPath(processTargetDateTime, processType, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of(zoneId)) + JSON_CRAC_FILE_NAME;
        try (InputStream is = memDataSource.newInputStream(JSON_CRAC_FILE_NAME)) {
            minioAdapter.uploadArtifactForTimestamp(cracPath, is, adaptTargetProcessName(processType), "", processTargetDateTime);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }

    public String saveNetworkInArtifact(Network network, OffsetDateTime processTargetDateTime, String fileType, ProcessType processType) {
        String networkPath = MinioStorageHelper.makeDestinationMinioPath(processTargetDateTime, processType, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of(zoneId)) + NETWORK_FILE_NAME;
        return saveNetworkInArtifact(network, networkPath, GridcapaFileGroup.ARTIFACT, fileType, processTargetDateTime, processType);
    }

    public String saveNetworkInArtifact(Network network, String networkFilePath, GridcapaFileGroup fileGroup, String fileType, OffsetDateTime processTargetDateTime, ProcessType processType) {
        exportAndUploadNetwork(network, "XIIDM", fileGroup, networkFilePath, fileType, processTargetDateTime, processType);
        return minioAdapter.generatePreSignedUrl(networkFilePath);
    }

    public String saveRaoParameters(OffsetDateTime offsetDateTime, ProcessType processType) {
        RaoParameters raoParameters = getRaoParameters();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        String raoParametersDestinationPath = MinioStorageHelper.makeDestinationMinioPath(offsetDateTime, processType, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of(zoneId)) + RAO_PARAMETERS_FILE_NAME;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadArtifactForTimestamp(raoParametersDestinationPath, bais, adaptTargetProcessName(processType), "", offsetDateTime);
        return minioAdapter.generatePreSignedUrl(raoParametersDestinationPath);
    }

    RaoParameters getRaoParameters() {
        RaoParameters raoParameters = RaoParameters.load();
        try (InputStream is = new FileInputStream(combinedRasFilePath)) {
            ObjectMapper objectMapper = new ObjectMapper();
            List<List<String>> combinedRas = objectMapper.readValue(is.readAllBytes(), List.class);
            SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
            if (searchTreeRaoParameters != null) {
                searchTreeRaoParameters.setNetworkActionIdCombinations(combinedRas);
            }
        } catch (IOException e) {
            throw new CseDataException(String.format("Impossible to read combined RAs file: %s", combinedRasFilePath));
        }
        return raoParameters;
    }

    String saveTtcResult(Timestamp timestamp, OffsetDateTime processTargetDate, ProcessType processType) {
        StringWriter stringWriter = new StringWriter();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Timestamp.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // format the XML output
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            QName qName = new QName("Timestamp");
            JAXBElement<Timestamp> root = new JAXBElement<>(qName, Timestamp.class, timestamp);

            jaxbMarshaller.marshal(root, stringWriter);

        } catch (JAXBException e) {
            throw new CseInternalException("XSD matching error");
        }
        InputStream is = new ByteArrayInputStream(stringWriter.toString().getBytes());
        String outputFilePath = getFilePath(processTargetDate, processType);
        minioAdapter.uploadOutputForTimestamp(outputFilePath, is, adaptTargetProcessName(processType), outputsConfiguration.getTtcRes(), processTargetDate);
        return minioAdapter.generatePreSignedUrl(outputFilePath);
    }

    String getFilePath(OffsetDateTime processTargetDate, ProcessType processType) {
        String filename;
        ZonedDateTime targetDateInEuropeZone = processTargetDate.atZoneSameInstant(ZoneId.of(zoneId));
        if (processType == ProcessType.D2CC) {
            String dateAndTime = targetDateInEuropeZone.format(OUTPUTS_DATE_TIME_FORMATTER);
            filename = "TTC_Calculation_" + dateAndTime + "_2D0_CO_CSE1.xml";
        } else {
            String date = targetDateInEuropeZone.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            filename = date + "_XBID2_TTCRes_CSE1.xml";
        }
        return MinioStorageHelper.makeDestinationMinioPath(processTargetDate, processType, MinioStorageHelper.FileKind.OUTPUTS, ZoneId.of(zoneId)) + filename;
    }

    String exportAndUploadNetwork(Network network, String format, GridcapaFileGroup fileGroup, String filePath, String fileType, OffsetDateTime offsetDateTime, ProcessType processType) {
        try (InputStream is = getNetworkInputStream(network, format)) {
            switch (fileGroup) {
                case OUTPUT:
                    minioAdapter.uploadOutputForTimestamp(filePath, is, adaptTargetProcessName(processType), fileType, offsetDateTime);
                    break;
                case ARTIFACT:
                    minioAdapter.uploadArtifactForTimestamp(filePath, is, adaptTargetProcessName(processType), fileType, offsetDateTime);
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("File group %s not supported", fileGroup));

            }
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to save network", e);
        }
        return minioAdapter.generatePreSignedUrl(filePath);
    }

    private InputStream getNetworkInputStream(Network network, String format) throws IOException {
        MemDataSource memDataSource = new MemDataSource();
        switch (format) {
            case "UCTE":
                Exporters.export("UCTE", network, new Properties(), memDataSource);
                return memDataSource.newInputStream("", "uct");
            case "XIIDM":
                Exporters.export("XIIDM", network, new Properties(), memDataSource);
                return memDataSource.newInputStream("", "xiidm");
            default:
                throw new UnsupportedOperationException(String.format("Network format %s not supported", format));
        }
    }

    String getFinalNetworkFilePath(OffsetDateTime processTargetDate, ProcessType processType) {
        String filename;
        ZonedDateTime targetDateInEuropeZone = processTargetDate.atZoneSameInstant(ZoneId.of(zoneId));
        int dayOfWeek = targetDateInEuropeZone.getDayOfWeek().getValue();
        String dateAndTime = targetDateInEuropeZone.format(OUTPUTS_DATE_TIME_FORMATTER);
        if (processType == ProcessType.D2CC) {
            filename = dateAndTime + "_2D" + dayOfWeek + "_CO_Final_CSE1.uct";
        } else {
            filename = dateAndTime + "_" + processTargetDate.getHour() + dayOfWeek + "_CSE1.uct";
        }
        return MinioStorageHelper.makeDestinationMinioPath(processTargetDate, processType, MinioStorageHelper.FileKind.OUTPUTS, ZoneId.of(zoneId)) + filename;
    }

    String getBaseCaseFilePath(OffsetDateTime processTargetDate, ProcessType processType) {
        String filename;
        ZonedDateTime targetDateInEuropeZone = processTargetDate.atZoneSameInstant(ZoneId.of(zoneId));
        int dayOfWeek = targetDateInEuropeZone.getDayOfWeek().getValue();
        String dateAndTime = targetDateInEuropeZone.format(OUTPUTS_DATE_TIME_FORMATTER);
        if (processType == ProcessType.D2CC) {
            filename = dateAndTime + "_2D" + dayOfWeek + "_CO_CSE1.uct";
        } else {
            filename = dateAndTime + "_" + processTargetDate.getHour() + dayOfWeek + "_Initial_CSE1.uct";
        }
        return MinioStorageHelper.makeDestinationMinioPath(processTargetDate, processType, MinioStorageHelper.FileKind.OUTPUTS, ZoneId.of(zoneId)) + filename;
    }

    public String getZoneId() {
        return zoneId;
    }

    private String adaptTargetProcessName(ProcessType processType) {
        return PROCESS_TYPE_PREFIX + processType;
    }

}
