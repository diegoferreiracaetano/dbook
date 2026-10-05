package com.dbook.presentation.customerhistory

import com.dbook.presentation.customerprofile.CustomerProfileFixture
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get

abstract class CustomerHistoryFixture : CustomerProfileFixture() {
    /** GET /v1/admin/customers/{id}/{list} with an optional page and size. */
    protected fun history(
        staffToken: String,
        customerId: Long,
        list: String,
        vararg params: Pair<String, String>,
    ): MvcResult =
        mockMvc.get("/v1/admin/customers/$customerId/$list") {
            header("Authorization", "Bearer $staffToken")
            params.forEach { (name, value) -> param(name, value) }
        }.andReturn()

    protected fun idsOf(result: MvcResult): List<Long> = bodyOf(result)["items"].map { it["id"].asLong() }
}
