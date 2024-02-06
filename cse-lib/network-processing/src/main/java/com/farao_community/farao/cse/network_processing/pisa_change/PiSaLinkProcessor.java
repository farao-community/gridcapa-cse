/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.network_processing.pisa_change;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;

import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PiSaLinkProcessor {

    private final PiSaLinkConfiguration piSaLinkConfiguration;

    public PiSaLinkProcessor(PiSaLinkConfiguration piSaLinkConfiguration) {
        this.piSaLinkConfiguration = piSaLinkConfiguration;
    }

    /**
     * Check that all the fictive lines and generators are present. If some elements are present but not all
     * of them, an error is thrown for incomplete model reason.
     *
     * @param network: Network on which to test PiSa HVDC link presence.
     * @return True is all the elements of the model are present. False if none of the elements are present.
     */
    public boolean isLinkPresent(Network network) {
        // Gather presence information of all the fictive groups
        List<Boolean> elementsPresence = Stream.of(
            piSaLinkConfiguration.getPiSaLinkFictiveNodeFr(),
            piSaLinkConfiguration.getPiSaLinkFictiveNodeIt()
        ).map(nodeId -> getGenerator(network, nodeId) != null).collect(Collectors.toList());
        // Gather presence information of the fictive lines
        elementsPresence.addAll(
            piSaLinkConfiguration.getPiSaLinkFictiveLines().stream()
                .map(lineId -> network.getLine(lineId) != null)
                .collect(Collectors.toList()));
        if (elementsPresence.stream().allMatch(presence -> presence)) {
            return true;
        } else if (elementsPresence.stream().noneMatch(presence -> presence)) {
            return false;
        } else {
            throw new PiSaLinkException("Incomplete HVDC PiSa model. Impossible to compute.");
        }
    }

    public boolean isLinkConnected(Network network) {
        return network.getBusBreakerView().getBus(piSaLinkConfiguration.getPiSaLinkFictiveNodeFr()).isInMainConnectedComponent()
            && network.getBusBreakerView().getBus(piSaLinkConfiguration.getPiSaLinkFictiveNodeIt()).isInMainConnectedComponent();
    }

    public void alignFictiveGenerators(Network network) {
        Generator piSaGeneratorLinkNodeFr = getFrFictiveGenerator(network);
        Generator piSaGeneratorLinkNodeIt = getItFictiveGenerator(network);

        alignGenerators(piSaGeneratorLinkNodeFr, piSaGeneratorLinkNodeIt);
    }

    /**
     * There are 2 accepted configurations :
     * - AC emulation : One the fictive line is connected
     * - Set-point : All fictive lines must be disconnected
     * Otherwise the topology is not correct.
     *
     * @param network: Network on which to check the link mode.
     * @return True if the link matches AC emulation configuration and false it matches set-point configuration.
     * @throws PiSaLinkException : Throws if the actual topology matches none of the configurations.
     */
    public boolean isLinkInACEmulation(Network network) {
        List<Line> fictiveLines = piSaLinkConfiguration.getPiSaLinkFictiveLines().stream()
            .map(network::getLine)
            .collect(Collectors.toList());
        if (fictiveLines.stream().allMatch(line -> !line.getTerminal1().isConnected() && !line.getTerminal2().isConnected())) {
            return false;
        }
        if (fictiveLines.stream().anyMatch(line -> line.getTerminal1().isConnected() && line.getTerminal2().isConnected())) {
            return true;
        }
        throw new PiSaLinkException(
            String.format("Wrong configuration of PiSa link between %s and %s",
                piSaLinkConfiguration.getPiSaLinkFictiveNodeFr(), piSaLinkConfiguration.getPiSaLinkFictiveNodeIt()));
    }

    public void setLinkInSetpointMode(Network network, Crac crac) {
        List<Line> connectingLines = piSaLinkConfiguration.getPiSaLinkFictiveLines().stream()
            .map(network::getLine)
            .collect(Collectors.toList());
        Generator generatorFr = getFrFictiveGenerator(network);
        Generator generatorIt = getItFictiveGenerator(network);

        Set<InjectionRangeAction> hvdcRangeAction = getRelatedHvdcRangeActions(crac);
        double minFr = hvdcRangeAction.stream()
            .map(ra -> ra.getMinAdmissibleSetpoint(generatorFr.getMinP()))
            .reduce(generatorFr.getMinP(), BinaryOperator.maxBy(Double::compareTo));
        double maxIt = hvdcRangeAction.stream()
            .map(ra -> ra.getMaxAdmissibleSetpoint(generatorIt.getMaxP()))
            .reduce(generatorIt.getMaxP(), BinaryOperator.minBy(Double::compareTo));
        double setPointIt = Math.min(Math.abs(minFr), Math.abs(maxIt));

        generatorFr.getTerminal().connect();
        generatorFr.setTargetP(-setPointIt);
        generatorIt.getTerminal().connect();
        generatorIt.setTargetP(setPointIt);

        connectingLines.forEach(line -> {
            line.getTerminal1().disconnect();
            line.getTerminal2().disconnect();
        });
    }

    Set<InjectionRangeAction> getRelatedHvdcRangeActions(Crac crac) {
        Set<String> fictiveGeneratorIds = Stream.of(piSaLinkConfiguration.getPiSaLinkFictiveNodeFr(), piSaLinkConfiguration.getPiSaLinkFictiveNodeIt())
            .map(PiSaLinkProcessor::getGeneratorId)
            .collect(Collectors.toSet());

        return crac.getInjectionRangeActions().stream()
            .filter(injectionRangeAction -> injectionRangeAction.getInjectionDistributionKeys().keySet().stream()
                .map(Identifiable::getId)
                .collect(Collectors.toSet())
                .containsAll(fictiveGeneratorIds))
            .collect(Collectors.toSet());
    }

    public Generator getFrFictiveGenerator(Network network) {
        return getGenerator(network, piSaLinkConfiguration.getPiSaLinkFictiveNodeFr());
    }

    public Generator getItFictiveGenerator(Network network) {
        return getGenerator(network, piSaLinkConfiguration.getPiSaLinkFictiveNodeIt());
    }

    public double getItFictiveGeneratorTargetP(Network network) {
        return getItFictiveGenerator(network).getTargetP();
    }

    public String getPisaLinkPraName() {
        return piSaLinkConfiguration.getPiSaLinkPraName();
    }

    static Generator getGenerator(Network network, String nodeId) {
        return network.getGenerator(getGeneratorId(nodeId));
    }

    static String getGeneratorId(String nodeId) {
        return nodeId + "_generator";
    }

    /**
     * Put both generators at same absolute value of target P but opposite sign. It is aligned on the highest
     * absolute value of target P.
     *
     * @param generator1: First generator to align.
     * @param generator2: Second generator to align.
     */
    static void alignGenerators(Generator generator1, Generator generator2) {
        if (Math.abs(generator1.getTargetP()) > Math.abs(generator2.getTargetP())) {
            generator2.setTargetP(-generator1.getTargetP());
        } else {
            generator1.setTargetP(-generator2.getTargetP());
        }
    }
}
