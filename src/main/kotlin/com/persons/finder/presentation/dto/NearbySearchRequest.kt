package com.persons.finder.presentation.dto

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.DecimalMax
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive

/**
 * Query parameters for the nearby search. Fields are nullable so a missing
 * parameter surfaces as a @NotNull field error in the standard error shape
 * instead of a binding crash.
 */
data class NearbySearchRequest(
    @field:NotNull
    @field:DecimalMin("-90.0") @field:DecimalMax("90.0")
    @field:Schema(description = "Latitude of the query point", example = "-36.8485", required = true)
    val lat: Double?,

    @field:NotNull
    @field:DecimalMin("-180.0") @field:DecimalMax("180.0")
    @field:Schema(description = "Longitude of the query point", example = "174.7633", required = true)
    val lon: Double?,

    @field:NotNull
    @field:Positive
    @field:DecimalMax("20000.0")
    @field:Schema(
        description = "Search radius in kilometres (max 20000, roughly half the Earth's circumference)",
        example = "10.0",
        required = true
    )
    val radiusKm: Double?
)

data class NearbyPersonDto(
    @field:Schema(description = "Id of the person")
    val id: Long,

    @field:Schema(description = "Great-circle distance from the query point, in kilometres")
    val distanceKm: Double
)
