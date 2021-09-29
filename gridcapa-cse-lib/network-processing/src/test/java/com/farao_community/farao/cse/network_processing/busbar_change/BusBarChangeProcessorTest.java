package com.farao_community.farao.cse.network_processing.busbar_change;

import com.farao_community.farao.cse.network_processing.TestUtils;
import com.farao_community.farao.data.crac_creator_api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creator_api.parameters.JsonCracCreationParameters;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.rte_france.farao.data.crac.io.cse.crac_creator.parameters.BusBarChangeSwitches;
import com.rte_france.farao.data.crac.io.cse.crac_creator.parameters.CseCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BusBarChangeProcessorTest {

    private Network network;
    private Network network2;
    private Set<BusBarChangeSwitches> busBarChangeSwitchesSet;

    private void setUp(String networkFile, String cracFile) {
        network = Importers.loadNetwork(networkFile, getClass().getResourceAsStream("/BusBarChange/" + networkFile), LocalComputationManager.getDefault(), new ImportConfig(), null);
        network2 = Importers.loadNetwork(networkFile, getClass().getResourceAsStream("/BusBarChange/" + networkFile), LocalComputationManager.getDefault(), new ImportConfig(), null);
        InputStream is = getClass().getResourceAsStream("/BusBarChange/" + cracFile);
        busBarChangeSwitchesSet = new BusBarChangeProcessor().process(network, is);
    }

    private void compareLists(List<String> expected, List<String> actual) {
        assertEquals(expected.size(), actual.size());
        assertTrue(expected.containsAll(actual));
        assertTrue(actual.containsAll(expected));
    }

    @Test
    void testSimpleCase() {
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
    void testInvertedCase() {
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
    void testMixedCase() {
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
    void testMissingInitialAndFinalNodes() {
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
    void testWithTieLine() {
        // Create a switch on a dangling line
        setUp("BaseNetwork_tieline.uct", "BusBarChange_tieline.xml");
        Exporters.export("UCTE", network, new Properties(), "D:\\Users\\mitripet\\Desktop", "network");
        TestUtils.assertNetworksAreEqual(network, "/BusBarChange/ModifiedNetwork_tieline.uct");
        assertEquals(1, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches busBarChangeSwitches = busBarChangeSwitchesSet.iterator().next();
        assertEquals("RA_OK", busBarChangeSwitches.getRemedialActionId());
        compareLists(List.of("DDE3AA1Z DDE3AA1  1"), busBarChangeSwitches.getSwitchesToOpen());
        compareLists(List.of("DDE3AA1Z DDE3AA12 1"), busBarChangeSwitches.getSwitchesToClose());
        // Check that the tie line still exists
        Line tieLine = network.getLine("DDE3AA1Z X_DEFR1  1 + FFR2AA1  X_DEFR1  1");
        assertNotNull(tieLine);
        assertTrue(tieLine instanceof TieLine);
    }
    
    @Test
    void testWithPst() {
        // Create a switch on a PST
        setUp("BaseNetwork.uct", "BusBarChange_pst.xml");
        TestUtils.assertNetworksAreEqual(network, "/BusBarChange/ModifiedNetwork_pst.uct");
        assertEquals(1, busBarChangeSwitchesSet.size());
        BusBarChangeSwitches busBarChangeSwitches = busBarChangeSwitchesSet.iterator().next();
        assertEquals("RA_OK", busBarChangeSwitches.getRemedialActionId());
        compareLists(List.of("BBE2AA1Z BBE2AA1  1"), busBarChangeSwitches.getSwitchesToOpen());
        compareLists(List.of("BBE2AA1Z BBE2AA12 1"), busBarChangeSwitches.getSwitchesToClose());
        // check that the PST still exists
        TwoWindingsTransformer pst = network.getTwoWindingsTransformer("BBE2AA1Z BBE3AA1  1");
        assertNotNull(pst);
    }

    @Test
    void testMissingBranch() {
        // The RA has a branch missing from the network. The RA should be skipped and the network should not be modified.
        setUp("BaseNetwork.uct", "BusBarChange_missingBranch.xml");
        TestUtils.assertNetworksAreEqual(network, "/BusBarChange/BaseNetwork.uct");
        assertEquals(0, busBarChangeSwitchesSet.size());
    }

    @Test
    void testWrongBranchNotConnected() {
        // The RA has a branch which is not connected to the initial nor to the final node
        setUp("BaseNetwork.uct", "BusBarChange_wrongBranch2.xml");
        TestUtils.assertNetworksAreEqual(network, "/BusBarChange/BaseNetwork.uct");
        assertEquals(0, busBarChangeSwitchesSet.size());
    }

    @Test
    void testDoNotRecreateSwitches() {
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

    @Test
    void testCse() {
        setUp("20210901_2230_213_UX0.uct", "20210901_2230_423_CRAC_IT2.xml");
        Exporters.export("XIIDM", network, new Properties(), "D:\\Users\\mitripet\\Desktop", "network");
        Exporters.export("UCTE", network, new Properties(), "D:\\Users\\mitripet\\Desktop", "network");
        CracCreationParameters parameters = new CracCreationParameters();
        CseCracCreationParameters cseParameters = new CseCracCreationParameters();
        cseParameters.setBusBarChangeSwitchesSet(busBarChangeSwitchesSet);
        parameters.addExtension(CseCracCreationParameters.class, cseParameters);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(parameters, os);
        int c = 0;
    }
}
