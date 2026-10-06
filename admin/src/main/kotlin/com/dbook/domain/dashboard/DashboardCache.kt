package com.dbook.domain.dashboard

/**
 * A short-lived copy of an expensive answer. [load] runs when there is no copy (or the cache is down: it never gets in
 * the way of the answer, only of its speed).
 */
interface DashboardCache {
    fun <T : Any> remember(
        key: String,
        type: Class<T>,
        load: () -> T,
    ): T
}
