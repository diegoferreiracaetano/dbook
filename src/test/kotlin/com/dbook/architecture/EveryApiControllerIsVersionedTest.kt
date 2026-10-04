package com.dbook.architecture

import com.dbook.presentation.common.ApiPaths
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.test.Test

class EveryApiControllerIsVersionedTest : ArchitectureFixture() {
    // HealthController is the only controller outside a version: it is an operational endpoint
    @Test
    fun `given the rest controllers when their base path is checked then each one lives under an api version`() {
        classes()
            .that().areAnnotatedWith(RestController::class.java)
            .and().resideInAPackage("com.dbook.presentation..")
            .and().haveSimpleNameNotEndingWith("HealthController")
            .and().haveSimpleNameNotEndingWith("ApiExceptionHandler")
            .should(beMappedUnderAnApiVersion())
            .because("a business endpoint without a version prefix could not evolve without breaking its clients")
            .check(productionClasses)
    }

    private fun beMappedUnderAnApiVersion() =
        object : ArchCondition<JavaClass>("be mapped under ${ApiPaths.V1}") {
            override fun check(
                controller: JavaClass,
                events: ConditionEvents,
            ) {
                val paths = controller.getAnnotationOfType(RequestMapping::class.java)?.value.orEmpty().toList()
                val versioned = paths.isNotEmpty() && paths.all { it.startsWith("/v") && it[2].isDigit() }
                events.add(SimpleConditionEvent(controller, versioned, "${controller.name} is mapped to $paths"))
            }
        }
}
