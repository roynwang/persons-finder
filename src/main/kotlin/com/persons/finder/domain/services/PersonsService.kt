package com.persons.finder.domain.services

import com.persons.finder.data.Person

interface PersonsService {
    fun getById(id: Long): Person
    fun save(person: Person): Person

    /** Creates the person together with their initial location, atomically. */
    fun createWithLocation(person: Person, latitude: Double, longitude: Double): Person

    /**
     * Creates or replaces the person's current location.
     * @throws PersonNotFoundException if the person does not exist
     */
    fun updateLocation(id: Long, latitude: Double, longitude: Double)

    /**
     * Finds persons with a known location within radiusKm of the point,
     * each with their details and distance, closest first.
     */
    fun findNearby(latitude: Double, longitude: Double, radiusKm: Double): List<NearbyPersonResult>
}

/** A person paired with their distance from a query point. */
data class NearbyPersonResult(
    val person: Person,
    val distanceKm: Double
)
