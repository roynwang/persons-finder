package com.persons.finder.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.persons.finder.domain.repositories.LocationRepository
import com.persons.finder.domain.repositories.PersonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.transaction.annotation.Transactional

/**
 * Web-layer concerns: status codes, response body, request validation and the
 * error-response shape, one @Nested class per endpoint. Persistence mapping is
 * covered by the *RepositoryTest classes, service logic by *ServiceImplTest,
 * and error-body construction by ApiExceptionHandlerTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val personRepository: PersonRepository,
    private val locationRepository: LocationRepository
) {

    @Nested
    inner class CreatePerson {

        @Test
        fun `returns 201 with an id and persists both rows`() {
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
        fun `succeeds without optional fields`() {
            mockMvc.post("/api/v1/persons") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "name": "Minimal", "location": { "latitude": 0.0, "longitude": 0.0 } }"""
            }.andExpect {
                status { isCreated() }
            }
        }

        @Test
        fun `rejects a blank name with a field error`() {
            mockMvc.post("/api/v1/persons") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "name": "", "location": { "latitude": 0.0, "longitude": 0.0 } }"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.name") { exists() }
            }
        }

        @Test
        fun `rejects an out-of-range latitude`() {
            mockMvc.post("/api/v1/persons") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "name": "Bad Lat", "location": { "latitude": 91.0, "longitude": 0.0 } }"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$['location.latitude']") { exists() }
            }
        }

        @Test
        fun `rejects a numeric value sent as a string`() {
            mockMvc.post("/api/v1/persons") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "name": "Str Lat", "location": { "latitude": "45.0", "longitude": 10.0 } }"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$['location.latitude']") { exists() }
            }
        }

        @Test
        fun `rejects a missing location`() {
            mockMvc.post("/api/v1/persons") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "name": "No Location" }"""
            }.andExpect {
                status { isBadRequest() }
            }
        }

    }

    @Nested
    inner class UpdateLocation {

        @Test
        fun `returns 204 and replaces the stored coordinates`() {
            val id = createPerson("Mover")

            mockMvc.put("/api/v1/persons/$id/location") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "latitude": 51.5074, "longitude": -0.1278 }"""
            }.andExpect {
                status { isNoContent() }
            }

            val location = locationRepository.findByIdOrNull(id)!!
            assertEquals(51.5074, location.latitude)
            assertEquals(-0.1278, location.longitude)
        }

        @Test
        fun `returns 404 for an unknown person`() {
            mockMvc.put("/api/v1/persons/999999/location") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "latitude": 0.0, "longitude": 0.0 }"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error") { exists() }
            }
        }

        @Test
        fun `rejects an out-of-range longitude`() {
            val id = createPerson("Bad Lon")

            mockMvc.put("/api/v1/persons/$id/location") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "latitude": 0.0, "longitude": 181.0 }"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.longitude") { exists() }
            }
        }

        @Test
        fun `rejects a non-numeric person id`() {
            mockMvc.put("/api/v1/persons/abc/location") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "latitude": 0.0, "longitude": 0.0 }"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.id") { exists() }
            }
        }

    }

    private fun createPerson(name: String): Long {
        val result = mockMvc.post("/api/v1/persons") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "name": "$name", "location": { "latitude": 0.0, "longitude": 0.0 } }"""
        }.andReturn()
        return ObjectMapper().readTree(result.response.contentAsString)["id"].asLong()
    }

}
