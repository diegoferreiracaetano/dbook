package com.dbook.architecture

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.test.Test

class ApplicationDoesNotDependOnPresentationNorInfrastructureTest : ArchitectureFixture() {
    @Test
    fun `given the application package when its dependencies are checked then none is an outer layer`() {
        noClasses()
            .that().resideInAPackage("com.dbook.application..")
            .should().dependOnClassesThat().resideInAnyPackage("com.dbook.presentation..", "com.dbook.infrastructure..")
            .because("use cases only know the domain and its ports; adapters are wired in from outside")
            .check(productionClasses)
    }
}
