/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse.starter;

import com.farao_community.farao.gridcapa_cse.api.JsonApiConverter;
import com.farao_community.farao.gridcapa_cse.api.exception.CseInternalException;
import com.farao_community.farao.gridcapa_cse.api.resource.CseRequest;
import com.farao_community.farao.gridcapa_cse.api.resource.CseResponse;
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

    public Message buildMessage(CseRequest cseRequest, int priority) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(cseRequest))
                .andProperties(buildMessageProperties(priority))
                .build();
    }

    private MessageProperties buildMessageProperties(int priority) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(clientProperties.getAmqp().getApplicationId())
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(clientProperties.getAmqp().getExpiration())
                .setPriority(priority)
                .build();
    }

    public CseResponse readMessage(Message message) {
        if (message != null) {
            return jsonConverter.fromJsonMessage(message.getBody(), CseResponse.class);
        } else {
            throw new CseInternalException("Cse server did not respond");
        }
    }
}
