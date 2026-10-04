package com.apr.terminalbuttons

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        val title = TextView(this).apply {
            text = "בקרת סיבוב מסך"
            textSize = 26f
            setPadding(0, 0, 0, 32)
        }

        val disable = Button(this).apply {
            text = "ביטול סיבוב אוטומטי"
            setOnClickListener { setAutoRotate(false) }
        }

        val enable = Button(this).apply {
            text = "הפעלת סיבוב אוטומטי"
            setOnClickListener { setAutoRotate(true) }
        }

        root.addView(title)
        root.addView(disable)
        root.addView(enable)
        setContentView(root)
    }

    private fun setAutoRotate(enabled: Boolean) {
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "יש לאשר לאפליקציה שינוי הגדרות מערכת", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        Settings.System.putInt(
            contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            if (enabled) 1 else 0
        )

        Toast.makeText(
            this,
            if (enabled) "סיבוב אוטומטי הופעל" else "סיבוב אוטומטי בוטל",
            Toast.LENGTH_SHORT
        ).show()
    }
}
