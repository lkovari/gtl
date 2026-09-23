package com.lkovari.mobile.apps.gtl.tuhu

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.ui.theme.TitleMagenta

@Composable
fun TuhuAboutSection() {
    if (!TuhuFeature.showAbout()) {
        return
    }
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.tuhu_about_body),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(R.string.tuhu_about_website),
            color = TitleMagenta,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TuhuCatalog.WEBSITE_URL)))
            }
        )
    }
}

@Composable
fun TuhuHelpSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.tuhu_help_hungary), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.tuhu_help_intro), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.tuhu_help_blazes), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.tuhu_help_paths), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.tuhu_help_contours), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.tuhu_help_contours_minor), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.tuhu_help_hike_poi), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.tuhu_help_parks), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.tuhu_help_urban_poi), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.tuhu_help_hillshading), style = MaterialTheme.typography.bodyLarge)
    }
}
