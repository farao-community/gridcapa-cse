package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class MultipleDichotomyResult<I> {
    List<Pair<Set<String>, DichotomyResult<I>>> dichotomyHistory = new ArrayList<>();

    // Stores a new increasing TTC dichotomy result alongside the set of forced PRAs to keep track of them
    public void addResult(DichotomyResult<I> initialDichotomyResult, Set<String> forcedPras) {
        dichotomyHistory.add(Pair.of(forcedPras, initialDichotomyResult));
    }

    public DichotomyResult<I> getBestDichotomyResult() {
        return dichotomyHistory.get(dichotomyHistory.size() - 1).getRight();
    }

    public Set<String> getBestForcedPrasIds() {
        return dichotomyHistory.get(dichotomyHistory.size() - 1).getLeft();
    }
}
