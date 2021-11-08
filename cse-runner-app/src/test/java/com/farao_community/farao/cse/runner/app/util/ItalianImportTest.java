/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.runner.app.util;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class ItalianImportTest {

    @Test
    void computeItalianImportTest() {
        String filename = "20210901_2230_test_network.uct";
        Network network = Importers.loadNetwork(filename, getClass().getResourceAsStream(filename));
        double itInitialImport = ItalianImport.compute(network);
        assertEquals(6000, itInitialImport, 2);
    }
}
