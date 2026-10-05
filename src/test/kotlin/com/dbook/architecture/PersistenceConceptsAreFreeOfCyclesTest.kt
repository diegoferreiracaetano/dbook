package com.dbook.architecture

import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import kotlin.test.Test

class PersistenceConceptsAreFreeOfCyclesTest : ArchitectureFixture() {
    // No exception left: the catalog asks `seating` for free seats through the SeatAvailability port (M45).
    @Test
    fun `given the persistence concepts when their dependencies are checked then there is no new cycle between them`() {
        slices()
            .matching("com.dbook.infrastructure.persistence.(*)..")
            .should().beFreeOfCycles()
            .check(productionClasses)
    }
}
