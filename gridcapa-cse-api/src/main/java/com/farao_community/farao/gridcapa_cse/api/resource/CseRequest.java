/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Type("cse-request")
public class CseRequest {
    @Id
    private String id;
    private final String instant;
    private String cgmUrl;
    private String mergedCracUrl;
    private String mergedGlskUrl;
    private String ntcReductionsUrl;
    private String ntc2AtItUrl;
    private String ntc2ChItUrl;
    private String ntc2FrItUrl;
    private String ntc2SiItUrl;
    private String targetChUrl;
    private String vulcanusUrl;
    private String yearlyNtcUrl;

    @JsonCreator
    public CseRequest(@JsonProperty("id") String id,
                      @JsonProperty("instant") String instant,
                      @JsonProperty("cgmUrl") String cgmUrl,
                      @JsonProperty("mergedCracUrl") String mergedCracUrl,
                      @JsonProperty("mergedGlskUrl") String mergedGlskUrl,
                      @JsonProperty("ntcReductionsUrl") String ntcReductionsUrl,
                      @JsonProperty("ntc2AtItUrl") String ntc2AtItUrl,
                      @JsonProperty("ntc2ChItUrl") String ntc2ChItUrl,
                      @JsonProperty("ntc2FrItUrl") String ntc2FrItUrl,
                      @JsonProperty("ntc2SiItUrl") String ntc2SiItUrl,
                      @JsonProperty("targetChUrl") String targetChUrl,
                      @JsonProperty("vulcanusUrl") String vulcanusUrl,
                      @JsonProperty("yearlyNtcUrl") String yearlyNtcUrl) {
        this.id = id;
        this.instant = instant;
        this.cgmUrl = cgmUrl;
        this.mergedCracUrl = mergedCracUrl;
        this.mergedGlskUrl = mergedGlskUrl;
        this.ntc2AtItUrl = ntc2AtItUrl;
        this.ntc2ChItUrl = ntc2ChItUrl;
        this.ntc2FrItUrl = ntc2FrItUrl;
        this.ntc2SiItUrl = ntc2SiItUrl;
        this.ntcReductionsUrl = ntcReductionsUrl;
        this.targetChUrl = targetChUrl;
        this.vulcanusUrl = vulcanusUrl;
        this.yearlyNtcUrl = yearlyNtcUrl;
    }

    public CseRequest(@JsonProperty("id") String id,
                      @JsonProperty("instant") String instant,
                      @JsonProperty("cgmUrl") String cgmUrl,
                      @JsonProperty("mergedCracUrl") String mergedCracUrl,
                      @JsonProperty("mergedGlskUrl") String mergedGlskUrl,
                      @JsonProperty("ntcReductionsUrl") String ntcReductionsUrl,
                      @JsonProperty("vulcanusUrl") String vulcanusUrl,
                      @JsonProperty("yearlyNtcUrl") String yearlyNtcUrl) {
        this(id, instant, cgmUrl, mergedCracUrl, mergedGlskUrl, ntcReductionsUrl, null, null, null, null, null, vulcanusUrl, yearlyNtcUrl);
    }

    public String getId() {
        return id;
    }

    public String getInstant() {
        return instant;
    }

    public String getCgmUrl() {
        return cgmUrl;
    }

    public String getMergedCracUrl() {
        return mergedCracUrl;
    }

    public String getMergedGlskUrl() {
        return mergedGlskUrl;
    }

    public String getNtcReductionsUrl() {
        return ntcReductionsUrl;
    }

    public String getNtc2AtItUrl() {
        return ntc2AtItUrl;
    }

    public String getNtc2ChItUrl() {
        return ntc2ChItUrl;
    }

    public String getNtc2FrItUrl() {
        return ntc2FrItUrl;
    }

    public String getNtc2SiItUrl() {
        return ntc2SiItUrl;
    }

    public String getTargetChUrl() {
        return targetChUrl;
    }

    public String getVulcanusUrl() {
        return vulcanusUrl;
    }

    public String getYearlyNtcUrl() {
        return yearlyNtcUrl;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
