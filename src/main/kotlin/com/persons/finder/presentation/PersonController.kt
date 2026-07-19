package com.persons.finder.presentation

import com.persons.finder.data.Person
import com.persons.finder.domain.services.PersonsService
import com.persons.finder.presentation.dto.CreatePersonRequest
import com.persons.finder.presentation.dto.CreatePersonResponse
import com.persons.finder.presentation.dto.LocationDto
import com.persons.finder.presentation.dto.NearbyPersonDto
import com.persons.finder.presentation.dto.NearbySearchRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.api.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("api/v1/persons")
@Tag(name = "Persons", description = "Manage persons and their locations")
class PersonController(
    private val personsService: PersonsService
) {

    /*
        TODO GET API to retrieve a person or persons name using their ids
        // Example
        // John has the list of people around them, now they need to retrieve everybody's names to display in the app
        // API would be called using person or persons ids
     */

    @Operation(
        summary = "Create a person",
        description = "Creates a person with an initial location and returns the generated id. " +
            "Name and location are required; job title and hobbies are optional."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Person created",
        content = [Content(examples = [ExampleObject(value = """{"id": 1}""")])]
    )
    @ApiResponse(
        responseCode = "400",
        description = "Validation failure, one entry per invalid field",
        content = [Content(examples = [ExampleObject(value = """{"name": ["must not be blank"]}""")])]
    )
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPerson(@Valid @RequestBody request: CreatePersonRequest): CreatePersonResponse {
        val person = personsService.createWithLocation(
            Person(
                name = request.name,
                jobTitle = request.jobTitle,
                hobbies = request.hobbies
            ),
            latitude = request.location.latitude,
            longitude = request.location.longitude
        )
        return CreatePersonResponse(person.id)
    }

    @Operation(
        summary = "Find persons near a location",
        description = "Returns ids of persons whose current location is within radiusKm of the " +
            "query point, closest first, with the great-circle distance in kilometres."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Persons within the radius, closest first",
        content = [Content(examples = [ExampleObject(value = """[{"id": 1, "distanceKm": 0.4}, {"id": 2, "distanceKm": 3.2}]""")])]
    )
    @ApiResponse(
        responseCode = "400",
        description = "Missing or invalid query parameters, one entry per invalid field",
        content = [Content(examples = [ExampleObject(value = """{"lat": ["must be less than or equal to 90.0"]}""")])]
    )
    @GetMapping("/nearby")
    fun findNearby(@Valid @ParameterObject request: NearbySearchRequest): List<NearbyPersonDto> {
        return personsService
            .findNearby(request.lat!!, request.lon!!, request.radiusKm!!)
            .map { NearbyPersonDto(it.personId, it.distanceKm) }
    }

    @Operation(
        summary = "Update a person's location",
        description = "Creates or replaces the person's current location."
    )
    @ApiResponse(responseCode = "204", description = "Location updated")
    @ApiResponse(
        responseCode = "400",
        description = "Validation failure, one entry per invalid field",
        content = [Content(examples = [ExampleObject(value = """{"latitude": ["must be less than or equal to 90.0"]}""")])]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Person does not exist",
        content = [Content(examples = [ExampleObject(value = """{"error": "Person 1 not found"}""")])]
    )
    @PutMapping("/{id}/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateLocation(@PathVariable id: Long, @Valid @RequestBody request: LocationDto) {
        personsService.updateLocation(id, request.latitude, request.longitude)
    }

}
