package com.persons.finder.domain.repositories

import com.persons.finder.data.Location
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LocationRepository : JpaRepository<Location, Long> {

    /**
     * Persons within radiusKm of the query point, closest first, distance
     * computed in the database (haversine, spherical Earth). The bounding-box
     * predicate is the index-backed prefilter (idx_location_lat_lon); the
     * distance comparison is the exact filter. Covered by the nearby e2e
     * scenario (no DB in unit tests).
     */
    @Query(
        nativeQuery = true,
        value = """
            SELECT * FROM (
                SELECT l.person_id AS "personId",
                       2 * 6371.0088 * ASIN(SQRT(
                           POWER(SIN(RADIANS(l.latitude - :lat) / 2), 2) +
                           COS(RADIANS(:lat)) * COS(RADIANS(l.latitude)) *
                           POWER(SIN(RADIANS(l.longitude - :lon) / 2), 2)
                       )) AS "distanceKm"
                FROM location l
                WHERE l.latitude BETWEEN :minLat AND :maxLat
                  AND l.longitude BETWEEN :minLon AND :maxLon
            ) nearby
            WHERE nearby."distanceKm" <= :radiusKm
            ORDER BY nearby."distanceKm"
        """
    )
    fun findNearby(
        @Param("lat") latitude: Double,
        @Param("lon") longitude: Double,
        @Param("radiusKm") radiusKm: Double,
        @Param("minLat") minLatitude: Double,
        @Param("maxLat") maxLatitude: Double,
        @Param("minLon") minLongitude: Double,
        @Param("maxLon") maxLongitude: Double
    ): List<NearbyLocationProjection>
}

/** Row shape returned by [LocationRepository.findNearby]. */
interface NearbyLocationProjection {
    val personId: Long
    val distanceKm: Double
}
