/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.configurations;

import com.farao_community.farao.cse.import_runner.app.services.InterruptionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class InterruptConfiguration {

    @Bean
    public Consumer<String> interruptionConsumer(InterruptionService interruptionService) {
        return interruptionService::interruption;
    }
}
