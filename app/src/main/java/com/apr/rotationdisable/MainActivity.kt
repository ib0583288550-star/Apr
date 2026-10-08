package com.yb.usersmanager

import android.os.Bundle
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
import java.io.BufferedReader
import java.io.InputStreamReader

data class AndroidUser(val id: Int, val name: String)

class MainActivity : ComponentActivity() {
    private val manager = RootUserManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                UserManagerScreen(manager)
            }
        }
    }
}

@Composable
private fun UserManagerScreen(manager: RootUserManager) {
    var users by remember { mutableStateOf(manager.listUsers()) }
    var maxUsers by remember { mutableStateOf(manager.maxUsers()) }
    var rootOk by remember { mutableStateOf(manager.hasRoot()) }
    var newName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    fun refresh() {
        rootOk = manager.hasRoot()
        users = manager.listUsers()
        maxUsers = manager.maxUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ניהול משתמשים Root") })
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (rootOk) "Root: מחובר ✓" else "Root: לא זמין ✗")
                    Text("משתמשים קיימים: ${users.size}")
                    Text("מקסימום נתמך: ${maxUsers ?: "לא ידוע"}")
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("שם משתמש חדש") },
                    singleLine = true
                )
                Button(onClick = {
                    if (newName.isBlank()) {
                        message = "כתוב שם למשתמש"
                    } else {
                        message = manager.createUser(newName.trim())
                        newName = ""
                        refresh()
                    }
                }) { Text("צור") }
            }

            Button(
                onClick = { refresh(); message = "הרשימה עודכנה" },
                modifier = Modifier.fillMaxWidth()
            ) { Text("רענן") }

            if (message.isNotBlank()) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }

            Text("משתמשים", style = MaterialTheme.typography.titleLarge)

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(user.name.ifBlank { "ללא שם" })
                                Text("ID: ${user.id}", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = {
                                message = manager.switchUser(user.id)
                                refresh()
                            }) { Text("עבור") }
                            if (user.id != 0) {
                                OutlinedButton(onClick = {
                                    message = manager.removeUser(user.id)
                                    refresh()
                                }) { Text("מחק") }
                            }
                        }
                    }
                }
            }
        }
    }
}

class RootUserManager {
    private fun runRoot(command: String): String {
        return try {
            val p = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val text = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }.trim()
            p.waitFor()
            text
        } catch (e: Exception) {
            "ERROR: ${e.message ?: "unknown"}"
        }
    }

    fun hasRoot(): Boolean {
        val out = runRoot("id")
        return out.contains("uid=0")
    }

    fun listUsers(): List<AndroidUser> {
        val out = runRoot("cmd user list")
        if (out.startsWith("ERROR")) return emptyList()
        return out.lineSequence().mapNotNull { line ->
            val m = Regex("""UserInfo\{(\d+):([^:}]*)""").find(line) ?: return@mapNotNull null
            AndroidUser(m.groupValues[1].toInt(), m.groupValues[2])
        }.toList()
    }

    fun maxUsers(): Int? {
        val out = runRoot("pm get-max-users")
        return Regex("""(\d+)""").find(out)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun createUser(name: String): String {
        val safe = name.replace("'", "'\\''")
        val out = runRoot("cmd user create '$safe'")
        return if (out.contains("Success", true)) "המשתמש נוצר בהצלחה" else out
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
