/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing.busbar_change;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.ucte.converter.util.UcteConverterConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
class NetworkModifierTest {
    private static final Double DOUBLE_TOLERANCE = 1e-6;

    private Network network;
    private NetworkModifier networkModifier;

    private void setUpMock() {
        network = Mockito.mock(Network.class);
        networkModifier = new NetworkModifier(network);
    }

    private void setUp(String networkFile) {
        network = Network.read(networkFile, getClass().getResourceAsStream(networkFile));
        networkModifier = new NetworkModifier(network);
    }

    @Test
    void testBasicMethods() {
        setUp("BaseNetwork.uct");
        assertSame(network, networkModifier.getNetwork());
    }

    @Test
    void testCreateBus() {
        setUp("BaseNetwork.uct");
        networkModifier.createBus("BBE2AA1G", "BBE2AA1");
        networkModifier.commitAllChanges();
        Identifiable<?> createdBus = network.getIdentifiable("BBE2AA1G");
        assertNotNull(createdBus);
        assertTrue(createdBus instanceof Bus);
        assertEquals("BBE2AA1", ((Bus) createdBus).getVoltageLevel().getId());
    }

    @Test
    void testCreateBusException() {
        setUpMock();
        when(network.getVoltageLevel(any())).thenThrow(new PowsyblException("mock exception"));
        assertThrows(OpenRaoException.class, () -> networkModifier.createBus("BBE2AA12", "BBE2AA1"));
    }

    @Test
    void testCreateOpenSwitch() {
        setUp("ModifiedNetwork.uct");
        networkModifier.createSwitch(network.getVoltageLevel("BBE1AA1"), "BBE1AA11", "BBE1AA12", 1350., true);
        networkModifier.commitAllChanges();
        Switch createdSwitch = network.getSwitch("BBE1AA12 BBE1AA11 1");
        assertNotNull(createdSwitch);
        assertTrue(createdSwitch.isOpen());
        assertEquals(1350., Double.parseDouble(createdSwitch.getProperty(UcteConverterConstants.CURRENT_LIMIT_PROPERTY_KEY)), DOUBLE_TOLERANCE);
    }

    @Test
    void testCreateClosedSwitch() {
        setUp("ModifiedNetwork.uct");
        networkModifier.createSwitch(network.getVoltageLevel("BBE1AA1"), "BBE1AA11", "BBE1AA12", null, false);
        networkModifier.commitAllChanges();
        Switch createdSwitch = network.getSwitch("BBE1AA12 BBE1AA11 1");
        assertNotNull(createdSwitch);
        assertFalse(createdSwitch.isOpen());
        assertNull(createdSwitch.getProperty(UcteConverterConstants.CURRENT_LIMIT_PROPERTY_KEY));
    }

    @Test
    void testCreateSwitchError() {
        setUp("BaseNetwork.uct");
        VoltageLevel voltageLevel = network.getVoltageLevel("BBE1AA1");
        // should throw an error because buses are not on the same voltage level
        assertThrows(OpenRaoException.class, () -> networkModifier.createSwitch(voltageLevel, "BBE1AA11", "DDE1AA12", 1350., true));
    }

    void assertBranchesAreSimilar(Branch<?> expected, Branch<?> actual) {
        assertSameCurrentLimits(expected, actual);
        assertSameProperties(expected, actual);
    }

    void assertSameCurrentLimits(Branch<?> expected, Branch<?> actual) {
        for (TwoSides side : Set.of(TwoSides.ONE, TwoSides.TWO)) {
            if (expected.getNullableCurrentLimits(side) != null) {
                assertEquals(expected.getNullableCurrentLimits(side).getPermanentLimit(), actual.getNullableCurrentLimits(side).getPermanentLimit());
            } else {
                assertNull(actual.getNullableCurrentLimits(side));
            }
        }
    }

    void assertSameProperties(Branch<?> expected, Branch<?> actual) {
        assertEquals(expected.getPropertyNames(), actual.getPropertyNames());
        expected.getPropertyNames().forEach(property ->
            assertEquals(expected.getProperty(property), actual.getProperty(property)));
    }

    void assertSameFlow(Branch<?> expected, Branch<?> actual) {
        assertEquals(expected.getTerminal1().getP(), actual.getTerminal1().getP());
        assertEquals(expected.getTerminal1().getQ(), actual.getTerminal1().getQ());
        assertEquals(expected.getTerminal2().getP(), actual.getTerminal2().getP());
        assertEquals(expected.getTerminal2().getQ(), actual.getTerminal2().getQ());
    }

    @Test
    void testMoveLineSide1() {
        setUp("BaseNetwork.uct");

        // create a new bus BBE1AA1N between BBE1AA11 and BBE2AA1, a closed switch between BBE1AA1N and BBE1AA11,
        // and move line "BBE1AA11 BBE2AA1  1" side 1 from BBE1AA11 to BBE1AA1N
        networkModifier.createBus("BBE1AA1N", "BBE1AA1");
        networkModifier.createSwitch(network.getVoltageLevel("BBE1AA1"), "BBE1AA1N", "BBE1AA11", null, false);
        Branch<?> branchToMove = network.getLine("BBE1AA11 BBE2AA1  1");
        networkModifier.moveBranch(branchToMove, TwoSides.ONE, (Bus) network.getIdentifiable("BBE1AA1N"));
        networkModifier.commitAllChanges();

        // check that the correct modifications have been made to move the line
        assertNull(network.getIdentifiable("BBE1AA11 BBE2AA1  1"));
        Line createdLine = network.getLine("BBE1AA1N BBE2AA1  1");
        assertNotNull(createdLine);
        assertEquals("BBE1AA1N", createdLine.getTerminal1().getBusBreakerView().getBus().getId());
        assertEquals("BBE1AA1", createdLine.getTerminal1().getVoltageLevel().getId());
        assertEquals("BBE2AA1 ", createdLine.getTerminal2().getBusBreakerView().getBus().getId());
        assertEquals("BBE2AA1", createdLine.getTerminal2().getVoltageLevel().getId());

        // re-import network and check that the created line has the same properties as the old line
        Network initialNetwork = Network.read("BaseNetwork.uct", getClass().getResourceAsStream("BaseNetwork.uct"));
        Branch<?> initialBranch = initialNetwork.getLine("BBE1AA11 BBE2AA1  1");
        assertBranchesAreSimilar(initialBranch, createdLine);

        // check that the flow is the same on old line and new line
        LoadFlow.run(initialNetwork, LoadFlowParameters.load());
        LoadFlow.run(network, LoadFlowParameters.load());
        assertSameFlow(initialBranch, createdLine);
    }

    @Test
    void testMoveLineSide2() {
        // similar test to previous one, but moving line's side 2
        setUp("BaseNetwork.uct");

        networkModifier.createBus("BBE2AA1N", "BBE2AA1");
        networkModifier.createSwitch(network.getVoltageLevel("BBE2AA1"), "BBE2AA1N", "BBE2AA1 ", null, false);
        Branch<?> branchToMove = network.getLine("BBE1AA11 BBE2AA1  1");
        networkModifier.moveBranch(branchToMove, TwoSides.TWO, (Bus) network.getIdentifiable("BBE2AA1N"));
        networkModifier.commitAllChanges();

        assertNull(network.getIdentifiable("BBE1AA11 BBE2AA1  1"));
        Line createdLine = network.getLine("BBE1AA11 BBE2AA1N 1");
        assertNotNull(createdLine);
        assertEquals("BBE1AA11", createdLine.getTerminal1().getBusBreakerView().getBus().getId());
        assertEquals("BBE1AA1", createdLine.getTerminal1().getVoltageLevel().getId());
        assertEquals("BBE2AA1N", createdLine.getTerminal2().getBusBreakerView().getBus().getId());
        assertEquals("BBE2AA1", createdLine.getTerminal2().getVoltageLevel().getId());

        Network initialNetwork = Network.read("BaseNetwork.uct", getClass().getResourceAsStream("BaseNetwork.uct"));
        Branch<?> initialBranch = initialNetwork.getLine("BBE1AA11 BBE2AA1  1");
        assertBranchesAreSimilar(initialBranch, createdLine);

        LoadFlow.run(initialNetwork, LoadFlowParameters.load());
        LoadFlow.run(network, LoadFlowParameters.load());
        assertSameFlow(initialBranch, createdLine);
    }

    @Test
    void testMoveTieLine() {
        setUp("BaseNetwork_tieline.uct");
        String initialTieLineId = "DDE3AA1  X_DEFR1  1 + FFR2AA1  X_DEFR1  1";

        // create a new bus DDE3AA1N between DDE3AA1 and X_DEFR1, a closed switch between DDE3AA1N and DDE3AA1,
        // and move line "DDE3AA1  X_DEFR1  1" side 1 from DDE3AA1 to DDE3AA1N
        networkModifier.createBus("DDE3AA1N", "DDE3AA1");
        networkModifier.createSwitch(network.getVoltageLevel("DDE3AA1"), "DDE3AA1N", "DDE3AA1 ", null, false);
        Branch<?> branchToMove = network.getBranch(initialTieLineId);
        networkModifier.moveBranch(branchToMove, TwoSides.ONE, (Bus) network.getIdentifiable("DDE3AA1N"));
        networkModifier.commitAllChanges();

        // check that the correct modifications have been made to move the line
        assertNull(network.getIdentifiable("DDE3AA1  X_DEFR1  1"));
        Branch<?> createdTieLine = network.getBranch("DDE3AA1N X_DEFR1  1 + FFR2AA1  X_DEFR1  1");
        assertNotNull(createdTieLine);
        assertTrue(createdTieLine instanceof TieLine);
        assertEquals("DDE3AA1N", createdTieLine.getTerminal1().getBusBreakerView().getBus().getId());
        assertEquals("DDE3AA1", createdTieLine.getTerminal1().getVoltageLevel().getId());
        assertEquals("X_DEFR1 ", ((TieLine) createdTieLine).getPairingKey());
        assertEquals("FFR2AA1 ", createdTieLine.getTerminal2().getBusBreakerView().getBus().getId());
        assertEquals("FFR2AA1", createdTieLine.getTerminal2().getVoltageLevel().getId());

        // re-import network and check that the created line has the same properties as the old line
        Network initialNetwork = Network.read("BaseNetwork_tieline.uct", getClass().getResourceAsStream("BaseNetwork_tieline.uct"));
        Branch<?> initialBranch = initialNetwork.getBranch(initialTieLineId);
        assertBranchesAreSimilar(initialBranch, createdTieLine);

        // check that the flow is the same on old line and new line
        LoadFlow.run(initialNetwork, LoadFlowParameters.load());
        LoadFlow.run(network, LoadFlowParameters.load());
        assertSameFlow(initialBranch, createdTieLine);
    }

    @Test
    void testMoveTwoWindingsTransformer() {
        setUp("BaseNetwork.uct");

        // create a new bus BBE3AA1N between BBE2AA1 and BBE3AA1, a closed switch between BBE3AA1N and BBE3AA1,
        // and move line "BBE2AA1  BBE3AA1  1" side 2 from BBE3AA1 to BBE3AA1N
        networkModifier.createBus("BBE3AA1N", "BBE2AA1");
        networkModifier.createSwitch(network.getVoltageLevel("BBE2AA1"), "BBE3AA1N", "BBE3AA1 ", null, false);
        Branch<?> branchToMove = network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1");
        // PS: in Powsybl convention, side one of a TwoWindingsTransformer is node "to" (BBE3AA1  in this case)
        networkModifier.moveBranch(branchToMove, TwoSides.ONE, (Bus) network.getIdentifiable("BBE3AA1N"));
        networkModifier.commitAllChanges();

        // check that the correct modifications have been made to move the line
        assertNull(network.getIdentifiable("BBE2AA1  BBE3AA1  1"));
        TwoWindingsTransformer createdTransformer = network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1N 1");
        assertNotNull(createdTransformer);
        assertEquals("BBE2AA1 ", createdTransformer.getTerminal2().getBusBreakerView().getBus().getId());
        assertEquals("BBE2AA1", createdTransformer.getTerminal2().getVoltageLevel().getId());
        assertEquals("BBE3AA1N", createdTransformer.getTerminal1().getBusBreakerView().getBus().getId());
        assertEquals("BBE2AA1", createdTransformer.getTerminal1().getVoltageLevel().getId());

        // re-import network and check that the created line has the same properties as the old line
        Network initialNetwork = Network.read("BaseNetwork.uct", getClass().getResourceAsStream("BaseNetwork.uct"));
        TwoWindingsTransformer initialTransformer = initialNetwork.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1");
        assertBranchesAreSimilar(initialTransformer, createdTransformer);

        // check that PSTs are the same
        assertEquals(initialTransformer.getPhaseTapChanger().getLowTapPosition(), createdTransformer.getPhaseTapChanger().getLowTapPosition());
        assertEquals(initialTransformer.getPhaseTapChanger().getTapPosition(), createdTransformer.getPhaseTapChanger().getTapPosition());
        assertEquals(initialTransformer.getPhaseTapChanger().getRegulationValue(), createdTransformer.getPhaseTapChanger().getRegulationValue(), DOUBLE_TOLERANCE);
        assertEquals(initialTransformer.getPhaseTapChanger().getRegulationMode(), createdTransformer.getPhaseTapChanger().getRegulationMode());
        assertSamePstSteps(initialTransformer.getPhaseTapChanger(), createdTransformer.getPhaseTapChanger());

        // check that the flow is the same on old line and new line
        LoadFlow.run(initialNetwork, LoadFlowParameters.load());
        LoadFlow.run(network, LoadFlowParameters.load());
        assertSameFlow(initialTransformer, createdTransformer);
    }

    void assertSamePstSteps(PhaseTapChanger pst1, PhaseTapChanger pst2) {
        assertEquals(pst1.getAllSteps().keySet(), pst2.getAllSteps().keySet());
        pst1.getAllSteps().forEach((key, expectedValue) -> {
            PhaseTapChangerStep actualValue = pst2.getAllSteps().get(key);
            assertEquals(expectedValue.getAlpha(), actualValue.getAlpha(), DOUBLE_TOLERANCE);
            assertEquals(expectedValue.getB(), actualValue.getB(), DOUBLE_TOLERANCE);
            assertEquals(expectedValue.getG(), actualValue.getG(), DOUBLE_TOLERANCE);
            assertEquals(expectedValue.getR(), actualValue.getR(), DOUBLE_TOLERANCE);
            assertEquals(expectedValue.getRho(), actualValue.getRho(), DOUBLE_TOLERANCE);
            assertEquals(expectedValue.getX(), actualValue.getX(), DOUBLE_TOLERANCE);
        });
    }

    @Test
    void testMoveBranchException() {
        setUp("BaseNetwork.uct");
        Branch<?> branchToMove = network.getLine("BBE1AA11 BBE2AA1  1");
        Bus bus = (Bus) network.getIdentifiable("BBE3AA1 ");
        // should fail : The network already contains a line with the id 'BBE1AA11 BBE3AA1  1'
        assertThrows(OpenRaoException.class, () -> networkModifier.moveBranch(branchToMove, TwoSides.TWO, bus));
    }
}
