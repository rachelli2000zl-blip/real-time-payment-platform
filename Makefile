SHELL := /bin/bash

.PHONY: backend-test backend-build ingestion-local control-local consumer-local retry-local web-local docker-build-ingestion docker-build-control docker-build-consumer docker-build-retry

backend-test:
	mvn test

backend-build:
	mvn -DskipTests package

ingestion-local:
	mvn -pl backend/ingestion-service -am spring-boot:run

control-local:
	mvn -pl backend/control-plane-service -am spring-boot:run

consumer-local:
	mvn -pl backend/consumer-service -am spring-boot:run

retry-local:
	mvn -pl backend/retry-worker -am spring-boot:run

web-local:
	cd frontend && npm install && npm run dev

docker-build-ingestion:
	docker build -f backend/ingestion-service/Dockerfile -t data-platform-ingestion:latest .

docker-build-control:
	docker build -f backend/control-plane-service/Dockerfile -t data-platform-control:latest .

docker-build-consumer:
	docker build -f backend/consumer-service/Dockerfile -t data-platform-consumer:latest .

docker-build-retry:
	docker build -f backend/retry-worker/Dockerfile -t data-platform-retry:latest .
