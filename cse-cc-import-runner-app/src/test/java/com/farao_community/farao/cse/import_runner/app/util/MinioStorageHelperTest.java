/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.util;

import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class MinioStorageHelperTest {

    @Test
    void makeArtifactsMinioDestinationPathTest() {
        assertEquals("CSE/IMPORT/D2CC/2022/01/28/11_30/ARTIFACTS/", MinioStorageHelper.makeDestinationMinioPath(OffsetDateTime.parse("2022-01-28T10:30Z"), ProcessType.D2CC, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of("Europe/Paris"), false));
    }

    @Test
    void makeOutputsMinioDestinationPathTest() {
        assertEquals("CSE/IMPORT/IDCC/2022/01/28/16_30/OUTPUTS/", MinioStorageHelper.makeDestinationMinioPath(OffsetDateTime.parse("2022-01-28T15:30Z"), ProcessType.IDCC, MinioStorageHelper.FileKind.OUTPUTS, ZoneId.of("Europe/Paris"), false));
    }

    @Test
    void makeAConfigurationsMinioDestinationPathTest() {
        assertEquals("CSE/IMPORT/D2CC/2022/01/28/20_30/ARTIFACTS/", MinioStorageHelper.makeDestinationMinioPath(OffsetDateTime.parse("2022-01-28T19:30Z"), ProcessType.D2CC, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of("Europe/Paris"), false));
    }

    @Test
    void makeArtifactsMinioDestinationAdaptedPathTest() {
        assertEquals("CSE/IMPORT_EC/D2CC/2022/01/28/11_30/ARTIFACTS/", MinioStorageHelper.makeDestinationMinioPath(OffsetDateTime.parse("2022-01-28T10:30Z"), ProcessType.D2CC, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of("Europe/Paris"), true));
    }

    @Test
    void makeOutputsMinioDestinationAdaptedPathTest() {
        assertEquals("CSE/IMPORT_EC/IDCC/2022/01/28/16_30/OUTPUTS/", MinioStorageHelper.makeDestinationMinioPath(OffsetDateTime.parse("2022-01-28T15:30Z"), ProcessType.IDCC, MinioStorageHelper.FileKind.OUTPUTS, ZoneId.of("Europe/Paris"), true));
    }

    @Test
    void makeAConfigurationsMinioDestinationAdaptedPathTest() {
        assertEquals("CSE/IMPORT_EC/D2CC/2022/01/28/20_30/ARTIFACTS/", MinioStorageHelper.makeDestinationMinioPath(OffsetDateTime.parse("2022-01-28T19:30Z"), ProcessType.D2CC, MinioStorageHelper.FileKind.ARTIFACTS, ZoneId.of("Europe/Paris"), true));
    }
}
