/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ttc_rao;

import com.farao_community.farao.cse.data.cnec.CracResultsHelper;
import com.farao_community.farao.cse.data.xsd.ttc_rao.*;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cse.CseCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class TtcRaoTest {

    private void checkGeneratedXmlMatchesExpectedXml(CracResultsHelper cracResultsHelper, String expectedResultFilename) throws JAXBException {
        CseRaoResult cseRaoResult = TtcRao.generate(OffsetDateTime.parse("2022-05-06T16:30Z"), cracResultsHelper, Collections.emptyMap());
        assertEqualsXml(cseRaoResult, expectedResultFilename);
    }

    private void assertEqualsXml(CseRaoResult cseRaoResult, String expectedResultFilename) throws JAXBException {
        Marshaller marshaller = JAXBContext.newInstance(CseRaoResult.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(cseRaoResult, bos);
        String expectedTtcResultXml = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(expectedResultFilename))))
            .lines().collect(Collectors.joining("\n"));
        assertEquals(expectedTtcResultXml, bos.toString().trim());
    }

    private CracResultsHelper getCracResultsHelper(String raoResultFileName) throws IOException {
        InputStream cracInputStream = getClass().getResourceAsStream("crac.xml");

        Network network = Network.read("network.uct", getClass().getResourceAsStream("network.uct"));
        CseCracCreationContext cseCracCreationContext = (CseCracCreationContext) Crac.readWithContext("crac.xml", cracInputStream, network, null, new CracCreationParameters());
        InputStream raoResultInputStream = getClass().getResourceAsStream(raoResultFileName);
        RaoResult raoResult = RaoResult.read(raoResultInputStream, cseCracCreationContext.getCrac());
        return new CracResultsHelper(cseCracCreationContext, raoResult, network);
    }

    @Test
    void testSecureCseRaoResultGeneration() throws JAXBException, IOException {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("raoResult-secure.json");
        checkGeneratedXmlMatchesExpectedXml(cracResultsHelper, "expected-simple-secure-cse-rao-result.xml");
    }

    @Test
    void testUnsecureCseRaoResultGeneration() throws JAXBException, IOException {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("raoResult-unsecure.json");
        checkGeneratedXmlMatchesExpectedXml(cracResultsHelper, "expected-simple-unsecure-cse-rao-result.xml");
    }

    @Test
    void testSecureCseRaoResultGenerationWhenCostIsNull() throws JAXBException, IOException {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("raoResult-null-cost.json");
        checkGeneratedXmlMatchesExpectedXml(cracResultsHelper, "expected-simple-secure-cse-rao-result.xml");
    }

    @Test
    void testFailedCseRaoResultGeneration() throws JAXBException {
        CseRaoResult cseRaoResult = TtcRao.failed(OffsetDateTime.parse("2022-05-06T16:30Z"));
        assertEqualsXml(cseRaoResult, "expected-simple-failed-cse-rao-result.xml");
    }

    @Test
    void testPreprocessedPstsModifiedByRaoNotAddedToTtcRes() throws IOException {
        Map<String, Integer> preprocessedPsts = new HashMap<>();
        preprocessedPsts.put("PST_cra_3_BBE2AA1  BBE3AA1  1", 2);
        CracResultsHelper cracResultsHelper = getCracResultsHelper("raoResult-secure.json");
        CseRaoResult cseRaoResult = TtcRao.generate(OffsetDateTime.parse("2022-05-06T16:30Z"), cracResultsHelper, preprocessedPsts);

        assertEquals("cra_3", cseRaoResult.getResults().getPreventiveResult().getPreventiveActions().getAction().get(1).getName());
        assertNotEquals(2, cseRaoResult.getResults().getPreventiveResult().getPreventiveActions().getAction().get(1).getPSTtap().getV().intValue());
    }

    @Test
    void testPreprocessedPstsNotModifiedByRaoCorrectlyAddedToTtcRes() throws IOException {
        Map<String, Integer> preprocessedPsts = new HashMap<>();
        preprocessedPsts.put("PST_cra_7_BBE2AA1  BBE3AA1  1", 5);
        CracResultsHelper cracResultsHelper = getCracResultsHelper("raoResult-secure.json");
        CseRaoResult cseRaoResult = TtcRao.generate(OffsetDateTime.parse("2022-05-06T16:30Z"), cracResultsHelper, preprocessedPsts);

        assertEquals("cra_7", cseRaoResult.getResults().getPreventiveResult().getPreventiveActions().getAction().get(2).getName());
        assertEquals(5, cseRaoResult.getResults().getPreventiveResult().getPreventiveActions().getAction().get(2).getPSTtap().getV().intValue());
    }
}
