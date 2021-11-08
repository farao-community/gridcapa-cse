/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data.ttc_res;

import com.farao_community.farao.cse.data.xsd.ttc_res.*;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.cse.runner.api.exception.CseInternalException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class TtcResult {
    private static final String FLOW_UNIT = "A";

    public static class TtcFiles {
        private final String initialCgmUrl;
        private final String cracJsonUrl;
        private final String mergedGlskUrl;
        private final String ntcReductionFilename;
        private final String ntcReductionCreationDatetime;
        private final String finalCgmWithPraUrl;

        public TtcFiles(String initialCgmUrl, String cracJsonUrl, String mergedGlskUrl, String ntcReductionFilename, String ntcReductionCreationDatetime, String finalCgmWithPraUrl) {
            this.initialCgmUrl = initialCgmUrl;
            this.cracJsonUrl = cracJsonUrl;
            this.mergedGlskUrl = mergedGlskUrl;
            this.ntcReductionFilename = ntcReductionFilename;
            this.ntcReductionCreationDatetime = ntcReductionCreationDatetime;
            this.finalCgmWithPraUrl = finalCgmWithPraUrl;
        }
    }

    public static class ProcessData {
        private final Map<String, Double> referenceExchanges;
        private final Map<String, Double> reducedSplittingFactors;
        private final Map<String, Double> countryBalances;
        private final String limitingCause;
        private final double finalItalianImport;
        private final double mniiOffsetValue;
        private final String processTargetDate;

        public ProcessData(Map<String, Double> referenceExchanges, Map<String, Double> reducedSplittingFactors, Map<String, Double> countryBalances, String limitingCause, double finalItalianImport, double mniiOffsetValue, String processTargetDate) {
            this.referenceExchanges = referenceExchanges;
            this.reducedSplittingFactors = reducedSplittingFactors;
            this.countryBalances = countryBalances;
            this.limitingCause = limitingCause;
            this.finalItalianImport = finalItalianImport;
            this.mniiOffsetValue = mniiOffsetValue;
            this.processTargetDate = processTargetDate;
        }
    }

    private TtcResult() {
        // Should not be instantiated
    }

    public static Timestamp generate(TtcFiles ttcFiles, ProcessData processData, CracResultsHelper cracResultsHelper) {
        Timestamp ttcResults = new Timestamp();
        Time time = new Time();
        time.setV(processData.processTargetDate);
        ttcResults.setTime(time);
        if (processData.limitingCause.equalsIgnoreCase("COMPUTATION_FAILURE")
            || (processData.limitingCause.equalsIgnoreCase("INDEX_EVALUATION_OR_MAX_ITERATION"))) {
            fillFailureReason(processData.limitingCause, ttcResults);
        } else {
            Valid valid = new Valid();
            valid.setV(BigInteger.ONE);
            ttcResults.setValid(valid);

            calculateAndFillTtcAndMnii(processData, ttcResults);

            TTCLimitedBy ttcLimitedBy = new TTCLimitedBy();
            ttcLimitedBy.setV(processData.limitingCause);
            ttcResults.setTTCLimitedBy(ttcLimitedBy);
            fillCountriesBalance(processData, ttcResults);
            fillBordersExchanges(processData, ttcResults);
            fillSplittingFactors(processData, ttcResults);
            fillRequiredFiles(ttcFiles, ttcResults);
            fillLimitingElement(cracResultsHelper, ttcResults);
            Results results = new Results();
            fillActivatedPreventiveRemedialActions(cracResultsHelper, results);
            fillCriticalBranches(cracResultsHelper, results);
            ttcResults.setResults(results);
        }
        return ttcResults;
    }

    private static void calculateAndFillTtcAndMnii(ProcessData processData, Timestamp ttcResults) {
        double mniiValue;
        double ttcValue;
        if (processData.limitingCause.equalsIgnoreCase("GLSK_LIMITATION")) {
            ttcValue = processData.finalItalianImport;
            mniiValue = processData.finalItalianImport - processData.mniiOffsetValue;
        } else {
            ttcValue = processData.finalItalianImport + processData.mniiOffsetValue;
            mniiValue = processData.finalItalianImport;
        }
        TTC ttc = new TTC();
        ttc.setV(ttcValue);
        ttcResults.setTTC(ttc);

        MNII mnii = new MNII();
        mnii.setV(mniiValue);
        ttcResults.setMNII(mnii);
    }

    private static void fillRequiredFiles(TtcFiles ttcFiles, Timestamp ttcResults) {
        CGMfile cgmFile = new CGMfile();
        cgmFile.setV(ttcFiles.finalCgmWithPraUrl);
        ttcResults.setCGMfile(cgmFile);

        GSKfile gskFile = new GSKfile();
        gskFile.setV(ttcFiles.mergedGlskUrl);
        ttcResults.setGSKfile(gskFile);

        CRACfile cracFile = new CRACfile();
        cracFile.setV(ttcFiles.cracJsonUrl);
        ttcResults.setCRACfile(cracFile);

        BASECASEfile baseCaseFile = new BASECASEfile();
        baseCaseFile.setV(ttcFiles.initialCgmUrl);
        ttcResults.setBASECASEfile(baseCaseFile);

        Inputfiles inputFiles = new Inputfiles();
        CRACfiles cracFiles = new CRACfiles();
        inputFiles.setCRACfiles(cracFiles);
        GSKfiles gskFiles = new GSKfiles();
        inputFiles.setGSKfiles(gskFiles);
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
        idcfFileName.setV(ttcFiles.initialCgmUrl);
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
                factorType.setV(BigDecimal.valueOf(factor));
                splittingFactor.setCountry(country);
                splittingFactor.setFactor(factorType);
                splittingFactorList.add(splittingFactor);
            });
            splittingFactors.getSplittingFactor().addAll(splittingFactorList);
            ttcResults.setSplittingFactors(splittingFactors);
        }
    }

    private static void fillBordersExchanges(ProcessData processData, Timestamp ttcResults) {
        if (processData.referenceExchanges != null) {
            BorderExchanges borderExchanges = new BorderExchanges();
            List<BorderExchange> borderExchangeList = new ArrayList<>();
            processData.referenceExchanges.forEach((boundary, exchangedFlow) -> {
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
                balanceValue.setV(BigDecimal.valueOf(balancedFlow));
                countryBalance.setCountry(countryType);
                countryBalance.setBalanceValue(balanceValue);
                countryBalanceList.add(countryBalance);
            });
            formattedCountryBalances.getCountryBalance().addAll(countryBalanceList);
            ttcResults.setCountryBalances(formattedCountryBalances);
        }
    }

    private static void fillFailureReason(String limitingCause, Timestamp ttcResults) {
        Valid valid = new Valid();
        valid.setV(BigInteger.ZERO);
        ttcResults.setValid(valid);
        ReasonType reasonType = new ReasonType();
        reasonType.setReason(limitingCause);
        reasonType.setReasonCode("01");   // not Known at the moment the dev is done, set to 01 by developer choice.
        ttcResults.setReasonType(reasonType);
    }

    private static void fillLimitingElement(CracResultsHelper cracResultsHelper, Timestamp ttcResults) {
        LimitingElement limitingElement = new LimitingElement();
        CriticalBranch mostLimitingCriticalBranch = new CriticalBranch();
        Outage outage = new Outage();
        Name outageName = new Name();
        FlowCnec worstCnec = cracResultsHelper.getWorstCnec();
        MonitoredElement monitoredElement = new MonitoredElement();
        Element mostLimitingElement = new Element();
        fillCommonElementInformation(mostLimitingElement, worstCnec.getName(), worstCnec.getNetworkElement().getName(), cracResultsHelper.getAreaFrom(worstCnec.getNetworkElement()), cracResultsHelper.getAreaTo(worstCnec.getNetworkElement()));
        if (worstCnec.getState().isPreventive()) {
            outageName.setV(CracResultsHelper.PREVENTIVE_OUTAGE_NAME);
            outage.setName(outageName);
            fillPreventiveCnecFlow(mostLimitingElement, cracResultsHelper.getFlowCnecResultInAmpereAfterOptim(worstCnec, OptimizationState.AFTER_PRA));
        } else {
            outageName.setV(CracResultsHelper.getOutageName(worstCnec));
            outage.setName(outageName);

            if (worstCnec.getState().getInstant() == Instant.CURATIVE) {
                FlowCnecResult flowCnecResult = cracResultsHelper.getFlowCnecResultInAmpereAfterOptim(worstCnec, OptimizationState.AFTER_CRA);
                IAfterCRA iAfterCRA = new IAfterCRA();
                iAfterCRA.setUnit(FLOW_UNIT);
                iAfterCRA.setV(BigInteger.valueOf((int) flowCnecResult.getFlow()));
                ImaxAfterCRA imaxAfterCRA = new ImaxAfterCRA();
                imaxAfterCRA.setUnit(FLOW_UNIT);
                imaxAfterCRA.setV(BigInteger.valueOf((int) flowCnecResult.getiMax()));
                mostLimitingElement.setIAfterCRA(iAfterCRA);
                mostLimitingElement.setImaxAfterCRA(imaxAfterCRA);
            } else if (worstCnec.getState().getInstant() == Instant.OUTAGE) {
                FlowCnecResult flowCnecResult = cracResultsHelper.getFlowCnecResultInAmpereAfterOptim(worstCnec, OptimizationState.AFTER_PRA);
                IAfterOutage iAfterOutage = new IAfterOutage();
                iAfterOutage.setUnit(FLOW_UNIT);
                iAfterOutage.setV(BigInteger.valueOf((int) flowCnecResult.getFlow()));
                ImaxAfterOutage imaxAfterOutage = new ImaxAfterOutage();
                imaxAfterOutage.setUnit(FLOW_UNIT);
                imaxAfterOutage.setV(BigInteger.valueOf((int) flowCnecResult.getiMax()));
                mostLimitingElement.setIAfterOutage(iAfterOutage);
                mostLimitingElement.setImaxAfterOutage(imaxAfterOutage);
            } else if (worstCnec.getState().getInstant() == Instant.AUTO) {
                FlowCnecResult flowCnecResult = cracResultsHelper.getFlowCnecResultInAmpereAfterOptim(worstCnec, OptimizationState.AFTER_PRA);
                IAfterSPS iAfterSPS = new IAfterSPS();
                iAfterSPS.setUnit(FLOW_UNIT);
                iAfterSPS.setV(BigInteger.valueOf((int) flowCnecResult.getFlow()));
                ImaxAfterSPS imaxAfterSps = new ImaxAfterSPS();
                imaxAfterSps.setUnit(FLOW_UNIT);
                imaxAfterSps.setV(BigInteger.valueOf((int) flowCnecResult.getiMax()));
                mostLimitingElement.setIAfterSPS(iAfterSPS);
                mostLimitingElement.setImaxAfterSPS(imaxAfterSps);
            } else {
                throw new CseInternalException("Couldn't find Cnec type in cnec Id : " + worstCnec.getId());
            }
        }

        mostLimitingCriticalBranch.setOutage(outage);
        monitoredElement.getElement().add(mostLimitingElement);
        mostLimitingCriticalBranch.setMonitoredElement(monitoredElement);
        limitingElement.setCriticalBranch(mostLimitingCriticalBranch);
        ttcResults.setLimitingElement(limitingElement);
    }

    private static void fillCriticalBranches(CracResultsHelper cracResultsHelper, Results results) {
        fillPreventiveCnecs(cracResultsHelper, results);
        fillNotPreventiveCnecs(cracResultsHelper, results);
    }

    private static void fillNotPreventiveCnecs(CracResultsHelper cracResultsHelper, Results results) {
        Set<Contingency> contingencies = cracResultsHelper.getCrac().getContingencies();
        contingencies.forEach(contingency -> {
            CriticalBranch criticalBranch = new CriticalBranch();
            Outage outage = new Outage();
            Name outageName = new Name();
            outageName.setV(contingency.getName());
            outage.setName(outageName);
            MonitoredElement monitoredElement = new MonitoredElement();
            contingency.getNetworkElements().forEach(contingencyNetworkElement -> {
                Element outageElement = new Element();
                Code outageElementCode = new Code();
                outageElementCode.setV(contingencyNetworkElement.getId());
                Areafrom outageAreaFrom = new Areafrom();
                outageAreaFrom.setV(cracResultsHelper.getAreaFrom(contingencyNetworkElement));
                Areato outageAreaTo = new Areato();
                outageAreaTo.setV(cracResultsHelper.getAreaTo(contingencyNetworkElement));
                outageElement.setCode(outageElementCode);
                outageElement.setAreafrom(outageAreaFrom);
                outageElement.setAreato(outageAreaTo);
                outage.getElement().add(outageElement);
                Map<String, MergedCnec> mergedMonitoredCnecs = cracResultsHelper.getMergedCnecs(contingency.getId());

                mergedMonitoredCnecs.values().forEach(mergedCnec -> {
                    Element monitoredBranchElement = new Element();
                    fillCommonElementInformation(monitoredBranchElement, mergedCnec.getCnecCommon().getName(), mergedCnec.getCnecCommon().getCode(),
                        mergedCnec.getCnecCommon().getAreaFrom(), mergedCnec.getCnecCommon().getAreaTo());

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
            fillCommonElementInformation(preventiveCnecElement, cnecPrev.getCnecCommon().getName(), cnecPrev.getCnecCommon().getCode(), cnecPrev.getCnecCommon().getAreaFrom(), cnecPrev.getCnecCommon().getAreaTo());
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

    private static void fillCommonElementInformation(Element preventiveCnecElement, String nameValue, String codeValue, String areaFromValue, String areaToValue) {
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
    }

    private static void fillActivatedPreventiveRemedialActions(CracResultsHelper cracResultsHelper, Results results) {
        Preventive preventive = new Preventive();
        List<Action> actionList = new ArrayList<>();
        List<String> topoPreventiveRas = cracResultsHelper.getPreventiveNetworkActionIds();
        List<String> pstPreventiveRas = cracResultsHelper.getPreventiveRangeActionIds();
        topoPreventiveRas.forEach(parade -> {
            Action action = new Action();
            Name name = new Name();
            name.setV(parade);
            action.setName(name);
            actionList.add(action);
        });
        pstPreventiveRas.forEach(parade -> {
            Action action = new Action();
            Name name = new Name();
            name.setV(parade);
            PSTtap pstTap = new PSTtap();
            pstTap.setV(BigInteger.valueOf(cracResultsHelper.getTapOfPstRangeActionInPreventive(parade)));
            action.setPSTtap(pstTap);
            action.setName(name);
            actionList.add(action);
        });
        preventive.getAction().addAll(actionList);
        results.setPreventive(preventive);
    }
}