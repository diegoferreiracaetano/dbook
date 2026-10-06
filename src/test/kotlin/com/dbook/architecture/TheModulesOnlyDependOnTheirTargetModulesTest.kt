package com.dbook.architecture

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import kotlin.test.Test

// The graph of ADR 0001, in every layer (domain, application, presentation and persistence), not only in the domain.
// It was born with a list of frozen violations (the work list of the milestone, steps 48.1 to 48.12); the list is
// empty now, so the rule is plain: no class may depend on a module its module does not declare.
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
        classes().should(onlyUseTheirTargetModules)
            .because("the modules of ADR 0001 may only depend on the ones listed in ModuleGraph.mayUse")
            .check(productionClasses)
    }
}
