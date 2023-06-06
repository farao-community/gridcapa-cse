/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing.busbar_change;

import com.farao_community.farao.commons.FaraoException;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.ucte.converter.util.UcteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class exposes useful methods to modify a network
 *
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public class NetworkModifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkModifier.class);

    private final Network network;
    private Map<Branch<?>, Branch<?>> movedBranches;
    private Set<Bus> busesToRemove;

    public NetworkModifier(Network network) {
        this.network = network;
        this.movedBranches = new HashMap<>();
        this.busesToRemove = new HashSet<>();
    }

    public Network getNetwork() {
        return network;
    }

    /**
     * Create a bus with a given ID, on a given voltage level
     * Use a given reference bus to copy generators and loads
     *
     * @param newBusId:       the ID of the new bus
     * @param voltageLevelId: the ID of the voltage level
     * @return the bus created
     */
    public Bus createBus(String newBusId, String voltageLevelId) {
        try {
            VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
            Bus newBus = voltageLevel.getBusBreakerView().newBus()
                .setId(newBusId)
                .setFictitious(false)
                .setEnsureIdUnicity(true)
                .add();
            findAndSetGeographicalName(newBus, voltageLevel);
            LOGGER.debug("New bus '{}' has been created", newBus.getId());
            return newBus;
        } catch (PowsyblException e) {
            throw new FaraoException(String.format("Could not create bus %s: %s", newBusId, e.getMessage()));
        }
    }

    /**
     * Remove a bus from the network
     */
    public void removeBus(Bus bus) {
        // Should be done after committing branch changes
        busesToRemove.add(bus);
        LOGGER.debug("Bus {} has been removed", bus.getId());
    }

    private void effectivelyRemoveBus(Bus bus) {
        // Should be done after committing branch changes
        bus.getVoltageLevel().getBusBreakerView().removeBus(bus.getId());
    }

    /**
     * Create a switch on a given voltage level between two given buses
     * Set its current limit to a given value and its open/close status to a given status
     *
     * @param voltageLevel: the voltage level object
     * @param bus1Id:       the ID of the first bus
     * @param bus2Id:       the ID of the second bus
     * @param currentLimit: the value of the currentLimit
     * @param open:         a boolean that describes if the switch is open or close
     * @return the switch object created
     */
    public Switch createSwitch(VoltageLevel voltageLevel, String bus1Id, String bus2Id, Double currentLimit, boolean open) {
        try {
            String switchId = String.format("%s %s 1", bus2Id, bus1Id);
            if (voltageLevel.getBusBreakerView().getSwitch(switchId) != null) {
                // Switch has already been created (by other RA)
                return voltageLevel.getBusBreakerView().getSwitch(switchId);
            }
            Switch newSwitch = voltageLevel.getBusBreakerView().newSwitch()
                .setId(switchId)
                .setBus1(bus1Id)
                .setBus2(bus2Id)
                .setOpen(open)
                .setFictitious(false)
                .setEnsureIdUnicity(true)
                .add();
            if (currentLimit != null) {
                newSwitch.setProperty(UcteConstants.CURRENT_LIMIT_PROPERTY_KEY, String.valueOf((int) currentLimit.doubleValue()));
            }
            LOGGER.debug("New switch '{}' has been created", newSwitch.getId());
            return newSwitch;
        } catch (PowsyblException e) {
            throw new FaraoException(String.format("Could not create switch between %s and %s: %s", bus1Id, bus2Id, e.getMessage()));
        }
    }

    /**
     * Remove a switch from the network
     */
    public void removeSwitch(Switch networkSwitch) {
        networkSwitch.getVoltageLevel().getBusBreakerView().removeSwitch(networkSwitch.getId());
        LOGGER.debug("Switch {} has been removed", networkSwitch.getId());
    }

    /**
     * Move a branch's given side to a given bus
     * (The method actually copies the branch to a new one then deletes it)
     *
     * @param branch the branch to move
     * @param side   the side of the branch to move
     * @param bus    the new bus to connect to the side
     */
    public void moveBranch(Branch<?> branch, Branch.Side side, Bus bus) {
        // In case line has already been modified on the other side, then modify the new line instead
        // For example if we have a line "NODE1 NODE2"
        // RA1 moves side 1 : then the line is replaced with "FBUS1 NODE2" (FBUS = fictitious bus)
        // RA2 moves side 2 : then the line "FBUS1 NODE2" should be replaced with "FBUS1 FBUS2"
        // (instead of replacing "NODE1 NODE2" with "NODE1 FBUS2")
        Branch<?> branchToMove = movedBranches.getOrDefault(branch, branch);
        try {
            if (branchToMove instanceof TwoWindingsTransformer) {
                moveTwoWindingsTransformer((TwoWindingsTransformer) branchToMove, side, bus);
            } else if (branchToMove instanceof TieLine) {
                moveTieLine((TieLine) branchToMove, side, bus);
            } else if (branchToMove instanceof Line) {
                moveLine((Line) branchToMove, side, bus);
            } else {
                throw new FaraoException(String.format("Cannot move %s of type %s", branchToMove.getId(), branchToMove.getClass()));
            }
        } catch (PowsyblException e) {
            throw new FaraoException(String.format("Could not move line %s: %s", branchToMove.getId(), e.getMessage()));
        }
    }

    private static String generateUcteId(String node1, String node2, String suffix) {
        return String.format("%1$8s %2$8s %3$s", node1, node2, suffix);
    }

    private static void findAndSetGeographicalName(Bus bus, VoltageLevel voltageLevel) {
        for (Bus otherBus : voltageLevel.getBusBreakerView().getBuses()) {
            if (otherBus.hasProperty(UcteConstants.GEOGRAPHICAL_NAME_PROPERTY_KEY)) {
                bus.setProperty(UcteConstants.GEOGRAPHICAL_NAME_PROPERTY_KEY, otherBus.getProperty(UcteConstants.GEOGRAPHICAL_NAME_PROPERTY_KEY));
                break;
            }
        }
    }

    private static String getOrderCode(Branch<?> branch) {
        if (branch.hasProperty(UcteConstants.ORDER_CODE)) {
            return branch.getProperty(UcteConstants.ORDER_CODE);
        } else {
            return branch.getId().substring(branch.getId().length() - 1);
        }
    }

    private void moveLine(Line line, Branch.Side side, Bus bus) {
        String newLineId = replaceSimpleBranchNode(line, side, bus.getId());
        LineAdder adder = initializeLineAdderToMove(line, newLineId);
        setBranchAdderProperties(adder, line, side, bus);
        Line newLine = adder.add();
        copyCurrentLimits(line, newLine);
        copyProperties(line, newLine);
        LOGGER.debug("Line '{}' has been removed, new TieLine '{}' has been created", line.getId(), newLine.getId());
        movedBranches.put(line, newLine);
    }

    private void moveTieLine(TieLine tieLine, Branch.Side side, Bus bus) {
        String newLineId = replaceTieLineNode(tieLine, side, bus.getId());
        TieLineAdder adder = initializeTieLineAdderToMove(tieLine, newLineId, side, bus);
        setBranchAdderProperties(adder, tieLine, side, bus);
        TieLine newTieLine = adder.add();
        copyCurrentLimits(tieLine, newTieLine);
        copyProperties(tieLine, newTieLine);
        LOGGER.debug("TieLine '{}' has been removed, new TieLine '{}' has been created", tieLine.getId(), newTieLine.getId());
        movedBranches.put(tieLine, newTieLine);
    }

    private void moveTwoWindingsTransformer(TwoWindingsTransformer transformer, Branch.Side side, Bus bus) {
        String newId = replaceSimpleBranchNode(transformer, side, bus.getId());
        TwoWindingsTransformerAdder adder = initializeTwoWindingsTransformerAdderToMove(transformer, newId);
        setBranchAdderProperties(adder, transformer, side, bus);
        TwoWindingsTransformer newTransformer = adder.add();
        copyCurrentLimits(transformer, newTransformer);
        copyProperties(transformer, newTransformer);
        copyTapChanger(transformer, newTransformer);
        LOGGER.debug("TwoWindingsTransformer '{}' has been removed, new transformer '{}' has been created", transformer.getId(), newTransformer.getId());
        movedBranches.put(transformer, newTransformer);
    }

    private static void copyTapChanger(TwoWindingsTransformer transformerFrom, TwoWindingsTransformer transformerTo) {
        PhaseTapChanger pst = transformerFrom.getPhaseTapChanger();
        if (pst == null) {
            return;
        }
        PhaseTapChangerAdder ptca = transformerTo.newPhaseTapChanger()
            .setLowTapPosition(pst.getLowTapPosition())
            .setTapPosition(pst.getTapPosition())
            .setRegulationValue(pst.getRegulationValue())
            .setRegulationMode(pst.getRegulationMode());
        // NB: it is very important to sort the map when copying steps so as to keep the same positions
        pst.getAllSteps().entrySet()
            .stream().sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue).forEach(step ->
            ptca.beginStep()
                .setRho(step.getRho())
                .setAlpha(step.getAlpha())
                .setR(step.getR())
                .setX(step.getX())
                .setG(step.getG())
                .setB(step.getB())
                .endStep()
        );
        ptca.add();
    }

    private static void copyProperties(Identifiable<?> identifiableFrom, Identifiable<?> identifiableTo) {
        identifiableFrom.getPropertyNames().forEach(property ->
            identifiableTo.setProperty(property, identifiableFrom.getProperty(property))
        );
    }

    private LineAdder initializeLineAdderToMove(Line line, String newId) {
        return network.newLine()
            .setId(newId)
            .setR(line.getR())
            .setX(line.getX())
            .setG1(line.getG1())
            .setB1(line.getB1())
            .setG2(line.getG2())
            .setB2(line.getB2())
            .setFictitious(line.isFictitious())
            .setName(newId);
    }

    /**
     * Sets LineAdder's attributes identical to a given side of a branch
     * WARNING: This only works for bus breaker topology. UCTE import in PowSyBl is always done in bus breaker view.
     */
    private static BranchAdder<?, ?> setIdenticalToSide(Branch<?> branch, Branch.Side side, BranchAdder<?, ?> adder) {
        if (side == Branch.Side.ONE) {
            return adder.setVoltageLevel1(branch.getTerminal1().getVoltageLevel().getId())
                .setConnectableBus1(branch.getTerminal1().getBusBreakerView().getConnectableBus().getId())
                .setBus1(branch.getTerminal1().getBusBreakerView().getBus() != null ? branch.getTerminal1().getBusBreakerView().getBus().getId() : null);
        } else {
            return adder.setVoltageLevel2(branch.getTerminal2().getVoltageLevel().getId())
                .setConnectableBus2(branch.getTerminal2().getBusBreakerView().getConnectableBus().getId())
                .setBus2(branch.getTerminal2().getBusBreakerView().getBus() != null ? branch.getTerminal2().getBusBreakerView().getBus().getId() : null);
        }
    }

    private void setBranchAdderProperties(BranchAdder<?, ?> adder, Branch<?> branchToCopy, Branch.Side sideToUpdate, Bus busToUpdate) {
        if (sideToUpdate == Branch.Side.ONE) {
            setIdenticalToSide(branchToCopy, Branch.Side.TWO, adder)
                .setConnectableBus1(busToUpdate.getId())
                .setBus1(busToUpdate.getId())
                .setVoltageLevel1(busToUpdate.getVoltageLevel().getId());
        } else if (sideToUpdate == Branch.Side.TWO) {
            setIdenticalToSide(branchToCopy, Branch.Side.ONE, adder)
                .setConnectableBus2(busToUpdate.getId())
                .setBus2(busToUpdate.getId())
                .setVoltageLevel2(busToUpdate.getVoltageLevel().getId());
        }
    }

    private String replaceSimpleBranchNode(Branch<?> branch, Branch.Side side, String newNodeId) {
        String from = (side == Branch.Side.ONE) ? newNodeId : branch.getTerminal1().getBusBreakerView().getConnectableBus().getId();
        String to = (side == Branch.Side.ONE) ? branch.getTerminal2().getBusBreakerView().getConnectableBus().getId() : newNodeId;
        if (branch instanceof TwoWindingsTransformer) {
            // convention is inverted
            return generateUcteId(to, from, getOrderCode(branch));
        } else {
            return generateUcteId(from, to, getOrderCode(branch));
        }
    }

    private static String replaceTieLineNode(TieLine tieLine, Branch.Side side, String newNodeId) {
        String nodeToReplace = getTieLineNodeToReplace(tieLine, side);
        return tieLine.getId().replace(nodeToReplace, newNodeId);
    }

    private static String replaceHalfLineNode(TieLine tieLine, Branch.Side side, String newNodeId) {
        TieLine.HalfLine halfLine = (side == Branch.Side.ONE) ? tieLine.getHalf1() : tieLine.getHalf2();
        String nodeToReplace = getTieLineNodeToReplace(tieLine, side);
        return halfLine.getId().replace(nodeToReplace, newNodeId);
    }

    private static String getTieLineNodeToReplace(TieLine tieLine, Branch.Side side) {
        TieLine.HalfLine halfLine = (side == Branch.Side.ONE) ? tieLine.getHalf1() : tieLine.getHalf2();
        String node1 = halfLine.getId().substring(0, 8);
        String node2 = halfLine.getId().substring(9, 17);
        return node1.equals(tieLine.getUcteXnodeCode()) ? node2 : node1;
    }

    private TieLineAdder initializeTieLineAdderToMove(TieLine tieLine, String newId, Branch.Side sideToUpdate, Bus bus) {
        TieLine.HalfLine half1 = tieLine.getHalf1();
        TieLine.HalfLine half2 = tieLine.getHalf2();
        String xnodeCode = tieLine.getUcteXnodeCode();
        String newHalf1Id = (sideToUpdate == Branch.Side.ONE) ? replaceHalfLineNode(tieLine, Branch.Side.ONE, bus.getId()) : half1.getId();
        String newHalf2Id = (sideToUpdate == Branch.Side.TWO) ? replaceHalfLineNode(tieLine, Branch.Side.TWO, bus.getId()) : half2.getId();
        return network.newTieLine()
            .setId(newId)
            .newHalfLine1()
            .setId(newHalf1Id)
            .setR(half1.getR())
            .setX(half1.getX())
            .setB1(half1.getB1())
            .setB2(half1.getB2())
            .setG1(half1.getG1())
            .setG2(half1.getG2())
            .setFictitious(half1.isFictitious())
            .add()
            .newHalfLine2()
            .setId(newHalf2Id)
            .setR(half2.getR())
            .setX(half2.getX())
            .setB1(half2.getB1())
            .setB2(half2.getB2())
            .setG1(half2.getG1())
            .setG2(half2.getG2())
            .setFictitious(half2.isFictitious())
            .add()
            .setUcteXnodeCode(xnodeCode);
    }

    private TwoWindingsTransformerAdder initializeTwoWindingsTransformerAdderToMove(TwoWindingsTransformer twoWindingsTransformer, String newId) {
        return twoWindingsTransformer.getSubstation().orElseThrow(FaraoException::new).newTwoWindingsTransformer()
            .setEnsureIdUnicity(true)
            .setId(newId)
            .setRatedU1(twoWindingsTransformer.getRatedU1())
            .setRatedU2(twoWindingsTransformer.getRatedU2())
            .setR(twoWindingsTransformer.getR())
            .setX(twoWindingsTransformer.getX())
            .setG(twoWindingsTransformer.getG())
            .setB(twoWindingsTransformer.getB())
            .setFictitious(twoWindingsTransformer.isFictitious());
    }

    private static void copyCurrentLimits(Branch<?> branchFrom, Branch<?> branchTo) {
        branchFrom.getCurrentLimits1().ifPresent(currentLimits ->
            branchTo.newCurrentLimits1()
                .setPermanentLimit(currentLimits.getPermanentLimit())
                .add()
        );
        branchFrom.getCurrentLimits2().ifPresent(currentLimits ->
            branchTo.newCurrentLimits2()
                .setPermanentLimit(currentLimits.getPermanentLimit())
                .add()
        );
    }

    /**
     * Returns the connected branches to a given bus, and the side they're connected to it
     * If the branch has been moved, the move shall be taken into account
     */
    public Map<Branch<?>, Branch.Side> getBranchesStillConnectedToBus(Bus bus) {
        Map<Branch<?>, Branch.Side> connectedBranches = new HashMap<>();
        network.getBranchStream()
            .map(branch -> movedBranches.getOrDefault(branch, branch))
            .forEach(branch -> {
                if (branch.getTerminal1().getBusBreakerView().getConnectableBus().equals(bus)) {
                    connectedBranches.put(branch, Branch.Side.ONE);
                } else if (branch.getTerminal2().getBusBreakerView().getConnectableBus().equals(bus)) {
                    connectedBranches.put(branch, Branch.Side.TWO);
                }
            });
        return connectedBranches;
    }

    /**
     * Objects moved by the modifier are not removed until they are no longer needed
     * Call this function when you want to remove these objects
     */
    public void commitAllChanges() {
        try {
            movedBranches.keySet().forEach(branch -> branch.remove());
            movedBranches = new HashMap<>();
            busesToRemove.forEach(this::effectivelyRemoveBus);
            busesToRemove = new HashSet<>();
        } catch (PowsyblException e) {
            throw new FaraoException(String.format("Could not apply all changes to network: %s", e.getMessage()));
        }
    }
}
