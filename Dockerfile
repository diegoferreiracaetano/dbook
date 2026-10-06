# --- build stage: full JDK + Gradle, discarded after the jar is produced ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY build-logic ./build-logic
COPY config ./config
COPY src ./src
COPY core ./core
COPY audit ./audit
COPY identity ./identity
COPY catalog ./catalog
COPY booking ./booking
COPY flight ./flight
COPY accommodation ./accommodation
COPY pricing ./pricing
COPY favorite ./favorite
COPY ai ./ai
COPY payment ./payment
COPY review ./review
COPY trips ./trips
COPY notification ./notification
COPY admin ./admin
RUN ./gradlew bootJar --no-daemon -x test -x ktlintCheck -x detekt

# --- runtime stage: just the JRE + the jar, nothing else from the build ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S dbook && adduser -S dbook -G dbook
USER dbook

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
