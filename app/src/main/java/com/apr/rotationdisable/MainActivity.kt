package com.yb.usersmanager

import com.apr.rotationdisable.FullRotationService

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.concurrent.thread

private val Navy = Color(0xFF080D1D)
private val Blue = Color(0xFF477BFF)
private val Violet = Color(0xFF9B5CFF)
private val Mint = Color(0xFF50E3C2)

class MainActivity : ComponentActivity() {
    private var refreshState by mutableIntStateOf(0)
    private var rotationSuggestionHidden by mutableStateOf<Boolean?>(null)
    private var message by mutableStateOf("בדיקת הרשאת Root נדרשת להפעלת האפשרות")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 13, 29)
        window.navigationBarColor = android.graphics.Color.rgb(8, 13, 29)
        refreshRotationSuggestion()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Blue, secondary = Violet, background = Navy, surface = Color(0xFF151D33))) {
                RotationScreen(
                    refreshKey = refreshState,
                    hidden = rotationSuggestionHidden,
                    message = message,
                    onHideSuggestion = { setRotationSuggestion(true) },
                    onShowSuggestion = { setRotationSuggestion(false) },
                    onStartFullRotation = {
                        val serviceIntent = Intent(this, FullRotationService::class.java)
                            .putExtra(FullRotationService.EXTRA_PORTRAIT_ONLY, false)
                        startForegroundService(serviceIntent)
                        message = "מפעיל סיבוב מלא 360° דרך Root…"
                    },
                    onStartPortraitOnly = {
                        val serviceIntent = Intent(this, FullRotationService::class.java)
                            .putExtra(FullRotationService.EXTRA_PORTRAIT_ONLY, true)
                        startForegroundService(serviceIntent)
                        message = "מפעיל מצב אנכי בלבד: 0° ו־180°…"
                    },
                    onStopFullRotation = {
                        stopService(Intent(this, FullRotationService::class.java))
                        message = "הסיבוב המלא הופסק וההגדרה הרגילה הוחזרה"
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState++
        refreshRotationSuggestion()
    }

    private fun rootCommand(command: String): Pair<Int, String> {
        val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        return process.waitFor() to output.trim()
    }

    private fun refreshRotationSuggestion() {
        thread {
            try {
                val (code, output) = rootCommand("settings get secure show_rotation_suggestions")
                runOnUiThread {
                    rotationSuggestionHidden = code == 0 && output == "0"
                    if (code != 0) message = "לא התקבלה הרשאת Root. אשר את בקשת Magisk אם הופיעה."
                    else message = if (rotationSuggestionHidden == true) "כפתור הצעת הסיבוב מוסתר" else "כפתור הצעת הסיבוב מוצג"
                    refreshState++
                }
            } catch (_: Exception) {
                runOnUiThread {
                    rotationSuggestionHidden = null
                    message = "לא נמצא Root פעיל במכשיר"
                    refreshState++
                }
            }
        }
    }

    private fun setRotationSuggestion(hide: Boolean) {
        message = "מבצע שינוי דרך Root…"
        thread {
            try {
                val value = if (hide) 0 else 1
                val (code, output) = rootCommand("settings put secure show_rotation_suggestions $value && settings get secure show_rotation_suggestions")
                runOnUiThread {
                    if (code == 0 && output.endsWith(value.toString())) {
                        rotationSuggestionHidden = hide
                        message = if (hide) "הכפתור בפינה השמאלית התחתונה הוסתר" else "כפתור הצעת הסיבוב הוחזר"
                    } else {
                        message = "השינוי נכשל: $output"
                    }
                    refreshState++
                }
            } catch (e: Exception) {
                runOnUiThread {
                    message = "שגיאת Root: ${e.message ?: "לא ידועה"}"
                    refreshState++
                }
            }
        }
    }
}

@Composable
private fun RotationScreen(
    refreshKey: Int,
    hidden: Boolean?,
    message: String,
    onHideSuggestion: () -> Unit,
    onShowSuggestion: () -> Unit,
    onStartFullRotation: () -> Unit,
    onStartPortraitOnly: () -> Unit,
    onStopFullRotation: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF101A38), Navy, Color(0xFF17102D))))
            .systemBarsPadding().padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(76.dp).shadow(22.dp, CircleShape).background(Brush.linearGradient(listOf(Blue, Violet)), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ScreenRotation, null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("בקרת סיבוב מסך", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("ביטול כפתור הצעת הסיבוב של Android", fontSize = 13.sp, color = Color(0xFFADB9D6), textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Spacer(Modifier.height(14.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xCC192541))) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("כפתור הצעת הסיבוב בפינת המסך", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(when (hidden) { true -> "הכפתור מוסתר"; false -> "הכפתור מוצג"; null -> "מצב לא ידוע — נדרשת הרשאת Root" }, color = Mint, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(message, color = Color(0xFFADB9D6), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onHideSuggestion, modifier = Modifier.fillMaxWidth(), enabled = hidden != true, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("הסתרת הכפתור") }
                    OutlinedButton(onClick = onShowSuggestion, modifier = Modifier.fillMaxWidth(), enabled = hidden != false) { Text("החזרת הכפתור") }
                    Divider(color = Color(0x334477FF), modifier = Modifier.padding(vertical = 10.dp))
                    Text("סיבוב מלא 360°", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("כולל מצב הפוך של 180° — גם אם Android מסתיר אותו כברירת מחדל", color = Color(0xFFADB9D6), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onStartFullRotation, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Mint)) {
                        Text("הפעלת סיבוב מלא", color = Navy, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onStartPortraitOnly, modifier = Modifier.fillMaxWidth()) {
                        Text("מצב אנכי בלבד — 0° / 180°")
                    }
                    OutlinedButton(onClick = onStopFullRotation, modifier = Modifier.fillMaxWidth()) { Text("הפסקת סיבוב") }
                    Text("בחר סיבוב מלא לכל ארבעת הכיוונים, או מצב אנכי בלבד ללא שכיבה לצדדים. נדרשת הרשאת Root.", color = Color(0xFF8795B8), fontSize = 11.sp, textAlign = TextAlign.Center)
                    Text("הפעולה משנה את הגדרת Android דרך Root ואינה מכבה את הסיבוב האוטומטי.", color = Color(0xFF8795B8), fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, null, tint = Color(0xFF8795B8), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("yb apps", color = Color(0xFF8795B8), fontSize = 12.sp)
            }
        }
    }
}
