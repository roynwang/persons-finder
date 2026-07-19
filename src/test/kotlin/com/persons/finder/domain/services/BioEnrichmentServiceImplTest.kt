package com.persons.finder.domain.services

import com.persons.finder.data.Person
import com.persons.finder.domain.repositories.PersonRepository
import com.persons.finder.llm.BioGenerator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import java.util.Optional

/**
 * enrichAsync is @Async in production, but without a Spring context the
 * annotation is inert, so these tests exercise it as a plain synchronous method.
 */
class BioEnrichmentServiceImplTest {

    private val personRepository = mock(PersonRepository::class.java)
    private val bioGenerator = mock(BioGenerator::class.java)
    private val service = BioEnrichmentServiceImpl(personRepository, bioGenerator)

    private val person = Person(id = 7, name = "Jane", jobTitle = "Developer", hobbies = "chess")

    @Test
    fun `does nothing when the generator is disabled`() {
        `when`(bioGenerator.enabled).thenReturn(false)

        service.enrichAsync(7)

        verifyNoInteractions(personRepository)
        verify(bioGenerator, never()).generate(person)
    }

    @Test
    fun `does nothing when the person no longer exists`() {
        `when`(bioGenerator.enabled).thenReturn(true)
        `when`(personRepository.findById(7)).thenReturn(Optional.empty())

        service.enrichAsync(7)

        verify(bioGenerator, never()).generate(person)
        verify(personRepository, never()).updateBio(anyLong(), anyString())
    }

    @Test
    fun `generates a bio and writes it back`() {
        `when`(bioGenerator.enabled).thenReturn(true)
        `when`(personRepository.findById(7)).thenReturn(Optional.of(person))
        `when`(bioGenerator.generate(person)).thenReturn("Jane is a developer who loves chess.")

        service.enrichAsync(7)

        verify(personRepository).updateBio(7, "Jane is a developer who loves chess.")
    }

    @Test
    fun `truncates a bio longer than the column limit`() {
        `when`(bioGenerator.enabled).thenReturn(true)
        `when`(personRepository.findById(7)).thenReturn(Optional.of(person))
        `when`(bioGenerator.generate(person)).thenReturn("x".repeat(2000))

        service.enrichAsync(7)

        verify(personRepository).updateBio(7, "x".repeat(1024))
    }

    @Test
    fun `swallows generator failures and leaves the bio untouched`() {
        `when`(bioGenerator.enabled).thenReturn(true)
        `when`(personRepository.findById(7)).thenReturn(Optional.of(person))
        `when`(bioGenerator.generate(person)).thenThrow(IllegalStateException("Gemini returned no bio text"))

        service.enrichAsync(7) // must not throw

        verify(personRepository, never()).updateBio(anyLong(), anyString())
    }
}
