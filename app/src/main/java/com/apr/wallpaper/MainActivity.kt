package com.apr.wallpaper

import android.os.Bundle
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class TerminalButton(val id: Long, val title: String, val command: String)

private val allowedCommands = setOf("echo","pwd","ls","date","whoami","id","uname","getprop")

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

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("כפתורי טרמינל") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAdd = true }) { Text("+") }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("צור כפתורים שמריצים פקודות Shell בטוחות במכשיר.",
                        style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("פקודות מותרות: ${allowedCommands.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (buttons.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Text("עדיין אין כפתורים. לחץ על + כדי להוסיף.",
                                Modifier.padding(18.dp))
                        }
                    }
                }
                items(buttons, key = { it.id }) { button ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    runningId = button.id
                                    Thread {
                                        val result = runSafeCommand(button.command)
                                        Handler(Looper.getMainLooper()).post {
                                            output = ">${button.command}\\n\\n${result}"
                                            runningId = null
                                        }
                                    }.start()
                                },
                                enabled = runningId == null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (runningId == button.id) "מריץ..." else button.title)
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = { save(buttons.filterNot { it.id == button.id }) },
                                enabled = runningId == null
                            ) { Text("מחק") }
                        }
                        Text(button.command,
                            Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (showAdd) {
            AddButtonDialog(
                onDismiss = { showAdd = false },
                onAdd = { title, command ->
                    save(buttons + TerminalButton(System.currentTimeMillis(), title, command))
                    showAdd = false
                }
            )
        }

        output?.let { text ->
            AlertDialog(
                onDismissRequest = { output = null },
                confirmButton = { TextButton(onClick = { output = null }) { Text("סגור") } },
                title = { Text("תוצאת הפקודה") },
                text = { Text(text, style = MaterialTheme.typography.bodyMedium) }
            )
        }
    }
}

@Composable
private fun AddButtonDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("כפתור חדש") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("שם הכפתור") }, singleLine = true)
                OutlinedTextField(value = command, onValueChange = { command = it },
                    label = { Text("פקודת טרמינל") },
                    placeholder = { Text("לדוגמה: echo שלום") }, singleLine = true)
                Text("כרגע אפשר להשתמש רק בפקודות בטוחות מהרשימה שבמסך הראשי.",
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
                    else -> onAdd(cleanTitle, cleanCommand)
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
        val process = ProcessBuilder("sh", "-c", command)
            .redirectErrorStream(true).start()
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
            put("id", it.id); put("title", it.title); put("command", it.command)
        })
    }
    context.getSharedPreferences("terminal_buttons", 0).edit()
        .putString("buttons", array.toString()).apply()
}

private fun loadButtons(context: Context): List<TerminalButton> {
    val raw = context.getSharedPreferences("terminal_buttons", 0)
        .getString("buttons", "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(TerminalButton(item.getLong("id"), item.getString("title"), item.getString("command")))
            }
        }
    } catch (_: Exception) { emptyList() }
}
