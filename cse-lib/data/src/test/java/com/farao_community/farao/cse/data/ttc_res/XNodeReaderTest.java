/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ttc_res;

import com.farao_community.farao.cse.data.CseDataException;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class XNodeReaderTest {
    @Test
    void testXNodesLoading() {
        String path = Objects.requireNonNull(getClass().getResource("cvg_xnodes_20200714.xml")).getPath();
        assertEquals(413, XNodeReader.getXNodes(path).size());
    }

    @Test
    void testXNodesLoadingFailsBecauseOfMissingFile() {
        assertThrows(CseDataException.class, () -> XNodeReader.getXNodes("/faskePathToXNodes/xnodes.xml"));
    }

    @Test
    void testXNodesLoadingFailsBecauseOfIncorrectFile() {
        assertThrows(CseDataException.class, () -> XNodeReader.getXNodes("fake_xnodes.xml"));
    }
}
