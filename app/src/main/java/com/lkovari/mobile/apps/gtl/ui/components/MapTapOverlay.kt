package com.lkovari.mobile.apps.gtl.ui.components

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.PostalAddress
import com.lkovari.mobile.apps.gtl.engine.TapReadout
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun MapTapOverlay(
    screen: Offset,
    menuOpen: Boolean,
    showAddress: Boolean,
    coordinateLatitude: Double?,
    coordinateLongitude: Double?,
    address: PostalAddress?,
    onDistance: () -> Unit,
    onCoordinate: () -> Unit,
    onAddress: () -> Unit,
    onClose: () -> Unit
) {
    val card = Modifier
        .offset { IntOffset(screen.x.roundToInt(), screen.y.roundToInt()) }
        .background(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            RoundedCornerShape(8.dp)
        )
        .padding(4.dp)
    if (menuOpen) {
        Column(modifier = card) {
            TextButton(onClick = onDistance) {
                Text(stringResource(R.string.map_tap_distance))
            }
            TextButton(onClick = onCoordinate) {
                Text(stringResource(R.string.map_tap_gps))
            }
            if (showAddress) {
                TextButton(onClick = onAddress) {
                    Text(stringResource(R.string.map_tap_address))
                }
            }
        }
    } else if ((coordinateLatitude != null && coordinateLongitude != null) || address != null) {
        Column(modifier = card.padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (coordinateLatitude != null && coordinateLongitude != null) {
                CoordinateLines(coordinateLatitude, coordinateLongitude)
            }
            if (address != null) {
                AddressLine(R.string.map_tap_zip, address.zip)
                AddressLine(R.string.map_tap_country, address.country)
                AddressLine(R.string.map_tap_city, address.city)
                AddressLine(R.string.map_tap_street, address.street)
                AddressLine(R.string.map_tap_house, address.houseNumber)
            }
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.map_tap_close))
            }
        }
    }
}

@Composable
private fun CoordinateLines(latitude: Double, longitude: Double) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val searchText = TapReadout.formatCoordinate(latitude, longitude)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(
                text = stringResource(R.string.map_tap_lat, TapReadout.formatLatitude(latitude)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.map_tap_lon, TapReadout.formatLongitude(longitude)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText("coordinate", searchText))
                    )
                }
            }
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.map_tap_copy)
            )
        }
    }
}

@Composable
private fun AddressLine(labelRes: Int, value: String) {
    Text(
        text = stringResource(labelRes) + ": " + value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}
