package com.dbook.architecture

import com.tngtech.archunit.base.DescribedPredicate.describe
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import kotlin.test.Test

class PersistenceConceptsAreFreeOfCyclesTest : ArchitectureFixture() {
    // The one cycle left, on purpose and until step 48.9 of M48: the mapper that turns any `Bookable` entity into its
    // domain object knows each kind (flight, accommodation), and each kind extends the generic entity. The registry of
    // mappers by kind (48.9) removes it, and with it this exception.
    @Test
    fun `given the persistence concepts when their dependencies are checked then there is no new cycle between them`() {
        slices()
            .matching("com.dbook.infrastructure.persistence.(*)..")
            .should().beFreeOfCycles()
            .ignoreDependency(
                describe(
                    "the catalog mappers",
                ) { it.name.startsWith("com.dbook.infrastructure.persistence.catalog.CatalogMappers") },
                resideInAnyPackage(
                    "com.dbook.infrastructure.persistence.flight..",
                    "com.dbook.infrastructure.persistence.accommodation..",
                ),
            )
            .check(productionClasses)
    }
}
