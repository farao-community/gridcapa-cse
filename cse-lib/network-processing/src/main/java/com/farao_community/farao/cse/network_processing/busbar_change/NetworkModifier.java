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

import java.util.*;

/**
 * This class exposes useful methods to modify a network
 *
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public class NetworkModifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkModifier.class);

    private final Network network;
    private Set<Bus> busesToRemove;
    private final Map<String, List<String>> identifiableAliases = new HashMap<>();

    public NetworkModifier(Network network) {
        this.network = network;
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
            throw new FaraoException(String.format("Could not create bus %s", newBusId), e);
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
            throw new FaraoException(String.format("Could not create switch between %s and %s", bus1Id, bus2Id), e);
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
        try {
            if (branch instanceof TwoWindingsTransformer) {
                moveTwoWindingsTransformer((TwoWindingsTransformer) branch, side, bus);
            } else if (branch instanceof TieLine) {
                moveTieLine((TieLine) branch, side, bus);
            } else if (branch instanceof Line) {
                moveLine((Line) branch, side, bus);
            } else {
                throw new FaraoException(String.format("Cannot move %s of type %s", branch.getId(), branch.getClass()));
            }
        } catch (PowsyblException e) {
            throw new FaraoException(String.format("Could not move line %s", branch.getId()), e);
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
        removeFromNetworkAndAliasesMap(line);
        Line newLine = adder.add();
        copyCurrentLimits(line, newLine);
        copyProperties(line, newLine);
        LOGGER.debug("Line '{}' has been removed, new Line '{}' has been created", line.getId(), newLine.getId());
        addAliasesFromOldToNew(line, newLine);
    }

    private void moveTwoWindingsTransformer(TwoWindingsTransformer transformer, Branch.Side side, Bus bus) {
        String newId = replaceSimpleBranchNode(transformer, side, bus.getId());
        TwoWindingsTransformerAdder adder = initializeTwoWindingsTransformerAdderToMove(transformer, newId);
        setBranchAdderProperties(adder, transformer, side, bus);
        removeFromNetworkAndAliasesMap(transformer);
        TwoWindingsTransformer newTransformer = adder.add();
        copyCurrentLimits(transformer, newTransformer);
        copyProperties(transformer, newTransformer);
        copyTapChanger(transformer, newTransformer);
        LOGGER.debug("TwoWindingsTransformer '{}' has been removed, new transformer '{}' has been created", transformer.getId(), newTransformer.getId());
        addAliasesFromOldToNew(transformer, newTransformer);
    }

    private void moveTieLine(TieLine tieLine, Branch.Side side, Bus bus) {
        DanglingLine danglingLineToReplace = (side == Branch.Side.ONE) ? tieLine.getDanglingLine1() : tieLine.getDanglingLine2();
        String newTieLineId = replaceTieLineNodeInTieLineId(tieLine, danglingLineToReplace, bus.getId());
        TieLine newTieLine = replaceTieLine(tieLine, newTieLineId, danglingLineToReplace, bus);
        copyCurrentLimits(tieLine, newTieLine);
        copyProperties(tieLine, newTieLine);
        LOGGER.debug("TieLine '{}' has been removed, new TieLine '{}' has been created", tieLine.getId(), newTieLine.getId());
        addAliasesFromOldToNew(tieLine, newTieLine);
    }

    private void addAliasesFromOldToNew(Identifiable<?> oldIdentifiable, Identifiable<?> newIdentifiable) {
        addAlias(newIdentifiable, oldIdentifiable.getId());
        oldIdentifiable.getAliases().forEach(alias -> addAlias(newIdentifiable, alias));
    }

    private void addAlias(Identifiable<?> identifiable, String alias) {
        identifiable.addAlias(alias);
        identifiableAliases.putIfAbsent(identifiable.getId(), new ArrayList<>());
        identifiableAliases.get(identifiable.getId()).add(alias);
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

    private static String replaceTieLineNodeInTieLineId(TieLine tieLine, DanglingLine danglingLineToReplace, String newNodeId) {
        String nodeToReplace = getTieLineNodeToReplace(tieLine, danglingLineToReplace);
        return tieLine.getId().replace(nodeToReplace, newNodeId);
    }

    private static DanglingLine replaceDanglingLineAtSide(TieLine tieLine, DanglingLine danglingLineToReplace, String newNodeId) {
        VoltageLevel voltageLevel = danglingLineToReplace.getTerminal().getVoltageLevel();
        danglingLineToReplace.remove();
        String nodeToReplace = getTieLineNodeToReplace(tieLine, danglingLineToReplace);
        return voltageLevel.newDanglingLine()
                .setId(danglingLineToReplace.getId().replace(nodeToReplace, newNodeId))
                .setFictitious(danglingLineToReplace.isFictitious())
                .setPairingKey(danglingLineToReplace.getPairingKey())
                .setR(danglingLineToReplace.getR())
                .setX(danglingLineToReplace.getX())
                .setG(danglingLineToReplace.getG())
                .setB(danglingLineToReplace.getB())
                .setConnectableBus(newNodeId)
                .setBus(newNodeId)
                .setP0(danglingLineToReplace.getP0())
                .setQ0(danglingLineToReplace.getQ0())
                .add();
    }

    private static String getTieLineNodeToReplace(TieLine tieLine, DanglingLine danglingLineToReplace) {
        String node1 = danglingLineToReplace.getId().substring(0, 8);
        String node2 = danglingLineToReplace.getId().substring(9, 17);
        return node1.equals(tieLine.getPairingKey()) ? node2 : node1;
    }

    private TieLine replaceTieLine(TieLine tieLine, String newId, DanglingLine danglingLineToReplace, Bus bus) {
        DanglingLine half1 = tieLine.getDanglingLine1();
        DanglingLine half2 = tieLine.getDanglingLine2();
        removeFromNetworkAndAliasesMap(tieLine);
        DanglingLine newHalf1 = (half1 == danglingLineToReplace) ? replaceDanglingLineAtSide(tieLine, danglingLineToReplace, bus.getId()) : half1;
        DanglingLine newHalf2 = (half2 == danglingLineToReplace) ? replaceDanglingLineAtSide(tieLine, danglingLineToReplace, bus.getId()) : half2;
        return network.newTieLine()
            .setId(newId)
            .setDanglingLine1(newHalf1.getId())
            .setDanglingLine2(newHalf2.getId())
            .add();
    }

    private void removeFromNetworkAndAliasesMap(Connectable<?> connectable) {
        connectable.remove();
        identifiableAliases.remove(connectable.getId());
    }

    /**
     * TieLine do not implement Connectable interface anymore. But it still have a *remove* method
     */
    private void removeFromNetworkAndAliasesMap(TieLine tieLine) {
        tieLine.remove();
        identifiableAliases.remove(tieLine.getId());
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
            busesToRemove.forEach(this::effectivelyRemoveBus);
            busesToRemove = new HashSet<>();
            cleanAliases();
        } catch (PowsyblException e) {
            throw new FaraoException("Could not apply all changes to network", e);
        }
    }

    private void cleanAliases() {
        for (var identifiableAliasesEntry : identifiableAliases.entrySet()) {
            for (String alias : identifiableAliasesEntry.getValue()) {
                network.getIdentifiable(identifiableAliasesEntry.getKey()).removeAlias(alias);
            }
        }
    }
}
