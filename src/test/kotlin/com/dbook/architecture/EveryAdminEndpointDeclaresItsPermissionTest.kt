package com.dbook.architecture

import com.dbook.presentation.common.PublicEndpoints
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.test.Test

// SecurityConfig only checks that a caller is staff at all; WHICH staff may do WHAT is each endpoint's own
// @PreAuthorize. A new /admin endpoint that forgets it would let every staff role in, so the build fails instead.
class EveryAdminEndpointDeclaresItsPermissionTest : ArchitectureFixture() {
    @Test
    fun `given the admin controllers when their endpoints are checked then each one declares a PreAuthorize`() {
        classes()
            .that().areAnnotatedWith(RestController::class.java)
            .and().resideInAPackage("com.dbook.presentation..")
            .and(areMappedUnderAdmin())
            .and().areNotAnnotatedWith(PublicEndpoints::class.java)
            .should(declarePreAuthorizeOnEveryEndpoint())
            .because("an /admin endpoint without its own permission would be open to every staff role")
            .check(productionClasses)
    }

    private fun areMappedUnderAdmin() =
        object : DescribedPredicate<JavaClass>("are mapped under /admin") {
            override fun test(controller: JavaClass): Boolean {
                val mapping = controller.tryGetAnnotationOfType(RequestMapping::class.java).orElse(null)
                return mapping?.value.orEmpty().any { it.contains("/admin") }
            }
        }

    private fun declarePreAuthorizeOnEveryEndpoint() =
        object : ArchCondition<JavaClass>("declare @PreAuthorize on every endpoint") {
            override fun check(
                controller: JavaClass,
                events: ConditionEvents,
            ) {
                controller.methods.filter(::isEndpoint).forEach { method ->
                    val guarded = method.isAnnotatedWith(PreAuthorize::class.java)
                    events.add(SimpleConditionEvent(method, guarded, "${method.fullName} is guarded: $guarded"))
                }
            }
        }

    private fun isEndpoint(method: JavaMethod) =
        listOf(
            GetMapping::class.java,
            PostMapping::class.java,
            PutMapping::class.java,
            PatchMapping::class.java,
            DeleteMapping::class.java,
            RequestMapping::class.java,
        ).any { method.isAnnotatedWith(it) }
}
