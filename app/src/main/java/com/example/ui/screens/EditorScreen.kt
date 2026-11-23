package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.GroupLayer
import com.example.model.ImageLayer
import com.example.model.TextLayer
import com.example.ui.ExportFeedbackState
import com.example.ui.EyedropperTarget
import com.example.ui.StudioViewModel
import com.example.ui.ToolDrawerTab
import com.example.ui.components.AdjustToolPanel
import com.example.ui.components.BackgroundSettingsPanel
import com.example.ui.components.CanvasSettingsPanel
import com.example.ui.components.ColorPalettePickerDialog
import com.example.ui.components.ColorToolPanel
import com.example.ui.components.CornerPinToolPanel
import com.example.ui.components.CropToolPanel
import com.example.ui.components.EditorCanvasViewport
import com.example.ui.components.EmojiSettingsPanel
import com.example.ui.components.FilterToolPanel
import com.example.ui.components.FloatingDraggableResizablePanel
import com.example.ui.components.FloatingEyedropperOverlay
import com.example.ui.components.FloatingPreviewWindow
import com.example.ui.components.FloatingThreeDControllerPanel
import com.example.ui.components.FontBrowserToolPanel
import com.example.ui.components.HorizontalLayerStrip
import com.example.ui.components.LayersSideDrawer
import com.example.ui.components.MyProjectsSideDrawer
import com.example.ui.components.PendingPhotoCropOverlay
import com.example.ui.components.RotateTransformPanel
import com.example.ui.components.SaveExportDialog
import com.example.ui.components.TextToolPanel
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

private enum class ThreeDotSheetSection(val title: String) {
    NONE(""),
    CANVAS("Canvas"),
    BACKGROUND("Background"),
    EMOJI("Emoji"),
    SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: StudioViewModel,
    onNavigateBack: () -> Unit,
    onPickPhotosDirect: () -> Unit,
    onPickFillMediaDirect: () -> Unit = {},
    onOpenFontLibrary: () -> Unit,
    onImportFontsClick: () -> Unit = onOpenFontLibrary,
    onImportArchiveClick: () -> Unit = onOpenFontLibrary
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val document by viewModel.document.collectAsStateWithLifecycle()
    val imageBitmaps by viewModel.imageBitmaps.collectAsStateWithLifecycle()
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
    val transformLocks by viewModel.transformLocks.collectAsStateWithLifecycle()
    val pendingCropQueue by viewModel.pendingCropQueue.collectAsStateWithLifecycle()
    val isCornerPinAddPointMode by viewModel.isCornerPinAddPointMode.collectAsStateWithLifecycle()
    val isEditingFillMedia by viewModel.isEditingFillMedia.collectAsStateWithLifecycle()
    val isCreatorViewActive by viewModel.isCreatorViewActive.collectAsStateWithLifecycle()
    val showCreatorViewEyeButton by viewModel.showCreatorViewEyeButton.collectAsStateWithLifecycle()

    var showAddMenu by remember { mutableStateOf(false) }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var activeThreeDotSection by remember { mutableStateOf(ThreeDotSheetSection.NONE) }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingHideTab by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCreatorColorPalette by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    // Multi-stage Android Back behavior: never quit app directly when panels/options are open
    BackHandler {
        when {
            showCreatorColorPalette -> {
                showCreatorColorPalette = false
            }
            pendingHideTab != null -> {
                pendingHideTab = null
            }
            isEyedropperActive -> {
                viewModel.setEyedropperActive(false)
            }
            isEditingFillMedia -> {
                viewModel.setEditingFillMedia(false)
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
    val activeFont = fonts.firstOrNull { it.id == (selectedTextLayer?.fontId ?: selectedFontId) }
    val isClearTypingMode = isImeVisible && activeToolDrawer == ToolDrawerTab.TEXT

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isCreatorViewActive) {
                Surface(
                    color = StudioAzure,
                    contentColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .detectUnconsumedTap(key = "topbar_empty") {
                            viewModel.dismissAllOverlaysAndSelection()
                        }
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
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (settings.previewWindowEnabled) "5. Preview (ON)" else "5. Preview (OFF)"
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Preview, contentDescription = null) },
                                    modifier = Modifier.testTag("menu_preview_toggle"),
                                    onClick = {
                                        showThreeDotMenu = false
                                        viewModel.setPreviewWindowEnabled(!settings.previewWindowEnabled)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("6. Settings") },
                                    leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                    modifier = Modifier.testTag("menu_settings"),
                                    onClick = {
                                        showThreeDotMenu = false
                                        activeThreeDotSection = ThreeDotSheetSection.SETTINGS
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.18f))

                    // Row 2: Original-Size Top Action Bar:
                    // Delete | Corner Pin | 3D | Filter | Crop | S | Z | R | P Locks
                    val hasSelection = selectedLayer != null
                    val topControls = listOf(
                        TopBarControlSpec.Action(
                            id = "top_delete",
                            icon = Icons.Rounded.DeleteOutline,
                            label = "Delete",
                            enabled = hasSelection,
                            tag = "top_delete_button",
                            onClick = { selectedLayer?.let { viewModel.deleteLayer(it.id) } }
                        ),
                        TopBarControlSpec.Action(
                            id = "top_corner_pin",
                            icon = Icons.Rounded.Transform,
                            label = "Corner Pin",
                            enabled = hasSelection,
                            highlighted = activeToolDrawer == ToolDrawerTab.CORNER_PIN,
                            highlightColor = StudioAmber,
                            tag = "top_tool_corner_pin",
                            onClick = { viewModel.openToolDrawer(ToolDrawerTab.CORNER_PIN) }
                        ),
                        TopBarControlSpec.Action(
                            id = "top_3d",
                            icon = Icons.Rounded.ViewInAr,
                            label = "3D",
                            enabled = hasSelection,
                            highlighted = activeToolDrawer == ToolDrawerTab.THREE_D,
                            highlightColor = StudioAmber,
                            tag = "top_tool_3d",
                            onClick = { viewModel.openToolDrawer(ToolDrawerTab.THREE_D) }
                        ),
                        TopBarControlSpec.Action(
                            id = "top_filter",
                            icon = Icons.Rounded.AutoAwesome,
                            label = "Filter",
                            enabled = hasSelection,
                            highlighted = activeToolDrawer == ToolDrawerTab.FILTER,
                            highlightColor = StudioAmber,
                            tag = "top_tool_filter",
                            onClick = { viewModel.openToolDrawer(ToolDrawerTab.FILTER) }
                        ),
                        TopBarControlSpec.Action(
                            id = "top_crop",
                            icon = Icons.Rounded.Crop,
                            label = "Crop",
                            enabled = hasSelection,
                            highlighted = activeToolDrawer == ToolDrawerTab.CROP,
                            highlightColor = StudioAmber,
                            tag = "top_tool_crop",
                            onClick = { viewModel.openToolDrawer(ToolDrawerTab.CROP) }
                        ),
                        TopBarControlSpec.Lock(
                            id = "top_lock_s",
                            shortCode = "S",
                            label = "Stretch",
                            isLocked = transformLocks.stretchLocked,
                            tag = "top_lock_stretch_button",
                            onClick = { viewModel.toggleStretchLock() }
                        ),
                        TopBarControlSpec.Lock(
                            id = "top_lock_z",
                            shortCode = "Z",
                            label = "Zoom",
                            isLocked = transformLocks.zoomLocked,
                            tag = "top_lock_zoom_button",
                            onClick = { viewModel.toggleZoomLock() }
                        ),
                        TopBarControlSpec.Lock(
                            id = "top_lock_r",
                            shortCode = "R",
                            label = "Rotate",
                            isLocked = transformLocks.rotationLocked,
                            tag = "top_lock_rotation_button",
                            onClick = { viewModel.toggleRotationLock() }
                        ),
                        TopBarControlSpec.Lock(
                            id = "top_lock_p",
                            shortCode = "P",
                            label = "Move",
                            isLocked = transformLocks.positionLocked,
                            tag = "top_lock_position_button",
                            onClick = { viewModel.togglePositionLock() }
                        )
                    ).filter { it.id !in settings.hiddenTabIds }

                    if (topControls.isNotEmpty()) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("top_controls_row2")
                        ) {
                            val canvasRatioForTop = document.canvas.ratioFloat.coerceAtLeast(0.1f)
                            val topCols = (maxWidth / 60.dp).toInt().coerceIn(3, 9)
                            val allowTwoTopRows = canvasRatioForTop >= 0.85f || maxWidth >= 500.dp
                            val neededTopRows = ceil(topControls.size / topCols.toFloat()).toInt().coerceAtLeast(1)
                            val topRowsPerPage = if (allowTwoTopRows) minOf(2, neededTopRows) else 1
                            val topPageSize = (topCols * topRowsPerPage).coerceAtLeast(1)
                            val topPages = topControls.chunked(topPageSize)

                            if (topPages.size <= 1) {
                                TopActionPageGrid(
                                    pageItems = topControls,
                                    cols = topCols,
                                    rows = topRowsPerPage,
                                    holdHideEnabled = settings.tabHideFeatureEnabled,
                                    onRequestHideTab = { id, lbl -> pendingHideTab = id to lbl }
                                )
                            } else {
                                val topPagerState = rememberPagerState(pageCount = { topPages.size })
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    HorizontalPager(
                                        state = topPagerState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("top_row2_pager")
                                    ) { pageIdx ->
                                        TopActionPageGrid(
                                            pageItems = topPages[pageIdx],
                                            cols = topCols,
                                            rows = topRowsPerPage,
                                            holdHideEnabled = settings.tabHideFeatureEnabled,
                                            onRequestHideTab = { id, lbl -> pendingHideTab = id to lbl }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.padding(bottom = 4.dp, top = 1.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(topPages.size) { dotIdx ->
                                            val isCur = topPagerState.currentPage == dotIdx
                                            Box(
                                                modifier = Modifier
                                                    .size(if (isCur) 7.dp else 5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isCur) Color.White else Color.White.copy(alpha = 0.38f)
                                                    )
                                                    .clickable {
                                                        scope.launch { topPagerState.animateScrollToPage(dotIdx) }
                                                    }
                                                    .testTag("top_pager_dot_$dotIdx")
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
        }
    ) { innerPadding ->
        var rootOriginInRoot by remember { mutableStateOf(Offset.Zero) }
        var canvasRightInRoot by remember { mutableFloatStateOf(0f) }
        var canvasTopInRoot by remember { mutableFloatStateOf(0f) }
        var zoomBtnOffset by remember { mutableStateOf(Offset.Zero) }
        var zoomBtnDragged by remember { mutableStateOf(false) }
        var zoomBtnSize by remember { mutableStateOf(IntSize.Zero) }
        var layersBtnOffset by remember { mutableStateOf(Offset.Zero) }
        var layersBtnDragged by remember { mutableStateOf(false) }
        var layersBtnSize by remember { mutableStateOf(IntSize.Zero) }
        var creatorEyeOffset by remember { mutableStateOf(Offset.Zero) }
        var creatorEyeDragged by remember { mutableStateOf(false) }
        var creatorEyeSize by remember { mutableStateOf(IntSize.Zero) }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
                .background(
                    if (isCreatorViewActive) Color(settings.creatorViewColorArgb.toInt())
                    else MaterialTheme.colorScheme.background
                )
                .onGloballyPositioned { coords ->
                    rootOriginInRoot = coords.positionInRoot()
                }
                .detectUnconsumedTap(key = "editor_theme_bg") {
                    if (!isCreatorViewActive) {
                        viewModel.dismissAllOverlaysAndSelection()
                    }
                }
                .testTag("editor_root_theme_background")
        ) {
            val availW = maxWidth
            val availH = maxHeight
            val availWpx = with(density) { availW.toPx() }
            val availHpx = with(density) { availH.toPx() }
            val canvasRatio = document.canvas.ratioFloat.coerceAtLeast(0.1f)
            val estimatedCanvasH = ((availW - 16.dp).coerceAtLeast(120.dp)) / canvasRatio
            val spareVerticalDp = (availH - estimatedCanvasH).coerceAtLeast(0.dp)
            val isSpaciousLandscapeCanvas = canvasRatio > 1.15f && spareVerticalDp > 220.dp
            val maxSafeDrawerHeight = (availH - 92.dp).coerceAtLeast(72.dp)
            val toolDrawerMaxHeight = when {
                isClearTypingMode -> 140.dp
                isSpaciousLandscapeCanvas -> (spareVerticalDp - 88.dp).coerceIn(280.dp, 460.dp)
                else -> 265.dp
            }.coerceAtMost(maxSafeDrawerHeight)

            Column(modifier = Modifier.fillMaxSize()) {
                if (isSpaciousLandscapeCanvas) {
                    Spacer(modifier = Modifier.weight(0.28f))
                }

                // Canvas Viewport
                Box(
                    modifier = (if (isSpaciousLandscapeCanvas) {
                        Modifier
                            .fillMaxWidth()
                            .height(estimatedCanvasH.coerceIn(140.dp, availH * 0.56f))
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    }).onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        canvasRightInRoot = pos.x + coords.size.width
                        canvasTopInRoot = pos.y
                    },
                    contentAlignment = Alignment.Center
                ) {
                    EditorCanvasViewport(
                        document = document,
                        imageBitmaps = imageBitmaps,
                        fontRenderEngine = viewModel.fontRenderEngine,
                        isZoomMode = isZoomMode,
                        isCropMode = activeToolDrawer == ToolDrawerTab.CROP,
                        isCornerPinMode = activeToolDrawer == ToolDrawerTab.CORNER_PIN,
                        isCornerPinAddPointMode = isCornerPinAddPointMode,
                        isEditingFillMedia = isEditingFillMedia,
                        isCreatorViewActive = isCreatorViewActive,
                        creatorViewMaskColorArgb = settings.creatorViewColorArgb,
                        transformLocks = transformLocks,
                        canvasZoom = canvasZoom,
                        canvasPan = canvasPan,
                        isEyedropperActive = isEyedropperActive,
                        draftText = draftText,
                        draftTextStyle = draftTextStyle,
                        isDraftTextPreview = (activeToolDrawer == ToolDrawerTab.TEXT || activeToolDrawer == ToolDrawerTab.FONT) && document.selectedLayerId == null,
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
                        onTextWidthReflow = { id, signX, sdx, bw, cw, ch ->
                            viewModel.updateSelectedTextWrapWidthTransient(id, signX, sdx, bw, cw, ch)
                        },
                        onCornerPinDrag = { id, cornerIndex, dxRatio, dyRatio ->
                            viewModel.updateSelectedLayerCornerPinTransient(id, cornerIndex, dxRatio, dyRatio)
                        },
                        onAddPuppetPin = { id, ax, ay ->
                            viewModel.addPuppetPinToLayer(id, ax, ay)
                        },
                        onPuppetPinDrag = { id, pinId, dxR, dyR ->
                            viewModel.updatePuppetPinDragTransient(id, pinId, dxR, dyR)
                        },
                        onTransformFillMedia = { dx, dy, sc, rot ->
                            viewModel.transformSelectedLayerFillMediaTransient(dx, dy, sc, rot)
                        },
                        onBeginTransformTransaction = { viewModel.beginTransformTransaction() },
                        onEndTransformTransaction = { viewModel.endTransformTransaction() },
                        onUpdateCropTransient = { viewModel.updateSelectedLayerCropTransient(it) },
                        onCommitCrop = { viewModel.updateSelectedLayerCrop(it) },
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
                                    viewModel.applyEyedropperColor(sampled)
                                } else {
                                    viewModel.setEyedropperActive(false)
                                }
                            }
                        }
                    )

                    if (!isCreatorViewActive && (canvasZoom != 1f || canvasPan.first != 0f || canvasPan.second != 0f)) {
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

                // Always-visible Layer Strip + Single-Row Bottom Tool Bar (hidden in Creator View)
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Always-visible Horizontal Layer Strip whenever NO floating tool option panel is open
                    AnimatedVisibility(
                        visible = activeToolDrawer == ToolDrawerTab.NONE && !isCreatorViewActive,
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

                    // Original-Size Context-Sensitive Bottom Tool Bar
                    AnimatedVisibility(visible = !isClearTypingMode && !isCreatorViewActive) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 4.dp
                            ) {
                                val isInTextMode = activeToolDrawer == ToolDrawerTab.TEXT || selectedLayer is TextLayer
                                val isPhotoSelected = selectedLayer is ImageLayer

                                val dockTools = when {
                                    isInTextMode -> listOf(
                                        DockToolSpec(
                                            id = "dock_edit_text",
                                            icon = Icons.Rounded.Edit,
                                            label = "Edit",
                                            selected = activeToolDrawer == ToolDrawerTab.TEXT,
                                            tag = "bottom_tool_edit_text",
                                            onClick = { viewModel.openTextToolDrawer() }
                                        ),
                                        DockToolSpec(
                                            id = "dock_color",
                                            icon = Icons.Rounded.Palette,
                                            label = "Color",
                                            selected = activeToolDrawer == ToolDrawerTab.COLOR,
                                            tag = "bottom_tool_color",
                                            onClick = { viewModel.openToolDrawer(ToolDrawerTab.COLOR) }
                                        ),
                                        DockToolSpec(
                                            id = "dock_font",
                                            icon = Icons.Rounded.FontDownload,
                                            label = "Font",
                                            selected = activeToolDrawer == ToolDrawerTab.FONT,
                                            tag = "bottom_tool_font",
                                            onClick = { viewModel.openToolDrawer(ToolDrawerTab.FONT) }
                                        ),
                                        DockToolSpec(
                                            id = "dock_rotate",
                                            icon = Icons.AutoMirrored.Rounded.RotateRight,
                                            label = "Rotate",
                                            selected = activeToolDrawer == ToolDrawerTab.ROTATE,
                                            tag = "bottom_tool_rotate",
                                            onClick = { viewModel.openToolDrawer(ToolDrawerTab.ROTATE) }
                                        )
                                    )
                                    isPhotoSelected -> listOf(
                                        DockToolSpec(
                                            id = "dock_color",
                                            icon = Icons.Rounded.Palette,
                                            label = "Color",
                                            selected = activeToolDrawer == ToolDrawerTab.COLOR,
                                            tag = "bottom_tool_color",
                                            onClick = { viewModel.openToolDrawer(ToolDrawerTab.COLOR) }
                                        ),
                                        DockToolSpec(
                                            id = "dock_adjust",
                                            icon = Icons.Rounded.Tune,
                                            label = "Adjust",
                                            selected = activeToolDrawer == ToolDrawerTab.ADJUST,
                                            tag = "bottom_tool_adjust",
                                            onClick = {
                                                viewModel.ensureImageLayerSelectedForAdjust()
                                                viewModel.openToolDrawer(ToolDrawerTab.ADJUST)
                                            }
                                        ),
                                        DockToolSpec(
                                            id = "dock_rotate",
                                            icon = Icons.AutoMirrored.Rounded.RotateRight,
                                            label = "Rotate",
                                            selected = activeToolDrawer == ToolDrawerTab.ROTATE,
                                            tag = "bottom_tool_rotate",
                                            onClick = { viewModel.openToolDrawer(ToolDrawerTab.ROTATE) }
                                        )
                                    )
                                    selectedLayer is GroupLayer -> listOf(
                                        DockToolSpec(
                                            id = "dock_rotate",
                                            icon = Icons.AutoMirrored.Rounded.RotateRight,
                                            label = "Rotate",
                                            selected = activeToolDrawer == ToolDrawerTab.ROTATE,
                                            tag = "bottom_tool_rotate",
                                            onClick = { viewModel.openToolDrawer(ToolDrawerTab.ROTATE) }
                                        )
                                    )
                                    else -> listOf(
                                        DockToolSpec(
                                            id = "dock_photo",
                                            icon = Icons.Rounded.Image,
                                            label = "Photo",
                                            selected = false,
                                            tag = "bottom_tool_photo",
                                            onClick = onPickPhotosDirect
                                        ),
                                        DockToolSpec(
                                            id = "dock_text_plus",
                                            icon = Icons.Rounded.Title,
                                            label = "Text+",
                                            selected = activeToolDrawer == ToolDrawerTab.TEXT,
                                            tag = "bottom_tool_text_plus",
                                            onClick = { viewModel.startNewTextDraft() }
                                        )
                                    )
                                }.filter { it.id !in settings.hiddenTabIds }

                                if (dockTools.isNotEmpty()) {
                                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                        val cols = (maxWidth / 76.dp).toInt().coerceIn(2, 6)
                                        val maxSpareRows = if (activeToolDrawer != ToolDrawerTab.NONE) {
                                            1
                                        } else {
                                            ((spareVerticalDp - 72.dp) / 60.dp).toInt().coerceIn(1, 3)
                                        }
                                        val neededRows = ceil(dockTools.size / cols.toFloat()).toInt().coerceAtLeast(1)
                                        val rowsPerPage = minOf(neededRows, maxSpareRows).coerceAtLeast(1)
                                        val pageSize = (cols * rowsPerPage).coerceAtLeast(1)
                                        val pages = dockTools.chunked(pageSize)

                                        if (pages.size <= 1) {
                                            BottomDockPageGrid(
                                                pageItems = dockTools,
                                                cols = cols,
                                                rows = rowsPerPage,
                                                holdHideEnabled = settings.tabHideFeatureEnabled,
                                                onRequestHideTab = { id, lbl -> pendingHideTab = id to lbl }
                                            )
                                        } else {
                                            val dockPagerState = rememberPagerState(pageCount = { pages.size })
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                HorizontalPager(
                                                    state = dockPagerState,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("bottom_tool_pager")
                                                ) { pageIdx ->
                                                    BottomDockPageGrid(
                                                        pageItems = pages[pageIdx],
                                                        cols = cols,
                                                        rows = rowsPerPage,
                                                        holdHideEnabled = settings.tabHideFeatureEnabled,
                                                        onRequestHideTab = { id, lbl -> pendingHideTab = id to lbl }
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.padding(bottom = 6.dp, top = 2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    repeat(pages.size) { dotIdx ->
                                                        val isCur = dockPagerState.currentPage == dotIdx
                                                        Box(
                                                            modifier = Modifier
                                                                .size(if (isCur) 7.dp else 5.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isCur) StudioAzure
                                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                                                )
                                                                .clickable {
                                                                    scope.launch { dockPagerState.animateScrollToPage(dotIdx) }
                                                                }
                                                                .testTag("bottom_pager_dot_$dotIdx")
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
                }

                if (isSpaciousLandscapeCanvas && activeToolDrawer == ToolDrawerTab.NONE) {
                    Spacer(modifier = Modifier.weight(0.72f))
                }
            }

            // Movable Floating Zoom & Layers Buttons (freely draggable anywhere on screen, beneath active tool panels)
            if (!isCreatorViewActive) {
                val margin10Px = with(density) { 10.dp.toPx() }
                val gap8Px = with(density) { 8.dp.toPx() }
                val fallbackBtnPx = with(density) { 52.dp.toPx() }
                val zW = if (zoomBtnSize.width > 0) zoomBtnSize.width.toFloat() else fallbackBtnPx
                val zH = if (zoomBtnSize.height > 0) zoomBtnSize.height.toFloat() else fallbackBtnPx
                val lW = if (layersBtnSize.width > 0) layersBtnSize.width.toFloat() else fallbackBtnPx
                val lH = if (layersBtnSize.height > 0) layersBtnSize.height.toFloat() else fallbackBtnPx
                val canvasRightLocal = if (canvasRightInRoot > 0f) (canvasRightInRoot - rootOriginInRoot.x) else availWpx
                val canvasTopLocal = (canvasTopInRoot - rootOriginInRoot.y).coerceAtLeast(0f)

                val defaultZoomPos = Offset(
                    x = (canvasRightLocal - zW - margin10Px).coerceIn(0f, (availWpx - zW).coerceAtLeast(0f)),
                    y = (canvasTopLocal + margin10Px).coerceIn(0f, (availHpx - zH).coerceAtLeast(0f))
                )
                val defaultLayersPos = Offset(
                    x = (canvasRightLocal - lW - margin10Px).coerceIn(0f, (availWpx - lW).coerceAtLeast(0f)),
                    y = (canvasTopLocal + margin10Px + zH + gap8Px).coerceIn(0f, (availHpx - lH).coerceAtLeast(0f))
                )
                val effectiveZoomPos = if (zoomBtnDragged) {
                    Offset(
                        zoomBtnOffset.x.coerceIn(0f, (availWpx - zW).coerceAtLeast(0f)),
                        zoomBtnOffset.y.coerceIn(0f, (availHpx - zH).coerceAtLeast(0f))
                    )
                } else defaultZoomPos

                val effectiveLayersPos = if (layersBtnDragged) {
                    Offset(
                        layersBtnOffset.x.coerceIn(0f, (availWpx - lW).coerceAtLeast(0f)),
                        layersBtnOffset.y.coerceIn(0f, (availHpx - lH).coerceAtLeast(0f))
                    )
                } else defaultLayersPos

                FloatingCanvasCornerButton(
                    icon = Icons.Rounded.ZoomIn,
                    label = "Zoom",
                    highlighted = isZoomMode,
                    tag = "top_zoom_button",
                    modifier = Modifier
                        .offset { IntOffset(effectiveZoomPos.x.roundToInt(), effectiveZoomPos.y.roundToInt()) }
                        .onSizeChanged { zoomBtnSize = it }
                        .pointerInput(availWpx, availHpx) {
                            detectDragGestures(
                                onDragStart = {
                                    if (!zoomBtnDragged) {
                                        zoomBtnOffset = defaultZoomPos
                                        zoomBtnDragged = true
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val w = if (zoomBtnSize.width > 0) zoomBtnSize.width.toFloat() else fallbackBtnPx
                                    val h = if (zoomBtnSize.height > 0) zoomBtnSize.height.toFloat() else fallbackBtnPx
                                    zoomBtnOffset = Offset(
                                        x = (zoomBtnOffset.x + dragAmount.x).coerceIn(0f, (availWpx - w).coerceAtLeast(0f)),
                                        y = (zoomBtnOffset.y + dragAmount.y).coerceIn(0f, (availHpx - h).coerceAtLeast(0f))
                                    )
                                }
                            )
                        },
                    onClick = { viewModel.toggleZoomMode() }
                )

                FloatingCanvasCornerButton(
                    icon = Icons.Rounded.Layers,
                    label = "Layers",
                    highlighted = showLayersDrawer,
                    tag = "top_layers_button",
                    modifier = Modifier
                        .offset { IntOffset(effectiveLayersPos.x.roundToInt(), effectiveLayersPos.y.roundToInt()) }
                        .onSizeChanged { layersBtnSize = it }
                        .pointerInput(availWpx, availHpx) {
                            detectDragGestures(
                                onDragStart = {
                                    if (!layersBtnDragged) {
                                        layersBtnOffset = defaultLayersPos
                                        layersBtnDragged = true
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val w = if (layersBtnSize.width > 0) layersBtnSize.width.toFloat() else fallbackBtnPx
                                    val h = if (layersBtnSize.height > 0) layersBtnSize.height.toFloat() else fallbackBtnPx
                                    layersBtnOffset = Offset(
                                        x = (layersBtnOffset.x + dragAmount.x).coerceIn(0f, (availWpx - w).coerceAtLeast(0f)),
                                        y = (layersBtnOffset.y + dragAmount.y).coerceIn(0f, (availHpx - h).coerceAtLeast(0f))
                                    )
                                }
                            )
                        },
                    onClick = { viewModel.toggleLayersDrawer() }
                )
            }

            // Universal Floating & 4-Corner-Resizable Option Panels (canvas stays fixed full-size underneath)
            if (activeToolDrawer != ToolDrawerTab.NONE && activeToolDrawer != ToolDrawerTab.THREE_D) {
                FloatingDraggableResizablePanel(
                    panelTag = "tool_drawer_container",
                    dragHandleTag = "tool_drawer_drag_handle",
                    resizeTagPrefix = "tool_drawer_resize",
                    defaultAlignment = Alignment.BottomCenter,
                    bottomClearanceDp = if (isClearTypingMode || isCreatorViewActive) 10.dp else 72.dp,
                    defaultHeightDp = toolDrawerMaxHeight,
                    minWidthDp = 210.dp,
                    minHeightDp = if (isClearTypingMode) 90.dp else 110.dp,
                    showTopDragHandleStrip = !isClearTypingMode,
                    onDismissSwipeDown = { viewModel.closeToolDrawer() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = if (isClearTypingMode) 2.dp else 8.dp)
                    ) {
                        when (activeToolDrawer) {
                            ToolDrawerTab.CROP -> {
                                CropToolPanel(
                                    selectedLayer = selectedLayer,
                                    onResetCrop = { viewModel.resetSelectedLayerCrop() },
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
                                    onResetAdjustments = { viewModel.resetSelectedImageAdjustments() },
                                    onEndTransaction = { viewModel.endTransformTransaction() }
                                )
                            }
                            ToolDrawerTab.ROTATE -> {
                                RotateTransformPanel(
                                    selectedLayer = selectedLayer,
                                    onRotateBy = { viewModel.rotateSelectedLayerBy(it) },
                                    onSetRotation = { viewModel.setSelectedLayerRotation(it) },
                                    onSetScale = { viewModel.setSelectedLayerScale(it) },
                                    onMoveOrder = { dir ->
                                        selectedLayer?.let { viewModel.moveLayerOrder(it.id, dir) }
                                    },
                                    onDuplicate = {
                                        selectedLayer?.let { viewModel.duplicateLayer(it.id) }
                                    },
                                    onEndTransaction = { viewModel.endTransformTransaction() }
                                )
                            }
                            ToolDrawerTab.CORNER_PIN -> {
                                CornerPinToolPanel(
                                    canUndo = canUndo,
                                    isAddPointMode = isCornerPinAddPointMode,
                                    puppetPinCount = selectedLayer?.cornerPin?.puppetPins?.size ?: 0,
                                    onToggleAddPointMode = { viewModel.toggleCornerPinAddPointMode() },
                                    onClearPuppetPins = {
                                        selectedLayer?.let { viewModel.clearPuppetPins(it.id) }
                                    },
                                    onUndo = { viewModel.undo() },
                                    onConfirm = { viewModel.confirmCornerPin() }
                                )
                            }
                            ToolDrawerTab.FILTER -> {
                                FilterToolPanel(
                                    selectedLayer = selectedLayer,
                                    isEditingFillMedia = isEditingFillMedia,
                                    onSetBlendMode = { viewModel.setSelectedLayerBlendMode(it) },
                                    onUpdateEffectStyleTransient = { transform ->
                                        viewModel.updateSelectedLayerEffectStyle(transform, transient = true)
                                    },
                                    onSetOpacityTransient = { viewModel.setSelectedLayerOpacity(it) },
                                    onSetFillColor = { viewModel.setSelectedLayerFillColor(it) },
                                    onPickFillMedia = onPickFillMediaDirect,
                                    onSetEditingFillMedia = { viewModel.setEditingFillMedia(it) },
                                    onClearFillMedia = { viewModel.clearSelectedLayerFillMedia() },
                                    onLaunchEyedropper = { target -> viewModel.startEyedropper(target) },
                                    onEndTransaction = { viewModel.endTransformTransaction() }
                                )
                            }
                            ToolDrawerTab.THREE_D, ToolDrawerTab.NONE -> Unit
                        }
                    }
                }
            }

            // Floating Alight-Motion-Style 3D Controller Overlay (freely movable & 4-corner resizable)
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
                    onEndTransaction = { viewModel.endTransformTransaction() }
                )
            }

            // Optional Floating Live Preview Window (3-Dot Menu -> Preview toggle)
            if (settings.previewWindowEnabled && activeToolDrawer != ToolDrawerTab.NONE && !isCreatorViewActive) {
                FloatingPreviewWindow(
                    document = document,
                    imageBitmaps = imageBitmaps,
                    fontRenderEngine = viewModel.fontRenderEngine,
                    canvasZoom = canvasZoom,
                    canvasPan = canvasPan,
                    draftText = draftText,
                    draftTextStyle = draftTextStyle,
                    isDraftTextPreview = (activeToolDrawer == ToolDrawerTab.TEXT || activeToolDrawer == ToolDrawerTab.FONT) && document.selectedLayerId == null,
                    onClose = { viewModel.setPreviewWindowEnabled(false) }
                )
            }

            // Creator View: compact active-tab chip at top-start
            if (isCreatorViewActive && activeToolDrawer != ToolDrawerTab.NONE) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StudioAmber.copy(alpha = 0.95f),
                    contentColor = Color.Black,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.closeToolDrawer() }
                        .minimumInteractiveComponentSize()
                        .testTag("creator_active_tab_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = toolTabIcon(activeToolDrawer),
                            contentDescription = toolTabLabel(activeToolDrawer),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = toolTabLabel(activeToolDrawer),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Creator View Movable Floating Eye Button (Bottom-Right default, visible while active + 3s grace)
            if (showCreatorViewEyeButton) {
                val eyeFallbackPx = with(density) { 48.dp.toPx() }
                val eW = if (creatorEyeSize.width > 0) creatorEyeSize.width.toFloat() else eyeFallbackPx
                val eH = if (creatorEyeSize.height > 0) creatorEyeSize.height.toFloat() else eyeFallbackPx
                val defaultEyePos = Offset(
                    x = (availWpx - eW - with(density) { 14.dp.toPx() }).coerceAtLeast(0f),
                    y = (availHpx - eH - with(density) { 18.dp.toPx() }).coerceAtLeast(0f)
                )
                val effectiveEyePos = if (creatorEyeDragged) {
                    Offset(
                        creatorEyeOffset.x.coerceIn(0f, (availWpx - eW).coerceAtLeast(0f)),
                        creatorEyeOffset.y.coerceIn(0f, (availHpx - eH).coerceAtLeast(0f))
                    )
                } else defaultEyePos

                Surface(
                    shape = CircleShape,
                    color = if (isCreatorViewActive) Color.Black.copy(alpha = 0.78f) else StudioAmber,
                    contentColor = if (isCreatorViewActive) Color.White else Color.Black,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .offset { IntOffset(effectiveEyePos.x.roundToInt(), effectiveEyePos.y.roundToInt()) }
                        .onSizeChanged { creatorEyeSize = it }
                        .pointerInput(availWpx, availHpx) {
                            detectDragGestures(
                                onDragStart = {
                                    if (!creatorEyeDragged) {
                                        creatorEyeOffset = defaultEyePos
                                        creatorEyeDragged = true
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val w = if (creatorEyeSize.width > 0) creatorEyeSize.width.toFloat() else eyeFallbackPx
                                    val h = if (creatorEyeSize.height > 0) creatorEyeSize.height.toFloat() else eyeFallbackPx
                                    creatorEyeOffset = Offset(
                                        x = (creatorEyeOffset.x + dragAmount.x).coerceIn(0f, (availWpx - w).coerceAtLeast(0f)),
                                        y = (creatorEyeOffset.y + dragAmount.y).coerceIn(0f, (availHpx - h).coerceAtLeast(0f))
                                    )
                                }
                            )
                        }
                        .clip(CircleShape)
                        .clickable { viewModel.onCreatorViewEyeTapped() }
                        .minimumInteractiveComponentSize()
                        .testTag("creator_view_eye_button")
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCreatorViewActive) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = if (isCreatorViewActive) "Turn off Creator View" else "Re-enable Creator View",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Floating 2-Second Long-Press Hide Tab Prompt
            val hidePromptTarget = pendingHideTab
            if (hidePromptTarget != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { pendingHideTab = null },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .widthIn(min = 250.dp, max = 340.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}
                            .testTag("hide_tab_prompt")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Hide \"${hidePromptTarget.second}\"?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You can unhide it anytime from 3-Dot Menu → Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { pendingHideTab = null },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hide_tab_cancel_button")
                                ) {
                                    Text("Not")
                                }
                                Button(
                                    onClick = {
                                        viewModel.hideTab(hidePromptTarget.first)
                                        pendingHideTab = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioAzure),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hide_tab_confirm_button")
                                ) {
                                    Text("Hide")
                                }
                            }
                        }
                    }
                }
            }

            // Right-Half Vertical Side Layers Drawer (Top-to-bottom on right side)
            AnimatedVisibility(
                visible = showLayersDrawer && !isCreatorViewActive,
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
                visible = showProjectsDrawer && !isCreatorViewActive,
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
                            onUpdateBackground = { viewModel.updateBackground(it) }
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
                    ThreeDotSheetSection.SETTINGS -> {
                        EditorSettingsSheetContent(
                            tabHideEnabled = settings.tabHideFeatureEnabled,
                            hiddenTabIds = settings.hiddenTabIds,
                            creatorViewEnabled = settings.creatorViewEnabled,
                            creatorViewColorArgb = settings.creatorViewColorArgb,
                            onToggleTabHide = { viewModel.setTabHideFeatureEnabled(it) },
                            onUnhideTab = { viewModel.unhideTab(it) },
                            onToggleCreatorView = { viewModel.setCreatorViewEnabled(it) },
                            onSelectCreatorColor = { viewModel.setCreatorViewColorArgb(it) },
                            onOpenCreatorPalette = { showCreatorColorPalette = true }
                        )
                    }
                    ThreeDotSheetSection.NONE -> Unit
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showCreatorColorPalette) {
        ColorPalettePickerDialog(
            initialArgb = settings.creatorViewColorArgb,
            onColorSelected = { argb ->
                viewModel.setCreatorViewColorArgb(argb)
                showCreatorColorPalette = false
            },
            onDismiss = { showCreatorColorPalette = false }
        )
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
private fun EditorSettingsSheetContent(
    tabHideEnabled: Boolean,
    hiddenTabIds: Set<String>,
    creatorViewEnabled: Boolean,
    creatorViewColorArgb: Long,
    onToggleTabHide: (Boolean) -> Unit,
    onUnhideTab: (String) -> Unit,
    onToggleCreatorView: (Boolean) -> Unit,
    onSelectCreatorColor: (Long) -> Unit,
    onOpenCreatorPalette: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab Hide & Unhide Section
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hold Tab to Hide (2s)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Press & hold any editor tab for 2 seconds to hide it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = tabHideEnabled,
                        onCheckedChange = onToggleTabHide,
                        modifier = Modifier.testTag("settings_hide_tabs_switch")
                    )
                }

                if (hiddenTabIds.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Text(
                        text = "Hidden Tabs (${hiddenTabIds.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    hiddenTabIds.sorted().forEach { tabId ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tabLabelForId(tabId),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { onUnhideTab(tabId) },
                                modifier = Modifier.testTag("unhide_tab_$tabId")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Visibility,
                                    contentDescription = "Unhide ${tabLabelForId(tabId)}",
                                    tint = StudioAzure
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No tabs are currently hidden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Creator View Section
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Creator View",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Isolate selected layer & option panel over chroma mask",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = creatorViewEnabled,
                        onCheckedChange = onToggleCreatorView,
                        modifier = Modifier.testTag("settings_creator_view_switch")
                    )
                }

                Text(
                    text = "Mask Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val presets = listOf(
                    0xFF00FF00L to "creator_color_green",
                    0xFF000000L to "creator_color_black",
                    0xFFFFFFFFL to "creator_color_white",
                    0xFF007AFFL to "creator_color_blue",
                    0xFFFF00FFL to "creator_color_magenta",
                    0xFFFF3B30L to "creator_color_red"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presets.forEach { (argb, tag) ->
                        val selected = creatorViewColorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(argb.toInt()))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) StudioAmber else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onSelectCreatorColor(argb) }
                                .testTag(tag),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected Mask Color",
                                    tint = if (argb == 0xFFFFFFFFL || argb == 0xFF00FF00L) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onOpenCreatorPalette,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("creator_color_palette")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "Custom Mask Color Palette",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun tabLabelForId(tabId: String): String = when (tabId) {
    "top_delete" -> "Delete"
    "top_corner_pin" -> "Corner Pin"
    "top_3d" -> "3D"
    "top_filter" -> "Filter"
    "top_crop" -> "Crop"
    "top_lock_s" -> "Stretch (S)"
    "top_lock_z" -> "Zoom (Z)"
    "top_lock_r" -> "Rotate (R)"
    "top_lock_p" -> "Move (P)"
    "dock_edit_text" -> "Edit Text"
    "dock_color" -> "Color"
    "dock_font" -> "Font"
    "dock_rotate" -> "Rotate"
    "dock_adjust" -> "Adjust"
    "dock_photo" -> "Photo"
    "dock_text_plus" -> "Text+"
    else -> tabId
}

private fun toolTabIcon(tab: ToolDrawerTab): ImageVector = when (tab) {
    ToolDrawerTab.TEXT -> Icons.Rounded.Edit
    ToolDrawerTab.COLOR -> Icons.Rounded.Palette
    ToolDrawerTab.FONT -> Icons.Rounded.FontDownload
    ToolDrawerTab.ROTATE -> Icons.AutoMirrored.Rounded.RotateRight
    ToolDrawerTab.ADJUST -> Icons.Rounded.Tune
    ToolDrawerTab.CROP -> Icons.Rounded.Crop
    ToolDrawerTab.CORNER_PIN -> Icons.Rounded.Transform
    ToolDrawerTab.FILTER -> Icons.Rounded.AutoAwesome
    ToolDrawerTab.THREE_D -> Icons.Rounded.ViewInAr
    ToolDrawerTab.PHOTO -> Icons.Rounded.Image
    ToolDrawerTab.NONE -> Icons.Rounded.AutoAwesome
}

private fun toolTabLabel(tab: ToolDrawerTab): String = when (tab) {
    ToolDrawerTab.TEXT -> "Text"
    ToolDrawerTab.COLOR -> "Color"
    ToolDrawerTab.FONT -> "Font"
    ToolDrawerTab.ROTATE -> "Rotate"
    ToolDrawerTab.ADJUST -> "Adjust"
    ToolDrawerTab.CROP -> "Crop"
    ToolDrawerTab.CORNER_PIN -> "Corner Pin"
    ToolDrawerTab.FILTER -> "Filter"
    ToolDrawerTab.THREE_D -> "3D"
    ToolDrawerTab.PHOTO -> "Photo"
    ToolDrawerTab.NONE -> ""
}

@Composable
private fun FloatingCanvasCornerButton(
    icon: ImageVector,
    label: String,
    highlighted: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlighted) StudioAmber.copy(alpha = 0.94f) else Color.Black.copy(alpha = 0.72f),
        contentColor = if (highlighted) Color.Black else Color.White,
        shadowElevation = 4.dp,
        modifier = modifier
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

private sealed class TopBarControlSpec {
    abstract val id: String
    abstract val label: String

    data class Action(
        override val id: String,
        val icon: ImageVector,
        override val label: String,
        val enabled: Boolean = true,
        val highlighted: Boolean = false,
        val highlightColor: Color = StudioAmber,
        val tag: String,
        val onClick: () -> Unit
    ) : TopBarControlSpec()

    data class Lock(
        override val id: String,
        val shortCode: String,
        override val label: String,
        val isLocked: Boolean,
        val tag: String,
        val onClick: () -> Unit
    ) : TopBarControlSpec()
}

private data class DockToolSpec(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
private fun TopActionPageGrid(
    pageItems: List<TopBarControlSpec>,
    cols: Int,
    rows: Int,
    holdHideEnabled: Boolean,
    onRequestHideTab: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val rowChunks = pageItems.chunked(cols)
        for (r in 0 until rows) {
            val rowItems = rowChunks.getOrNull(r) ?: emptyList()
            if (rowItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowItems.forEach { item ->
                        when (item) {
                            is TopBarControlSpec.Action -> TopActionItem(
                                icon = item.icon,
                                label = item.label,
                                enabled = item.enabled,
                                highlighted = item.highlighted,
                                highlightColor = item.highlightColor,
                                tag = item.tag,
                                modifier = Modifier.onTwoSecondHold(
                                    key = item.id,
                                    enabled = holdHideEnabled
                                ) { onRequestHideTab(item.id, item.label) },
                                onClick = item.onClick
                            )
                            is TopBarControlSpec.Lock -> TransformLockTopItem(
                                shortCode = item.shortCode,
                                label = item.label,
                                isLocked = item.isLocked,
                                tag = item.tag,
                                modifier = Modifier.onTwoSecondHold(
                                    key = item.id,
                                    enabled = holdHideEnabled
                                ) { onRequestHideTab(item.id, "${item.label} (${item.shortCode})") },
                                onClick = item.onClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomDockPageGrid(
    pageItems: List<DockToolSpec>,
    cols: Int,
    rows: Int,
    holdHideEnabled: Boolean,
    onRequestHideTab: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val rowChunks = pageItems.chunked(cols)
        for (r in 0 until rows) {
            val rowItems = rowChunks.getOrNull(r) ?: emptyList()
            if (rowItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowItems.forEach { item ->
                        BottomToolItem(
                            icon = item.icon,
                            label = item.label,
                            selected = item.selected,
                            tag = item.tag,
                            modifier = Modifier.onTwoSecondHold(
                                key = item.id,
                                enabled = holdHideEnabled
                            ) { onRequestHideTab(item.id, item.label) },
                            onClick = item.onClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fires [onHoldTriggered] when user presses & holds cleanly (within touch slop) for [holdMillis] (default 2s),
 * consuming the release so click handlers do not also trigger, while staying transparent to normal clicks & swipes.
 */
private fun Modifier.onTwoSecondHold(
    key: Any,
    enabled: Boolean,
    holdMillis: Long = 2000L,
    onHoldTriggered: () -> Unit
): Modifier = if (!enabled) this else pointerInput(key, enabled) {
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var triggered = false
            var cancelled = false
            val holdJob = launch {
                delay(holdMillis)
                if (!cancelled && !triggered) {
                    triggered = true
                    onHoldTriggered()
                }
            }
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                if (triggered) {
                    event.changes.forEach { it.consume() }
                    if (event.changes.none { it.pressed }) {
                        break
                    }
                    continue
                }
                val active = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                if (active == null || !active.pressed) {
                    cancelled = true
                    holdJob.cancel()
                    if (active != null && (active.uptimeMillis - down.uptimeMillis) >= holdMillis) {
                        triggered = true
                        event.changes.forEach { it.consume() }
                        onHoldTriggered()
                    }
                    break
                }
                if ((active.position - down.position).getDistance() >= viewConfiguration.touchSlop) {
                    cancelled = true
                    holdJob.cancel()
                    break
                }
            }
        }
    }
}

/**
 * Fires [onTap] ONLY when a single-finger press releases inside touch slop without ANY
 * child clickable, slider, drag handle, pager, or canvas viewport consuming it.
 */
private fun Modifier.detectUnconsumedTap(
    key: Any = Unit,
    onTap: () -> Unit
): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
        var anyConsumedOrMulti = down.isConsumed
        var totalMove = Offset.Zero
        var pastSlop = false
        var cleanUp = false
        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Main)
            if (event.changes.count { it.pressed } >= 2 || event.changes.any { it.isConsumed }) {
                anyConsumedOrMulti = true
                break
            }
            val active = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
            if (active == null || !active.pressed) {
                if (active != null && !active.isConsumed) {
                    cleanUp = true
                }
                break
            }
            totalMove += (active.position - active.previousPosition)
            if (totalMove.getDistance() >= viewConfiguration.touchSlop) {
                pastSlop = true
            }
        }
        if (!anyConsumedOrMulti && !pastSlop && cleanUp) {
            onTap()
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint = when {
        !enabled -> Color.White.copy(alpha = 0.35f)
        highlighted -> highlightColor
        else -> Color.White
    }
    Column(
        modifier = modifier
            .widthIn(min = 50.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 4.dp, vertical = 4.dp)
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint = if (isLocked) StudioCoral else Color.White
    val bgColor = if (isLocked) Color.White.copy(alpha = 0.2f) else Color.Transparent
    Column(
        modifier = modifier
            .widthIn(min = 50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 6.dp, vertical = 4.dp)
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint = if (selected) StudioAzure else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .widthIn(min = 68.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) StudioAzure.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
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
