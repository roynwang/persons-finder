package com.persons.finder.domain.services

import com.persons.finder.data.Location

interface LocationsService {
    fun addLocation(location: Location)
    fun removeLocation(personId: Long)

    /**
     * Finds persons with a location within radiusInKm of the point
     * (great-circle distance), closest first.
     */
    fun findAround(latitude: Double, longitude: Double, radiusInKm: Double): List<NearbyPerson>
}

/** A person's id paired with their distance from a query point. */
data class NearbyPerson(
    val personId: Long,
    val distanceKm: Double
)
