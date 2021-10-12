package com.farao_community.farao.gridcapa.api;

import com.farao_community.farao.gridcapa_cse.api.JsonApiConverter;
import com.farao_community.farao.gridcapa_cse.api.exception.CseInternalException;
import com.farao_community.farao.gridcapa_cse.api.resource.CseRequest;
import com.farao_community.farao.gridcapa_cse.api.resource.CseResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonApiConverterTest {
    @Test
    void checkCseRequestJsonConversion() throws IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        byte[] requestBytes = getClass().getResourceAsStream("/cseRequestMessage.json").readAllBytes();
        CseRequest request = jsonApiConverter.fromJsonMessage(requestBytes, CseRequest.class);

        assertEquals("id", request.getId());
        assertEquals("networkFileUrl", request.getCgmUrl());
    }

    @Test
    void checkCseResponseJsonConversion() throws IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        byte[] responseBytes = getClass().getResourceAsStream("/cseResponseMessage.json").readAllBytes();
        CseResponse response = jsonApiConverter.fromJsonMessage(responseBytes, CseResponse.class);

        assertEquals("id", response.getId());
        assertEquals("ttcFileUrl", response.getTtcFileUrl());

    }

    @Test
    void checkExceptionJsonConversion() throws URISyntaxException, IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        CseInternalException exception = new CseInternalException("Something really bad happened");
        String expectedExceptionMessage = Files.readString(Paths.get(getClass().getResource("/errorMessage.json").toURI()));
        assertEquals(expectedExceptionMessage, new String(jsonApiConverter.toJsonMessage(exception)));

    }
}
