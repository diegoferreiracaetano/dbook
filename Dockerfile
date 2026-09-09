# --- build stage: full JDK + Gradle, discarded after the jar is produced ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only what's needed to resolve dependencies first, so Docker can cache this layer
# and skip re-downloading everything when only application source changes.
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
COPY config ./config
RUN ./gradlew bootJar --no-daemon -x test -x ktlintCheck -x detekt

# --- runtime stage: just the JRE + the jar, nothing else from the build ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S dbook && adduser -S dbook -G dbook
USER dbook

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
