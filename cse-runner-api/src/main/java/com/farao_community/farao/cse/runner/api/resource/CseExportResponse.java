package com.farao_community.farao.cse.runner.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Type("cse-export-response")
public class CseExportResponse {
    @Id
    private final String id;
    private final String ttcFileUrl;
    private final String finalCgmFileUrl;
    private final boolean interrupted;

    @JsonCreator
    public CseExportResponse(@JsonProperty("id") String id, @JsonProperty("ttcFileUrl") String ttcFileUrl, @JsonProperty("finalCgmFileUrl") String finalCgmFileUrl, @JsonProperty("interrupted") boolean interrupted) {
        this.id = id;
        this.ttcFileUrl = ttcFileUrl;
        this.finalCgmFileUrl = finalCgmFileUrl;
        this.interrupted = interrupted;
    }

    public String getId() {
        return id;
    }

    public String getTtcFileUrl() {
        return ttcFileUrl;
    }

    public String getFinalCgmFileUrl() {
        return finalCgmFileUrl;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
