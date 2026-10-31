package com.kards.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kards.app.ui.KardsApp
import com.kards.app.ui.theme.KardsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashLogger()
        enableEdgeToEdge()
        setContent {
            KardsTheme {
                KardsApp()
            }
        }
    }

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val file = java.io.File(getExternalFilesDir(null), "crash.log")
                file.appendText("\n=== ${System.currentTimeMillis()} ===\n")
                file.appendText(throwable.stackTraceToString())
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}