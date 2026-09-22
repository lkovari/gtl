package com.lkovari.mobile.apps.gtl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.MapPlaceKind
import com.lkovari.mobile.apps.gtl.engine.MapSearch
import com.lkovari.mobile.apps.gtl.engine.MapSearchHit
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.TapReadout

@Composable
fun MapSearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    hits: List<MapSearchHit>,
    indexing: Boolean,
    failed: Boolean,
    truncated: Boolean,
    searching: Boolean,
    measurement: MeasurementSystem,
    onNavigate: (MapSearchHit) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var pending by remember { mutableStateOf<MapSearchHit?>(null) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
    val accepted = MapSearch.accepts(query)
    val message = when {
        query.isNotBlank() && !accepted -> stringResource(R.string.map_search_min)
        failed && hits.isEmpty() && !indexing && !searching && accepted -> stringResource(R.string.map_search_failed)
        indexing && hits.isEmpty() && accepted -> stringResource(R.string.map_search_indexing)
        truncated && hits.isEmpty() && !searching && accepted && !indexing -> stringResource(R.string.map_search_partial)
        accepted && !searching && hits.isEmpty() && !indexing && !truncated -> stringResource(R.string.map_search_empty)
        else -> null
    }
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.map_search_hint)) },
            trailingIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.map_search_close)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )
        if (indexing && hits.isNotEmpty()) {
            Text(
                text = stringResource(R.string.map_search_indexing),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp)
            )
        }
        if (truncated && hits.isNotEmpty() && !indexing) {
            Text(
                text = stringResource(R.string.map_search_partial),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp)
            )
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        hits.forEach { hit ->
            val kind = kindLabel(hit.kind)
            val distance = TapReadout.formatStraightLine(hit.distanceMeters, measurement, "")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pending = hit }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = hit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.map_navigate_body, kind, distance),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    val chosen = pending
    if (chosen != null) {
        val kind = kindLabel(chosen.kind)
        val distance = TapReadout.formatStraightLine(chosen.distanceMeters, measurement, "")
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.map_navigate_title, chosen.name)) },
            text = { Text(stringResource(R.string.map_navigate_body, kind, distance)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pending = null
                        onNavigate(chosen)
                    }
                ) {
                    Text(stringResource(R.string.action_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) {
                    Text(stringResource(R.string.action_no))
                }
            }
        )
    }
}

@Composable
private fun kindLabel(kind: MapPlaceKind): String {
    val res = when (kind) {
        MapPlaceKind.City -> R.string.map_kind_city
        MapPlaceKind.Town -> R.string.map_kind_town
        MapPlaceKind.Village -> R.string.map_kind_village
        MapPlaceKind.Hamlet -> R.string.map_kind_hamlet
        MapPlaceKind.Suburb -> R.string.map_kind_suburb
        MapPlaceKind.Peak -> R.string.map_kind_peak
        MapPlaceKind.Statue -> R.string.map_kind_statue
        MapPlaceKind.Monument -> R.string.map_kind_monument
        MapPlaceKind.Landmark -> R.string.map_kind_landmark
        MapPlaceKind.House -> R.string.map_kind_house
        MapPlaceKind.Building -> R.string.map_kind_building
        MapPlaceKind.Street -> R.string.map_kind_street
        MapPlaceKind.Place -> R.string.map_kind_place
    }
    return stringResource(res)
}
