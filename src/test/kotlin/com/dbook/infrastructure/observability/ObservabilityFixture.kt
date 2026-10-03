package com.dbook.infrastructure.observability

import com.dbook.AbstractIntegrationTest
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalManagementPort
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

// The real application on two real ports: the public API one and the management one
// (actuator), which is what Prometheus and the health probes talk to in production.
// @AutoConfigureObservability: Spring Boot turns metric exporters OFF in tests by default,
// which would make /actuator/prometheus disappear here although it exists in production.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
abstract class ObservabilityFixture : AbstractIntegrationTest() {
    @LocalServerPort
    var port: Int = 0

    @LocalManagementPort
    var managementPort: Int = 0

    protected fun getFromApi(path: String): HttpResponse<String> = get(port, path)

    protected fun getFromManagement(path: String): HttpResponse<String> = get(managementPort, path)

    private fun get(
        targetPort: Int,
        path: String,
    ): HttpResponse<String> =
        HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI("http://localhost:$targetPort$path")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )
}
