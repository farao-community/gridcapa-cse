/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ttc_res;

import com.farao_community.farao.cse.data.CseDataException;
import com.farao_community.farao.cse.data.cnec.*;
import com.farao_community.farao.cse.data.xsd.ttc_res.*;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.cse.remedialaction.CseHvdcCreationContext;
import com.powsybl.openrao.data.crac.io.cse.remedialaction.CsePstCreationContext;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import org.apache.commons.lang3.NotImplementedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class TtcResult {
    private static final String FLOW_UNIT = "A";

    public static class TtcFiles {
        private final String baseCaseFileName;
        private final String initialCgmFileName;
        private final String cracJsonFileName;
        private final String mergedGlskFileName;
        private final String ntcReductionFilename;
        private final String ntcReductionCreationDatetime;
        private final String finalCgmWithPraFileName;

        public TtcFiles(String baseCaseFileName, String initialCgmFileName, String cracJsonFileName, String mergedGlskFileName, String ntcReductionFilename, String ntcReductionCreationDatetime, String finalCgmWithPraFileName) {
            this.baseCaseFileName = baseCaseFileName;
            this.initialCgmFileName = initialCgmFileName;
            this.cracJsonFileName = cracJsonFileName;
            this.mergedGlskFileName = mergedGlskFileName;
            this.ntcReductionFilename = ntcReductionFilename;
            this.ntcReductionCreationDatetime = ntcReductionCreationDatetime;
            this.finalCgmWithPraFileName = finalCgmWithPraFileName;
        }
    }

    public static class ProcessData {
        private final Set<String> forcedPrasIds;
        private final Map<String, Double> borderExchanges;
        private final Map<String, Double> reducedSplittingFactors;
        private final Map<String, Double> countryBalances;
        private final LimitingCause limitingCause;
        private final double finalItalianImport;
        private final double mniiOffsetValue;
        private final String processTargetDate;

        public ProcessData(Set<String> forcedPrasIds, Map<String, Double> borderExchanges, Map<String, Double> reducedSplittingFactors, Map<String, Double> countryBalances, LimitingCause limitingCause, double finalItalianImport, double mniiOffsetValue, String processTargetDate) {
            this.forcedPrasIds = forcedPrasIds;
            this.borderExchanges = borderExchanges;
            this.reducedSplittingFactors = reducedSplittingFactors;
            this.countryBalances = countryBalances;
            this.limitingCause = limitingCause;
            this.finalItalianImport = finalItalianImport;
            this.mniiOffsetValue = mniiOffsetValue;
            this.processTargetDate = processTargetDate;
        }
    }

    public static class FailedProcessData {
        private final String processTargetDate;
        private final FailedProcessReason failedProcessReason;
        private final Optional<String> additionalFailureMessage;

        public FailedProcessData(String processTargetDate, FailedProcessReason failedProcessReason, String additionalFailureMessage) {
            this.processTargetDate = processTargetDate;
            this.failedProcessReason = failedProcessReason;
            this.additionalFailureMessage = Optional.of(additionalFailureMessage);
        }

        public FailedProcessData(String processTargetDate, FailedProcessReason failedProcessReason) {
            this.processTargetDate = processTargetDate;
            this.failedProcessReason = failedProcessReason;
            this.additionalFailureMessage = Optional.empty();
        }

        public enum FailedProcessReason {
            NO_SECURE_TTC("A98", "No secure TTC found"),
            INVALID_FILES("A94", "Some input files are invalid: "),
            LOAD_FLOW_FAILURE("A42", "Load flow divergence during calculation"),
            IT_ISSUE("A93", "IT issues on CE side"),
            OTHER("B18", "");

            private final String reasonCode;
            private final String reasonText;

            FailedProcessReason(String reasonCode, String reasonText) {
                this.reasonCode = reasonCode;
                this.reasonText = reasonText;
            }

            public String getReasonCode() {
                return reasonCode;
            }

            public String getReasonText() {
                return reasonText;
            }
        }
    }

    private TtcResult() {
        // Should not be instantiated
    }

    public static Timestamp generate(TtcFiles ttcFiles, FailedProcessData failedProcessData) {
        Timestamp ttcResults = new Timestamp();
        Time time = new Time();
        time.setV(failedProcessData.processTargetDate);
        ttcResults.setTime(time);
        fillFailureReason(failedProcessData, ttcResults);
        fillRequiredFiles(ttcFiles, ttcResults);
        return ttcResults;
    }

    public static Timestamp generate(TtcFiles ttcFiles, ProcessData processData, CracResultsHelper cracResultsHelper, Map<String, Integer> preprocessedPsts, Map<String, Double> preprocessedPisaLinks) {
        Timestamp ttcResults = new Timestamp();
        Time time = new Time();
        time.setV(processData.processTargetDate);
        ttcResults.setTime(time);
        Valid valid = new Valid();
        valid.setV(BigInteger.ONE);
        ttcResults.setValid(valid);

        calculateAndFillTtcAndMnii(processData, ttcResults);

        TTCLimitedBy ttcLimitedBy = new TTCLimitedBy();
        // It is still unclear what to do if the TTC is limited by computation failure in the dichotomy or by reaching max iteration
        ttcLimitedBy.setV(limitingCauseToString(processData.limitingCause));
        ttcResults.setTTCLimitedBy(ttcLimitedBy);
        fillCountriesBalance(processData, ttcResults);
        fillBordersExchanges(processData, ttcResults);
        fillSplittingFactors(processData, ttcResults);
        fillRequiredFiles(ttcFiles, ttcResults);
        fillLimitingElement(cracResultsHelper, ttcResults);
        Results results = new Results();
        addPreventiveRemedialActions(cracResultsHelper, results, processData.forcedPrasIds, preprocessedPsts, preprocessedPisaLinks);
        fillCriticalBranches(cracResultsHelper, results);
        ttcResults.setResults(results);
        return ttcResults;
    }

    private static void calculateAndFillTtcAndMnii(ProcessData processData, Timestamp ttcResults) {
        double mniiValue;
        double ttcValue;
        if (processData.limitingCause == LimitingCause.GLSK_LIMITATION) {
            ttcValue = processData.finalItalianImport;
            mniiValue = processData.finalItalianImport - processData.mniiOffsetValue;
        } else {
            ttcValue = processData.finalItalianImport + processData.mniiOffsetValue;
            mniiValue = processData.finalItalianImport;
        }
        TTC ttc = new TTC();
        ttc.setV(BigInteger.valueOf((int) ttcValue));
        ttcResults.setTTC(ttc);

        MNII mnii = new MNII();
        mnii.setV(BigInteger.valueOf((int) mniiValue));
        ttcResults.setMNII(mnii);
    }

    private static void fillRequiredFiles(TtcFiles ttcFiles, Timestamp ttcResults) {
        CGMfile cgmFile = new CGMfile();
        cgmFile.setV(ttcFiles.finalCgmWithPraFileName);
        ttcResults.setCGMfile(cgmFile);

        GSKfile gskFile = new GSKfile();
        gskFile.setV(ttcFiles.mergedGlskFileName);
        ttcResults.setGSKfile(gskFile);

        CRACfile cracFile = new CRACfile();
        cracFile.setV(ttcFiles.cracJsonFileName);
        ttcResults.setCRACfile(cracFile);

        BASECASEfile baseCaseFile = new BASECASEfile();
        baseCaseFile.setV(ttcFiles.baseCaseFileName);
        ttcResults.setBASECASEfile(baseCaseFile);

        Inputfiles inputFiles = new Inputfiles();
        NTCRedfiles ntcRedFiles = new NTCRedfiles();
        File ntcRedFile = new File();
        Filename ntcRedFileName = new Filename();
        ntcRedFileName.setV(ttcFiles.ntcReductionFilename);
        ntcRedFile.setFilename(ntcRedFileName);
        Country ntcRedCountry = new Country();
        ntcRedCountry.setV("IT");
        ntcRedFile.setCountry(ntcRedCountry);
        CreationDateTime ntcRedCreationDateTime = new CreationDateTime();
        ntcRedCreationDateTime.setV(ttcFiles.ntcReductionCreationDatetime);
        ntcRedFile.setCreationDateTime(ntcRedCreationDateTime);
        Backupfile ntcRedBackupFile = new Backupfile();
        ntcRedBackupFile.setV("A02");
        ntcRedFile.setBackupfile(ntcRedBackupFile);
        ntcRedFiles.setFile(ntcRedFile);
        inputFiles.setNTCRedfiles(ntcRedFiles);
        IDCFfiles idcfFiles = new IDCFfiles();
        File idcfFile = new File();
        Filename idcfFileName = new Filename();
        idcfFileName.setV(ttcFiles.initialCgmFileName);
        idcfFile.setFilename(idcfFileName);
        Country idcfCountry = new Country();
        idcfCountry.setV("UX");
        idcfFile.setCountry(idcfCountry);
        CreationDateTime idcfCreationDateTime = new CreationDateTime();
        idcfCreationDateTime.setV("");
        idcfFile.setCreationDateTime(idcfCreationDateTime);
        Backupfile idcfBackupFile = new Backupfile();
        idcfBackupFile.setV("A02");
        idcfFile.setBackupfile(idcfBackupFile);
        idcfFiles.setFile(idcfFile);
        inputFiles.setIDCFfiles(idcfFiles);
        ttcResults.setInputfiles(inputFiles);
    }

    private static void fillSplittingFactors(ProcessData processData, Timestamp ttcResults) {
        if (processData.reducedSplittingFactors != null) {
            SplittingFactors splittingFactors = new SplittingFactors();
            List<SplittingFactor> splittingFactorList = new ArrayList<>();
            processData.reducedSplittingFactors.forEach((countryString, factor) -> {
                SplittingFactor splittingFactor = new SplittingFactor();
                Country country = new Country();
                country.setV(countryString);
                Factor factorType = new Factor();
                factorType.setV(BigDecimal.valueOf(factor).setScale(6, RoundingMode.HALF_EVEN));
                splittingFactor.setCountry(country);
                splittingFactor.setFactor(factorType);
                splittingFactorList.add(splittingFactor);
            });
            splittingFactors.getSplittingFactor().addAll(splittingFactorList);
            ttcResults.setSplittingFactors(splittingFactors);
        }
    }

    private static void fillBordersExchanges(ProcessData processData, Timestamp ttcResults) {
        if (processData.borderExchanges != null) {
            BorderExchanges borderExchanges = new BorderExchanges();
            List<BorderExchange> borderExchangeList = new ArrayList<>();
            processData.borderExchanges.forEach((boundary, exchangedFlow) -> {
                BorderExchange borderExchange = new BorderExchange();
                Border border = new Border();
                border.setV(boundary);
                ExchangeValue exchangeValue = new ExchangeValue();
                exchangeValue.setV(BigDecimal.valueOf(exchangedFlow).toBigInteger());
                borderExchange.setBorder(border);
                borderExchange.setExchangeValue(exchangeValue);
                borderExchangeList.add(borderExchange);
            });
            borderExchanges.getBorderExchange().addAll(borderExchangeList);
            ttcResults.setBorderExchanges(borderExchanges);
        }
    }

    private static void fillCountriesBalance(ProcessData processData, Timestamp ttcResults) {
        if (processData.countryBalances != null) {
            CountryBalances formattedCountryBalances = new CountryBalances();
            List<CountryBalance> countryBalanceList = new ArrayList<>();
            processData.countryBalances.forEach((country, balancedFlow) -> {
                CountryBalance countryBalance = new CountryBalance();
                Country countryType = new Country();
                countryType.setV(country);
                BalanceValue balanceValue = new BalanceValue();
                balanceValue.setV(BigDecimal.valueOf(balancedFlow).setScale(1, RoundingMode.HALF_EVEN));
                countryBalance.setCountry(countryType);
                countryBalance.setBalanceValue(balanceValue);
                countryBalanceList.add(countryBalance);
            });
            formattedCountryBalances.getCountryBalance().addAll(countryBalanceList);
            ttcResults.setCountryBalances(formattedCountryBalances);
        }
    }

    private static void fillFailureReason(FailedProcessData failedProcessData, Timestamp ttcResults) {
        Valid valid = new Valid();
        valid.setV(BigInteger.ZERO);
        ttcResults.setValid(valid);
        Reason reason = new Reason();
        Reason.ReasonText reasonText = new Reason.ReasonText();
        Reason.ReasonCode reasonCode = new Reason.ReasonCode();
        String reasonTextValue = failedProcessData.additionalFailureMessage
            .map(s -> failedProcessData.failedProcessReason.getReasonText() + s)
            .orElseGet(failedProcessData.failedProcessReason::getReasonText);
        reasonText.setV(reasonTextValue);
        reasonCode.setV(failedProcessData.failedProcessReason.getReasonCode());
        reason.setReasonText(reasonText);
        reason.setReasonCode(reasonCode);
        ttcResults.setReason(reason);
    }

    private static void fillLimitingElement(CracResultsHelper cracResultsHelper, Timestamp ttcResults) {
        Crac crac = cracResultsHelper.getCrac();
        LimitingElement limitingElement = new LimitingElement();
        CriticalBranch mostLimitingCriticalBranch = new CriticalBranch();
        Outage outage = new Outage();
        Name outageName = new Name();
        FlowCnec worstCnec = CnecUtil.getWorstCnec(cracResultsHelper.getCrac(), cracResultsHelper.getRaoResult());
        MonitoredElement monitoredElement = new MonitoredElement();
        Element mostLimitingElement = new Element();
        fillCommonElementInformation(mostLimitingElement, worstCnec.getName(), worstCnec.getNetworkElement().getName(),
            cracResultsHelper.getAreaFrom(worstCnec.getNetworkElement()), cracResultsHelper.getAreaTo(worstCnec.getNetworkElement()),
            worstCnec.isOptimized());
        if (worstCnec.getState().isPreventive()) {
            outageName.setV(CracResultsHelper.PREVENTIVE_OUTAGE_NAME);
            outage.setName(outageName);
            fillPreventiveCnecFlow(mostLimitingElement, cracResultsHelper.getFlowCnecResultInAmpere(worstCnec, crac.getPreventiveInstant()));
        } else {
            outageName.setV(CracResultsHelper.getOutageName(worstCnec));
            outage.setName(outageName);
            CracResultsHelper.getOutageElements(worstCnec).forEach(contingencyElement ->
                fillOutageElements(cracResultsHelper, outage, contingencyElement)
            );

            if (worstCnec.getState().getInstant() == crac.getLastInstant()) {
                FlowCnecResult flowCnecResult = cracResultsHelper.getFlowCnecResultInAmpere(worstCnec, crac.getLastInstant());
                IAfterCRA iAfterCRA = new IAfterCRA();
                iAfterCRA.setUnit(FLOW_UNIT);
                iAfterCRA.setV(BigInteger.valueOf((int) flowCnecResult.getFlow()));
                ImaxAfterCRA imaxAfterCRA = new ImaxAfterCRA();
                imaxAfterCRA.setUnit(FLOW_UNIT);
                imaxAfterCRA.setV(BigInteger.valueOf((int) flowCnecResult.getiMax()));
                mostLimitingElement.setIAfterCRA(iAfterCRA);
                mostLimitingElement.setImaxAfterCRA(imaxAfterCRA);
            } else if (worstCnec.getState().getInstant() == crac.getOutageInstant()) {
                FlowCnecResult flowCnecResult = cracResultsHelper.getFlowCnecResultInAmpere(worstCnec, crac.getPreventiveInstant());
                IAfterOutage iAfterOutage = new IAfterOutage();
                iAfterOutage.setUnit(FLOW_UNIT);
                iAfterOutage.setV(BigInteger.valueOf((int) flowCnecResult.getFlow()));
                ImaxAfterOutage imaxAfterOutage = new ImaxAfterOutage();
                imaxAfterOutage.setUnit(FLOW_UNIT);
                imaxAfterOutage.setV(BigInteger.valueOf((int) flowCnecResult.getiMax()));
                mostLimitingElement.setIAfterOutage(iAfterOutage);
                mostLimitingElement.setImaxAfterOutage(imaxAfterOutage);
            } else if (worstCnec.getState().getInstant() == crac.getInstant(InstantKind.AUTO)) {
                FlowCnecResult flowCnecResult = cracResultsHelper.getFlowCnecResultInAmpere(worstCnec, crac.getPreventiveInstant());
                IAfterSPS iAfterSPS = new IAfterSPS();
                iAfterSPS.setUnit(FLOW_UNIT);
                iAfterSPS.setV(BigInteger.valueOf((int) flowCnecResult.getFlow()));
                ImaxAfterSPS imaxAfterSps = new ImaxAfterSPS();
                imaxAfterSps.setUnit(FLOW_UNIT);
                imaxAfterSps.setV(BigInteger.valueOf((int) flowCnecResult.getiMax()));
                mostLimitingElement.setIAfterSPS(iAfterSPS);
                mostLimitingElement.setImaxAfterSPS(imaxAfterSps);
            } else {
                throw new CseDataException("Couldn't find Cnec type in cnec Id : " + worstCnec.getId());
            }
        }

        mostLimitingCriticalBranch.setOutage(outage);
        monitoredElement.getElement().add(mostLimitingElement);
        mostLimitingCriticalBranch.setMonitoredElement(monitoredElement);
        limitingElement.setCriticalBranch(mostLimitingCriticalBranch);
        ttcResults.setLimitingElement(limitingElement);
    }

    private static void fillOutageElements(CracResultsHelper cracResultsHelper, Outage outage, ContingencyElement contingencyElement) {
        Element outageElement = new Element();
        Code outageElementCode = new Code();
        outageElementCode.setV(contingencyElement.getId());
        Areafrom outageAreaFrom = new Areafrom();
        outageAreaFrom.setV(cracResultsHelper.getAreaFrom(contingencyElement));
        Areato outageAreaTo = new Areato();
        outageAreaTo.setV(cracResultsHelper.getAreaTo(contingencyElement));
        outageElement.setCode(outageElementCode);
        outageElement.setAreafrom(outageAreaFrom);
        outageElement.setAreato(outageAreaTo);
        outage.getElement().add(outageElement);

    }

    private static void fillCriticalBranches(CracResultsHelper cracResultsHelper, Results results) {
        fillPreventiveCnecs(cracResultsHelper, results);
        fillNotPreventiveCnecs(cracResultsHelper, results);
    }

    private static void fillNotPreventiveCnecs(CracResultsHelper cracResultsHelper, Results results) {
        List<ElementaryCreationContext> cseOutageCreationContexts = cracResultsHelper.getOutageCreationContext();
        cseOutageCreationContexts.forEach(cseOutageCreationContext -> {
            CriticalBranch criticalBranch = new CriticalBranch();
            Outage outage = new Outage();
            Name outageName = new Name();
            outageName.setV(cseOutageCreationContext.getNativeObjectId());
            outage.setName(outageName);
            MonitoredElement monitoredElement = new MonitoredElement();
            Contingency contingency = cracResultsHelper.getCrac().getContingency(cseOutageCreationContext.getCreatedObjectId());
            contingency.getElements().forEach(contingencyElement ->
                fillOutageElements(cracResultsHelper, outage, contingencyElement)
            );

            addCurativeRemedialActions(contingency.getId(), cracResultsHelper, criticalBranch);

            Map<String, MergedCnec> mergedMonitoredCnecs = cracResultsHelper.getMergedCnecs(contingency.getId());
            mergedMonitoredCnecs.values().forEach(mergedCnec -> {
                Element monitoredBranchElement = new Element();
                fillCommonElementInformation(monitoredBranchElement, mergedCnec.getCnecCommon().getName(), mergedCnec.getCnecCommon().getCode(),
                    mergedCnec.getCnecCommon().getAreaFrom(), mergedCnec.getCnecCommon().getAreaTo(), mergedCnec.getCnecCommon().isSelected());
                if (mergedCnec.getiMaxAfterOutage() != 0) {
                    IAfterOutage iAfterOutage = new IAfterOutage();
                    iAfterOutage.setV(BigInteger.valueOf((int) mergedCnec.getiAfterOutage()));
                    iAfterOutage.setUnit(FLOW_UNIT);
                    monitoredBranchElement.setIAfterOutage(iAfterOutage);
                    ImaxAfterOutage imaxAfterOutage = new ImaxAfterOutage();
                    imaxAfterOutage.setV(BigInteger.valueOf((int) mergedCnec.getiMaxAfterOutage()));
                    imaxAfterOutage.setUnit(FLOW_UNIT);
                    monitoredBranchElement.setImaxAfterOutage(imaxAfterOutage);
                }
                if (mergedCnec.getiMaxAfterCra() != 0) {
                    IAfterCRA iAfterCRA = new IAfterCRA();
                    iAfterCRA.setV(BigInteger.valueOf((int) mergedCnec.getiAfterCra()));
                    iAfterCRA.setUnit(FLOW_UNIT);
                    monitoredBranchElement.setIAfterCRA(iAfterCRA);
                    ImaxAfterCRA imaxAfterCRA = new ImaxAfterCRA();
                    imaxAfterCRA.setV(BigInteger.valueOf((int) mergedCnec.getiMaxAfterCra()));
                    imaxAfterCRA.setUnit(FLOW_UNIT);
                    monitoredBranchElement.setImaxAfterCRA(imaxAfterCRA);
                }

                if (mergedCnec.getiMaxAfterSps() != 0) {
                    IAfterSPS iAfterSps = new IAfterSPS();
                    iAfterSps.setV(BigInteger.valueOf((int) mergedCnec.getiAfterSps()));
                    iAfterSps.setUnit(FLOW_UNIT);
                    monitoredBranchElement.setIAfterSPS(iAfterSps);
                    ImaxAfterSPS imaxAfterSps = new ImaxAfterSPS();
                    imaxAfterSps.setV(BigInteger.valueOf((int) mergedCnec.getiMaxAfterSps()));
                    imaxAfterSps.setUnit(FLOW_UNIT);
                    monitoredBranchElement.setImaxAfterSPS(imaxAfterSps);
                }

                monitoredElement.getElement().add(monitoredBranchElement);
            });
            criticalBranch.setOutage(outage);
            criticalBranch.setMonitoredElement(monitoredElement);
            results.getCriticalBranch().add(criticalBranch);
        });
    }

    private static void fillPreventiveCnecs(CracResultsHelper cracResultsHelper, Results results) {
        List<CnecPreventive> preventiveCnecs = cracResultsHelper.getPreventiveCnecs();
        CriticalBranch criticalBranchPreventive = new CriticalBranch();
        Outage outagePreventive = new Outage();
        Name outagePreventiveName = new Name();
        outagePreventiveName.setV(CracResultsHelper.PREVENTIVE_OUTAGE_NAME);
        outagePreventive.setName(outagePreventiveName);
        criticalBranchPreventive.setOutage(outagePreventive);
        MonitoredElement monitoredElementPreventive = new MonitoredElement();
        preventiveCnecs.forEach(cnecPrev -> {
            Element preventiveCnecElement = new Element();
            fillCommonElementInformation(preventiveCnecElement, cnecPrev.getCnecCommon().getName(),
                cnecPrev.getCnecCommon().getCode(), cnecPrev.getCnecCommon().getAreaFrom(),
                cnecPrev.getCnecCommon().getAreaTo(), cnecPrev.getCnecCommon().isSelected());
            fillPreventiveCnecFlow(preventiveCnecElement, cnecPrev);
            monitoredElementPreventive.getElement().add(preventiveCnecElement);
        });
        criticalBranchPreventive.setMonitoredElement(monitoredElementPreventive);
        results.getCriticalBranch().add(criticalBranchPreventive);
    }

    private static void fillPreventiveCnecFlow(Element preventiveCnecElement, CnecPreventive cnecPreventive) {
        I i = new I();
        i.setUnit(FLOW_UNIT);
        i.setV(BigInteger.valueOf((int) cnecPreventive.getI()));
        preventiveCnecElement.setI(i);
        Imax imax = new Imax();
        imax.setUnit(FLOW_UNIT);
        imax.setV(BigInteger.valueOf((int) cnecPreventive.getiMax()));
        preventiveCnecElement.setImax(imax);
    }

    private static void fillPreventiveCnecFlow(Element preventiveCnecElement, FlowCnecResult flowCnecResult) {
        I i = new I();
        i.setUnit(FLOW_UNIT);
        i.setV(BigInteger.valueOf((int) flowCnecResult.getFlow()));
        preventiveCnecElement.setI(i);
        Imax imax = new Imax();
        imax.setUnit(FLOW_UNIT);
        imax.setV(BigInteger.valueOf((int) flowCnecResult.getiMax()));
        preventiveCnecElement.setImax(imax);
    }

    private static void fillCommonElementInformation(Element preventiveCnecElement, String nameValue, String codeValue, String areaFromValue, String areaToValue, boolean selected) {
        Name name = new Name();
        name.setV(nameValue);
        preventiveCnecElement.setName(name);
        Code code = new Code();
        code.setV(codeValue);
        preventiveCnecElement.setCode(code);
        Areafrom areaFrom = new Areafrom();
        areaFrom.setV(areaFromValue);
        preventiveCnecElement.setAreafrom(areaFrom);
        Areato areaTo = new Areato();
        areaTo.setV(areaToValue);
        preventiveCnecElement.setAreato(areaTo);
        Selected tSelected = new Selected();
        tSelected.setV(selected ? "true" : "false");
        preventiveCnecElement.setSelected(tSelected);
    }

    private static void addPreventiveRemedialActions(CracResultsHelper cracResultsHelper, Results results, Set<String> forcedPrasIds, Map<String, Integer> preprocessedPsts, Map<String, Double> preprocessedPisaLinks) {
        Preventive preventive = new Preventive();
        List<Action> actionList = new ArrayList<>();

        List<ElementaryCreationContext> importedRemedialActionCreationContext = cracResultsHelper.getCseCracCreationContext()
            .getRemedialActionCreationContexts()
            .stream()
            .filter(ElementaryCreationContext::isImported)
            .collect(Collectors.toList());

        importedRemedialActionCreationContext.stream()
            .filter(remedialActionCreationContext -> cracResultsHelper.getPreventiveNetworkActionIds().contains(remedialActionCreationContext.getCreatedObjectId()))
            .forEach(remedialActionCreationContext -> addTopologicalAction(actionList, remedialActionCreationContext));

        importedRemedialActionCreationContext.stream()
            .filter(remedialActionCreationContext -> forcedPrasIds.contains(remedialActionCreationContext.getCreatedObjectId()))
            .forEach(remedialActionCreationContext -> addTopologicalAction(actionList, remedialActionCreationContext));

        importedRemedialActionCreationContext.stream()
            .filter(CsePstCreationContext.class::isInstance)
            .map(CsePstCreationContext.class::cast)
            .filter(csePstCreationContext -> cracResultsHelper.getPreventivePstRangeActionIds().contains(csePstCreationContext.getCreatedObjectId()))
            .forEach(csePstCreationContext -> addPstAction(actionList, csePstCreationContext, cracResultsHelper::getTapOfPstRangeActionInPreventive));
        addPstsActionsModifiedByPreprocessingAndNotByRao(importedRemedialActionCreationContext, preprocessedPsts, cracResultsHelper, actionList);

        importedRemedialActionCreationContext.stream()
            .filter(CseHvdcCreationContext.class::isInstance)
            .map(CseHvdcCreationContext.class::cast)
            .filter(csePstCreationContext -> cracResultsHelper.getPreventiveHvdcRangeActionIds().contains(csePstCreationContext.getCreatedObjectId()))
            .forEach(cseHvdcCreationContext -> addHvdcAction(actionList, cseHvdcCreationContext, cracResultsHelper::getSetpointOfHvdcRangeActionInPreventive));
        addPisaActionsModifiedByPreprocessingAndNotByRao(importedRemedialActionCreationContext, preprocessedPisaLinks, cracResultsHelper, actionList);

        preventive.getAction().addAll(actionList);
        results.setPreventive(preventive);
    }

    private static void addCurativeRemedialActions(String contingencyId, CracResultsHelper cracResultsHelper, CriticalBranch criticalBranch) {
        CurativeAction curativeAction = new CurativeAction();
        List<Action> actionList = new ArrayList<>();

        List<ElementaryCreationContext> importedRemedialActionCreationContext = cracResultsHelper.getCseCracCreationContext()
            .getRemedialActionCreationContexts()
            .stream()
            .filter(ElementaryCreationContext::isImported)
            .collect(Collectors.toList());

        importedRemedialActionCreationContext.stream()
            .filter(remedialActionCreationContext -> cracResultsHelper.getCurativeNetworkActionIds(contingencyId).contains(remedialActionCreationContext.getCreatedObjectId()))
            .forEach(remedialActionCreationContext -> addTopologicalAction(actionList, remedialActionCreationContext));

        importedRemedialActionCreationContext.stream()
            .filter(CsePstCreationContext.class::isInstance)
            .map(CsePstCreationContext.class::cast)
            .filter(csePstCreationContext -> cracResultsHelper.getCurativePstRangeActionIds(contingencyId).contains(csePstCreationContext.getCreatedObjectId()))
            .forEach(csePstCreationContext -> addPstAction(actionList, csePstCreationContext, raId -> cracResultsHelper.getTapOfPstRangeActionInCurative(contingencyId, raId)));

        importedRemedialActionCreationContext.stream()
            .filter(CseHvdcCreationContext.class::isInstance)
            .map(CseHvdcCreationContext.class::cast)
            .filter(csePstCreationContext -> cracResultsHelper.getCurativeHvdcRangeActionIds(contingencyId).contains(csePstCreationContext.getCreatedObjectId()))
            .forEach(cseHvdcCreationContext -> addHvdcAction(actionList, cseHvdcCreationContext, raId -> cracResultsHelper.getSetpointOfHvdcRangeActionInCurative(contingencyId, raId)));

        curativeAction.getAction().addAll(actionList);
        criticalBranch.setCurativeAction(curativeAction);
    }

    private static void addTopologicalAction(List<Action> actions, ElementaryCreationContext remedialActionCreationContext) {
        Action action = new Action();
        Name name = new Name();
        name.setV(remedialActionCreationContext.getNativeObjectId());
        action.setName(name);
        actions.add(action);
    }

    interface SetPointFinder {
        int findSetPoint(String raId);
    }

    private static void addPstAction(List<Action> actions, CsePstCreationContext csePstCreationContext, SetPointFinder finder) {
        String nativeId = csePstCreationContext.getNativeObjectId();
        int tap = csePstCreationContext.isInverted() ?
                -finder.findSetPoint(csePstCreationContext.getCreatedObjectId()) :
                finder.findSetPoint(csePstCreationContext.getCreatedObjectId());
        Action action = new Action();
        Name name = new Name();
        name.setV(nativeId);
        PSTtap pstTap = new PSTtap();
        pstTap.setV(BigInteger.valueOf(tap));
        action.setPSTtap(pstTap);
        action.setName(name);
        actions.add(action);
    }

    private static void addPstsActionsModifiedByPreprocessingAndNotByRao(List<ElementaryCreationContext> importedRemedialActionCreationContext, Map<String, Integer> preprocessedPsts, CracResultsHelper cracResultsHelper, List<Action> actions) {
        importedRemedialActionCreationContext.stream()
            .filter(CsePstCreationContext.class::isInstance)
            .map(CsePstCreationContext.class::cast)
            .filter(csePstCreationContext -> !cracResultsHelper.getPreventivePstRangeActionIds().contains(csePstCreationContext.getCreatedObjectId()))
            .filter(csePstCreationContext -> preprocessedPsts.containsKey(csePstCreationContext.getCreatedObjectId()))
            .forEach(remedialActionCreationContext -> {
                int pstTap = preprocessedPsts.get(remedialActionCreationContext.getCreatedObjectId());
                addPstAction(actions, remedialActionCreationContext, ra -> pstTap);
            });
    }

    private static void addPisaActionsModifiedByPreprocessingAndNotByRao(List<ElementaryCreationContext> importedRemedialActionCreationContext, Map<String, Double> prepocessedHvdc, CracResultsHelper cracResultsHelper, List<Action> actions) {

        importedRemedialActionCreationContext.stream()
            .filter(CseHvdcCreationContext.class::isInstance)
            .map(CseHvdcCreationContext.class::cast)
            .filter(csePstCreationContext -> !cracResultsHelper.getPreventiveHvdcRangeActionIds().contains(csePstCreationContext.getCreatedObjectId()))
            .filter(csePstCreationContext -> prepocessedHvdc.containsKey(csePstCreationContext.getNativeObjectId()))
            .forEach(remedialActionCreationContext -> {
                int setPoint = prepocessedHvdc.get(remedialActionCreationContext.getNativeObjectId()).intValue();
                addHvdcAction(actions, remedialActionCreationContext, ra -> setPoint);
            });
    }

    private static void addHvdcAction(List<Action> actions, CseHvdcCreationContext cseHvdcCreationContext, SetPointFinder finder) {
        String nativeId = cseHvdcCreationContext.getNativeObjectId();
        int setPoint = finder.findSetPoint(cseHvdcCreationContext.getCreatedObjectId());
        Action action = new Action();
        Name name = new Name();
        name.setV(nativeId);
        Setpoint setpoint = new Setpoint();
        setpoint.setV(BigInteger.valueOf(setPoint));
        action.setSetpoint(setpoint);
        action.setName(name);
        actions.add(action);
    }

    public static String limitingCauseToString(LimitingCause limitingCause) {
        return switch (limitingCause) {
            case COMPUTATION_FAILURE, INDEX_EVALUATION_OR_MAX_ITERATION, CRITICAL_BRANCH -> "Critical Branch";
            case GLSK_LIMITATION -> "GSK Limitation";
            default -> throw new NotImplementedException(String.format("Limiting cause %s has no description", limitingCause));
        };
    }
}
