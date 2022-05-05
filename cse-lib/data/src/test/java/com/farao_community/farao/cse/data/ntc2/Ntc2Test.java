/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ntc2;

import com.farao_community.farao.cse.data.CseDataException;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class Ntc2Test {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ntc2Test.class);

    private Map<String, InputStream> test1Nt2Files;
    private Map<String, InputStream> test2Nt2Files;

    @BeforeEach
    void setUp() throws IOException {
        test1Nt2Files = getNtc2Files("test1");
        test2Nt2Files = getNtc2Files("test2");
    }

    private Map<String, InputStream> getNtc2Files(String directory) throws IOException {
        return Files.list(Paths.get(Objects.requireNonNull(getClass().getResource(directory)).getPath()))
            .collect(Collectors.toMap(
                path -> path.getFileName().toString(),
                path -> {
                    try {
                        return new FileInputStream(path.toFile());
                    } catch (FileNotFoundException e) {
                        LOGGER.warn(() -> path + " not found.");
                        return null;
                    }
                }
            ));
    }

    @Test
    void functionalTest1() {
        Ntc2 ntc2 = Ntc2.create(OffsetDateTime.parse("2017-05-31T22:00Z"), test1Nt2Files);
        assertEquals(238, ntc2.getExchange(Country.AT));
        assertEquals(2260, ntc2.getExchange(Country.FR));
        assertEquals(435, ntc2.getExchange(Country.SI));
        assertEquals(2767, ntc2.getExchange(Country.CH));
    }

    @Test
    void functionalTest2() {
        Ntc2 ntc2 = Ntc2.create(OffsetDateTime.parse("2017-06-01T10:00Z"), test1Nt2Files);
        assertEquals(239, ntc2.getExchange(Country.AT));
        assertEquals(2295, ntc2.getExchange(Country.FR));
        assertEquals(438, ntc2.getExchange(Country.SI));
        assertEquals(1683, ntc2.getExchange(Country.CH));
    }

    @Test
    void functionalTest3() {
        Ntc2 ntc2 = Ntc2.create(OffsetDateTime.parse("2017-06-01T21:00Z"), test1Nt2Files);
        assertEquals(227, ntc2.getExchange(Country.AT));
        assertEquals(2165, ntc2.getExchange(Country.FR));
        assertEquals(424, ntc2.getExchange(Country.SI));
        assertEquals(1633, ntc2.getExchange(Country.CH));
    }

    @Test
    void assertThrowsWhenDataIsMissing() {
        assertThrows(CseDataException.class, () -> Ntc2.create(OffsetDateTime.parse("2021-02-07T23:00Z"), test2Nt2Files));
    }

    @Test
    void assertThrowsWhenTargetDateTimeOutOfBound() {
        assertThrows(CseDataException.class, () -> Ntc2.create(OffsetDateTime.parse("2021-06-01T22:00Z"), test1Nt2Files));
    }

    @Test
    void testImportFailsWithMissingPositions() {
        String filename = "NTC2_20170601_2D4_AT-IT1.xml";
        Map<String, InputStream> isMap = Map.of(filename, Objects.requireNonNull(getClass().getResourceAsStream(filename)));
        OffsetDateTime timestamp = OffsetDateTime.parse("2017-06-01T16:30Z");
        Throwable e = assertThrows(CseDataException.class, () -> Ntc2.create(timestamp, isMap));
        assertEquals("Impossible to import NTC2 file for area: 10YAT-APG------L", e.getMessage());
        Throwable nestedE = e.getCause();
        assertEquals("Impossible to retrieve NTC2 position 19. It does not exist", nestedE.getMessage());
    }
}
