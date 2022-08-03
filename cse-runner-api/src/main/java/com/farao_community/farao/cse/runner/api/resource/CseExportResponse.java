package com.farao_community.farao.cse.runner.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Type("cse-export-response")
@Data
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CseExportResponse {
    @Id
    String id;
    String ttcFileUrl;
    String finalCgmFileUrl;
    String logsFileUrl;

    @JsonCreator
    public CseExportResponse(@JsonProperty("id") String id, @JsonProperty("ttcFileUrl") String ttcFileUrl, @JsonProperty("finalCgmFileUrl") String finalCgmFileUrl, @JsonProperty("logsFileUrl") String logsFileUrl) {
        this.id = id;
        this.ttcFileUrl = ttcFileUrl;
        this.finalCgmFileUrl = finalCgmFileUrl;
        this.logsFileUrl = logsFileUrl;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
