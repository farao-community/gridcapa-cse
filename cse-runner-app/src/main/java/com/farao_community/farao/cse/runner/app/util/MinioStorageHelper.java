/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.util;

import com.farao_community.farao.cse.runner.api.resource.ProcessType;

import java.time.OffsetDateTime;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public final class MinioStorageHelper {

    private static final String MINIO_SEPARATOR = "/";
    private static final String ZONE = "CSE";
    private static final String ARTIFACTS = "ARTIFACTS";
    private static final String OUTPUTS = "OUTPUTS";
    private static final String CONFIGURATIONS = "CONFIGURATIONS";

    private MinioStorageHelper() {
        // should not be constructed
    }

    public static String makeArtifactsMinioDestinationPath(OffsetDateTime targetDateTime, ProcessType processType) {
        String filesKind = makeFilesKindBaseDir(processType, ARTIFACTS);
        String formattedDate = transformDateToMinioPath(targetDateTime);
        return filesKind + formattedDate;
    }

    public static String makeOutputsMinioDestination(OffsetDateTime targetDateTime, ProcessType processType) {
        String filesKind = makeFilesKindBaseDir(processType, OUTPUTS);
        String formattedDate = transformDateToMinioPath(targetDateTime);
        return filesKind + formattedDate;
    }

    // TODO: discuss me: rao parameters file by process,  -- can't debug/rerun correctly computation in case of change in parameters, ++ saving disc space
    public static String makeGlobalConfigurationsDestinationPath(ProcessType processType) {
        return makeFilesKindBaseDir(processType, CONFIGURATIONS);
    }

    private static String makeFilesKindBaseDir(ProcessType processType, String kind) {
        return ZONE + MINIO_SEPARATOR + processType + MINIO_SEPARATOR + kind + MINIO_SEPARATOR;
    }

    private static String transformDateToMinioPath(OffsetDateTime targetDateTime) {
        return targetDateTime.getYear() + MINIO_SEPARATOR
            +  String.format("%02d", targetDateTime.getMonthValue()) + MINIO_SEPARATOR
            +  String.format("%02d", targetDateTime.getDayOfMonth()) + MINIO_SEPARATOR
            +  String.format("%02d", targetDateTime.getHour()) + ":30Z" + MINIO_SEPARATOR;
    }
}
