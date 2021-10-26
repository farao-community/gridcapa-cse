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
import com.farao_community.farao.gridcapa_cse.api.exception.CseInvalidDataException;
import com.farao_community.farao.gridcapa_cse.api.resource.CseRequest;
import com.farao_community.farao.gridcapa_cse.app.services.UrlValidationService;
import com.powsybl.iidm.network.Country;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CseData {
    private final CseRequest cseRequest;
    private final UrlValidationService urlValidationService;

    private Ntc ntc;
    private Map<String, Double> reducedSplittingFactors;
    private Double mniiOffset;
    private CseReferenceExchanges cseReferenceExchanges;
    private Ntc2 ntc2;
    private String jsonCracUrl;
    private String preProcesedNetworkUrl;

    public CseData(CseRequest cseRequest, UrlValidationService urlValidationService) {
        this.cseRequest = cseRequest;
        this.urlValidationService = urlValidationService;
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
        try (InputStream yearlyNtcStream = urlValidationService.openUrlStream(cseRequest.getYearlyNtcUrl());
             InputStream dailyNtcStream = urlValidationService.openUrlStream(cseRequest.getNtcReductionsUrl())) {
            ntc = Ntc.create(cseRequest.getTargetProcessDateTime(), yearlyNtcStream, dailyNtcStream);
            return ntc;
        } catch (IOException | JAXBException e) {
            throw new CseInvalidDataException("Impossible to create NTC", e);
        }
    }

    public Ntc2 getNtc2() {
        if (ntc2 != null) {
            return ntc2;
        }
        try (InputStream ntc2ChItStream = urlValidationService.openUrlStream(cseRequest.getNtc2ChItUrl());
             InputStream ntc2AtItStream = urlValidationService.openUrlStream(cseRequest.getNtc2ChItUrl());
             InputStream ntc2FrItStream = urlValidationService.openUrlStream(cseRequest.getNtc2FrItUrl());
             InputStream ntc2SiItStream = urlValidationService.openUrlStream(cseRequest.getNtc2SiItUrl())) {
            Map<String, InputStream> ntc2Streams = Map.of(
                urlValidationService.getFileNameFromUrl(cseRequest.getNtc2AtItUrl()), ntc2AtItStream,
                urlValidationService.getFileNameFromUrl(cseRequest.getNtc2ChItUrl()), ntc2ChItStream,
                urlValidationService.getFileNameFromUrl(cseRequest.getNtc2FrItUrl()), ntc2FrItStream,
                urlValidationService.getFileNameFromUrl(cseRequest.getNtc2SiItUrl()), ntc2SiItStream
            );
            ntc2 = Ntc2.create(cseRequest.getTargetProcessDateTime(), ntc2Streams);
            return ntc2;
        } catch (IOException e) {
            throw new CseInvalidDataException("Impossible to create NTC2", e);
        }
    }

    public CseReferenceExchanges getCseReferenceExchanges() {
        if (cseReferenceExchanges != null) {
            return cseReferenceExchanges;
        }
        try (InputStream vulcanusStream = urlValidationService.openUrlStream(cseRequest.getVulcanusUrl())) {
            cseReferenceExchanges = CseReferenceExchanges.fromVulcanusFile(cseRequest.getTargetProcessDateTime(), vulcanusStream);
            return  cseReferenceExchanges;
        } catch (IOException e) {
            throw new CseInvalidDataException("Impossible to create NTC", e);
        }
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
