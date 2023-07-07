/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.network_processing.ucte_pst_change;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class PstInitializerTest {

    @Test
    void testInitializePstWhenOutOfRange() {
        String networkFilename = "pst_initially_out_of_range.uct";
        String cracFilename = "pst_with_range.xml";

        Network network = Network.read(networkFilename, getClass().getResourceAsStream(networkFilename));
        Crac crac = CracCreators.importAndCreateCrac(
            cracFilename,
            Objects.requireNonNull(getClass().getResourceAsStream(cracFilename)),
            network,
            null).getCrac();

        PhaseTapChanger ptc = network.getTwoWindingsTransformer(crac.getPstRangeAction("PST_PST_RANGE_BBE2AA1  BBE3AA1  1").getNetworkElement().getId()).getPhaseTapChanger();
        assertEquals(-5, ptc.getTapPosition());

        PstInitializer.withDefaultLogger().initializePsts(network, crac);
        assertEquals(0, ptc.getTapPosition());
    }

    @Test
    void testInitializePstWhenInsideRange() {
        String networkFilename = "pst_initially_inside_range.uct";
        String cracFilename = "pst_with_range.xml";

        Network network = Network.read(networkFilename, getClass().getResourceAsStream(networkFilename));
        Crac crac = CracCreators.importAndCreateCrac(
            cracFilename,
            Objects.requireNonNull(getClass().getResourceAsStream(cracFilename)),
            network,
            null).getCrac();

        PhaseTapChanger ptc = network.getTwoWindingsTransformer(crac.getPstRangeAction("PST_PST_RANGE_BBE2AA1  BBE3AA1  1").getNetworkElement().getId()).getPhaseTapChanger();
        assertEquals(5, ptc.getTapPosition());

        PstInitializer.withDefaultLogger().initializePsts(network, crac);
        assertEquals(5, ptc.getTapPosition());
    }

    @Test
    void testInitializePstWithMaxOfMinCracMinCgm() {
        String networkFilename = "pst_initially_out_of_range_2.uct";
        String cracFilename = "pst_with_range_2.xml";

        Network network = Network.read(networkFilename, getClass().getResourceAsStream(networkFilename));
        Crac crac = CracCreators.importAndCreateCrac(
                cracFilename,
                Objects.requireNonNull(getClass().getResourceAsStream(cracFilename)),
                network,
                null).getCrac();

        PhaseTapChanger ptc = network.getTwoWindingsTransformer(crac.getPstRangeAction("PST_PST_RANGE_BBE2AA1  BBE3AA1  1").getNetworkElement().getId()).getPhaseTapChanger();
        assertEquals(4, ptc.getTapPosition());

        PstInitializer.withDefaultLogger().initializePsts(network, crac);
        assertEquals(-22, ptc.getTapPosition());
    }

    @Test
    void testInitializePstWithMaxOfMinCracMinCgm2() {
        String networkFilename = "pst_initially_out_of_range_3.uct";
        String cracFilename = "pst_with_range_3.xml";

        Network network = Network.read(networkFilename, getClass().getResourceAsStream(networkFilename));
        Crac crac = CracCreators.importAndCreateCrac(
                cracFilename,
                Objects.requireNonNull(getClass().getResourceAsStream(cracFilename)),
                network,
                null).getCrac();

        PhaseTapChanger ptc = network.getTwoWindingsTransformer(crac.getPstRangeAction("PST_PST_RANGE_BBE2AA1  BBE3AA1  1").getNetworkElement().getId()).getPhaseTapChanger();
        assertEquals(-4, ptc.getTapPosition());

        PstInitializer.withDefaultLogger().initializePsts(network, crac);
        assertEquals(0, ptc.getTapPosition());
    }
}
