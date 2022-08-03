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
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Type("cse-request")
@Data
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CseRequest {
    @Id
    String id;
    ProcessType processType;
    OffsetDateTime targetProcessDateTime;
    String cgmUrl;
    String mergedCracUrl;
    String mergedGlskUrl;
    String ntcReductionsUrl;
    String ntc2AtItUrl;
    String ntc2ChItUrl;
    String ntc2FrItUrl;
    String ntc2SiItUrl;
    String targetChUrl;
    String vulcanusUrl;
    String yearlyNtcUrl;
    List<String> manualForcedPrasIds;
    Map<String, List<Set<String>>> automatedForcedPrasIds;
    double dichotomyPrecision;
    double initialDichotomyStep;
    Double initialDichotomyIndex;

    @JsonCreator
    public CseRequest(@JsonProperty("id") String id,
                      @JsonProperty("processType") ProcessType processType,
                      @JsonProperty("targetProcessDateTime") OffsetDateTime targetProcessDateTime,
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
                      @JsonProperty("yearlyNtcUrl") String yearlyNtcUrl,
                      @JsonProperty("manualForcedPrasIds") List<String> manualForcedPrasIds,
                      @JsonProperty("automatedForcedPras") Map<String, List<Set<String>>> automatedForcedPrasIds,
                      @JsonProperty("dichotomyPrecision") double dichotomyPrecision,
                      @JsonProperty("initialDichotomyStep") double initialDichotomyStep,
                      @JsonProperty("initialDichotomyIndex") Double initialDichotomyIndex) {
        this.id = id;
        this.processType = processType;
        this.targetProcessDateTime = targetProcessDateTime;
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
        this.manualForcedPrasIds = manualForcedPrasIds;
        this.automatedForcedPrasIds = automatedForcedPrasIds;
        this.dichotomyPrecision = dichotomyPrecision;
        this.initialDichotomyStep = initialDichotomyStep;
        this.initialDichotomyIndex = initialDichotomyIndex;
    }

    public static CseRequest d2ccProcess(String id,
                                         OffsetDateTime targetProcessDateTime,
                                         String cgmUrl,
                                         String mergedCracUrl,
                                         String mergedGlskUrl,
                                         String ntcReductionsUrl,
                                         String targetChUrl,
                                         String yearlyNtcUrl,
                                         List<String> manualFrcedPrasIds,
                                         Map<String, List<Set<String>>> automatedForcedPras,
                                         double dichotomyPrecision,
                                         double initialDichotomyStep,
                                         Double initialDichotomyIndex) {
        return new CseRequest(
            id, ProcessType.D2CC, targetProcessDateTime, cgmUrl, mergedCracUrl, mergedGlskUrl, ntcReductionsUrl, null,
            null, null, null, targetChUrl, null, yearlyNtcUrl, manualFrcedPrasIds,
            automatedForcedPras, dichotomyPrecision, initialDichotomyStep, initialDichotomyIndex);
    }

    public static CseRequest idccProcess(String id,
                                         OffsetDateTime targetProcessDateTime,
                                         String cgmUrl,
                                         String mergedCracUrl,
                                         String mergedGlskUrl,
                                         String ntcReductionsUrl,
                                         String ntc2AtItUrl,
                                         String ntc2ChItUrl,
                                         String ntc2FrItUrl,
                                         String ntc2SiItUrl,
                                         String vulcanusUrl,
                                         String yearlyNtcUrl,
                                         List<String> manualForcedPrasIds,
                                         Map<String, List<Set<String>>> automatedForcedPras,
                                         double dichotomyPrecision,
                                         double initialDichotomyStep,
                                         Double initialDichotomyIndex) {
        return new CseRequest(id, ProcessType.IDCC, targetProcessDateTime, cgmUrl, mergedCracUrl, mergedGlskUrl, ntcReductionsUrl,
            ntc2AtItUrl, ntc2ChItUrl, ntc2FrItUrl, ntc2SiItUrl, null, vulcanusUrl, yearlyNtcUrl, manualForcedPrasIds,
            automatedForcedPras, dichotomyPrecision, initialDichotomyStep, initialDichotomyIndex);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
