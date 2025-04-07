package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.computation.LoadflowComputationException;
import com.farao_community.farao.cse.computation.BorderExchanges;
import com.farao_community.farao.cse.import_runner.app.CseData;
import com.farao_community.farao.cse.import_runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.cse.import_runner.app.dichotomy.CseCountry;
import com.farao_community.farao.cse.import_runner.app.dichotomy.NetworkShifterUtil;
import com.farao_community.farao.cse.import_runner.app.dichotomy.ZonalScalableProvider;
import com.farao_community.farao.cse.import_runner.app.util.FileUtil;
import com.farao_community.farao.cse.runner.api.resource.CseRequest;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.modification.scalable.ScalingParameters;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.EICode;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class InitialShiftService {

    private final Logger businessLogger;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final ProcessConfiguration processConfiguration;
    private final ZonalScalableProvider zonalScalableProvider;
    private static final Set<String> BORDER_COUNTRIES = Set.of(CseCountry.FR.getEiCode(), CseCountry.CH.getEiCode(), CseCountry.AT.getEiCode(), CseCountry.SI.getEiCode());

    public InitialShiftService(ZonalScalableProvider zonalScalableProvider, Logger businessLogger, FileExporter fileExporter, FileImporter fileImporter, ProcessConfiguration processConfiguration) {
        this.businessLogger = businessLogger;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.processConfiguration = processConfiguration;
        this.zonalScalableProvider = zonalScalableProvider;
    }

    void performInitialShiftFromVulcanusLevelToNtcLevel(Network network, CseData cseData, CseRequest cseRequest, Map<String, Double> referenceExchanges, Map<String, Double> ntcsByEic) throws LoadflowComputationException {
        Map<String, Double> preprocessedNetworkNps = BorderExchanges.computeCseCountriesBalances(network);
        for (Map.Entry<String, Double> entry : preprocessedNetworkNps.entrySet()) {
            businessLogger.info("Summary : Net positions on preprocessed network : for area {} : net position is {}.", entry.getKey(), entry.getValue());
        }

        Map<String, Double> initialShifts = getInitialShiftValues(cseData, referenceExchanges, ntcsByEic);

        for (Map.Entry<String, Double> entry : initialShifts.entrySet()) {
            businessLogger.info("Summary : Initial shift for area {} : {}.", entry.getKey(), entry.getValue());
        }

        shiftNetwork(initialShifts, cseRequest, network);
        Map<String, Double> netPositionsAfterInitialShift = BorderExchanges.computeCseCountriesBalances(network);
        for (Map.Entry<String, Double> entry : netPositionsAfterInitialShift.entrySet()) {
            businessLogger.info("Summary : Net positions after initial shift : for area {} : net position is {}.", entry.getKey(), entry.getValue());
        }

        String networkAfterInitialShiftPath = fileExporter.getFirstShiftNetworkPath(cseRequest.getTargetProcessDateTime(), FileUtil.getFilenameFromUrl(cseRequest.getCgmUrl()),
            cseRequest.getProcessType(), cseRequest.isImportEcProcess());

        fileExporter.exportAndUploadNetwork(network, "UCTE", GridcapaFileGroup.OUTPUT, networkAfterInitialShiftPath, processConfiguration.getInitialCgm(), cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType(), cseRequest.isImportEcProcess());
        businessLogger.info("Summary : Initial shift is finished, network is updated and initial model is exported to outputs.");
    }

    public Map<String, Double> getInitialShiftValues(CseData cseData, Map<String, Double> referenceExchanges, Map<String, Double> ntcsByEic) {
        Map<String, Double> initialShifts = new HashMap<>();

        Map<String, Double> flowOnNotModelledLinesPerCountryEic =
            NetworkShifterUtil.convertMapByCountryToMapByEic(cseData.getNtc().getFlowPerCountryOnNotModelizedLines());

        BORDER_COUNTRIES.forEach(country -> {
            double initialShift = ntcsByEic.get(country)
                - referenceExchanges.get(country)
                - flowOnNotModelledLinesPerCountryEic.get(country);
            initialShifts.put(country, initialShift);
        });

        initialShifts.put(CseCountry.IT.getEiCode(), -initialShifts.values().stream().mapToDouble(Double::doubleValue).sum());

        return initialShifts;
    }

    private void shiftNetwork(Map<String, Double> scalingValuesByCountry, CseRequest cseRequest, Network network) {
        ZonalData<Scalable> zonalScalable = getZonalScalableForProcess(cseRequest, network);
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
         // SecureRandom used to be compliant with sonar
        String newVariant = "temporary-working-variant" + new SecureRandom().nextInt(100);

        network.getVariantManager().cloneVariant(initialVariantId, newVariant, true);
        network.getVariantManager().setWorkingVariant(newVariant);
        ScalingParameters scalingParameters = new ScalingParameters()
                .setPriority(ScalingParameters.Priority.RESPECT_OF_VOLUME_ASKED)
                .setReconnect(true);
        for (Map.Entry<String, Double> entry : scalingValuesByCountry.entrySet()) {
            String zoneId = entry.getKey();
            double asked = entry.getValue();
            double done = zonalScalable.getData(zoneId).scale(network, asked, scalingParameters);
            final String message = String.format("Applying variation on zone %s (target: %.2f, done: %.2f)", zoneId, asked, done);
            businessLogger.info(message);

            if (Math.abs(done - asked) > 1e-2) {
                final String warningMessage = String.format("Glsk limitation : Incomplete variation on zone %s (target: %.3f, done: %.3f)", zoneId, asked, done);
                businessLogger.warn(warningMessage);
                if (zoneId.equals(new EICode(Country.IT).getAreaCode())) {
                    double italyGlskLimitationSplittingFactor = done / asked;
                    businessLogger.warn("Glsk limitation is reached for italy, shifts will be updated proportionally to coefficient: {}", italyGlskLimitationSplittingFactor);
                    scalingValuesByCountry.forEach((key, value) -> scalingValuesByCountry.put(key, value * italyGlskLimitationSplittingFactor));
                    network.getVariantManager().setWorkingVariant(initialVariantId);
                    network.getVariantManager().removeVariant(newVariant);
                    shiftNetwork(scalingValuesByCountry, cseRequest, network);
                    break;
                }
            }
        }

    }

    private ZonalData<Scalable> getZonalScalableForProcess(CseRequest cseRequest, Network network) {
        return cseRequest.getProcessType().equals(ProcessType.D2CC) ?
            zonalScalableProvider.get(cseRequest.getMergedGlskUrl(), network, ProcessType.D2CC) :
            GlskDocumentImporters.importGlsk(fileImporter.openUrlStream(cseRequest.getMergedGlskUrl())).getZonalScalable(network);
    }
}
