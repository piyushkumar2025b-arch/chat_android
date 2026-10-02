package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.FileUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceStorageSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onBrowsePhoneStorage: () -> Unit,
    onPickDocuments: () -> Unit,
    onPickPhotos: () -> Unit,
    onPickCode: () -> Unit,
    onAddSampleFile: (sampleType: String) -> Unit,
    onAttachCachedFile: ((File) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageStats = remember { FileUtils.getDeviceStorageStats(context) }
    val cachedFiles = remember { FileUtils.getCachedAttachments(context) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("device_storage_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Storage Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SdStorage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Phone Storage & File Hub",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Connected to Phone Storage • SAF Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Device Storage Telemetry Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Device Internal Storage",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "${storageStats.availableFormatted} free of ${storageStats.totalFormatted}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (storageStats.usedPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "App Attachment Vault: ${storageStats.cachedAttachmentsCount} files (${storageStats.cachedAttachmentsFormatted})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${storageStats.usedPercent}% used",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Storage Action Cards Grid
            Text(
                text = "Access Phone Storage",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            StorageSourceCard(
                icon = Icons.Default.Folder,
                title = "Browse Phone Storage (All Files)",
                subtitle = "Open device file explorer: Internal Storage, Downloads, SD Card & Cloud",
                tag = "browse_phone_storage_button",
                onClick = {
                    onDismiss()
                    onBrowsePhoneStorage()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            StorageSourceCard(
                icon = Icons.Default.Description,
                title = "Documents & Spreadsheets",
                subtitle = "Extract text & tables from PDF, Word (.docx), Excel (.xlsx), PPTX, RTF",
                tag = "pick_documents_button",
                onClick = {
                    onDismiss()
                    onPickDocuments()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            StorageSourceCard(
                icon = Icons.Default.Code,
                title = "Code, Scripts & Data",
                subtitle = "Python (.py), Kotlin (.kt), Java, JS, JSON, CSV, TSV, SQL, XML, Logs",
                tag = "pick_code_button",
                onClick = {
                    onDismiss()
                    onPickCode()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            StorageSourceCard(
                icon = Icons.Default.Image,
                title = "Photo Library & Camera",
                subtitle = "Images with OCR and multimodal vision analysis (JPG, PNG, WEBP)",
                tag = "pick_photos_button",
                onClick = {
                    onDismiss()
                    onPickPhotos()
                }
            )

            // Recently Cached Phone Files on Device
            if (cachedFiles.isNotEmpty() && onAttachCachedFile != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Saved Files on Device (${cachedFiles.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cachedFiles.take(8)) { file ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clickable {
                                    onDismiss()
                                    onAttachCachedFile(file)
                                }
                                .testTag("cached_file_${file.name.take(10)}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (file.name.length > 20) file.name.take(18) + "..." else file.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = FileUtils.formatFileSize(file.length()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Preloaded Sample Files for Instant Testing
            Text(
                text = "Instant Testing Sample Files",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap any file below to verify instant data retrieval across formats",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    SampleFileChip(
                        name = "Financial_Q3.csv",
                        type = "CSV Spreadsheet",
                        icon = Icons.Default.TableChart,
                        onClick = {
                            onDismiss()
                            onAddSampleFile("csv")
                        }
                    )
                }
                item {
                    SampleFileChip(
                        name = "Analysis_Script.py",
                        type = "Python Code",
                        icon = Icons.Default.Code,
                        onClick = {
                            onDismiss()
                            onAddSampleFile("python")
                        }
                    )
                }
                item {
                    SampleFileChip(
                        name = "Sales_Forecast.xlsx",
                        type = "Excel Sheet",
                        icon = Icons.Default.TableChart,
                        onClick = {
                            onDismiss()
                            onAddSampleFile("excel")
                        }
                    )
                }
                item {
                    SampleFileChip(
                        name = "System_Metrics.json",
                        type = "JSON Data",
                        icon = Icons.Default.Description,
                        onClick = {
                            onDismiss()
                            onAddSampleFile("json")
                        }
                    )
                }
                item {
                    SampleFileChip(
                        name = "Project_Notes.txt",
                        type = "Text Doc",
                        icon = Icons.Default.Description,
                        onClick = {
                            onDismiss()
                            onAddSampleFile("text")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Supported Formats Pills
            Text(
                text = "Supported Formats (${15}+ types)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val formats = listOf(
                    "PDF (Streams & OCR)",
                    "Word (.docx & .doc)",
                    "Excel (.xlsx & .xls)",
                    "PowerPoint (.pptx)",
                    "CSV & TSV Data",
                    "JSON & JSONL",
                    "XML & HTML",
                    "RTF Documents",
                    "Python & Kotlin",
                    "Java & JS / TS",
                    "C / C++ & C#",
                    "SQL & Shell Scripts",
                    "TXT & Markdown",
                    "ZIP Archives",
                    "JPG, PNG, WEBP",
                    "Audio / Video Meta"
                )
                for (fmt in formats) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = fmt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageSourceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SampleFileChip(
    name: String,
    type: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("sample_${name.substringBefore('.')}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "+ Add $type",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
