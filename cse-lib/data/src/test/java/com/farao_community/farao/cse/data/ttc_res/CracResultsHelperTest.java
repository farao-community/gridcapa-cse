/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ttc_res;

import com.farao_community.farao.cse.data.cnec.CnecPreventive;
import com.farao_community.farao.cse.data.cnec.CracResultsHelper;
import com.farao_community.farao.cse.data.cnec.MergedCnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class CracResultsHelperTest {

    @Test
    void preventivePstRangeActionsRetrievingTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("crac.json", "raoResult.json");
        assertEquals(1, cracResultsHelper.getPreventivePstRangeActionIds().size());
        assertEquals("PRA_PST_BE", cracResultsHelper.getPreventivePstRangeActionIds().get(0));
    }

    @Test
    void preventiveHvdcRangeActionsRetrievingTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("crac_with_HVDC.json", "raoResult_with_HVDC.json");
        assertEquals(1, cracResultsHelper.getPreventiveHvdcRangeActionIds().size());
        assertEquals("PRA_HVDC_GILE_PIOSSASCO_2", cracResultsHelper.getPreventiveHvdcRangeActionIds().get(0));
    }

    @Test
    void preventiveNetworkActionsRetrievingTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("crac.json", "raoResult.json");
        assertEquals(1, cracResultsHelper.getPreventiveNetworkActionIds().size());
        assertEquals("Open line NL1-NL2", cracResultsHelper.getPreventiveNetworkActionIds().get(0));
    }

    @Test
    void pstTapPositionRetrievingTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("crac.json", "raoResult.json");
        assertEquals(-16, cracResultsHelper.getTapOfPstRangeActionInPreventive("PRA_PST_BE"));
    }

    @Test
    void pstHvdcSetpointRetrievingTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("crac_with_HVDC.json", "raoResult_with_HVDC.json");
        assertEquals(600, cracResultsHelper.getSetpointOfHvdcRangeActionInPreventive("PRA_HVDC_GILE_PIOSSASCO_2"));
    }

    @Test
    void checkMonitoredBranchesRetrievedCorrectly() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("cracWithMonitoredBranches.json", "raoResultMonitoredBranches.json");
        String contingencyId = "Contingency FR1 FR3";
        List<FlowCnec> monitoredBranchesForContingency = cracResultsHelper.getMonitoredBranchesForOutage(contingencyId);
        assertEquals(5, monitoredBranchesForContingency.size());

        FlowCnec branchCnec = cracResultsHelper.getCrac().getFlowCnec("FFR2AA1  DDE3AA1  1 - outage - Contingency FR1 FR3");
        assertEquals(1858, cracResultsHelper.getFlowCnecResultInAmpere(branchCnec, OptimizationState.AFTER_PRA).getFlow(), 0.1);
    }

    @Test
    void getPreventiveCnecsTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("cracWithMonitoredBranches.json", "raoResultMonitoredBranches.json");
        List<CnecPreventive> cnecPreventives = cracResultsHelper.getPreventiveCnecs();
        assertEquals(2, cnecPreventives.size());
        CnecPreventive germanLine = cnecPreventives.stream()
            .filter(cnecPreventive -> cnecPreventive.getCnecCommon().getName().equals("Line DE1 DE2"))
            .findFirst()
            .orElseThrow();
        assertEquals("Line DE1 DE2", germanLine.getCnecCommon().getName());
        assertEquals("DDE1AA1  DDE2AA1  1", germanLine.getCnecCommon().getCode());
        assertEquals("DE", germanLine.getCnecCommon().getAreaFrom());
        assertEquals("DE", germanLine.getCnecCommon().getAreaTo());
        assertEquals(564, germanLine.getI());
        assertEquals(1500, germanLine.getiMax());
    }

    @Test
    void geMergedCnecsTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("cracWithMonitoredBranches.json", "raoResultMonitoredBranches.json");
        Map<String, MergedCnec> mergedCnecs = cracResultsHelper.getMergedCnecs("Contingency FR1 FR3");
        assertEquals(2, mergedCnecs.size());

        MergedCnec deDeMergedCnec = mergedCnecs.get("Line DE1 DE2");
        assertEquals("Line DE1 DE2", deDeMergedCnec.getCnecCommon().getName());
        assertEquals("DDE1AA1  DDE2AA1  1", deDeMergedCnec.getCnecCommon().getCode());
        assertEquals("DE", deDeMergedCnec.getCnecCommon().getAreaFrom());
        assertEquals("DE", deDeMergedCnec.getCnecCommon().getAreaTo());
        assertEquals(583, deDeMergedCnec.getiAfterOutage());
        assertEquals(1500, deDeMergedCnec.getiMaxAfterOutage());
        assertEquals(583, deDeMergedCnec.getiAfterCra());
        assertEquals(1500, deDeMergedCnec.getiMaxAfterCra());
        assertEquals(0, deDeMergedCnec.getiAfterSps());
        assertEquals(0, deDeMergedCnec.getiMaxAfterSps());

        MergedCnec frDeMergedCnec = mergedCnecs.get("Tie-line FR DE");
        assertEquals("Tie-line FR DE", frDeMergedCnec.getCnecCommon().getName());
        assertEquals("FFR2AA1  DDE3AA1  1", frDeMergedCnec.getCnecCommon().getCode());
        assertEquals("FR", frDeMergedCnec.getCnecCommon().getAreaFrom());
        assertEquals("DE", frDeMergedCnec.getCnecCommon().getAreaTo());
        assertEquals(1858, frDeMergedCnec.getiAfterOutage());
        assertEquals(1500, frDeMergedCnec.getiMaxAfterOutage());
        assertEquals(1858, frDeMergedCnec.getiAfterCra());
        assertEquals(1500, frDeMergedCnec.getiMaxAfterCra());
        assertEquals(1000, frDeMergedCnec.getiAfterSps());
        assertEquals(1500, frDeMergedCnec.getiMaxAfterSps());
    }

    @Test
    void mostLimitingElementTest() {
        CracResultsHelper cracResultsHelper = getCracResultsHelper("cracWithMonitoredBranches.json", "raoResultMonitoredBranches.json");
        FlowCnec worstCnec = cracResultsHelper.getWorstCnec();
        assertEquals("Tie-line FR DE", worstCnec.getName());
    }

    private CracResultsHelper getCracResultsHelper(String cracPath, String raoResultPath) {
        InputStream cracInputStream = getClass().getResourceAsStream(cracPath);
        InputStream raoResultInputStream = getClass().getResourceAsStream(raoResultPath);
        Crac crac = CracImporters.importCrac("crac.json", cracInputStream);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultInputStream, crac);
        return new CracResultsHelper(crac, raoResult, Mockito.mock(List.class));
    }
}
