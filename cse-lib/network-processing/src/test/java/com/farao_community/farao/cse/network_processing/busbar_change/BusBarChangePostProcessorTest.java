/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing.busbar_change;

import com.farao_community.farao.cse.network_processing.TestUtils;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.BusBarChangeSwitches;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.SwitchPairId;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
class BusBarChangePostProcessorTest {
    private Network network;
    private Set<BusBarChangeSwitches> busBarChangeSwitchesSet;

    private void setUpPreProcessing(String networkFile, String cracFile) {
        network = Network.read(networkFile, getClass().getResourceAsStream(networkFile));
        InputStream is = getClass().getResourceAsStream(cracFile);
        busBarChangeSwitchesSet = BusBarChangePreProcessor.process(network, is);
    }

    private void setUpRoundTrip(String networkFile, String cracFile) {
        network = Network.read(networkFile, getClass().getResourceAsStream(networkFile));
        InputStream is = getClass().getResourceAsStream(cracFile);
        busBarChangeSwitchesSet = BusBarChangePreProcessor.process(network, is);
        BusBarChangePostProcessor.process(network, busBarChangeSwitchesSet);
    }

    @Test
    void testRoundTripSimpleCase() {
        setUpRoundTrip("BaseNetwork.uct", "BusBarChange.xml");
        // The preprocessor also creates bus BBE1AA12 which is referenced by the CRAC but initially missing from BaseNetwork.uct
        // This bus will not be removed in the postprocessor
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork_withInitiallyMissingBus.uct", getClass());
    }

    private void applySwitchPairRa(SwitchPairId switchPairId, Network network) {
        network.getSwitch(switchPairId.getSwitchToOpenId()).setOpen(true);
        network.getSwitch(switchPairId.getSwitchToCloseId()).setOpen(false);
    }

    @Test
    void testRoundTripSimpleCaseAfterApplyingSwitchPair() {
        setUpPreProcessing("BaseNetwork.uct", "BusBarChange.xml");
        // apply RA = switch switches' states
        // Lines should be between * and BBE1AA12, instead of BBE1AA11
        Set.of(
            new SwitchPairId("BBE1AA1Z BBE1AA11 1", "BBE1AA1Z BBE1AA12 1"),
            new SwitchPairId("BBE1AA1Y BBE1AA11 1", "BBE1AA1Y BBE1AA12 1"),
            new SwitchPairId("BBE1AA1X BBE1AA11 1", "BBE1AA1X BBE1AA12 1"),
            new SwitchPairId("BBE1AA1W BBE1AA11 1", "BBE1AA1W BBE1AA12 1")
        ).forEach(switchPairId -> applySwitchPairRa(switchPairId, network));
        BusBarChangePostProcessor.process(network, busBarChangeSwitchesSet);
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork_withInitiallyMissingBus_afterRa.uct", getClass());
    }

    @Test
    void testRoundTripInvertedCase() {
        setUpRoundTrip("BaseNetwork.uct", "BusBarChange_inverted.xml");
        // The preprocessor also creates bus BBE1AA12 which is referenced by the CRAC but initially missing from BaseNetwork.uct
        // This bus will not be removed in the postprocessor
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork_withInitiallyMissingBus.uct", getClass());
    }

    @Test
    void testRoundTripMixedCase() {
        setUpRoundTrip("MixedNetwork.uct", "BusBarChange.xml");
        TestUtils.assertNetworksAreEqual(network, "MixedNetwork.uct", getClass());
    }

    @Test
    void testRoundTripMissingInitialAndFinalNodes() {
        setUpRoundTrip("BaseNetwork.uct", "BusBarChange_missingInitialFinalNodes.xml");
        // The preprocessor also creates bus BBE1AA12 which is referenced by the CRAC but initially missing from BaseNetwork.uct
        // This bus will not be removed in the postprocessor
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork_withInitiallyMissingBus.uct", getClass());
    }

    @Test
    void testRoundTripWithTieLine() {
        setUpRoundTrip("BaseNetwork_tieline.uct", "BusBarChange_tieline.xml");
        // The preprocessor also creates bus DDE3AA12 which is referenced by the CRAC but initially missing from BaseNetwork_tieline.uct
        // This bus will not be removed in the postprocessor
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork_tieline_withInitiallyMissingBus.uct", getClass());
    }

    @Test
    void testRoundTripWithPst() {
        setUpRoundTrip("BaseNetwork.uct", "BusBarChange_pst.xml");
        // The preprocessor also creates bus BBE2AA12 which is referenced by the CRAC but initially missing from BaseNetwork.uct
        // This bus will not be removed in the postprocessor
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork_withInitiallyMissingBus_pst.uct", getClass());
    }

    @Test
    void testRoundTripMissingBranch() {
        setUpRoundTrip("BaseNetwork.uct", "BusBarChange_missingBranch.xml");
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork.uct", getClass());
    }

    @Test
    void testRoundTripWrongBranchNotConnected() {
        setUpRoundTrip("BaseNetwork.uct", "BusBarChange_wrongBranch2.xml");
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork.uct", getClass());
    }

    @Test
    void testRoundTripDoNotRecreateSwitches() {
        setUpRoundTrip("BaseNetwork.uct", "BusBarChange_redundance.xml");
        // The preprocessor also creates bus BBE1AA12 which is referenced by the CRAC but initially missing from BaseNetwork.uct
        // This bus will not be removed in the postprocessor
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork_withInitiallyMissingBus.uct", getClass());
    }

    @Test
    void testRoundTrip3NodeCase() {
        setUpRoundTrip("BaseNetwork.uct", "BusBarChange_3nodes.xml");
        // The preprocessor also creates buses BBE1AA12 & BBE1AA13 which are referenced by the CRAC but initially missing from BaseNetwork.uct
        // These buses will not be removed in the postprocessor
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork_with2InitiallyMissingBuses.uct", getClass());
    }

    @Test
    void testRoundTripTwoRasOnTwoEndsOfSameLine() {
        setUpRoundTrip("BaseNetwork_twoBusBarsOnSameLine.uct", "BusBarChange_twoBusBarsOnSameLine.xml");
        // The preprocessor also creates buses BBE1AA12 & BBE2AA12 which are referenced by the CRAC but initially missing from BaseNetwork.uct
        // These buses will not be removed in the postprocessor
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork_twoBusBarsOnSameLine_with2InitiallyMissingBuses.uct", getClass());
    }

    @Test
    void testRoundTripTwoRasOnTwoEndsOfSameLine2() {
        setUpRoundTrip("BaseNetwork_2RA_on_1_line.uct", "BusBarChange_2RA_on_1_line.xml");
        // The preprocessor also creates buses BBE1AA12 & BBE2AA12 which are referenced by the CRAC but initially missing from BaseNetwork.uct
        // These buses will not be removed in the postprocessor
        TestUtils.assertNetworksAreEqual(network, "BaseNetwork_2RA_on_1_line_with2InitiallyMissingBuses.uct", getClass());
    }
}
