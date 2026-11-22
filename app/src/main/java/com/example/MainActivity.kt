package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.navigation.WifiToolsApp
import com.example.presentation.viewmodel.WifiToolsViewModel
import com.example.ui.theme.WifiToolsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK)
        )
        val appContainer = (application as WifiToolsApplication).container

        setContent {
            val viewModel: WifiToolsViewModel = viewModel(
                factory = WifiToolsViewModel.factory(appContainer)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            WifiToolsTheme(language = uiState.language) {
                WifiToolsApp(viewModel = viewModel)
            }
        }
    }
}
