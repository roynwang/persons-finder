package com.persons.finder.domain.repositories

import com.persons.finder.data.Location
import com.persons.finder.data.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull

// replace = NONE: use the H2 datasource from test application.properties so
// Flyway migrates the real V1 schema and Hibernate validates against it.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LocationRepositoryTest @Autowired constructor(
    private val personRepository: PersonRepository,
    private val locationRepository: LocationRepository
) {

    @Test
    fun `location round-trips keyed by person id`() {
        val person = personRepository.save(Person(name = "Jane"))
        locationRepository.save(Location(personId = person.id, latitude = -36.85, longitude = 174.76))

        val found = locationRepository.findByIdOrNull(person.id)!!
        assertEquals(-36.85, found.latitude)
        assertEquals(174.76, found.longitude)
    }

    @Test
    fun `saving again for the same person updates instead of duplicating`() {
        val person = personRepository.save(Person(name = "Mover"))
        locationRepository.save(Location(personId = person.id, latitude = 1.0, longitude = 2.0))

        locationRepository.save(Location(personId = person.id, latitude = 3.0, longitude = 4.0))
        locationRepository.flush()

        assertEquals(1, locationRepository.count())
        val found = locationRepository.findByIdOrNull(person.id)!!
        assertEquals(3.0, found.latitude)
        assertEquals(4.0, found.longitude)
    }

    @Test
    fun `location for a nonexistent person is rejected by the foreign key`() {
        assertThrows(DataIntegrityViolationException::class.java) {
            locationRepository.save(Location(personId = 99999, latitude = 0.0, longitude = 0.0))
            locationRepository.flush()
        }
    }

}
