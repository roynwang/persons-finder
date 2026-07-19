package com.persons.finder.presentation

import com.persons.finder.config.JacksonConfig
import com.persons.finder.data.Person
import com.persons.finder.domain.services.NearbyPerson
import com.persons.finder.domain.services.PersonNotFoundException
import com.persons.finder.domain.services.PersonsService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

/**
 * Web-layer concerns only, against a mocked PersonsService, one @Nested class
 * per endpoint: happy path (status, body, delegation arguments) plus ONE
 * representative routing test per distinct error path. Error-body logic lives
 * in ApiExceptionHandlerTest, domain rules in *ServiceImplTest, persistence in
 * the e2e suite against real PostgreSQL.
 */
@WebMvcTest(PersonController::class)
@Import(JacksonConfig::class)
class PersonControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @MockBean
    private lateinit var personsService: PersonsService

    @Nested
    inner class CreatePerson {

        @Test
        fun `returns 201 with the generated id and delegates the full payload`() {
            val toCreate = Person(name = "John Doe", jobTitle = "Developer", hobbies = "chess, hiking")
            `when`(personsService.createWithLocation(toCreate, latitude = -36.8485, longitude = 174.7633))
                .thenReturn(toCreate.copy(id = 42))

            mockMvc.post("/api/v1/persons") {
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
                jsonPath("$.id") { value(42) }
            }

            verify(personsService).createWithLocation(toCreate, latitude = -36.8485, longitude = 174.7633)
        }

        // Routing: MethodArgumentNotValidException -> 400 field map
        @Test
        fun `rejects a blank name with a field error`() {
            mockMvc.post("/api/v1/persons") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "name": "", "location": { "latitude": 0.0, "longitude": 0.0 } }"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.name") { exists() }
            }

            verifyNoInteractions(personsService)
        }

        // Routing: strict coercion -> HttpMessageNotReadableException -> 400
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

    }

    @Nested
    inner class FindNearby {

        @Test
        fun `returns 200 with persons closest first`() {
            `when`(personsService.findNearby(latitude = 10.0, longitude = 10.0, radiusKm = 10.0))
                .thenReturn(listOf(NearbyPerson(1, 1.1), NearbyPerson(2, 5.5)))

            mockMvc.get("/api/v1/persons/nearby") {
                param("lat", "10.0")
                param("lon", "10.0")
                param("radiusKm", "10.0")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].id") { value(1) }
                jsonPath("$[0].distanceKm") { value(1.1) }
                jsonPath("$[1].id") { value(2) }
            }
        }

        // Routing: BindException (query DTO validation) -> 400 field map
        @Test
        fun `rejects an out-of-range latitude`() {
            mockMvc.get("/api/v1/persons/nearby") {
                param("lat", "91.0")
                param("lon", "0.0")
                param("radiusKm", "5.0")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.lat") { exists() }
            }

            verifyNoInteractions(personsService)
        }

        // Routing: query binding failure inside BindException -> 400
        @Test
        fun `rejects a non-numeric radius`() {
            mockMvc.get("/api/v1/persons/nearby") {
                param("lat", "0.0")
                param("lon", "0.0")
                param("radiusKm", "abc")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.radiusKm") { exists() }
            }
        }

    }

    @Nested
    inner class UpdateLocation {

        @Test
        fun `returns 204 and delegates the coordinates`() {
            mockMvc.put("/api/v1/persons/5/location") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "latitude": 51.5074, "longitude": -0.1278 }"""
            }.andExpect {
                status { isNoContent() }
            }

            verify(personsService).updateLocation(5, latitude = 51.5074, longitude = -0.1278)
        }

        // Routing: PersonNotFoundException -> 404
        @Test
        fun `returns 404 for an unknown person`() {
            doThrow(PersonNotFoundException(999999))
                .`when`(personsService).updateLocation(999999, latitude = 0.0, longitude = 0.0)

            mockMvc.put("/api/v1/persons/999999/location") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "latitude": 0.0, "longitude": 0.0 }"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error") { exists() }
            }
        }

        // Routing: MethodArgumentTypeMismatchException -> 400
        @Test
        fun `rejects a non-numeric person id`() {
            mockMvc.put("/api/v1/persons/abc/location") {
                contentType = MediaType.APPLICATION_JSON
                content = """{ "latitude": 0.0, "longitude": 0.0 }"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.id") { exists() }
            }

            verifyNoInteractions(personsService)
        }

    }

}
