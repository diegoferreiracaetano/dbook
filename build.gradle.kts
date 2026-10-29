plugins {
    // Kotlin, ktlint, detekt, JaCoCo and the way the tests run: build-logic/ (the same for every module)
    id("dbook.kotlin-conventions")
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.owasp.dependencycheck") version "10.0.4"
}

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":admin"))
    implementation(project(":notification"))
    implementation(project(":trips"))
    implementation(project(":review"))
    implementation(project(":payment"))
    implementation(project(":ai"))
    implementation(project(":favorite"))
    implementation(project(":pricing"))
    implementation(project(":accommodation"))
    implementation(project(":flight"))
    implementation(project(":booking"))
    implementation(project(":catalog"))
    implementation(project(":identity"))
    implementation(project(":audit"))
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    // a span per SQL statement (the statement text, never the parameter values)
    implementation("net.ttddyy.observation:datasource-micrometer-spring-boot:1.0.5")
    implementation("software.amazon.awssdk:bedrockruntime:2.55.11")
    implementation("software.amazon.awssdk:sqs:2.55.11")
    implementation("com.bucket4j:bucket4j-core:8.10.1")
    // the circuit breaker and the bulkhead in front of Bedrock (core modules only, no Spring starter)
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("io.github.resilience4j:resilience4j-bulkhead:2.2.0")
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
    testImplementation("com.tngtech.archunit:archunit:1.3.0")
    testImplementation("io.micrometer:micrometer-tracing-test")
    testImplementation("org.testcontainers:localstack:1.20.4")
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

val moduleClassDirs = files(subprojects.map { it.layout.buildDirectory.dir("classes/kotlin/main") })

tasks.jacocoTestReport {
    dependsOn(tasks.test, subprojects.map { "${it.path}:classes" })
    classDirectories.setFrom(
        (classDirectories.files + moduleClassDirs.files).map { fileTree(it) { exclude(coverageExclusions) } },
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    classDirectories.setFrom(
        (classDirectories.files + moduleClassDirs.files).map { fileTree(it) { exclude(coverageExclusions) } },
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

// Run by the Security workflow (and by hand: ./gradlew dependencyCheckAnalyze), not by `check`: it downloads the
// vulnerability database, which is slow and needs the network. A known vulnerability of CVSS 7 or more fails it.
dependencyCheck {
    failBuildOnCVSS = 7.0f
    nvd.apiKey = System.getenv("NVD_API_KEY")
    analyzers.assemblyEnabled = false
}

// the checks of every module (style, static analysis) are part of the application's check
tasks.check {
    dependsOn(subprojects.map { "${it.path}:check" })
}
