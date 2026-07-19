package com.persons.finder.domain.services

import com.persons.finder.data.Location
import com.persons.finder.domain.repositories.LocationRepository
import org.springframework.stereotype.Service
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

@Service
class LocationsServiceImpl(
    private val locationRepository: LocationRepository
) : LocationsService {

    override fun addLocation(location: Location) {
        locationRepository.save(location)
    }

    override fun removeLocation(personId: Long) {
        if (locationRepository.existsById(personId)) {
            locationRepository.deleteById(personId)
        }
    }

    /**
     * Distance filtering and ordering happen in the database; this method's
     * job is the bounding box: a lat/lon window guaranteed to contain the
     * radius circle, so the query can prefilter via the lat/lon index.
     */
    override fun findAround(latitude: Double, longitude: Double, radiusInKm: Double): List<NearbyPerson> {
        val degreeDelta = radiusInKm / KM_PER_DEGREE
        val minLat = max(latitude - degreeDelta, -90.0)
        val maxLat = min(latitude + degreeDelta, 90.0)

        // Longitude degrees shrink toward the poles, so the longitude half-width
        // is the latitude half-width scaled by 1/cos(lat).
        val cosLat = cos(Math.toRadians(latitude))
        val lonDelta = if (cosLat > MIN_COS_LATITUDE) degreeDelta / cosLat else FULL_LON_RANGE

        // A circle that reaches a pole, spans at least half the globe in
        // longitude, or wraps past ±180 covers every meridian, so no single
        // longitude BETWEEN range can hold it — search the full range. This
        // sacrifices the index prefilter for these queries; the eventual
        // PostGIS migration removes the bounding box entirely.
        val allLongitudes = maxLat >= 90.0 || minLat <= -90.0 || lonDelta >= 180.0 ||
            longitude - lonDelta < -180.0 || longitude + lonDelta > 180.0
        val minLon = if (allLongitudes) -180.0 else longitude - lonDelta
        val maxLon = if (allLongitudes) 180.0 else longitude + lonDelta

        return locationRepository
            .findNearby(latitude, longitude, radiusInKm, minLat, maxLat, minLon, maxLon)
            .map { NearbyPerson(it.personId, it.distanceKm) }
    }

    companion object {
        // Slightly below the true km-per-degree of latitude (~111.2) so the
        // box always errs on the wide side of the circle.
        private const val KM_PER_DEGREE = 110.574
        private const val MIN_COS_LATITUDE = 1e-6
        private const val FULL_LON_RANGE = 360.0
    }

}
