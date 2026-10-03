package com.dbook.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption

// The architecture rules documented in .claude/CLAUDE.md, checked on every build instead
// of by hand. Only production classes are imported (the test sources are excluded).
abstract class ArchitectureFixture {
    protected val productionClasses: JavaClasses = importedProductionClasses

    protected val concepts = listOf("catalog", "seating", "booking", "payment", "review", "identity", "ai")

    private companion object {
        // importing the class files is the slow part: do it once per test JVM
        val importedProductionClasses: JavaClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages("com.dbook")
    }
}
