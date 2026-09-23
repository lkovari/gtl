package com.lkovari.mobile.apps.gtl

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.lkovari.mobile.apps.gtl.ui.GtlApp
import com.lkovari.mobile.apps.gtl.viewmodel.GtlViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: GtlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { !viewModel.uiState.value.settingsLoaded }
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.settingsLoaded) {
                    findViewById<View>(android.R.id.content).invalidate()
                }
            }
        }
        setContent {
            GtlApp(viewModel)
        }
    }
}
