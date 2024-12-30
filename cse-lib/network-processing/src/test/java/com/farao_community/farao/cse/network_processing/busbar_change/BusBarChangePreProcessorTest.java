/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing.busbar_change;

import com.farao_community.farao.cse.network_processing.TestUtils;
import com.powsybl.openrao.data.crac.io.cse.parameters.BusBarChangeSwitches;
import com.powsybl.openrao.data.crac.io.cse.parameters.SwitchPairId;
import com.powsybl.iidm.network.*;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
class BusBarChangePreProcessorTest {

    private Network network;
    private Set<BusBarChangeSwitches> busBarChangeSwitchesSet;

    private void setUp(String networkFile, String cracFile) {
        network = Network.read(networkFile, getClass().getResourceAsStream(networkFile));
        InputStream is = getClass().getResourceAsStream(cracFile);
        busBarChangeSwitchesSet = BusBarChangePreProcessor.process(network, is);
    }

    @Test
    void testSimpleCase() {
        // All branches are initially connected to initial node of the RA
        setUp("BaseNetwork.uct", "BusBarChange.xml");
        TestUtils.assertNetworksAreEqual(network, "ModifiedNetwork.uct", getClass());
        assertEquals(1, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches busBarChangeSwitches = busBarChangeSwitchesSet.iterator().next();
        assertEquals("Bus bar ok test", busBarChangeSwitches.getRemedialActionId());
        Set<SwitchPairId> switchPairs = Set.of(
            new SwitchPairId("BBE1AA1Z BBE1AA11 1", "BBE1AA1Z BBE1AA12 1"),
            new SwitchPairId("BBE1AA1Y BBE1AA11 1", "BBE1AA1Y BBE1AA12 1"),
            new SwitchPairId("BBE1AA1X BBE1AA11 1", "BBE1AA1X BBE1AA12 1"),
            new SwitchPairId("BBE1AA1W BBE1AA11 1", "BBE1AA1W BBE1AA12 1")
        );
        assertEquals(switchPairs, busBarChangeSwitches.getSwitchPairs());
    }

    @Test
    void testInvertedCase() {
        // All branches are initially connected to final node of the RA
        setUp("BaseNetwork.uct", "BusBarChange_inverted.xml");
        TestUtils.assertNetworksAreEqual(network, "ModifiedNetwork.uct", getClass());
        assertEquals(1, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches busBarChangeSwitches = busBarChangeSwitchesSet.iterator().next();
        assertEquals("Bus bar ok test", busBarChangeSwitches.getRemedialActionId());
        Set<SwitchPairId> switchPairs = Set.of(
            new SwitchPairId("BBE1AA1Z BBE1AA12 1", "BBE1AA1Z BBE1AA11 1"),
            new SwitchPairId("BBE1AA1Y BBE1AA12 1", "BBE1AA1Y BBE1AA11 1"),
            new SwitchPairId("BBE1AA1X BBE1AA12 1", "BBE1AA1X BBE1AA11 1"),
            new SwitchPairId("BBE1AA1W BBE1AA12 1", "BBE1AA1W BBE1AA11 1")
        );
        assertEquals(switchPairs, busBarChangeSwitches.getSwitchPairs());
    }

    @Test
    void testMixedCase() {
        // Some branches are initially connected to initial node, others to final node of the RA
        setUp("MixedNetwork.uct", "BusBarChange.xml");
        TestUtils.assertNetworksAreEqual(network, "ModifiedMixedNetwork.uct", getClass());
        assertEquals(1, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches busBarChangeSwitches = busBarChangeSwitchesSet.iterator().next();
        assertEquals("Bus bar ok test", busBarChangeSwitches.getRemedialActionId());
        Set<SwitchPairId> switchPairs = Set.of(
            new SwitchPairId("BBE1AA1Z BBE1AA11 1", "BBE1AA1Z BBE1AA12 1"),
            new SwitchPairId("BBE1AA1Y BBE1AA11 1", "BBE1AA1Y BBE1AA12 1"),
            new SwitchPairId("BBE1AA1X BBE1AA11 1", "BBE1AA1X BBE1AA12 1"),
            new SwitchPairId("BBE1AA1W BBE1AA11 1", "BBE1AA1W BBE1AA12 1")
        );
        assertEquals(switchPairs, busBarChangeSwitches.getSwitchPairs());
    }

    @Test
    void testMissingInitialAndFinalNodes() {
        // One of the 2 RAs' initial and final nodes do not exist in the network. The RA should be skipped, the other processed normally
        setUp("BaseNetwork.uct", "BusBarChange_missingInitialFinalNodes.xml");
        TestUtils.assertNetworksAreEqual(network, "ModifiedNetwork.uct", getClass());
        assertEquals(1, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches busBarChangeSwitches = busBarChangeSwitchesSet.iterator().next();
        assertEquals("RA_OK", busBarChangeSwitches.getRemedialActionId());
        Set<SwitchPairId> switchPairs = Set.of(
            new SwitchPairId("BBE1AA1Z BBE1AA11 1", "BBE1AA1Z BBE1AA12 1"),
            new SwitchPairId("BBE1AA1Y BBE1AA11 1", "BBE1AA1Y BBE1AA12 1"),
            new SwitchPairId("BBE1AA1X BBE1AA11 1", "BBE1AA1X BBE1AA12 1"),
            new SwitchPairId("BBE1AA1W BBE1AA11 1", "BBE1AA1W BBE1AA12 1")
        );
        assertEquals(switchPairs, busBarChangeSwitches.getSwitchPairs());
    }

    @Test
    void testWithTieLine() {
        // Create a switch on a dangling line
        setUp("BaseNetwork_tieline.uct", "BusBarChange_tieline.xml");
        TestUtils.assertNetworksAreEqual(network, "ModifiedNetwork_tieline.uct", getClass());
        assertEquals(1, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches busBarChangeSwitches = busBarChangeSwitchesSet.iterator().next();
        assertEquals("RA_OK", busBarChangeSwitches.getRemedialActionId());
        assertEquals(Set.of(new SwitchPairId("DDE3AA1Z DDE3AA1  1", "DDE3AA1Z DDE3AA12 1")), busBarChangeSwitches.getSwitchPairs());
        // Check that the tie line still exists
        TieLine tieLine = network.getTieLine("DDE3AA1Z X_DEFR1  1 + FFR2AA1  X_DEFR1  1");
        assertNotNull(tieLine);
        assertTrue(tieLine instanceof TieLine);
    }

    @Test
    void testWithPst() {
        // Create a switch on a PST
        setUp("BaseNetwork.uct", "BusBarChange_pst.xml");
        TestUtils.assertNetworksAreEqual(network, "ModifiedNetwork_pst.uct", getClass());
        assertEquals(1, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches busBarChangeSwitches = busBarChangeSwitchesSet.iterator().next();
        assertEquals("RA_OK", busBarChangeSwitches.getRemedialActionId());
        assertEquals(Set.of(new SwitchPairId("BBE2AA1Z BBE2AA1  1", "BBE2AA1Z BBE2AA12 1")), busBarChangeSwitches.getSwitchPairs());
        // check that the PST still exists
        TwoWindingsTransformer pst = network.getTwoWindingsTransformer("BBE2AA1Z BBE3AA1  1");
        assertNotNull(pst);
    }

    @Test
    void testMissingBranch() {
        // The RA has a branch missing from the network. The RA should be skipped and the network should not be modified.
        setUp("BaseNetwork.uct", "BusBarChange_missingBranch.xml");
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork.uct", getClass());
        assertEquals(0, busBarChangeSwitchesSet.size());
    }

    @Test
    void testWrongBranchNotConnected() {
        // The RA has a branch which is not connected to the initial nor to the final node
        setUp("BaseNetwork.uct", "BusBarChange_wrongBranch2.xml");
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork.uct", getClass());
        assertEquals(0, busBarChangeSwitchesSet.size());
    }

    @Test
    void testDoNotRecreateSwitches() {
        // In this case, the 2 RAs have same initial & final node (but inverted), and some branches in common.
        // Processor should not recreate switches multiple times for common branches.
        setUp("BaseNetwork.uct", "BusBarChange_redundance.xml");
        TestUtils.assertNetworksAreEqual(network, "ModifiedNetwork_redundance.uct", getClass());
        assertEquals(2, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches bbcs = busBarChangeSwitchesSet.stream().filter(busBarChangeSwitches -> busBarChangeSwitches.getRemedialActionId().equals("RA_1")).findAny().orElseThrow();
        Set<SwitchPairId> switchPairs = Set.of(
            new SwitchPairId("BBE1AA1Z BBE1AA11 1", "BBE1AA1Z BBE1AA12 1"),
            new SwitchPairId("BBE1AA1Y BBE1AA11 1", "BBE1AA1Y BBE1AA12 1"),
            new SwitchPairId("BBE1AA1W BBE1AA11 1", "BBE1AA1W BBE1AA12 1")
        );
        assertEquals(switchPairs, bbcs.getSwitchPairs());
        bbcs = busBarChangeSwitchesSet.stream().filter(busBarChangeSwitches -> busBarChangeSwitches.getRemedialActionId().equals("RA_2")).findAny().orElseThrow();
        switchPairs = Set.of(
            new SwitchPairId("BBE1AA1Y BBE1AA12 1", "BBE1AA1Y BBE1AA11 1"),
            new SwitchPairId("BBE1AA1W BBE1AA12 1", "BBE1AA1W BBE1AA11 1"),
            new SwitchPairId("BBE1AA1X BBE1AA12 1", "BBE1AA1X BBE1AA11 1")
        );
        assertEquals(switchPairs, bbcs.getSwitchPairs());
    }

    @Test
    void test3NodeCase() {
        setUp("BaseNetwork.uct", "BusBarChange_3nodes.xml");
        TestUtils.assertNetworksAreEqual(network, "ModifiedNetwork_3nodes.uct", getClass());
        assertEquals(2, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches bbcs = busBarChangeSwitchesSet.stream().filter(busBarChangeSwitches -> busBarChangeSwitches.getRemedialActionId().equals("RA_1")).findAny().orElseThrow();
        Set<SwitchPairId> switchPairs = Set.of(
            new SwitchPairId("BBE1AA1Z BBE1AA11 1", "BBE1AA1Z BBE1AA12 1"),
            new SwitchPairId("BBE1AA1X BBE1AA11 1", "BBE1AA1X BBE1AA12 1")
        );
        assertEquals(switchPairs, bbcs.getSwitchPairs());
        bbcs = busBarChangeSwitchesSet.stream().filter(busBarChangeSwitches -> busBarChangeSwitches.getRemedialActionId().equals("RA_2")).findAny().orElseThrow();
        switchPairs = Set.of(
            new SwitchPairId("BBE1AA1Y BBE1AA13 1", "BBE1AA1Y BBE1AA11 1"),
            new SwitchPairId("BBE1AA1X BBE1AA13 1", "BBE1AA1X BBE1AA11 1"),
            new SwitchPairId("BBE1AA1Z BBE1AA13 1", "BBE1AA1Z BBE1AA11 1")
        );
        assertEquals(switchPairs, bbcs.getSwitchPairs());
    }

    @Test
    void testTwoRasOnTwoEndsOfSameLine() {
        setUp("BaseNetwork_twoBusBarsOnSameLine.uct", "BusBarChange_twoBusBarsOnSameLine.xml");
        TestUtils.assertNetworksAreEqual(network, "ModifiedNetwork_twoBusBarsOnSameLine.uct", getClass());
        assertEquals(3, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches bbcs = busBarChangeSwitchesSet.stream().filter(busBarChangeSwitches -> busBarChangeSwitches.getRemedialActionId().equals("RA_1")).findAny().orElseThrow();
        Set<SwitchPairId> switchPairs = Set.of(
            new SwitchPairId("BBE1AA1Z BBE1AA11 1", "BBE1AA1Z BBE1AA12 1")
        );
        assertEquals(switchPairs, bbcs.getSwitchPairs());
        bbcs = busBarChangeSwitchesSet.stream().filter(busBarChangeSwitches -> busBarChangeSwitches.getRemedialActionId().equals("RA_2")).findAny().orElseThrow();
        switchPairs = Set.of(
            new SwitchPairId("BBE2AA1Z BBE2AA11 1", "BBE2AA1Z BBE2AA12 1")
        );
        assertEquals(switchPairs, bbcs.getSwitchPairs());
        bbcs = busBarChangeSwitchesSet.stream().filter(busBarChangeSwitches -> busBarChangeSwitches.getRemedialActionId().equals("RA_1_inverted")).findAny().orElseThrow();
        switchPairs = Set.of(
            new SwitchPairId("BBE1AA1Z BBE1AA12 1", "BBE1AA1Z BBE1AA11 1")
        );
        assertEquals(switchPairs, bbcs.getSwitchPairs());

        assertTrue(network.getIdentifiable("BBE1AA1Z BBE1AA11 1") instanceof Switch);
        assertFalse(((Switch) network.getIdentifiable("BBE1AA1Z BBE1AA11 1")).isOpen());

        assertTrue(network.getIdentifiable("BBE1AA1Z BBE1AA12 1") instanceof Switch);
        assertTrue(((Switch) network.getIdentifiable("BBE1AA1Z BBE1AA12 1")).isOpen());

        assertTrue(network.getIdentifiable("BBE2AA1Z BBE2AA11 1") instanceof Switch);
        assertFalse(((Switch) network.getIdentifiable("BBE2AA1Z BBE2AA11 1")).isOpen());

        assertTrue(network.getIdentifiable("BBE2AA1Z BBE2AA12 1") instanceof Switch);
        assertTrue(((Switch) network.getIdentifiable("BBE2AA1Z BBE2AA12 1")).isOpen());

        assertNull(network.getIdentifiable("BBE1AA11 BBE2AA11 1"));
        assertNull(network.getIdentifiable("BBE1AA11 BBE2AA1Z 1"));
        assertNull(network.getIdentifiable("BBE1AA1Z BBE2AA11 1"));
        assertTrue(network.getIdentifiable("BBE1AA1Z BBE2AA1Z 1") instanceof Line);
    }

    @Test
    void testTwoRasOnTwoEndsOfSameLine2() {
        setUp("BaseNetwork_2RA_on_1_line.uct", "BusBarChange_2RA_on_1_line.xml");
        TestUtils.assertNetworksAreEqual(network, "ModifiedNetwork_2RA_on_1_line.uct", getClass());
        assertEquals(2, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches bbcs = busBarChangeSwitchesSet.stream().filter(busBarChangeSwitches -> busBarChangeSwitches.getRemedialActionId().equals("Bus bar ok test - 1")).findAny().orElseThrow();
        Set<SwitchPairId> switchPairs = Set.of(
            new SwitchPairId("BBE1AA1Z BBE1AA11 1", "BBE1AA1Z BBE1AA12 1"),
            new SwitchPairId("BBE1AA1Y BBE1AA11 1", "BBE1AA1Y BBE1AA12 1"),
            new SwitchPairId("BBE1AA1X BBE1AA11 1", "BBE1AA1X BBE1AA12 1"),
            new SwitchPairId("BBE1AA1W BBE1AA11 1", "BBE1AA1W BBE1AA12 1")
        );
        assertEquals(switchPairs, bbcs.getSwitchPairs());
        bbcs = busBarChangeSwitchesSet.stream().filter(busBarChangeSwitches -> busBarChangeSwitches.getRemedialActionId().equals("Bus bar ok test - 2")).findAny().orElseThrow();
        switchPairs = Set.of(
            new SwitchPairId("BBE2AA1Y BBE2AA11 1", "BBE2AA1Y BBE2AA12 1"),
            new SwitchPairId("BBE2AA1Z BBE2AA11 1", "BBE2AA1Z BBE2AA12 1")
        );
        assertEquals(switchPairs, bbcs.getSwitchPairs());
    }
}
