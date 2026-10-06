// The build logic every module of dbook shares (M48, step 48.2): what is configured here once is what each module
// would otherwise repeat. The versions of the plugins the convention plugin applies live here, and only here.
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    // the `spring` and `allopen` plugins, and the `jpa` (no-arg) one
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-noarg:2.0.21")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.1")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.6")
}
