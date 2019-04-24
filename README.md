# Camunda BPM example based on Spring Boot

Based on [Camunda Spring Boot starter app](https://github.com/camunda/camunda-bpm-examples/tree/master/spring-boot-starter/example-webapp?ref=7f807189b443c2f10e8cb192303a46b0fef7ac62)

This example packages Camunda BPM platform as Spring Boot Web application with following configured:

* Spring Boot 2.1 + Java 8
* Embedded Camunda engine
* Camunda web applications (cockpit, admin, tasklist) 
* Sample process application and one BPMN process deployed
* Test user configured with login and password in [`application.properties`](src/main/resources/application.properties)
* BPMN Process sample
* DMN Decision table sample
* Service task sample
* External service task sample
* Process unit testing
* Introduction and welcome page - launchpad
* Swagger UI + OpenSchema.json

![Launchpad](src/main/resources/static/launchpad/launchpad.png)

![Tasklist](src/main/resources/static/launchpad/tasklist.png)

![Cockpit](src/main/resources/static/launchpad/cockpit.png)

![User admin](src/main/resources/static/launchpad/useradmin.png)

![Swagger UI](src/main/resources/static/launchpad/swagger.png)

## Building

Execute following:

```bash
mvn clean package
```

## Running

To run application at http://localhost:8080 execute:

```bash
mvn spring-boot:run
```

## Deploying locally to docker

Execute following to start via Docker

```bash
docker build . -t camundacloud/camunda-demo
docker run -d -p 8080:8080 camundacloud/camunda-demo
```

## Security

By default, **password** are generated for user demo when you run it first time.
It is shown on the first line printed by container when it starts:

```
========= YOUR CAMUNDA PASSWORD IS 123 =========
```

Specify fixed password by providing CAMUNDA_PWD environment variable using docker or Google Cloud Run.

## Deploying to Google Cloud run

```
gcloud beta run deploy --image gcr.io/camundacloud/camunda-demo --memory=1G
```

## Push to google cloud

Create your GCR in Google Cloud and use your own Docker image.

```
docker build . -t camundacloud/camunda-demo
docker tag camundacloud/camunda-demo gcr.io/camundacloud/camunda-demo
docker push gcr.io/camundacloud/camunda-demo
```
