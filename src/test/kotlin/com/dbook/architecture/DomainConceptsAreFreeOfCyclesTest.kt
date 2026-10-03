package com.dbook.architecture

import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import kotlin.test.Test

class DomainConceptsAreFreeOfCyclesTest : ArchitectureFixture() {
    @Test
    fun `given the domain concepts when their dependencies are checked then there is no cycle between them`() {
        slices()
            .matching("com.dbook.domain.(*)..")
            .should().beFreeOfCycles()
            .check(productionClasses)
    }
}
