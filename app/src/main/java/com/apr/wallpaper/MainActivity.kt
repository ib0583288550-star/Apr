package com.apr.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WallpaperApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperApp() {
    val context = LocalContext.current
    var images by remember { mutableStateOf(loadImages(context)) }
    var interval by remember { mutableStateOf(60L) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            images = (images + uris.map(Uri::toString)).distinct()
            saveImages(context, images)
        }
    }

    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("מחליף הטפטים") }) }) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("טפטים", style = MaterialTheme.typography.headlineMedium)
                    Text("הוסף תמונות ובחר כל כמה זמן להחליף את הטפט.")
                }
                item {
                    Button(
                        onClick = { picker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("הוסף תמונות מהטלפון") }
                }
                item {
                    Text("החלפה כל: ${interval} דקות")
                    Slider(
                        value = interval.toFloat(),
                        onValueChange = { interval = it.toLong() },
                        valueRange = 1f..1440f,
                        steps = 1439
                    )
                }
                item {
                    Button(
                        onClick = { scheduleWallpaper(context, interval) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = images.isNotEmpty()
                    ) { Text("הפעל החלפה אוטומטית") }
                }
                item {
                    OutlinedButton(
                        onClick = { setFirstWallpaper(context, images) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = images.isNotEmpty()
                    ) { Text("החלף עכשיו") }
                }
                item { Text("התמונות שנבחרו: ${images.size}") }
                items(images) { uri ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(uri.substringAfterLast('/'), Modifier.weight(1f), maxLines = 1)
                            TextButton(onClick = {
                                images = images.filterNot { it == uri }
                                saveImages(context, images)
                            }) { Text("הסר") }
                        }
                    }
                }
            }
        }
    }
}

private fun saveImages(context: Context, images: List<String>) {
    context.getSharedPreferences("wallpapers", Context.MODE_PRIVATE)
        .edit().putStringSet("images", images.toSet()).apply()
}

private fun loadImages(context: Context): List<String> =
    context.getSharedPreferences("wallpapers", Context.MODE_PRIVATE)
        .getStringSet("images", emptySet())?.toList() ?: emptyList()

private fun setFirstWallpaper(context: Context, images: List<String>) {
    if (images.isEmpty()) return
    try {
        context.contentResolver.openInputStream(Uri.parse(images.first()))?.use {
            WallpaperManager.getInstance(context).setStream(it)
        }
    } catch (_: Exception) {}
}

private fun scheduleWallpaper(context: Context, minutes: Long) {
    val request = PeriodicWorkRequestBuilder<WallpaperWorker>(
        minutes.coerceAtLeast(15), TimeUnit.MINUTES
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "wallpaper_rotation",
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

class WallpaperWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val images = loadImages(applicationContext)
        if (images.isEmpty()) return Result.success()

        val prefs = applicationContext.getSharedPreferences("wallpapers", Context.MODE_PRIVATE)
        val index = (prefs.getInt("index", -1) + 1) % images.size
        prefs.edit().putInt("index", index).apply()

        return try {
            applicationContext.contentResolver
                .openInputStream(Uri.parse(images[index]))?.use {
                    WallpaperManager.getInstance(applicationContext).setStream(it)
                }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
