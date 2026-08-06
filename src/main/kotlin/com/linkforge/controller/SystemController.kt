package com.linkforge.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/system")
@Tag(name = "System", description = "System level operations")
class SystemController {

    @GetMapping("/ping")
    @Operation(summary = "Health check ping", description = "Returns a simple acknowledgment to verify API is up")
    fun ping(): Map<String, String> {
        return mapOf("status" to "UP", "message" to "Linkforge API is running")
    }
}
