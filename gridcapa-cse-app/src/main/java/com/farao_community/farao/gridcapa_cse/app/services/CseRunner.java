/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_cse.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.dichotomy_runner.api.resource.*;
import com.farao_community.farao.gridcapa_cse.api.exception.CseInternalException;
import com.farao_community.farao.gridcapa_cse.api.resource.CseRequest;
import com.farao_community.farao.gridcapa_cse.api.resource.CseResponse;
import com.farao_community.farao.gridcapa_cse.app.*;
import com.farao_community.farao.gridcapa_cse.app.util.ItalianImport;
import com.farao_community.farao.gridcapa_cse.app.util.MerchantLine;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class CseRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CseRunner.class);
    private static final double NETWORK_AND_REFERENCE_EXCHANGES_DIFFERENCE_THRESHOLD = 0.05;
    private static final String NETWORK_FILE_NAME = "network_pre_processed.xiidm";
    private static final String JSON_CRAC_FILE_NAME = "crac.json";
    public static final String ARTIFACTS_S = "artifacts/%s";

    private final UrlValidationService urlValidationService;
    private final MinioAdapter minioAdapter;
    private final DichotomyRunner dichotomyRunner;
    private final TtcResultService ttcResultService;

    public CseRunner(UrlValidationService urlValidationService, MinioAdapter minioAdapter, DichotomyRunner dichotomyRunner, TtcResultService ttcResultService) {
        this.urlValidationService = urlValidationService;
        this.minioAdapter = minioAdapter;
        this.dichotomyRunner = dichotomyRunner;
        this.ttcResultService = ttcResultService;
    }

    public CseResponse run(CseRequest cseRequest) throws IOException {
        CseData cseData = new CseData(cseRequest, urlValidationService);

        Network network = loadNetwork(cseRequest);
        MerchantLine.activateMerchantLine(cseRequest.getProcessType(), network);
        cseData.setPreProcesedNetworkUrl(saveNetwork(network));
        double initialItalianImportFromNetwork = ItalianImport.compute(network);
        checkNetworkAndReferenceExchangesDifference(cseData, initialItalianImportFromNetwork);

        Crac crac = loadCrac(cseRequest, network);
        cseData.setJsonCracUrl(convertCracInJsonAndSave(crac));

        DichotomyResponse dichotomyResponse = dichotomyRunner.runDichotomy(cseRequest, cseData, initialItalianImportFromNetwork);
        String ttcResultUrl = ttcResultService.saveTtcResult(cseRequest, cseData, dichotomyResponse, crac);

        return new CseResponse(cseRequest.getId(), ttcResultUrl);
    }

    private Network loadNetwork(CseRequest cseRequest) throws IOException {
        return Importers.loadNetwork(
            urlValidationService.getFileNameFromUrl(cseRequest.getCgmUrl()),
            urlValidationService.openUrlStream(cseRequest.getCgmUrl())
        );
    }

    private Crac loadCrac(CseRequest cseRequest, Network network) throws IOException {
        String cracFilename = urlValidationService.getFileNameFromUrl(cseRequest.getMergedCracUrl());
        InputStream cracInputStream = urlValidationService.openUrlStream(cseRequest.getMergedCracUrl());
        return CracCreators.importAndCreateCrac(cracFilename, cracInputStream, network, cseRequest.getTargetProcessDateTime()).getCrac();
    }

    private void checkNetworkAndReferenceExchangesDifference(CseData cseData, double initialItalianImportFromNetwork) {
        double referenceItalianImport = cseData.getCseReferenceExchanges().getExchanges().values().stream().reduce(0., Double::sum);
        if (Math.abs(referenceItalianImport - initialItalianImportFromNetwork) / Math.abs(referenceItalianImport) > NETWORK_AND_REFERENCE_EXCHANGES_DIFFERENCE_THRESHOLD) {
            LOGGER.warn("Difference between vulcanus exchanges and network exchanges too high.");
        }
    }

    private String convertCracInJsonAndSave(Crac crac) {
        MemDataSource memDataSource = new MemDataSource();
        try (OutputStream os = memDataSource.newOutputStream(JSON_CRAC_FILE_NAME, false)) {
            CracExporters.exportCrac(crac, "Json", os);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to save converted CRAC file.", e);
        }
        String cracPath = String.format(ARTIFACTS_S, JSON_CRAC_FILE_NAME);
        try (InputStream is = memDataSource.newInputStream(JSON_CRAC_FILE_NAME)) {
            minioAdapter.uploadFile(cracPath, is);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }

    private String saveNetwork(Network network) {
        String networkPath = String.format(ARTIFACTS_S, NETWORK_FILE_NAME);
        MemDataSource memDataSource = new MemDataSource();
        Exporters.export("XIIDM", network, new Properties(), memDataSource);
        try (InputStream is = memDataSource.newInputStream("", "xiidm")) {
            minioAdapter.uploadFile(networkPath, is);
        } catch (IOException e) {
            throw new CseInternalException("Error while trying to save pre-processed network", e);
        }
        return minioAdapter.generatePreSignedUrl(networkPath);
    }
}
