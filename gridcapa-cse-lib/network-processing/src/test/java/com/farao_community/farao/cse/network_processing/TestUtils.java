package com.farao_community.farao.cse.network_processing;

import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.converter.UcteExporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TestUtils {

    private TestUtils() {

    }

    public static void assertNetworksAreEqual(Network network, String reference) throws IOException {
        MemDataSource dataSource = new MemDataSource();

        UcteExporter exporter = new UcteExporter();
        exporter.export(network, new Properties(), dataSource);

        try (InputStream actual = dataSource.newInputStream(null, "uct");
             InputStream expected = TestUtils.class.getResourceAsStream(reference)) {
            compareTxt(expected, actual, Arrays.asList(1, 2));
        }
    }

    private static void compareTxt(InputStream expected, InputStream actual, List<Integer> excludedLines) {
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
