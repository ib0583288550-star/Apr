package com.apr.rotationdisable

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        val title = TextView(this).apply {
            text = "ביטול כפתור סיבוב מסך"
            textSize = 26f
            setPadding(0, 0, 0, 24)
        }

        val info = TextView(this).apply {
            text = "האפליקציה משנה את הגדרת Android שמפעילה את הצעת הסיבוב שמופיעה כאשר סיבוב אוטומטי כבוי.\n\nנדרשת הרשאת Root. אם Magisk מותקן, הוא אמור להציג בקשת הרשאה בפעם הראשונה."
            textSize = 16f
            setPadding(0, 0, 0, 32)
        }

        val disable = Button(this).apply {
            text = "בטל את כפתור הסיבוב"
            setOnClickListener { setRotationSuggestions(false) }
        }

        val enable = Button(this).apply {
            text = "הפעל את כפתור הסיבוב"
            setOnClickListener { setRotationSuggestions(true) }
        }

        val check = Button(this).apply {
            text = "בדוק מצב"
            setOnClickListener { checkRotationSuggestions() }
        }

        root.addView(title)
        root.addView(info)
        root.addView(disable)
        root.addView(enable)
        root.addView(check)
        setContentView(root)
    }

    private fun setRotationSuggestions(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        val command = "settings put secure show_rotation_suggestions $value"
        runAsRoot(command) { success, output ->
            if (success) {
                Toast.makeText(
                    this,
                    if (enabled) "כפתור הסיבוב הופעל" else "כפתור הסיבוב בוטל",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "לא ניתן לבצע את הפקודה. ודא ש-Magisk אישר Root.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun checkRotationSuggestions() {
        runAsRoot("settings get secure show_rotation_suggestions") { success, output ->
            val state = output.trim()
            val message = if (success) {
                when (state) {
                    "0" -> "כפתור הסיבוב: מבוטל"
                    "1" -> "כפתור הסיבוב: פעיל"
                    else -> "הערך הנוכחי: $state"
                }
            } else {
                "לא ניתן לקרוא את ההגדרה. נדרשת הרשאת Root."
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun runAsRoot(command: String, callback: (Boolean, String) -> Unit) {
        Thread {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
                val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
                val exitCode = process.waitFor()
                runOnUiThread {
                    callback(exitCode == 0, if (output.isNotBlank()) output else error)
                }
            } catch (e: Exception) {
                runOnUiThread { callback(false, e.message ?: "") }
            }
        }.start()
    }
}
