package com.persons.finder.domain.repositories

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.repository.Query

/**
 * Change-detector for the hand-written native SQL. It does NOT execute the
 * query (no DB in unit tests) — it pins the query text against a checked-in
 * copy so any edit fails here and forces a conscious re-verification against
 * PostgreSQL. Execution correctness (dialect, projection mapping, distances,
 * ordering) is owned by the nearby e2e scenario.
 */
class LocationRepositoryTest {

    @Test
    fun `findNearby native query matches the reviewed SQL`() {
        val actual = LocationRepository::class.java.methods
            .single { it.name == "findNearby" }
            .getAnnotation(Query::class.java)!!
            .value

        val expected = """
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

        // Compare on tokens, not layout: reindentation is not a query change,
        // but any altered column, function, alias, or clause is.
        assertEquals(expected.normalizeSql(), actual.normalizeSql())
    }

    private fun String.normalizeSql() = trim().replace(Regex("\\s+"), " ")

}
