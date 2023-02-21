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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Type("cse-request")
public class CseRequest {
    @Id
    private final String id;
    private final ProcessType processType;
    private final OffsetDateTime targetProcessDateTime;
    private final String cgmUrl;
    private final String mergedCracUrl;
    private final String mergedGlskUrl;
    private final String ntcReductionsUrl;
    private final String ntc2AtItUrl;
    private final String ntc2ChItUrl;
    private final String ntc2FrItUrl;
    private final String ntc2SiItUrl;
    private final String targetChUrl;
    private final String vulcanusUrl;
    private final String yearlyNtcUrl;
    private final List<String> manualForcedPrasIds;
    private final Map<String, List<Set<String>>> automatedForcedPrasIds;
    private final Integer maximumDichotomiesNumber;
    private final double dichotomyPrecision;
    private final double initialDichotomyStep;
    private final Double initialDichotomyIndex;

    private final boolean importAdaptedProcess;

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
                      @JsonProperty("maximumDichotomiesNumber") Integer maximumDichotomiesNumber,
                      @JsonProperty("dichotomyPrecision") double dichotomyPrecision,
                      @JsonProperty("initialDichotomyStep") double initialDichotomyStep,
                      @JsonProperty("initialDichotomyIndex") Double initialDichotomyIndex,
                      @JsonProperty("importAdaptedProcess") boolean importAdaptedProcess) {
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
        this.maximumDichotomiesNumber = maximumDichotomiesNumber;
        this.dichotomyPrecision = dichotomyPrecision;
        this.initialDichotomyStep = initialDichotomyStep;
        this.initialDichotomyIndex = initialDichotomyIndex;
        this.importAdaptedProcess = importAdaptedProcess;
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
                                         Integer maximumDichotomiesNumber,
                                         double dichotomyPrecision,
                                         double initialDichotomyStep,
                                         Double initialDichotomyIndex,
                                         boolean isAdapted) {
        return new CseRequest(
            id, ProcessType.D2CC, targetProcessDateTime, cgmUrl, mergedCracUrl, mergedGlskUrl, ntcReductionsUrl, null,
            null, null, null, targetChUrl, null, yearlyNtcUrl, manualFrcedPrasIds,
            automatedForcedPras, maximumDichotomiesNumber, dichotomyPrecision, initialDichotomyStep, initialDichotomyIndex, isAdapted);
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
                                         Integer maximumDichotomiesNumber,
                                         double dichotomyPrecision,
                                         double initialDichotomyStep,
                                         Double initialDichotomyIndex,
                                         boolean isAdapted) {
        return new CseRequest(id, ProcessType.IDCC, targetProcessDateTime, cgmUrl, mergedCracUrl, mergedGlskUrl, ntcReductionsUrl,
            ntc2AtItUrl, ntc2ChItUrl, ntc2FrItUrl, ntc2SiItUrl, null, vulcanusUrl, yearlyNtcUrl, manualForcedPrasIds,
            automatedForcedPras, maximumDichotomiesNumber, dichotomyPrecision, initialDichotomyStep, initialDichotomyIndex, isAdapted);
    }

    public String getId() {
        return id;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public OffsetDateTime getTargetProcessDateTime() {
        return targetProcessDateTime;
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

    public List<String> getManualForcedPrasIds() {
        return manualForcedPrasIds;
    }

    public Integer getMaximumDichotomiesNumber() {
        return maximumDichotomiesNumber;
    }

    public Map<String, List<Set<String>>> getAutomatedForcedPrasIds() {
        return automatedForcedPrasIds;
    }

    public double getDichotomyPrecision() {
        return dichotomyPrecision;
    }

    public double getInitialDichotomyStep() {
        return initialDichotomyStep;
    }

    public Double getInitialDichotomyIndex() {
        return initialDichotomyIndex;
    }

    public boolean isImportAdaptedProcess() {
        return importAdaptedProcess;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
