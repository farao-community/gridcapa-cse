/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.app.util;

import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class MinioStorageHelperTest {

    @Test
    void makeArtifactsMinioDestinationPathTest() {
        assertEquals("CSE/D2CC/ARTIFACTS/2022/01/28/10:30Z/", MinioStorageHelper.makeArtifactsMinioDestinationPath(OffsetDateTime.parse("2022-01-28T10:30Z"), ProcessType.D2CC));
    }

    @Test
    void makeOutputsMinioDestinationPathTest() {
        assertEquals("CSE/IDCC/OUTPUTS/2022/01/28/15:30Z/", MinioStorageHelper.makeOutputsMinioDestination(OffsetDateTime.parse("2022-01-28T15:30Z"), ProcessType.IDCC));
    }

    @Test
    void makeAConfigurationsMinioDestinationPathTest() {
        assertEquals("CSE/D2CC/CONFIGURATIONS/", MinioStorageHelper.makeGlobalConfigurationsDestinationPath(ProcessType.D2CC));
    }
}
