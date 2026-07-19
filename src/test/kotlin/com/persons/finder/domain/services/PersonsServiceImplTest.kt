package com.persons.finder.domain.services

import com.persons.finder.data.Person
import com.persons.finder.domain.repositories.PersonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional

class PersonsServiceImplTest {

    private val personRepository = mock(PersonRepository::class.java)
    private val service = PersonsServiceImpl(personRepository)

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

}
