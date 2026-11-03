# syntax=docker/dockerfile:1
ARG PAX_JDK_VER
ARG PAX_JRE_VER

FROM eclipse-temurin:${PAX_JDK_VER} AS build
WORKDIR /workspace

COPY gradle/wrapper gradle/wrapper
COPY gradle/libs.versions.toml gradle/libs.versions.toml
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

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app
USER app:app

COPY --from=build --chown=app:app /workspace/application.jar /app/application.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
