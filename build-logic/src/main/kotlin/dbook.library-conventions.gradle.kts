// A library module of dbook (core, catalog, booking, ...): the convention of every Kotlin module, the Spring Boot
// versions, and the one dependency baseline the modules share. What is strict between modules is the graph of OUR
// modules (each declares exactly the modules it may use, see docs/adr/0001); the third-party libraries are a common
// baseline on purpose, so that a module does not repeat them and the build does not need a fine-grained table of them.
plugins {
    id("dbook.kotlin-conventions")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        // the same version as the Spring Boot plugin of the application (build.gradle.kts of the root)
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.4")
    }
}

dependencies {
    "implementation"("org.springframework.boot:spring-boot-starter-web")
    "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
    "implementation"("org.springframework.boot:spring-boot-starter-security")
    "implementation"("org.springframework.boot:spring-boot-starter-data-redis")
    "implementation"("org.springframework.boot:spring-boot-starter-actuator")
    "implementation"("io.micrometer:micrometer-tracing")
    "implementation"("software.amazon.awssdk:sqs:2.28.29")
    "implementation"("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    "implementation"("org.jetbrains.kotlin:kotlin-reflect")
}
