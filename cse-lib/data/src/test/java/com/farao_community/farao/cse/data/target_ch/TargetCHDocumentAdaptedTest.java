/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.target_ch;

import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TargetCHDocumentAdaptedTest {

    @Test
    void testImport() throws JAXBException {
        InputStream is = getClass().getResourceAsStream("20191120_Targets_CH_Adapted_completetest.xml");
        TargetChDocumentAdapted targetChDocument = TargetChDocumentAdapted.create(is);

        checkValues(targetChDocument, "2020-01-01T00:00Z", 100, 80);
        checkValues(targetChDocument, "2020-03-31T07:00Z", 150, 40);
        checkValues(targetChDocument, "2020-02-01T23:00Z", 200, 60);
        checkValues(targetChDocument, "2020-01-05T00:00Z", 100, 20);
        checkValues(targetChDocument, "2020-05-01T00:00Z", 90, 70);
        checkValues(targetChDocument, "2020-06-01T07:00Z", 75, 30);
        checkValues(targetChDocument, "2020-09-30T23:00Z", 50, 50);
        checkValues(targetChDocument, "2020-05-01T23:00Z", 50, 50);
        checkValues(targetChDocument, "2020-10-01T06:00Z", 95, 76);
        checkValues(targetChDocument, "2020-10-01T22:00Z", 120, 40);
        checkValues(targetChDocument, "2020-12-26T23:00Z", 130, 60);
        checkValues(targetChDocument, "2020-12-27T23:00Z", 130, 20);
    }

    private void checkValues(TargetChDocumentAdapted targetChDocument, String targetDateTime, double mannoMendrisioTarget, double magadinoSoazzaTarget) {
        Map<String, List<OutageInformation>> information = targetChDocument.getOutagesInformationPerLineId(OffsetDateTime.parse(targetDateTime));
        OutageInformation mannoMendrisioInformation = getOutageInformation(information, "150kV Manno-Mendrisio");
        OutageInformation magadinoSoazzaInformation = getOutageInformation(information, "220kV Magadino-Soazza 'Moesa Sud'");
        assertEquals(mannoMendrisioTarget, mannoMendrisioInformation.getFixedFlow());
        assertEquals(magadinoSoazzaTarget, magadinoSoazzaInformation.getFixedFlow());
    }

    private OutageInformation getOutageInformation(Map<String, List<OutageInformation>> information, String outageName) {
        return information.get("ml_0001").stream()
                .filter(outageInformation -> outageInformation.getName().equals(outageName))
                .findFirst()
                .orElseThrow();
    }
}
