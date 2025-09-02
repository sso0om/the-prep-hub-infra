package com.back.global.app

import com.back.standard.util.Ut
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class AppConfig {

    companion object {
        private lateinit var environment: Environment
        private lateinit var objectMapper: ObjectMapper

        fun isDev(): Boolean {
            return environment.matchesProfiles("dev")
        }

        fun isTest(): Boolean {
            return environment.matchesProfiles("test")
        }

        fun isProd(): Boolean {
            return environment.matchesProfiles("prod")
        }

        fun isNotProd(): Boolean {
            return !isProd()
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Autowired
    fun setEnvironment(environment: Environment) {
        AppConfig.environment = environment
    }

    @Autowired
    fun setObjectMapper(objectMapper: ObjectMapper) {
        AppConfig.objectMapper = objectMapper
    }

    @PostConstruct
    fun postConstruct() {
        Ut.json.objectMapper = objectMapper
    }
}