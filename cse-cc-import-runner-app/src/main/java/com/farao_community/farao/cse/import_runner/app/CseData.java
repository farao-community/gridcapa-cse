/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app;

import com.farao_community.farao.cse.data.CseReferenceExchanges;
import com.farao_community.farao.cse.data.ntc.Ntc;
import com.farao_community.farao.cse.data.ntc2.Ntc2;
import com.farao_community.farao.cse.data.target_ch.LineFixedFlows;
import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;

import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CseData {
    private final CseRequest cseRequest;
    private final FileImporter fileImporter;

    private Ntc ntc;
    private Map<String, Double> reducedSplittingFactors;
    private Double mniiOffset;
    private CseReferenceExchanges cseReferenceExchanges; // only for IDCC process
    private Ntc2 ntc2; // only for IDCC process
    private String jsonCracUrl;
    private String preProcesedNetworkUrl;
    private LineFixedFlows lineFixedFlows; // only for D2CC process
    private Map<String, Double> ntcPerCountry;

    public CseData(CseRequest cseRequest, FileImporter fileImporter) {
        this.cseRequest = cseRequest;
        this.fileImporter = fileImporter;
    }

    public Map<String, Double> getReducedSplittingFactors() {
        if (reducedSplittingFactors == null) {
            reducedSplittingFactors = getNtc().computeReducedSplittingFactors();
        }
        return reducedSplittingFactors;
    }

    public LineFixedFlows getLineFixedFlows() {
        if (cseRequest.getProcessType() == ProcessType.IDCC) {
            throw new CseInternalException("Impossible to retrieve line fixed flows for IDCC process. No target CH file available");
        }
        if (lineFixedFlows == null) {
            lineFixedFlows = fileImporter.importLineFixedFlowFromTargetChFile(
                    cseRequest.getTargetProcessDateTime(), cseRequest.getTargetChUrl(), cseRequest.isImportAdaptedProcess());
        }
        return lineFixedFlows;
    }

    public Double getMniiOffset() {
        if (mniiOffset != null) {
            return mniiOffset;
        }
        mniiOffset = getNtc().computeMniiOffset();
        return mniiOffset;
    }

    public Map<String, Double> getNtcPerCountry() {
        if (ntcPerCountry != null) {
            return ntcPerCountry;
        }
        ntcPerCountry = getNtc().getNtcPerCountry();
        return ntcPerCountry;
    }

    public Ntc getNtc() {
        if (ntc != null) {
            return ntc;
        }
        ntc = fileImporter.importNtc(
            cseRequest.getTargetProcessDateTime(),
            cseRequest.getYearlyNtcUrl(),
            cseRequest.getNtcReductionsUrl());
        return ntc;
    }

    public Ntc2 getNtc2() {
        if (cseRequest.getProcessType() == ProcessType.D2CC) {
            throw new CseInternalException("Impossible to retrieve NTC2 for D2CC process. No NTC2 files available");
        }
        if (ntc2 != null) {
            return ntc2;
        }
        ntc2 = fileImporter.importNtc2(
            cseRequest.getTargetProcessDateTime(),
            cseRequest.getNtc2AtItUrl(),
            cseRequest.getNtc2ChItUrl(),
            cseRequest.getNtc2FrItUrl(),
            cseRequest.getNtc2SiItUrl());
        return ntc2;
    }

    public CseReferenceExchanges getCseReferenceExchanges() {
        if (cseRequest.getProcessType() == ProcessType.D2CC) {
            throw new CseInternalException("Impossible to retrieve reference exchanges for D2CC process. No vulcanus file available");
        }
        if (cseReferenceExchanges != null) {
            return cseReferenceExchanges;
        }
        cseReferenceExchanges = fileImporter.importCseReferenceExchanges(
            cseRequest.getTargetProcessDateTime(),
            cseRequest.getVulcanusUrl());
        return  cseReferenceExchanges;
    }

    public String getJsonCracUrl() {
        return jsonCracUrl;
    }

    public void setJsonCracUrl(String jsonCracUrl) {
        this.jsonCracUrl = jsonCracUrl;
    }

    public String getPreProcesedNetworkUrl() {
        return preProcesedNetworkUrl;
    }

    public void setPreProcesedNetworkUrl(String preProcesedNetworkUrl) {
        this.preProcesedNetworkUrl = preProcesedNetworkUrl;
    }

}
