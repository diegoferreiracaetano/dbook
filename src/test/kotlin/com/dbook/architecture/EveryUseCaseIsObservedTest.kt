package com.dbook.architecture

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import io.micrometer.observation.annotation.Observed
import kotlin.test.Test

class EveryUseCaseIsObservedTest : ArchitectureFixture() {
    @Test
    fun `given the application use cases when their annotations are checked then every one is observed`() {
        classes()
            .that().resideInAPackage("com.dbook.application..")
            .and().haveSimpleNameEndingWith("UseCase")
            .should().beAnnotatedWith(Observed::class.java)
            .because("every use case gets a timer (and a trace span): a new one must not silently go unmeasured")
            .check(productionClasses)
    }
}
