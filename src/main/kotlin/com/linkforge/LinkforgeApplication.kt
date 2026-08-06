package com.linkforge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LinkforgeApplication

fun main(args: Array<String>) {
    runApplication<LinkforgeApplication>(*args)
}
