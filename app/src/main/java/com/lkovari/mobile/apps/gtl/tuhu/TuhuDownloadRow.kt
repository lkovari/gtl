package com.lkovari.mobile.apps.gtl.tuhu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.data.maps.OsmDownloadState
import com.lkovari.mobile.apps.gtl.viewmodel.GtlViewModel

@Composable
fun TuhuDownloadRow(viewModel: GtlViewModel) {
    if (!TuhuFeature.showDownloadRow()) {
        return
    }
    val download by viewModel.observeTuhuDownload().collectAsState(
        initial = OsmDownloadState(TuhuCatalog.REGION_ID, false, 0, false)
    )
    val mapsRevision by viewModel.observeDownloadedRevision().collectAsState()
    val prefs by viewModel.settings.collectAsState()
    val downloaded = remember(mapsRevision, download.running, download.failed) {
        viewModel.isTuhuDownloaded()
    }
    val inUse = remember(downloaded, prefs.useOfflineMap, prefs.selectedMapFile, mapsRevision) {
        downloaded && viewModel.isTuhuInUse()
    }
    var pendingDelete by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(stringResource(R.string.tuhu_label), style = MaterialTheme.typography.titleLarge)
                Text(
                    if (downloaded) stringResource(R.string.tuhu_downloaded) else TuhuCatalog.REGION_ID,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 28.dp) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (downloaded) {
                        TuhuActionButton(
                            stringResource(if (inUse) R.string.osm_in_use else R.string.osm_can_use)
                        ) {
                            viewModel.toggleTuhuMap()
                        }
                        TuhuActionButton(stringResource(R.string.action_delete)) {
                            pendingDelete = true
                        }
                    } else {
                        TuhuActionButton(
                            stringResource(R.string.tuhu_download),
                            enabled = !download.running
                        ) {
                            viewModel.downloadTuhu()
                        }
                    }
                }
            }
        }
        if (download.running) {
            LinearProgressIndicator(
                progress = { download.progress / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Text("${download.progress} %", style = MaterialTheme.typography.labelMedium)
        }
        if (download.failed) {
            Text(stringResource(R.string.tuhu_failed), color = MaterialTheme.colorScheme.error)
        }
    }
    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = { Text(stringResource(R.string.tuhu_delete_title)) },
            text = { Text(stringResource(R.string.tuhu_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTuhuMap()
                        pendingDelete = false
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun TuhuActionButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
