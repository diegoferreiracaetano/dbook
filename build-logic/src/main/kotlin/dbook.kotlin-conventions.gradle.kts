// The convention every Kotlin module of dbook follows: the language and the toolchain, the Spring and JPA compiler
// plugins, the style (ktlint) and static analysis (detekt) with the same rules, JaCoCo, and how the tests run.
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("plugin.allopen")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    jacoco
}

group = "com.dbook"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
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
    // one set of rules for every module, kept at the root of the repository
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

// detektMain/detektTest use type resolution and catch more (e.g. UnsafeCallOnNullableType)
// than the plain `detekt` task, which skips type-aware rules for speed.
tasks.named("check") {
    dependsOn(tasks.named("detektMain"), tasks.named("detektTest"))
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.withType<Test> {
    useJUnitPlatform()
    // docker-java (inside Testcontainers 1.20.4) defaults to API 1.32, which recent Docker
    // Desktop rejects with a 400 ("Could not find a valid Docker environment"). It reads
    // this system property, not the DOCKER_API_VERSION env var the CLI tools use.
    systemProperty("api.version", "1.41")
}
