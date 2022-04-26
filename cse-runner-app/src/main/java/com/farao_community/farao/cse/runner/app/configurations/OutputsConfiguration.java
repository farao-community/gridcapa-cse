package com.farao_community.farao.cse.runner.app.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("outputs")
public class OutputsConfiguration {

    private String initialCgm;
    private String finalCgm;
    private String ttcRes;

    public OutputsConfiguration(String initialCgm, String finalCgm, String ttcRes) {
        this.initialCgm = initialCgm;
        this.finalCgm = finalCgm;
        this.ttcRes = ttcRes;
    }

    public String getInitialCgm() {
        return initialCgm;
    }

    public String getFinalCgm() {
        return finalCgm;
    }

    public String getTtcRes() {
        return ttcRes;
    }

    public void setInitialCgm(String initialCgm) {
        this.initialCgm = initialCgm;
    }

    public void setFinalCgm(String finalCgm) {
        this.finalCgm = finalCgm;
    }

    public void setTtcRes(String ttcRes) {
        this.ttcRes = ttcRes;
    }
}
