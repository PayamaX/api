# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.2.0-M2/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.2.0-M2/gradle-plugin/packaging-oci-image.html)
* [Coroutines section of the Spring Framework Documentation](https://docs.spring.io/spring-framework/reference/7.1.0-M2/languages/kotlin/coroutines.html)
* [Spring Boot Testcontainers support](https://docs.spring.io/spring-boot/4.2.0-M2/reference/testing/testcontainers.html#testing.testcontainers)
* [Testcontainers Kafka Modules Reference Guide](https://java.testcontainers.org/modules/kafka/)
* [Testcontainers Postgres Module Reference Guide](https://java.testcontainers.org/modules/databases/postgres/)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.2.0-M2/reference/actuator/index.html)
* [Spring Batch](https://docs.spring.io/spring-boot/4.2.0-M2/how-to/batch.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.2.0-M2/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Data Reactive Redis](https://docs.spring.io/spring-boot/4.2.0-M2/reference/data/nosql.html#data.nosql.redis)
* [Datadog](https://docs.spring.io/spring-boot/4.2.0-M2/reference/actuator/metrics.html#actuator.metrics.export.datadog)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/4.2.0-M2/reference/using/devtools.html)
* [Docker Compose Support](https://docs.spring.io/spring-boot/4.2.0-M2/reference/features/dev-services.html#features.dev-services.docker-compose)
* [Flyway Migration](https://docs.spring.io/spring-boot/4.2.0-M2/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
* [JDBC API](https://docs.spring.io/spring-boot/4.2.0-M2/reference/data/sql.html)
* [Spring for Apache Kafka](https://docs.spring.io/spring-boot/4.2.0-M2/reference/messaging/kafka.html)
* [Apache Kafka Streams Support](https://docs.spring.io/spring-kafka/reference/streams.html)
* [Apache Kafka Streams Binding Capabilities of Spring Cloud Stream](https://docs.spring.io/spring-cloud-stream/reference/kafka/kafka-streams-binder/usage.html)
* [Spring Modulith](https://docs.spring.io/spring-modulith/reference/)
* [OAuth2 Client](https://docs.spring.io/spring-boot/4.2.0-M2/reference/web/spring-security.html#web.security.oauth2.client)
* [Quartz Scheduler](https://docs.spring.io/spring-boot/4.2.0-M2/reference/io/quartz.html)
* [Spring Security](https://docs.spring.io/spring-boot/4.2.0-M2/reference/web/spring-security.html)
* [Reactive HTTP Client](https://docs.spring.io/spring-boot/4.2.0-M2/reference/io/rest-client.html#io.rest-client.webclient)
* [Testcontainers](https://java.testcontainers.org/)
* [Validation](https://docs.spring.io/spring-boot/4.2.0-M2/reference/io/validation.html)
* [Spring Reactive Web](https://docs.spring.io/spring-boot/4.2.0-M2/reference/web/reactive.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
* [Creating a Batch Service](https://spring.io/guides/gs/batch-processing/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Messaging with Redis](https://spring.io/guides/gs/messaging-redis/)
* [Accessing Relational Data using JDBC with Spring](https://spring.io/guides/gs/relational-data-access/)
* [Managing Transactions](https://spring.io/guides/gs/managing-transactions/)
* [Samples for using Apache Kafka Streams with Spring Cloud stream](https://github.com/spring-cloud/spring-cloud-stream-samples/tree/main/kafka-streams-samples)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Validation](https://spring.io/guides/gs/validating-form-input/)
* [Building a Reactive RESTful Web Service](https://spring.io/guides/gs/reactive-rest-service/)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

### Docker Compose support
This project contains a Docker Compose file named `compose.yaml`.
In this file, the following services have been defined:

* postgres: [`postgres:latest`](https://hub.docker.com/_/postgres)
* redis: [`redis:latest`](https://hub.docker.com/_/redis)

Please review the tags of the used images and set them to the same as you're running in production.

### Testcontainers support

This project uses [Testcontainers at development time](https://docs.spring.io/spring-boot/4.2.0-M2/reference/features/dev-services.html#features.dev-services.testcontainers).

Testcontainers has been configured to use the following Docker images:

* [`apache/kafka-native:latest`](https://hub.docker.com/r/apache/kafka-native)
* [`postgres:latest`](https://hub.docker.com/_/postgres)
* [`redis:latest`](https://hub.docker.com/_/redis)

Please review the tags of the used images and set them to the same as you're running in production.

