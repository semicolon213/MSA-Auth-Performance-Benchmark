package com.msa.benchmark.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String mode = "no-auth";

    private int artificialLatencyMs = 0;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getArtificialLatencyMs() {
        return artificialLatencyMs;
    }

    public void setArtificialLatencyMs(int artificialLatencyMs) {
        this.artificialLatencyMs = artificialLatencyMs;
    }
}
