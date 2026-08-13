package com.linkforge.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory

@Service
class QrCodeService(
    @Value("\${app.qr.width:250}") private val width: Int,
    @Value("\${app.qr.height:250}") private val height: Int
) {
    private val log = LoggerFactory.getLogger(QrCodeService::class.java)

    fun generateQrCode(text: String): ByteArray {
        log.info("Generating QR code for: {}", text)
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height)
        val outputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
        return outputStream.toByteArray()
    }
}
