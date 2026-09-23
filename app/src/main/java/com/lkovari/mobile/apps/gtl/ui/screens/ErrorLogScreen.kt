package com.lkovari.mobile.apps.gtl.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.diagnostics.AppErrorLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ErrorLogScreen(onBack: () -> Unit) {
    var body by remember { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        body = withContext(Dispatchers.IO) { AppErrorLog.read() }
    }
    SecondaryScaffold(stringResource(R.string.error_log_title), onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { AppErrorLog.clear() }
                        body = ""
                    }
                }
            ) {
                Text(stringResource(R.string.error_log_clear))
            }
            val shown = body
            if (shown != null) {
                SelectionContainer(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scroll)
                ) {
                    Text(
                        text = shown.ifEmpty { stringResource(R.string.error_log_empty) },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
