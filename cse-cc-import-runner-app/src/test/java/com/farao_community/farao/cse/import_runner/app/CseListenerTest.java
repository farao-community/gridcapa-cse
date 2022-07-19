package com.farao_community.farao.cse.import_runner.app;

import com.farao_community.farao.cse.import_runner.app.configurations.AmqpConfiguration;
import com.farao_community.farao.cse.import_runner.app.services.CseRunner;
import com.farao_community.farao.cse.runner.api.JsonApiConverter;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.amqp.core.MessageBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
class CseListenerTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    @MockBean
    private RabbitMessagingTemplate messagingTemplate;

    @MockBean
    private StreamBridge streamBridge;
    @MockBean
    private AmqpConfiguration amqpConfiguration;

    @MockBean
    private CseRunner cseServer;
    @MockBean
    private Logger businessLogger;

    @Autowired
    CseListener cseListener;

    @BeforeEach
    public void setup() {
        messagingTemplate = mock(RabbitMessagingTemplate.class);
    }

    @Test
    void testReceivedNewRequest() throws IOException {
        Message amqpMsg = createMessage();
        Mockito.when(cseServer.run(any())).thenReturn(getCseResponse());
        Mockito.doNothing().when(messagingTemplate).send(any());
        cseListener.onMessage(amqpMsg);
        ArgumentCaptor<String> replyToArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Message> messageArgument = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(replyToArgument.capture(), messageArgument.capture());
        assertEquals("replyTo", replyToArgument.getValue());
        assertEquals(new String(messageArgument.getValue().getBody()), getBodyResponseOK());
    }

    @Test
    void testInterruption() throws IOException {
        Message amqpMsg = createMessage();
        Mockito.when(cseServer.run(any())).thenThrow(createInterruptException());
        Mockito.doNothing().when(messagingTemplate).send(any());
        cseListener.onMessage(amqpMsg);
        ArgumentCaptor<String> replyToArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Message> messageArgument = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(replyToArgument.capture(), messageArgument.capture());
        assertEquals("replyTo", replyToArgument.getValue());
        assertEquals(new String(messageArgument.getValue().getBody()), getBodyResponseNOK());
    }

    private Message createMessage() throws IOException {

        String file = getClass().getResource("/request.json").getPath();
        String payload = new String(Files.readAllBytes(Paths.get(file)));
        return MessageBuilder
                .withBody(payload.getBytes())
                .andProperties(MessagePropertiesBuilder.newInstance()
                        .setCorrelationId("correlationId")
                        .setReplyTo("replyTo")
                        .build()
                )
                .build();
    }

    private CseResponse getCseResponse() throws IOException {

        String file = getClass().getResource("/response_OK.json").getPath();
        String payload = new String(Files.readAllBytes(Paths.get(file)));
        return jsonApiConverter.fromJsonMessage(payload.getBytes(), CseResponse.class);
    }

    private String getBodyResponseOK() throws IOException {
        String file = getClass().getResource("/response_OK.json").getPath();
        return new String(Files.readAllBytes(Paths.get(file)));
    }

    private String getBodyResponseNOK() throws IOException {
        String file = getClass().getResource("/response_NOK.json").getPath();
        return new String(Files.readAllBytes(Paths.get(file)));
    }

    private IOException createInterruptException() {
        return new IOException(new InterruptedException("interrupted"));
    }
}
