/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MultipleDichotomyResult<I> {
    List<Pair<Set<String>, DichotomyResult<I>>> dichotomyHistory = new ArrayList<>();
    private boolean interrupted = false;
    private boolean raoFailed = false;

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

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    public boolean isRaoFailed() {
        return raoFailed;
    }

    public void setRaoFailed(final boolean raoFailed) {
        this.raoFailed = raoFailed;
    }
}
