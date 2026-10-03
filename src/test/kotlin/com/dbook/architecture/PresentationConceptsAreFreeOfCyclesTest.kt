package com.dbook.architecture

import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import kotlin.test.Test

class PresentationConceptsAreFreeOfCyclesTest : ArchitectureFixture() {
    @Test
    fun `given the presentation concepts when their dependencies are checked then there is no cycle between them`() {
        slices()
            .matching("com.dbook.presentation.(*)..")
            .should().beFreeOfCycles()
            .check(productionClasses)
    }
}
