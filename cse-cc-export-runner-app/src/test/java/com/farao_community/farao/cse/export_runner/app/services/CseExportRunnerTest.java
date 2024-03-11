/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.export_runner.app.services;

import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class CseExportRunnerTest {

    @Autowired
    private CseExportRunner cseExportRunner;

    @Test
    void getFinalNetworkFilenameTest() {
        OffsetDateTime processTargetDate = OffsetDateTime.parse("2022-10-20T16:30Z");
        String initialCgmFilename = "20221020_1830_155_Transit_CSE1.uct";
        String actualFilenameWithoutExtension = cseExportRunner.getFinalNetworkFilenameWithoutExtension(processTargetDate, initialCgmFilename, ProcessType.IDCC);
        String expectedFilenameWithoutExtension = "20221020_1830_2D4_ce_Transit_RAO_CSE1";
        assertEquals(expectedFilenameWithoutExtension, actualFilenameWithoutExtension);

    }
}
