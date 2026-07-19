package com.persons.finder.domain.services

import com.persons.finder.data.Person
import com.persons.finder.domain.repositories.PersonRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class PersonsServiceImpl(
    private val personRepository: PersonRepository
) : PersonsService {

    override fun getById(id: Long): Person {
        return personRepository.findByIdOrNull(id)
            ?: throw PersonNotFoundException(id)
    }

    override fun save(person: Person): Person {
        return personRepository.save(person)
    }

}

class PersonNotFoundException(id: Long) : RuntimeException("Person $id not found")
