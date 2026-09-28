package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SamScreen
import com.example.ui.SamViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var viewModelInstance: SamViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: SamViewModel = viewModel()
            viewModelInstance = vm
            val uiState by vm.uiState.collectAsState()

            MyApplicationTheme(
                accentPrimary = uiState.themeAccent.primary,
                accentSecondary = uiState.themeAccent.secondary,
                accentTertiary = uiState.themeAccent.tertiary,
                accentGlow = uiState.themeAccent.glow
            ) {
                SamScreen(viewModel = vm)
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("trigger_listening", false) == true) {
            viewModelInstance?.startSession()
        }
    }
}
