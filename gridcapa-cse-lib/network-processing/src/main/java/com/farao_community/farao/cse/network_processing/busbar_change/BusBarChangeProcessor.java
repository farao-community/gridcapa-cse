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
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.xml.XMLExporter;
import com.powsybl.iidm.xml.XMLImporter;
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

/**
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public class BusBarChangeProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusBarChangeProcessor.class);

    private Network originalNetwork;
    private Network modifiedNetwork;
    private UcteNetworkAnalyzer originalNetworkAnalyzer;
    private UcteNetworkAnalyzer modifiedNetworkAnalyzer;
    private Map<String, String> createdSwitches;

    public BusBarChangeProcessor() {
        createdSwitches = new HashMap<>();
    }

    private static Network copyUcteNetwork(Network network) {
        MemDataSource dataSource = new MemDataSource();
        new XMLExporter().export(network, new Properties(), dataSource);
        return new XMLImporter().importData(dataSource, NetworkFactory.findDefault(), new Properties());
    }

    public Set<BusBarChangeSwitches> process(Network modifiedNetwork, Network originalNetwork, InputStream cracInputStream) {
        // TODO : copy original network
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(cracInputStream);
        this.originalNetwork = originalNetwork; // we have to keep a copy in case several RAs share branches
        this.modifiedNetwork = modifiedNetwork;
        originalNetworkAnalyzer = new UcteNetworkAnalyzer(originalNetwork, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        modifiedNetworkAnalyzer = new UcteNetworkAnalyzer(modifiedNetwork, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));

        TCRACSeries tcracSeries = CseCracCreator.getCracSeries(cseCrac.getCracDocument());
        List<TRemedialActions> tRemedialActionsList = tcracSeries.getRemedialActions();
        Set<BusBarChangeSwitches> busBarChangeSwitchesSet = new HashSet<>();
        for (TRemedialActions tRemedialActions : tRemedialActionsList) {
            if (tRemedialActions != null) {
                tRemedialActions.getRemedialAction().stream()
                    .filter(tRemedialAction -> tRemedialAction.getBusBar() != null)
                    .forEach(tRemedialAction -> {
                        try {
                            busBarChangeSwitchesSet.add(createSwitches(tRemedialAction));
                        } catch (FaraoException e) {
                            LOGGER.warn("RA {} has been skipped: {}", tRemedialAction.getName().getV(), e.getMessage());
                        }
                    }
                );
            }
        }
        return busBarChangeSwitchesSet;
    }

    private void updateModifiedNetworkAnalyzer() {
        modifiedNetworkAnalyzer = new UcteNetworkAnalyzer(modifiedNetwork, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
    }

    /**
     * Create fictitious switches for a given BusBar change remedial action
     * For every branch in the RA, this method creates two switches (open or closed initially depending on initial network)
     * for initial and final node, and a fictitious intermediary bus
     *
     * @param tRemedialAction: the native remedial action in the CSE CRAC file
     * @return a {@link BusBarChangeSwitches} object mapping the IDs of created switches that should be open or closed by the remedial action
     */
    private BusBarChangeSwitches createSwitches(TRemedialAction tRemedialAction) {
        String raId = tRemedialAction.getName().getV();

        // Get initial and final nodes
        UcteBusHelper initialNodeHelper = new UcteBusHelper(tRemedialAction.getBusBar().getInitialNode().getV(), modifiedNetworkAnalyzer);
        UcteBusHelper finalNodeHelper = new UcteBusHelper(tRemedialAction.getBusBar().getFinalNode().getV(), modifiedNetworkAnalyzer);

        String initialNodeId;
        String finalNodeId;
        Bus referenceBus;
        VoltageLevel voltageLevel;
        boolean shouldCreateInitialNode = false;
        boolean shouldCreateFinalNode = false;

        if (initialNodeHelper.isValid() && finalNodeHelper.isValid()) {
            initialNodeId = initialNodeHelper.getIdInNetwork();
            finalNodeId = finalNodeHelper.getIdInNetwork();
            referenceBus = (Bus) modifiedNetwork.getIdentifiable(initialNodeHelper.getIdInNetwork());
            voltageLevel = referenceBus.getVoltageLevel();
        } else if (initialNodeHelper.isValid() && !finalNodeHelper.isValid()) {
            initialNodeId = initialNodeHelper.getIdInNetwork();
            referenceBus = (Bus) modifiedNetwork.getIdentifiable(initialNodeId);
            voltageLevel = referenceBus.getVoltageLevel();
            finalNodeId = tRemedialAction.getBusBar().getFinalNode().getV();
            shouldCreateFinalNode = true;
        } else if (!initialNodeHelper.isValid() && finalNodeHelper.isValid()) {
            finalNodeId = finalNodeHelper.getIdInNetwork();
            referenceBus = (Bus) modifiedNetwork.getIdentifiable(finalNodeId);
            voltageLevel = referenceBus.getVoltageLevel();
            initialNodeId = tRemedialAction.getBusBar().getInitialNode().getV();
            shouldCreateInitialNode = true;
        } else {
            throw new FaraoException(String.format("Remedial action's initial and final nodes are not valid: %s", initialNodeHelper.getInvalidReason()));
        }

        // Get all branches
        Set<String> branches = new HashSet<>();
        for (TBranch tBranch : tRemedialAction.getBusBar().getBranch()) {
            String suffix =  String.valueOf(tBranch.getOrder().getV());
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
            } else if (!(originalNetwork.getIdentifiable(branchHelper.getIdInNetwork()) instanceof Line)) {
                throw new FaraoException(String.format("One of the branches (%s) in the remedial action is not a line: %s", branchHelper.getIdInNetwork(), modifiedNetwork.getIdentifiable(branchHelper.getIdInNetwork()).getClass()));
            }
            branches.add(branchHelper.getIdInNetwork());
        }

        if (shouldCreateInitialNode) {
            createBus(initialNodeId, voltageLevel, referenceBus);
            updateModifiedNetworkAnalyzer();
        }
        if (shouldCreateFinalNode) {
            createBus(finalNodeId, voltageLevel, referenceBus);
            updateModifiedNetworkAnalyzer();
        }

        List<String> switchesToOpen = new ArrayList<>();
        List<String> switchesToClose = new ArrayList<>();

        branches.forEach(branchId -> {
            Pair<String, String> switches = createSwitches(branchId, initialNodeId, finalNodeId, voltageLevel);
            switchesToOpen.add(switches.getFirst());
            switchesToClose.add(switches.getSecond());
        });

        return new BusBarChangeSwitches(raId, switchesToOpen, switchesToClose);
    }

    private UcteCnecElementHelper getBranchHelper(String from, String to, String suffix) {
        return new UcteCnecElementHelper(from, to, suffix, originalNetworkAnalyzer);
    }

    private static Bus createBus(String newBusId, VoltageLevel voltageLevel, Bus baseBus) {
        Bus newBus = voltageLevel.getBusBreakerView().newBus()
            .setId(newBusId)
            .setFictitious(false)
            .setEnsureIdUnicity(true)
            .add();
        findAndSetGeographicalName(newBus, voltageLevel);
        copyGenerators(baseBus, newBusId, voltageLevel);
        copyLoads(baseBus, newBusId, voltageLevel);
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

    private String getCreatedSwitchKey(String branchId, String node) {
        return branchId + " - " + node;
    }

    private Pair<String, String> createSwitches(String branchId, String initialNode, String finalNode, VoltageLevel voltageLevel) {
        String initialKey = getCreatedSwitchKey(branchId, initialNode);
        String finalKey = getCreatedSwitchKey(branchId, finalNode);

        // Check if switches have been created fo other RAs before
        if (createdSwitches.containsKey(initialKey) && createdSwitches.containsKey(finalKey)) {
            return Pair.create(createdSwitches.get(initialKey),  createdSwitches.get(finalKey));
        } else if ((createdSwitches.containsKey(initialKey) && !createdSwitches.containsKey(finalKey))
            || (!createdSwitches.containsKey(initialKey) && createdSwitches.containsKey(finalKey))) {
            throw new FaraoException("3-node cases are not handled for now");
        }

        // Else create them
        Line line = (Line) modifiedNetwork.getIdentifiable(branchId);
        String bus1 = line.getTerminal1().getBusBreakerView().getConnectableBus().getId();
        String bus2 = line.getTerminal2().getBusBreakerView().getConnectableBus().getId();

        // Create fictitious bus
        String busId = generateFictitiousBusId(voltageLevel);
        Bus fictitiousBus = createBus(busId, voltageLevel, line.getTerminal1().getBusBreakerView().getConnectableBus());

        // Move one line end to the fictitious bus
        boolean raIsInverted; // check if branch is already on final node
        if (bus1.equals(initialNode) || bus1.equals(finalNode)) {
            moveLine(line, fictitiousBus, Branch.Side.ONE);
            raIsInverted = bus1.equals(finalNode);
        } else if (bus2.equals(initialNode) || bus2.equals(finalNode)) {
            moveLine(line, fictitiousBus, Branch.Side.TWO);
            raIsInverted = bus2.equals(finalNode);
        } else {
            throw new FaraoException(String.format("Neither initial node (%s) nor final node (%s) belongs to the branch %s", initialNode, finalNode, branchId));
        }

        // Create switches
        // If branch is initially on the final node (raIsInverted = false):
        // - create a closed switch between fictitious bus and initial node
        // - create an open switch between fictitious bus and final node
        // If branch is initially on the final node (raIsInverted = true):
        // - create an open switch between fictitious bus and initial node
        // - create a closed switch between fictitious bus and final node
        double currentLimit = Math.min(line.getCurrentLimits1().getPermanentLimit(), line.getCurrentLimits2().getPermanentLimit());
        String switchToOpen = createSwitch(voltageLevel, initialNode, fictitiousBus.getId(), currentLimit, raIsInverted).getId();
        String switchToClose = createSwitch(voltageLevel, finalNode, fictitiousBus.getId(), currentLimit, !raIsInverted).getId();

        // Put them in map for RAs coming next
        createdSwitches.put(initialKey, switchToOpen);
        createdSwitches.put(finalKey, switchToClose);

        return Pair.create(switchToOpen, switchToClose);
    }

    private String generateFictitiousBusId(VoltageLevel voltageLevel) {
        char suffix = 'Z';
        String busId;
        do {
            busId = String.format("%s%s", voltageLevel.getId(), suffix);
            suffix -= 1;
        }
        while (modifiedNetwork.getIdentifiable(busId) != null && suffix >= '9');
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

    private Line moveLine(Line line, Bus bus, Branch.Side side) {
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
        return modifiedNetwork.newLine()
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

}
