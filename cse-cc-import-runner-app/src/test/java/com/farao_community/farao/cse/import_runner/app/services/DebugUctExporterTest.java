package com.farao_community.farao.cse.import_runner.app.services;

import com.farao_community.farao.cse.computation.BorderExchanges;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DebugUctExporterTest {

    @Test
    void roundTripInputNetwork() {
        Network networkFromSourceUct = Network.read("source.uct", getClass().getResourceAsStream("source.uct"));
        LoadFlow.run(networkFromSourceUct);

        DataSource dataSourceGeneratedUct = new FileDataSource(Path.of("/tmp"), "generated.uct");
        networkFromSourceUct.write("UCTE", new Properties(), dataSourceGeneratedUct);
        Network networkFromGeneratedUct = Network.read(dataSourceGeneratedUct);
        LoadFlow.run(networkFromGeneratedUct);

        LoggerFactory.getLogger("").info("________________network From Source Uct____________________");
        Map<String, Double> npsSource = BorderExchanges.computeCseCountriesBalances(networkFromSourceUct);
        logNps(npsSource);

        LoggerFactory.getLogger("").info("___________________network From generated UCT_________________");
        Map<String, Double> npsGenerated = BorderExchanges.computeCseCountriesBalances(networkFromGeneratedUct);
        logNps(npsGenerated);
        assertEquals(npsSource.get("FR"), npsGenerated.get("FR"));
        assertEquals(npsSource.get("CH"), npsGenerated.get("CH"));
        assertEquals(npsSource.get("SI"), npsGenerated.get("SI"));
        assertEquals(npsSource.get("AT"), npsGenerated.get("AT"));
        assertEquals(npsSource.get("IT"), npsGenerated.get("IT"));

    }

    void logNps(Map<String, Double> nps) {
        for (Map.Entry<String, Double> entry : nps.entrySet()) {
            LoggerFactory.getLogger("").info("NP for area {} : {}.", entry.getKey(), entry.getValue());
        }
    }
}
