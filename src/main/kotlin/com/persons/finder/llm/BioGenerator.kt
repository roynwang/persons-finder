package com.persons.finder.llm

import com.persons.finder.data.Person

/**
 * Pure LLM call: turns a person's profile fields into a short bio. Implementations
 * must be free of persistence and web concerns so they can be exercised standalone
 * (unit tests with a mocked HTTP layer, eval suite against the real provider).
 */
interface BioGenerator {

    /** False when no provider credentials are configured; callers skip generation. */
    val enabled: Boolean

    /** Returns a short bio for the person. Throws on provider failure. */
    fun generate(person: Person): String
}
