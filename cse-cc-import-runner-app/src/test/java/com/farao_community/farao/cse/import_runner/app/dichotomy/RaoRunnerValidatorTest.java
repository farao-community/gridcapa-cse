/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.services.FileExporter;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class RaoRunnerValidatorTest {

    private static final String NETWORK_PRE_SIGNED_URL = "http://network.url";
    private static final String BASE_DIR_PATH = "/base/dir/path";
    private static final String RAO_PARAMETERS_URL = "http://parameters.url";
    private static final String REQUEST_ID = "requestId";
    private static final String CRAC_URL = "http://crac.url";

    @MockBean
    private FileExporter fileExporter;

    @Test
    void buildRaoRequestWithEmptyPreviousActionsShouldNotSaveParameters() {
        List<String> previousActions = Collections.singletonList("Action1");

        RaoRunnerValidator raoRunnerValidator = new RaoRunnerValidator(ProcessType.D2CC, REQUEST_ID, null, CRAC_URL, RAO_PARAMETERS_URL, null, null, null, null, null, false);
        RaoRequest result = raoRunnerValidator.buildRaoRequest(NETWORK_PRE_SIGNED_URL, BASE_DIR_PATH, previousActions);

        verify(fileExporter, never()).saveRaoParameters(anyString(), anyList(), any(), any(), anyBoolean());
        assertNotNull(result);
        assertEquals(RAO_PARAMETERS_URL, result.getRaoParametersFileUrl());
    }
}
