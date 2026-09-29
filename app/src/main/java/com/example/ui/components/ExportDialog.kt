package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.export.ExportBitrate
import com.example.export.ExportConfig
import com.example.export.ExportFormat
import com.example.export.ExportFramerate
import com.example.export.ExportProgress
import com.example.export.ExportResolution
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.Indicator4K
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ExportDialog(
    config: ExportConfig,
    progress: ExportProgress,
    onConfigChange: (ExportConfig) -> Unit,
    onStartExport: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { if (!progress.isExporting) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(20.dp)),
            color = StudioSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = Indicator4K,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Render & Export Master",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!progress.isExporting) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("export_dialog_close")) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (progress.isExporting || progress.isFinished) {
                    // Progress UI
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F131C))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = progress.currentStage,
                            color = if (progress.isFinished) AccentGreen else AccentGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { progress.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (progress.isFinished) AccentGreen else AccentCyan,
                            trackColor = Color(0x33FFFFFF)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${(progress.progress * 100).toInt()}% Rendered",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    // Export Settings Selector
                    Text("RESOLUTION", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    ExportResolution.entries.forEach { res ->
                        val isSelected = config.resolution == res
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0x33FFB703) else Color(0x1AFFFFFF))
                                .border(1.dp, if (isSelected) AccentGold else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { onConfigChange(config.copy(resolution = res)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(res.displayName, color = if (isSelected) AccentGold else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                if (res == ExportResolution.UHD_4K || res == ExportResolution.CINEMA_20_9) {
                                    StatusBadge(text = "4K UHD", textColor = Indicator4K)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("TARGET BITRATE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportBitrate.entries.forEach { bit ->
                            val isSelected = config.bitrate == bit
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x3300F5D4) else Color(0x1AFFFFFF))
                                    .border(1.dp, if (isSelected) AccentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { onConfigChange(config.copy(bitrate = bit)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (bit == ExportBitrate.HIGH) "50M (High)" else if (bit == ExportBitrate.MEDIUM) "20M (Bal)" else "10M (Low)",
                                    color = if (isSelected) AccentCyan else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("FORMAT & ENCODER", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportFormat.entries.forEach { fmt ->
                            val isSelected = config.format == fmt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x3338BDF8) else Color(0x1AFFFFFF))
                                    .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { onConfigChange(config.copy(format = fmt)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fmt.displayName,
                                    color = if (isSelected) Color(0xFF38BDF8) else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (progress.isFinished) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier.testTag("export_done_button")
                        ) {
                            Text("Done (Saved to Gallery)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else if (!progress.isExporting) {
                        Button(
                            onClick = onStartExport,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_export_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start 4K Render", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
