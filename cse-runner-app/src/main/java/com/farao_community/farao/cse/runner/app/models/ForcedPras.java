package com.farao_community.farao.cse.runner.app.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ForcedPras {
    private final double initialDichotomyStep;
    private final List<String> remedialActionsIds;

    @JsonCreator
    public ForcedPras(@JsonProperty("initialDichotomyStep") double initialDichotomyStep, @JsonProperty("remedialActionsIds") List<String> remedialActionsIds) {
        this.initialDichotomyStep = initialDichotomyStep;
        this.remedialActionsIds = remedialActionsIds;
    }

    public double getInitialDichotomyStep() {
        return initialDichotomyStep;
    }

    public List<String> getRemedialActionsIds() {
        return remedialActionsIds;
    }
}
