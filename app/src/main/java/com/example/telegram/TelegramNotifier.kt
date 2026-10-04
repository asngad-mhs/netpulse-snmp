package com.example.telegram

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Handles automated Telegram notifications for network threshold breaches,
 * link status changes, and critical SNMP device alerts.
 */
class TelegramNotifier(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    data class SendResult(
        val success: Boolean,
        val message: String,
        val statusCode: Int = 0
    )

    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        text: String,
        parseMode: String = "HTML"
    ): SendResult = withContext(Dispatchers.IO) {
        if (botToken.isBlank() || chatId.isBlank()) {
            return@withContext SendResult(
                success = false,
                message = "Token bot Telegram atau Chat ID belum diisi di Pengaturan."
            )
        }

        try {
            val url = "https://api.telegram.org/bot${botToken.trim()}/sendMessage"
            val formBody = FormBody.Builder()
                .add("chat_id", chatId.trim())
                .add("text", text)
                .add("parse_mode", parseMode)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    SendResult(
                        success = true,
                        message = "Pesan Telegram berhasil terkirim!",
                        statusCode = response.code
                    )
                } else {
                    SendResult(
                        success = false,
                        message = "Telegram API error (${response.code}): $bodyStr",
                        statusCode = response.code
                    )
                }
            }
        } catch (e: Exception) {
            SendResult(
                success = false,
                message = "Gagal menghubungi server Telegram: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Formats an alert for network incident reports
     */
    fun formatAlertMessage(
        severity: String,
        vendor: String,
        deviceName: String,
        host: String,
        alertTitle: String,
        metricDetail: String
    ): String {
        val icon = when (severity.uppercase()) {
            "CRITICAL" -> "🚨"
            "WARNING" -> "⚠️"
            else -> "ℹ️"
        }

        return """
            $icon <b>[NETPULSE SNMP ALERT]</b>
            ━━━━━━━━━━━━━━━━━━━
            <b>Device:</b> $deviceName
            <b>Vendor:</b> $vendor ($host)
            <b>Status:</b> <b>$severity</b>
            <b>Peringatan:</b> $alertTitle
            <b>Detail Metrik:</b> $metricDetail
            <b>Waktu:</b> ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
            ━━━━━━━━━━━━━━━━━━━
            <i>NetPulse SNMP Monitor System</i>
        """.trimIndent()
    }
}
