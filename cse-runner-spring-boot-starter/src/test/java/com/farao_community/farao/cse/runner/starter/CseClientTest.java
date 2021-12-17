/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.starter;

import com.farao_community.farao.cse.runner.api.JsonApiConverter;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
class CseClientTest {
    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    @Test
    void checkThatCseClientHandlesMessagesCorrectly() throws IOException {
        AmqpTemplate amqpTemplate = Mockito.mock(AmqpTemplate.class);
        CseClient cseClient = new CseClient(amqpTemplate, buildProperties());
        CseRequest cseRequest = jsonApiConverter.fromJsonMessage(getClass().getResourceAsStream("/cseRequestMessage.json").readAllBytes(), CseRequest.class);
        Message responseMessage = Mockito.mock(Message.class);

        Mockito.when(responseMessage.getBody()).thenReturn(getClass().getResourceAsStream("/cseResponseMessage.json").readAllBytes());
        Mockito.when(amqpTemplate.sendAndReceive(Mockito.same("my-exchange"), Mockito.same("#"), Mockito.any())).thenReturn(responseMessage);
        CseResponse cseResponse = cseClient.run(cseRequest);

        assertEquals("ttcFileUrl", cseResponse.getTtcFileUrl());
    }

    private CseClientProperties buildProperties() {
        CseClientProperties properties = new CseClientProperties();
        CseClientProperties.BindingConfiguration bindingConfiguration = new CseClientProperties.BindingConfiguration();
        bindingConfiguration.setDestination("my-exchange");
        bindingConfiguration.setRoutingKey("#");
        bindingConfiguration.setExpiration("60000");
        bindingConfiguration.setApplicationId("application-id");
        properties.setBinding(bindingConfiguration);
        return properties;
    }
}
