/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.dichotomy;

import com.farao_community.farao.cse.import_runner.app.services.FileImporter;
import com.farao_community.farao.cse.runner.api.exception.CseInvalidDataException;
import com.farao_community.farao.cse.runner.api.resource.ProcessType;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class ZonalScalableProvider {

    private final FileImporter fileImporter;

    public ZonalScalableProvider(FileImporter fileImporter) {
        this.fileImporter = fileImporter;
    }

    public ZonalData<Scalable> get(String glskUrl, Network network, ProcessType processType) {
        ZonalData<Scalable> zonalScalable = fileImporter.importGlsk(glskUrl, network);
        Arrays.stream(CseCountry.values()).forEach(country -> checkCseCountryInGlsk(zonalScalable, country));
        stackScalableOnLoads(network, zonalScalable, processType);
        return zonalScalable;
    }

    private static void stackScalableOnLoads(Network network, ZonalData<Scalable> zonalScalable, ProcessType processType) {
        zonalScalable.getDataPerZone().forEach((eiCode, scalable) -> {
            CseCountry cseCountry = CseCountry.byEiCode(eiCode);
            if (processType == ProcessType.IDCC && cseCountry == CseCountry.IT) {
                return;
            }
            double sum = getZoneSumOfActiveLoads(network, cseCountry);
            // No need to go further if a country has no active load
            if (sum == 0.0) {
                return;
            }
            Scalable stackedScalable = getStackedScalable(cseCountry, scalable, network, sum);
            zonalScalable.getDataPerZone().put(eiCode, stackedScalable);
        });
    }

    private static double getZoneSumOfActiveLoads(Network network, CseCountry cseCountry) {
        return network.getLoadStream()
            .filter(load -> isLoadCorrespondingToTheCountry(load, cseCountry)).map(Load::getP0)
            .filter(p0 -> p0 > 0)
            .reduce(0., Double::sum);
    }

    private static boolean isLoadCorrespondingToTheCountry(Load load, CseCountry cseCountry) {
        return load.getTerminal().getVoltageLevel().getSubstation()
            .flatMap(Substation::getCountry)
            .filter(CseCountry::contains)
            .map(country -> CseCountry.byCountry(country) == cseCountry)
            .orElse(false);
    }

    private static Scalable getStackedScalable(CseCountry cseCountry, Scalable scalable, Network network, double sum) {
        List<Double> percentageList = new ArrayList<>();
        List<Scalable> scalableList = new ArrayList<>();

        network.getLoadStream()
            .filter(load -> isLoadCorrespondingToTheCountry(load, cseCountry) && load.getP0() > 0)
            .forEach(load -> {
                percentageList.add(Math.abs(load.getP0() / sum) * 100);
                scalableList.add(Scalable.onLoad(load.getId()));
            });

        return Scalable.stack(scalable, Scalable.proportional(percentageList, scalableList));
    }

    private static void checkCseCountryInGlsk(ZonalData<Scalable> zonalScalable, CseCountry country) {
        if (!zonalScalable.getDataPerZone().containsKey(country.getEiCode())) {
            throw new CseInvalidDataException(String.format("Area '%s' was not found in the glsk file.", country.getEiCode()));
        }
    }
}
