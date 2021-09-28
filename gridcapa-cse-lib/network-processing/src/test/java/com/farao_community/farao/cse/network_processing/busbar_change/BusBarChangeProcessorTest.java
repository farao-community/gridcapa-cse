package com.farao_community.farao.cse.network_processing.busbar_change;

import com.farao_community.farao.cse.network_processing.TestUtils;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.rte_france.farao.data.crac.io.cse.crac_creator.parameters.BusBarChangeSwitches;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusBarChangeProcessorTest {

    private Network network;
    private Set<BusBarChangeSwitches> busBarChangeSwitchesSet;

    private void setUp(String networkFile, String cracFile) {
        network = Importers.loadNetwork(networkFile, getClass().getResourceAsStream("/BusBarChange/" + networkFile), LocalComputationManager.getDefault(), new ImportConfig(), null);
        InputStream is = getClass().getResourceAsStream("/BusBarChange/" + cracFile);
        busBarChangeSwitchesSet = new BusBarChangeProcessor().process(network, is);
    }

    private void compareLists(List<String> expected, List<String> actual) {
        assertEquals(expected.size(), actual.size());
        assertTrue(expected.containsAll(actual));
        assertTrue(actual.containsAll(expected));
    }

    @Test
    void testSimpleCase() throws IOException {
        // All branches are initially connected to initial node of the RA
        setUp("BaseNetwork.uct", "BusBarChange.xml");
        TestUtils.assertNetworksAreEqual(network, "/BusBarChange/ModifiedNetwork.uct");
        assertEquals(1, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches busBarChangeSwitches = busBarChangeSwitchesSet.iterator().next();
        assertEquals("Bus bar ok test", busBarChangeSwitches.getRemedialActionId());
        compareLists(List.of("BBE1AA1Z BBE1AA11 1", "BBE1AA1Y BBE1AA11 1", "BBE1AA1X BBE1AA11 1", "BBE1AA1W BBE1AA11 1"), busBarChangeSwitches.getSwitchesToOpen());
        compareLists(List.of("BBE1AA1Z BBE1AA12 1", "BBE1AA1Y BBE1AA12 1", "BBE1AA1X BBE1AA12 1", "BBE1AA1W BBE1AA12 1"), busBarChangeSwitches.getSwitchesToClose());
    }

    @Test
    void testInvertedCase() throws IOException {
        // All branches are initially connected to final node of the RA
        setUp("BaseNetwork.uct", "BusBarChange_inverted.xml");
        TestUtils.assertNetworksAreEqual(network, "/BusBarChange/ModifiedNetwork.uct");
        assertEquals(1, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches busBarChangeSwitches = busBarChangeSwitchesSet.iterator().next();
        assertEquals("Bus bar ok test", busBarChangeSwitches.getRemedialActionId());
        compareLists(List.of("BBE1AA1Z BBE1AA12 1", "BBE1AA1Y BBE1AA12 1", "BBE1AA1X BBE1AA12 1", "BBE1AA1W BBE1AA12 1"), busBarChangeSwitches.getSwitchesToOpen());
        compareLists(List.of("BBE1AA1Z BBE1AA11 1", "BBE1AA1Y BBE1AA11 1", "BBE1AA1X BBE1AA11 1", "BBE1AA1W BBE1AA11 1"), busBarChangeSwitches.getSwitchesToClose());
    }

    @Test
    void testMixedCase() throws IOException {
        // Some branches are initially connected to initial node, others to final node of the RA
        setUp("MixedNetwork.uct", "BusBarChange.xml");
        TestUtils.assertNetworksAreEqual(network, "/BusBarChange/ModifiedMixedNetwork.uct");
        assertEquals(1, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches busBarChangeSwitches = busBarChangeSwitchesSet.iterator().next();
        assertEquals("Bus bar ok test", busBarChangeSwitches.getRemedialActionId());
        compareLists(List.of("BBE1AA1Z BBE1AA11 1", "BBE1AA1Y BBE1AA11 1", "BBE1AA1X BBE1AA11 1", "BBE1AA1W BBE1AA11 1"), busBarChangeSwitches.getSwitchesToOpen());
        compareLists(List.of("BBE1AA1Z BBE1AA12 1", "BBE1AA1Y BBE1AA12 1", "BBE1AA1X BBE1AA12 1", "BBE1AA1W BBE1AA12 1"), busBarChangeSwitches.getSwitchesToClose());
    }

    @Test
    void testMissingInitialAndFinalNodes() throws IOException {
        // One of the 2 RAs' initial and final nodes do not exist in the network. The RA should be skipped, the other processed normally
        setUp("BaseNetwork.uct", "BusBarChange_missingInitialFinalNodes.xml");
        TestUtils.assertNetworksAreEqual(network, "/BusBarChange/ModifiedNetwork.uct");
        assertEquals(1, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches busBarChangeSwitches = busBarChangeSwitchesSet.iterator().next();
        assertEquals("RA_OK", busBarChangeSwitches.getRemedialActionId());
        compareLists(List.of("BBE1AA1Z BBE1AA11 1", "BBE1AA1Y BBE1AA11 1", "BBE1AA1X BBE1AA11 1", "BBE1AA1W BBE1AA11 1"), busBarChangeSwitches.getSwitchesToOpen());
        compareLists(List.of("BBE1AA1Z BBE1AA12 1", "BBE1AA1Y BBE1AA12 1", "BBE1AA1X BBE1AA12 1", "BBE1AA1W BBE1AA12 1"), busBarChangeSwitches.getSwitchesToClose());
    }

    @Test
    void testMissingBranch() throws IOException {
        // The RA has a branch missing from the network. The RA should be skipped and the network should not be modified.
        setUp("BaseNetwork.uct", "BusBarChange_missingBranch.xml");
        TestUtils.assertNetworksAreEqual(network, "/BusBarChange/BaseNetwork.uct");
        assertEquals(0, busBarChangeSwitchesSet.size());
    }

    @Test
    void testWrongBranch() throws IOException {
        // The RA has a branch of type PST. The RA should be skipped and the network should not be modified.
        setUp("BaseNetwork.uct", "BusBarChange_wrongBranch.xml");
        TestUtils.assertNetworksAreEqual(network, "/BusBarChange/BaseNetwork.uct");
        assertEquals(0, busBarChangeSwitchesSet.size());
    }

    @Test
    void testDoNotRecreateSwitches() throws IOException {
        // In this case, the 2 RAs have same initial & final node (but inverted), and some branches in common.
        // Processor should not recreate switches multiple times for common branches.
        setUp("BaseNetwork.uct", "BusBarChange_redundance.xml");
        TestUtils.assertNetworksAreEqual(network, "/BusBarChange/ModifiedNetwork_redundance.uct");
        assertEquals(2, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches bbcs = busBarChangeSwitchesSet.stream().filter(busBarChangeSwitches -> busBarChangeSwitches.getRemedialActionId().equals("RA_1")).findAny().orElseThrow();
        compareLists(List.of("BBE1AA1Z BBE1AA11 1", "BBE1AA1Y BBE1AA11 1", "BBE1AA1W BBE1AA11 1"), bbcs.getSwitchesToOpen());
        compareLists(List.of("BBE1AA1Z BBE1AA12 1", "BBE1AA1Y BBE1AA12 1", "BBE1AA1W BBE1AA12 1"), bbcs.getSwitchesToClose());
        bbcs = busBarChangeSwitchesSet.stream().filter(busBarChangeSwitches -> busBarChangeSwitches.getRemedialActionId().equals("RA_2")).findAny().orElseThrow();
        compareLists(List.of("BBE1AA1Y BBE1AA12 1", "BBE1AA1W BBE1AA12 1", "BBE1AA1X BBE1AA12 1"), bbcs.getSwitchesToOpen());
        compareLists(List.of("BBE1AA1Y BBE1AA11 1", "BBE1AA1W BBE1AA11 1", "BBE1AA1X BBE1AA11 1"), bbcs.getSwitchesToClose());
    }
}
