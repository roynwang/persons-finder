package com.persons.finder.domain.services

import com.persons.finder.data.Location
import com.persons.finder.domain.repositories.LocationRepository
import org.springframework.stereotype.Service

@Service
class LocationsServiceImpl(
    private val locationRepository: LocationRepository
) : LocationsService {

    override fun addLocation(location: Location) {
        locationRepository.save(location)
    }

    override fun removeLocation(personId: Long) {
        if (locationRepository.existsById(personId)) {
            locationRepository.deleteById(personId)
        }
    }

    override fun findAround(latitude: Double, longitude: Double, radiusInKm: Double): List<Location> {
        TODO("Not yet implemented")
    }

}
