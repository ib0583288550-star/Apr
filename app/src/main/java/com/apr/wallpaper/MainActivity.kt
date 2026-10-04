package com.apr.terminalbuttons

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

data class TerminalButton(val id: Long, val title: String, val command: String, val icon: String)

private val allowedCommands = setOf("echo","pwd","ls","date","whoami","id","uname","getprop")
private val iconOptions = listOf("⌘","▶","⚡","★","✓","⌁","▣","●","◆","☰")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TerminalButtonsApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalButtonsApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var buttons by remember { mutableStateOf(loadButtons(context)) }
    var showAdd by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf<String?>(null) }
    var runningId by remember { mutableStateOf<Long?>(null) }

    fun save(list: List<TerminalButton>) {
        buttons = list
        saveButtons(context, list)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7C9CFF),
            secondary = Color(0xFF61D7C9),
            surface = Color(0xFF151922),
            background = Color(0xFF0B0E14)
        )
    ) {
        Scaffold(
            containerColor = Color(0xFF0B0E14),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Terminal Buttons", fontWeight = FontWeight.Bold)
                            Text("כפתורים חכמים לפקודות", fontSize = 12.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0E14))
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Text("+", fontSize = 25.sp) },
                    text = { Text("כפתור חדש") },
                    containerColor = Color(0xFF7C9CFF),
                    contentColor = Color(0xFF08101F)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF171D29))
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("הפקודות שלך", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text("הוסף כפתור, בחר לו אייקון והגדר מה הוא יריץ.", color = Color(0xFFB8C0D0))
                            Spacer(Modifier.height(12.dp))
                            Text("${buttons.size} כפתורים שמורים", color = Color(0xFF61D7C9), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (buttons.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF151922))) {
                            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⌘", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("עדיין אין כפתורים", fontWeight = FontWeight.Bold)
                                Text("לחץ על \"כפתור חדש\" כדי להתחיל.", color = Color(0xFFB8C0D0))
                            }
                        }
                    }
                }
                items(buttons, key = { it.id }) { button ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151922))) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF252D40), modifier = Modifier.size(56.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(button.icon, fontSize = 27.sp, color = Color(0xFF7C9CFF))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(button.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(button.command, color = Color(0xFF9DA7BA), maxLines = 1)
                                }
                                Button(
                                    onClick = {
                                        runningId = button.id
                                        Thread {
                                            val result = runSafeCommand(button.command)
                                            Handler(Looper.getMainLooper()).post {
                                                output = "> ${button.command}\\n\\n${result}"
                                                runningId = null
                                            }
                                        }.start()
                                    },
                                    enabled = runningId == null,
                                    shape = RoundedCornerShape(14.dp)
                                ) { Text(if (runningId == button.id) "..." else "הרץ") }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { save(buttons.filterNot { it.id == button.id }) }, enabled = runningId == null) {
                                    Text("מחק", color = Color(0xFFFF8A8A))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAdd) {
            AddButtonDialog(
                onDismiss = { showAdd = false },
                onAdd = { title, command, icon ->
                    save(buttons + TerminalButton(System.currentTimeMillis(), title, command, icon))
                    showAdd = false
                }
            )
        }

        output?.let { resultText ->
            AlertDialog(
                onDismissRequest = { output = null },
                confirmButton = { TextButton(onClick = { output = null }) { Text("סגור") } },
                title = { Text("תוצאת הפקודה") },
                text = { Text(resultText, style = MaterialTheme.typography.bodyMedium) }
            )
        }
    }
}

@Composable
private fun AddButtonDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(iconOptions.first()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("כפתור חדש") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("שם הכפתור") }, singleLine = true)
                Text("בחר אייקון", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    iconOptions.take(5).forEach { icon ->
                        FilterChip(selected = selectedIcon == icon, onClick = { selectedIcon = icon },
                            label = { Text(icon, fontSize = 18.sp) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    iconOptions.drop(5).forEach { icon ->
                        FilterChip(selected = selectedIcon == icon, onClick = { selectedIcon = icon },
                            label = { Text(icon, fontSize = 18.sp) })
                    }
                }
                OutlinedTextField(value = command, onValueChange = { command = it },
                    label = { Text("פקודת טרמינל") },
                    placeholder = { Text("לדוגמה: echo שלום") }, singleLine = true)
                Text("פקודות זמינות: ${allowedCommands.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cleanTitle = title.trim()
                val cleanCommand = command.trim()
                val first = cleanCommand.split(Regex("\\s+")).firstOrNull().orEmpty()
                when {
                    cleanTitle.isEmpty() -> error = "צריך לתת שם לכפתור."
                    cleanCommand.isEmpty() -> error = "צריך להכניס פקודה."
                    first !in allowedCommands -> error = "הפקודה הזו לא מאושרת באפליקציה."
                    else -> onAdd(cleanTitle, cleanCommand, selectedIcon)
                }
            }) { Text("הוסף") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}

private fun runSafeCommand(command: String): String {
    val first = command.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
    if (first !in allowedCommands) return "הפקודה לא מאושרת."
    return try {
        val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()
        "קוד יציאה: ${exit}\\n${text}".trim()
    } catch (e: Exception) {
        "שגיאה: ${e.message ?: "לא ידוע"}"
    }
}

private fun saveButtons(context: Context, buttons: List<TerminalButton>) {
    val array = JSONArray()
    buttons.forEach {
        array.put(JSONObject().apply {
            put("id", it.id)
            put("title", it.title)
            put("command", it.command)
            put("icon", it.icon)
        })
    }
    context.getSharedPreferences("terminal_buttons", 0).edit().putString("buttons", array.toString()).apply()
}

private fun loadButtons(context: Context): List<TerminalButton> {
    val raw = context.getSharedPreferences("terminal_buttons", 0).getString("buttons", "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(TerminalButton(item.getLong("id"), item.getString("title"), item.getString("command"), item.optString("icon", "⌘")))
            }
        }
    } catch (_: Exception) { emptyList() }
}
