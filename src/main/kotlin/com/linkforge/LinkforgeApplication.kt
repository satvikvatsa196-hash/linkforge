package com.linkforge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class LinkforgeApplication

fun main(args: Array<String>) {
    runApplication<LinkforgeApplication>(*args)
}
