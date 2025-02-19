/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.computation.LoadflowComputationException;
import com.farao_community.farao.cse.data.cnec.CracResultsHelper;
import com.farao_community.farao.cse.data.ttc_res.TtcResult;
import com.farao_community.farao.cse.data.xsd.ttc_res.Timestamp;
import com.farao_community.farao.cse.import_runner.app.dichotomy.DichotomyRaoResponse;
import com.farao_community.farao.cse.import_runner.app.util.FileUtil;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.io.cse.CseCracCreationContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class TtcResultService {

    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final Logger businessLogger;

    public TtcResultService(FileExporter fileExporter, FileImporter fileImporter, Logger businessLogger) {
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.businessLogger = businessLogger;
    }

    public String saveFailedTtcResult(CseRequest cseRequest, String firstShiftNetworkName, TtcResult.FailedProcessData.FailedProcessReason failedProcessReason) {
        TtcResult.TtcFiles ttcFiles = createTtcFiles(cseRequest, firstShiftNetworkName, firstShiftNetworkName);
        Timestamp timestamp = TtcResult.generate(ttcFiles, new TtcResult.FailedProcessData(
            cseRequest.getTargetProcessDateTime().toString(),
            failedProcessReason
        ));
        return fileExporter.saveTtcResult(timestamp, cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType(), cseRequest.isImportEcProcess());
    }

    public String saveTtcResult(CseRequest cseRequest, CseData cseData, CseCracCreationContext cseCracCreationContext, DichotomyRaoResponse highestSecureStepRaoResponse, LimitingCause limitingCause, String firstShiftNetworkName, String finalNetworkName, Map<String, Integer> preprocessedPsts, Map<String, Double> preprocessedPisaLinks) {
        try {
            TtcResult.TtcFiles ttcFiles = createTtcFiles(cseRequest, firstShiftNetworkName, finalNetworkName);
            String networkWithPraUrl = highestSecureStepRaoResponse.getRaoResponse().getNetworkWithPraFileUrl();
            Network networkWithPra = fileImporter.importNetwork(networkWithPraUrl);
            double finalItalianImport = BorderExchanges.computeItalianImport(networkWithPra);
            TtcResult.ProcessData processData = new TtcResult.ProcessData(
                    highestSecureStepRaoResponse.getForcedPrasIds(),
                    BorderExchanges.computeCseBordersExchanges(networkWithPra),
                    cseData.getReducedSplittingFactors(),
                    BorderExchanges.computeCseCountriesBalances(networkWithPra),
                    limitingCause,
                    finalItalianImport,
                    cseData.getMniiOffset(),
                    cseRequest.getTargetProcessDateTime().toString()
            );

            RaoResult raoResult = fileImporter.importRaoResult(highestSecureStepRaoResponse.getRaoResponse().getRaoResultFileUrl(), cseCracCreationContext.getCrac());
            CracResultsHelper cracResultsHelper = new CracResultsHelper(
                    cseCracCreationContext, raoResult, networkWithPra, businessLogger);
            Timestamp timestamp = TtcResult.generate(ttcFiles, processData, cracResultsHelper, preprocessedPsts, preprocessedPisaLinks);
            return fileExporter.saveTtcResult(timestamp, cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType(), cseRequest.isImportEcProcess());
        } catch (LoadflowComputationException e) {
            saveFailedTtcResult(
                    cseRequest,
                    firstShiftNetworkName,
                    TtcResult.FailedProcessData.FailedProcessReason.IT_ISSUE);
            throw new CseInternalException(e.getMessage());
        }
    }

    private static TtcResult.TtcFiles createTtcFiles(CseRequest cseRequest, String firstShiftNetworkName, String finalNetworkName) {
        String ntcRedFileName = StringUtils.isNotBlank(cseRequest.getNtcReductionsUrl()) ? FileUtil.getFilenameFromUrl(cseRequest.getNtcReductionsUrl()) : "";
        return new TtcResult.TtcFiles(
            firstShiftNetworkName,
            FileUtil.getFilenameFromUrl(cseRequest.getCgmUrl()),
            FileUtil.getFilenameFromUrl(cseRequest.getMergedCracUrl()),
            FileUtil.getFilenameFromUrl(cseRequest.getMergedGlskUrl()),
            ntcRedFileName,
            "ntcReductionCreationDatetime",
            finalNetworkName
        );
    }
}
