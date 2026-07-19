package com.persons.finder.config

import com.fasterxml.jackson.databind.cfg.CoercionAction
import com.fasterxml.jackson.databind.cfg.CoercionInputShape
import com.fasterxml.jackson.databind.type.LogicalType
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Enforce the published contract: fields declared as JSON numbers (e.g. a
 * location's latitude/longitude) reject string input rather than silently
 * coercing "45.0" into 45.0. The rejection surfaces as a 400 via
 * [com.persons.finder.presentation.ApiExceptionHandler].
 */
@Configuration
class JacksonConfig {

    @Bean
    fun strictNumberCoercion(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.postConfigurer { mapper ->
                listOf(LogicalType.Float, LogicalType.Integer).forEach { type ->
                    mapper.coercionConfigFor(type)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                }
            }
        }
    }

}
