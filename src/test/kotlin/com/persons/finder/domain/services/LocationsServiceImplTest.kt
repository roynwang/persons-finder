package com.persons.finder.domain.services

import com.persons.finder.data.Location
import com.persons.finder.domain.repositories.LocationRepository
import com.persons.finder.domain.repositories.NearbyLocationProjection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.doubleThat
import org.mockito.ArgumentMatchers.eq
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

    @Test
    fun `findAround maps query rows to nearby persons in order`() {
        stubFindNearby(listOf(projection(2, 2.2), projection(1, 5.5)))

        val result = service.findAround(latitude = 0.0, longitude = 0.0, radiusInKm = 10.0)

        assertEquals(listOf(NearbyPerson(2, 2.2), NearbyPerson(1, 5.5)), result)
    }

    @Test
    fun `findAround queries a bounding box that covers the radius circle`() {
        stubFindNearby(emptyList())

        service.findAround(latitude = 40.0, longitude = 10.0, radiusInKm = 10.0)

        // 10 km is ~0.090° of latitude and ~0.117° of longitude at 40°N; the
        // box must reach at least that far on every side.
        verify(locationRepository).findNearby(
            eq(40.0), eq(10.0), eq(10.0),
            doubleThat { it <= 40.0 - 0.089 }, doubleThat { it >= 40.0 + 0.089 },
            doubleThat { it <= 10.0 - 0.117 }, doubleThat { it >= 10.0 + 0.117 }
        )
    }

    @Test
    fun `findAround crossing the antimeridian widens to the full longitude range`() {
        stubFindNearby(emptyList())

        service.findAround(latitude = 0.0, longitude = 179.99, radiusInKm = 10.0)

        verify(locationRepository).findNearby(
            eq(0.0), eq(179.99), eq(10.0),
            anyDouble(), anyDouble(), eq(-180.0), eq(180.0)
        )
    }

    @Test
    fun `findAround near a pole clamps latitude and searches all longitudes`() {
        stubFindNearby(emptyList())

        service.findAround(latitude = 89.9999, longitude = 0.0, radiusInKm = 10.0)

        verify(locationRepository).findNearby(
            eq(89.9999), eq(0.0), eq(10.0),
            anyDouble(), eq(90.0), eq(-180.0), eq(180.0)
        )
    }

    private fun stubFindNearby(rows: List<NearbyLocationProjection>) {
        `when`(
            locationRepository.findNearby(
                anyDouble(), anyDouble(), anyDouble(),
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
            )
        ).thenReturn(rows)
    }

    private fun projection(id: Long, distance: Double) = object : NearbyLocationProjection {
        override val personId = id
        override val distanceKm = distance
    }

}
