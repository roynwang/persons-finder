package com.persons.finder.presentation.dto

import javax.validation.Valid
import javax.validation.constraints.DecimalMax
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class CreatePersonRequest(
    @field:NotBlank
    @field:Size(max = 128)
    val name: String,

    @field:Size(max = 128)
    val jobTitle: String? = null,

    @field:Size(max = 512)
    val hobbies: String? = null,

    @field:Valid
    val location: LocationDto
)

data class LocationDto(
    @field:DecimalMin("-90.0") @field:DecimalMax("90.0")
    val latitude: Double,

    @field:DecimalMin("-180.0") @field:DecimalMax("180.0")
    val longitude: Double
)

data class CreatePersonResponse(
    val id: Long
)
