package com.dbook

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

// Shared ("singleton") Testcontainers Postgres: started once per test JVM, reused by
// every test class that extends this one, instead of one container per class (slow) or
// requiring a manually-run `docker compose up -d` (what M1-M3 did — fine for local dev,
// but CI has no such container running). Testcontainers' own reaper (Ryuk) stops it
// when the JVM exits; there is no explicit stop() call here on purpose.
// not a utility class: it's a base class meant to be extended (BookingConcurrencyTest,
// SecurityIntegrationTest), which is why it can't be an `object`. Can't be an interface
// either: it needs to carry the class-level @SpringBootTest annotation and the
// companion object that starts the shared container.
@Suppress("UtilityClassWithPublicConstructor", "UnnecessaryAbstractClass")
@SpringBootTest
abstract class AbstractIntegrationTest {
    companion object {
        private val postgres =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("dbook")
                .withUsername("dbook")
                .withPassword("dbook")
                .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun overrideDatasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
