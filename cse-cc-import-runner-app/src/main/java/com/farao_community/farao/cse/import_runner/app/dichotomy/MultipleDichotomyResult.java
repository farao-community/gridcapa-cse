package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class MultipleDichotomyResult {
    List<Pair<Set<String>, DichotomyResult<RaoResponse>>> dichotomyHistory = new ArrayList<>();

    // Stores a new increasing TTC dichotomy result alongside the set of forced PRAs to keep track of them
    public void addResult(DichotomyResult<RaoResponse> initialDichotomyResult, Set<String> forcedPras) {
        dichotomyHistory.add(Pair.of(forcedPras, initialDichotomyResult));
    }

    public String getBestNetworkUrl() {
        return getBestDichotomyResult().getHighestValidStep().getValidationData().getNetworkWithPraFileUrl();
    }

    public DichotomyResult<RaoResponse> getBestDichotomyResult() {
        return dichotomyHistory.get(dichotomyHistory.size() - 1).getRight();
    }

    public Set<String> getBestForcedPrasIds() {
        return dichotomyHistory.get(dichotomyHistory.size() - 1).getLeft();
    }
}
