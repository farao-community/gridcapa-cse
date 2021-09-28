/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing.busbar_change;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation_util.ucte.UcteBusHelper;
import com.farao_community.farao.data.crac_creation_util.ucte.UcteCnecElementHelper;
import com.farao_community.farao.data.crac_creation_util.ucte.UcteNetworkAnalyzer;
import com.farao_community.farao.data.crac_creation_util.ucte.UcteNetworkAnalyzerProperties;
import com.powsybl.iidm.network.*;
import com.powsybl.ucte.converter.util.UcteConstants;
import com.rte_france.farao.data.crac.io.cse.*;
import com.rte_france.farao.data.crac.io.cse.crac_creator.CseCracCreator;
import com.rte_france.farao.data.crac.io.cse.crac_creator.parameters.BusBarChangeSwitches;
import com.rte_france.farao.data.crac.io.cse.importer.CseCracImporter;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class processes a CSE crac file in order to create, in the network, switches that can be used to apply
 * BusBar remedial actions
 *
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public class BusBarChangeProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusBarChangeProcessor.class);

    private Network network;
    private UcteNetworkAnalyzer ucteNetworkAnalyzer;

    private Map<String, Set<SwitchPairToCreate>> switchesToCreatePerRa;
    private Map<String, String> initialNodePerRa;
    private Map<String, String> finalNodePerRa;
    private Map<String, BusToCreate> busesToCreate;
    private Map<String, Map<String, String>> createdSwitches;

    public BusBarChangeProcessor() {
        // nothing to do here
    }

    public Set<BusBarChangeSwitches> process(Network network, InputStream cracInputStream) {
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(cracInputStream);
        this.network = network;
        ucteNetworkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));

        switchesToCreatePerRa = new HashMap<>();
        initialNodePerRa = new HashMap<>();
        finalNodePerRa = new HashMap<>();
        busesToCreate = new HashMap<>();
        createdSwitches = new HashMap<>();

        TCRACSeries tcracSeries = CseCracCreator.getCracSeries(cseCrac.getCracDocument());
        List<TRemedialActions> tRemedialActionsList = tcracSeries.getRemedialActions();

        for (TRemedialActions tRemedialActions : tRemedialActionsList) {
            if (tRemedialActions != null) {
                tRemedialActions.getRemedialAction().stream()
                    .filter(tRemedialAction -> tRemedialAction.getBusBar() != null)
                    .forEach(tRemedialAction -> {
                        try {
                            computeBusesAndSwitchesToCreate(tRemedialAction);
                        } catch (FaraoException e) {
                            LOGGER.warn("RA {} has been skipped: {}", tRemedialAction.getName().getV(), e.getMessage());
                        }
                    });
            }
        }
        createBuses();
        createSwitches();
        return computeBusBarChangeSwitches();
    }

    /**
     * For every BusBar TRemedialAction, this method detects and stores info about elements that should be created:
     * - buses, if initial or final node does not exist in the network (info stored in busesToCreate)
     * - switches, to be able to move the branch from the initial to the final node (info stored in switchesToCreatePerRa)
     */
    private void computeBusesAndSwitchesToCreate(TRemedialAction tRemedialAction) {
        String raId = tRemedialAction.getName().getV();
        Set<BusToCreate> raBusesToCreate = new HashSet<>();
        Set<SwitchPairToCreate> raSwitchesToCreate = new HashSet<>();

        // Get initial and final nodes
        UcteBusHelper initialNodeHelper = new UcteBusHelper(tRemedialAction.getBusBar().getInitialNode().getV(), ucteNetworkAnalyzer);
        UcteBusHelper finalNodeHelper = new UcteBusHelper(tRemedialAction.getBusBar().getFinalNode().getV(), ucteNetworkAnalyzer);

        String initialNodeId;
        String finalNodeId;
        Bus referenceBus;
        if (initialNodeHelper.isValid() && finalNodeHelper.isValid()) {
            initialNodeId = initialNodeHelper.getIdInNetwork();
            finalNodeId = finalNodeHelper.getIdInNetwork();
        } else if (initialNodeHelper.isValid() && !finalNodeHelper.isValid()) {
            initialNodeId = initialNodeHelper.getIdInNetwork();
            referenceBus = (Bus) network.getIdentifiable(initialNodeId);
            finalNodeId = tRemedialAction.getBusBar().getFinalNode().getV();
            raBusesToCreate.add(new BusToCreate(finalNodeId, referenceBus.getVoltageLevel().getId(), initialNodeId));
        } else if (!initialNodeHelper.isValid() && finalNodeHelper.isValid()) {
            finalNodeId = finalNodeHelper.getIdInNetwork();
            referenceBus = (Bus) network.getIdentifiable(finalNodeId);
            initialNodeId = tRemedialAction.getBusBar().getInitialNode().getV();
            raBusesToCreate.add(new BusToCreate(initialNodeId, referenceBus.getVoltageLevel().getId(), finalNodeId));
        } else {
            throw new FaraoException(String.format("Remedial action's initial and final nodes are not valid: %s", initialNodeHelper.getInvalidReason()));
        }

        // Store all switches to create
        switchesToCreatePerRa.put(raId, new HashSet<>());
        for (TBranch tBranch : tRemedialAction.getBusBar().getBranch()) {
            String suffix = String.valueOf(tBranch.getOrder().getV());
            // Try to get from->to branch
            UcteCnecElementHelper branchHelper = getBranchHelper(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), suffix);
            // Branch may be already on final node, try from/to->final node
            if (!branchHelper.isValid()) {
                branchHelper = getBranchHelper(tBranch.getFromNode().getV(), finalNodeId, suffix);
            }
            if (!branchHelper.isValid()) {
                branchHelper = getBranchHelper(tBranch.getToNode().getV(), finalNodeId, suffix);
            }
            if (!branchHelper.isValid()) {
                throw new FaraoException(String.format("One of the branches (%s) in the remedial action is not valid", generateUcteId(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), suffix)));
            } else if (!(network.getIdentifiable(branchHelper.getIdInNetwork()) instanceof Line)) {
                throw new FaraoException(String.format("One of the branches (%s) in the remedial action is not a line: %s", branchHelper.getIdInNetwork(), network.getIdentifiable(branchHelper.getIdInNetwork()).getClass()));
            }
            raSwitchesToCreate.add(new SwitchPairToCreate(branchHelper.getIdInNetwork(), initialNodeId, finalNodeId));
        }

        // If everything is OK with the RA, store what should be created
        raBusesToCreate.forEach(busToCreate -> busesToCreate.putIfAbsent(busToCreate.getBusId(), busToCreate));
        switchesToCreatePerRa.get(raId).addAll(raSwitchesToCreate);
        initialNodePerRa.put(raId, initialNodeId);
        finalNodePerRa.put(raId, finalNodeId);
    }

    /**
     * Creates buses needed in busesToCreate
     */
    private void createBuses() {
        busesToCreate.values().stream()
            .sorted(Comparator.comparing(BusToCreate::getBusId))
            .forEach(busToCreate -> createBus(busToCreate.busId, busToCreate.voltageLevelId, busToCreate.referenceBusId));
    }

    /**
     * Creates switches needed in switchesToCreatePerRa
     * Stores info about created switches in createdSwitches, per connected node (initial/final)
     */
    private void createSwitches() {
        switchesToCreatePerRa.values().stream().flatMap(Set::stream)
            .sorted(Comparator.comparing(SwitchPairToCreate::uniqueId))
            .forEach(switchPairToCreate -> {
                if (!createdSwitches.containsKey(switchPairToCreate.uniqueId())) {
                    Pair<String, String> switches = createSwitchPair(switchPairToCreate.branchId, switchPairToCreate.initialNodeId, switchPairToCreate.finalNodeId);
                    createdSwitches.put(switchPairToCreate.uniqueId(),
                        Map.of(switchPairToCreate.initialNodeId, switches.getFirst(),
                            switchPairToCreate.finalNodeId, switches.getSecond()));
                }
            });
    }

    /**
     * Maps info in createdSwitches to needed switches for RAs, and creates a BusBarChangeSwitches set
     */
    private Set<BusBarChangeSwitches> computeBusBarChangeSwitches() {
        Set<BusBarChangeSwitches> busBarChangeSwitches = new HashSet<>();
        initialNodePerRa.keySet().forEach(raId -> {
            Set<String> switchPairsIds = switchesToCreatePerRa.get(raId).stream().map(SwitchPairToCreate::uniqueId).collect(Collectors.toSet());
            List<String> switchesToOpen = switchPairsIds.stream().map(switchPairId ->
                createdSwitches.get(switchPairId).get(initialNodePerRa.get(raId))).collect(Collectors.toList());
            List<String> switchesToClose = switchPairsIds.stream().map(switchPairId ->
                createdSwitches.get(switchPairId).get(finalNodePerRa.get(raId))).collect(Collectors.toList());
            busBarChangeSwitches.add(new BusBarChangeSwitches(raId, switchesToOpen, switchesToClose));
        });
        return busBarChangeSwitches;
    }

    private UcteCnecElementHelper getBranchHelper(String from, String to, String suffix) {
        return new UcteCnecElementHelper(from, to, suffix, ucteNetworkAnalyzer);
    }

    private Bus createBus(String newBusId, String voltageLevelId, String referenceBusId) {
        VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
        Bus referenceBus = (Bus) network.getIdentifiable(referenceBusId);
        Bus newBus = voltageLevel.getBusBreakerView().newBus()
            .setId(newBusId)
            .setFictitious(false)
            .setEnsureIdUnicity(true)
            .add();
        findAndSetGeographicalName(newBus, voltageLevel);
        copyGenerators(referenceBus, newBusId, voltageLevel);
        copyLoads(referenceBus, newBusId, voltageLevel);
        return newBus;
    }

    private static void findAndSetGeographicalName(Bus bus, VoltageLevel voltageLevel) {
        for (Bus otherBus : voltageLevel.getBusBreakerView().getBuses()) {
            if (otherBus.hasProperty(UcteConstants.GEOGRAPHICAL_NAME_PROPERTY_KEY)) {
                bus.setProperty(UcteConstants.GEOGRAPHICAL_NAME_PROPERTY_KEY, otherBus.getProperty(UcteConstants.GEOGRAPHICAL_NAME_PROPERTY_KEY));
                break;
            }
        }
    }

    private static void copyGenerators(Bus busFrom, String busToId, VoltageLevel onVoltageLevel) {
        for (Generator generator : busFrom.getGenerators()) {
            Generator newGenerator = onVoltageLevel.newGenerator()
                .setId(String.format("%s_generator", busToId))
                .setBus(busToId)
                .setMaxP(generator.getMaxP())
                .setMinP(generator.getMinP())
                .setTargetP(0)
                .setTargetQ(0)
                .setRatedS(generator.getRatedS())
                .setVoltageRegulatorOn(generator.isVoltageRegulatorOn())
                .setTargetV(generator.getTargetV())
                .setConnectableBus(busToId)
                .setEnsureIdUnicity(true)
                .add();
            newGenerator.newMinMaxReactiveLimits()
                .setMinQ(generator.getReactiveLimits().getMinQ(generator.getTargetP()))
                .setMaxQ(generator.getReactiveLimits().getMaxQ(generator.getTargetP()))
                .add();
        }

    }

    private static void copyLoads(Bus busFrom, String busToId, VoltageLevel onVoltageLevel) {
        for (Load load : busFrom.getLoads()) {
            onVoltageLevel.newLoad()
                .setId(String.format("%s_load", busToId))
                .setBus(busToId)
                .setP0(0)
                .setQ0(0)
                .setLoadType(load.getLoadType())
                .setEnsureIdUnicity(true)
                .setConnectableBus(busToId)
                .add();
        }
    }

    private static Switch createSwitch(VoltageLevel voltageLevel, String bus1Id, String bus2Id, double currentLimit, boolean open) {
        String switchId = String.format("%s %s 1", bus2Id, bus1Id);
        Switch newSwitch = voltageLevel.getBusBreakerView().newSwitch()
            .setId(switchId)
            .setBus1(bus1Id)
            .setBus2(bus2Id)
            .setOpen(open)
            .setFictitious(false)
            .setEnsureIdUnicity(true)
            .add();
        newSwitch.setProperty(UcteConstants.CURRENT_LIMIT_PROPERTY_KEY, String.valueOf((int) currentLimit));
        return newSwitch;
    }

    /**
     * Creates a switch pair between a branch and two nodes, by creating an intermediary fictitious switch
     * The branch should be initially connected to one of the two nodes
     * The switches are open or closed depending on the initial state of the branch
     *
     * @param branchId: ID of the branch
     * @param node1:    ID of node1
     * @param node2:    ID of node2
     * @return the pair of IDs of the two created switches (switch to node1, switch to node2)
     */
    private Pair<String, String> createSwitchPair(String branchId, String node1, String node2) {
        Line line = (Line) network.getIdentifiable(branchId);
        String bus1 = line.getTerminal1().getBusBreakerView().getConnectableBus().getId();
        String bus2 = line.getTerminal2().getBusBreakerView().getConnectableBus().getId();

        VoltageLevel voltageLevel = ((Bus) network.getIdentifiable(node1)).getVoltageLevel();

        // Create fictitious bus
        String busId = generateFictitiousBusId(voltageLevel);
        Bus fictitiousBus = createBus(busId, voltageLevel.getId(), line.getTerminal1().getBusBreakerView().getConnectableBus().getId());

        // Move one line end to the fictitious bus
        boolean branchIsOnNode1; // check if branch is initially on node1
        if (bus1.equals(node1) || bus1.equals(node2)) {
            moveLine(line, Branch.Side.ONE, fictitiousBus);
            branchIsOnNode1 = bus1.equals(node1);
        } else if (bus2.equals(node1) || bus2.equals(node2)) {
            moveLine(line, Branch.Side.TWO, fictitiousBus);
            branchIsOnNode1 = bus2.equals(node1);
        } else {
            throw new FaraoException(String.format("Neither node1 (%s) nor node2 (%s) belongs to the branch %s. Cannot create switch.", node1, node2, branchId));
        }

        // Create switches
        // Set OPEN/CLOSED status depending on the branch's initially connected node
        double currentLimit = Math.min(line.getCurrentLimits1().getPermanentLimit(), line.getCurrentLimits2().getPermanentLimit());
        String switchOnInitial = createSwitch(voltageLevel, node1, fictitiousBus.getId(), currentLimit, !branchIsOnNode1).getId();
        String switchOnFinal = createSwitch(voltageLevel, node2, fictitiousBus.getId(), currentLimit, branchIsOnNode1).getId();

        return Pair.create(switchOnInitial, switchOnFinal);
    }

    /**
     * Generate a fictitious bus ID for a given voltage level, starting with the "Z" suffix and going back to 9
     */
    private String generateFictitiousBusId(VoltageLevel voltageLevel) {
        char suffix = 'Z';
        String busId;
        do {
            busId = String.format("%s%s", voltageLevel.getId(), suffix);
            suffix -= 1;
        }
        while (network.getIdentifiable(busId) != null && suffix >= '9');
        return busId;
    }

    private static String generateUcteId(String node1, String node2, String suffix) {
        return String.format("%1$8s %2$8s %3$s", node1, node2, suffix);
    }

    private static String getOrderCode(Line line) {
        if (line.hasProperty(UcteConstants.ORDER_CODE)) {
            return line.getProperty(UcteConstants.ORDER_CODE);
        } else {
            return line.getId().substring(line.getId().length() - 1);
        }
    }

    /**
     * Move a line's given side to a given bus
     * The method actually copies the line to a new one then deletes it
     *
     * @param line the line to move
     * @param side the side of the line to move
     * @param bus  the new bus to connect to the side
     * @return the new line
     */
    private Line moveLine(Line line, Branch.Side side, Bus bus) {
        String from = (side == Branch.Side.ONE) ? bus.getId() : line.getTerminal1().getBusBreakerView().getBus().getId();
        String to = (side == Branch.Side.ONE) ? line.getTerminal2().getBusBreakerView().getBus().getId() : bus.getId();
        String newLineId = generateUcteId(from, to, getOrderCode(line));
        LineAdder adder = initializeAdderToMove(line, newLineId);
        if (side == Branch.Side.ONE) {
            setIdenticalToSide(line, Branch.Side.TWO, adder)
                .setConnectableBus1(bus.getId())
                .setBus1(bus.getId())
                .setVoltageLevel1(bus.getVoltageLevel().getId());
        } else if (side == Branch.Side.TWO) {
            setIdenticalToSide(line, Branch.Side.ONE, adder)
                .setConnectableBus2(bus.getId())
                .setBus2(bus.getId())
                .setVoltageLevel2(bus.getVoltageLevel().getId());
        }
        Line newLine = adder.add();
        copyCurrentLimits(line, newLine);
        line.remove();
        return newLine;
    }

    private LineAdder initializeAdderToMove(Line line, String newId) {
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

    private static LineAdder setIdenticalToSide(Line line, Branch.Side side, LineAdder adder) {
        TopologyKind topologyKind = line.getTerminal(side).getVoltageLevel().getTopologyKind();
        if (topologyKind == TopologyKind.BUS_BREAKER) {
            if (side == Branch.Side.ONE) {
                return adder.setVoltageLevel1(line.getTerminal1().getVoltageLevel().getId())
                    .setConnectableBus1(line.getTerminal1().getBusBreakerView().getConnectableBus().getId())
                    .setBus1(line.getTerminal1().getBusBreakerView().getBus() != null ? line.getTerminal1().getBusBreakerView().getBus().getId() : null);
            } else if (side == Branch.Side.TWO) {
                return adder.setVoltageLevel2(line.getTerminal2().getVoltageLevel().getId())
                    .setConnectableBus2(line.getTerminal2().getBusBreakerView().getConnectableBus().getId())
                    .setBus2(line.getTerminal2().getBusBreakerView().getBus() != null ? line.getTerminal2().getBusBreakerView().getBus().getId() : null);
            }
        }
        throw new AssertionError();
    }

    private static void copyCurrentLimits(Line lineFrom, Line lineTo) {
        if (lineFrom.getCurrentLimits1() != null) {
            lineTo.newCurrentLimits1()
                .setPermanentLimit(lineFrom.getCurrentLimits1().getPermanentLimit())
                .add();
        }
        if (lineFrom.getCurrentLimits2() != null) {
            lineTo.newCurrentLimits2()
                .setPermanentLimit(lineFrom.getCurrentLimits2().getPermanentLimit())
                .add();
        }
    }

    /**
     * Contains needed info to create a bus
     */
    private static class BusToCreate {
        String busId; // ID of the bus to create
        String voltageLevelId; // ID of the voltage level to create the bus upon
        String referenceBusId; // ID of the reference bus

        BusToCreate(String busId, String voltageLevelId, String referenceBusId) {
            this.busId = busId;
            this.voltageLevelId = voltageLevelId;
            this.referenceBusId = referenceBusId;
        }

        String getBusId() {
            return busId;
        }
    }

    /**
     * Contains needed info to create a pair of switches
     */
    private static class SwitchPairToCreate {
        String branchId; // ID of the branch in the network
        String initialNodeId; // ID of the initial node to connect the switch to
        String finalNodeId; // ID of the final node to connect the switch to

        SwitchPairToCreate(String branchId, String initialNodeId, String finalNodeId) {
            this.branchId = branchId;
            this.initialNodeId = initialNodeId;
            this.finalNodeId = finalNodeId;
        }

        String uniqueId() {
            if (initialNodeId.compareTo(finalNodeId) < 0) {
                return String.format("%s - %s - %s", branchId, initialNodeId, finalNodeId);
            } else {
                return String.format("%s - %s - %s", branchId, finalNodeId, initialNodeId);
            }
        }
    }

}
