package com.persons.finder.domain.repositories

import com.persons.finder.data.Person
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface PersonRepository : JpaRepository<Person, Long> {

    // Targeted write-back for the async bio worker: a no-op when the person was
    // deleted mid-generation, and cannot clobber concurrent changes to other
    // fields the way saving a stale entity would.
    @Modifying
    @Transactional
    @Query("update Person p set p.bio = :bio where p.id = :id")
    fun updateBio(@Param("id") id: Long, @Param("bio") bio: String): Int
}
