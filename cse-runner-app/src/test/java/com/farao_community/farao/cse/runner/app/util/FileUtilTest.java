/*
 *
 *  * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.cse.runner.app.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class FileUtilTest {

    @Test
    void testGetFilename() {
        assertEquals("test_file.xml", FileUtil.getFilenameFromUrl("file://fake_folder/test_file.xml?variableId=4"));
    }
}
