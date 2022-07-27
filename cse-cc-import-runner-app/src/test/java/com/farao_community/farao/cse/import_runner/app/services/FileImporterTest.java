/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.data.target_ch.LineFixedFlows;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class FileImporterTest {

    @Autowired
    private FileImporter fileImporter;

    @Test
    void testCseCracImport() throws IOException {
        CseCrac cseCrac = fileImporter.importCseCrac(Objects.requireNonNull(getClass().getResource("20210901_2230_213_CRAC_CO_CSE1.xml")).toString());
        assertEquals(2, cseCrac.getCracDocument().getCRACSeries().get(0).getCriticalBranches().getBaseCaseBranches().getBranch().size());
    }

    @Test
    void testTargetChImport() {
        LineFixedFlows lineFixedFlows = fileImporter.importLineFixedFlowFromTargetChFile(
            OffsetDateTime.parse("2021-01-01T00:00Z"),
            Objects.requireNonNull(getClass().getResource("20210901_2230_213_CRAC_CO_CSE1.xml")).toString());
        assertNotNull(lineFixedFlows);
    }
}
