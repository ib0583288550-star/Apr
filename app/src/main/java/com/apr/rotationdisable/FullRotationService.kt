package com.apr.rotationdisable

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.OrientationEventListener
import android.hardware.SensorManager
import kotlin.concurrent.thread

class FullRotationService : Service() {
    companion object {
        const val EXTRA_PORTRAIT_ONLY = "portrait_only"
        const val EXTRA_SENSITIVITY = "sensitivity_degrees"
    }

    private var listener: OrientationEventListener? = null
    @Volatile private var lastRotation = -1
    @Volatile private var portraitOnly = false
    @Volatile private var sensitivity = 15
    @Volatile private var lastSensorQuadrant = -1

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel("full_rotation", "סיבוב מסך", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, "full_rotation")
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentTitle("בקרת סיבוב מסך פעילה")
            .setContentText("הסיבוב נשלט לפי מצב המכשיר")
            .setOngoing(true)
            .build()
        startForeground(77, notification)
        thread { runRoot("settings put system accelerometer_rotation 0") }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        portraitOnly = intent?.getBooleanExtra(EXTRA_PORTRAIT_ONLY, false) ?: portraitOnly
        sensitivity = (intent?.getIntExtra(EXTRA_SENSITIVITY, sensitivity) ?: sensitivity).coerceIn(0, 30)
        lastRotation = -1
        lastSensorQuadrant = -1
        if (listener == null) {
            listener = object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
                override fun onOrientationChanged(orientation: Int) {
                    if (orientation == ORIENTATION_UNKNOWN) return

                    val nearestQuadrant = ((orientation + 45) / 90) % 4
                    if (lastSensorQuadrant < 0) {
                        lastSensorQuadrant = if (portraitOnly) {
                            if (orientation in 135..225) 2 else 0
                        } else nearestQuadrant
                    } else if (portraitOnly) {
                        // Require a stronger tilt before switching between 0° and 180°.
                        val currentCenter = lastSensorQuadrant * 90
                        val distance = kotlin.math.abs(((orientation - currentCenter + 540) % 360) - 180)
                        if (distance > 135 + sensitivity) {
                            lastSensorQuadrant = if (lastSensorQuadrant == 0) 2 else 0
                        }
                    } else {
                        // Hysteresis: keep the current direction until the device passes
                        // the normal 45° boundary plus the selected sensitivity margin.
                        val currentCenter = lastSensorQuadrant * 90
                        val distance = kotlin.math.abs(((orientation - currentCenter + 540) % 360) - 180)
                        if (distance > 45 + sensitivity) lastSensorQuadrant = nearestQuadrant
                    }

                    val rotation = if (portraitOnly) {
                        lastSensorQuadrant
                    } else {
                        // Sensor landscape quadrants are opposite to Surface rotation numbering.
                        when (lastSensorQuadrant) {
                            1 -> 3
                            3 -> 1
                            else -> lastSensorQuadrant
                        }
                    }

                    if (rotation != lastRotation) {
                        lastRotation = rotation
                        thread {
                            val result = runRoot("wm set-user-rotation lock $rotation")
                            if (result.first != 0) {
                                runRoot("settings put system user_rotation $rotation")
                            }
                        }
                    }
                }
            }
            listener?.enable()
        }
        return START_STICKY
    }

    private fun runRoot(command: String): Pair<Int, String> {
        return try {
            val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor() to output.trim()
        } catch (e: Exception) {
            1 to (e.message ?: "Root unavailable")
        }
    }

    override fun onDestroy() {
        listener?.disable()
        listener = null
        thread {
            runRoot("wm set-user-rotation free; settings put system accelerometer_rotation 1")
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
