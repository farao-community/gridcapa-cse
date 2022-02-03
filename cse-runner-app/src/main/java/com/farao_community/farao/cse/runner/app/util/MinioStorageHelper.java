/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.util;

import com.farao_community.farao.cse.runner.api.resource.ProcessType;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public final class MinioStorageHelper {

    private static final String MINIO_SEPARATOR = "/";
    private static final String REGION = "CSE";

    private MinioStorageHelper() {
        // should not be constructed
    }

    public static String makeDestinationMinioPath(OffsetDateTime offsetDateTime, ProcessType processType, FileKind filekind, ZoneId zoneId) {
        ZonedDateTime targetDateTime = offsetDateTime.atZoneSameInstant(zoneId);
        return REGION + MINIO_SEPARATOR
            + processType + MINIO_SEPARATOR
            + targetDateTime.getYear() + MINIO_SEPARATOR
            + String.format("%02d", targetDateTime.getMonthValue()) + MINIO_SEPARATOR
            + String.format("%02d", targetDateTime.getDayOfMonth()) + MINIO_SEPARATOR
            + String.format("%02d", targetDateTime.getHour()) + "_30" + MINIO_SEPARATOR
            + filekind + MINIO_SEPARATOR;
    }

    // TODO discuss me create an enum FilesKind in task-manager api
    public enum FileKind {
        ARTIFACTS,
        OUTPUTS
    }
}
