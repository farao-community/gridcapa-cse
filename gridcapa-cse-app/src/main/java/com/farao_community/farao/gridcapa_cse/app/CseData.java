/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_cse.app;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse.data.CseReferenceExchanges;
import com.farao_community.farao.cse.data.ntc.Ntc;
import com.farao_community.farao.cse.data.ntc2.Ntc2;
import com.farao_community.farao.gridcapa_cse.api.resource.CseRequest;
import com.farao_community.farao.gridcapa_cse.app.services.FileImporter;
import com.powsybl.iidm.network.Country;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CseData {
    private final CseRequest cseRequest;
    private final FileImporter fileImporter;

    private Ntc ntc;
    private Map<String, Double> reducedSplittingFactors;
    private Double mniiOffset;
    private CseReferenceExchanges cseReferenceExchanges;
    private Ntc2 ntc2;
    private String jsonCracUrl;
    private String preProcesedNetworkUrl;

    public CseData(CseRequest cseRequest, FileImporter fileImporter) {
        this.cseRequest = cseRequest;
        this.fileImporter = fileImporter;
    }

    public Map<String, Double> getReducedSplittingFactors() {
        if (reducedSplittingFactors != null) {
            return reducedSplittingFactors;
        }
        reducedSplittingFactors = convertSplittingFactors(getNtc().computeReducedSplittingFactors());
        return reducedSplittingFactors;
    }

    public Double getMniiOffset() {
        if (mniiOffset != null) {
            return mniiOffset;
        }
        mniiOffset = getNtc().computeMniiOffset();
        return mniiOffset;
    }

    private Ntc getNtc() {
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

    private Map<String, Double> convertSplittingFactors(Map<String, Double> tSplittingFactors) {
        Map<String, Double> splittingFactors = new TreeMap<>();
        tSplittingFactors.forEach((key, value) -> splittingFactors.put(toEic(key), value));
        splittingFactors.put(toEic("IT"), -splittingFactors.values().stream().reduce(0., Double::sum));
        return splittingFactors;
    }

    private String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }
}
