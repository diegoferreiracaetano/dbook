package com.dbook.architecture

import com.tngtech.archunit.base.DescribedPredicate.describe
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import kotlin.test.Test

class PersistenceConceptsAreFreeOfCyclesTest : ArchitectureFixture() {
    // The one known coupling: a flight's availableCapacity is derived from its free seats, so
    // the catalog adapters ask the SeatJpaRepository to count them, while a SeatJpaEntity
    // points back to its BookableJpaEntity. In the domain the two stay independent. Anything
    // beyond this single dependency is a new cycle and fails the build.
    @Test
    fun `given the persistence concepts when their dependencies are checked then there is no new cycle between them`() {
        slices()
            .matching("com.dbook.infrastructure.persistence.(*)..")
            .should().beFreeOfCycles()
            .ignoreDependency(
                describe(
                    "a catalog adapter",
                ) { it.packageName.startsWith("com.dbook.infrastructure.persistence.catalog") },
                describe("the seat repository") { it.simpleName == "SeatJpaRepository" },
            )
            .check(productionClasses)
    }
}
