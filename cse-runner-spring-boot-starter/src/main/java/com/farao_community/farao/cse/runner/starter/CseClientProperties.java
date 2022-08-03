/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.starter;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;


/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@ConfigurationProperties("cse-cc-runner")
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CseClientProperties {
    BindingConfiguration binding;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BindingConfiguration {
        String destination;
        String routingKey;
        String expiration;
        String applicationId;

        public String getRoutingKey() {
            return Optional.ofNullable(routingKey).orElse("#");
        }
    }

}
