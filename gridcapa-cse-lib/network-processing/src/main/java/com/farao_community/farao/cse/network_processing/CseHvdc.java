/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing;

import com.powsybl.iidm.network.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards@rte-france.com>}
 */
public class CseHvdc {

    private final String id;
    private final String vscName;
    private final String originSubstationName;
    private final String extremitySubstationName;
    private final Integer numberOfCablesPerPole;
    private final Double rdc;
    private final Double pMax;
    private final Double nominalVdc;
    private final float lossCoefficient;

    public CseHvdc(String id,
                   String vscName,
                   String originSubstationName,
                   String extremitySubstationName,
                   Integer numberOfCablesPerPole,
                   Double rdc,
                   Double pMax,
                   Double nominalVdc,
                   float lossCoefficient) {
        this.id = id;
        this.vscName = vscName;
        this.originSubstationName = originSubstationName;
        this.extremitySubstationName = extremitySubstationName;
        this.numberOfCablesPerPole = numberOfCablesPerPole;
        this.rdc = rdc;
        this.pMax = pMax;
        this.nominalVdc = nominalVdc;
        this.lossCoefficient = lossCoefficient;
    }

    public void create(Network network) {
        String lineOr = vscName.substring(0, 8);
        String lineEx = vscName.substring(9, 17);

        // Getting fictive generators on each end of the hvdc.
        Generator generatorOr = ((Bus) network.getIdentifiable(lineOr)).getGenerators().iterator().next();
        Generator generatorEx = ((Bus) network.getIdentifiable(lineEx)).getGenerators().iterator().next();

        // Removing the fictive generators.
        generatorOr.getTerminal().disconnect();
        generatorEx.getTerminal().disconnect();

        // Removing the extra lines
        ((Bus) network.getIdentifiable(lineOr)).getLines().forEach(line -> {
            line.getTerminal1().disconnect();
            line.getTerminal2().disconnect();
        });
        ((Bus) network.getIdentifiable(lineEx)).getLines().forEach(line -> {
            line.getTerminal1().disconnect();
            line.getTerminal2().disconnect();
        });

        // Creating vsc converter station on origin substation
        VscConverterStation vscOr = ((Bus) network.getIdentifiable(originSubstationName)).getVoltageLevel().newVscConverterStation()
                .setId(id + "_" + originSubstationName + "_vsc")
                .setBus(originSubstationName)
                .setReactivePowerSetpoint(generatorOr.getTargetQ())
                .setVoltageSetpoint(generatorOr.getTargetV())
                .setVoltageRegulatorOn(generatorOr.isVoltageRegulatorOn())
                .setLossFactor(lossCoefficient)
                .add();
        vscOr.newReactiveCapabilityCurve()
                .beginPoint()
                .setP(-pMax)
                .setMinQ(generatorOr.getReactiveLimits().getMinQ(-pMax))
                .setMaxQ(generatorOr.getReactiveLimits().getMaxQ(-pMax))
                .endPoint()
                .beginPoint()
                .setP(pMax)
                .setMinQ(generatorOr.getReactiveLimits().getMinQ(pMax))
                .setMaxQ(generatorOr.getReactiveLimits().getMaxQ(pMax))
                .endPoint()
                .add();

        // Creating vsc converter station on extremity substation
        VscConverterStation vscEx = ((Bus) network.getIdentifiable(extremitySubstationName)).getVoltageLevel().newVscConverterStation()
                .setId(id + "_" + extremitySubstationName + "_vsc")
                .setBus(extremitySubstationName)
                .setReactivePowerSetpoint(generatorEx.getTargetQ())
                .setVoltageSetpoint(generatorEx.getTargetV())
                .setVoltageRegulatorOn(generatorEx.isVoltageRegulatorOn())
                .setLossFactor(lossCoefficient)
                .add();
        vscEx.newReactiveCapabilityCurve()
                .beginPoint()
                .setP(-pMax)
                .setMinQ(generatorEx.getReactiveLimits().getMinQ(-pMax))
                .setMaxQ(generatorEx.getReactiveLimits().getMaxQ(-pMax))
                .endPoint()
                .beginPoint()
                .setP(pMax)
                .setMinQ(generatorEx.getReactiveLimits().getMinQ(pMax))
                .setMaxQ(generatorEx.getReactiveLimits().getMaxQ(pMax))
                .endPoint()
                .add();

        // Check converters mode:
        HvdcLine.ConvertersMode convertersMode;
        if (generatorOr.getTargetP() >= 0 && generatorEx.getTargetP() <= 0) {
            convertersMode = HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER;
        } else if (generatorOr.getTargetP() <= 0 && generatorEx.getTargetP() >= 0) {
            convertersMode = HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER;
        } else {
            throw new RuntimeException(String.format("Fictive generators' target P for hvdc %s are of the same sign. Unable to decide flow direction.", id));
        }

        // Creating the hvdc line between the two vsc converter stations
        network.newHvdcLine()
                .setId(vscName)
                .setR(rdc / numberOfCablesPerPole)
                .setNominalV(nominalVdc)
                .setActivePowerSetpoint(Math.max(generatorOr.getTargetP(), generatorEx.getTargetP()))
                .setMaxP(pMax)
                .setConverterStationId1(id + "_" + originSubstationName + "_vsc")
                .setConverterStationId2(id + "_" + extremitySubstationName + "_vsc")
                .setConvertersMode(convertersMode)
                .add();
    }

    public void removeAndReset(Network initialNetwork, Network modifiedNetwork) {
        // Removing the hvdc line
        HvdcLine hvdcLine = modifiedNetwork.getHvdcLine(vscName);
        hvdcLine.remove();

        // Removing the vsc converter stations
        ((VscConverterStation) modifiedNetwork.getIdentifiable(id + "_" + originSubstationName + "_vsc")).remove();
        ((VscConverterStation) modifiedNetwork.getIdentifiable(id + "_" + extremitySubstationName + "_vsc")).remove();

        String lineOr = vscName.substring(0, 8);
        String lineEx = vscName.substring(9, 17);

        // Reconnecting the fictive generators
        String generatorOrId = ((Bus) initialNetwork.getIdentifiable(lineOr)).getGenerators().iterator().next().getId();
        String generatorExId = ((Bus) initialNetwork.getIdentifiable(lineEx)).getGenerators().iterator().next().getId();
        Generator generatorOr = (Generator) modifiedNetwork.getIdentifiable(generatorOrId);
        generatorOr.getTerminal().connect();
        Generator generatorEx = (Generator) modifiedNetwork.getIdentifiable(generatorExId);
        generatorEx.getTerminal().connect();

        // Setting the right P for the generators (may have been changed by the RAO)
        HvdcLine.ConvertersMode convertersMode = hvdcLine.getConvertersMode();
        if (convertersMode == HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER) {
            generatorOr.setTargetP(-hvdcLine.getActivePowerSetpoint());
            generatorEx.setTargetP(hvdcLine.getActivePowerSetpoint());
        } else if (convertersMode == HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER) {
            generatorOr.setTargetP(hvdcLine.getActivePowerSetpoint());
            generatorEx.setTargetP(-hvdcLine.getActivePowerSetpoint());
        }

        // Reconnecting the extra lines
        ((Bus) initialNetwork.getIdentifiable(lineOr)).getLines().forEach(line -> {
            Line disconnectedLine = modifiedNetwork.getLine(line.getId());
            disconnectedLine.getTerminal1().disconnect();
            disconnectedLine.getTerminal2().disconnect();
        });
        ((Bus) initialNetwork.getIdentifiable(lineEx)).getLines().forEach(line -> {
            Line disconnectedLine = modifiedNetwork.getLine(line.getId());
            disconnectedLine.getTerminal1().disconnect();
            disconnectedLine.getTerminal2().disconnect();
        });

    }
}
