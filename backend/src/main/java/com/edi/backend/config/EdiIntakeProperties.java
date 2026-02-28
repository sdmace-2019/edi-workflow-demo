package com.edi.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edi.intake")
public class EdiIntakeProperties {

    private String sampleDir;

    public String getSampleDir() {
        return sampleDir;
    }

    public void setSampleDir(String sampleDir) {
        this.sampleDir = sampleDir;
    }
}