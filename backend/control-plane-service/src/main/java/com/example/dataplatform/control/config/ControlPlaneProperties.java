package com.example.dataplatform.control.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class ControlPlaneProperties {

    private final Aws aws = new Aws();
    private final Replay replay = new Replay();
    private final Security security = new Security();
    private final Processing processing = new Processing();

    public Aws getAws() {
        return aws;
    }

    public Replay getReplay() {
        return replay;
    }

    public Security getSecurity() {
        return security;
    }

    public Processing getProcessing() {
        return processing;
    }

    public static class Aws {
        private String region = "us-east-1";
        private String endpointOverride = "";
        private String retryQueueUrl;
        private String dlqQueueUrl;
        private String kinesisStreamName;
        private String dataLakeBucket;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getEndpointOverride() {
            return endpointOverride;
        }

        public void setEndpointOverride(String endpointOverride) {
            this.endpointOverride = endpointOverride;
        }

        public String getRetryQueueUrl() {
            return retryQueueUrl;
        }

        public void setRetryQueueUrl(String retryQueueUrl) {
            this.retryQueueUrl = retryQueueUrl;
        }

        public String getDlqQueueUrl() {
            return dlqQueueUrl;
        }

        public void setDlqQueueUrl(String dlqQueueUrl) {
            this.dlqQueueUrl = dlqQueueUrl;
        }

        public String getKinesisStreamName() {
            return kinesisStreamName;
        }

        public void setKinesisStreamName(String kinesisStreamName) {
            this.kinesisStreamName = kinesisStreamName;
        }

        public String getDataLakeBucket() {
            return dataLakeBucket;
        }

        public void setDataLakeBucket(String dataLakeBucket) {
            this.dataLakeBucket = dataLakeBucket;
        }
    }

    public static class Replay {
        private int maxRecords = 1000;

        public int getMaxRecords() {
            return maxRecords;
        }

        public void setMaxRecords(int maxRecords) {
            this.maxRecords = maxRecords;
        }
    }

    public static class Security {
        private boolean enabled = false;
        private String allowedOrigins = "*";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Processing {
        private int maxAttempts = 5;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }
}
