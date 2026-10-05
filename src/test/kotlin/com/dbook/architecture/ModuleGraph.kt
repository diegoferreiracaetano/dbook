package com.dbook.architecture

import com.tngtech.archunit.core.domain.JavaClass

/**
 * The target modules of M48 (ADR 0001) as data: which concept packages each module will own, and which other modules
 * it may use. The rule that checks it runs today, with everything still in one Gradle module: the code that already
 * breaks the graph is frozen (see [TheModulesOnlyDependOnTheirTargetModulesTest]) and each cycle undone shrinks that
 * list. When the modules become real, Gradle enforces the same graph at compile time.
 */
object ModuleGraph {
    /** The concept packages (under domain, application, presentation and infrastructure.persistence) of each module. */
    val conceptsOf: Map<String, Set<String>> =
        mapOf(
            "core" to setOf("common", "messaging"),
            "audit" to setOf("audit"),
            "identity" to setOf("identity"),
            "catalog" to setOf("catalog"),
            "booking" to setOf("booking"),
            // the seats are part of the flight; the `flight` package arrives with step 48.3
            "flight" to setOf("flight", "seating"),
            "accommodation" to setOf("accommodation"),
            "pricing" to setOf("pricing"),
            "favorite" to setOf("favorite"),
            "ai" to setOf("ai"),
            // a promo code only exists to lower a payment: the package stays, inside the payment module
            "payment" to setOf("payment", "promo"),
            "review" to setOf("review"),
            // the package arrives with step 48.4
            "trips" to setOf("trips"),
            "notification" to setOf("notification"),
            "admin" to setOf("crm", "dashboard"),
            "app" to setOf("appconfig"),
        )

    /** What each module may use, besides itself (direct dependencies, as its build file will declare them). */
    val mayUse: Map<String, Set<String>> =
        mapOf(
            "core" to emptySet(),
            "audit" to setOf("core"),
            "identity" to setOf("core"),
            "catalog" to setOf("core"),
            "booking" to setOf("catalog", "core"),
            "flight" to setOf("catalog", "booking", "core"),
            "accommodation" to setOf("catalog", "booking", "core"),
            "pricing" to setOf("flight", "catalog", "core"),
            "favorite" to setOf("flight", "catalog", "core"),
            "ai" to setOf("flight", "core"),
            "payment" to setOf("booking", "flight", "core"),
            "review" to setOf("booking", "accommodation", "flight", "core"),
            "trips" to setOf("booking", "flight", "accommodation", "review", "core"),
            "notification" to setOf("booking", "flight", "payment", "pricing", "identity", "core"),
            // the customer area manages users (block, anonymize), shows their trail and exports their favorites
            "admin" to setOf("identity", "audit", "favorite", "core"),
            "app" to conceptsOf.keys - "app",
        )

    /**
     * The shared vocabulary that lives inside `identity` and `audit` today but belongs to `core` in the target: who is
     * acting and what they may do (`Actor`, `Role`, `Permission`) and what is written to the trail (`AuditAction`,
     * `AuditEvent`, `AuditLog`). Every module uses it, so none of them may need `identity` or `audit` to get it.
     * Step 48.5 makes it true on disk; until then the rule already counts these classes as `core`.
     */
    private val sharedVocabulary: Set<String> =
        setOf("identity.Actor", "identity.Role", "identity.Permission").map { "com.dbook.domain.$it" }.toSet() +
            setOf("AuditAction", "AuditEvent", "AuditOutcome", "AuditLog").map { "com.dbook.domain.audit.$it" }.toSet()

    private val moduleOfConcept: Map<String, String> =
        conceptsOf.flatMap { (module, concepts) -> concepts.map { it to module } }.toMap()

    private val layers = setOf("domain", "application", "presentation")
    private val PERSISTENCE_PREFIX = listOf("com", "dbook", "infrastructure", "persistence")

    /** The module of a production class, or null for what is not mapped yet (config, infrastructure adapters). */
    fun moduleOf(javaClass: JavaClass): String? {
        // an array of a class belongs where its elements do
        val type = if (javaClass.isArray) javaClass.baseComponentType else javaClass
        if (type.name.substringBefore('$') in sharedVocabulary) return "core"
        val parts = type.packageName.split('.')
        val concept =
            when {
                parts.size > 3 && parts[0] == "com" && parts[1] == "dbook" && parts[2] in layers -> parts[3]
                parts.size > 4 && parts.subList(0, 4) == PERSISTENCE_PREFIX -> parts[4]
                else -> null
            }
        return concept?.let(moduleOfConcept::get)
    }
}
