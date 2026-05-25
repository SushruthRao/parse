package com.project.rxparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "rx.batch")
@Validated
public class RxBatchConfiguration {

    private int batchSize = 50;

    public int getBatchSize() {
        return batchSize;
    }
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
