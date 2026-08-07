package com.linkforge.util

object Base62Encoder {
    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private val BASE = ALPHABET.length.toLong()

    fun encode(num: Long): String {
        var n = num
        if (n == 0L) return ALPHABET[0].toString()
        val sb = StringBuilder()
        while (n > 0) {
            sb.append(ALPHABET[(n % BASE).toInt()])
            n /= BASE
        }
        return sb.reverse().toString()
    }

    fun decode(str: String): Long {
        var num = 0L
        for (char in str) {
            num = num * BASE + ALPHABET.indexOf(char)
        }
        return num
    }
}
