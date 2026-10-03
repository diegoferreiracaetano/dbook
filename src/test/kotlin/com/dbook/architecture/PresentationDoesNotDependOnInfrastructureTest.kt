package com.dbook.architecture

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.test.Test

class PresentationDoesNotDependOnInfrastructureTest : ArchitectureFixture() {
    @Test
    fun `given the presentation package when its dependencies are checked then none is infrastructure`() {
        noClasses()
            .that().resideInAPackage("com.dbook.presentation..")
            .should().dependOnClassesThat().resideInAPackage("com.dbook.infrastructure..")
            .because("controllers go through use cases; they never reach an adapter directly")
            .check(productionClasses)
    }
}
