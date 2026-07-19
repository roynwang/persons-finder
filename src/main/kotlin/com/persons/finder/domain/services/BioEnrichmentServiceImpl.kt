package com.persons.finder.domain.services

import com.persons.finder.domain.repositories.PersonRepository
import com.persons.finder.llm.BioGenerator
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

private const val BIO_MAX_LENGTH = 1024

@Service
class BioEnrichmentServiceImpl(
    private val personRepository: PersonRepository,
    private val bioGenerator: BioGenerator
) : BioEnrichmentService {

    private val log = LoggerFactory.getLogger(BioEnrichmentServiceImpl::class.java)

    // Deliberately not @Transactional: the LLM call must not hold a transaction
    // open, and updateBio carries its own.
    @Async("bioTaskExecutor")
    override fun enrichAsync(personId: Long) {
        if (!bioGenerator.enabled) return
        // Deleted before the worker ran: benign no-op.
        val person = personRepository.findByIdOrNull(personId) ?: return
        try {
            val bio = bioGenerator.generate(person).take(BIO_MAX_LENGTH)
            personRepository.updateBio(personId, bio)
        } catch (e: Exception) {
            log.error("Bio generation failed for person {}; bio left null", personId, e)
        }
    }
}
