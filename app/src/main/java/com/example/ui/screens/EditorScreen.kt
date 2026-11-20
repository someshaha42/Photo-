package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TextLayer
import com.example.model.VideoLayer
import com.example.ui.EditorBottomTab
import com.example.ui.ExportFeedbackState
import com.example.ui.StudioViewModel
import com.example.ui.components.BackgroundSettingsPanel
import com.example.ui.components.CanvasSettingsPanel
import com.example.ui.components.EditorCanvasViewport
import com.example.ui.components.LayersAndAiPanel
import com.example.ui.components.MediaAndVideoPanel
import com.example.ui.components.TextEditorPanel
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioTeal
import com.example.video.VideoExportState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    viewModel: StudioViewModel,
    onNavigateBack: () -> Unit,
    onPickImageDirect: () -> Unit,
    onPickVideoDirect: () -> Unit,
    onPickBackgroundImageDirect: () -> Unit,
    onOpenFontLibrary: () -> Unit,
    onOpenAiImage: () -> Unit,
    onOpenAiChat: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val document by viewModel.document.collectAsStateWithLifecycle()
    val imageBitmaps by viewModel.imageBitmaps.collectAsStateWithLifecycle()
    val blurredPreview by viewModel.blurredBgPreview.collectAsStateWithLifecycle()
    val activeBottomTab by viewModel.activeBottomTab.collectAsStateWithLifecycle()
    val canvasZoom by viewModel.canvasZoom.collectAsStateWithLifecycle()
    val showGrid by viewModel.showGrid.collectAsStateWithLifecycle()
    val isEyedropperActive by viewModel.isEyedropperActive.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val exportFeedback by viewModel.exportFeedback.collectAsStateWithLifecycle()
    val videoExportState by viewModel.videoExportState.collectAsStateWithLifecycle()
    val fonts by viewModel.fonts.collectAsStateWithLifecycle()
    val selectedFontId by viewModel.selectedFontId.collectAsStateWithLifecycle()

    var showZoomMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(true) }

    BackHandler {
        viewModel.saveProjectToHistory(silent = true)
        onNavigateBack()
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    val selectedLayer = document.layers.firstOrNull { it.id == document.selectedLayerId }
    val selectedTextLayer = selectedLayer as? TextLayer
    val selectedVideoLayer = (selectedLayer as? VideoLayer)
        ?: document.layers.filterIsInstance<VideoLayer>().firstOrNull()
    val activeFont = fonts.firstOrNull { it.id == (selectedTextLayer?.fontId ?: selectedFontId) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // PixelLab-Inspired Dual Top Bar
            Surface(
                color = StudioAzure,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                viewModel.saveProjectToHistory(silent = true)
                                onNavigateBack()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Home")
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "PixelForge",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = canUndo,
                            modifier = Modifier.testTag("undo_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.35f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = canRedo,
                            modifier = Modifier.testTag("redo_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.35f)
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { showZoomMenu = true },
                                modifier = Modifier.testTag("zoom_button")
                            ) {
                                Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom (${(canvasZoom * 100).roundToInt()}%)")
                            }
                            DropdownMenu(
                                expanded = showZoomMenu,
                                onDismissRequest = { showZoomMenu = false }
                            ) {
                                listOf(1f, 2f, 4f, 8f).forEach { z ->
                                    DropdownMenuItem(
                                        text = { Text("Zoom ${(z * 100).roundToInt()}%") },
                                        onClick = {
                                            viewModel.setCanvasZoom(z)
                                            showZoomMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { viewModel.toggleGrid() },
                            modifier = Modifier.testTag("grid_toggle_button")
                        ) {
                            Icon(
                                Icons.Rounded.GridOn,
                                contentDescription = "Toggle Grid",
                                tint = if (showGrid) StudioAmber else Color.White
                            )
                        }
                        IconButton(
                            onClick = { viewModel.saveProjectToHistory(silent = false) },
                            modifier = Modifier.testTag("save_project_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = "Save to History")
                        }
                        IconButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.testTag("export_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = "Export")
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    val tabs = listOf(
                        Triple(EditorBottomTab.CANVAS, Icons.Rounded.AspectRatio, "Canvas"),
                        Triple(EditorBottomTab.TEXT, Icons.Rounded.Title, "Text"),
                        Triple(EditorBottomTab.MEDIA, Icons.Rounded.PhotoLibrary, "Media"),
                        Triple(EditorBottomTab.BACKGROUND, Icons.Rounded.Palette, "Background"),
                        Triple(EditorBottomTab.LAYERS, Icons.Rounded.Layers, "Layers")
                    )
                    tabs.forEach { (tab, icon, label) ->
                        NavigationBarItem(
                            selected = activeBottomTab == tab,
                            onClick = { viewModel.setBottomTab(tab) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, maxLines = 1) },
                            modifier = Modifier.testTag("editor_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Live Editing Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                EditorCanvasViewport(
                    document = document,
                    imageBitmaps = imageBitmaps,
                    blurredPreview = blurredPreview,
                    fontRenderEngine = viewModel.fontRenderEngine,
                    canvasZoom = canvasZoom,
                    showGrid = showGrid,
                    isEyedropperActive = isEyedropperActive,
                    isVideoPlaying = isVideoPlaying,
                    onSelectLayer = { viewModel.selectLayer(it) },
                    onTransformLayer = { id, dx, dy, sc, rot ->
                        viewModel.transformLayerTransient(id, dx, dy, sc, rot)
                    },
                    onEyedropperTap = { nx, ny ->
                        scope.launch {
                            val bmp = viewModel.mediaManager.renderProjectToBitmap(
                                document = document,
                                fontRenderEngine = viewModel.fontRenderEngine,
                                targetWidth = 360,
                                targetHeight = (360 / document.aspectRatio.ratioFloat).roundToInt().coerceAtLeast(180)
                            )
                            val px = (nx * (bmp.width - 1)).roundToInt().coerceIn(0, bmp.width - 1)
                            val py = (ny * (bmp.height - 1)).roundToInt().coerceIn(0, bmp.height - 1)
                            val sampled = bmp.getPixel(px, py)
                            bmp.recycle()
                            if (android.graphics.Color.alpha(sampled) > 0) {
                                viewModel.updateBackground { bg ->
                                    bg.copy(colorArgb = sampled.toLong() and 0xFFFFFFFFL)
                                }
                            }
                            viewModel.setEyedropperActive(false)
                        }
                    }
                )

                // Zoom Indicator Chip when zoomed > 100%
                if (canvasZoom > 1.05f) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clickable { viewModel.setCanvasZoom(1f) }
                    ) {
                        Text(
                            text = "${(canvasZoom * 100).roundToInt()}% (Tap reset)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 2. Direct Quick-Action Bar: Image | Video | Text | Font (+ AI Art / AI Chat)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickActionToolButton(
                        icon = Icons.Rounded.Image,
                        label = "Image",
                        tint = StudioAzure,
                        tag = "quick_image_button",
                        onClick = onPickImageDirect
                    )
                    QuickActionToolButton(
                        icon = Icons.Rounded.Movie,
                        label = "Video",
                        tint = StudioTeal,
                        tag = "quick_video_button",
                        onClick = onPickVideoDirect
                    )
                    QuickActionToolButton(
                        icon = Icons.Rounded.TextFields,
                        label = "Text",
                        tint = StudioAmber,
                        tag = "quick_text_button",
                        onClick = { viewModel.addTextLayer() }
                    )
                    QuickActionToolButton(
                        icon = Icons.Rounded.FontDownload,
                        label = "Font",
                        tint = MaterialTheme.colorScheme.onSurface,
                        tag = "quick_font_button",
                        onClick = onOpenFontLibrary
                    )
                    QuickActionToolButton(
                        icon = Icons.Rounded.AutoAwesome,
                        label = "AI Image",
                        tint = StudioPurple,
                        tag = "quick_ai_image_button",
                        onClick = onOpenAiImage
                    )
                    QuickActionToolButton(
                        icon = Icons.Rounded.Chat,
                        label = "AI Chat",
                        tint = StudioAzure,
                        tag = "quick_ai_chat_button",
                        onClick = onOpenAiChat
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            // 3. Active Tool Panel (Compact & Scrollable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 175.dp, max = 310.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                when (activeBottomTab) {
                    EditorBottomTab.CANVAS -> {
                        CanvasSettingsPanel(
                            selectedRatio = document.aspectRatio,
                            autoImageDetectionEnabled = document.autoImageDetectionEnabled,
                            manualOverride = document.manualAspectOverride,
                            onSelectRatio = { viewModel.selectAspectRatioManually(it) },
                            onToggleAutoDetection = { viewModel.toggleAutoImageDetection() }
                        )
                    }
                    EditorBottomTab.TEXT -> {
                        TextEditorPanel(
                            selectedTextLayer = selectedTextLayer,
                            activeFont = activeFont,
                            onAddTextLayer = { viewModel.addTextLayer(it) },
                            onOpenFontLibrary = onOpenFontLibrary,
                            onUpdateTextLayer = { viewModel.updateSelectedTextLayer(it) }
                        )
                    }
                    EditorBottomTab.MEDIA -> {
                        MediaAndVideoPanel(
                            selectedVideoLayer = selectedVideoLayer,
                            isVideoPlaying = isVideoPlaying,
                            videoExportState = videoExportState,
                            onPickImage = onPickImageDirect,
                            onPickVideo = onPickVideoDirect,
                            onToggleVideoPlay = { isVideoPlaying = !isVideoPlaying },
                            onAddShape = { viewModel.addShapeLayer(it) },
                            onUpdateVideoLayer = { viewModel.updateSelectedVideoLayer(it) },
                            onExportVideo = { viewModel.exportActiveVideo() },
                            onCancelVideoExport = { viewModel.videoEditorEngine.cancelExport() },
                            onOpenAiImage = onOpenAiImage
                        )
                    }
                    EditorBottomTab.BACKGROUND -> {
                        BackgroundSettingsPanel(
                            background = document.background,
                            blurredPreview = blurredPreview,
                            selectedBgImageBitmap = document.background.backgroundImageUri?.let { imageBitmaps[it] },
                            isEyedropperActive = isEyedropperActive,
                            onUpdateBackground = { viewModel.updateBackground(it) },
                            onPickBackgroundImage = onPickBackgroundImageDirect,
                            onToggleEyedropper = { viewModel.setEyedropperActive(!isEyedropperActive) }
                        )
                    }
                    EditorBottomTab.LAYERS -> {
                        LayersAndAiPanel(
                            layers = document.layers,
                            selectedLayerId = document.selectedLayerId,
                            onSelectLayer = { viewModel.selectLayer(it) },
                            onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                            onToggleLock = { viewModel.toggleLayerLock(it) },
                            onMoveLayer = { id, dir -> viewModel.moveLayerOrder(id, dir) },
                            onDuplicateLayer = { viewModel.duplicateLayer(it) },
                            onDeleteLayer = { viewModel.deleteLayer(it) },
                            onOpenAiChat = onOpenAiChat,
                            onOpenAiImage = onOpenAiImage
                        )
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearExportFeedback()
                showExportDialog = false
            },
            title = { Text("Export Project", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Canvas: ${document.aspectRatio.label} (${document.aspectRatio.exportWidth}×${document.aspectRatio.exportHeight}px RGBA8888)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Checkerboard preview is never exported. Transparent areas keep full 8-bit alpha in PNG.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    when (val fb = exportFeedback) {
                        ExportFeedbackState.Idle -> Unit
                        ExportFeedbackState.Exporting -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Text("Rendering high-res RGBA8888 PNG...")
                            }
                        }
                        is ExportFeedbackState.Success -> {
                            Text(fb.message, color = StudioTeal, fontWeight = FontWeight.SemiBold)
                            OutlinedButton(
                                onClick = {
                                    val shareUri = viewModel.mediaManager.getShareUri(fb.file)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share PNG"))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Share Exported PNG")
                            }
                        }
                        is ExportFeedbackState.Error -> {
                            Text(fb.message, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    when (val vfb = videoExportState) {
                        VideoExportState.Idle -> Unit
                        is VideoExportState.Exporting -> Text("Video exporting: ${vfb.progressPercent}%")
                        is VideoExportState.Completed -> {
                            Text("MP4 Video exported successfully!", color = StudioTeal, fontWeight = FontWeight.SemiBold)
                            OutlinedButton(
                                onClick = {
                                    val shareUri = viewModel.mediaManager.getShareUri(vfb.outputFile)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Share Exported MP4")
                            }
                        }
                        is VideoExportState.Error -> Text(vfb.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.exportProjectPng() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export RGBA8888 PNG")
                    }
                    if (selectedVideoLayer != null) {
                        OutlinedButton(
                            onClick = { viewModel.exportActiveVideo() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export MP4 Video")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.clearExportFeedback()
                        showExportDialog = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun QuickActionToolButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
