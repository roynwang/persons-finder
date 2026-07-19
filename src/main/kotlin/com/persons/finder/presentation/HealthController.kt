package com.persons.finder.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/health")
@Tag(name = "Health", description = "Service health")
class HealthController @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate
) {

    @Operation(
        summary = "Health check",
        description = "Verifies the service is up and can reach the database. " +
            "Performs a real round-trip to PostgreSQL and returns its version."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Service and database are healthy",
        content = [Content(
            examples = [ExampleObject(
                value = """{"status": "UP", "database": "PostgreSQL 16.14 on aarch64-unknown-linux-musl"}"""
            )]
        )]
    )
    @GetMapping("")
    fun health(): Map<String, String> {
        val databaseVersion = jdbcTemplate.queryForObject("select version()", String::class.java)
        return mapOf(
            "status" to "UP",
            "database" to (databaseVersion ?: "unknown")
        )
    }

}
