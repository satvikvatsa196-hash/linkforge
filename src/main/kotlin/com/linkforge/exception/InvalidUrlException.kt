package com.linkforge.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidUrlException(message: String = "Invalid URL provided") : RuntimeException(message)
