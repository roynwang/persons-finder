package com.persons.finder.presentation

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.persons.finder.domain.services.PersonNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ApiExceptionHandler {

    private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    /**
     * Bean-validation failures. A single field can violate several constraints
     * at once, so messages are grouped per field rather than collapsed.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException): Map<String, List<String?>> {
        return ex.bindingResult.fieldErrors
            .groupBy({ it.field }, { it.defaultMessage })
    }

    /**
     * Malformed body or a value whose JSON type does not match the target type
     * (e.g. a string where a number is expected). Numeric fields are configured
     * to reject string input (see JacksonConfig), so those land here too.
     * Reported in the same field -> messages shape as validation errors.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleUnreadable(ex: HttpMessageNotReadableException): Map<String, List<String>> {
        val cause = ex.cause
        val field = (cause as? MismatchedInputException)?.let(::fieldPath)
        return if (field != null) {
            mapOf(field to listOf("invalid value for expected type"))
        } else {
            mapOf("error" to listOf("malformed request body"))
        }
    }

    /**
     * A path/query parameter whose value cannot be converted to the declared
     * type (e.g. a non-numeric person id). Same shape as validation errors.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): Map<String, List<String>> {
        return mapOf(ex.name to listOf("invalid value for expected type"))
    }

    @ExceptionHandler(PersonNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: PersonNotFoundException): Map<String, String?> {
        return mapOf("error" to ex.message)
    }

    /**
     * Database failures (connection loss, constraint violations, ...).
     * The transaction has already rolled back; respond in the standard error
     * shape without leaking driver/SQL details, and log the real cause.
     * Deliberately scoped to DataAccessException rather than Exception so
     * Spring's own defaults (405, 415, ...) stay intact.
     */
    @ExceptionHandler(DataAccessException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleDataAccess(ex: DataAccessException): Map<String, List<String>> {
        log.error("Database access failure", ex)
        return mapOf("error" to listOf("internal error"))
    }

    private fun fieldPath(ex: MismatchedInputException): String? {
        if (ex.path.isEmpty()) return null
        return ex.path.joinToString(".") { ref -> ref.fieldName ?: "[${ref.index}]" }
    }

}
