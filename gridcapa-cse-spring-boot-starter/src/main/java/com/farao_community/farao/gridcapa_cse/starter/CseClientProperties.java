/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@ConfigurationProperties("cse-cc")
public class CseClientProperties {
    private AmqpConfiguration amqp;

    public CseClientProperties.AmqpConfiguration getAmqp() {
        return amqp;
    }

    public void setAmqp(CseClientProperties.AmqpConfiguration amqp) {
        this.amqp = amqp;
    }

    public static class AmqpConfiguration {
        private String queueName;
        private String expiration;
        private String applicationId;

        public String getQueueName() {
            return queueName;
        }

        public void setQueueName(String queueName) {
            this.queueName = queueName;
        }

        public String getExpiration() {
            return expiration;
        }

        public void setExpiration(String expiration) {
            this.expiration = expiration;
        }

        public String getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(String applicationId) {
            this.applicationId = applicationId;
        }
    }
}
