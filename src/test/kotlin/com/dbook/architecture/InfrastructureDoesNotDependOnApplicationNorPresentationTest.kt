package com.dbook.architecture

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.test.Test

class InfrastructureDoesNotDependOnApplicationNorPresentationTest : ArchitectureFixture() {
    @Test
    fun `given the infrastructure package when its dependencies are checked then none is an inner caller`() {
        noClasses()
            .that().resideInAPackage("com.dbook.infrastructure..")
            .should().dependOnClassesThat().resideInAnyPackage("com.dbook.application..", "com.dbook.presentation..")
            .because(
                "adapters implement domain ports; the use cases and controllers that call them stay unaware of them",
            )
            .check(productionClasses)
    }
}
