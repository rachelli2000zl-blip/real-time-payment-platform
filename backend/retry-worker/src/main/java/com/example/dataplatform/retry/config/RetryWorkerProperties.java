package com.example.dataplatform.retry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class RetryWorkerProperties {

    private final Aws aws = new Aws();
    private final Worker worker = new Worker();
    private final Processing processing = new Processing();
    private final Schema schema = new Schema();

    public Aws getAws() {
        return aws;
    }

    public Worker getWorker() {
        return worker;
    }

    public Processing getProcessing() {
        return processing;
    }

    public Schema getSchema() {
        return schema;
    }

    public static class Aws {
        private String region = "us-east-1";
        private String endpointOverride = "";
        private String retryQueueUrl;
        private String dlqQueueUrl;
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

        public String getDataLakeBucket() {
            return dataLakeBucket;
        }

        public void setDataLakeBucket(String dataLakeBucket) {
            this.dataLakeBucket = dataLakeBucket;
        }
    }

    public static class Worker {
        private int pollIntervalMs = 4000;
        private int maxMessagesPerPoll = 10;

        public int getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(int pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public int getMaxMessagesPerPoll() {
            return maxMessagesPerPoll;
        }

        public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
            this.maxMessagesPerPoll = maxMessagesPerPoll;
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

    public static class Schema {
        private String path = "shared/schemas/payment/v1.json";

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
