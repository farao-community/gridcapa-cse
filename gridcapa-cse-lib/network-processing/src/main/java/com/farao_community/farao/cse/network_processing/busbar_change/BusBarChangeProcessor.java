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
                            System.out.println(String.format("RA %s has been skipped: %s", tRemedialAction.getName().getV(), e.getMessage()));
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
     * - switches, to be able to move the line from the initial to the final node (info stored in switchesToCreatePerRa)
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
            String triedIds = "tried IDs: ";
            // Try to get from->to line
            UcteCnecElementHelper branchHelper = getBranchHelper(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), suffix);
            triedIds += branchHelper.getUcteId();
            // Line may be already on final node, try from/to->final node
            if (!branchHelper.isValid()) {
                branchHelper = getBranchHelper(tBranch.getFromNode().getV(), finalNodeId, suffix);
                triedIds += ", " + branchHelper.getUcteId();
            }
            if (!branchHelper.isValid()) {
                branchHelper = getBranchHelper(tBranch.getToNode().getV(), finalNodeId, suffix);
                triedIds += ", " + branchHelper.getUcteId();
            }
            if (!branchHelper.isValid()) {
                throw new FaraoException(String.format("One of the branches in the remedial action was not found in the network (%s)", triedIds));
            } else if (!(network.getIdentifiable(branchHelper.getIdInNetwork()) instanceof Line) && !(network.getIdentifiable(branchHelper.getIdInNetwork()) instanceof TwoWindingsTransformer)) {
                throw new FaraoException(String.format("One of the branches (%s) in the remedial action is neither a line nor a two-windings-transformer: %s", branchHelper.getIdInNetwork(), network.getIdentifiable(branchHelper.getIdInNetwork()).getClass()));
            }
            if (!isBranchConnected(branchHelper.getIdInNetwork(), initialNodeId) && !isBranchConnected(branchHelper.getIdInNetwork(), finalNodeId)) {
                throw new FaraoException(String.format("Branch %s is neither connected to initial node (%s) nor to final node (%s)", branchHelper.getIdInNetwork(), initialNodeId, finalNodeId));
            }
            SwitchPairToCreate switchPairToCreate = new SwitchPairToCreate(branchHelper.getIdInNetwork(), initialNodeId, finalNodeId);
            // Throw an exception if a 3-node case is detected with another remedial action
            String otherRa = switchesToCreatePerRa.keySet().stream().filter(ra -> switchesToCreatePerRa.get(ra).stream().anyMatch(switchPairToCreate::generates3NodeCase)).findAny().orElse(null);
            if (otherRa != null) {
                throw new FaraoException(String.format("Branch %s is also used in another BusBar RemedialAction (%s) but with different initial/final nodes; this is not yet handled", branchHelper.getIdInNetwork(), otherRa));
            }
            raSwitchesToCreate.add(switchPairToCreate);
        }

        // If everything is OK with the RA, store what should be created
        raBusesToCreate.forEach(busToCreate -> busesToCreate.putIfAbsent(busToCreate.getBusId(), busToCreate));
        switchesToCreatePerRa.get(raId).addAll(raSwitchesToCreate);
        initialNodePerRa.put(raId, initialNodeId);
        finalNodePerRa.put(raId, finalNodeId);
    }

    /**
     * Returns true if a line is connected to a given node
     */
    private boolean isBranchConnected(String branchId, String nodeId) {
        Branch<?> branch = network.getBranch(branchId);
        String bus1 = branch.getTerminal1().getBusBreakerView().getConnectableBus().getId();
        String bus2 = branch.getTerminal2().getBusBreakerView().getConnectableBus().getId();
        return bus1.equals(nodeId) || bus2.equals(nodeId);
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
                    Pair<String, String> switches = createSwitchPair(switchPairToCreate.lineId, switchPairToCreate.initialNodeId, switchPairToCreate.finalNodeId);
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
        // TODO : is copying loads & generators necessary?
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

    private static Switch createSwitch(VoltageLevel voltageLevel, String bus1Id, String bus2Id, Double currentLimit, boolean open) {
        String switchId = String.format("%s %s 1", bus2Id, bus1Id);
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
        Branch<?> branch = network.getBranch(branchId);
        String bus1 = branch.getTerminal1().getBusBreakerView().getConnectableBus().getId();
        String bus2 = branch.getTerminal2().getBusBreakerView().getConnectableBus().getId();

        VoltageLevel voltageLevel = ((Bus) network.getIdentifiable(node1)).getVoltageLevel();

        // Create fictitious bus
        String busId = generateFictitiousBusId(voltageLevel);
        Bus fictitiousBus = createBus(busId, voltageLevel.getId(), branch.getTerminal1().getBusBreakerView().getConnectableBus().getId());

        // Move one branch end to the fictitious bus
        boolean branchIsOnNode1 = true; // check if branch is initially on node1
        if (bus1.equals(node1) || bus1.equals(node2)) {
            moveBranch(branch, Branch.Side.ONE, fictitiousBus);
            branchIsOnNode1 = bus1.equals(node1);
        } else if (bus2.equals(node1) || bus2.equals(node2)) {
            moveBranch(branch, Branch.Side.TWO, fictitiousBus);
            branchIsOnNode1 = bus2.equals(node1);
        }
        // else: should not happen, a check was done before

        // Create switches
        // Set OPEN/CLOSED status depending on the branch's initially connected node
        Double currentLimit = getMinimumCurrentLimit(branch);
        String switchOnInitial = createSwitch(voltageLevel, node1, fictitiousBus.getId(), currentLimit, !branchIsOnNode1).getId();
        String switchOnFinal = createSwitch(voltageLevel, node2, fictitiousBus.getId(), currentLimit, branchIsOnNode1).getId();

        return Pair.create(switchOnInitial, switchOnFinal);
    }

    private static Double getMinimumCurrentLimit(Branch<?> branch) {
        if (branch.getCurrentLimits1() != null && branch.getCurrentLimits2() != null) {
            return Math.min(branch.getCurrentLimits1().getPermanentLimit(), branch.getCurrentLimits2().getPermanentLimit());
        } else if (branch.getCurrentLimits1() != null) {
            return branch.getCurrentLimits1().getPermanentLimit();
        } else if (branch.getCurrentLimits2() != null) {
            return branch.getCurrentLimits2().getPermanentLimit();
        } else {
            return null;
        }
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

    private static String getOrderCode(Branch<?> branch) {
        if (branch.hasProperty(UcteConstants.ORDER_CODE)) {
            return branch.getProperty(UcteConstants.ORDER_CODE);
        } else {
            return branch.getId().substring(branch.getId().length() - 1);
        }
    }

    /**
     * Move a branch's given side to a given bus
     * (The method actually copies the branch to a new one then deletes it)
     *
     * @param branch the branch to move
     * @param side the side of the branch to move
     * @param bus  the new bus to connect to the side
     * @return the new line
     */
    private Branch<?> moveBranch(Branch<?> branch, Branch.Side side, Bus bus) {
        if (branch instanceof TwoWindingsTransformer) {
            return moveTwoWindingsTransformer((TwoWindingsTransformer) branch, side, bus);
        } else if (branch instanceof TieLine) {
            return moveTieLine((TieLine) branch, side, bus);
        } else if (branch instanceof Line) {
            return moveLine((Line) branch, side, bus);
        } else {
            throw new FaraoException(String.format("Cannot move %s of type %s", branch.getId(), branch.getClass()));
        }
    }

    private TwoWindingsTransformer moveTwoWindingsTransformer(TwoWindingsTransformer twoWindingsTransformer, Branch.Side side, Bus bus) {
        String newId = replaceSimpleBranchNode(twoWindingsTransformer, side, bus.getId());
        TwoWindingsTransformerAdder adder = initializeTwoWindingsTransformerAdderToMove(twoWindingsTransformer, newId);
        setBranchAdderProperties(adder, twoWindingsTransformer, side, bus);
        TwoWindingsTransformer newTransformer = adder.add();
        copyCurrentLimits(twoWindingsTransformer, newTransformer);
        copyProperties(twoWindingsTransformer, newTransformer);
        copyTapChanger(twoWindingsTransformer, newTransformer);
        twoWindingsTransformer.remove();
        return newTransformer;
    }

    private Line moveTieLine(TieLine tieLine, Branch.Side side, Bus bus) {
        String newLineId = replaceTieLineNode(tieLine, side, bus.getId());
        TieLineAdder adder = initializeTieLineAdderToMove(tieLine, newLineId, side, bus);
        setBranchAdderProperties(adder, tieLine, side, bus);
        TieLine newTieLine = adder.add();
        copyCurrentLimits(tieLine, newTieLine);
        copyProperties(tieLine, newTieLine);
        tieLine.remove();
        return newTieLine;
    }

    private Line moveLine(Line line, Branch.Side side, Bus bus) {
        String newLineId = replaceSimpleBranchNode(line, side, bus.getId());
        LineAdder adder = initializeLineAdderToMove(line, newLineId);
        setBranchAdderProperties(adder, line, side, bus);
        Line newLine = adder.add();
        copyCurrentLimits(line, newLine);
        copyProperties(line, newLine);
        line.remove();
        return newLine;
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
        pst.getAllSteps().values().forEach(step ->
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

    private static BranchAdder<?> setIdenticalToSide(Branch<?> branch, Branch.Side side, BranchAdder<?> adder) {
        TopologyKind topologyKind = branch.getTerminal(side).getVoltageLevel().getTopologyKind();
        if (topologyKind == TopologyKind.BUS_BREAKER) {
            if (side == Branch.Side.ONE) {
                return adder.setVoltageLevel1(branch.getTerminal1().getVoltageLevel().getId())
                    .setConnectableBus1(branch.getTerminal1().getBusBreakerView().getConnectableBus().getId())
                    .setBus1(branch.getTerminal1().getBusBreakerView().getBus() != null ? branch.getTerminal1().getBusBreakerView().getBus().getId() : null);
            } else if (side == Branch.Side.TWO) {
                return adder.setVoltageLevel2(branch.getTerminal2().getVoltageLevel().getId())
                    .setConnectableBus2(branch.getTerminal2().getBusBreakerView().getConnectableBus().getId())
                    .setBus2(branch.getTerminal2().getBusBreakerView().getBus() != null ? branch.getTerminal2().getBusBreakerView().getBus().getId() : null);
            }
        }
        throw new AssertionError();
    }

    private void setBranchAdderProperties(BranchAdder<?> adder, Branch<?> branchToCopy, Branch.Side sideToUpdate, Bus busToUpdate) {
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
        String from = (side == Branch.Side.ONE) ? newNodeId : branch.getTerminal1().getBusBreakerView().getBus().getId();
        String to = (side == Branch.Side.ONE) ? branch.getTerminal2().getBusBreakerView().getBus().getId() : newNodeId;
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
        String newHalf1Id = (sideToUpdate == Branch.Side.ONE) ? replaceHalfLineNode(tieLine, Branch.Side.ONE, bus.getId()): half1.getId();
        String newHalf2Id = (sideToUpdate == Branch.Side.TWO) ? replaceHalfLineNode(tieLine, Branch.Side.TWO, bus.getId()): half2.getId();
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
        return twoWindingsTransformer.getSubstation().newTwoWindingsTransformer()
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
        if (branchFrom.getCurrentLimits1() != null) {
            branchTo.newCurrentLimits1()
                .setPermanentLimit(branchFrom.getCurrentLimits1().getPermanentLimit())
                .add();
        }
        if (branchFrom.getCurrentLimits2() != null) {
            branchTo.newCurrentLimits2()
                .setPermanentLimit(branchFrom.getCurrentLimits2().getPermanentLimit())
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
        String lineId; // ID of the line in the network
        String initialNodeId; // ID of the initial node to connect the switch to
        String finalNodeId; // ID of the final node to connect the switch to

        SwitchPairToCreate(String lineId, String initialNodeId, String finalNodeId) {
            this.lineId = lineId;
            this.initialNodeId = initialNodeId;
            this.finalNodeId = finalNodeId;
        }

        String uniqueId() {
            if (initialNodeId.compareTo(finalNodeId) < 0) {
                return String.format("%s - %s - %s", lineId, initialNodeId, finalNodeId);
            } else {
                return String.format("%s - %s - %s", lineId, finalNodeId, initialNodeId);
            }
        }

        /**
         * Returns true if 2 switch pairs to create operate on the same branch
         * but on different initial & final nodes
         */
        boolean generates3NodeCase(SwitchPairToCreate otherPair) {
            return this.lineId.equals(otherPair.lineId) && !this.uniqueId().equals(otherPair.uniqueId());
        }
    }

}
