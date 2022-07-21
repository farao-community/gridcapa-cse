package com.farao_community.farao.cse.import_runner.app.services;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class InterruptionService {

    public void interruption(String taskId) {
        Optional<Thread> thread = isRunning(taskId);
        while (thread.isPresent()) {
            thread.get().interrupt();
            thread = isRunning(taskId);
        }
    }

    private Optional<Thread> isRunning(String id) {
        return Thread.getAllStackTraces()
                .keySet()
                .stream()
                .filter(t -> t.getName().equals(id))
                .findFirst();
    }


}
