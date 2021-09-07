/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.network_processing;

import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.ucte.converter.UcteExporter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class CseHvdcConfigurationTest {

    @BeforeEach
    public void setUp() {

    }

    @Test
    public void roundTripNetworkOk() throws IOException {
        Network network = Importers.loadNetwork("20210219_1630_2D5_CO_CSE5.uct", getClass().getResourceAsStream("/20210219_1630_2D5_CO_CSE1.uct"), LocalComputationManager.getDefault(), new ImportConfig(), null);

        File hvdcConfigFile = new File(getClass().getResource("/HvdcConfig.json").getFile());
        CseHvdcConfiguration hvdcConfiguration = CseHvdcJsonConfiguration.importConfiguration(new FileInputStream(hvdcConfigFile));
        hvdcConfiguration.getHvdcs().forEach(hvdc -> hvdc.create(network));

        Network initialNetwork = Importers.loadNetwork("20210219_1630_2D5_CO_CSE1.uct", getClass().getResourceAsStream("/20210219_1630_2D5_CO_CSE1.uct"), LocalComputationManager.getDefault(), new ImportConfig(), null);
        hvdcConfiguration.getHvdcs().forEach(hvdc -> {
            hvdc.removeAndReset(initialNetwork, network);
        });
        testEquality(network, "/20210219_1630_2D5_CO_CSE1.uct");
    }

    @Test
    public void roundTripNetworkShouldThrow() throws IOException {
        Network network = Importers.loadNetwork("20210219_1630_2D5_CO_CSE5.uct", getClass().getResourceAsStream("/20210219_1630_2D5_CO_CSE1.uct"), LocalComputationManager.getDefault(), new ImportConfig(), null);

        File hvdcConfigFile = new File(getClass().getResource("/HvdcConfig.json").getFile());
        CseHvdcConfiguration hvdcConfiguration = CseHvdcJsonConfiguration.importConfiguration(new FileInputStream(hvdcConfigFile));
        hvdcConfiguration.getHvdcs().forEach(hvdc -> hvdc.create(network));
        network.getHvdcLines().forEach(hvdcLine -> hvdcLine.setActivePowerSetpoint(100.));

        Network initialNetwork = Importers.loadNetwork("20210219_1630_2D5_CO_CSE1.uct", getClass().getResourceAsStream("/20210219_1630_2D5_CO_CSE1.uct"), LocalComputationManager.getDefault(), new ImportConfig(), null);
        hvdcConfiguration.getHvdcs().forEach(hvdc -> {
            hvdc.removeAndReset(initialNetwork, network);
        });
        try {
            testEquality(network, "/20210219_1630_2D5_CO_CSE1.uct");
            fail();
        } catch (AssertionFailedError e) {
            assertTrue(e.getActual().toString().contains("FFG.IL11              1 2 380.00 0.00000 0.00000 -100.00 150.000 600.000 -600.00 150.000 -150.00                               F"));
        }
    }

    private void testEquality(Network network, String reference) throws IOException {
        MemDataSource dataSource = new MemDataSource();

        UcteExporter exporter = new UcteExporter();
        exporter.export(network, new Properties(), dataSource);

        try (InputStream actual = dataSource.newInputStream(null, "uct");
             InputStream expected = getClass().getResourceAsStream(reference)) {
            compareTxt(expected, actual, Arrays.asList(1, 2));
        }
    }

    protected static void compareTxt(InputStream expected, InputStream actual, List<Integer> excludedLines) {
        BufferedReader expectedReader = new BufferedReader(new InputStreamReader(expected));
        List<String> expectedLines = expectedReader.lines().collect(Collectors.toList());
        BufferedReader actualReader = new BufferedReader(new InputStreamReader(actual));
        List<String> actualLines = actualReader.lines().collect(Collectors.toList());

        for (int i = 0; i < expectedLines.size(); i++) {
            if (!excludedLines.contains(i)) {
                assertEquals(expectedLines.get(i), actualLines.get(i));
            }
        }
    }
}

