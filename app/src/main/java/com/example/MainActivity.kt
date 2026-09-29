package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ChatViewModel = viewModel()
            val currentTheme by viewModel.selectedTheme.collectAsState()
            val currentThemeMode by viewModel.themeMode.collectAsState()

            MyApplicationTheme(
                selectedTheme = currentTheme,
                themeMode = currentThemeMode
            ) {
                ChatScreen(viewModel = viewModel)
            }
        }
    }
}
