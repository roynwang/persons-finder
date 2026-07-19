package com.persons.finder.presentation

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/health")
class HealthController @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate
) {

    @GetMapping("")
    fun health(): Map<String, String> {
        val databaseVersion = jdbcTemplate.queryForObject("select version()", String::class.java)
        return mapOf(
            "status" to "UP",
            "database" to (databaseVersion ?: "unknown")
        )
    }

}
