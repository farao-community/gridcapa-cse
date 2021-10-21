/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse.starter;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
@EnableConfigurationProperties(CseClientProperties.class)
public class CseClientAutoConfiguration {
    private final CseClientProperties clientProperties;

    public CseClientAutoConfiguration(CseClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    @Bean
    public CseClient cseClient(AmqpTemplate amqpTemplate) {
        return new CseClient(amqpTemplate, clientProperties);
    }
}
