package com.persons.finder.domain.services

import com.persons.finder.data.Location
import com.persons.finder.data.Person
import com.persons.finder.domain.repositories.PersonRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersonsServiceImpl(
    private val personRepository: PersonRepository,
    private val locationsService: LocationsService
) : PersonsService {

    override fun getById(id: Long): Person {
        return personRepository.findByIdOrNull(id)
            ?: throw PersonNotFoundException(id)
    }

    override fun save(person: Person): Person {
        return personRepository.save(person)
    }

    @Transactional
    override fun createWithLocation(person: Person, latitude: Double, longitude: Double): Person {
        val saved = personRepository.save(person)
        locationsService.addLocation(Location(saved.id, latitude, longitude))
        return saved
    }

    @Transactional
    override fun updateLocation(id: Long, latitude: Double, longitude: Double) {
        val person = getById(id)
        locationsService.addLocation(Location(person.id, latitude, longitude))
    }

    @Transactional(readOnly = true)
    override fun findNearby(latitude: Double, longitude: Double, radiusKm: Double): List<NearbyPersonResult> {
        val nearby = locationsService.findAround(latitude, longitude, radiusKm)
        // Batch-load the persons in one query, then re-attach in the
        // distance-sorted order (findAllById does not preserve it).
        val byId = personRepository.findAllById(nearby.map { it.personId }).associateBy { it.id }
        return nearby.mapNotNull { hit -> byId[hit.personId]?.let { NearbyPersonResult(it, hit.distanceKm) } }
    }

}

class PersonNotFoundException(id: Long) : RuntimeException("Person $id not found")
