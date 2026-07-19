package com.persons.finder.domain.services

import com.persons.finder.data.Location
import com.persons.finder.data.Person
import com.persons.finder.domain.repositories.PersonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.Optional

class PersonsServiceImplTest {

    private val personRepository = mock(PersonRepository::class.java)
    private val locationsService = mock(LocationsService::class.java)
    private val service = PersonsServiceImpl(personRepository, locationsService)

    @Test
    fun `getById returns the person when present`() {
        val person = Person(id = 1, name = "John")
        `when`(personRepository.findById(1)).thenReturn(Optional.of(person))

        assertEquals(person, service.getById(1))
    }

    @Test
    fun `getById throws PersonNotFoundException when absent`() {
        `when`(personRepository.findById(99)).thenReturn(Optional.empty())

        assertThrows(PersonNotFoundException::class.java) { service.getById(99) }
    }

    @Test
    fun `save delegates to the repository and returns the persisted person`() {
        val toSave = Person(name = "Jane")
        val saved = toSave.copy(id = 5)
        `when`(personRepository.save(toSave)).thenReturn(saved)

        val result = service.save(toSave)

        assertEquals(5, result.id)
        verify(personRepository).save(toSave)
    }

    @Test
    fun `createWithLocation saves the person and adds their location`() {
        val toSave = Person(name = "Jane")
        val saved = toSave.copy(id = 7)
        `when`(personRepository.save(toSave)).thenReturn(saved)

        val result = service.createWithLocation(toSave, latitude = 1.0, longitude = 2.0)

        assertEquals(7, result.id)
        verify(locationsService).addLocation(Location(personId = 7, latitude = 1.0, longitude = 2.0))
    }

    @Test
    fun `updateLocation upserts the location for an existing person`() {
        val person = Person(id = 3, name = "Mover")
        `when`(personRepository.findById(3)).thenReturn(Optional.of(person))

        service.updateLocation(3, latitude = 4.0, longitude = 5.0)

        verify(locationsService).addLocation(Location(personId = 3, latitude = 4.0, longitude = 5.0))
    }

    @Test
    fun `findNearby delegates to the locations service`() {
        val nearby = listOf(NearbyPerson(personId = 3, distanceKm = 0.5))
        `when`(locationsService.findAround(1.0, 2.0, 10.0)).thenReturn(nearby)

        assertEquals(nearby, service.findNearby(latitude = 1.0, longitude = 2.0, radiusKm = 10.0))
    }

    @Test
    fun `updateLocation throws for an unknown person and writes nothing`() {
        `when`(personRepository.findById(99)).thenReturn(Optional.empty())

        assertThrows(PersonNotFoundException::class.java) {
            service.updateLocation(99, latitude = 0.0, longitude = 0.0)
        }
        verifyNoInteractions(locationsService)
    }

}
