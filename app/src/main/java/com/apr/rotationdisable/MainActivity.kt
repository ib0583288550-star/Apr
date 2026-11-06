package com.yb.usersmanager

import com.apr.rotationdisable.FullRotationService

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    private var sensitivityDegrees by mutableIntStateOf(15)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 13, 29)
        window.navigationBarColor = android.graphics.Color.rgb(8, 13, 29)
        refreshRotationSuggestion()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Blue, secondary = Violet, background = Navy, surface = Color(0xFF151D33))) {
                RotationScreen(
                    hidden = rotationSuggestionHidden,
                    message = message,
                    sensitivityDegrees = sensitivityDegrees,
                    onSensitivityChange = { sensitivityDegrees = it },
                    onHideSuggestion = { setRotationSuggestion(true) },
                    onShowSuggestion = { setRotationSuggestion(false) },
                    onStartFullRotation = {
                        val serviceIntent = Intent(this, FullRotationService::class.java)
                            .putExtra(FullRotationService.EXTRA_PORTRAIT_ONLY, false)
                            .putExtra(FullRotationService.EXTRA_SENSITIVITY, sensitivityDegrees)
                        startForegroundService(serviceIntent)
                        message = "מפעיל סיבוב מלא 360° דרך Root…"
                    },
                    onStartPortraitOnly = {
                        val serviceIntent = Intent(this, FullRotationService::class.java)
                            .putExtra(FullRotationService.EXTRA_PORTRAIT_ONLY, true)
                            .putExtra(FullRotationService.EXTRA_SENSITIVITY, sensitivityDegrees)
                        startForegroundService(serviceIntent)
                        message = "מפעיל מצב אנכי בלבד: 0° ו־180°…"
                    },
                    onStopFullRotation = {
                        stopService(Intent(this, FullRotationService::class.java))
                        message = "הסיבוב הופסק — המסך נשאר נעול בכיוון הנוכחי"
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
    hidden: Boolean?,
    message: String,
    sensitivityDegrees: Int,
    onSensitivityChange: (Int) -> Unit,
    onHideSuggestion: () -> Unit,
    onShowSuggestion: () -> Unit,
    onStartFullRotation: () -> Unit,
    onStartPortraitOnly: () -> Unit,
    onStopFullRotation: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF101A38), Navy, Color(0xFF17102D))))
            .systemBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.size(66.dp).shadow(22.dp, CircleShape).background(Brush.linearGradient(listOf(Blue, Violet)), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ScreenRotation, null, tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text("בקרת סיבוב מסך", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("שליטה בכפתור ההצעה ובסיבוב האמיתי", fontSize = 12.sp, color = Color(0xFFADB9D6), textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xCC192541), contentColor = Mint) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("כפתורי הצעה") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("סיבוב אמיתי") })
            }
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xCC192541))) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedTab == 0) {
                        Text("כפתור הצעת הסיבוב בפינת המסך", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(when (hidden) { true -> "הכפתור מוסתר"; false -> "הכפתור מוצג"; null -> "מצב לא ידוע — נדרשת הרשאת Root" }, color = Mint, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(message, color = Color(0xFFADB9D6), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = onHideSuggestion, modifier = Modifier.fillMaxWidth(), enabled = hidden != true, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("הסתרת הכפתור") }
                        OutlinedButton(onClick = onShowSuggestion, modifier = Modifier.fillMaxWidth(), enabled = hidden != false) { Text("החזרת הכפתור") }
                    } else {
                        Text("סיבוב מסך אמיתי", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("הפעולות האלו נמצאות כאן בנפרד מכפתור ההצעה של Android", color = Color(0xFFADB9D6), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(14.dp))
                        Text("רגישות חיישן הסיבוב", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("ערך גבוה דורש הטיה חזקה ואיטית יותר לפני החלפת כיוון", color = Color(0xFFADB9D6), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("${sensitivityDegrees}° — ${when { sensitivityDegrees < 10 -> "רגיש"; sensitivityDegrees < 20 -> "בינוני"; else -> "דורש הטיה חזקה" }}", color = Mint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Slider(value = sensitivityDegrees.toFloat(), onValueChange = { onSensitivityChange(it.toInt()) }, valueRange = 0f..30f, steps = 5)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("רגיש", color = Color(0xFF8795B8), fontSize = 11.sp)
                            Text("דורש הטיה חזקה", color = Color(0xFF8795B8), fontSize = 11.sp)
                        }
                        Divider(color = Color(0x334477FF), modifier = Modifier.padding(vertical = 12.dp))
                        Button(onClick = onStartFullRotation, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Mint)) {
                            Text("הפעלת סיבוב מלא 360°", color = Navy, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = onStartPortraitOnly, modifier = Modifier.fillMaxWidth()) { Text("מצב אנכי בלבד — 0° / 180°") }
                        OutlinedButton(onClick = onStopFullRotation, modifier = Modifier.fillMaxWidth()) { Text("הפסקת סיבוב ונעילת הכיוון הנוכחי") }
                        Text("לאחר ההפסקה המסך יישאר בכיוון שבו הוא נמצא. הסיבוב האוטומטי לא יופעל מחדש.", color = Color(0xFF8795B8), fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, null, tint = Color(0xFF8795B8), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("yb apps", color = Color(0xFF8795B8), fontSize = 12.sp)
            }
        }
    }
}
