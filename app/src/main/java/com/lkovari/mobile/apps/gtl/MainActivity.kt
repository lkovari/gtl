package com.lkovari.mobile.apps.gtl

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lkovari.mobile.apps.gtl.ui.GtlApp
import com.lkovari.mobile.apps.gtl.ui.theme.applyGtlSystemBars
import com.lkovari.mobile.apps.gtl.viewmodel.GtlViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GtlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        applyGtlSystemBars(this, viewModel.uiState.value.darkTheme)
        splash.setKeepOnScreenCondition { !viewModel.settingsLoaded.value }
        setContent {
            GtlApp(viewModel)
        }
    }
}
