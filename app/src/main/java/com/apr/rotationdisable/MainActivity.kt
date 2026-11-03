package com.yb.usersmanager

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
import androidx.compose.material.icons.filled.CheckCircle
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

private val Navy = Color(0xFF080D1D)
private val Blue = Color(0xFF477BFF)
private val Violet = Color(0xFF9B5CFF)
private val Mint = Color(0xFF50E3C2)

class MainActivity : ComponentActivity() {
    private var refreshState by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 13, 29)
        window.navigationBarColor = android.graphics.Color.rgb(8, 13, 29)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Blue,
                    secondary = Violet,
                    background = Navy,
                    surface = Color(0xFF151D33)
                )
            ) {
                RotationScreen(
                    refreshKey = refreshState,
                    onEnable = { setAutoRotate(true) },
                    onDisable = { setAutoRotate(false) },
                    onGrant = { openWriteSettings() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState++
    }

    private fun openWriteSettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
        )
    }

    private fun setAutoRotate(enabled: Boolean) {
        if (!Settings.System.canWrite(this)) {
            openWriteSettings()
            return
        }
        Settings.System.putInt(
            contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            if (enabled) 1 else 0
        )
        refreshState++
    }
}

@Composable
private fun RotationScreen(
    refreshKey: Int,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onGrant: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val allowed = remember(refreshKey) { Settings.System.canWrite(context) }
    val enabled = remember(refreshKey, allowed) {
        if (allowed) Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            0
        ) == 1 else false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF101A38), Navy, Color(0xFF17102D))
                )
            )
            .systemBarsPadding()
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .shadow(22.dp, CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Blue, Violet)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ScreenRotation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "בקרת סיבוב מסך",
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "שליטה מהירה ונוחה בסיבוב האוטומטי",
                fontSize = 14.sp,
                color = Color(0xFFADB9D6),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(30.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC192541))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("מצב נוכחי", color = Color(0xFF9EACCC), fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(10.dp).background(
                                if (enabled) Mint else Color(0xFFFFB45C),
                                CircleShape
                            )
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            if (enabled) "סיבוב אוטומטי פעיל" else "סיבוב אוטומטי כבוי",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (!allowed) "נדרשת הרשאה חד־פעמית לשינוי הגדרת המערכת"
                        else if (enabled) "המסך יסתובב בהתאם לכיוון המכשיר"
                        else "כיוון המסך יישאר קבוע",
                        color = Color(0xFFADB9D6),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = if (allowed) onEnable else onGrant,
                modifier = Modifier.fillMaxWidth().height(64.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Blue,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.ScreenRotation, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (allowed) "הפעלת סיבוב אוטומטי" else "מתן הרשאה והמשך",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(13.dp))
            OutlinedButton(
                onClick = if (allowed) onDisable else onGrant,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Violet)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Mint)
                Spacer(Modifier.width(10.dp))
                Text("ביטול סיבוב אוטומטי", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = Color(0xFF8795B8), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("yb apps", color = Color(0xFF8795B8), fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
