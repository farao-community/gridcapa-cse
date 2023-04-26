/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.cse.import_runner.app.util.FileUtil;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.cse.import_runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.cse.import_runner.app.util.MinioStorageHelper;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class FileExporter {

    private static final String NETWORK_FILE_NAME = "network_pre_processed.xiidm";
    private static final String JSON_CRAC_FILE_NAME = "crac.json";
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters.json";
    private static final DateTimeFormatter OUTPUTS_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final String XIIDM_FORMAT = "XIIDM";

    private final MinioAdapter minioAdapter;
    private final ProcessConfiguration processConfiguration;

    private static final String PROCESS_TYPE_PREFIX = "CSE_IMPORT_";
    private static final String PROCESS_TYPE_IMPORT_EC_PREFIX = "CSE_IMPORT_EC_";

    public FileExporter(MinioAdapter minioAdapter, ProcessConfiguration processConfiguration) {
        this.minioAdapter = minioAdapter;
        this.processConfiguration = processConfiguration;
    }

    public String saveCracInJsonFormat(Crac crac, OffsetDateTime processTargetDateTime, ProcessType processType, boolean isImportEc) {
        MemDataSource memDataSource = new MemDataSource();
        try (OutputStream os = memDataSource.newOutputStream(JSON_CRAC_FILE_NAME, false)) {
            CracExporters.exportCrac(crac, "Json", os);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to save converted CRAC file.", e);
        }
        String cracPath = MinioStorageHelper.makeDestinationMinioPath(processTargetDateTime, processType, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of(processConfiguration.getZoneId()), isImportEc) + JSON_CRAC_FILE_NAME;
        try (InputStream is = memDataSource.newInputStream(JSON_CRAC_FILE_NAME)) {
            minioAdapter.uploadArtifactForTimestamp(cracPath, is, adaptTargetProcessName(processType, isImportEc), "", processTargetDateTime);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }

    public String saveNetworkInArtifact(Network network, OffsetDateTime processTargetDateTime, String fileType, ProcessType processType, boolean isImportEc) {
        String networkPath = MinioStorageHelper.makeDestinationMinioPath(processTargetDateTime, processType, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of(processConfiguration.getZoneId()), isImportEc) + NETWORK_FILE_NAME;
        return saveNetworkInArtifact(network, networkPath, fileType, processTargetDateTime, processType, isImportEc);
    }

    public String saveNetworkInArtifact(Network network, String networkFilePath, String fileType, OffsetDateTime processTargetDateTime, ProcessType processType, boolean isImportEc) {
        exportAndUploadNetwork(network, XIIDM_FORMAT, GridcapaFileGroup.ARTIFACT, networkFilePath, fileType, processTargetDateTime, processType, isImportEc);
        return minioAdapter.generatePreSignedUrl(networkFilePath);
    }

    public String saveRaoParameters(OffsetDateTime offsetDateTime, ProcessType processType, boolean isImportEc) {
        return saveRaoParameters("", Collections.emptyList(), offsetDateTime, processType, isImportEc);
    }

    public String saveRaoParameters(String basePath, List<String> remedialActionsAppliedInPreviousStep, OffsetDateTime offsetDateTime, ProcessType processType, boolean isImportEc) {
        RaoParameters raoParameters = getRaoParameters(remedialActionsAppliedInPreviousStep);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        String raoParametersDestinationPath = getRaoParametersDestinationPath(basePath, processType, offsetDateTime, isImportEc);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadArtifactForTimestamp(raoParametersDestinationPath, bais, adaptTargetProcessName(processType, isImportEc), "", offsetDateTime);
        return minioAdapter.generatePreSignedUrl(raoParametersDestinationPath);
    }

    String getRaoParametersDestinationPath(String basePath, ProcessType processType, OffsetDateTime offsetDateTime, boolean isImportEc) {
        return !StringUtils.isBlank(basePath) ? basePath + RAO_PARAMETERS_FILE_NAME : MinioStorageHelper.makeDestinationMinioPath(offsetDateTime, processType, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of(processConfiguration.getZoneId()), isImportEc) + RAO_PARAMETERS_FILE_NAME;
    }

    RaoParameters getRaoParameters(List<String> remedialActionsAppliedInPreviousStep) {
        RaoParameters raoParameters = loadRaoParameters();
        List<List<String>> combinedRas = raoParameters.getTopoOptimizationParameters().getPredefinedCombinations();
        if (!remedialActionsAppliedInPreviousStep.isEmpty()) {
            combinedRas.add(remedialActionsAppliedInPreviousStep);
        }
        return raoParameters;
    }

    public RaoParameters loadRaoParameters() {
        return RaoParameters.load();
    }

    String saveTtcResult(Timestamp timestamp, OffsetDateTime processTargetDate, ProcessType processType, boolean isImportEc) {
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
        String outputFilePath = getFilePath(processTargetDate, processType, isImportEc);
        minioAdapter.uploadOutputForTimestamp(outputFilePath, is, adaptTargetProcessName(processType, isImportEc), processConfiguration.getTtcRes(), processTargetDate);
        return minioAdapter.generatePreSignedUrl(outputFilePath);
    }

    String getFilePath(OffsetDateTime processTargetDate, ProcessType processType, boolean isImportEc) {
        String filename;
        ZonedDateTime targetDateInEuropeZone = processTargetDate.atZoneSameInstant(ZoneId.of(processConfiguration.getZoneId()));
        if (processType == ProcessType.D2CC) {
            String dateAndTime = targetDateInEuropeZone.format(OUTPUTS_DATE_TIME_FORMATTER);
            filename = "TTC_Calculation_" + dateAndTime + "_2D0_CO_CSE1.xml";
        } else {
            String date = targetDateInEuropeZone.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            filename = date + "_XBID2_TTCRes_CSE1.xml";
        }
        return MinioStorageHelper.makeDestinationMinioPath(processTargetDate, processType, MinioStorageHelper.FileKind.OUTPUTS, ZoneId.of(processConfiguration.getZoneId()), isImportEc) + filename;
    }

    public String exportAndUploadNetwork(Network network, String format, GridcapaFileGroup fileGroup, String filePath, String fileType, OffsetDateTime offsetDateTime, ProcessType processType, boolean isImportEc) {
        try (InputStream is = getNetworkInputStream(network, format)) {
            switch (fileGroup) {
                case OUTPUT:
                    minioAdapter.uploadOutputForTimestamp(filePath, is, adaptTargetProcessName(processType, isImportEc), fileType, offsetDateTime);
                    break;
                case ARTIFACT:
                    minioAdapter.uploadArtifactForTimestamp(filePath, is, adaptTargetProcessName(processType, isImportEc), fileType, offsetDateTime);
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
                network.write("UCTE", new Properties(), memDataSource);
                return memDataSource.newInputStream("", "uct");
            case XIIDM_FORMAT:
                network.write(XIIDM_FORMAT, new Properties(), memDataSource);
                return memDataSource.newInputStream("", "xiidm");
            default:
                throw new UnsupportedOperationException(String.format("Network format %s not supported", format));
        }
    }

    public String getNetworkFilePathByState(OffsetDateTime processTargetDate, ProcessType processType, boolean isImportEc, String state, String baseCaseCgmVersion) {
        String filename = getNetworkNameByState(processTargetDate, processType, state, baseCaseCgmVersion);
        return MinioStorageHelper.makeDestinationMinioPath(processTargetDate, processType, MinioStorageHelper.FileKind.OUTPUTS, ZoneId.of(processConfiguration.getZoneId()), isImportEc) + filename;
    }

    public String getNetworkNameByState(OffsetDateTime processTargetDate, ProcessType processType, String state, String version) {
        ZonedDateTime targetDateInEuropeZone = processTargetDate.atZoneSameInstant(ZoneId.of(processConfiguration.getZoneId()));
        String dateAndTime = targetDateInEuropeZone.format(OUTPUTS_DATE_TIME_FORMATTER);
        int dayOfWeek = targetDateInEuropeZone.getDayOfWeek().getValue();
        String filename;
        if (processType == ProcessType.D2CC) {
            filename = dateAndTime + "_2D" + dayOfWeek + "_ce_" + state + "_CSE" + version + ".uct";
        } else {
            filename = dateAndTime + "_" + processTargetDate.getHour() + dayOfWeek + "_" + state + "_CSE" + version + ".uct";
        }
        return filename;
    }

    public String retrieveVersionFromBaseCaseNetwork(String cgmUrl) {
        String baseCaseFileName = FileUtil.getFilenameFromUrl(cgmUrl);
        return baseCaseFileName.substring(baseCaseFileName.lastIndexOf("_UX") + 3, baseCaseFileName.lastIndexOf("_UX") + 4);
    }

    public String getZoneId() {
        return processConfiguration.getZoneId();
    }

    private String adaptTargetProcessName(ProcessType processType, boolean isImportEc) {
        return (isImportEc ? PROCESS_TYPE_IMPORT_EC_PREFIX : PROCESS_TYPE_PREFIX) + processType;
    }

}
