package com.persons.finder.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class GeminiConfig {

    // Timeouts keep a hung Gemini call from pinning bio executor threads.
    @Bean
    fun geminiRestTemplate(builder: RestTemplateBuilder): RestTemplate =
        builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(30))
            .build()
}
