/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.cse.runner.api.resource.FileResource;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
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
import org.apache.commons.io.FilenameUtils;
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
    private final String combinedRasFilePath;

    @Value("${minio-adapter.base-path}")
    private String minioBasePath;
    @Value("${cse-cc-runner.zone-id}")
    private String zoneId;
    @Value("${cse-cc-runner.files-metadata.process-id}")
    private String processId;
    @Value("${cse-cc-runner.files-metadata.outputs.ttc-res}")
    private String ttcRes;

    public FileExporter(MinioAdapter minioAdapter, String combinedRasFilePath) {
        this.minioAdapter = minioAdapter;
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
            minioAdapter.uploadArtifact(cracPath, is);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }

    public FileResource saveNetworkInArtifact(Network network, OffsetDateTime processTargetDateTime, String fileType, ProcessType processType) {
        String networkPath = MinioStorageHelper.makeDestinationMinioPath(processTargetDateTime, processType, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of(zoneId)) + NETWORK_FILE_NAME;
        return saveNetwork(network, networkPath, GridcapaFileGroup.ARTIFACT, fileType, processTargetDateTime);
    }

    public FileResource saveNetwork(Network network, String networkFilePath, GridcapaFileGroup fileGroup, String fileType, OffsetDateTime processTargetDateTime) {
        exportAndUploadFile(network, networkFilePath, "XIIDM", "xiidm", fileGroup, fileType, processTargetDateTime);
        return generateFileResource(networkFilePath);
    }

    public String saveRaoParameters(OffsetDateTime offsetDateTime, ProcessType processType) {
        RaoParameters raoParameters = getRaoParameters();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        String raoParametersDestinationPath = MinioStorageHelper.makeDestinationMinioPath(offsetDateTime, processType, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of(zoneId)) + RAO_PARAMETERS_FILE_NAME;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadArtifact(raoParametersDestinationPath, bais);
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
        minioAdapter.uploadOutput(outputFilePath, is, processId, ttcRes, processTargetDate + "/" + processTargetDate.plusHours(1));
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

    String saveNetworkInUcteFormat(Network network, String filePath, GridcapaFileGroup fileGroup, String fileType, OffsetDateTime offsetDateTime) {
        exportAndUploadFile(network, filePath, "UCTE", "uct", fileGroup, fileType, offsetDateTime);
        return minioAdapter.generatePreSignedUrl(filePath);
    }

    private void exportAndUploadFile(Network network, String filePath, String format, String ext,
                                     GridcapaFileGroup fileGroup, String fileType, OffsetDateTime offsetDateTime) {
        MemDataSource memDataSource = new MemDataSource();
        Exporters.export(format, network, new Properties(), memDataSource);
        try (InputStream is = memDataSource.newInputStream("", ext)) {
            switch (fileGroup) {
                case INPUT:
                    minioAdapter.uploadInput(filePath, is, processId, fileType, offsetDateTime + "/" + offsetDateTime.plusHours(1));
                    break;
                case ARTIFACT:
                    minioAdapter.uploadArtifact(filePath, is, processId, fileType, offsetDateTime + "/" + offsetDateTime.plusHours(1));
                    break;
                case OUTPUT:
                    minioAdapter.uploadOutput(filePath, is, processId, fileType, offsetDateTime + "/" + offsetDateTime.plusHours(1));
                    break;
                case EXTENDED_OUTPUT:
                    minioAdapter.uploadExtendedOutput(filePath, is, processId, fileType, offsetDateTime + "/" + offsetDateTime.plusHours(1));
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("File group %s not supported", fileGroup));
            }
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to save network", e);
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

    public FileResource generateFileResource(String filePath) {
        try {
            String targetPath = getTargetFilePath(filePath);
            String filename = FilenameUtils.getName(filePath);
            String url = minioAdapter.generatePreSignedUrl(targetPath);
            return new FileResource(filename, url);
        } catch (Exception e) {
            throw new CseInternalException("Exception in MinIO connection.", e);
        }
    }

    private String getTargetFilePath(String filePath) {
        if (minioBasePath.isEmpty()) {
            return filePath;
        } else {
            if (minioBasePath.endsWith("/")) {
                return minioBasePath + filePath;
            } else {
                return minioBasePath + "/" + filePath;
            }
        }
    }
}
