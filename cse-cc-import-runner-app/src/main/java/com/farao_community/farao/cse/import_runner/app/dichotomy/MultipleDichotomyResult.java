package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MultipleDichotomyResult<I> {
    private final List<Pair<Set<String>, DichotomyResult<I>>> dichotomyHistory = new ArrayList<>();
    private boolean interrupted = false;

    // Stores a new increasing TTC dichotomy result alongside the set of forced PRAs to keep track of them
    public void addResult(DichotomyResult<I> dichotomyResult, Set<String> forcedPras) {
        dichotomyHistory.add(Pair.of(forcedPras, dichotomyResult));
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    public DichotomyResult<I> getBestDichotomyResult() {
        return dichotomyHistory.get(dichotomyHistory.size() - 1).getRight();
    }

    public Set<String> getBestForcedPrasIds() {
        return dichotomyHistory.get(dichotomyHistory.size() - 1).getLeft();
    }

    public boolean isInterrupted() {
        return interrupted;
    }
}
