plugins {
    id("dbook.library-conventions")
}

dependencies {
    api(project(":flight"))
    api(project(":core"))
    implementation("software.amazon.awssdk:bedrockruntime:2.55.11")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("io.github.resilience4j:resilience4j-bulkhead:2.2.0")
}
