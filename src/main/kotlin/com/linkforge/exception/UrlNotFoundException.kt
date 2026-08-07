package com.linkforge.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class UrlNotFoundException(message: String = "URL not found") : RuntimeException(message)
