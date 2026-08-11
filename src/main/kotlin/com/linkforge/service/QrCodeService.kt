package com.linkforge.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

import org.springframework.data.redis.core.StringRedisTemplate
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.time.Duration
import org.slf4j.LoggerFactory

@Service
class QrCodeService(
    @Value("\${app.qr.width:250}") private val width: Int,
    @Value("\${app.qr.height:250}") private val height: Int,
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(QrCodeService::class.java)

    fun generateQrCode(text: String): ByteArray {
        val cacheKey = "qr:$text"
        
        try {
            val cached = redisTemplate.opsForValue().get(cacheKey)
            if (cached != null) {
                log.info("Cache hit for QR code: {}", text)
                return Base64.getDecoder().decode(cached)
            }
        } catch (e: Exception) {
            log.warn("Redis cache read failed for QR code {}: {}", text, e.message)
        }

        log.info("Generating QR code for: {}", text)
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height)
        val outputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
        val bytes = outputStream.toByteArray()

        CompletableFuture.runAsync {
            try {
                // Cache for 7 days
                redisTemplate.opsForValue().set(cacheKey, Base64.getEncoder().encodeToString(bytes), Duration.ofDays(7))
            } catch (e: Exception) {
                log.warn("Redis cache write failed for QR code {}: {}", text, e.message)
            }
        }
        
        return bytes
    }
}
