package com.dbook.architecture

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.test.Test

class DomainConceptsOnlyDependOnTheAllowedOnesTest : ArchitectureFixture() {
    // The only references between concepts in the domain today. Anything else must go by id
    // (bookableId, bookingId...), which is what keeps the concepts separable.
    private val allowed =
        mapOf(
            "booking" to setOf("catalog", "messaging"),
            "catalog" to setOf("messaging"),
            "payment" to setOf("messaging"),
            // the notification types are named after the events the other concepts write to the outbox
            "notification" to setOf("booking", "catalog", "payment", "pricing"),
            "pricing" to setOf("messaging"),
            "accommodation" to setOf("catalog", "booking"),
            "ai" to setOf("catalog"),
            "audit" to setOf("identity"),
            "crm" to setOf("identity"),
        )

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
