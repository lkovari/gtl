package com.lkovari.mobile.apps.gtl.ui

import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.domain.TrackShareFailure
import com.lkovari.mobile.apps.gtl.ui.screens.AboutScreen
import com.lkovari.mobile.apps.gtl.ui.screens.ErrorLogScreen
import com.lkovari.mobile.apps.gtl.ui.screens.DisclaimerScreen
import com.lkovari.mobile.apps.gtl.ui.screens.HelpScreen
import com.lkovari.mobile.apps.gtl.ui.screens.LocationSettingsScreen
import com.lkovari.mobile.apps.gtl.ui.screens.MainTrackerScreen
import com.lkovari.mobile.apps.gtl.ui.screens.OsmDownloadScreen
import com.lkovari.mobile.apps.gtl.ui.screens.SettingsScreen
import com.lkovari.mobile.apps.gtl.ui.screens.TracksScreen
import com.lkovari.mobile.apps.gtl.ui.theme.GtlTheme
import com.lkovari.mobile.apps.gtl.viewmodel.GtlViewModel

@Composable
fun GtlApp(viewModel: GtlViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    val keepScreenOn = state.live.logging && state.settings.keepScreenOnWhileLogging
    DisposableEffect(keepScreenOn, activity) {
        val window = activity?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    GtlTheme(darkTheme = isSystemInDarkTheme()) {
        when (launchScreen(state.settingsLoaded, state.settings.disclaimerAccepted)) {
            LaunchScreen.Hold -> Unit
            LaunchScreen.Disclaimer -> DisclaimerScreen(
                onAccept = { viewModel.acceptDisclaimer() },
                onRefuse = { activity?.finish() }
            )
            LaunchScreen.Tracker -> {
                val nav = rememberNavController()
                val context = LocalContext.current
                NavHost(navController = nav, startDestination = "main") {
                    composable("main") {
                        MainTrackerScreen(
                            state = state,
                            viewModel = viewModel,
                            onOpenSettings = { nav.navigate("settings") },
                            onOpenMaps = { nav.navigate("osm") },
                            onOpenTracks = { nav.navigate("tracks") },
                            onOpenHelp = { nav.navigate("help") },
                            onOpenAbout = { nav.navigate("about") },
                            onOpenErrorLog = { nav.navigate("diagnostics") },
                            onOpenLocationSettings = { nav.navigate("location") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(state, viewModel) { nav.popBackStack() }
                    }
                    composable("osm") {
                        OsmDownloadScreen(viewModel) { nav.popBackStack() }
                    }
                    composable("tracks") {
                        TracksScreen(
                            state = state,
                            viewModel = viewModel,
                            onBack = { nav.popBackStack() },
                            onShare = { ids, format ->
                                viewModel.shareSessions(
                                    ids,
                                    format,
                                    onReady = { intent ->
                                        context.startActivity(Intent.createChooser(intent, null))
                                    },
                                    onFailed = { failure ->
                                        val message = if (failure == TrackShareFailure.Empty) {
                                            R.string.export_empty
                                        } else {
                                            R.string.export_failed
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            onShowOnMap = { sessionId ->
                                viewModel.showSessionOnMap(sessionId)
                                nav.popBackStack()
                            }
                        )
                    }
                    composable("help") {
                        HelpScreen(
                            onBack = { nav.popBackStack() },
                            tuhuMapDownloaded = state.tuhuMapDownloaded
                        )
                    }
                    composable("location") {
                        LocationSettingsScreen { nav.popBackStack() }
                    }
                    composable("about") {
                        AboutScreen(onBack = { nav.popBackStack() })
                    }
                    composable("diagnostics") {
                        ErrorLogScreen { nav.popBackStack() }
                    }
                }
            }
        }
    }
}
