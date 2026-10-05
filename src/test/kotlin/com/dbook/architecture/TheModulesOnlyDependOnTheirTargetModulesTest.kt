package com.dbook.architecture

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.library.freeze.FreezingArchRule
import kotlin.test.Test

// The graph of ADR 0001, in every layer (domain, application, presentation and persistence), not only in the domain.
// What breaks it today is frozen in src/test/resources/archunit_store: the test fails for a NEW violation, and every
// cycle undone (steps 48.5 to 48.12) removes lines from the store, which is the work list of the milestone.
class TheModulesOnlyDependOnTheirTargetModulesTest : ArchitectureFixture() {
    private val onlyUseTheirTargetModules =
        object : ArchCondition<JavaClass>("only depend on the modules their module may use (ADR 0001)") {
            override fun check(
                item: JavaClass,
                events: ConditionEvents,
            ) {
                val from = ModuleGraph.moduleOf(item) ?: return
                val allowed = ModuleGraph.mayUse.getValue(from)
                item.directDependenciesFromSelf
                    .mapNotNull { dependency -> ModuleGraph.moduleOf(dependency.targetClass)?.let { it to dependency } }
                    .filter { (to, _) -> to != from && to !in allowed }
                    .map { (to, dependency) -> "${item.name} ($from) -> ${dependency.targetClass.name} ($to)" }
                    .distinct()
                    .forEach { events.add(SimpleConditionEvent.violated(item, it)) }
            }
        }

    @Test
    fun `given the production classes when their modules are compared then none depends on a module it may not use`() {
        FreezingArchRule.freeze(
            classes().should(onlyUseTheirTargetModules)
                .because("the modules of ADR 0001 may only depend on the ones listed in ModuleGraph.mayUse"),
        ).check(productionClasses)
    }
}
