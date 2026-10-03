package com.lkovari.mobile.apps.gtl.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.lkovari.mobile.apps.gtl.R

@Composable
fun rememberMapDownloadStart(onStart: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val start = rememberUpdatedState(onStart)
    var confirmMobile by rememberSaveable { mutableStateOf(false) }
    val continueDownload = {
        if (isActiveNetworkMetered(context)) {
            confirmMobile = true
        } else {
            start.value()
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        continueDownload()
    }
    if (confirmMobile) {
        AlertDialog(
            onDismissRequest = { confirmMobile = false },
            title = { Text(stringResource(R.string.download_mobile_title)) },
            text = { Text(stringResource(R.string.download_mobile_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmMobile = false
                        start.value()
                    }
                ) {
                    Text(stringResource(R.string.osm_download))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmMobile = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    return {
        val needsNotification = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        if (needsNotification) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            continueDownload()
        }
    }
}

fun isActiveNetworkMetered(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    return manager.isActiveNetworkMetered
}
