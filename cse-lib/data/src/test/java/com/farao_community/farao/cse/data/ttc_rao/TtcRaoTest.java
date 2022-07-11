/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ttc_rao;

import com.farao_community.farao.cse.data.cnec.CracResultsHelper;
import com.farao_community.farao.cse.data.xsd.ttc_rao.*;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class TtcRaoTest {

    private Crac crac;
    CseCracCreationContext cracCreationContext = Mockito.mock(CseCracCreationContext.class);

    @BeforeEach
    void setUp() {
        String cracFilename = "crac-for-rao-result-v1.1.json";
        crac = CracImporters.importCrac(cracFilename, Objects.requireNonNull(getClass().getResourceAsStream(cracFilename)));
        Mockito.when(cracCreationContext.getCrac()).thenReturn(crac);
    }

    private void writeResult(CseRaoResult cseRaoResult, String name) throws JAXBException, FileNotFoundException {
        Marshaller marshaller = JAXBContext.newInstance(CseRaoResult.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        FileOutputStream fileOutputStream = new FileOutputStream(name);
        marshaller.marshal(cseRaoResult, fileOutputStream);
    }

    private void assertEqualsXml(String raoResultFilename, String expectedResultFilename) throws JAXBException {
        RaoResult raoResult = new RaoResultImporter().importRaoResult(getClass().getResourceAsStream(raoResultFilename), crac);
        CseRaoResult cseRaoResult = TtcRao.generate(OffsetDateTime.parse("2022-05-06T16:30Z"), raoResult, new CracResultsHelper(cracCreationContext, raoResult, Mockito.mock(List.class)));
        assertEqualsXml(cseRaoResult, expectedResultFilename);
    }

    private void assertEqualsXml(CseRaoResult cseRaoResult, String expectedResultFilename) throws JAXBException {
        Marshaller marshaller = JAXBContext.newInstance(CseRaoResult.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(cseRaoResult, bos);
        String expectedTtcResultXml = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(expectedResultFilename))))
            .lines().collect(Collectors.joining("\n"));
        Assertions.assertEquals(expectedTtcResultXml, bos.toString().trim());
    }

    @Test
    void testUnsecureCseRaoResultGeneration() throws JAXBException {
        assertEqualsXml("rao-result-v1.1-unsecure.json", "expected-simple-unsecure-cse-rao-result.xml");
    }

    @Test
    void testSecureCseRaoResultGeneration() throws JAXBException {
        assertEqualsXml("rao-result-v1.1.json", "expected-simple-secure-cse-rao-result.xml");
    }

    @Test
    void testSecureCseRaoResultGenerationWhenCostIsNull() throws JAXBException {
        assertEqualsXml("rao-result-v1.1-null-cost.json", "expected-simple-secure-cse-rao-result.xml");
    }

    @Test
    void testFailedCseRaoResultGeneration() throws JAXBException {
        CseRaoResult cseRaoResult = TtcRao.failed(OffsetDateTime.parse("2022-05-06T16:30Z"));
        assertEqualsXml(cseRaoResult, "expected-simple-failed-cse-rao-result.xml");
    }
}
