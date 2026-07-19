package com.persons.finder.presentation.dto

import io.swagger.v3.oas.annotations.media.Schema

data class PersonDto(
    val id: Long,
    val name: String,
    val jobTitle: String?,
    val hobbies: String?,
    @field:Schema(description = "AI-generated bio; null until background generation completes (or when generation is disabled/failed)")
    val bio: String?
)
