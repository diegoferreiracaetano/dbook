package com.dbook

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
class DbookApplication

@Suppress("SpreadOperator") // standard Spring Boot Kotlin bootstrap idiom, runs once at startup
fun main(args: Array<String>) {
    runApplication<DbookApplication>(*args)
}
