/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.network_processing;

import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.BusBarChangeSwitches;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.CseCracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.SwitchPairId;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class CracCreationParametersServiceTest {
    @Test
    void getCracCreationParametersTest() {
        InputStream cracCreationParametersJsonPath = Objects.requireNonNull(getClass().getResourceAsStream("cseCracCreationParameters.json"));
        Set<BusBarChangeSwitches> busBarChangesSwitches = Set.of(new BusBarChangeSwitches("ra-id", Set.of(new SwitchPairId("switch-to-open", "switch-to-close"))));
        CracCreationParameters cracCreationParameters = CracCreationParametersService.getCracCreationParameters(cracCreationParametersJsonPath, busBarChangesSwitches);
        RaUsageLimits raUsageLimits = cracCreationParameters.getRaUsageLimitsPerInstant().get("curative");
        assertTrue(raUsageLimits.getMaxTopoPerTso().isEmpty());
        assertTrue(raUsageLimits.getMaxTopoPerTso().isEmpty());
        assertEquals(6, raUsageLimits.getMaxRaPerTso().get("IT"));
        assertEquals(5, raUsageLimits.getMaxRaPerTso().get("FR"));
        assertEquals(1, raUsageLimits.getMaxRaPerTso().get("CH"));
        assertEquals(3, raUsageLimits.getMaxRaPerTso().get("AT"));
        assertEquals(3, raUsageLimits.getMaxRaPerTso().get("SI"));
        CseCracCreationParameters cseCracCreationParameters = cracCreationParameters.getExtension(CseCracCreationParameters.class);
        assertEquals(1, cseCracCreationParameters.getBusBarChangeSwitches("ra-id").getSwitchPairs().size());
    }
}
