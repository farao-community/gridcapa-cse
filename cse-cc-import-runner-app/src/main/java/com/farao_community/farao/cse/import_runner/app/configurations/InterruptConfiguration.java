package com.farao_community.farao.cse.import_runner.app.configurations;

import com.farao_community.farao.cse.import_runner.app.services.InterruptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class InterruptConfiguration {

    @Autowired
    InterruptionService interruptionService;

    @Bean
    public Consumer<String> interruptionConsumer () {
        return (taskId) -> interruptionService.interruption(taskId);
    }
}
