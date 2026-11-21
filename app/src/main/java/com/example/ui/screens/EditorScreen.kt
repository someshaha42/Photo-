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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEmotions
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
    val transformLocks by viewModel.transformLocks.collectAsStateWithLifecycle()
    val pendingCropQueue by viewModel.pendingCropQueue.collectAsStateWithLifecycle()

    var showAddMenu by remember { mutableStateOf(false) }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var activeThreeDotSection by remember { mutableStateOf(ThreeDotSheetSection.NONE) }
    var showExportDialog by remember { mutableStateOf(false) }
    val topBarPagerState = rememberPagerState(pageCount = { 2 })
    val bottomBarPagerState = rememberPagerState(pageCount = { 2 })

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
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.18f))

                    // Row 2: Horizontally swipeable 2-page action bar
                    // Page 0: Delete | Layers | Zoom
                    // Page 1: Transform Locks (S: Stretch, Z: Zoom, R: Rotation, P: Position)
                    HorizontalPager(
                        state = topBarPagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("top_action_pager")
                    ) { page ->
                        if (page == 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val hasSelection = selectedLayer != null
                                TopActionItem(
                                    icon = Icons.Rounded.DeleteOutline,
                                    label = "Delete",
                                    enabled = hasSelection,
                                    tag = "top_delete_button",
                                    onClick = { selectedLayer?.let { viewModel.deleteLayer(it.id) } }
                                )
                                TopActionItem(
                                    icon = Icons.Rounded.Layers,
                                    label = "Layers",
                                    highlighted = showLayersDrawer,
                                    highlightColor = StudioAmber,
                                    tag = "top_layers_button",
                                    onClick = { viewModel.toggleLayersDrawer() }
                                )
                                TopActionItem(
                                    icon = Icons.Rounded.ZoomIn,
                                    label = "Zoom",
                                    highlighted = isZoomMode,
                                    highlightColor = StudioAmber,
                                    tag = "top_zoom_button",
                                    onClick = { viewModel.toggleZoomMode() }
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
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
                    }

                    // Compact 2-dot page indicator (swipe or tap dot to switch pages)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(2) { idx ->
                            val isCurrent = topBarPagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(width = if (isCurrent) 16.dp else 6.dp, height = 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) Color.White else Color.White.copy(alpha = 0.35f)
                                    )
                                    .clickable {
                                        scope.launch { topBarPagerState.animateScrollToPage(idx) }
                                    }
                                    .testTag("top_pager_dot_$idx")
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // Swipe-down Tool Drawer (appears cleanly BELOW fixed canvas, ABOVE bottom tool bar)
                // Note: ToolDrawerTab.THREE_D floats directly over the canvas instead of using the bottom drawer.
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
                                .heightIn(max = if (isClearTypingMode) 140.dp else 265.dp)
                        ) {
                            // Drag handle collapses while typing so canvas remains maximally visible
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
                                            canUndoStroke = hasDrawContent,
                                            onSelectColor = { viewModel.setDrawColor(it) },
                                            onSetBrushSize = { viewModel.setDrawBrushSizeNorm(it) },
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

                AnimatedVisibility(visible = !isClearTypingMode) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                        // Context-Sensitive Bottom Tool Bar (swipeable 2-page HorizontalPager on Photo & Text):
                        // - Text Mode -> Page 0: Edit | Color | Font | Rotate ; Page 1: Corner Pin | 3D
                        // - Photo selected -> Page 0: Crop | Color | Adjust | Rotate ; Page 1: Corner Pin | 3D
                        // - Shape selected -> Shapes | Color | Rotate
                        // - Draw selected -> Draw | Color | Rotate
                        // - Group selected -> Rotate
                        // - Unselected main screen -> ONLY Photo (direct gallery) | Text+ | Shapes | Draw
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            val isInTextMode = activeToolDrawer == ToolDrawerTab.TEXT || selectedLayer is TextLayer
                            val isPhotoSelected = selectedLayer is ImageLayer

                            LaunchedEffect(isInTextMode, isPhotoSelected, selectedLayer?.id) {
                                if (!isInTextMode && !isPhotoSelected && bottomBarPagerState.currentPage != 0) {
                                    bottomBarPagerState.scrollToPage(0)
                                }
                            }

                            if (isInTextMode || isPhotoSelected) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    HorizontalPager(
                                        state = bottomBarPagerState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("bottom_tool_pager")
                                    ) { page ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isInTextMode) {
                                                if (page == 0) {
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
                                                } else {
                                                    BottomToolItem(
                                                        icon = Icons.Rounded.Transform,
                                                        label = "Corner Pin",
                                                        selected = activeToolDrawer == ToolDrawerTab.CORNER_PIN,
                                                        tag = "bottom_tool_corner_pin",
                                                        onClick = { viewModel.openToolDrawer(ToolDrawerTab.CORNER_PIN) }
                                                    )
                                                    BottomToolItem(
                                                        icon = Icons.Rounded.ViewInAr,
                                                        label = "3D",
                                                        selected = activeToolDrawer == ToolDrawerTab.THREE_D,
                                                        tag = "bottom_tool_3d",
                                                        onClick = { viewModel.openToolDrawer(ToolDrawerTab.THREE_D) }
                                                    )
                                                }
                                            } else {
                                                if (page == 0) {
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
                                                } else {
                                                    BottomToolItem(
                                                        icon = Icons.Rounded.Transform,
                                                        label = "Corner Pin",
                                                        selected = activeToolDrawer == ToolDrawerTab.CORNER_PIN,
                                                        tag = "bottom_tool_corner_pin",
                                                        onClick = { viewModel.openToolDrawer(ToolDrawerTab.CORNER_PIN) }
                                                    )
                                                    BottomToolItem(
                                                        icon = Icons.Rounded.ViewInAr,
                                                        label = "3D",
                                                        selected = activeToolDrawer == ToolDrawerTab.THREE_D,
                                                        tag = "bottom_tool_3d",
                                                        onClick = { viewModel.openToolDrawer(ToolDrawerTab.THREE_D) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Compact 2-dot page indicator for Photo/Text bottom bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(2) { idx ->
                                            val isCurrent = bottomBarPagerState.currentPage == idx
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .size(width = if (isCurrent) 16.dp else 6.dp, height = 5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isCurrent) StudioAzure
                                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                                    )
                                                    .clickable {
                                                        scope.launch { bottomBarPagerState.animateScrollToPage(idx) }
                                                    }
                                                    .testTag("bottom_pager_dot_$idx")
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when {
                                    selectedLayer is ShapeLayer -> {
                                        BottomToolItem(
                                            icon = Icons.Rounded.Category,
                                            label = "Shapes",
                                            selected = activeToolDrawer == ToolDrawerTab.SHAPES,
                                            tag = "bottom_tool_shapes",
                                            onClick = { viewModel.openToolDrawer(ToolDrawerTab.SHAPES) }
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
            }
        }
    ) { innerPadding ->
        // Fixed Middle Canvas Viewport + Right-Half Layers Side Drawer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
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
                drawColorArgb = drawColorArgb,
                drawBrushSizeNorm = drawBrushSizeNorm,
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

            // Floating Alight-Motion-Style 3D Controller Overlay (Photo & Text)
            if (activeToolDrawer == ToolDrawerTab.THREE_D && (selectedLayer is ImageLayer || selectedLayer is TextLayer)) {
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
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            if (canvasZoom != 1f || canvasPan.first != 0f || canvasPan.second != 0f) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
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

            // Right-Half Vertical Side Layers Drawer (Top-to-bottom on right side of canvas)
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
private fun TopActionItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    highlightColor: Color = StudioAmber,
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
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
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
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                contentDescription = "$label ${if (isLocked) "Locked" else "Unlocked"}",
                tint = tint,
                modifier = Modifier.size(16.dp)
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
