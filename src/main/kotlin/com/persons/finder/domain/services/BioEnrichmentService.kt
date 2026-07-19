package com.persons.finder.domain.services

interface BioEnrichmentService {

    /**
     * Fire-and-forget: generates a bio for the person on a background thread and
     * writes it to the bio column. Never throws; on failure the bio stays null.
     * No-op when no LLM credentials are configured.
     */
    fun enrichAsync(personId: Long)
}
