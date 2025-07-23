package com.farao_community.farao.cse.export_runner.app.services;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MerchantLineServiceTest {

    @Autowired
    MerchantLineService merchantLineService;

    @Test
    void setTransformerInActivePowerRegulationOK() {
        String filename = "network_with_mendrisio.uct";
        Network network = Network.read(filename, getClass().getResourceAsStream(filename));
        merchantLineService.setTransformerInActivePowerRegulation(network);
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer("SMENDR3T SMENDR32 1");
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        assertEquals(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL, phaseTapChanger.getRegulationMode());
        assertTrue(phaseTapChanger.isRegulating());
        assertEquals(twoWindingsTransformer.getTerminal1(), phaseTapChanger.getRegulationTerminal());
    }

    @Test
    void setTransformerInActivePowerRegulationNoTargetFlow() {
        String filename = "network_with_mendrisio_no_taget_flow.uct";
        Network network = Network.read(filename, getClass().getResourceAsStream(filename));
        merchantLineService.setTransformerInActivePowerRegulation(network);
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer("SMENDR3T SMENDR32 1");
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
        assertEquals(PhaseTapChanger.RegulationMode.CURRENT_LIMITER, phaseTapChanger.getRegulationMode());
        assertFalse(phaseTapChanger.isRegulating());
    }
}
