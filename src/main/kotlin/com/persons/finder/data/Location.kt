package com.persons.finder.data

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "location")
data class Location(
    @Id
    @Column(name = "person_id")
    val personId: Long,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double
)
