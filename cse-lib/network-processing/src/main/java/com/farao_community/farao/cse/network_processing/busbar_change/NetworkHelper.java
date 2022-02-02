/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing.busbar_change;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteCnecElementHelper;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A utility class to help querying the network
 *
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public final class NetworkHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkHelper.class);

    private NetworkHelper() {
      // utility class
    }

    /**
     * Returns the line ID in the network, for a branch affected by a busbar change.
     * Tries all possible combinations that are coherent with the bus bar change RA.
     * @param fromNodeId: the "from" node of the branch
     * @param toNodeId: the "to" node of the branch
     * @param suffix: the suffix or order code of the branch
     * @param initialNodeId: the initial node ID of the bus bar change RA
     * @param finalNodeId: the final node ID of the bus bar change RA
     * @param ucteNetworkAnalyzer: the UCTE network analyzer
     * @return the ID of the line in the network if it is found
     * @throws FaraoException if the line is not found
     */
    public static String getLineIdInNetwork(String fromNodeId, String toNodeId, String suffix, String initialNodeId, String finalNodeId, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        Network network = ucteNetworkAnalyzer.getNetwork();
        String triedIds = "tried IDs: ";
        // Try to get from->to line
        UcteCnecElementHelper branchHelper = getBranchHelper(fromNodeId, toNodeId, suffix, ucteNetworkAnalyzer);
        triedIds += branchHelper.getUcteId();
        // Line may be already on final node, try from/to->final node
        if (!branchHelper.isValid()) {
            branchHelper = getBranchHelper(fromNodeId, finalNodeId, suffix, ucteNetworkAnalyzer);
            triedIds += ", " + branchHelper.getUcteId();
        }
        if (!branchHelper.isValid()) {
            branchHelper = getBranchHelper(toNodeId, finalNodeId, suffix, ucteNetworkAnalyzer);
            triedIds += ", " + branchHelper.getUcteId();
        }
        if (!branchHelper.isValid()) {
            throw new FaraoException(String.format("One of the branches in the remedial action was not found in the network (%s)", triedIds));
        } else if (!(network.getIdentifiable(branchHelper.getIdInNetwork()) instanceof Line) && !(network.getIdentifiable(branchHelper.getIdInNetwork()) instanceof TwoWindingsTransformer)) {
            throw new FaraoException(String.format("One of the branches (%s) in the remedial action is neither a line nor a two-windings-transformer: %s", branchHelper.getIdInNetwork(), network.getIdentifiable(branchHelper.getIdInNetwork()).getClass()));
        }
        if (!isBranchConnected(branchHelper.getIdInNetwork(), initialNodeId, network) && !isBranchConnected(branchHelper.getIdInNetwork(), finalNodeId, network)) {
            throw new FaraoException(String.format("Branch %s is neither connected to initial node (%s) nor to final node (%s)", branchHelper.getIdInNetwork(), initialNodeId, finalNodeId));
        }
        return branchHelper.getIdInNetwork();
    }

    private static UcteCnecElementHelper getBranchHelper(String from, String to, String suffix, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        return new UcteCnecElementHelper(from, to, suffix, ucteNetworkAnalyzer);
    }

    /**
     * Returns true if a line is connected to a given node
     */
    private static boolean isBranchConnected(String branchId, String nodeId, Network network) {
        Branch<?> branch = network.getBranch(branchId);
        String bus1 = branch.getTerminal1().getBusBreakerView().getConnectableBus().getId();
        String bus2 = branch.getTerminal2().getBusBreakerView().getConnectableBus().getId();
        return bus1.equals(nodeId) || bus2.equals(nodeId);
    }

    /**
     * Creates a switch pair between a branch and two nodes, by creating an intermediary fictitious switch
     * The branch should be initially connected to one of the two nodes
     * The switches are open or closed depending on the initial state of the branch
     *
     * @param switchPairToCreate: object containing ID of the branch, ID of node1, ID of node2
     * @param  networkModifier: object that modifies a network
     * @return a map, mapping initial and final node IDs to the two created switches' IDs
     */
    public static Map<String, String> createSwitchPair(SwitchPairToCreate switchPairToCreate, NetworkModifier networkModifier) {
        LOGGER.debug("Creating switch pair: {}", switchPairToCreate.uniqueId);
        String node1 = switchPairToCreate.initialNodeId;
        String node2 = switchPairToCreate.finalNodeId;
        Network network = networkModifier.getNetwork();
        Branch<?> branch = network.getBranch(switchPairToCreate.branchId);
        String bus1 = branch.getTerminal1().getBusBreakerView().getConnectableBus().getId();
        String bus2 = branch.getTerminal2().getBusBreakerView().getConnectableBus().getId();

        VoltageLevel voltageLevel = ((Bus) network.getIdentifiable(node1)).getVoltageLevel();

        // Create fictitious bus
        String busId = generateFictitiousBusId(voltageLevel, network);
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
        String switch1 = networkModifier.createSwitch(voltageLevel, node1, fictitiousBus.getId(), currentLimit, !branchIsOnNode1).getId();
        String switch2 = networkModifier.createSwitch(voltageLevel, node2, fictitiousBus.getId(), currentLimit, branchIsOnNode1).getId();

        return Map.of(node1, switch1, node2, switch2);
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
    private static String generateFictitiousBusId(VoltageLevel voltageLevel, Network network) {
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
     * Contains needed info to create a pair of switches
     */
    public static class SwitchPairToCreate implements Comparable<SwitchPairToCreate> {
        String branchId; // ID of the branch in the network
        String initialNodeId; // ID of the initial node to connect the switch to
        String finalNodeId; // ID of the final node to connect the switch to
        String uniqueId;

        SwitchPairToCreate(String branchId, String initialNodeId, String finalNodeId) {
            this.branchId = branchId;
            this.initialNodeId = initialNodeId;
            this.finalNodeId = finalNodeId;
            // Unique ID guarantees that at most 1 pair of switches is asked to be created,
            // for a given branch and given initial & final nodes (even if they are inverted)
            // This allows two busbar RAs, that are opposite to one another, to use the same pair of switches
            if (initialNodeId.compareTo(finalNodeId) < 0) {
                this.uniqueId = String.format("%s {%s, %s}", branchId, initialNodeId, finalNodeId);
            } else {
                this.uniqueId = String.format("%s {%s, %s}", branchId, finalNodeId, initialNodeId);
            }
        }

        String uniqueId() {
            return uniqueId;
        }

        @Override
        public int hashCode() {
            return branchId.hashCode() + 31 * (initialNodeId.hashCode() + finalNodeId.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SwitchPairToCreate other = (SwitchPairToCreate) obj;
            return this.branchId.equals(other.branchId) &&
                ((this.initialNodeId.equals(other.initialNodeId) && this.finalNodeId.equals(other.finalNodeId)) ||
                    (this.initialNodeId.equals(other.finalNodeId) && this.finalNodeId.equals(other.initialNodeId)));
        }

        @Override
        public int compareTo(SwitchPairToCreate other) {
            return uniqueId.compareTo(other.uniqueId);
        }
    }
}
