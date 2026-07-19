package com.persons.finder.data

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "person")
data class Person(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 128)
    val name: String,

    @Column(name = "job_title", length = 128)
    val jobTitle: String? = null,

    @Column(length = 512)
    val hobbies: String? = null
)
