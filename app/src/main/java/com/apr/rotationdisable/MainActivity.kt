package com.apr.rotationdisable

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private val bg = Color.rgb(246, 248, 252)
    private val primary = Color.rgb(55, 105, 190)
    private val textColor = Color.rgb(35, 45, 60)
    private val secondary = Color.rgb(105, 115, 130)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 56, 40, 40)
            setBackgroundColor(bg)
        }

        val title = TextView(this).apply {
            text = "ביטול כפתור סיבוב מסך"
            textSize = 27f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }

        val subtitle = TextView(this).apply {
            text = "שליטה בכפתור הצעת הסיבוב של Android"
            textSize = 15f
            setTextColor(secondary)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 28)
        }

        val info = TextView(this).apply {
            text = "האפליקציה מאפשרת להפעיל או לבטל את כפתור הסיבוב שמופיע כאשר סיבוב אוטומטי כבוי.\n\nנדרשת הרשאת Root. אם Magisk מותקן, הוא אמור להציג בקשת הרשאה בפעם הראשונה."
            textSize = 15f
            setTextColor(textColor)
            setPadding(0, 0, 0, 28)
        }

        val disable = makeButton("בטל את כפתור הסיבוב") {
            setRotationSuggestions(false)
        }

        val enable = makeButton("הפעל את כפתור הסיבוב") {
            setRotationSuggestions(true)
        }

        val check = makeButton("בדוק מצב") {
            checkRotationSuggestions()
        }

        val about = makeButton("אודות") {
            android.app.AlertDialog.Builder(this)
                .setTitle("אודות")
                .setMessage("ביטול כפתור סיבוב מסך\n\nאפליקציה פשוטה לשליטה בהצעת הסיבוב של Android.\n\nקרדיט: y.b apps")
                .setPositiveButton("סגור", null)
                .show()
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(info)
        root.addView(disable)
        root.addView(enable)
        root.addView(check)
        root.addView(about)

        setContentView(root)
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(primary)
            setPadding(20, 14, 20, 14)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 14)
            layoutParams = params
            setOnClickListener { action() }
        }
    }

    private fun setRotationSuggestions(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        val command = "settings put secure show_rotation_suggestions $value"
        runAsRoot(command) { success, _ ->
            android.widget.Toast.makeText(
                this,
                if (success) {
                    if (enabled) "כפתור הסיבוב הופעל" else "כפתור הסיבוב בוטל"
                } else {
                    "לא ניתן לבצע את הפקודה. ודא ש-Magisk אישר Root."
                },
                android.widget.Toast.LENGTH_LONG
            ).show()
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
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun runAsRoot(command: String, callback: (Boolean, String) -> Unit) {
        Thread {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                val output = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream)).use { it.readText() }
                val error = java.io.BufferedReader(java.io.InputStreamReader(process.errorStream)).use { it.readText() }
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
