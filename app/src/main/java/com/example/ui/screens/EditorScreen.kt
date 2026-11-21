package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DrawLayer
import com.example.model.GroupLayer
import com.example.model.ImageLayer
import com.example.model.LayerShapeType
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import com.example.ui.ExportFeedbackState
import com.example.ui.EyedropperTarget
import com.example.ui.StudioViewModel
import com.example.ui.ToolDrawerTab
import com.example.ui.components.AdjustToolPanel
import com.example.ui.components.BackgroundSettingsPanel
import com.example.ui.components.CanvasSettingsPanel
import com.example.ui.components.ColorToolPanel
import com.example.ui.components.CornerPinToolPanel
import com.example.ui.components.CropToolPanel
import com.example.ui.components.DrawToolPanel
import com.example.ui.components.EditorCanvasViewport
import com.example.ui.components.EmojiSettingsPanel
import com.example.ui.components.FloatingEyedropperOverlay
import com.example.ui.components.FloatingThreeDControllerPanel
import com.example.ui.components.FontBrowserToolPanel
import com.example.ui.components.HorizontalLayerStrip
import com.example.ui.components.LayersSideDrawer
import com.example.ui.components.MyProjectsSideDrawer
import com.example.ui.components.PendingPhotoCropOverlay
import com.example.ui.components.RotateTransformPanel
import com.example.ui.components.SaveExportDialog
import com.example.ui.components.ShapesToolPanel
import com.example.ui.components.TextToolPanel
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class ThreeDotSheetSection(val title: String) {
    NONE(""),
    CANVAS("Canvas"),
    BACKGROUND("Background"),
    EMOJI("Emoji")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: StudioViewModel,
    onNavigateBack: () -> Unit,
    onPickPhotosDirect: () -> Unit,
    onPickBackgroundImageDirect: () -> Unit,
    onOpenFontLibrary: () -> Unit,
    onImportFontsClick: () -> Unit = onOpenFontLibrary,
    onImportArchiveClick: () -> Unit = onOpenFontLibrary
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val document by viewModel.document.collectAsStateWithLifecycle()
    val imageBitmaps by viewModel.imageBitmaps.collectAsStateWithLifecycle()
    val blurredPreview by viewModel.blurredBgPreview.collectAsStateWithLifecycle()
    val activeToolDrawer by viewModel.activeToolDrawer.collectAsStateWithLifecycle()
    val showLayersDrawer by viewModel.showLayersDrawer.collectAsStateWithLifecycle()
    val showProjectsDrawer by viewModel.showProjectsDrawer.collectAsStateWithLifecycle()
    val projectHistory by viewModel.projectHistory.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val draftText by viewModel.draftText.collectAsStateWithLifecycle()
    val draftTextStyle by viewModel.draftTextStyle.collectAsStateWithLifecycle()
    val isZoomMode by viewModel.isZoomMode.collectAsStateWithLifecycle()
    val canvasZoom by viewModel.canvasZoom.collectAsStateWithLifecycle()
    val canvasPan by viewModel.canvasPan.collectAsStateWithLifecycle()
    val isEyedropperActive by viewModel.isEyedropperActive.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val exportFeedback by viewModel.exportFeedback.collectAsStateWithLifecycle()
    val fonts by viewModel.fonts.collectAsStateWithLifecycle()
    val fontsRecentToOld by viewModel.fontsRecentToOld.collectAsStateWithLifecycle()
    val selectedFontId by viewModel.selectedFontId.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val drawColorArgb by viewModel.drawColorArgb.collectAsStateWithLifecycle()
    val drawBrushSizeNorm by viewModel.drawBrushSizeNorm.collectAsStateWithLifecycle()
    val drawStrokeStyle by viewModel.drawStrokeStyle.collectAsStateWithLifecycle()
    val showStrokeControls by viewModel.showStrokeControls.collectAsStateWithLifecycle()
    val shapeEditPointsMode by viewModel.shapeEditPointsMode.collectAsStateWithLifecycle()
    val transformLocks by viewModel.transformLocks.collectAsStateWithLifecycle()
    val pendingCropQueue by viewModel.pendingCropQueue.collectAsStateWithLifecycle()

    var showAddMenu by remember { mutableStateOf(false) }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var activeThreeDotSection by remember { mutableStateOf(ThreeDotSheetSection.NONE) }
    var showExportDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    // Multi-stage Android Back behavior: never quit app directly when panels/options are open
    BackHandler {
        when {
            isEyedropperActive -> {
                viewModel.setEyedropperActive(false)
            }
            showExportDialog -> {
                viewModel.clearExportFeedback()
                showExportDialog = false
            }
            activeThreeDotSection != ThreeDotSheetSection.NONE -> {
                activeThreeDotSection = ThreeDotSheetSection.NONE
            }
            showThreeDotMenu -> showThreeDotMenu = false
            showAddMenu -> showAddMenu = false
            showProjectsDrawer -> {
                viewModel.setShowProjectsDrawer(false)
            }
            showLayersDrawer -> {
                viewModel.setShowLayersDrawer(false)
            }
            activeToolDrawer != ToolDrawerTab.NONE -> {
                viewModel.closeToolDrawer()
            }
            isZoomMode -> {
                viewModel.setZoomMode(false)
            }
            document.selectedLayerId != null -> {
                viewModel.selectLayer(null)
            }
            else -> {
                viewModel.saveProjectToHistory(silent = true)
                onNavigateBack()
            }
        }
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    LaunchedEffect(exportFeedback) {
        val fb = exportFeedback
        if (fb is ExportFeedbackState.Success && fb.shareImmediately) {
            val shareUri = viewModel.mediaManager.getShareUri(fb.file)
            val mime = if (fb.file.extension.equals("jpg", true)) "image/jpeg" else "image/png"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share"))
            viewModel.clearExportFeedback()
        }
    }

    val selectedLayer = StudioViewModel.findLayerRecursive(document.layers, document.selectedLayerId)
    val selectedTextLayer = selectedLayer as? TextLayer
    val selectedImageLayer = (selectedLayer as? ImageLayer)
        ?: document.layers.filterIsInstance<ImageLayer>().lastOrNull()
    val selectedShapeLayer = selectedLayer as? ShapeLayer
    val activeFont = fonts.firstOrNull { it.id == (selectedTextLayer?.fontId ?: selectedFontId) }
    val hasDrawContent = (selectedLayer as? DrawLayer)?.strokes?.isNotEmpty() == true ||
        document.layers.filterIsInstance<DrawLayer>().any { it.strokes.isNotEmpty() }
    val selectedDrawLayerCount = remember(document.layers, document.selectedLayerId, document.selectionOrder) {
        viewModel.selectedDrawLayerIds().size
    }
    val isClearTypingMode = isImeVisible && activeToolDrawer == ToolDrawerTab.TEXT

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = StudioAzure,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    // Row 1: Add | Undo | Redo | Save | Share | 3-Dot Menu (⋮)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box {
                            TopActionItem(
                                icon = Icons.Rounded.Add,
                                label = "Add",
                                tag = "top_add_button",
                                onClick = { showAddMenu = true }
                            )
                            DropdownMenu(
                                expanded = showAddMenu,
                                onDismissRequest = { showAddMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Photo") },
                                    leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null) },
                                    onClick = {
                                        showAddMenu = false
                                        onPickPhotosDirect()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Text") },
                                    leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
                                    onClick = {
                                        showAddMenu = false
                                        viewModel.startNewTextDraft()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Shapes") },
                                    leadingIcon = { Icon(Icons.Rounded.Category, contentDescription = null) },
                                    onClick = {
                                        showAddMenu = false
                                        viewModel.addShapeLayer(LayerShapeType.ROUNDED_RECT)
                                        viewModel.openToolDrawer(ToolDrawerTab.SHAPES)
                                    }
                                )
                            }
                        }

                        TopActionItem(
                            icon = Icons.AutoMirrored.Rounded.Undo,
                            label = "Undo",
                            enabled = canUndo,
                            tag = "undo_button",
                            onClick = { viewModel.undo() }
                        )
                        TopActionItem(
                            icon = Icons.AutoMirrored.Rounded.Redo,
                            label = "Redo",
                            enabled = canRedo,
                            tag = "redo_button",
                            onClick = { viewModel.redo() }
                        )
                        TopActionItem(
                            icon = Icons.Rounded.Save,
                            label = "Save",
                            tag = "save_project_button",
                            onClick = {
                                viewModel.clearExportFeedback()
                                showExportDialog = true
                            }
                        )
                        TopActionItem(
                            icon = Icons.Rounded.Share,
                            label = "Share",
                            tag = "share_project_button",
                            onClick = {
                                viewModel.exportProjectPng(shareImmediately = true)
                            }
                        )

                        Box {
                            TopActionItem(
                                icon = Icons.Rounded.MoreVert,
                                label = "Menu",
                                tag = "three_dot_menu_button",
                                onClick = { showThreeDotMenu = true }
                            )
                            DropdownMenu(
                                expanded = showThreeDotMenu,
                                onDismissRequest = { showThreeDotMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("1. Canvas") },
                                    leadingIcon = { Icon(Icons.Rounded.AspectRatio, contentDescription = null) },
                                    onClick = {
                                        showThreeDotMenu = false
                                        activeThreeDotSection = ThreeDotSheetSection.CANVAS
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("2. Background") },
                                    leadingIcon = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                                    onClick = {
                                        showThreeDotMenu = false
                                        activeThreeDotSection = ThreeDotSheetSection.BACKGROUND
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("3. Emoji") },
                                    leadingIcon = { Icon(Icons.Rounded.EmojiEmotions, contentDescription = null) },
                                    onClick = {
                                        showThreeDotMenu = false
                                        activeThreeDotSection = ThreeDotSheetSection.EMOJI
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("4. My Projects") },
                                    leadingIcon = { Icon(Icons.Rounded.FolderCopy, contentDescription = null) },
                                    modifier = Modifier.testTag("menu_my_projects"),
                                    onClick = {
                                        showThreeDotMenu = false
                                        viewModel.setShowProjectsDrawer(true)
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.18f))

                    // Row 2: Unwrapped Top Action Bar — Delete | Corner Pin | 3D | S | Z | R | P Locks
                    // Automatically unwraps into 2 balanced rows on wide/landscape canvas ratios (e.g., 16:9)
                    // or compact single row on portrait/square screens (no hidden HorizontalPager swipe required)
                    val hasSelection = selectedLayer != null
                    val useTwoRowTopControls = document.canvas.ratioFloat > 1.15f

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("top_controls_row2")
                    ) {
                        if (useTwoRowTopControls || maxWidth >= 520.dp) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TopActionItem(
                                        icon = Icons.Rounded.DeleteOutline,
                                        label = "Delete",
                                        enabled = hasSelection,
                                        tag = "top_delete_button",
                                        onClick = { selectedLayer?.let { viewModel.deleteLayer(it.id) } }
                                    )
                                    TopActionItem(
                                        icon = Icons.Rounded.Transform,
                                        label = "Corner Pin",
                                        enabled = hasSelection,
                                        highlighted = activeToolDrawer == ToolDrawerTab.CORNER_PIN,
                                        highlightColor = StudioAmber,
                                        tag = "top_tool_corner_pin",
                                        onClick = { viewModel.openToolDrawer(ToolDrawerTab.CORNER_PIN) }
                                    )
                                    TopActionItem(
                                        icon = Icons.Rounded.ViewInAr,
                                        label = "3D",
                                        enabled = hasSelection,
                                        highlighted = activeToolDrawer == ToolDrawerTab.THREE_D,
                                        highlightColor = StudioAmber,
                                        tag = "top_tool_3d",
                                        onClick = { viewModel.openToolDrawer(ToolDrawerTab.THREE_D) }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TransformLockTopItem(
                                        shortCode = "S",
                                        label = "Stretch",
                                        isLocked = transformLocks.stretchLocked,
                                        tag = "top_lock_stretch_button",
                                        onClick = { viewModel.toggleStretchLock() }
                                    )
                                    TransformLockTopItem(
                                        shortCode = "Z",
                                        label = "Zoom",
                                        isLocked = transformLocks.zoomLocked,
                                        tag = "top_lock_zoom_button",
                                        onClick = { viewModel.toggleZoomLock() }
                                    )
                                    TransformLockTopItem(
                                        shortCode = "R",
                                        label = "Rotate",
                                        isLocked = transformLocks.rotationLocked,
                                        tag = "top_lock_rotation_button",
                                        onClick = { viewModel.toggleRotationLock() }
                                    )
                                    TransformLockTopItem(
                                        shortCode = "P",
                                        label = "Move",
                                        isLocked = transformLocks.positionLocked,
                                        tag = "top_lock_position_button",
                                        onClick = { viewModel.togglePositionLock() }
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (maxWidth < 340.dp) Modifier.horizontalScroll(rememberScrollState())
                                        else Modifier
                                    )
                                    .padding(horizontal = 2.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TopActionItem(
                                    icon = Icons.Rounded.DeleteOutline,
                                    label = "Delete",
                                    enabled = hasSelection,
                                    tag = "top_delete_button",
                                    compact = true,
                                    onClick = { selectedLayer?.let { viewModel.deleteLayer(it.id) } }
                                )
                                TopActionItem(
                                    icon = Icons.Rounded.Transform,
                                    label = "Pin",
                                    enabled = hasSelection,
                                    highlighted = activeToolDrawer == ToolDrawerTab.CORNER_PIN,
                                    highlightColor = StudioAmber,
                                    tag = "top_tool_corner_pin",
                                    compact = true,
                                    onClick = { viewModel.openToolDrawer(ToolDrawerTab.CORNER_PIN) }
                                )
                                TopActionItem(
                                    icon = Icons.Rounded.ViewInAr,
                                    label = "3D",
                                    enabled = hasSelection,
                                    highlighted = activeToolDrawer == ToolDrawerTab.THREE_D,
                                    highlightColor = StudioAmber,
                                    tag = "top_tool_3d",
                                    compact = true,
                                    onClick = { viewModel.openToolDrawer(ToolDrawerTab.THREE_D) }
                                )
                                TransformLockTopItem(
                                    shortCode = "S",
                                    label = "Stretch",
                                    isLocked = transformLocks.stretchLocked,
                                    compact = true,
                                    tag = "top_lock_stretch_button",
                                    onClick = { viewModel.toggleStretchLock() }
                                )
                                TransformLockTopItem(
                                    shortCode = "Z",
                                    label = "Zoom",
                                    isLocked = transformLocks.zoomLocked,
                                    compact = true,
                                    tag = "top_lock_zoom_button",
                                    onClick = { viewModel.toggleZoomLock() }
                                )
                                TransformLockTopItem(
                                    shortCode = "R",
                                    label = "Rotate",
                                    isLocked = transformLocks.rotationLocked,
                                    compact = true,
                                    tag = "top_lock_rotation_button",
                                    onClick = { viewModel.toggleRotationLock() }
                                )
                                TransformLockTopItem(
                                    shortCode = "P",
                                    label = "Move",
                                    isLocked = transformLocks.positionLocked,
                                    compact = true,
                                    tag = "top_lock_position_button",
                                    onClick = { viewModel.togglePositionLock() }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val availW = maxWidth
            val availH = maxHeight
            val canvasRatio = document.canvas.ratioFloat.coerceAtLeast(0.1f)
            val estimatedCanvasH = ((availW - 16.dp).coerceAtLeast(120.dp)) / canvasRatio
            val spareVerticalDp = availH - estimatedCanvasH
            val isSpaciousLandscapeCanvas = canvasRatio > 1.15f && spareVerticalDp > 220.dp
            val maxSafeDrawerHeight = (availH - 150.dp).coerceAtLeast(160.dp)
            val toolDrawerMaxHeight = when {
                isClearTypingMode -> 140.dp
                isSpaciousLandscapeCanvas -> (spareVerticalDp - 88.dp).coerceIn(280.dp, 460.dp)
                showStrokeControls || activeToolDrawer == ToolDrawerTab.SHAPES -> 340.dp
                else -> 265.dp
            }.coerceAtMost(maxSafeDrawerHeight)

            Column(modifier = Modifier.fillMaxSize()) {
                if (isSpaciousLandscapeCanvas) {
                    Spacer(modifier = Modifier.weight(0.28f))
                }

                // Canvas Viewport + Upper-Right Vertical Floating Zoom & Layers
                Box(
                    modifier = if (isSpaciousLandscapeCanvas) {
                        Modifier
                            .fillMaxWidth()
                            .height(estimatedCanvasH.coerceIn(140.dp, availH * 0.56f))
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    EditorCanvasViewport(
                        document = document,
                        imageBitmaps = imageBitmaps,
                        blurredPreview = blurredPreview,
                        fontRenderEngine = viewModel.fontRenderEngine,
                        isZoomMode = isZoomMode,
                        isCropMode = activeToolDrawer == ToolDrawerTab.CROP,
                        isDrawMode = activeToolDrawer == ToolDrawerTab.DRAW,
                        isCornerPinMode = activeToolDrawer == ToolDrawerTab.CORNER_PIN,
                        isShapeEditPointsMode = shapeEditPointsMode,
                        drawColorArgb = drawColorArgb,
                        drawBrushSizeNorm = drawBrushSizeNorm,
                        drawStrokeStyle = drawStrokeStyle,
                        transformLocks = transformLocks,
                        canvasZoom = canvasZoom,
                        canvasPan = canvasPan,
                        isEyedropperActive = isEyedropperActive,
                        onCanvasViewportTransform = { zoomDelta, dx, dy ->
                            viewModel.updateCanvasViewportTransform(zoomDelta, dx, dy)
                        },
                        onSelectLayer = { viewModel.selectLayer(it) },
                        onLayerTapped = { viewModel.onCanvasLayerTapped(it) },
                        onEmptyCanvasTap = { viewModel.dismissAllOverlaysAndSelection() },
                        onDeleteLayer = { viewModel.deleteLayer(it) },
                        onTransformLayer = { id, dx, dy, sc, rot, sx, sy ->
                            viewModel.transformLayerTransient(id, dx, dy, sc, rot, sx, sy)
                        },
                        onAnchoredStretch = { id, sx, sy, sdx, sdy, bw, bh, cw, ch ->
                            viewModel.transformLayerAnchoredStretch(id, sx, sy, sdx, sdy, bw, bh, cw, ch)
                        },
                        onCornerPinDrag = { id, cornerIndex, dxRatio, dyRatio ->
                            viewModel.updateSelectedLayerCornerPinTransient(id, cornerIndex, dxRatio, dyRatio)
                        },
                        onShapeHandleDrag = { id, handleId, dxPx, dyPx, basePx ->
                            viewModel.applyShapeVectorHandleDragTransient(id, handleId, dxPx, dyPx, basePx)
                        },
                        onBeginTransformTransaction = { viewModel.beginTransformTransaction() },
                        onEndTransformTransaction = { viewModel.endTransformTransaction() },
                        onUpdateCropTransient = { viewModel.updateSelectedImageCropTransient(it) },
                        onCommitCrop = { viewModel.updateSelectedImageCrop(it) },
                        onAddDrawStroke = { viewModel.addDrawStroke(it) },
                        onEyedropperTap = { nx, ny ->
                            scope.launch {
                                val bmp = viewModel.mediaManager.renderProjectToBitmap(
                                    document = document,
                                    fontRenderEngine = viewModel.fontRenderEngine,
                                    targetWidth = 360,
                                    targetHeight = (360 / document.canvas.ratioFloat).roundToInt().coerceAtLeast(180)
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

                    // Upper-Right Vertical Stack inside Canvas: Zoom on top, Layers directly below
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        FloatingCanvasCornerButton(
                            icon = Icons.Rounded.ZoomIn,
                            label = "Zoom",
                            highlighted = isZoomMode,
                            tag = "top_zoom_button",
                            onClick = { viewModel.toggleZoomMode() }
                        )
                        FloatingCanvasCornerButton(
                            icon = Icons.Rounded.Layers,
                            label = "Layers",
                            highlighted = showLayersDrawer,
                            tag = "top_layers_button",
                            onClick = { viewModel.toggleLayersDrawer() }
                        )
                    }

                    if (canvasZoom != 1f || canvasPan.first != 0f || canvasPan.second != 0f) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 12.dp, end = 72.dp)
                                .clickable { viewModel.resetCanvasViewport() }
                        ) {
                            Text(
                                text = "${(canvasZoom * 100).roundToInt()}% ✕",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (isSpaciousLandscapeCanvas) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Tool Drawer + Always-Visible Layer Strip + Single-Row Bottom Tool Bar
                // Hugs tightly below wide/16:9 canvases or sits cleanly at the bottom on 1:1 / 9:16
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = activeToolDrawer != ToolDrawerTab.NONE && activeToolDrawer != ToolDrawerTab.THREE_D,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        val dragThresholdPx = with(LocalDensity.current) { 42.dp.toPx() }
                        var dragSum by remember { mutableFloatStateOf(0f) }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tool_drawer_container")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = toolDrawerMaxHeight)
                            ) {
                                AnimatedVisibility(visible = !isClearTypingMode) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(Unit) {
                                                detectVerticalDragGestures(
                                                    onDragStart = { dragSum = 0f },
                                                    onVerticalDrag = { _, dragAmount ->
                                                        dragSum += dragAmount
                                                        if (dragSum > dragThresholdPx) {
                                                            viewModel.closeToolDrawer()
                                                            dragSum = 0f
                                                        }
                                                    }
                                                )
                                            }
                                            .clickable { viewModel.closeToolDrawer() }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 40.dp, height = 4.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(MaterialTheme.colorScheme.outline)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(bottom = if (isClearTypingMode) 2.dp else 8.dp)
                                ) {
                                    when (activeToolDrawer) {
                                        ToolDrawerTab.CROP -> {
                                            CropToolPanel(
                                                selectedImageLayer = selectedImageLayer,
                                                onDone = { viewModel.closeToolDrawer() }
                                            )
                                        }
                                        ToolDrawerTab.COLOR -> {
                                            ColorToolPanel(
                                                selectedLayer = selectedLayer,
                                                onSetColorTransient = { viewModel.setSelectedLayerColorTransient(it) },
                                                onCommitColor = { viewModel.applyColorToSelectedLayer(it) },
                                                onLaunchFloatingEyedropper = {
                                                    viewModel.startEyedropper(EyedropperTarget.SELECTED_LAYER)
                                                }
                                            )
                                        }
                                        ToolDrawerTab.FONT -> {
                                            FontBrowserToolPanel(
                                                selectedTextLayer = selectedTextLayer,
                                                draftStyle = draftTextStyle,
                                                fonts = fonts,
                                                fontsRecentToOld = fontsRecentToOld,
                                                selectedFontId = selectedFontId,
                                                renderEngine = viewModel.fontRenderEngine,
                                                onSelectFont = { viewModel.applyFontGloballyAndToSelection(it) },
                                                onUpdateTextSize = { sz ->
                                                    viewModel.updateSelectedTextLayerTransient { it.copy(fontSizeSp = sz) }
                                                },
                                                onUpdateTextSizeFinished = {
                                                    viewModel.endTransformTransaction()
                                                },
                                                onImportFontsClick = onImportFontsClick,
                                                onImportArchiveClick = onImportArchiveClick,
                                                onOpenFullFontLibrary = onOpenFontLibrary
                                            )
                                        }
                                        ToolDrawerTab.PHOTO -> Unit
                                        ToolDrawerTab.TEXT -> {
                                            TextToolPanel(
                                                selectedTextLayer = selectedTextLayer,
                                                draftText = draftText,
                                                draftStyle = draftTextStyle,
                                                activeFont = activeFont,
                                                onTextChangeLive = { viewModel.updateSelectedTextTransient(it) },
                                                onConfirmText = { viewModel.confirmTextBox() },
                                                onCycleTextCase = { viewModel.cycleTextCase() },
                                                onOpenFontLibrary = onOpenFontLibrary,
                                                onUpdateTextLayer = { viewModel.updateSelectedTextLayer(it) },
                                                onUpdateTextLayerTransient = { viewModel.updateSelectedTextLayerTransient(it) },
                                                onEndTransaction = { viewModel.endTransformTransaction() },
                                                isCompactImeMode = isClearTypingMode
                                            )
                                        }
                                        ToolDrawerTab.ADJUST -> {
                                            AdjustToolPanel(
                                                selectedImageLayer = selectedImageLayer,
                                                onUpdateAdjustments = { viewModel.updateSelectedImageAdjustments(it) },
                                                onSetImageOpacity = { viewModel.setSelectedImageOpacity(it) },
                                                onResetAdjustments = { viewModel.resetSelectedImageAdjustments() },
                                                onEndTransaction = { viewModel.endTransformTransaction() }
                                            )
                                        }
                                        ToolDrawerTab.SHAPES -> {
                                            ShapesToolPanel(
                                                selectedShapeLayer = selectedShapeLayer,
                                                isEditPointsMode = shapeEditPointsMode,
                                                onToggleEditPointsMode = { viewModel.toggleShapeEditPointsMode() },
                                                onAddShape = { viewModel.addShapeLayer(it) },
                                                onUpdateShape = { viewModel.updateSelectedShapeLayer(it) },
                                                onUpdateShapeTransient = { viewModel.updateSelectedShapeLayerTransient(it) },
                                                onEndTransaction = { viewModel.endTransformTransaction() }
                                            )
                                        }
                                        ToolDrawerTab.ROTATE -> {
                                            RotateTransformPanel(
                                                selectedLayer = selectedLayer,
                                                onRotateBy = { viewModel.rotateSelectedLayerBy(it) },
                                                onSetRotation = { viewModel.setSelectedLayerRotation(it) },
                                                onSetScale = { viewModel.setSelectedLayerScale(it) },
                                                onSetOpacity = { viewModel.setSelectedLayerOpacity(it) },
                                                onMoveOrder = { dir ->
                                                    selectedLayer?.let { viewModel.moveLayerOrder(it.id, dir) }
                                                },
                                                onDuplicate = {
                                                    selectedLayer?.let { viewModel.duplicateLayer(it.id) }
                                                },
                                                onEndTransaction = { viewModel.endTransformTransaction() }
                                            )
                                        }
                                        ToolDrawerTab.DRAW -> {
                                            DrawToolPanel(
                                                brushColorArgb = drawColorArgb,
                                                brushSizeNorm = drawBrushSizeNorm,
                                                strokeStyle = drawStrokeStyle,
                                                showStrokeControls = showStrokeControls,
                                                selectedDrawLayerCount = selectedDrawLayerCount,
                                                canUndoStroke = hasDrawContent,
                                                onSelectColor = { viewModel.setDrawColor(it) },
                                                onSetBrushSize = { viewModel.setDrawBrushSizeNorm(it) },
                                                onToggleStrokeControls = { viewModel.toggleStrokeControls() },
                                                onStartNewStrokeSession = { viewModel.startNewDrawLayerSession() },
                                                onUpdateStrokeStyleTransient = { viewModel.updateStrokeStyleTransient(it) },
                                                onUpdateStrokeStyleCommit = { viewModel.updateStrokeStyle(it) },
                                                onEndTransaction = { viewModel.endTransformTransaction() },
                                                onUndoStroke = { viewModel.undoLastDrawStroke() },
                                                onClearDraw = { viewModel.clearActiveDrawLayer() },
                                                onDone = { viewModel.closeToolDrawer() }
                                            )
                                        }
                                        ToolDrawerTab.CORNER_PIN -> {
                                            CornerPinToolPanel(
                                                canUndo = canUndo,
                                                onUndo = { viewModel.undo() },
                                                onConfirm = { viewModel.confirmCornerPin() }
                                            )
                                        }
                                        ToolDrawerTab.THREE_D, ToolDrawerTab.NONE -> Unit
                                    }
                                }
                            }
                        }
                    }

                    // Always-visible Horizontal Layer Strip whenever NO bottom tool option drawer is open
                    AnimatedVisibility(
                        visible = activeToolDrawer == ToolDrawerTab.NONE,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        HorizontalLayerStrip(
                            layers = document.layers,
                            selectedLayerId = document.selectedLayerId,
                            imageBitmaps = imageBitmaps,
                            onSelectLayer = { viewModel.selectLayer(it) },
                            onMoveLayerOrder = { id, dir -> viewModel.moveLayerOrder(id, dir) }
                        )
                    }

                    // Single-row Context-Sensitive Bottom Tool Bar (no Corner Pin or 3D here)
                    AnimatedVisibility(visible = !isClearTypingMode) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 4.dp
                            ) {
                                val isInTextMode = activeToolDrawer == ToolDrawerTab.TEXT || selectedLayer is TextLayer
                                val isPhotoSelected = selectedLayer is ImageLayer

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when {
                                        isInTextMode -> {
                                            BottomToolItem(
                                                icon = Icons.Rounded.Edit,
                                                label = "Edit",
                                                selected = activeToolDrawer == ToolDrawerTab.TEXT,
                                                tag = "bottom_tool_edit_text",
                                                onClick = { viewModel.openTextToolDrawer() }
                                            )
                                            BottomToolItem(
                                                icon = Icons.Rounded.Palette,
                                                label = "Color",
                                                selected = activeToolDrawer == ToolDrawerTab.COLOR,
                                                tag = "bottom_tool_color",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.COLOR) }
                                            )
                                            BottomToolItem(
                                                icon = Icons.Rounded.FontDownload,
                                                label = "Font",
                                                selected = activeToolDrawer == ToolDrawerTab.FONT,
                                                tag = "bottom_tool_font",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.FONT) }
                                            )
                                            BottomToolItem(
                                                icon = Icons.AutoMirrored.Rounded.RotateRight,
                                                label = "Rotate",
                                                selected = activeToolDrawer == ToolDrawerTab.ROTATE,
                                                tag = "bottom_tool_rotate",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.ROTATE) }
                                            )
                                        }
                                        isPhotoSelected -> {
                                            BottomToolItem(
                                                icon = Icons.Rounded.Crop,
                                                label = "Crop",
                                                selected = activeToolDrawer == ToolDrawerTab.CROP,
                                                tag = "bottom_tool_crop",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.CROP) }
                                            )
                                            BottomToolItem(
                                                icon = Icons.Rounded.Palette,
                                                label = "Color",
                                                selected = activeToolDrawer == ToolDrawerTab.COLOR,
                                                tag = "bottom_tool_color",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.COLOR) }
                                            )
                                            BottomToolItem(
                                                icon = Icons.Rounded.Tune,
                                                label = "Adjust",
                                                selected = activeToolDrawer == ToolDrawerTab.ADJUST,
                                                tag = "bottom_tool_adjust",
                                                onClick = {
                                                    viewModel.ensureImageLayerSelectedForAdjust()
                                                    viewModel.openToolDrawer(ToolDrawerTab.ADJUST)
                                                }
                                            )
                                            BottomToolItem(
                                                icon = Icons.AutoMirrored.Rounded.RotateRight,
                                                label = "Rotate",
                                                selected = activeToolDrawer == ToolDrawerTab.ROTATE,
                                                tag = "bottom_tool_rotate",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.ROTATE) }
                                            )
                                        }
                                        selectedLayer is ShapeLayer -> {
                                            BottomToolItem(
                                                icon = Icons.Rounded.Category,
                                                label = "Shapes",
                                                selected = activeToolDrawer == ToolDrawerTab.SHAPES,
                                                tag = "bottom_tool_shapes",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.SHAPES) }
                                            )
                                            BottomToolItem(
                                                icon = Icons.Rounded.AutoFixHigh,
                                                label = "Points",
                                                selected = shapeEditPointsMode,
                                                tag = "bottom_tool_shape_points",
                                                onClick = { viewModel.toggleShapeEditPointsMode() }
                                            )
                                            BottomToolItem(
                                                icon = Icons.Rounded.Palette,
                                                label = "Color",
                                                selected = activeToolDrawer == ToolDrawerTab.COLOR,
                                                tag = "bottom_tool_color",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.COLOR) }
                                            )
                                            BottomToolItem(
                                                icon = Icons.AutoMirrored.Rounded.RotateRight,
                                                label = "Rotate",
                                                selected = activeToolDrawer == ToolDrawerTab.ROTATE,
                                                tag = "bottom_tool_rotate",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.ROTATE) }
                                            )
                                        }
                                        selectedLayer is DrawLayer -> {
                                            BottomToolItem(
                                                icon = Icons.Rounded.Brush,
                                                label = "Draw",
                                                selected = activeToolDrawer == ToolDrawerTab.DRAW,
                                                tag = "bottom_tool_draw",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.DRAW) }
                                            )
                                            BottomToolItem(
                                                icon = Icons.Rounded.Palette,
                                                label = "Color",
                                                selected = activeToolDrawer == ToolDrawerTab.COLOR,
                                                tag = "bottom_tool_color",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.COLOR) }
                                            )
                                            BottomToolItem(
                                                icon = Icons.AutoMirrored.Rounded.RotateRight,
                                                label = "Rotate",
                                                selected = activeToolDrawer == ToolDrawerTab.ROTATE,
                                                tag = "bottom_tool_rotate",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.ROTATE) }
                                            )
                                        }
                                        selectedLayer is GroupLayer -> {
                                            BottomToolItem(
                                                icon = Icons.AutoMirrored.Rounded.RotateRight,
                                                label = "Rotate",
                                                selected = activeToolDrawer == ToolDrawerTab.ROTATE,
                                                tag = "bottom_tool_rotate",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.ROTATE) }
                                            )
                                        }
                                        else -> {
                                            BottomToolItem(
                                                icon = Icons.Rounded.Image,
                                                label = "Photo",
                                                selected = false,
                                                tag = "bottom_tool_photo",
                                                onClick = onPickPhotosDirect
                                            )
                                            BottomToolItem(
                                                icon = Icons.Rounded.Title,
                                                label = "Text+",
                                                selected = activeToolDrawer == ToolDrawerTab.TEXT,
                                                tag = "bottom_tool_text_plus",
                                                onClick = { viewModel.startNewTextDraft() }
                                            )
                                            BottomToolItem(
                                                icon = Icons.Rounded.Category,
                                                label = "Shapes",
                                                selected = activeToolDrawer == ToolDrawerTab.SHAPES,
                                                tag = "bottom_tool_shapes",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.SHAPES) }
                                            )
                                            BottomToolItem(
                                                icon = Icons.Rounded.Brush,
                                                label = "Draw",
                                                selected = activeToolDrawer == ToolDrawerTab.DRAW,
                                                tag = "bottom_tool_draw",
                                                onClick = { viewModel.openToolDrawer(ToolDrawerTab.DRAW) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isSpaciousLandscapeCanvas) {
                    Spacer(modifier = Modifier.weight(0.72f))
                }
            }

            // Floating Alight-Motion-Style 3D Controller Overlay (supports ALL layer types)
            if (activeToolDrawer == ToolDrawerTab.THREE_D && selectedLayer != null) {
                FloatingThreeDControllerPanel(
                    selectedLayer = selectedLayer,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onMoveXY = { dxNorm, dyNorm -> viewModel.moveSelectedLayerXYTransient(dxNorm, dyNorm) },
                    onUpdate3D = { transform -> viewModel.updateSelectedLayer3DTransient(transform) },
                    onSetRotationZ = { deg -> viewModel.setSelectedLayerRotation(deg) },
                    onScale3D = { sc, sx, sy -> viewModel.scaleSelectedLayer3DTransient(sc, sx, sy) },
                    onResetMode = { modeIdx -> viewModel.resetSelectedLayer3DMode(modeIdx) },
                    onCenterLayer = { viewModel.centerSelectedLayer() },
                    onResetAll = { viewModel.resetAllSelectedLayer3D() },
                    onEndTransaction = { viewModel.endTransformTransaction() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = when {
                                isClearTypingMode -> 8.dp
                                isSpaciousLandscapeCanvas -> 10.dp
                                else -> 72.dp
                            }
                        )
                )
            }

            // Right-Half Vertical Side Layers Drawer (Top-to-bottom on right side)
            AnimatedVisibility(
                visible = showLayersDrawer,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                LayersSideDrawer(
                    layers = document.layers,
                    selectedLayerId = document.selectedLayerId,
                    selectionOrder = document.effectiveSelectionOrder,
                    isMultiSelectMode = isMultiSelectMode,
                    imageBitmaps = imageBitmaps,
                    onSelectLayer = { viewModel.selectLayer(it) },
                    onHoldSelectMulti = { viewModel.enterMultiSelectWithLayer(it) },
                    onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                    onToggleLock = { viewModel.toggleLayerLock(it) },
                    onDeleteLayer = { viewModel.deleteLayer(it) },
                    onMoveLayerOrder = { id, dir -> viewModel.moveLayerOrder(id, dir) },
                    onGroupSelected = { viewModel.handleGroupAction() },
                    onToggleGroupExpanded = { viewModel.toggleGroupExpanded(it) },
                    onUngroupLayer = { viewModel.ungroupLayer(it) },
                    onClose = { viewModel.setShowLayersDrawer(false) }
                )
            }

            // Right-Half Vertical Side My Projects Drawer (Auto-backups current project on switch)
            AnimatedVisibility(
                visible = showProjectsDrawer,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                MyProjectsSideDrawer(
                    projects = projectHistory,
                    currentProjectId = document.id,
                    onSelectProject = { targetId ->
                        scope.launch { viewModel.switchProjectInEditor(targetId) }
                    },
                    onClose = { viewModel.setShowProjectsDrawer(false) }
                )
            }
        }
    }

    // 3-Dot Settings Bottom Sheet (Canvas | Background | Emoji)
    if (activeThreeDotSection != ThreeDotSheetSection.NONE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeThreeDotSection = ThreeDotSheetSection.NONE },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThreeDotSheetSection.entries
                        .filter { it != ThreeDotSheetSection.NONE }
                        .forEach { sec ->
                            FilterChip(
                                selected = activeThreeDotSection == sec,
                                onClick = { activeThreeDotSection = sec },
                                label = { Text(sec.title) }
                            )
                        }
                }

                when (activeThreeDotSection) {
                    ThreeDotSheetSection.CANVAS -> {
                        CanvasSettingsPanel(
                            canvas = document.canvas,
                            onSelectSquare = { viewModel.selectSquareCanvas() },
                            onSelectPreset = { viewModel.selectCanvasPreset(it) },
                            onApplyCustom = { r, p, preferPx ->
                                viewModel.applyCustomCanvas(r, p, preferPx)
                            }
                        )
                    }
                    ThreeDotSheetSection.BACKGROUND -> {
                        BackgroundSettingsPanel(
                            background = document.background,
                            blurredPreview = blurredPreview,
                            selectedBgImageBitmap = document.background.backgroundImageUri?.let { imageBitmaps[it] },
                            isEyedropperActive = isEyedropperActive,
                            onUpdateBackground = { viewModel.updateBackground(it) },
                            onPickBackgroundImage = {
                                activeThreeDotSection = ThreeDotSheetSection.NONE
                                onPickBackgroundImageDirect()
                            },
                            onToggleEyedropper = {
                                viewModel.startEyedropper(EyedropperTarget.BACKGROUND)
                                activeThreeDotSection = ThreeDotSheetSection.NONE
                            }
                        )
                    }
                    ThreeDotSheetSection.EMOJI -> {
                        EmojiSettingsPanel(
                            emojiSourceMode = document.emojiSourceMode,
                            appEmojiFontId = document.appEmojiFontId,
                            fontsRecentToOld = fontsRecentToOld,
                            renderEngine = viewModel.fontRenderEngine,
                            onSelectMode = { viewModel.setEmojiSourceMode(it) },
                            onSelectAppEmojiFont = { viewModel.setAppEmojiFont(it) }
                        )
                    }
                    ThreeDotSheetSection.NONE -> Unit
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showExportDialog) {
        SaveExportDialog(
            canvas = document.canvas,
            initialFormat = settings.exportFormat,
            initialPreset = settings.exportResolutionPreset,
            initialCustomWidth = settings.exportCustomWidth,
            initialCustomHeight = settings.exportCustomHeight,
            exportFeedback = exportFeedback,
            onExport = { fmt, preset, cw, ch ->
                viewModel.exportProjectWithOptions(
                    format = fmt,
                    preset = preset,
                    customWidth = cw,
                    customHeight = ch,
                    shareImmediately = false
                )
            },
            onDismiss = {
                viewModel.clearExportFeedback()
                showExportDialog = false
            }
        )
    }

        if (isEyedropperActive) {
            FloatingEyedropperOverlay(
                onPickColor = { argbInt -> viewModel.applyEyedropperColor(argbInt) },
                onCancel = { viewModel.setEyedropperActive(false) }
            )
        }

        val pendingPhoto = pendingCropQueue.firstOrNull()
        if (pendingPhoto != null) {
            PendingPhotoCropOverlay(
                pendingPhoto = pendingPhoto,
                onDelete = { viewModel.discardCurrentPendingCrop() },
                onDone = { crop, shape -> viewModel.commitCurrentPendingCrop(crop, shape) }
            )
        }
    }
}

@Composable
private fun FloatingCanvasCornerButton(
    icon: ImageVector,
    label: String,
    highlighted: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlighted) StudioAmber.copy(alpha = 0.94f) else Color.Black.copy(alpha = 0.72f),
        contentColor = if (highlighted) Color.Black else Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TopActionItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    highlightColor: Color = StudioAmber,
    compact: Boolean = false,
    tag: String,
    onClick: () -> Unit
) {
    val tint = when {
        !enabled -> Color.White.copy(alpha = 0.35f)
        highlighted -> highlightColor
        else -> Color.White
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = if (compact) 4.dp else 8.dp, vertical = 4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(if (compact) 18.dp else 20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun TransformLockTopItem(
    shortCode: String,
    label: String,
    isLocked: Boolean,
    compact: Boolean = false,
    tag: String,
    onClick: () -> Unit
) {
    val tint = if (isLocked) StudioCoral else Color.White
    val bgColor = if (isLocked) Color.White.copy(alpha = 0.2f) else Color.Transparent
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = if (compact) 5.dp else 10.dp, vertical = 4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                contentDescription = "$label ${if (isLocked) "Locked" else "Unlocked"}",
                tint = tint,
                modifier = Modifier.size(if (compact) 14.dp else 16.dp)
            )
            Text(
                text = shortCode,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = tint,
                maxLines = 1
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun BottomToolItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val tint = if (selected) StudioAzure else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) StudioAzure.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
