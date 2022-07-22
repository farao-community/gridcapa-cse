package com.farao_community.farao.cse.import_runner.app.configurations;

import com.farao_community.farao.cse.import_runner.app.util.UpdateStatusProducer;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Sinks;

@Configuration
public class UpdateStatusConfiguration {

    private Sinks.Many<Message<TaskStatusUpdate>> processor = Sinks.many().multicast().onBackpressureBuffer();

    @Bean("notification-producer")
    UpdateStatusProducer updateStatusProducer() {
        return new UpdateStatusProducer();
    }

}
