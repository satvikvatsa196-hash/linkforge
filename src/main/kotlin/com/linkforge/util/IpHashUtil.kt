package com.linkforge.util

import java.security.MessageDigest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class IpHashUtil(
    @Value("\${app.security.ip-salt:default-salt-value-for-dev}")
    private val salt: String
) {
    fun hashIp(ipAddress: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$ipAddress:$salt"
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
