package com.yb.usersmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.InputStreamReader

data class AndroidUser(val id: Int, val name: String)

class MainActivity : ComponentActivity() {
    private val manager = RootUserManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkMode by remember { mutableStateOf(false) }
            MaterialTheme(colorScheme = if (darkMode) darkColorScheme(primary = Color(0xFF9FA8DA)) else lightColorScheme(primary = Color(0xFF3F51B5))) {
                UserSettingsScreen(manager, darkMode, { darkMode = it })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserSettingsScreen(manager: RootUserManager, darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    var users by remember { mutableStateOf(manager.listUsers()) }
    var currentId by remember { mutableStateOf(manager.currentUser()) }
    var maxUsers by remember { mutableStateOf(manager.maxUsers()) }
    var rootOk by remember { mutableStateOf(manager.hasRoot()) }
    var runningAsOwner by remember { mutableStateOf(manager.currentUserNonRoot() == 0) }
    var message by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var creatingUser by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var deleteUser by remember { mutableStateOf<AndroidUser?>(null) }
    val canCreateUser = runningAsOwner && rootOk && (maxUsers == null || users.size < maxUsers!!)

    fun refresh() {
        rootOk = manager.hasRoot()
        runningAsOwner = manager.currentUserNonRoot() == 0
        users = manager.listUsers()
        currentId = manager.currentUser()
        maxUsers = manager.maxUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("משתמשים") },
                actions = {
                    IconButton(onClick = { onDarkModeChange(!darkMode) }) {
                        Icon(Icons.Default.DarkMode, contentDescription = "מצב כהה")
                    }
                    IconButton(onClick = { refresh(); message = "הרשימה עודכנה" }) {
                        Icon(Icons.Default.Refresh, contentDescription = "רענן")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (canCreateUser) { newName = ""; showAdd = true }
                else message = when {
                    !runningAsOwner -> "כדי ליצור משתמש חדש, פתח את האפליקציה במשתמש הראשי (Owner)."
                    !rootOk -> "אין הרשאת Root. אשר את בקשת Root לאפליקציה ונסה שוב."
                    else -> "הגעת למספר המשתמשים המרבי שהמכשיר מאפשר."
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "יצירת משתמש חדש")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text("משתמשים", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "בחר משתמש כדי לעבור אליו. לכל משתמש יש מרחב אישי משלו במכשיר.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (!runningAsOwner) "ניהול מלא זמין רק במשתמש הראשי"
                        else if (maxUsers != null) "עד ${maxUsers} משתמשים נתמכים במכשיר"
                        else "מספר המשתמשים המרבי לא ידוע",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (canCreateUser) { newName = ""; showAdd = true }
                            else message = when {
                                !runningAsOwner -> "כדי ליצור משתמש חדש, פתח את האפליקציה במשתמש הראשי (Owner)."
                                !rootOk -> "אין הרשאת Root. אשר את בקשת Root לאפליקציה ונסה שוב."
                                else -> "הגעת למספר המשתמשים המרבי שהמכשיר מאפשר."
                            }
                        },
                        enabled = canCreateUser,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("יצירת משתמש חדש")
                    }
                }
            }

            items(users, key = { it.id }) { user ->
                UserSettingsRow(
                    user = user,
                    selected = user.id == currentId,
                    onClick = {
                        if (!rootOk) {
                            message = "Root לא מורשה למשתמש הנוכחי. פתח את האפליקציה במשתמש הראשי."
                        } else if (user.id == currentId) {
                            message = "זה המשתמש הפעיל כרגע"
                        } else {
                            message = manager.switchUser(user.id)
                            refresh()
                        }
                    },
                    onDelete = if (user.id != 0) ({ deleteUser = user }) else null
                )
            }

            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (!runningAsOwner) "ניהול Root: זמין במשתמש הראשי בלבד" else if (rootOk) "גישה למערכת: Root פעיל ✓" else "גישה למערכת: Root לא מורשית ✗",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    if (message.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("הוספת משתמש") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("שם המשתמש") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !creatingUser,
                    onClick = {
                        if (!runningAsOwner || !rootOk) {
                            message = "יש לפתוח את האפליקציה במשתמש הראשי עם Root מורשה"
                        } else if (newName.isBlank()) {
                            message = "יש להזין שם משתמש"
                        } else {
                            creatingUser = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    manager.createUser(newName.trim())
                                }
                                message = result
                                creatingUser = false
                                showAdd = false
                                withContext(Dispatchers.IO) {
                                    users = manager.listUsers()
                                    currentId = manager.currentUser()
                                    maxUsers = manager.maxUsers()
                                    rootOk = manager.hasRoot()
                                    runningAsOwner = manager.currentUserNonRoot() == 0
                                }
                            }
                        }
                    }
                ) { Text(if (creatingUser) "יוצר משתמש..." else "הוסף") }
            },
            dismissButton = {
                TextButton(enabled = !creatingUser, onClick = { showAdd = false }) { Text("ביטול") }
            }
        )
    }

    deleteUser?.let { user ->
        AlertDialog(
            onDismissRequest = { deleteUser = null },
            title = { Text("מחיקת משתמש") },
            text = { Text("למחוק את המשתמש \"${user.name.ifBlank { "ללא שם" }}\"? הפעולה תמחק את נתוני המשתמש.") },
            confirmButton = {
                TextButton(onClick = {
                    message = if (!runningAsOwner || !rootOk) "יש לפתוח את האפליקציה במשתמש הראשי עם Root מורשה" else manager.removeUser(user.id)
                    deleteUser = null
                    refresh()
                }) { Text("מחק") }
            },
            dismissButton = {
                TextButton(onClick = { deleteUser = null }) { Text("ביטול") }
            }
        )
    }
}

@Composable
private fun UserSettingsRow(
    user: AndroidUser,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(user.id, user.name)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                user.name.ifBlank { if (user.id == 0) "בעלים" else "משתמש ${user.id}" },
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                if (selected) "המשתמש הנוכחי" else "לחץ כדי לעבור למשתמש הזה",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "נבחר", tint = MaterialTheme.colorScheme.primary)
        } else if (onDelete != null) {
            TextButton(onClick = onDelete) { Text("מחק") }
        }
    }
}

@Composable
private fun UserAvatar(id: Int, name: String) {
    val letter = name.trim().take(1).ifBlank { if (id == 0) "ב" else "מ" }
    val avatarColors = listOf(
        Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFF8E7CC3),
        Color(0xFFEF8A5B), Color(0xFF4FA3D1), Color(0xFF7E57C2), Color(0xFF00897B)
    )
    val bg = avatarColors[kotlin.math.abs(id) % avatarColors.size]

    Box(
        modifier = Modifier.size(56.dp).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (name.isBlank()) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        } else {
            Text(letter, color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
    }
}

class RootUserManager {
    private fun runRoot(command: String): String {
        return try {
            val p = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val text = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }.trim()
            p.waitFor()
            text
        } catch (e: Exception) {
            "ERROR: ${e.message ?: "unknown"}"
        }
    }

    fun hasRoot(): Boolean = runRoot("id").contains("uid=0")

    fun currentUserNonRoot(): Int? =
        Regex("""(\d+)""").find(runRoot("am get-current-user"))?.groupValues?.get(1)?.toIntOrNull()

    fun listUsers(): List<AndroidUser> {
        val out = runRoot("cmd user list")
        if (out.startsWith("ERROR")) return emptyList()
        return out.lineSequence().mapNotNull { line ->
            val m = Regex("""UserInfo\{(\d+):([^:}]*)""").find(line) ?: return@mapNotNull null
            AndroidUser(m.groupValues[1].toInt(), m.groupValues[2])
        }.toList()
    }

    fun currentUser(): Int? =
        Regex("""(\d+)""").find(runRoot("am get-current-user"))?.groupValues?.get(1)?.toIntOrNull()

    fun maxUsers(): Int? =
        Regex("""(\d+)""").find(runRoot("pm get-max-users"))?.groupValues?.get(1)?.toIntOrNull()

    fun createUser(name: String): String {
        val safe = name.replace("'", "'\\''")
        val commands = listOf("cmd user create '$safe'", "pm create-user '$safe'")
        var lastOutput = ""
        var lastExit = -1
        for (command in commands) {
            val result = runRootDetailed(command)
            lastOutput = result.first
            lastExit = result.second
            if (lastExit == 0 && (lastOutput.contains("Success", true) ||
                Regex("created user id\\s+\\d+", RegexOption.IGNORE_CASE).containsMatchIn(lastOutput))) {
                return "המשתמש נוצר בהצלחה. $lastOutput"
            }
            if (lastOutput.contains("maximum number", true) || lastOutput.contains("too many users", true)) {
                return "אי אפשר ליצור משתמש נוסף: המכשיר הגיע למספר המשתמשים המרבי. $lastOutput"
            }
            if (lastOutput.contains("not allowed", true) || lastOutput.contains("not supported", true)) {
                return "המערכת חסמה יצירת משתמש במכשיר הזה. $lastOutput"
            }
        }
        return "יצירת המשתמש נכשלה (קוד $lastExit). פלט המערכת: ${lastOutput.ifBlank { "לא התקבלה תשובה מהמערכת" }}"
    }

    private fun runRootDetailed(command: String): Pair<String, Int> {
        return try {
            val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }.trim()
            val finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return "הפקודה לא הסתיימה בתוך 30 שניות" to -1
            }
            output to process.exitValue()
        } catch (e: Exception) {
            "שגיאת Root: ${e.message ?: "unknown"}" to -1
        }
    }

    fun switchUser(id: Int): String {
        val out = runRoot("am switch-user $id")
        return if (out.isBlank()) "בוצע מעבר למשתמש $id" else out
    }

    fun removeUser(id: Int): String {
        if (id == 0) return "אי אפשר למחוק את הבעלים"
        val out = runRoot("cmd user remove $id")
        return if (out.contains("Success", true) || out.isBlank()) "המשתמש נמחק" else out
    }
}
