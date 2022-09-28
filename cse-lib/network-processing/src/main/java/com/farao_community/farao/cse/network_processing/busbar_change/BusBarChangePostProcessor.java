/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing.busbar_change;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.BusBarChangeSwitches;
import com.farao_community.farao.data.crac_creation.creator.cse.parameters.SwitchPairId;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * This class reverts the changes inflected by {@code BusBarChangePreProcessor} on a network
 * while taking into account the state of the fictitious switches
 *
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public class BusBarChangePostProcessor {
    private BusBarChangePostProcessor() {
        // utility class
    }

    /**
     * Remove fictitious buses and switches and reconnect lines in place of closed switches
     */
    public static void process(Network network, Set<BusBarChangeSwitches> busBarChangeSwitches) {
        process(network, getFictitiousBusesAndSwitches(busBarChangeSwitches, network));
    }

    /**
     * Returns a map containing, for every fictitious bus created in a BusBarChangeSwitches, the set of
     * fictitious switches connected to it.
     */
    private static Map<Bus, Set<Switch>> getFictitiousBusesAndSwitches(Set<BusBarChangeSwitches> busBarChangeSwitchesSet, Network network) {
        Map<Bus, Set<Switch>> fictitiousBusesAndSwitches = new HashMap<>();
        for (BusBarChangeSwitches busBarChangeSwitches : busBarChangeSwitchesSet) {
            for (SwitchPairId switchPairId : busBarChangeSwitches.getSwitchPairs()) {
                Bus fictitiousBus = getFictitiousBus(switchPairId, network);
                Set<Switch> fictitiousSwitches = getSwitches(switchPairId, network);
                fictitiousBusesAndSwitches
                    .computeIfAbsent(fictitiousBus, l -> new HashSet<>())
                    .addAll(fictitiousSwitches);
            }
        }
        return fictitiousBusesAndSwitches;
    }

    /**
     * Returns the fictitious bus of a switch pair
     * It is simply the common bus between the two switches
     */
    private static Bus getFictitiousBus(SwitchPairId switchPairId, Network network) {
        Set<Bus> buses1 = NetworkHelper.getBuses(network.getSwitch(switchPairId.getSwitchToCloseId()));
        Set<Bus> buses2 = NetworkHelper.getBuses(network.getSwitch(switchPairId.getSwitchToOpenId()));
        return buses1.stream().filter(buses2::contains).collect(toSingleton());
    }

    /**
     * Returns switches to close and open referenced by a SwitchPairId
     */
    private static Set<Switch> getSwitches(SwitchPairId switchPairId, Network network) {
        return Set.of(
            network.getSwitch(switchPairId.getSwitchToOpenId()),
            network.getSwitch(switchPairId.getSwitchToCloseId())
        );
    }

    private static void process(Network network, Map<Bus, Set<Switch>> fictitiousBusesAndSwitches) {
        NetworkModifier networkModifier = new NetworkModifier(network);
        for (Map.Entry<Bus, Set<Switch>> entry : fictitiousBusesAndSwitches.entrySet()) {
            Bus fictitiousBus = entry.getKey();
            Set<Switch> switches = entry.getValue();
            Switch closedSwitch = switches.stream().filter(s -> !s.isOpen()).collect(toSingleton());
            Bus realBus = NetworkHelper.getBuses(closedSwitch).stream().filter(otherBus -> !otherBus.equals(fictitiousBus)).collect(toSingleton());
            Map<Branch<?>, Branch.Side> connectedBranches = networkModifier.getBranchesStillConnectedToBus(fictitiousBus);
            for (Map.Entry<Branch<?>, Branch.Side> branchEntry : connectedBranches.entrySet()) {
                networkModifier.moveBranch(branchEntry.getKey(), branchEntry.getValue(), realBus);
            }
            networkModifier.removeBus(fictitiousBus);
            switches.forEach(networkModifier::removeSwitch);
        }
        networkModifier.commitAllChanges();
    }

    /**
     * Checks that there is exactly one element and returns it
     */
    private static <T> Collector<T, ?, T> toSingleton() {
        return Collectors.collectingAndThen(
            Collectors.toSet(),
            set -> {
                if (set.size() != 1) {
                    throw new FaraoException("Expected exactly one element");
                }
                return set.iterator().next();
            }
        );
    }
}
