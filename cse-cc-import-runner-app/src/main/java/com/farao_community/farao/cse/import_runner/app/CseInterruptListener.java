/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app;

import com.farao_community.farao.cse.import_runner.app.configurations.AmqpInterruptConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CseInterruptListener implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CseInterruptListener.class);
    private static final String TASK_STATUS_UPDATE = "task-status-update";

    private final AmqpTemplate amqpTemplate;
    private final AmqpInterruptConfiguration amqpConfiguration;

    @Autowired
    private CseListener cseListener;

    public CseInterruptListener(AmqpTemplate amqpTemplate, AmqpInterruptConfiguration amqpConfiguration) {
        this.amqpTemplate = amqpTemplate;
        this.amqpConfiguration = amqpConfiguration;
    }

    @Override
    public void onMessage(Message message) {
        String replyTo = message.getMessageProperties().getReplyTo();
        String correlationId = message.getMessageProperties().getCorrelationId();
        String taskId = new String(message.getBody());
        Optional<Thread> thread = isRunning(taskId);
        while (thread.isPresent()) {
            thread.get().interrupt();
            thread = isRunning(taskId);
        }
        Message mess = MessageBuilder.withBody(new byte[1])
                .andProperties(buildMessageResponseProperties(correlationId))
                .build();

        amqpTemplate.send(replyTo, mess);
    }

    private MessageProperties buildMessageResponseProperties(String correlationId) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId("cse-server")
                .setContentEncoding("UTF-8")
                .setContentType("application/vnd.api+json")
                .setCorrelationId(correlationId)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(amqpConfiguration.getCseInterruptResponseExpiration())
                .setPriority(1)
                .build();
    }

    private Optional<Thread> isRunning(String id) {
        return Thread.getAllStackTraces()
                .keySet()
                .stream()
                .filter(t -> t.getName().equals(id))
                .findFirst();
    }
}
