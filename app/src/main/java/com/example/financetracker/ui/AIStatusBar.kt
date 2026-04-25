package com.example.financetracker.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financetracker.ml.DownloadStatus

@Composable
fun AIStatusBar(
    downloadStatus: DownloadStatus,
    isAnalyzing: Boolean
) {
    val visible = isAnalyzing || downloadStatus !is DownloadStatus.Idle

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val backgroundColor = when {
            isAnalyzing -> MaterialTheme.colorScheme.tertiaryContainer
            downloadStatus is DownloadStatus.Error -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        }

        val contentColor = when {
            isAnalyzing -> MaterialTheme.colorScheme.onTertiaryContainer
            downloadStatus is DownloadStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onSecondaryContainer
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when {
                        isAnalyzing -> "AI: Analyzing receipt..."
                        downloadStatus is DownloadStatus.Downloading -> "AI: Downloading model..."
                        downloadStatus is DownloadStatus.Error -> "AI: Error"
                        downloadStatus is DownloadStatus.Downloaded -> "AI: Model ready"
                        else -> ""
                    },
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                if (downloadStatus is DownloadStatus.Downloading) {
                    Text(
                        text = "${(downloadStatus.progress * 100).toInt()}%",
                        color = contentColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (downloadStatus is DownloadStatus.Downloading) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { downloadStatus.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.2f)
                )
            } else if (isAnalyzing) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.2f)
                )
            } else if (downloadStatus is DownloadStatus.Error) {
                Text(
                    text = downloadStatus.message,
                    color = contentColor,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}
