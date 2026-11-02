# syntax=docker/dockerfile:1
ARG PAX_JDK_VER
ARG PAX_JRE_VER

FROM eclipse-temurin:${PAX_JDK_VER} AS build
WORKDIR /workspace

COPY gradle/wrapper gradle/wrapper
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
RUN chmod +x gradlew

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies

COPY src src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test \
    && jar="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar')" \
    && test -n "$jar" \
    && cp "$jar" /workspace/application.jar

FROM eclipse-temurin:${PAX_JRE_VER}
WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring
USER spring:spring

COPY --from=build --chown=spring:spring /workspace/application.jar /app/application.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
