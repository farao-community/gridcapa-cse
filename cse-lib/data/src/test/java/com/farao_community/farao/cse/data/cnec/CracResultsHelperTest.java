/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.cnec;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cse.CseCrac;
import com.powsybl.openrao.data.craccreation.creator.cse.CseCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cse.CseCracCreator;
import com.powsybl.openrao.data.craccreation.creator.cse.CseCracImporter;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultjson.RaoResultImporter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class CracResultsHelperTest {

    @Test
    void preventivePstRangeActionsRetrievingTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("pst_and_topo/crac.xml", "pst_and_topo/network.uct", "pst_and_topo/raoResult.json");
        assertEquals(1, cracResultsHelper.getPreventivePstRangeActionIds().size());
        assertEquals("PST_cra_3_BBE2AA1  BBE3AA1  1", cracResultsHelper.getPreventivePstRangeActionIds().get(0));
    }

    @Test
    void preventiveHvdcRangeActionsRetrievingTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("hvdc/crac.xml", "hvdc/network.uct", "hvdc/raoResult.json");
        assertEquals(3, cracResultsHelper.getPreventiveHvdcRangeActionIds().size());
    }

    @Test
    void preventiveNetworkActionsRetrievingTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("pst_and_topo/crac.xml", "pst_and_topo/network.uct", "pst_and_topo/raoResult.json");
        assertEquals(1, cracResultsHelper.getPreventiveNetworkActionIds().size());
        assertEquals("ra_1", cracResultsHelper.getPreventiveNetworkActionIds().get(0));
    }

    @Test
    void pstTapPositionRetrievingTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("pst_and_topo/crac.xml", "pst_and_topo/network.uct", "pst_and_topo/raoResult.json");
        assertEquals(16, cracResultsHelper.getTapOfPstRangeActionInPreventive("PST_cra_3_BBE2AA1  BBE3AA1  1"));
    }

    @Test
    void pstHvdcSetpointRetrievingTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("hvdc/crac.xml", "hvdc/network.uct", "hvdc/raoResult.json");
        assertEquals(800, cracResultsHelper.getSetpointOfHvdcRangeActionInPreventive("PRA_HVDC"));
    }

    @Test
    void checkMonitoredBranchesRetrievedCorrectly() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("pst_and_topo/crac.xml", "pst_and_topo/network.uct", "pst_and_topo/raoResult.json");
        String contingencyId = "outage_1";
        List<BranchCnecCreationContext> monitoredBranchesForContingency = cracResultsHelper.getMonitoredBranchesForOutage(contingencyId);
        assertEquals(1, monitoredBranchesForContingency.size());

        FlowCnec branchCnec = cracResultsHelper.getCrac().getFlowCnec("French line 1 - FFR1AA1 ->FFR2AA1   - outage_1 - outage");
        assertEquals(50, cracResultsHelper.getFlowCnecResultInAmpere(branchCnec, cracResultsHelper.getCrac().getPreventiveInstant()).getFlow(), 0.1);
    }

    @Test
    void getPreventiveCnecsTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("pst_and_topo/crac.xml", "pst_and_topo/network.uct", "pst_and_topo/raoResult.json");
        List<CnecPreventive> cnecPreventives = cracResultsHelper.getPreventiveCnecs();
        assertEquals(3, cnecPreventives.size());
        CnecPreventive nlLine = cnecPreventives.stream()
                .filter(cnecPreventive -> cnecPreventive.getCnecCommon().getCode().equals("NNL2AA1  NNL3AA1  1"))
                .findFirst()
                .orElseThrow();
        assertEquals("NL", nlLine.getCnecCommon().getAreaFrom());
        assertEquals("NL", nlLine.getCnecCommon().getAreaTo());
        assertEquals(818.1, nlLine.getI(), .1);
        assertEquals(4000, nlLine.getiMax(), .1);
    }

    @Test
    void geMergedCnecsTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("pst_and_topo/crac.xml", "pst_and_topo/network.uct", "pst_and_topo/raoResult.json");
        Map<String, MergedCnec> mergedCnecs = cracResultsHelper.getMergedCnecs("outage_1");
        assertEquals(1, mergedCnecs.size());

        MergedCnec frFrMergedCnec = mergedCnecs.get("French line 1 - FFR1AA1  - FFR2AA1  - outage_1");
        assertEquals("French line 1", frFrMergedCnec.getCnecCommon().getName());
        assertEquals("FFR1AA1  FFR2AA1  1", frFrMergedCnec.getCnecCommon().getCode());
        assertEquals("FR", frFrMergedCnec.getCnecCommon().getAreaFrom());
        assertEquals("FR", frFrMergedCnec.getCnecCommon().getAreaTo());
        assertEquals(50, frFrMergedCnec.getiAfterOutage(), .1);
        assertEquals(4318, frFrMergedCnec.getiMaxAfterOutage(), .1);
        assertEquals(Double.NaN, frFrMergedCnec.getiAfterCra(), .1);
        assertEquals(3099, frFrMergedCnec.getiMaxAfterCra(), .1);
        assertEquals(0, frFrMergedCnec.getiAfterSps(), .1);
        assertEquals(0, frFrMergedCnec.getiMaxAfterSps(), .1);
    }

    @Test
    void geMergedCnecsXnodeTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("pst_and_topo/crac.xml", "pst_and_topo/network.uct", "pst_and_topo/raoResult.json");
        Map<String, MergedCnec> mergedCnecs = cracResultsHelper.getMergedCnecs("outage_4_xnode");
        assertEquals(1, mergedCnecs.size());

        MergedCnec xnodeMergedCnec = mergedCnecs.get("Xnode - XAA_AA11 - NNL2AA1 - outage_4_xnode");
        assertEquals("Xnode", xnodeMergedCnec.getCnecCommon().getName());
        assertEquals("XAA_AA11 NNL2AA1 1", xnodeMergedCnec.getCnecCommon().getCode());
        assertEquals("FR", xnodeMergedCnec.getCnecCommon().getAreaFrom());
        assertEquals("NL", xnodeMergedCnec.getCnecCommon().getAreaTo());
        assertEquals(Double.NaN, xnodeMergedCnec.getiAfterOutage(), .1);
        assertEquals(2940, xnodeMergedCnec.getiMaxAfterOutage(), .1);
        assertEquals(Double.NaN, xnodeMergedCnec.getiAfterCra(), .1);
        assertEquals(2450, xnodeMergedCnec.getiMaxAfterCra(), .1);
        assertEquals(0, xnodeMergedCnec.getiAfterSps(), .1);
        assertEquals(0, xnodeMergedCnec.getiMaxAfterSps(), .1);
    }

    @Test
    void testNullFlowCnec() {
        final CseCracCreationContext mockedContext = Mockito.mock(CseCracCreationContext.class);
        final Logger mockedBusinessLogger = Mockito.mock(Logger.class);
        final CracResultsHelper helper = new CracResultsHelper(mockedContext, null, null, mockedBusinessLogger);
        final BranchCnecCreationContext branchCnecContext = Mockito.mock(BranchCnecCreationContext.class);
        Mockito.when(branchCnecContext.isImported()).thenReturn(true);
        final String contingencyId = "contingencyId";
        Mockito.when(branchCnecContext.getContingencyId()).thenReturn(Optional.of(contingencyId));
        Mockito.when(branchCnecContext.getCreatedCnecsIds()).thenReturn(Collections.emptyMap());
        Mockito.when(branchCnecContext.getNativeId()).thenReturn("nativeId");
        final List<BranchCnecCreationContext> mockedList = List.of(branchCnecContext);

        whenReturningBranchCnecCreationContexts(mockedContext, mockedList);

        Assertions.assertTrue(helper.getMergedCnecs(contingencyId).isEmpty());
        verify(mockedBusinessLogger, times(1))
                .warn("Couldn't find flowCnec with native id : {}", "nativeId");
    }

    private static void whenReturningBranchCnecCreationContexts(CseCracCreationContext mockedContext,
                                                                List<BranchCnecCreationContext> mockedList) {
        Mockito.when(mockedContext.getBranchCnecCreationContexts())
                .thenAnswer(invocation -> mockedList);
    }

    private CracResultsHelper getCracResultsHelper(String cracXmlFileName, String networkFileName, String raoResultFileName) {
        InputStream cracInputStream = getClass().getResourceAsStream(cracXmlFileName);
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(cracInputStream);

        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));

        CseCracCreator cseCracCreator = new CseCracCreator();
        CseCracCreationContext cseCracCreationContext = cseCracCreator.createCrac(cseCrac, network, null, new CracCreationParameters());

        InputStream raoResultInputStream = getClass().getResourceAsStream(raoResultFileName);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultInputStream, cseCracCreationContext.getCrac());

        return new CracResultsHelper(cseCracCreationContext, raoResult, network, Mockito.mock(Logger.class));
    }
}
