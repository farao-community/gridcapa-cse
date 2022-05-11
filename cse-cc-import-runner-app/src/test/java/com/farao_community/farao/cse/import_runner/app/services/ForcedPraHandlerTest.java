/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ForcedPraHandlerTest {

    @Autowired
    ForcedPrasHandler forcedPrasHandler;

    @Test
    void checkCracAndForcedPrasAreConsistent() {
        Crac crac = CracImporters.importCrac("crac-for-forced-pras.json", Objects.requireNonNull(getClass().getResourceAsStream("crac-for-forced-pras.json")));
        Network network = Importers.loadNetwork("network-for-forced-pras.xiidm", getClass().getResourceAsStream("network-for-forced-pras.xiidm"));
        List<String> forcedPrasIds = List.of("Open line NL1-NL2", "Open line BE2-FR3");
        assertDoesNotThrow(() -> forcedPrasHandler.checkInputForcesPrasConsistencyWithCrac(forcedPrasIds, crac, network));
    }

    @Test
    void checkCracAndForcedPrasAreNotConsistent() {
        Crac crac = CracImporters.importCrac("crac-for-forced-pras.json", Objects.requireNonNull(getClass().getResourceAsStream("crac-for-forced-pras.json")));
        Network network = Importers.loadNetwork("network-for-forced-pras.xiidm", getClass().getResourceAsStream("network-for-forced-pras.xiidm"));
        List<String> forcedPrasIds = List.of("PRA_PST_BE");
        assertThrows(CseDataException.class, () -> forcedPrasHandler.checkInputForcesPrasConsistencyWithCrac(forcedPrasIds, crac, network));
    }

    @Test
    void checkForcedPrasPresentInCrac() {
        Crac crac = CracImporters.importCrac("crac-for-forced-pras.json", Objects.requireNonNull(getClass().getResourceAsStream("crac-for-forced-pras.json")));
        Network network = Importers.loadNetwork("network-for-forced-pras.xiidm", getClass().getResourceAsStream("network-for-forced-pras.xiidm"));
        forcedPrasHandler.forcePras(List.of("Open line NL1-NL2", "Open line BE2-FR3"), network, crac);
        //Assertions.assertEquals(UsageMethod.FORCED, crac.getNetworkAction("Open line NL1-NL2").getUsageMethod(crac.getPreventiveState()));
        //Assertions.assertEquals(UsageMethod.FORCED, crac.getNetworkAction("Open line BE2-FR3").getUsageMethod(crac.getPreventiveState()));
    }
}
