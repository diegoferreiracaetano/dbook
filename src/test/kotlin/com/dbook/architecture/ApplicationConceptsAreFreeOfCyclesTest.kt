package com.dbook.architecture

import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import kotlin.test.Test

class ApplicationConceptsAreFreeOfCyclesTest : ArchitectureFixture() {
    @Test
    fun `given the application concepts when their dependencies are checked then there is no cycle between them`() {
        slices()
            .matching("com.dbook.application.(*)..")
            .should().beFreeOfCycles()
            .check(productionClasses)
    }
}
