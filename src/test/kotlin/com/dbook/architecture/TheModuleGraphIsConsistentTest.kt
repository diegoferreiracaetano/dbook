package com.dbook.architecture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The map the rule checks must itself be sound, or the rule proves nothing.
class TheModuleGraphIsConsistentTest : ArchitectureFixture() {
    @Test
    fun `given the concepts of the code when mapped then every one belongs to a module`() {
        val mapped = ModuleGraph.conceptsOf.values.flatten().toSet()

        val forgotten = (concepts + "common").toSet() - mapped

        assertEquals(emptySet<String>(), forgotten, "a concept with no module: it would escape the rule")
    }

    @Test
    fun `given the modules when their dependencies are followed then there is no cycle`() {
        // a module is "placed" once everything it may use is placed: the order Gradle will build them in
        val placed = mutableSetOf<String>()
        var progress = true
        while (progress) {
            val next =
                ModuleGraph.mayUse.filter {
                        (module, uses) ->
                    module !in placed && placed.containsAll(uses)
                }.keys
            progress = next.isNotEmpty()
            placed += next
        }

        assertEquals(ModuleGraph.mayUse.keys, placed, "modules in a cycle: ${ModuleGraph.mayUse.keys - placed}")
    }

    @Test
    fun `given the modules when their dependencies are read then each is known and core is shared`() {
        ModuleGraph.mayUse.forEach { (module, uses) ->
            assertTrue(ModuleGraph.conceptsOf.keys.containsAll(uses), "$module uses a module that does not exist")
            if (module != "core") assertTrue("core" in uses, "$module should use core")
        }
    }
}
