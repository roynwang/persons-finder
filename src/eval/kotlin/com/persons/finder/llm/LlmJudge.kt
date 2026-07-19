package com.persons.finder.llm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate

/**
 * Shared LLM-as-judge helper for the eval suite: asks Gemini whether some text
 * satisfies a list of natural-language criteria, and fails the test — with the
 * judge's own per-criterion reasons — for any it rejects. Verdicts are pinned to
 * JSON so they parse deterministically even though the wording doesn't.
 *
 * Use for semantic checks a rule-based `contains`/regex assertion can't make
 * (voice, tone, "did it actually mention X"). Calls the REAL Gemini API
 * (billable); build with [fromEnv] and run under `make eval`.
 */
class LlmJudge(
    private val restTemplate: RestTemplate,
    private val apiKey: String,
    private val model: String,
    private val baseUrl: String
) {

    fun assertVerifies(actual: String, criteria: List<String>) {
        val numbered = criteria.mapIndexed { i, c -> "${i + 1}. $c" }.joinToString("\n")
        val prompt = buildString {
            appendLine("You are a strict evaluator. For each CRITERION, decide whether the TEXT satisfies it.")
            appendLine("Judge only what the TEXT actually says; do not assume unstated facts. Be lenient about")
            appendLine("wording and synonyms, strict about substance.")
            appendLine("""Respond as JSON: {"results":[{"criterion":"<verbatim>","verdict":"PASS"|"FAIL","reason":"<short>"}]}""")
            appendLine()
            appendLine("TEXT:")
            appendLine("\"\"\"")
            appendLine(actual)
            appendLine("\"\"\"")
            appendLine()
            appendLine("CRITERIA:")
            append(numbered)
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("x-goog-api-key", apiKey)
        }
        val request = JudgeRequest(
            contents = listOf(JudgeContent(listOf(JudgePart(prompt)))),
            generationConfig = JudgeGenerationConfig(responseMimeType = "application/json")
        )
        val response = restTemplate.postForObject(
            "$baseUrl/v1beta/models/$model:generateContent",
            HttpEntity(request, headers),
            JudgeResponse::class.java
        )
        val json = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: error("judge returned no text")

        val verdicts = jacksonObjectMapper().readValue<JudgeVerdicts>(json).results
        println("[judge] ${verdicts.joinToString("\n        ") { "${it.verdict}: ${it.criterion} — ${it.reason}" }}")

        assertTrue(
            verdicts.size >= criteria.size,
            "judge returned ${verdicts.size} verdicts for ${criteria.size} criteria: $json"
        )
        val failures = verdicts.filter { !it.verdict.equals("PASS", ignoreCase = true) }
        assertTrue(
            failures.isEmpty(),
            "LLM judge rejected:\n" + failures.joinToString("\n") { "- ${it.criterion}: ${it.reason}" }
        )
    }

    companion object {
        /** Builds a judge from the same GEMINI_* env vars the eval suite uses. */
        fun fromEnv(): LlmJudge = LlmJudge(
            restTemplate = RestTemplate(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(5_000)
                    setReadTimeout(60_000)
                }
            ).apply {
                messageConverters = listOf(MappingJackson2HttpMessageConverter(jacksonObjectMapper()))
            },
            apiKey = System.getenv("GEMINI_API_KEY") ?: "",
            model = System.getenv("GEMINI_MODEL") ?: "gemini-3.1-flash-lite",
            baseUrl = System.getenv("GEMINI_BASE_URL") ?: "https://generativelanguage.googleapis.com"
        )
    }
}

// Judge wire shapes (Gemini generateContent).
private data class JudgeRequest(
    val contents: List<JudgeContent>,
    val generationConfig: JudgeGenerationConfig
)

private data class JudgeContent(val parts: List<JudgePart>)
private data class JudgePart(val text: String)
private data class JudgeGenerationConfig(val responseMimeType: String)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class JudgeResponse(val candidates: List<JudgeCandidate> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
private data class JudgeCandidate(val content: JudgeContentOut? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class JudgeContentOut(val parts: List<JudgePartOut> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
private data class JudgePartOut(val text: String? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class JudgeVerdicts(val results: List<JudgeVerdict> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
private data class JudgeVerdict(
    val criterion: String = "",
    val verdict: String = "",
    val reason: String = ""
)
