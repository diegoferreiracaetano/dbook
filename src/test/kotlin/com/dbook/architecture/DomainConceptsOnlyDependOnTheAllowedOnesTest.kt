package com.dbook.architecture

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.test.Test

class DomainConceptsOnlyDependOnTheAllowedOnesTest : ArchitectureFixture() {
    // The only references between concepts in the domain today. Anything else must go by id
    // (bookableId, bookingId...), which is what keeps the concepts separable.
    private val allowed = mapOf("booking" to setOf("catalog"), "ai" to setOf("catalog"), "audit" to setOf("identity"))

    @Test
    @Suppress("SpreadOperator")
    fun `given the domain concepts when their dependencies are checked then only the allowed ones are used`() {
        concepts.forEach { concept ->
            val forbidden =
                concepts
                    .filter { it != concept && it !in allowed[concept].orEmpty() }
                    .map { "com.dbook.domain.$it.." }

            noClasses()
                .that().resideInAPackage("com.dbook.domain.$concept..")
                .should().dependOnClassesThat().resideInAnyPackage(*forbidden.toTypedArray())
                .because("'$concept' may only depend on ${allowed[concept].orEmpty()} inside the domain")
                .check(productionClasses)
        }
    }
}
