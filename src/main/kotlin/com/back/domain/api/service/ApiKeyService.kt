package com.back.domain.api.service

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ApiKeyService {
    fun generateApiKey(): String = "api_" + UUID.randomUUID().toString().replace("-", "")

}