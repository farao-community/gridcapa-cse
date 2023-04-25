/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.export_runner.app;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
class FileUtilTest {

    @Test
    void getFileVersionTest() {
        assertEquals("1", FileUtil.getFileVersion("20220131_1530_155_Transit_CSE1.uct", ProcessType.IDCC));
        assertEquals("3", FileUtil.getFileVersion("20221020_1530_2D4_CO_Transit_CSE3.uct", ProcessType.D2CC));
        assertEquals("12", FileUtil.getFileVersion("20220131_1530_155_Transit_CSE12.uct", ProcessType.IDCC));
        assertEquals("11", FileUtil.getFileVersion("20220530_1130_2D4_CO_Transit_CSE11.uct", ProcessType.D2CC));
    }

    @Test
    void checkCgmFileNameTest() {
        Assertions.assertDoesNotThrow(() -> FileUtil.checkCgmFileName("http://test-url/20220131_1530_876_Transit_CSE2.uct", ProcessType.IDCC));
        Assertions.assertDoesNotThrow(() -> FileUtil.checkCgmFileName("http://test-url/20220131_1530_123_CO_Transit_CSE1.UCT", ProcessType.D2CC));
        Assertions.assertThrows(CseDataException.class, () -> FileUtil.checkCgmFileName("http://test-url/20220131_1530_87_Transit_CSE2.uct", ProcessType.IDCC));
        Assertions.assertThrows(CseDataException.class, () -> FileUtil.checkCgmFileName("http://test-url/20220131_1530_123_Transit_CSE3.uct", ProcessType.D2CC));
        Assertions.assertThrows(CseInternalException.class, () -> FileUtil.checkCgmFileName("http://test-url/20220131_1530_876_Transit_CSE2.uct", null));
    }
}
