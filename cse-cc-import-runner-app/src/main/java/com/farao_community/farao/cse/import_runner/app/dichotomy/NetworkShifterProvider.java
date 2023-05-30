/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class NetworkShifterProvider {
    private static final double SHIFT_TOLERANCE = 1;

    private final ZonalScalableProvider zonalScalableProvider;
    private final Logger businessLogger;

    public NetworkShifterProvider(ZonalScalableProvider zonalScalableProvider, Logger businessLogger) {
        this.zonalScalableProvider = zonalScalableProvider;
        this.businessLogger = businessLogger;
    }

    public NetworkShifter get(CseRequest request,
                              CseData cseData,
                              Network network,
                              Map<String, Double> referenceExchanges,
                              Map<String, Double> ntcsByEic) throws IOException {
        return new LinearScaler(
            zonalScalableProvider.get(request.getMergedGlskUrl(), network, request.getProcessType()),
            getShiftDispatcher(request.getProcessType(), cseData, referenceExchanges, ntcsByEic),
            SHIFT_TOLERANCE);
    }

    private ShiftDispatcher getShiftDispatcher(ProcessType processType, CseData cseData, Map<String, Double> referenceExchanges, Map<String, Double> ntcsByEic) {
        if (processType.equals(ProcessType.D2CC)) {
            return new CseD2ccShiftDispatcher(
                businessLogger,
                NetworkShifterUtil.convertMapByCountryToMapByEic(cseData.getReducedSplittingFactors(ntcsByEic)),
                ntcsByEic);
        } else {
            return new CseIdccShiftDispatcher(
                businessLogger,
                ntcsByEic,
                NetworkShifterUtil.convertMapByCountryToMapByEic(cseData.getReducedSplittingFactors(ntcsByEic)),
                referenceExchanges);
        }
    }
}
