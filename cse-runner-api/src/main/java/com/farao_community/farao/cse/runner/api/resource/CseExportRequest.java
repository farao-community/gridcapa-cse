/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.OffsetDateTime;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Type("cse-export-request")
public class CseExportRequest {
    @Id
    private final String id;
    private final String currentRunId;
    private final ProcessType processType;
    private final OffsetDateTime targetProcessDateTime;
    private final String cgmUrl;
    private final String mergedCracUrl;

    @JsonCreator
    public CseExportRequest(@JsonProperty("id") String id,
                            @JsonProperty("currentRunId") String currentRunId,
                            @JsonProperty("targetProcessDateTime") OffsetDateTime targetProcessDateTime,
                            @JsonProperty("processType") ProcessType processType,
                            @JsonProperty("cgmUrl") String cgmUrl,
                            @JsonProperty("mergedCracUrl") String mergedCracUrl) {
        this.id = id;
        this.currentRunId = currentRunId;
        this.targetProcessDateTime = targetProcessDateTime;
        this.processType = processType;
        this.cgmUrl = cgmUrl;
        this.mergedCracUrl = mergedCracUrl;
    }

    public String getId() {
        return id;
    }

    public String getCurrentRunId() {
        return currentRunId;
    }

    public String getCgmUrl() {
        return cgmUrl;
    }

    public String getMergedCracUrl() {
        return mergedCracUrl;
    }

    public OffsetDateTime getTargetProcessDateTime() {
        return targetProcessDateTime;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
