/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ttc_rao;

import com.farao_community.farao.cse.data.cnec.CracResultsHelper;
import com.farao_community.farao.cse.data.xsd.ttc_rao.*;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreator;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracImporter;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
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

    private CracResultsHelper getCracResultsHelper(String raoResultFileName) {
        InputStream cracInputStream = getClass().getResourceAsStream("crac.xml");
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(cracInputStream);
        Network network = Network.read("network.uct", getClass().getResourceAsStream("network.uct"));
        CseCracCreator cseCracCreator = new CseCracCreator();
        CseCracCreationContext cseCracCreationContext = cseCracCreator.createCrac(cseCrac, network, null, new CracCreationParameters());
        InputStream raoResultInputStream = getClass().getResourceAsStream(raoResultFileName);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultInputStream, cseCracCreationContext.getCrac());
        return new CracResultsHelper(cseCracCreationContext, raoResult, new ArrayList<>());
    }

    @Test
    void testSecureCseRaoResultGeneration() throws JAXBException {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("raoResult-secure.json");
        checkGeneratedXmlMatchesExpectedXml(cracResultsHelper, "expected-simple-secure-cse-rao-result.xml");
    }

    @Test
    void testUnsecureCseRaoResultGeneration() throws JAXBException {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("raoResult-unsecure.json");
        checkGeneratedXmlMatchesExpectedXml(cracResultsHelper, "expected-simple-unsecure-cse-rao-result.xml");
    }

    @Test
    void testSecureCseRaoResultGenerationWhenCostIsNull() throws JAXBException {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("raoResult-null-cost.json");
        checkGeneratedXmlMatchesExpectedXml(cracResultsHelper, "expected-simple-secure-cse-rao-result.xml");
    }

    @Test
    void testFailedCseRaoResultGeneration() throws JAXBException {
        CseRaoResult cseRaoResult = TtcRao.failed(OffsetDateTime.parse("2022-05-06T16:30Z"));
        assertEqualsXml(cseRaoResult, "expected-simple-failed-cse-rao-result.xml");
    }

    @Test
    void testPreprocessedPstsModifiedByRaoNotAddedToTtcRes() {
        Map<String, Integer> preprocessedPsts = new HashMap<>();
        preprocessedPsts.put("PST_cra_3_BBE2AA1  BBE3AA1  1", 2);
        CracResultsHelper cracResultsHelper = getCracResultsHelper("raoResult-secure.json");
        CseRaoResult cseRaoResult = TtcRao.generate(OffsetDateTime.parse("2022-05-06T16:30Z"), cracResultsHelper, preprocessedPsts);

        assertEquals("cra_3", cseRaoResult.getResults().getPreventiveResult().getPreventiveActions().getAction().get(1).getName());
        assertNotEquals(2, cseRaoResult.getResults().getPreventiveResult().getPreventiveActions().getAction().get(1).getPSTtap().getV().intValue());
    }

    @Test
    void testPreprocessedPstsNotModifiedByRaoCorrectlyAddedToTtcRes() {
        Map<String, Integer> preprocessedPsts = new HashMap<>();
        preprocessedPsts.put("PST_cra_7_BBE2AA1  BBE3AA1  1", 5);
        CracResultsHelper cracResultsHelper = getCracResultsHelper("raoResult-secure.json");
        CseRaoResult cseRaoResult = TtcRao.generate(OffsetDateTime.parse("2022-05-06T16:30Z"), cracResultsHelper, preprocessedPsts);

        assertEquals("cra_7", cseRaoResult.getResults().getPreventiveResult().getPreventiveActions().getAction().get(2).getName());
        assertEquals(5, cseRaoResult.getResults().getPreventiveResult().getPreventiveActions().getAction().get(2).getPSTtap().getV().intValue());
    }
}
