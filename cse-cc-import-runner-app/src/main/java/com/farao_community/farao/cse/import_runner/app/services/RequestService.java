package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.import_runner.app.util.GenericThreadLauncher;
import com.farao_community.farao.cse.import_runner.app.util.UpdateStatusProducer;
import com.farao_community.farao.cse.runner.api.JsonApiConverter;
import com.farao_community.farao.cse.runner.api.exception.AbstractCseException;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.CseResponse;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestService.class);
    @Autowired
    private CseRunner cseServer;
    @Autowired
    private Logger businessLogger;
    @Autowired
    private UpdateStatusProducer updateStatusProducer;
    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    public byte[] launchCseRequest(byte[] req) {
        byte[] result = new byte[1];
        CseRequest cseRequest = jsonApiConverter.fromJsonMessage(req, CseRequest.class);
        // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
        // This should be done only once, as soon as the information to add in mdc is available.
        MDC.put("gridcapa-task-id", cseRequest.getId());
        try {
            updateStatusProducer.produce(new TaskStatusUpdate(UUID.fromString(cseRequest.getId()), TaskStatus.RUNNING));
            LOGGER.info("Cse request received : {}", cseRequest);
            GenericThreadLauncher launcher = new GenericThreadLauncher<CseRunner, CseResponse>(cseServer, cseRequest.getId());
            launcher.launch(cseRequest);
            launcher.join();
            CseResponse response = (CseResponse) launcher.getResult();
            result = sendCseResponse(response);
            LOGGER.info("Cse response sent: {}", response);
        } catch (InterruptedException e) {
            result = handleError(e, cseRequest.getId());
        } catch (Exception e) {
            result = handleError(e, cseRequest.getId());
        }

        return result;
    }

    private byte[] sendCseResponse(CseResponse cseResponse) {
        updateStatusProducer.produce(new TaskStatusUpdate(UUID.fromString(cseResponse.getId()), TaskStatus.SUCCESS));
        return jsonApiConverter.toJsonMessage(cseResponse, CseResponse.class);
    }

    private byte[] handleError(Exception e, String requestId) {
        AbstractCseException cseException = new CseInternalException("CSE run failed", e);
        LOGGER.error(cseException.getDetails(), cseException);
        businessLogger.error(cseException.getDetails());
        return sendErrorResponse(requestId, cseException);
    }

    private byte[] sendErrorResponse(String requestId, AbstractCseException exception) {

        if (exception.getCause().getClass() == InterruptedException.class) {
            updateStatusProducer.produce(new TaskStatusUpdate(UUID.fromString(requestId), TaskStatus.INTERRUPTED));
        } else {
            updateStatusProducer.produce(new TaskStatusUpdate(UUID.fromString(requestId), TaskStatus.ERROR));
        }
        return exceptionToJsonMessage(exception);

    }

    private byte[] exceptionToJsonMessage(AbstractCseException e) {
        return jsonApiConverter.toJsonMessage(e);
    }

}
