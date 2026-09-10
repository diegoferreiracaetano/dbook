plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.jpa") version "2.0.21"
    kotlin("plugin.allopen") version "2.0.21"
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    jacoco
}

group = "com.dbook"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("software.amazon.awssdk:bedrockruntime:2.28.29")
    implementation("com.bucket4j:bucket4j-core:8.10.1")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    // pinned explicitly (newer than Spring Boot 3.3.4's managed 1.19.8) — needed for
    // compatibility with recent Docker Desktop versions. Spring's BOM manages the core
    // `testcontainers` artifact too, silently downgrading it back to 1.19.8 unless it's
    // also pinned directly here (not just the two artifacts we actually import).
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$projectDir/config/detekt/detekt.yml"))
}

// detektMain/detektTest use type resolution and catch more (e.g. UnsafeCallOnNullableType)
// than the plain `detekt` task, which skips type-aware rules for speed.
tasks.check {
    dependsOn(tasks.named("detektMain"), tasks.named("detektTest"))
}

jacoco {
    toolVersion = "0.8.12"
}

// JPA entities and DTOs are plain data holders (no branching logic of their own — their
// behavior is exercised indirectly through the adapters/controllers that use them); the
// main() entrypoint is framework bootstrap, not application logic. Excluding them keeps
// the coverage number meaningful instead of diluted by classes with nothing to branch on.
val coverageExclusions =
    listOf(
        "com/dbook/DbookApplicationKt*",
        "**/*JpaEntity*",
        "**/presentation/*Request*",
        "**/presentation/*Response*",
    )

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        classDirectories.files.map { fileTree(it) { exclude(coverageExclusions) } },
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    classDirectories.setFrom(
        classDirectories.files.map { fileTree(it) { exclude(coverageExclusions) } },
    )
    violationRules {
        rule {
            limit {
                minimum = "0.75".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.withType<Test> {
    useJUnitPlatform()
    environment("DOCKER_API_VERSION", "1.41")
}
