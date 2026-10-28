package com.example.premiere

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.premiere.theme.PremiereTheme
import com.example.premiere.ui.AppScaffold
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val openTickets = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        
        openTickets.value = intent?.action == "com.example.premiere.ACTION_OPEN_TICKETS"
        
        // Force system dark mode so IME follows the app theme
        val uiModeManager = getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            uiModeManager.setApplicationNightMode(android.app.UiModeManager.MODE_NIGHT_YES)
        } else {
            val config = resources.configuration
            config.uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES or (config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv())
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }

        enableEdgeToEdge()
        setContent {
            val showTicketsInitial by openTickets.collectAsState()
            PremiereTheme {
                AppScaffold(
                    modifier = Modifier.fillMaxSize(),
                    initialShowTickets = showTicketsInitial,
                    onInitialTicketsConsumed = { openTickets.value = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == "com.example.premiere.ACTION_OPEN_TICKETS") {
            openTickets.value = true
        }
    }
}
