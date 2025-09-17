/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.services.FileExporter;
import com.farao_community.farao.cse.import_runner.app.util.MinioStorageHelper;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.dichotomy.api.NetworkExporter;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public class CseNetworkExporter implements NetworkExporter {
    private final CseRequest cseRequest;
    private final FileExporter fileExporter;

    private static final Logger LOGGER = LoggerFactory.getLogger(CseNetworkExporter.class);

    public CseNetworkExporter(final CseRequest cseRequest, final FileExporter fileExporter) {
        this.cseRequest = cseRequest;
        this.fileExporter = fileExporter;
    }

    @Override
    public void export(final Network network) {
        final String variantName = network.getVariantManager().getWorkingVariantId();
        export(network, variantName);
    }

    public void export(final Network network, String step) {
        final OffsetDateTime processTargetDateTime = cseRequest.getTargetProcessDateTime();
        final ProcessType processType = cseRequest.getProcessType();
        final boolean isImportEcProcess = cseRequest.isImportEcProcess();

        final String basePath = MinioStorageHelper.makeDestinationMinioPath(processTargetDateTime, processType, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of(fileExporter.getZoneId()), isImportEcProcess);
        final String separator = basePath.endsWith("/") ? "" : "/";
        final String baseDirPathForCurrentStep = String.format("%s%s%s/", basePath, separator, step);

        final String scaledNetworkInUcteFormatName = network.getNameOrId() + "-diverged.uct";
        final String filePath = baseDirPathForCurrentStep + scaledNetworkInUcteFormatName;

        LOGGER.info("Exporting network at: {}", filePath);
        fileExporter.exportAndUploadNetwork(network, "UCTE", GridcapaFileGroup.ARTIFACT,
                filePath,
                "", processTargetDateTime, processType, isImportEcProcess);
    }
}
