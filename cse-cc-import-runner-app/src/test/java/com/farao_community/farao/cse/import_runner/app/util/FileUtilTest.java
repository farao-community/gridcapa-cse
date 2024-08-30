/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class FileUtilTest {

    @Test
    void testGetFilename() {
        assertEquals("test_file.xml", FileUtil.getFilenameFromUrl("file://fake_folder/test_file.xml?variableId=4"));
        assertEquals("20210901_2230_test_network.uct", FileUtil.getFilenameFromUrl("http://minio:9000/gridcapa/CSE/D2CC/IDCC-1/20210901_2230_test_network.uct?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=gridcapa%2F20211223%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20211223T092947Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=4d24c286f490fcb18eb078cf21c1503d5e4eb557337469dda3c86d7b9998bf09"));
    }

    @Test
    void testGetDayOfWeekAndDifferenceBetweenBusinessHourAndPublicationHourFromInputCgm() {
        assertEquals("153", FileUtil.getDayOfWeekAndDifferenceBetweenBusinessHourAndPublicationHourFromInputCgm("20240731_1430_153_UX0.uct"));
        assertNotEquals("153", FileUtil.getDayOfWeekAndDifferenceBetweenBusinessHourAndPublicationHourFromInputCgm("20240731_1430_193_UX0.uct"));
        assertEquals("112", FileUtil.getDayOfWeekAndDifferenceBetweenBusinessHourAndPublicationHourFromInputCgm("20240731_1430_INITIAL_112_UX0.uct"));
    }
}
