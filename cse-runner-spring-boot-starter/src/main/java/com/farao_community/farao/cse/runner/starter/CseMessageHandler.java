/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.starter;

import com.farao_community.farao.cse.runner.api.JsonApiConverter;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import org.springframework.amqp.core.*;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class CseMessageHandler {
    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";

    private final CseClientProperties clientProperties;
    private final JsonApiConverter jsonConverter;

    public CseMessageHandler(CseClientProperties clientProperties) {
        this.clientProperties = clientProperties;
        this.jsonConverter = new JsonApiConverter();
    }

    public <I> Message buildMessage(I request, int priority) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(request))
                .andProperties(buildMessageProperties(priority))
                .build();
    }

    private MessageProperties buildMessageProperties(int priority) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(clientProperties.getBinding().getApplicationId())
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(clientProperties.getBinding().getExpiration())
                .setPriority(priority)
                .build();
    }

    public <J> J readMessage(Message message, Class<J> clazz) {
        if (message != null) {
            return jsonConverter.fromJsonMessage(message.getBody(), clazz);
        } else {
            throw new CseInternalException("Cse server did not respond");
        }
    }
}
