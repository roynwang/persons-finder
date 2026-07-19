package com.persons.finder.domain.repositories

import com.persons.finder.data.Person
import org.springframework.data.jpa.repository.JpaRepository

interface PersonRepository : JpaRepository<Person, Long>
