/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.util;

import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Supplier;

public class UpdateStatusProducer implements Supplier<Flux<Message<TaskStatusUpdate>>> {

    private Sinks.Many<Message<TaskStatusUpdate>> sink = Sinks.many().unicast().onBackpressureBuffer();

    public void produce(TaskStatusUpdate event) {
        Message<TaskStatusUpdate> message = MessageBuilder
                .withPayload(event)
                .build();
        sink.emitNext(message, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    @Override
    public Flux<Message<TaskStatusUpdate>> get() {
        return sink.asFlux();
    }

}
