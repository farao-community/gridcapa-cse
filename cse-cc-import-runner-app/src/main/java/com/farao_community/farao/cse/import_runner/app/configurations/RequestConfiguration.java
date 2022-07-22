package com.farao_community.farao.cse.import_runner.app.configurations;

import com.farao_community.farao.cse.import_runner.app.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Configuration
public class RequestConfiguration {

    @Autowired
    RequestService requestService;

    @Bean
    public Function<Flux<byte[]>, Flux<byte[]>> requestProcessor() {
        return cseRequestFlux -> cseRequestFlux
                .map(s -> requestService.launchCseRequest(s))
                .log();
    }

}
