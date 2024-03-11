/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.dichotomy.DichotomyRaoResponse;
import com.farao_community.farao.cse.import_runner.app.dichotomy.MultipleDichotomyResult;
import com.farao_community.farao.cse.import_runner.app.dichotomy.MultipleDichotomyRunner;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.powsybl.openrao.data.cracapi.Crac;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.BusBarChangeSwitches;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.CseCracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.SwitchPairId;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class CseRunnerTest {

    @Autowired
    private CseRunner cseRunner;

    @MockBean
    private MultipleDichotomyRunner multipleDichotomyRunner;

    @MockBean
    private InitialShiftService initialShiftService;

    @MockBean
    private FileExporter fileExporter;

    @Test
    void testCracImportAndBusbarPreprocess() {
        String cracUrl = Objects.requireNonNull(getClass().getResource("20210901_2230_213_CRAC_CO_CSE1_busbar.xml")).toString();
        Network network = Network.read("20210901_2230_test_network.uct", getClass().getResourceAsStream("20210901_2230_test_network.uct"));
        // Initially only one bus in this voltage level ITALY111
        assertEquals(1, Stream.of(network.getVoltageLevel("ITALY11").getBusBreakerView().getBuses()).count());

        Crac crac = cseRunner.importCracAndModifyNetworkForBusBars(cracUrl, OffsetDateTime.parse("2021-09-01T20:30Z"), network)
            .cseCracCreationContext.getCrac();

        // After pre-processing 4 buses in this voltage level ITALY111, ITALY112, ITALY11Z and ITALY11Y
        assertEquals(4, StreamSupport.stream(network.getVoltageLevel("ITALY11").getBusBreakerView().getBuses().spliterator(), true).count());
        assertEquals(1, crac.getRemedialActions().size());
    }

    @Test
    void testRun() {
        CseRequest cseRequest = null;
        try {
            cseRequest = new CseRequest(
                "ID1",
                ProcessType.IDCC,
                OffsetDateTime.parse("2021-09-01T20:30Z"),
                getClass().getResource("20210901_2230_test_network_pisa_test_both_links_connected_setpoint_and_emulation_ok_for_run.uct").toURI().toURL().toString(),
                getClass().getResource("20210901_2230_213_CRAC_CO_CSE1.xml").toURI().toURL().toString(),
                getClass().getResource("crac.xml").toURI().toURL().toString(),
                getClass().getResource("20210624_2D4_NTC_reductions_CSE1_Adapted_v8_8.xml").toURI().toURL().toString(),
                getClass().getResource("NTC2_20210901_2D1_AT-IT1.xml").toURI().toURL().toString(),
                getClass().getResource("NTC2_20210901_2D1_CH-IT1.xml").toURI().toURL().toString(),
                getClass().getResource("NTC2_20210901_2D1_FR-IT1.xml").toURI().toURL().toString(),
                getClass().getResource("NTC2_20210901_2D1_SI-IT1.xml").toURI().toURL().toString(),
                null,
                getClass().getResource("vulcanus_01032021_96.xls").toURI().toURL().toString(),
                getClass().getResource("2021_2Dp_NTC_annual_CSE1_Adapted_v8_8.xml").toURI().toURL().toString(),
                null,
                null,
                null,
                50.0,
                0.0,
                null,
                true
            );
        } catch (MalformedURLException | URISyntaxException e) {
            fail();
        }
        try {
            MultipleDichotomyResult<DichotomyRaoResponse> dichotomyResult = mock(MultipleDichotomyResult.class);

            when(multipleDichotomyRunner.runMultipleDichotomy(
                any(CseRequest.class),
                any(CseData.class),
                any(Network.class),
                any(Crac.class),
                anyDouble(),
                any(Map.class),
                any(Map.class)
            )).thenReturn(dichotomyResult);
            DichotomyResult<DichotomyRaoResponse> raoResponse = mock(DichotomyResult.class);

            when(dichotomyResult.getBestDichotomyResult()).thenReturn(raoResponse);
            when(raoResponse.hasValidStep()).thenReturn(false);

            when(fileExporter.getFinalNetworkFilePath(any(OffsetDateTime.class), any(ProcessType.class), anyBoolean())).thenReturn("AnyString");
            when(fileExporter.exportAndUploadNetwork(any(Network.class), anyString(), any(GridcapaFileGroup.class), anyString(), anyString(), any(OffsetDateTime.class), any(ProcessType.class), anyBoolean())).thenReturn("file:/AnyString/IMPORT_EC/test");
            when(fileExporter.saveTtcResult(any(Timestamp.class), any(OffsetDateTime.class), any(ProcessType.class), anyBoolean())).thenReturn("file:/AnyTTCfilepath/IMPORT_EC/test");

            doNothing().when(initialShiftService).performInitialShiftFromVulcanusLevelToNtcLevel(any(), any(), any(), anyMap(), anyMap());

            CseResponse response = cseRunner.run(cseRequest);

            assertNotNull(response);
            assertTrue(StringUtils.contains(response.getTtcFileUrl(), "IMPORT_EC"));
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    void getCracCreationParametersTest() {
        Set<BusBarChangeSwitches> busBarChangesSwitches = Set.of(new BusBarChangeSwitches("ra-id", Set.of(new SwitchPairId("switch-to-open", "switch-to-close"))));
        CracCreationParameters cracCreationParameters = cseRunner.getCracCreationParameters(busBarChangesSwitches);
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
