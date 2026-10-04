package com.dbook.architecture

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.test.Test

class DomainDependsOnNoFrameworkNorOuterLayerTest : ArchitectureFixture() {
    @Test
    @Suppress("SpreadOperator")
    fun `given the domain package when its dependencies are checked then none is a framework or an outer layer`() {
        noClasses()
            .that().resideInAPackage("com.dbook.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "jakarta..",
                "io.micrometer..",
                "io.opentelemetry..",
                "com.dbook.application..",
                "com.dbook.presentation..",
                "com.dbook.infrastructure..",
            )
            .because(
                "the domain holds rules and ports only: no Spring, no JPA, no metrics or tracing, " +
                    "nothing from the layers around it",
            )
            .check(productionClasses)
    }
}
