/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.ttc_res;

import com.farao_community.farao.cse.data.cnec.CracResultsHelper;
import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreator;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracImporter;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class TtcResultTest {
    private TtcResult.TtcFiles ttcFiles;
    private CracResultsHelper cracResultsHelper;

    @BeforeEach
    public void setUp() {
        ttcFiles = new TtcResult.TtcFiles(
            "20210101_1930_185_Initial_CSE1.uct",
            "20210101_1930_185_CSE1.uct",
            "mockCrac.json",
            "20210101_1930_185_GSK_CO_CSE1.xml",
            "20210101_2D5_NTC_reductions_CSE1.xml",
            "2020-12-30T18:28Z",
            "secure_CGM_with_PRA.uct"
        );
        InputStream cracInputStream = getClass().getResourceAsStream("pst_and_topo/crac.xml");
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(cracInputStream);

        Network network = Importers.loadNetwork("pst_and_topo/network.uct", getClass().getResourceAsStream("pst_and_topo/network.uct"));
        CseCracCreator cseCracCreator = new CseCracCreator();
        CseCracCreationContext cseCracCreationContext = cseCracCreator.createCrac(cseCrac, network, null, new CracCreationParameters());
        RaoResult raoResult = new RaoResultImporter().importRaoResult(getClass().getResourceAsStream("pst_and_topo/raoResult.json"), cseCracCreationContext.getCrac());

        cracResultsHelper = new CracResultsHelper(cseCracCreationContext, raoResult, new ArrayList<>());
    }

    private TtcResult.ProcessData initProcessData(LimitingCause limitingCause, double finalItalianImport, double mniiOffsetValue) {
        Map<String, Double> countryBalances = new HashMap<>();
        countryBalances.put("FR", -1839.0);
        countryBalances.put("CH", 320.0);
        countryBalances.put("DE", 4216.0);
        countryBalances.put("IT", -4271.0);
        countryBalances.put("SI", 621.0);
        countryBalances.put("AT", -1250.0);

        Map<String, Double> borderExchanges = new HashMap<>();
        borderExchanges.put("IT-CH", -1884.);
        borderExchanges.put("CH-FR", 741.);
        borderExchanges.put("FR-DE", -595.);
        borderExchanges.put("CH-DE", -1199.);
        borderExchanges.put("IT-FR", -1064.);
        borderExchanges.put("IT-SI", -665.);
        borderExchanges.put("IT-AT", -219.);

        Map<String, Double> splittingFactorPerCountry = new HashMap<>();
        splittingFactorPerCountry.put("FR", 0.3);
        splittingFactorPerCountry.put("CH", 0.5);
        splittingFactorPerCountry.put("AT", 0.02);
        splittingFactorPerCountry.put("SI", 0.08);

        return new TtcResult.ProcessData(
            Collections.emptySet(),
            borderExchanges,
            splittingFactorPerCountry,
            countryBalances,
            limitingCause,
            finalItalianImport,
            mniiOffsetValue,
            "2021-01-01T18:30Z"
        );
    }

    private static byte[] writeInBytes(Timestamp timestamp) throws JAXBException {
        Marshaller marshaller = JAXBContext.newInstance(Timestamp.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(timestamp, bos);
        return bos.toByteArray();
    }

    @Test
    void validateTtcResultCreation() throws JAXBException {
        TtcResult.ProcessData processData = initProcessData(LimitingCause.CRITICAL_BRANCH, 4200, 3925);
        Timestamp ttcResults = TtcResult.generate(ttcFiles, processData, cracResultsHelper, Collections.EMPTY_MAP, Collections.EMPTY_MAP);

        assertEquals("2021-01-01T18:30Z", ttcResults.getTime().getV());
        String ttcResultXml = new String(writeInBytes(ttcResults)).trim();
        String expectedTtcResultXml = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("expected_ttc_result.xml"))))
            .lines().collect(Collectors.joining("\n"));
        Assertions.assertEquals(expectedTtcResultXml, ttcResultXml);
    }

    @Test
    void testTtcAndMniiValuesForGlskLimitationWithPositiveValues() {
        TtcResult.ProcessData processData = initProcessData(LimitingCause.GLSK_LIMITATION, 1200, 700);
        Timestamp ttcResults = TtcResult.generate(ttcFiles, processData, cracResultsHelper, Collections.EMPTY_MAP, Collections.EMPTY_MAP);

        assertEquals(BigInteger.valueOf(1200), ttcResults.getTTC().getV());
        assertEquals(BigInteger.valueOf(500), ttcResults.getMNII().getV());
    }

    @Test
    void testTtcAndMniiValuesForGlskLimitationWithNegativeMniiValue() {
        TtcResult.ProcessData processData = initProcessData(LimitingCause.GLSK_LIMITATION, 700, 2100);
        Timestamp ttcResults = TtcResult.generate(ttcFiles, processData, cracResultsHelper, Collections.EMPTY_MAP, Collections.EMPTY_MAP);

        assertEquals(BigInteger.valueOf(700), ttcResults.getTTC().getV());
        assertEquals(BigInteger.valueOf(-1400), ttcResults.getMNII().getV());
    }

    @Test
    void testTtcAndMniiValuesForCriticalBranch() {
        TtcResult.ProcessData processData = initProcessData(LimitingCause.CRITICAL_BRANCH, 700, 2100);
        Timestamp ttcResults = TtcResult.generate(ttcFiles, processData, cracResultsHelper, Collections.EMPTY_MAP, Collections.EMPTY_MAP);

        assertEquals(BigInteger.valueOf(2800), ttcResults.getTTC().getV());
        assertEquals(BigInteger.valueOf(700), ttcResults.getMNII().getV());
    }

    @Test
    void testPreprocessedPstsNotModifiedByRaoCorrectlyAddedToTtcRes() {
        Map<String, Integer> preprocessedPsts = new HashMap<>();
        preprocessedPsts.put("PST_cra_7_BBE2AA1  BBE3AA1  1", 2);

        TtcResult.ProcessData processData = initProcessData(LimitingCause.CRITICAL_BRANCH, 700, 2100);
        Timestamp ttcResults = TtcResult.generate(ttcFiles, processData, cracResultsHelper, preprocessedPsts, Collections.EMPTY_MAP);

        assertEquals("cra_7", ttcResults.getResults().getPreventive().getAction().get(2).getName().getV());
        assertEquals(2, ttcResults.getResults().getPreventive().getAction().get(2).getPSTtap().getV().intValue());
    }

    @Test
    void testPreprocessedPstsModifiedByRaoNotAddedToTtcRes() {
        Map<String, Integer> preprocessedPsts = new HashMap<>();
        preprocessedPsts.put("PST_cra_3_BBE2AA1  BBE3AA1  1", 2);

        TtcResult.ProcessData processData = initProcessData(LimitingCause.CRITICAL_BRANCH, 700, 2100);
        Timestamp ttcResults = TtcResult.generate(ttcFiles, processData, cracResultsHelper, preprocessedPsts, Collections.EMPTY_MAP);

        assertEquals("cra_3", ttcResults.getResults().getPreventive().getAction().get(1).getName().getV());
        assertNotEquals(2, ttcResults.getResults().getPreventive().getAction().get(1).getPSTtap().getV().intValue());
    }
}
