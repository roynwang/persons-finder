package com.persons.finder.domain.services

import com.persons.finder.data.Location
import com.persons.finder.domain.repositories.LocationRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class LocationsServiceImplTest {

    private val locationRepository = mock(LocationRepository::class.java)
    private val service = LocationsServiceImpl(locationRepository)

    @Test
    fun `addLocation saves the location`() {
        val location = Location(personId = 1, latitude = 1.0, longitude = 2.0)

        service.addLocation(location)

        verify(locationRepository).save(location)
    }

    @Test
    fun `removeLocation deletes when the location exists`() {
        `when`(locationRepository.existsById(1)).thenReturn(true)

        service.removeLocation(1)

        verify(locationRepository).deleteById(1)
    }

    @Test
    fun `removeLocation is a no-op when the location is absent`() {
        `when`(locationRepository.existsById(2)).thenReturn(false)

        service.removeLocation(2)

        verify(locationRepository, never()).deleteById(anyLong())
    }

}
