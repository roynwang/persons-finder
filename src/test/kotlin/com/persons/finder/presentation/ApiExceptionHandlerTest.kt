package com.persons.finder.presentation

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.persons.finder.domain.services.PersonNotFoundException
import com.persons.finder.presentation.dto.CreatePersonRequest
import com.persons.finder.presentation.dto.LocationDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.http.HttpStatus
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ResponseStatus

class ApiExceptionHandlerTest {

    private val handler = ApiExceptionHandler()

    @Test
    fun `validation errors are grouped per field so none are dropped`() {
        val binding = BeanPropertyBindingResult(Any(), "request")
        binding.addError(FieldError("request", "name", "must not be blank"))
        binding.addError(FieldError("request", "name", "size must be between 0 and 128"))
        binding.addError(FieldError("request", "location.latitude", "must be less than or equal to 90.0"))

        val result = handler.handleValidation(MethodArgumentNotValidException(anyMethodParameter(), binding))

        assertEquals(listOf("must not be blank", "size must be between 0 and 128"), result["name"])
        assertEquals(listOf("must be less than or equal to 90.0"), result["location.latitude"])
    }

    @Test
    fun `type mismatch reports the full field path`() {
        val cause = MismatchedInputException.from(null as JsonParser?, Double::class.java, "not a number")
        cause.prependPath(LocationDto::class.java, "latitude")
        cause.prependPath(CreatePersonRequest::class.java, "location")
        val ex = HttpMessageNotReadableException("bad", cause, MockHttpInputMessage(ByteArray(0)))

        val result = handler.handleUnreadable(ex)

        assertEquals(mapOf("location.latitude" to listOf("invalid value for expected type")), result)
    }

    @Test
    fun `unparseable body falls back to a generic error`() {
        val ex = HttpMessageNotReadableException("garbage", MockHttpInputMessage(ByteArray(0)))

        val result = handler.handleUnreadable(ex)

        assertEquals(mapOf("error" to listOf("malformed request body")), result)
    }

    @Test
    fun `path parameter type mismatch reports the parameter name`() {
        val ex = org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
            "abc", Long::class.java, "id", anyMethodParameter(), NumberFormatException("abc")
        )

        val result = handler.handleTypeMismatch(ex)

        assertEquals(mapOf("id" to listOf("invalid value for expected type")), result)
    }

    @Test
    fun `person not found reports the id`() {
        val result = handler.handleNotFound(PersonNotFoundException(7))

        assertEquals(mapOf("error" to "Person 7 not found"), result)
    }

    @Test
    fun `database failure maps to 500 with a generic message`() {
        val result = handler.handleDataAccess(DataAccessResourceFailureException("connection refused"))

        assertEquals(mapOf("error" to listOf("internal error")), result)
        val status = ApiExceptionHandler::class.java
            .getMethod("handleDataAccess", DataAccessException::class.java)
            .getAnnotation(ResponseStatus::class.java).value
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status)
    }

    private fun anyMethodParameter(): MethodParameter {
        val method = ApiExceptionHandler::class.java
            .getMethod("handleValidation", MethodArgumentNotValidException::class.java)
        return MethodParameter(method, 0)
    }

}
