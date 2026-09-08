package com.dbook

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DbookApplication

fun main(args: Array<String>) {
	runApplication<DbookApplication>(*args)
}
