package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ProjectEntity
import com.example.ui.theme.CheckerDark1
import com.example.ui.theme.CheckerDark2
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    projects: List<ProjectEntity>,
    isDarkTheme: Boolean,
    statusBanner: String? = null,
    onClearBanner: () -> Unit = {},
    onNewProjectClick: () -> Unit,
    onOpenProject: (String) -> Unit,
    onSaveProject: (String) -> Unit = {},
    onDeleteProject: (String) -> Unit,
    onDeleteProjects: (Set<String>) -> Unit = { ids -> ids.forEach(onDeleteProject) },
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()) }
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedProjectIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedProjectIds.isNotEmpty()

    LaunchedEffect(projects) {
        val validIds = projects.mapTo(HashSet()) { it.id }
        if (selectedProjectIds.any { it !in validIds }) {
            selectedProjectIds = selectedProjectIds.intersect(validIds)
        }
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onClearBanner()
        }
    }

    BackHandler(enabled = isSelectionMode) {
        selectedProjectIds = emptySet()
    }

    fun toggleSelection(projectId: String) {
        selectedProjectIds = if (projectId in selectedProjectIds) {
            selectedProjectIds - projectId
        } else {
            selectedProjectIds + projectId
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isSelectionMode) "${selectedProjectIds.size} Selected" else "PixelForge",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isSelectionMode) {
                            IconButton(
                                onClick = { selectedProjectIds = emptySet() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_clear_selection_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear Selection",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = if (isDarkTheme) "White Mode" else "Dark Mode",
                                    tint = if (isDarkTheme) StudioAmber else StudioAzure
                                )
                            }
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
            ) {
                // Large Prominent "+ New" Action Button at top
                item {
                    Button(
                        onClick = onNewProjectClick,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAzure),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("new_project_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "New Project",
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "New Project",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // History Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isSelectionMode) "${selectedProjectIds.size}/${projects.size}" else "${projects.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (projects.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(StudioAzure.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PhotoLibrary,
                                        contentDescription = null,
                                        tint = StudioAzure,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "No projects yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    // Ordered oldest -> newest so #1 is oldest, #2 is next newer, etc.
                    itemsIndexed(projects, key = { _, item -> item.id }) { index, project ->
                        val number = index + 1
                        val isSelected = project.id in selectedProjectIds
                        HistoryProjectCard(
                            number = number,
                            project = project,
                            formattedDate = dateFormat.format(Date(project.updatedAtMillis)),
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onTap = {
                                if (isSelectionMode) {
                                    toggleSelection(project.id)
                                } else {
                                    onOpenProject(project.id)
                                }
                            },
                            onHoldSelect = {
                                selectedProjectIds = selectedProjectIds + project.id
                            },
                            onSave = { onSaveProject(project.id) },
                            onDeleteAllSelected = {
                                val toDelete = selectedProjectIds
                                selectedProjectIds = emptySet()
                                onDeleteProjects(toDelete)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryProjectCard(
    number: Int,
    project: ProjectEntity,
    formattedDate: String,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onTap: () -> Unit,
    onHoldSelect: () -> Unit,
    onSave: () -> Unit,
    onDeleteAllSelected: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var holdProgress by remember(project.id) { mutableFloatStateOf(0f) }

    val thumbBitmap = remember(project.thumbnailPath, project.updatedAtMillis) {
        project.thumbnailPath?.let { path ->
            val f = File(path)
            if (f.exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .semantics {
                onClick {
                    onTap()
                    true
                }
            }
            .pointerInput(project.id, isSelectionMode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var triggeredHold = false
                    val progressJob = scope.launch {
                        val totalMs = 500L
                        val stepMs = 40L
                        var elapsed = 0L
                        while (elapsed < totalMs) {
                            delay(stepMs)
                            elapsed += stepMs
                            holdProgress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                        }
                        triggeredHold = true
                        holdProgress = 0f
                        onHoldSelect()
                    }
                    val up = waitForUpOrCancellation()
                    progressJob.cancel()
                    holdProgress = 0f
                    if (up != null && !triggeredHold) {
                        onTap()
                    }
                }
            }
            .testTag("history_project_card_$number"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                StudioAzure.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (holdProgress > 0f) {
                LinearProgressIndicator(
                    progress = { holdProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = StudioAzure
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Select",
                        tint = if (isSelected) StudioAzure else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    // Sequential Numbering Badge (1, 2, 3... Oldest to Newest)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(StudioAzure.copy(alpha = 0.18f))
                            .border(1.dp, StudioAzure.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = number.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StudioAzure
                        )
                    }
                }

                // Direct Thumbnail
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cell = 8.dp.toPx()
                        val cols = (size.width / cell).toInt() + 1
                        val rows = (size.height / cell).toInt() + 1
                        for (r in 0..rows) {
                            for (c in 0..cols) {
                                drawRect(
                                    color = if ((r + c) % 2 == 0) CheckerDark1 else CheckerDark2,
                                    topLeft = Offset(c * cell, r * cell),
                                    size = Size(cell, cell)
                                )
                            }
                        }
                    }
                    if (thumbBitmap != null) {
                        Image(
                            bitmap = thumbBitmap,
                            contentDescription = project.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Title + Ratio + Date/Time
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = project.aspectLabel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${project.layerCount} layers",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSelectionMode) {
                    if (isSelected) {
                        IconButton(
                            onClick = onDeleteAllSelected,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("history_delete_selected_button_$number")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete Selected",
                                tint = StudioCoral
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("history_save_button_$number")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = "Save Project",
                            tint = StudioEmerald
                        )
                    }
                }
            }
        }
    }
}

