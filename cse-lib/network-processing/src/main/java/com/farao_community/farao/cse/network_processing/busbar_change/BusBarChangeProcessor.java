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
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.SwitchPairId;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TCRACSeries;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TRemedialAction;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TRemedialActions;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteBusHelper;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzerProperties;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.cse.network_processing.busbar_change.NetworkHelper.SwitchPairToCreate;

/**
 * This class processes a CSE crac file in order to create, in the network, switches that can be used to apply
 * BusBar remedial actions
 *
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public final class BusBarChangeProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusBarChangeProcessor.class);

    private BusBarChangeProcessor() {
        // utility class
    }

    public static Set<BusBarChangeSwitches> process(Network network, InputStream cracInputStream) {
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(cracInputStream);
        return process(network, cseCrac);
    }

    public static Set<BusBarChangeSwitches> process(Network network, CseCrac cseCrac) {
        UcteNetworkAnalyzer ucteNetworkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
        NetworkModifier networkModifier = new NetworkModifier(network);

        Map<String, Set<SwitchPairToCreate>> switchesToCreatePerRa = new HashMap<>();
        SortedSet<BusToCreate> busesToCreate = new TreeSet<>();
        Map<String, NetworkHelper.BusBarEquivalentModel> createdSwitches = new HashMap<>();

        TCRACSeries tcracSeries = CseCracCreator.getCracSeries(cseCrac.getCracDocument());
        List<TRemedialActions> tRemedialActionsList = tcracSeries.getRemedialActions();

        for (TRemedialActions tRemedialActions : tRemedialActionsList) {
            if (tRemedialActions != null) {
                tRemedialActions.getRemedialAction().stream()
                    .filter(tRemedialAction -> tRemedialAction.getBusBar() != null)
                    .forEach(tRemedialAction -> {
                        try {
                            computeBusesAndSwitchesToCreate(tRemedialAction, ucteNetworkAnalyzer, busesToCreate, switchesToCreatePerRa);
                        } catch (FaraoException e) {
                            LOGGER.warn("RA {} has been skipped: {}", tRemedialAction.getName().getV(), e.getMessage());
                        }
                    });
            }
        }
        createBuses(busesToCreate, networkModifier);
        createSwitches(switchesToCreatePerRa, createdSwitches, networkModifier);
        networkModifier.commitAllChanges();
        return computeBusBarChangeSwitches(switchesToCreatePerRa, createdSwitches);
    }

    /**
     * For every BusBar TRemedialAction, this method detects and stores info about elements that should be created:
     * - buses, if initial or final node does not exist in the network (info stored in busesToCreate)
     * - switches, to be able to move the line from the initial to the final node (info stored in switchesToCreatePerRa)
     */
    private static void computeBusesAndSwitchesToCreate(TRemedialAction tRemedialAction, UcteNetworkAnalyzer ucteNetworkAnalyzer, SortedSet<BusToCreate> busesToCreate, Map<String, Set<SwitchPairToCreate>> switchesToCreatePerRa) {
        Optional<BusToCreate> raBusToCreate = Optional.empty();
        Set<SwitchPairToCreate> raSwitchesToCreate = new HashSet<>();

        // Get initial and final nodes
        UcteBusHelper initialNodeHelper = new UcteBusHelper(tRemedialAction.getBusBar().getInitialNode().getV(), ucteNetworkAnalyzer);
        UcteBusHelper finalNodeHelper = new UcteBusHelper(tRemedialAction.getBusBar().getFinalNode().getV(), ucteNetworkAnalyzer);

        String initialNodeId;
        String finalNodeId;
        if (initialNodeHelper.isValid() && finalNodeHelper.isValid()) {
            initialNodeId = initialNodeHelper.getIdInNetwork();
            finalNodeId = finalNodeHelper.getIdInNetwork();
        } else if (initialNodeHelper.isValid() && !finalNodeHelper.isValid()) {
            initialNodeId = initialNodeHelper.getIdInNetwork();
            finalNodeId = tRemedialAction.getBusBar().getFinalNode().getV();
            raBusToCreate = Optional.of(new BusToCreate(finalNodeId, ((Bus) ucteNetworkAnalyzer.getNetwork().getIdentifiable(initialNodeId)).getVoltageLevel().getId()));
        } else if (!initialNodeHelper.isValid() && finalNodeHelper.isValid()) {
            finalNodeId = finalNodeHelper.getIdInNetwork();
            initialNodeId = tRemedialAction.getBusBar().getInitialNode().getV();
            raBusToCreate = Optional.of(new BusToCreate(initialNodeId, ((Bus) ucteNetworkAnalyzer.getNetwork().getIdentifiable(finalNodeId)).getVoltageLevel().getId()));
        } else {
            throw new FaraoException(String.format("Remedial action's initial and final nodes are not valid: %s", initialNodeHelper.getInvalidReason()));
        }

        // Store all switches to create
        tRemedialAction.getBusBar().getBranch().forEach(tBranch -> {
            String linId = NetworkHelper.getLineIdInNetwork(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), String.valueOf(tBranch.getOrder().getV()), initialNodeId, finalNodeId, ucteNetworkAnalyzer);
            raSwitchesToCreate.add(new SwitchPairToCreate(linId, initialNodeId, finalNodeId));
        });

        // If everything is OK with the RA, store what should be created
        raBusToCreate.ifPresent(busesToCreate::add);
        switchesToCreatePerRa.put(tRemedialAction.getName().getV(), raSwitchesToCreate);
    }

    /**
     * Creates buses needed in busesToCreate
     */
    private static void createBuses(SortedSet<BusToCreate> busesToCreate, NetworkModifier networkModifier) {
        busesToCreate.forEach(busToCreate -> networkModifier.createBus(busToCreate.busId, busToCreate.voltageLevelId));
    }

    /**
     * Creates switches needed in switchesToCreatePerRa
     * Stores info about created switches in createdSwitches, SwitchPairToCreate ID
     */
    private static void createSwitches(Map<String, Set<SwitchPairToCreate>> switchesToCreatePerRa, Map<String, NetworkHelper.BusBarEquivalentModel> createdSwitches, NetworkModifier networkModifier) {
        // Get a list of all switch paris to create, some of them may be in common for different RAs
        List<SwitchPairToCreate> uniqueSwitchPairsToCreate = switchesToCreatePerRa.values().stream().flatMap(Set::stream).sorted().collect(Collectors.toList());
        // Store information about created fictitious buses for moving branches
        Map<String, String> fictitiousBusIdPerBranchId = new HashMap<>();
        for (SwitchPairToCreate switchPairToCreate: uniqueSwitchPairsToCreate) {
            String fictitiousBusId;
            // First, see if the branch has already been moved to a fictitious bus by a previous switch pair creation
            if (fictitiousBusIdPerBranchId.containsKey(switchPairToCreate.branchId)) {
                // If yes, re-use the same fictitious bus
                fictitiousBusId = fictitiousBusIdPerBranchId.get(switchPairToCreate.branchId);
            } else {
                // If not, create a new fictitious bus and store the info
                fictitiousBusId = NetworkHelper.moveBranchToNewFictitiousBus(switchPairToCreate, networkModifier);
                fictitiousBusIdPerBranchId.put(switchPairToCreate.branchId, fictitiousBusId);
            }
            // Then create the switch pair
            createdSwitches.put(switchPairToCreate.uniqueId, NetworkHelper.createSwitchPair(switchPairToCreate, networkModifier, fictitiousBusId));
        }
    }

    /**
     * Maps info in createdSwitches to needed switches for RAs, and creates a BusBarChangeSwitches set
     */
    private static Set<BusBarChangeSwitches> computeBusBarChangeSwitches(Map<String, Set<SwitchPairToCreate>> switchesToCreatePerRa, Map<String, NetworkHelper.BusBarEquivalentModel> createdSwitches) {
        Set<BusBarChangeSwitches> busBarChangeSwitches = new HashSet<>();

        // Loop through the RAs and add switch pairs IDs to the set
        switchesToCreatePerRa.forEach((raId, switchPairsToCreateForRa) -> {
            // Loop through the branches of RA and fetch switch pair IDs
            Set<SwitchPairId> switchPairs = switchPairsToCreateForRa.stream()
                .map(switchPairToCreate -> getCreatedSwitchPairId(switchPairToCreate, createdSwitches))
                .collect(Collectors.toSet());
            busBarChangeSwitches.add(new BusBarChangeSwitches(raId, switchPairs));
        });
        return busBarChangeSwitches;
    }

    /**
     * Fetches the IDs of the created switches to open & close, for a given SwitchPairToCreate
     */
    private static SwitchPairId getCreatedSwitchPairId(SwitchPairToCreate switchPairToCreate, Map<String, NetworkHelper.BusBarEquivalentModel> createdSwitches) {
        String switchPairToCreateId = switchPairToCreate.uniqueId();
        // OPEN switch between branch and initial node, CLOSE switch between branch and final node
        return new SwitchPairId(
            createdSwitches.get(switchPairToCreateId).getSwitchId(switchPairToCreate.initialNodeId),
            createdSwitches.get(switchPairToCreateId).getSwitchId(switchPairToCreate.finalNodeId)
        );
    }

    /**
     * Contains needed info to create a bus
     */
    private static class BusToCreate implements Comparable<BusToCreate> {
        String busId; // ID of the bus to create
        String voltageLevelId; // ID of the voltage level to create the bus on

        BusToCreate(String busId, String voltageLevelId) {
            this.busId = busId;
            this.voltageLevelId = voltageLevelId;
        }

        @Override
        public int hashCode() {
            return busId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return busId.equals(((BusToCreate) obj).busId);
        }

        @Override
        public int compareTo(BusToCreate other) {
            return this.busId.compareTo(other.busId);
        }
    }

}
