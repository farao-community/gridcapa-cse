/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app;

import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.farao_community.farao.cse.import_runner.app.services.UrlValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class UrlValidationServiceTest {

    @Autowired
    private UrlValidationService urlValidationService;

    @Test
    void checkExceptionThrownWhenUrlIsNotPartOfWhitelistedUrls() {
        Exception exception = assertThrows(CseInvalidDataException.class, () -> urlValidationService.openUrlStream("url1"));
        String expectedMessage = "is not part of application's whitelisted url's";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }
}
