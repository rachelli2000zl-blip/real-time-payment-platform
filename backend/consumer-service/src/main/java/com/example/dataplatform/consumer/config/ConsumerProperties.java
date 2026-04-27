package com.example.dataplatform.consumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class ConsumerProperties {

    private final Aws aws = new Aws();
    private final Kcl kcl = new Kcl();
    private final Processing processing = new Processing();
    private final Schema schema = new Schema();

    public Aws getAws() {
        return aws;
    }

    public Kcl getKcl() {
        return kcl;
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
        private String dataLakeBucket;
        private String retryQueueUrl;
        private String dlqQueueUrl;

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

        public String getDataLakeBucket() {
            return dataLakeBucket;
        }

        public void setDataLakeBucket(String dataLakeBucket) {
            this.dataLakeBucket = dataLakeBucket;
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
    }

    public static class Kcl {
        private String streamName = "payment-events-stream";
        private String applicationName = "payment-processor-app";
        private String workerId = "";

        public String getStreamName() {
            return streamName;
        }

        public void setStreamName(String streamName) {
            this.streamName = streamName;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        public String getWorkerId() {
            return workerId;
        }

        public void setWorkerId(String workerId) {
            this.workerId = workerId;
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
