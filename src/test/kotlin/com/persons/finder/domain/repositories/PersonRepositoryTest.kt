package com.persons.finder.domain.repositories

import com.persons.finder.data.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.repository.findByIdOrNull

// replace = NONE: use the H2 datasource from test application.properties so
// Flyway migrates the real V1 schema and Hibernate validates against it.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersonRepositoryTest @Autowired constructor(
    private val personRepository: PersonRepository
) {

    @Test
    fun `person round-trips with a generated id and null optional fields`() {
        val saved = personRepository.save(Person(name = "John", jobTitle = null, hobbies = null))

        assertTrue(saved.id > 0)
        val found = personRepository.findByIdOrNull(saved.id)!!
        assertEquals("John", found.name)
        assertNull(found.jobTitle)
        assertNull(found.hobbies)
    }

    @Test
    fun `populated optional fields round-trip`() {
        val saved = personRepository.save(
            Person(name = "Jane", jobTitle = "Developer", hobbies = "chess, hiking")
        )

        val found = personRepository.findByIdOrNull(saved.id)!!
        assertEquals("Developer", found.jobTitle)
        assertEquals("chess, hiking", found.hobbies)
    }

}
