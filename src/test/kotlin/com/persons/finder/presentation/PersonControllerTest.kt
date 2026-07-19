package com.persons.finder.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.persons.finder.domain.repositories.LocationRepository
import com.persons.finder.domain.repositories.PersonRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional

/**
 * Web-layer concerns: status codes, response body, request validation and the
 * error-response shape. Persistence mapping is covered by PersonPersistenceTest
 * and service logic by the *ServiceImplTest classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val personRepository: PersonRepository,
    private val locationRepository: LocationRepository
) {

    @Test
    fun `create person returns 201 with an id and persists both rows`() {
        val result = mockMvc.post("/api/v1/persons") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "name": "John Doe",
                  "jobTitle": "Developer",
                  "hobbies": "chess, hiking",
                  "location": { "latitude": -36.8485, "longitude": 174.7633 }
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNumber() }
        }.andReturn()

        val id = ObjectMapper().readTree(result.response.contentAsString)["id"].asLong()
        assertTrue(personRepository.existsById(id), "person row should exist")
        assertTrue(locationRepository.existsById(id), "location row should exist")
    }

    @Test
    fun `create person without optional fields succeeds`() {
        mockMvc.post("/api/v1/persons") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "name": "Minimal", "location": { "latitude": 0.0, "longitude": 0.0 } }"""
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    fun `blank name is rejected with a field error`() {
        mockMvc.post("/api/v1/persons") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "name": "", "location": { "latitude": 0.0, "longitude": 0.0 } }"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.name") { exists() }
        }
    }

    @Test
    fun `out of range latitude is rejected`() {
        mockMvc.post("/api/v1/persons") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "name": "Bad Lat", "location": { "latitude": 91.0, "longitude": 0.0 } }"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$['location.latitude']") { exists() }
        }
    }

    @Test
    fun `numeric value sent as a string is rejected`() {
        mockMvc.post("/api/v1/persons") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "name": "Str Lat", "location": { "latitude": "45.0", "longitude": 10.0 } }"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$['location.latitude']") { exists() }
        }
    }

    @Test
    fun `non-numeric string latitude is rejected`() {
        mockMvc.post("/api/v1/persons") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "name": "Bad Lat", "location": { "latitude": "abc", "longitude": 10.0 } }"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$['location.latitude']") { exists() }
        }
    }

    @Test
    fun `missing location is rejected`() {
        mockMvc.post("/api/v1/persons") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "name": "No Location" }"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

}
