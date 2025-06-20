/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.services.FileExporter;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class CseNetworkExporterTest {

    @Test
    void testExport() {
        // Given
        final Network network = Mockito.mock(Network.class);
        final CseRequest cseRequest = Mockito.mock(CseRequest.class);
        final FileExporter fileExporter = Mockito.mock(FileExporter.class);
        final OffsetDateTime processTargetDateTime = OffsetDateTime.of(2025, 6, 20, 15, 12, 0, 0, ZoneOffset.UTC);
        final VariantManager variantManager = Mockito.mock(VariantManager.class);

        Mockito.when(cseRequest.getTargetProcessDateTime()).thenReturn(processTargetDateTime);
        Mockito.when(cseRequest.getProcessType()).thenReturn(ProcessType.D2CC);
        Mockito.when(cseRequest.isImportEcProcess()).thenReturn(true);
        Mockito.when(fileExporter.getZoneId()).thenReturn("UTC");
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        Mockito.when(variantManager.getWorkingVariantId()).thenReturn("variantId");
        Mockito.when(network.getNameOrId()).thenReturn("testId");

        // When
        new CseNetworkExporter(cseRequest, fileExporter).export(network);

        // Then
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(fileExporter).exportAndUploadNetwork(
                Mockito.eq(network),
                Mockito.eq("UCTE"),
                Mockito.eq(GridcapaFileGroup.ARTIFACT),
                captor.capture(),
                Mockito.eq(""),
                Mockito.eq(processTargetDateTime),
                Mockito.eq(ProcessType.D2CC),
                Mockito.eq(true));

        Assertions.assertThat(captor.getValue()).isEqualTo("CSE/IMPORT_EC/D2CC/2025/06/20/15_30/ARTIFACTS/variantId/testId.uct");
    }
}
