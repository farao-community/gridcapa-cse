/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.CseReferenceExchanges;
import com.farao_community.farao.cse.data.DataUtil;
import com.farao_community.farao.cse.data.ntc.*;
import com.farao_community.farao.cse.data.ntc2.Ntc2;
import com.farao_community.farao.cse.data.target_ch.LineFixedFlows;
import com.farao_community.farao.cse.data.xsd.NTCAnnualDocument;
import com.farao_community.farao.cse.data.xsd.NTCReductionsDocument;
import com.farao_community.farao.cse.data.xsd.ntc2.CapacityDocument;
import com.farao_community.farao.cse.import_runner.app.configurations.UrlWhitelistConfiguration;
import com.farao_community.farao.cse.import_runner.app.util.FileUtil;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracImporter;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class FileImporter {
    private static final String AUSTRIAN_TAG = "AT";
    private static final String SWISS_TAG = "CH";
    private static final String FRENCH_TAG = "FR";
    private static final String SLOVENIAN_TAG = "SI";
    public static final String IMPOSSIBLE_TO_CREATE_NTC = "Impossible to create NTC";

    private final UrlWhitelistConfiguration urlWhitelistConfiguration;
    private final Logger businessLogger;

    public FileImporter(UrlWhitelistConfiguration urlWhitelistConfiguration, Logger businessLogger) {
        this.urlWhitelistConfiguration = urlWhitelistConfiguration;
        this.businessLogger = businessLogger;
    }

    public Network importNetwork(String cgmUrl) {
        return Network.read(FileUtil.getFilenameFromUrl(cgmUrl), openUrlStream(cgmUrl));
    }

    public CseCrac importCseCrac(String cracUrl) {
        InputStream cracInputStream = openUrlStream(cracUrl);
        CseCracImporter cseCracImporter = new CseCracImporter();
        return cseCracImporter.importNativeCrac(cracInputStream);
    }

    public Crac importCracFromJson(String cracUrl) {
        InputStream cracResultStream = openUrlStream(cracUrl);
        return CracImporters.importCrac(FileUtil.getFilenameFromUrl(cracUrl), cracResultStream);
    }

    public ZonalData<Scalable> importGlsk(String glskUrl, Network network) {
        return GlskDocumentImporters.importGlsk(openUrlStream(glskUrl)).getZonalScalable(network);
    }

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) {
        return new RaoResultImporter().importRaoResult(openUrlStream(raoResultUrl), crac);
    }

    public Ntc importNtc(OffsetDateTime targetProcessDateTime, String yearlyNtcUrl, String dailyNtcUrl, boolean isImportEc) {
        if (isImportEc) {
            return getNtcFromAdapted(targetProcessDateTime, yearlyNtcUrl, dailyNtcUrl, isImportEc);
        } else {
            return getNtc(targetProcessDateTime, yearlyNtcUrl, dailyNtcUrl, isImportEc);
        }
    }

    private Ntc getNtc(OffsetDateTime targetProcessDateTime, String yearlyNtcUrl, String dailyNtcUrl, boolean isImportEc) {
        try (InputStream yearlyNtcStream = openUrlStream(yearlyNtcUrl)) {
            YearlyNtcDocument yearlyNtc = new YearlyNtcDocument(targetProcessDateTime, DataUtil.unmarshalFromInputStream(yearlyNtcStream, NTCAnnualDocument.class));
            DailyNtcDocument dailyNtc = null;
            if (StringUtils.isNotBlank(dailyNtcUrl)) {
                dailyNtc = getDailyNtcDocument(targetProcessDateTime, dailyNtcUrl);
            }
            return new Ntc(yearlyNtc, dailyNtc, isImportEc);
        } catch (IOException | JAXBException e) {
            throw new CseInvalidDataException(IMPOSSIBLE_TO_CREATE_NTC, e);
        }
    }

    private DailyNtcDocument getDailyNtcDocument(OffsetDateTime targetProcessDateTime, String dailyNtcUrl) {
        try (InputStream dailyNtcStream = openUrlStream(dailyNtcUrl)) {
            return new DailyNtcDocument(targetProcessDateTime, DataUtil.unmarshalFromInputStream(dailyNtcStream, NTCReductionsDocument.class));
        } catch (IOException | JAXBException e) {
            throw new CseInvalidDataException(IMPOSSIBLE_TO_CREATE_NTC, e);
        }
    }

    private Ntc getNtcFromAdapted(OffsetDateTime targetProcessDateTime, String yearlyNtcUrl, String dailyNtcUrl, boolean isImportEc) {
        try (InputStream yearlyNtcStream = openUrlStream(yearlyNtcUrl)) {
            YearlyNtcDocumentAdapted yearlyNtc = new YearlyNtcDocumentAdapted(targetProcessDateTime, DataUtil.unmarshalFromInputStream(yearlyNtcStream, com.farao_community.farao.cse.data.xsd.ntc_adapted.NTCAnnualDocument.class));
            DailyNtcDocumentAdapted dailyNtc = null;

            if (StringUtils.isNotBlank(dailyNtcUrl)) {
                dailyNtc = getDailyNtcDocumentAdapted(targetProcessDateTime, dailyNtcUrl);
            }
            return new Ntc(yearlyNtc, dailyNtc, isImportEc);
        } catch (IOException | JAXBException e) {
            throw new CseInvalidDataException(IMPOSSIBLE_TO_CREATE_NTC, e);
        }
    }

    private DailyNtcDocumentAdapted getDailyNtcDocumentAdapted(OffsetDateTime targetProcessDateTime, String dailyNtcUrl) {
        try (InputStream dailyNtcStream = openUrlStream(dailyNtcUrl)) {
            return new DailyNtcDocumentAdapted(targetProcessDateTime, DataUtil.unmarshalFromInputStream(dailyNtcStream, com.farao_community.farao.cse.data.xsd.ntc_adapted.NTCReductionsDocument.class));
        } catch (IOException | JAXBException e) {
            throw new CseInvalidDataException(IMPOSSIBLE_TO_CREATE_NTC, e);
        }
    }

    public Ntc2 importNtc2(OffsetDateTime targetProcessDateTime, String ntc2AtItUrl, String ntc2ChItUrl, String ntc2FrItUrl, String ntc2SiItUrl) {
        Map<String, Double> ntc2Result = new HashMap<>();
        extractNtc2FromUrl(targetProcessDateTime, ntc2AtItUrl, ntc2Result);
        extractNtc2FromUrl(targetProcessDateTime, ntc2ChItUrl, ntc2Result);
        extractNtc2FromUrl(targetProcessDateTime, ntc2FrItUrl, ntc2Result);
        extractNtc2FromUrl(targetProcessDateTime, ntc2SiItUrl, ntc2Result);
        return new Ntc2(ntc2Result);
    }

    public CseReferenceExchanges importCseReferenceExchanges(OffsetDateTime targetProcessDateTime, String vulcanusUrl, ProcessType processType) {
        try (InputStream vulcanusStream = openUrlStream(vulcanusUrl)) {
            return CseReferenceExchanges.fromVulcanusFile(targetProcessDateTime, vulcanusStream, FileUtil.getFilenameFromUrl(vulcanusUrl), processType);
        } catch (IOException e) {
            throw new CseInvalidDataException("Impossible to create CseReferenceExchanges", e);
        }
    }

    public LineFixedFlows importLineFixedFlowFromTargetChFile(OffsetDateTime targetProcessDateTime, String targetChUrl, boolean isImportEc) {
        try (InputStream targetChStream = openUrlStream(targetChUrl)) {
            return LineFixedFlows.create(targetProcessDateTime, targetChStream, isImportEc);
        } catch (Exception e) {
            throw new CseInvalidDataException("Impossible to import LineFixedFlow from Target ch file", e);
        }
    }

    InputStream openUrlStream(String urlString) {
        try {
            if (urlWhitelistConfiguration.getWhitelist().stream().noneMatch(urlString::startsWith)) {
                throw new CseInvalidDataException(String.format("URL '%s' is not part of application's whitelisted url's.", urlString));
            }
            URL url = new URL(urlString);
            return url.openStream(); // NOSONAR Usage of whitelist not triggered by Sonar quality assessment, even if listed as a solution to the vulnerability
        } catch (IOException e) {
            businessLogger.error("Error while retrieving content of file : {}, Link may have expired.", getFileNameFromUrl(urlString));
            throw new CseDataException(String.format("Exception occurred while retrieving file content from : %s Cause: %s ", urlString, e.getMessage()));
        }
    }

    private String getFileNameFromUrl(String stringUrl) {
        try {
            URL url = new URL(stringUrl);
            return FilenameUtils.getName(url.getPath());
        } catch (IOException e) {
            throw new CseDataException(String.format("Exception occurred while retrieving file name from : %s Cause: %s ", stringUrl, e.getMessage()));
        }
    }

    private void extractNtc2FromUrl(OffsetDateTime targetDateTime, String ntcFileUrl, Map<String, Double> ntc2Result) {
        if (StringUtils.isNotBlank(ntcFileUrl)) {
            try (InputStream inputStream = openUrlStream(ntcFileUrl)) {
                Optional<String> optAreaCode = getAreaCodeFromFilename(FileUtil.getFilenameFromUrl(ntcFileUrl));
                optAreaCode.ifPresent(areaCode -> {
                    try {
                        double d2Exchange = getD2ExchangeByOffsetDateTime(inputStream, targetDateTime);
                        ntc2Result.put(areaCode, d2Exchange);
                    } catch (Exception e) {
                        throw new CseDataException(String.format("Impossible to import NTC2 file for area: %s", areaCode), e);
                    }
                });
            } catch (IOException e) {
                throw new CseInvalidDataException("Impossible to create NTC2", e);
            }
        }
    }

    private static Double getD2ExchangeByOffsetDateTime(InputStream ntc2InputStream, OffsetDateTime targetDateTime) throws JAXBException {
        Map<Integer, Double> qtyByPositionMap = new HashMap<>();
        CapacityDocument capacityDocument = DataUtil.unmarshalFromInputStream(ntc2InputStream, CapacityDocument.class);
        checkTimeInterval(capacityDocument, targetDateTime);
        int position = targetDateTime.atZoneSameInstant(ZoneId.of("CET")).getHour() + 1;
        capacityDocument.getCapacityTimeSeries().getPeriod().getInterval().forEach(interval -> {
            int index = interval.getPos().getV().intValue();
            qtyByPositionMap.put(index, interval.getQty().getV().doubleValue());
        });
        return Optional.ofNullable(qtyByPositionMap.get(position))
                .orElseThrow(() -> new CseDataException(
                        String.format("Impossible to retrieve NTC2 position %d. It does not exist", position)));
    }

    private static void checkTimeInterval(CapacityDocument capacityDocument, OffsetDateTime targetDateTime) {
        List<OffsetDateTime> interval = Stream.of(capacityDocument.getCapacityTimeInterval().getV().split("/"))
                .map(OffsetDateTime::parse)
                .collect(Collectors.toList());
        if (targetDateTime.isBefore(interval.get(0)) || targetDateTime.isAfter(interval.get(1)) || targetDateTime.equals(interval.get(1))) {
            throw new CseDataException("Target date time is out of bound for NTC2 archive");
        }
    }

    private static Optional<String> getAreaCodeFromFilename(String fileName) {
        if (fileName.contains(AUSTRIAN_TAG)) {
            return Optional.of(new EICode(Country.AT).getAreaCode());
        } else if (fileName.contains(SLOVENIAN_TAG)) {
            return Optional.of(new EICode(Country.SI).getAreaCode());
        } else if (fileName.contains(SWISS_TAG)) {
            return Optional.of(new EICode(Country.CH).getAreaCode());
        } else if (fileName.contains(FRENCH_TAG)) {
            return Optional.of(new EICode(Country.FR).getAreaCode());
        } else {
            return Optional.empty();
        }
    }

}
