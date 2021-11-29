/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing.busbar_change;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreator;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracImporter;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.BusBarChangeSwitches;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TBranch;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TCRACSeries;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TRemedialAction;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TRemedialActions;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteBusHelper;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteCnecElementHelper;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzerProperties;
import com.powsybl.iidm.network.*;
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
    private NetworkModifier networkModifier;

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
        this.ucteNetworkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        this.networkModifier = new NetworkModifier(network);

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
        networkModifier.commitAllChanges();
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
            raBusesToCreate.add(new BusToCreate(finalNodeId, referenceBus.getVoltageLevel().getId()));
        } else if (!initialNodeHelper.isValid() && finalNodeHelper.isValid()) {
            finalNodeId = finalNodeHelper.getIdInNetwork();
            referenceBus = (Bus) network.getIdentifiable(finalNodeId);
            initialNodeId = tRemedialAction.getBusBar().getInitialNode().getV();
            raBusesToCreate.add(new BusToCreate(initialNodeId, referenceBus.getVoltageLevel().getId()));
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
            .forEach(busToCreate -> networkModifier.createBus(busToCreate.busId, busToCreate.voltageLevelId));
    }

    /**
     * Creates switches needed in switchesToCreatePerRa
     * Stores info about created switches in createdSwitches, per connected node (initial/final)
     */
    private void createSwitches() {
        switchesToCreatePerRa.values().stream().flatMap(Set::stream)
            .sorted(Comparator.comparing(SwitchPairToCreate::uniqueId))
            .forEach(switchPairToCreate -> {
                if (!createdSwitches.containsKey(switchPairToCreate.uniqueId)) {
                    Pair<String, String> switches = createSwitchPair(switchPairToCreate);
                    createdSwitches.put(switchPairToCreate.uniqueId,
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

    /**
     * Creates a switch pair between a branch and two nodes, by creating an intermediary fictitious switch
     * The branch should be initially connected to one of the two nodes
     * The switches are open or closed depending on the initial state of the branch
     *
     * @param switchPairToCreate: object containing ID of the branch, ID of node1, ID of node2
     * @return the pair of IDs of the two created switches (switch to node1, switch to node2)
     */
    private Pair<String, String> createSwitchPair(SwitchPairToCreate switchPairToCreate) {
        LOGGER.debug("Creating switch pair: {}", switchPairToCreate.uniqueId);
        String node1 = switchPairToCreate.initialNodeId;
        String node2 = switchPairToCreate.finalNodeId;
        Branch<?> branch = network.getBranch(switchPairToCreate.branchId);
        String bus1 = branch.getTerminal1().getBusBreakerView().getConnectableBus().getId();
        String bus2 = branch.getTerminal2().getBusBreakerView().getConnectableBus().getId();

        VoltageLevel voltageLevel = ((Bus) network.getIdentifiable(node1)).getVoltageLevel();

        // Create fictitious bus
        String busId = generateFictitiousBusId(voltageLevel);
        Bus fictitiousBus = networkModifier.createBus(busId, voltageLevel.getId());

        // Move one branch end to the fictitious bus
        boolean branchIsOnNode1 = true; // check if branch is initially on node1
        if (bus1.equals(node1) || bus1.equals(node2)) {
            networkModifier.moveBranch(branch, Branch.Side.ONE, fictitiousBus);
            branchIsOnNode1 = bus1.equals(node1);
        } else if (bus2.equals(node1) || bus2.equals(node2)) {
            networkModifier.moveBranch(branch, Branch.Side.TWO, fictitiousBus);
            branchIsOnNode1 = bus2.equals(node1);
        }
        // else: should not happen, a check was done before

        // Create switches
        // Set OPEN/CLOSED status depending on the branch's initially connected node
        Double currentLimit = getMinimumCurrentLimit(branch);
        String switchOnInitial = networkModifier.createSwitch(voltageLevel, node1, fictitiousBus.getId(), currentLimit, !branchIsOnNode1).getId();
        String switchOnFinal = networkModifier.createSwitch(voltageLevel, node2, fictitiousBus.getId(), currentLimit, branchIsOnNode1).getId();

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
     * This is important, we need to keep the same first 7 letters in order to match critical branch names later if needed
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

    /**
     * Contains needed info to create a bus
     */
    private static class BusToCreate {
        String busId; // ID of the bus to create
        String voltageLevelId; // ID of the voltage level to create the bus upon

        BusToCreate(String busId, String voltageLevelId) {
            this.busId = busId;
            this.voltageLevelId = voltageLevelId;
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
        String uniqueId;

        SwitchPairToCreate(String branchId, String initialNodeId, String finalNodeId) {
            this.branchId = branchId;
            this.initialNodeId = initialNodeId;
            this.finalNodeId = finalNodeId;
            if (initialNodeId.compareTo(finalNodeId) < 0) {
                this.uniqueId =  String.format("%s {%s, %s}", branchId, initialNodeId, finalNodeId);
            } else {
                this.uniqueId =  String.format("%s {%s, %s}", branchId, finalNodeId, initialNodeId);
            }
        }

        String uniqueId() {
            return uniqueId;
        }
    }

}
