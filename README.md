# AWS-Native Streaming Data Platform (Spring Boot + Kinesis + ECS)

Production-style, interview-ready reference implementation for a payment event streaming platform:

- Ingestion API (`POST /events`) on ECS Fargate behind ALB
- Kinesis Data Streams for ordering and fan-out
- KCL consumer service on ECS Fargate with DynamoDB checkpoint/lease coordination
- Retry worker consuming SQS retry queue with exponential backoff and DLQ routing
- Control Plane API for ops actions (summary/errors/DLQ/replay)
- PostgreSQL for operational state (idempotency + error history + audit)
- S3 data lake (raw + processed zones)
- React + TypeScript dashboard with Cognito JWT auth
- Infrastructure as Code via AWS CDK (TypeScript)
- CI/CD build pipeline artifacts via CodeBuild `buildspec.yml`

## Architecture

1. **Ingestion Service** validates event JSON schema, enriches payload with `requestId`/`receivedAt`, writes raw object to S3, then puts to Kinesis with `partitionKey=customerId`.
2. **KCL Consumer** reads from Kinesis, validates schema again, performs idempotent insert into `processed_events`, writes processed payload to S3, and routes failures to retry queue.
3. **Retry Worker** polls SQS retry queue, reprocesses with shared logic, applies attempt tracking/backoff, sends poison messages to DLQ after max attempts.
4. **Control Plane API** exposes summary/errors/DLQ/replay endpoints for dashboard and operations.
5. **React Dashboard** reads metrics/errors/DLQ and triggers replay actions using Cognito-issued JWTs.

## Repository Tree (Key Files)

```text
.
├── README.md
├── .env.example
├── docker-compose.yml
├── Makefile
├── buildspec.yml
├── pom.xml
├── shared/
│   └── schemas/payment/v1.json
├── sql/
│   ├── migrations/V1__init.sql
│   └── athena/processed_events_ddl.sql
├── backend/
│   ├── shared-lib/
│   │   ├── pom.xml
│   │   ├── src/main/java/com/example/dataplatform/shared/
│   │   │   ├── model/{PaymentEvent,RetryMessage,ProcessingContext,ProcessingOutcome}.java
│   │   │   ├── schema/{PaymentSchemaValidator,SchemaValidationException}.java
│   │   │   ├── processing/PaymentProcessingService.java
│   │   │   ├── db/{JdbcProcessedEventRepository,JdbcOperationalRepository}.java
│   │   │   ├── s3/S3DataLakeWriter.java
│   │   │   ├── queue/FailureRouter.java
│   │   │   └── config/JacksonSupport.java
│   │   ├── src/main/resources/db/migration/V1__init.sql
│   │   ├── src/main/resources/schemas/payment/v1.json
│   │   └── src/test/java/com/example/dataplatform/shared/
│   │       ├── schema/PaymentSchemaValidatorTest.java
│   │       └── db/JdbcProcessedEventRepositoryTest.java
│   ├── ingestion-service/
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/java/com/example/dataplatform/ingestion/
│   │       ├── IngestionApplication.java
│   │       ├── config/{AwsClientConfig,IngestionProperties}.java
│   │       ├── controller/{EventController,ApiExceptionHandler}.java
│   │       ├── service/IngestionService.java
│   │       └── dto/EventAcceptedResponse.java
│   ├── control-plane-service/
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/java/com/example/dataplatform/control/
│   │       ├── ControlPlaneApplication.java
│   │       ├── config/{ControlPlaneProperties,AwsConfig,SecurityConfig}.java
│   │       ├── controller/ControlPlaneController.java
│   │       ├── service/{SummaryService,ErrorService,DlqService,ReplayService,SchemaService}.java
│   │       └── dto/*.java
│   ├── consumer-service/
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/java/com/example/dataplatform/consumer/
│   │       ├── ConsumerApplication.java
│   │       ├── config/{ConsumerProperties,AwsClientConfig,ProcessingConfig}.java
│   │       └── kcl/{KclRunner,PaymentRecordProcessor,PaymentRecordProcessorFactory}.java
│   └── retry-worker/
│       ├── Dockerfile
│       ├── pom.xml
│       └── src/main/java/com/example/dataplatform/retry/
│           ├── RetryWorkerApplication.java
│           ├── config/{RetryWorkerProperties,AwsConfig,ProcessingConfig}.java
│           └── worker/RetryPollingWorker.java
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── api/client.ts
│       ├── auth/AuthContext.tsx
│       ├── auth/cognito.ts
│       ├── components/{NavBar,SimpleLineChart}.tsx
│       ├── pages/{OverviewPage,ErrorsPage,DlqPage,ConfigPage}.tsx
│       └── styles/app.css
└── infra/
    └── cdk/
        ├── package.json
        ├── cdk.json
        ├── tsconfig.json
        ├── bin/app.ts
        └── lib/data-platform-stack.ts
```

## Domain Model

`PaymentEvent` fields:

- `eventId` (UUID string)
- `eventType` (`payment.created`, `payment.failed`, `refund.created`)
- `occurredAt` (ISO timestamp)
- `customerId` (string)
- `amount` (decimal)
- `currency` (string)
- `schemaVersion` (int)
- `metadata` (json object)
- Enriched server-side fields: `requestId`, `receivedAt`

Schema file:

- `shared/schemas/payment/v1.json`

## API Endpoints

### Ingestion API

- `POST /events`
  - Validates against JSON schema
  - Writes raw event to `s3://<bucket>/raw/event_type=.../dt=YYYY-MM-DD/<eventId>.json`
  - Publishes to Kinesis with partition key = `customerId`
  - Returns `202 Accepted` with `requestId`

### Control Plane API

- `GET /summary`
- `GET /errors?eventId=&stage=&limit=`
- `GET /dlq?limit=`
- `POST /dlq/replay`
- `POST /replay/by-date?date=YYYY-MM-DD&eventType=...&maxRecords=...&actor=...`
- `GET /schemas`
- `GET /config`

## Database Schema

Migration SQL:

- `sql/migrations/V1__init.sql`

Tables:

- `processed_events`
- `processing_errors`
- `dlq_events`
- `audit_logs`

Idempotency is enforced by `processed_events.event_id PRIMARY KEY` and `INSERT ... ON CONFLICT DO NOTHING`.

## Data Lake Layout

- Raw zone: `s3://<bucket>/raw/event_type=.../dt=YYYY-MM-DD/<eventId>.json`
- Processed zone: `s3://<bucket>/processed/event_type=.../dt=YYYY-MM-DD/<eventId>.json`

Athena DDL example:

- `sql/athena/processed_events_ddl.sql`

## Local Development

1. Copy env template:
   ```bash
   cp .env.example .env
   ```
2. Start Postgres:
   ```bash
   docker compose up -d postgres
   ```
3. Run backend tests/build:
   ```bash
   make backend-test
   make backend-build
   ```
4. Run services locally (in separate terminals):
   ```bash
   make ingestion-local
   make control-local
   make consumer-local
   make retry-local
   ```
5. Run dashboard:
   ```bash
   make web-local
   ```

## AWS Deployment (CDK)

1. Build/push service images to ECR repos.
2. Deploy stack:
   ```bash
   cd infra/cdk
   npm install
   npm run synth
   npm run deploy
   ```
3. Use stack outputs for:
   - ALB URL
   - CloudFront URL
   - ECR URIs
   - Queue URLs
   - Kinesis stream name
   - Data lake bucket

## CI/CD

`buildspec.yml` implements CodeBuild phases:

- run Maven tests
- build jars and Docker images
- push to ECR
- emit ECS image definition artifacts

### CodePipeline Outline

1. **Source stage**: GitHub/CodeCommit trigger on main branch.
2. **Build stage**: CodeBuild runs `buildspec.yml`.
3. **Deploy stage**:
   - ECS deploy action for ingestion service
   - ECS deploy action for control-plane service
   - ECS deploy action for consumer service
   - ECS deploy action for retry service
4. Optional: add manual approval before production deploy.

## Frontend Auth (Cognito)

Dashboard uses Cognito Hosted UI OAuth implicit flow (`id_token`) and sends:

- `Authorization: Bearer <jwt>`

Set these frontend env vars:

- `VITE_COGNITO_DOMAIN`
- `VITE_COGNITO_CLIENT_ID`
- `VITE_COGNITO_REDIRECT_URI`
- `VITE_COGNITO_LOGOUT_URI`
- `VITE_CONTROL_PLANE_URL`

Enable JWT enforcement in control-plane by setting:

- `SECURITY_ENABLED=true`
- `COGNITO_ISSUER_URI=https://cognito-idp.<region>.amazonaws.com/<userPoolId>`

## Notes

- Consumer and retry worker both validate schema for defense-in-depth.
- KCL handles shard lease/checkpoint coordination using DynamoDB.
- Logs include event/request identifiers and outcomes for CloudWatch logs-based metrics.
- Processed JSON can be converted to Parquet later for better Athena performance.
