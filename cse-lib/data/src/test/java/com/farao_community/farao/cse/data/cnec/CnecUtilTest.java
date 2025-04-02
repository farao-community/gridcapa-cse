/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.data.cnec;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.cse.CseCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class CnecUtilTest {

    @Test
    void checkThatWorstCnecIsFoundedCorrectly() throws IOException {
        InputStream cracInputStream = getClass().getResourceAsStream("pst_and_topo/crac.xml");
        Network network = Network.read("pst_and_topo/network.uct", getClass().getResourceAsStream("pst_and_topo/network.uct"));
        CseCracCreationContext cseCracCreationContext = (CseCracCreationContext) Crac.readWithContext("crac.xml", cracInputStream, network, new CracCreationParameters());
        InputStream raoResultInputStream = getClass().getResourceAsStream("pst_and_topo/raoResult.json");
        RaoResult raoResult = RaoResult.read(raoResultInputStream, cseCracCreationContext.getCrac());

        FlowCnec worstCnec = CnecUtil.getWorstCnec(cseCracCreationContext.getCrac(), raoResult);
        assertEquals("French line 1", worstCnec.getName());
    }

}
