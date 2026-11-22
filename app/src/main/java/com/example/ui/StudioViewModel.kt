package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettings
import com.example.data.MediaStorageManager
import com.example.data.ProjectDatabase
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import com.example.data.SettingsRepository
import com.example.fonts.FontDiagnosticsSnapshot
import com.example.fonts.FontImportProgress
import com.example.fonts.FontMetadata
import com.example.fonts.FontRenderEngine
import com.example.fonts.FontRepository
import com.example.fonts.FontSource
import com.example.fonts.ImportHistoryEntry
import com.example.model.BackgroundConfig
import com.example.model.CanvasDimensions
import com.example.model.CornerPinOffsets
import com.example.model.EditorLayer
import com.example.model.EmojiSourceMode
import com.example.model.ExportImageFormat
import com.example.model.ExportResolutionPreset
import com.example.model.GroupLayer
import com.example.model.ImageAdjustments
import com.example.model.ImageCropRect
import com.example.model.ImageCropShape
import com.example.model.ImageLayer
import com.example.model.Layer3DTransform
import com.example.model.LayerBlendMode
import com.example.model.LayerEffectStyle
import com.example.model.ProjectDocument
import com.example.model.PuppetPinPoint
import com.example.model.TextCaseMode
import com.example.model.TextCaseTransformer
import com.example.model.TextLayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

sealed interface ExportFeedbackState {
    data object Idle : ExportFeedbackState
    data object Exporting : ExportFeedbackState
    data class Success(val file: File, val message: String, val shareImmediately: Boolean = false) : ExportFeedbackState
    data class Error(val message: String) : ExportFeedbackState
}

enum class ToolDrawerTab(val label: String) {
    NONE(""),
    CROP("Crop"),
    COLOR("Color"),
    FONT("Font"),
    PHOTO("Photo"),
    TEXT("Text"),
    ADJUST("Adjust"),
    ROTATE("Rotate"),
    CORNER_PIN("Corner Pin"),
    THREE_D("3D"),
    FILTER("Filter")
}

enum class EyedropperTarget {
    NONE,
    SELECTED_LAYER
}

/**
 * Lock flags where ON (true) disables that transform on canvas touch/handles,
 * and OFF (false) keeps that transform active.
 * Stretch is ON (locked) by default so accidental dragging/zooming never stretches;
 * Zoom, Rotation, and Position are OFF (unlocked) by default.
 */
data class TransformLockState(
    val stretchLocked: Boolean = true,
    val zoomLocked: Boolean = false,
    val rotationLocked: Boolean = false,
    val positionLocked: Boolean = false
)

data class PendingImportPhoto(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val displayName: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val previewBitmap: Bitmap
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    val settingsRepository = SettingsRepository(context)
    private val database = ProjectDatabase.getInstance(context)
    val projectRepository = ProjectRepository(context, database.projectDao())
    val mediaManager = MediaStorageManager(context)
    val fontRenderEngine = FontRenderEngine(context)
    val fontRepository = FontRepository(context, fontRenderEngine)

    private val _previewWindowOverride = MutableStateFlow<Boolean?>(null)
    val settings: StateFlow<AppSettings> = combine(
        settingsRepository.settingsFlow,
        _previewWindowOverride
    ) { repoSettings, previewOverride ->
        if (previewOverride != null) repoSettings.copy(previewWindowEnabled = previewOverride)
        else repoSettings
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val projectHistory: StateFlow<List<ProjectEntity>> = projectRepository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fonts: StateFlow<List<FontMetadata>> = fontRepository.fonts
    val fontsRecentToOld: StateFlow<List<FontMetadata>> = fontRepository.fonts
        .map { list ->
            val imported = list.filter { it.source == FontSource.IMPORTED }
                .sortedByDescending { it.importedAtMillis }
            val bundled = list.filter { it.source == FontSource.BUNDLED }
            imported + bundled
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedFontId: StateFlow<String> = fontRepository.selectedFontId
    val fontImportProgress: StateFlow<FontImportProgress> = fontRepository.importProgress
    val fontImportHistory: StateFlow<List<ImportHistoryEntry>> = fontRepository.importHistory

    private val _document = MutableStateFlow(ProjectDocument())
    val document: StateFlow<ProjectDocument> = _document.asStateFlow()

    private val undoStack = ArrayDeque<ProjectDocument>()
    private val redoStack = ArrayDeque<ProjectDocument>()
    private var activeTransactionBaseline: ProjectDocument? = null
    private var locksBeforeCornerPin: TransformLockState? = null

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _activeToolDrawer = MutableStateFlow(ToolDrawerTab.NONE)
    val activeToolDrawer: StateFlow<ToolDrawerTab> = _activeToolDrawer.asStateFlow()

    private val _showLayersDrawer = MutableStateFlow(false)
    val showLayersDrawer: StateFlow<Boolean> = _showLayersDrawer.asStateFlow()

    private val _showProjectsDrawer = MutableStateFlow(false)
    val showProjectsDrawer: StateFlow<Boolean> = _showProjectsDrawer.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _draftText = MutableStateFlow("")
    val draftText: StateFlow<String> = _draftText.asStateFlow()

    private val _draftTextStyle = MutableStateFlow(TextLayer(text = ""))
    val draftTextStyle: StateFlow<TextLayer> = _draftTextStyle.asStateFlow()

    private var lastAppliedCaseMode: TextCaseMode? = null

    private val _isZoomMode = MutableStateFlow(false)
    val isZoomMode: StateFlow<Boolean> = _isZoomMode.asStateFlow()

    private val _canvasZoom = MutableStateFlow(1f)
    val canvasZoom: StateFlow<Float> = _canvasZoom.asStateFlow()

    private val _canvasPan = MutableStateFlow(0f to 0f)
    val canvasPan: StateFlow<Pair<Float, Float>> = _canvasPan.asStateFlow()

    private val _eyedropperTarget = MutableStateFlow(EyedropperTarget.NONE)
    val eyedropperTarget: StateFlow<EyedropperTarget> = _eyedropperTarget.asStateFlow()

    val isEyedropperActive: StateFlow<Boolean> = _eyedropperTarget
        .map { it != EyedropperTarget.NONE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _exportFeedback = MutableStateFlow<ExportFeedbackState>(ExportFeedbackState.Idle)
    val exportFeedback: StateFlow<ExportFeedbackState> = _exportFeedback.asStateFlow()

    private val _imageBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val imageBitmaps: StateFlow<Map<String, Bitmap>> = _imageBitmaps.asStateFlow()

    private val _diagnostics = MutableStateFlow(FontDiagnosticsSnapshot())
    val diagnostics: StateFlow<FontDiagnosticsSnapshot> = _diagnostics.asStateFlow()

    private val _transformLocks = MutableStateFlow(TransformLockState())
    val transformLocks: StateFlow<TransformLockState> = _transformLocks.asStateFlow()

    private val _pendingCropQueue = MutableStateFlow<List<PendingImportPhoto>>(emptyList())
    val pendingCropQueue: StateFlow<List<PendingImportPhoto>> = _pendingCropQueue.asStateFlow()

    private val _isCornerPinAddPointMode = MutableStateFlow(false)
    val isCornerPinAddPointMode: StateFlow<Boolean> = _isCornerPinAddPointMode.asStateFlow()

    private val _isEditingFillMedia = MutableStateFlow(false)
    val isEditingFillMedia: StateFlow<Boolean> = _isEditingFillMedia.asStateFlow()

    init {
        viewModelScope.launch {
            fontRepository.initialize()
            _draftTextStyle.value = _draftTextStyle.value.copy(fontId = fontRepository.selectedFontId.value)
            syncPinnedFonts()
            refreshDiagnostics(includeDetails = false)
        }
    }

    fun refreshDiagnostics(includeDetails: Boolean = true) {
        _diagnostics.value = fontRenderEngine.getDiagnosticsSnapshot(includePerFontDetails = includeDetails)
    }

    private fun syncPinnedFonts() {
        val ids = mutableSetOf(fontRepository.selectedFontId.value)
        _document.value.appEmojiFontId?.let { ids.add(it) }
        fun collectTextFonts(layers: List<EditorLayer>) {
            for (layer in layers) {
                when (layer) {
                    is TextLayer -> ids.add(layer.fontId)
                    is GroupLayer -> collectTextFonts(layer.children)
                    else -> Unit
                }
            }
        }
        collectTextFonts(_document.value.layers)
        fontRenderEngine.pinFonts(ids)
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun openToolDrawer(tab: ToolDrawerTab) {
        endTransformTransaction()
        val prevTab = _activeToolDrawer.value
        val nextTab = if (prevTab == tab) ToolDrawerTab.NONE else tab
        _isCornerPinAddPointMode.value = false
        if (nextTab != ToolDrawerTab.FILTER) {
            _isEditingFillMedia.value = false
        }
        if (prevTab == ToolDrawerTab.CORNER_PIN && nextTab != ToolDrawerTab.CORNER_PIN) {
            restoreLocksAfterCornerPin()
        } else if (nextTab == ToolDrawerTab.CORNER_PIN && prevTab != ToolDrawerTab.CORNER_PIN) {
            if (locksBeforeCornerPin == null) {
                locksBeforeCornerPin = _transformLocks.value
            }
            _transformLocks.value = TransformLockState(
                stretchLocked = true,
                zoomLocked = true,
                rotationLocked = true,
                positionLocked = true
            )
        }
        _activeToolDrawer.value = nextTab
    }

    fun closeToolDrawer() {
        endTransformTransaction()
        _isCornerPinAddPointMode.value = false
        _isEditingFillMedia.value = false
        if (_activeToolDrawer.value == ToolDrawerTab.CORNER_PIN) {
            restoreLocksAfterCornerPin()
        }
        _activeToolDrawer.value = ToolDrawerTab.NONE
        val selected = findLayerRecursive(_document.value.layers, _document.value.selectedLayerId)
        if (selected !is TextLayer) {
            _draftText.value = ""
            lastAppliedCaseMode = null
        }
    }

    fun confirmCornerPin() {
        endTransformTransaction()
        _isCornerPinAddPointMode.value = false
        if (_activeToolDrawer.value == ToolDrawerTab.CORNER_PIN) {
            restoreLocksAfterCornerPin()
            _activeToolDrawer.value = ToolDrawerTab.NONE
        }
    }

    private fun restoreLocksAfterCornerPin() {
        locksBeforeCornerPin?.let { saved ->
            _transformLocks.value = saved
        }
        locksBeforeCornerPin = null
    }

    fun dismissAllOverlaysAndSelection() {
        endTransformTransaction()
        _isCornerPinAddPointMode.value = false
        _isEditingFillMedia.value = false
        if (_activeToolDrawer.value == ToolDrawerTab.CORNER_PIN) {
            restoreLocksAfterCornerPin()
        } else if (_activeToolDrawer.value == ToolDrawerTab.TEXT &&
            _document.value.selectedLayerId == null &&
            _draftText.value.isNotBlank()
        ) {
            addTextLayer(_draftText.value)
        }
        _activeToolDrawer.value = ToolDrawerTab.NONE
        // Sticky drawers & modes: do NOT auto-close showLayersDrawer or isZoomMode on canvas/background tap!
        _showProjectsDrawer.value = false
        _eyedropperTarget.value = EyedropperTarget.NONE
        _draftText.value = ""
        lastAppliedCaseMode = null
        selectLayer(null)
    }

    fun toggleLayersDrawer() {
        val next = !_showLayersDrawer.value
        _showLayersDrawer.value = next
        if (next) _showProjectsDrawer.value = false
    }

    fun setShowLayersDrawer(open: Boolean) {
        _showLayersDrawer.value = open
        if (open) _showProjectsDrawer.value = false
    }

    fun toggleProjectsDrawer() {
        val next = !_showProjectsDrawer.value
        _showProjectsDrawer.value = next
        if (next) _showLayersDrawer.value = false
    }

    fun setShowProjectsDrawer(open: Boolean) {
        _showProjectsDrawer.value = open
        if (open) _showLayersDrawer.value = false
    }

    fun toggleMultiSelectMode() {
        val next = !_isMultiSelectMode.value
        _isMultiSelectMode.value = next
        if (!next) {
            val primary = _document.value.selectedLayerId ?: _document.value.selectionOrder.lastOrNull()
            _document.value = _document.value.copy(
                selectedLayerId = primary,
                selectionOrder = listOfNotNull(primary)
            )
        }
    }

    fun toggleZoomMode() {
        _isZoomMode.value = !_isZoomMode.value
    }

    fun setZoomMode(enabled: Boolean) {
        _isZoomMode.value = enabled
    }

    fun toggleStretchLock() {
        val cur = _transformLocks.value
        _transformLocks.value = cur.copy(stretchLocked = !cur.stretchLocked)
    }

    fun toggleZoomLock() {
        val cur = _transformLocks.value
        _transformLocks.value = cur.copy(zoomLocked = !cur.zoomLocked)
    }

    fun toggleRotationLock() {
        val cur = _transformLocks.value
        _transformLocks.value = cur.copy(rotationLocked = !cur.rotationLocked)
    }

    fun togglePositionLock() {
        val cur = _transformLocks.value
        _transformLocks.value = cur.copy(positionLocked = !cur.positionLocked)
    }

    fun updateCanvasViewportTransform(zoomDelta: Float, panDx: Float, panDy: Float) {
        _canvasZoom.value = (_canvasZoom.value * zoomDelta).coerceIn(0.25f, 100f)
        val (px, py) = _canvasPan.value
        _canvasPan.value = (px + panDx) to (py + panDy)
    }

    fun resetCanvasViewport() {
        _canvasZoom.value = 1f
        _canvasPan.value = 0f to 0f
    }

    fun setEyedropperActive(active: Boolean, target: EyedropperTarget = EyedropperTarget.SELECTED_LAYER) {
        _eyedropperTarget.value = if (active) target else EyedropperTarget.NONE
    }

    fun startEyedropper(target: EyedropperTarget) {
        _eyedropperTarget.value = target
    }

    fun applyEyedropperColor(argbInt: Int) {
        val opaqueLong = (argbInt.toLong() and 0xFFFFFFFFL) or 0xFF000000L
        when (_eyedropperTarget.value) {
            EyedropperTarget.SELECTED_LAYER -> {
                applyColorToSelectedLayer(opaqueLong)
            }
            EyedropperTarget.NONE -> Unit
        }
        _eyedropperTarget.value = EyedropperTarget.NONE
    }

    fun toggleTheme() {
        viewModelScope.launch { settingsRepository.toggleTheme() }
    }

    fun setPreviewWindowEnabled(enabled: Boolean) {
        _previewWindowOverride.value = enabled
        viewModelScope.launch { settingsRepository.setPreviewWindowEnabled(enabled) }
    }

    fun setKeepLocalMediaCopy(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepLocalMediaCopy(enabled) }
    }

    fun setUnlimitedLayers(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUnlimitedLayers(enabled) }
    }

    fun createNewProject() {
        activeTransactionBaseline = null
        restoreLocksAfterCornerPin()
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _canvasZoom.value = 1f
        _canvasPan.value = 0f to 0f
        _isZoomMode.value = false
        _isMultiSelectMode.value = false
        _showLayersDrawer.value = false
        _showProjectsDrawer.value = false
        _activeToolDrawer.value = ToolDrawerTab.NONE
        _isCornerPinAddPointMode.value = false
        _isEditingFillMedia.value = false
        _eyedropperTarget.value = EyedropperTarget.NONE
        _draftText.value = ""
        lastAppliedCaseMode = null
        val s = settings.value
        val fresh = ProjectDocument(
            id = UUID.randomUUID().toString(),
            title = "Project ${projectHistory.value.size + 1}",
            canvas = CanvasDimensions.SQUARE_1_1,
            background = BackgroundConfig(),
            layers = emptyList(),
            selectedLayerId = null,
            selectionOrder = emptyList(),
            emojiSourceMode = s.emojiSourceMode,
            appEmojiFontId = s.appEmojiFontId
        )
        _document.value = fresh
    }

    suspend fun openProjectFromHistory(projectId: String): Boolean {
        val loaded = projectRepository.loadDocument(projectId) ?: return false
        activeTransactionBaseline = null
        restoreLocksAfterCornerPin()
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _canvasZoom.value = 1f
        _canvasPan.value = 0f to 0f
        _isZoomMode.value = false
        _isMultiSelectMode.value = false
        _showLayersDrawer.value = false
        _showProjectsDrawer.value = false
        _activeToolDrawer.value = ToolDrawerTab.NONE
        _isCornerPinAddPointMode.value = false
        _isEditingFillMedia.value = false
        _eyedropperTarget.value = EyedropperTarget.NONE
        _draftText.value = ""
        lastAppliedCaseMode = null
        _document.value = loaded
        syncPinnedFonts()
        preloadProjectAssets(loaded)
        return true
    }

    suspend fun switchProjectInEditor(targetProjectId: String): Boolean {
        val currentDoc = _document.value
        if (currentDoc.id == targetProjectId) {
            _showProjectsDrawer.value = false
            return true
        }
        // Auto-backup the currently open project so no work is ever lost or deleted
        val thumbPath = mediaManager.saveProjectThumbnail(currentDoc, fontRenderEngine)
        projectRepository.saveDocument(currentDoc, thumbPath)
        val switched = openProjectFromHistory(targetProjectId)
        if (switched) {
            _statusBanner.value = "Switched to \"${_document.value.title}\" (previous saved)"
        }
        return switched
    }

    fun deleteHistoryProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
        }
    }

    fun deleteHistoryProjects(projectIds: Set<String>) {
        if (projectIds.isEmpty()) return
        viewModelScope.launch {
            projectRepository.deleteProjects(projectIds)
            _statusBanner.value = "Deleted ${projectIds.size} project${if (projectIds.size == 1) "" else "s"}"
        }
    }

    fun saveHistoryProjectToDevice(projectId: String) {
        viewModelScope.launch {
            val doc = projectRepository.loadDocument(projectId)
            if (doc == null) {
                _statusBanner.value = "Unable to load project"
                return@launch
            }
            val thumbPath = mediaManager.saveProjectThumbnail(doc, fontRenderEngine)
            projectRepository.saveDocument(doc, thumbPath)
            val s = settings.value
            val (targetW, targetH) = MediaStorageManager.computeTargetResolution(
                canvas = doc.canvas,
                preset = s.exportResolutionPreset,
                customWidth = s.exportCustomWidth,
                customHeight = s.exportCustomHeight
            )
            val res = mediaManager.exportImageToAppAndGallery(
                document = doc,
                fontRenderEngine = fontRenderEngine,
                format = s.exportFormat,
                targetWidth = targetW,
                targetHeight = targetH
            )
            res.onSuccess { (_, galleryUri) ->
                val dest = if (galleryUri != null) "Pictures/PixelForge" else "App Exports"
                _statusBanner.value = "Saved \"${doc.title}\" (${s.exportFormat.label}) to $dest"
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Save failed"
            }
        }
    }

    private fun preloadProjectAssets(doc: ProjectDocument) {
        viewModelScope.launch {
            val map = _imageBitmaps.value.toMutableMap()
            collectAllBitmapUris(doc.layers).forEach { uri ->
                if (!map.containsKey(uri)) {
                    mediaManager.decodeRgba8888Bitmap(uri, maxDimension = null)?.let {
                        map[uri] = it
                    }
                }
            }
            _imageBitmaps.value = map
        }
    }

    private fun collectImageLayers(layers: List<EditorLayer>): List<ImageLayer> {
        val out = mutableListOf<ImageLayer>()
        for (layer in layers) {
            when (layer) {
                is ImageLayer -> out.add(layer)
                is GroupLayer -> out.addAll(collectImageLayers(layer.children))
                else -> Unit
            }
        }
        return out
    }

    private fun collectAllBitmapUris(layers: List<EditorLayer>): List<String> {
        val out = mutableListOf<String>()
        for (layer in layers) {
            if (layer is ImageLayer) out.add(layer.uriString)
            layer.fillStyle.fillMediaUri?.takeIf { it.isNotBlank() }?.let { out.add(it) }
            if (layer is GroupLayer) out.addAll(collectAllBitmapUris(layer.children))
        }
        return out
    }

    fun beginTransformTransaction() {
        if (activeTransactionBaseline == null) {
            activeTransactionBaseline = _document.value
        }
    }

    fun endTransformTransaction() {
        val baseline = activeTransactionBaseline ?: return
        activeTransactionBaseline = null
        val current = _document.value
        if (baseline != current) {
            undoStack.addLast(baseline)
            if (undoStack.size > 40) undoStack.removeFirst()
            redoStack.clear()
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = false
            syncPinnedFonts()
        }
    }

    private fun commitChange(transform: (ProjectDocument) -> ProjectDocument) {
        val baseline = activeTransactionBaseline ?: _document.value
        activeTransactionBaseline = null
        val next = transform(_document.value)
        if (baseline == next) return
        undoStack.addLast(baseline)
        if (undoStack.size > 40) undoStack.removeFirst()
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
        _document.value = next
        syncPinnedFonts()
    }

    fun undo() {
        endTransformTransaction()
        if (undoStack.isEmpty()) return
        val current = _document.value
        val prev = undoStack.removeLast()
        redoStack.addLast(current)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _document.value = prev
    }

    fun redo() {
        endTransformTransaction()
        if (redoStack.isEmpty()) return
        val current = _document.value
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _document.value = next
    }

    private fun canAddLayer(extraCount: Int = 1): Boolean {
        if (settings.value.unlimitedLayers) return true
        if (_document.value.layers.size + extraCount <= 5) return true
        _statusBanner.value = "Layer limit (5) reached. Enable Unlimited Layers in Settings."
        return false
    }

    fun selectLayer(layerId: String?) {
        endTransformTransaction()
        lastAppliedCaseMode = null
        _isEditingFillMedia.value = false
        if (layerId == null) {
            _isCornerPinAddPointMode.value = false
            if (_activeToolDrawer.value == ToolDrawerTab.CORNER_PIN) {
                restoreLocksAfterCornerPin()
                _activeToolDrawer.value = ToolDrawerTab.NONE
            } else if (_activeToolDrawer.value == ToolDrawerTab.THREE_D ||
                _activeToolDrawer.value == ToolDrawerTab.CROP ||
                _activeToolDrawer.value == ToolDrawerTab.FILTER
            ) {
                _activeToolDrawer.value = ToolDrawerTab.NONE
            }
            _isMultiSelectMode.value = false
            _document.value = _document.value.copy(
                selectedLayerId = null,
                selectionOrder = emptyList()
            )
            return
        }
        if (_isMultiSelectMode.value) {
            toggleMultiSelectLayer(layerId)
            return
        }
        _document.value = _document.value.copy(
            selectedLayerId = layerId,
            selectionOrder = listOf(layerId)
        )
        val layer = findLayerRecursive(_document.value.layers, layerId)
        if (layer is TextLayer) {
            fontRepository.selectFont(layer.fontId)
            _draftTextStyle.value = layer
        }
    }

    fun onCanvasLayerTapped(layerId: String) {
        selectLayer(layerId)
        if (_isMultiSelectMode.value) return
        val layer = findLayerRecursive(_document.value.layers, layerId)
        val currentTab = _activeToolDrawer.value
        when (layer) {
            is TextLayer -> {
                if (currentTab != ToolDrawerTab.COLOR &&
                    currentTab != ToolDrawerTab.FONT &&
                    currentTab != ToolDrawerTab.ROTATE &&
                    currentTab != ToolDrawerTab.CORNER_PIN &&
                    currentTab != ToolDrawerTab.THREE_D &&
                    currentTab != ToolDrawerTab.CROP &&
                    currentTab != ToolDrawerTab.FILTER
                ) {
                    _activeToolDrawer.value = ToolDrawerTab.TEXT
                }
            }
            is ImageLayer, is GroupLayer -> {
                if (currentTab == ToolDrawerTab.TEXT ||
                    currentTab == ToolDrawerTab.FONT
                ) {
                    _activeToolDrawer.value = ToolDrawerTab.NONE
                }
            }
            else -> Unit
        }
    }

    fun enterMultiSelectWithLayer(layerId: String) {
        lastAppliedCaseMode = null
        val current = _document.value
        val existingOrder = current.effectiveSelectionOrder.toMutableList()
        if (layerId !in existingOrder) {
            existingOrder.add(layerId)
        }
        _isMultiSelectMode.value = true
        _document.value = current.copy(
            selectedLayerId = layerId,
            selectionOrder = existingOrder
        )
        val layer = findLayerRecursive(_document.value.layers, layerId)
        if (layer is TextLayer) {
            fontRepository.selectFont(layer.fontId)
            _draftTextStyle.value = layer
        }
    }

    fun toggleMultiSelectLayer(layerId: String) {
        lastAppliedCaseMode = null
        val current = _document.value
        val existingOrder = current.effectiveSelectionOrder.toMutableList()
        if (layerId in existingOrder) {
            existingOrder.remove(layerId)
        } else {
            existingOrder.add(layerId)
        }
        if (existingOrder.isEmpty()) {
            _isMultiSelectMode.value = false
        } else if (existingOrder.size > 1) {
            _isMultiSelectMode.value = true
        }
        val nextPrimary = existingOrder.lastOrNull()
        _document.value = current.copy(
            selectedLayerId = nextPrimary,
            selectionOrder = existingOrder
        )
        val layer = findLayerRecursive(_document.value.layers, nextPrimary)
        if (layer is TextLayer) {
            fontRepository.selectFont(layer.fontId)
            _draftTextStyle.value = layer
        }
    }

    fun stagePickedPhotosForCrop(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (!canAddLayer(1)) return
        viewModelScope.launch {
            val doc = _document.value
            val staged = mutableListOf<PendingImportPhoto>()
            for (uri in uris) {
                if (!settings.value.unlimitedLayers && doc.layers.size + _pendingCropQueue.value.size + staged.size >= 5) {
                    _statusBanner.value = "Layer limit (5) reached."
                    break
                }
                val meta = mediaManager.importImage(
                    sourceUri = uri,
                    projectId = doc.id,
                    keepLocalCopy = settings.value.keepLocalMediaCopy
                )
                val bmp = mediaManager.decodeRgba8888Bitmap(meta.uriString, maxDimension = null)
                if (bmp != null) {
                    staged.add(
                        PendingImportPhoto(
                            uriString = meta.uriString,
                            displayName = meta.displayName.substringBeforeLast('.').take(18).ifBlank { "Photo" },
                            originalWidth = meta.width,
                            originalHeight = meta.height,
                            previewBitmap = bmp
                        )
                    )
                }
            }
            if (staged.isNotEmpty()) {
                _pendingCropQueue.value = _pendingCropQueue.value + staged
            }
        }
    }

    fun discardCurrentPendingCrop() {
        val queue = _pendingCropQueue.value
        if (queue.isEmpty()) return
        _pendingCropQueue.value = queue.drop(1)
    }

    fun commitCurrentPendingCrop(crop: ImageCropRect, cropShape: ImageCropShape) {
        val queue = _pendingCropQueue.value
        val current = queue.firstOrNull() ?: return
        _pendingCropQueue.value = queue.drop(1)
        if (!canAddLayer(1)) return

        val decodedMap = _imageBitmaps.value.toMutableMap()
        decodedMap[current.uriString] = current.previewBitmap
        _imageBitmaps.value = decodedMap

        val layer = ImageLayer(
            name = current.displayName,
            uriString = current.uriString,
            originalWidth = current.originalWidth,
            originalHeight = current.originalHeight,
            crop = crop.normalized(),
            cropShape = cropShape
        )
        commitChange { doc ->
            doc.copy(
                layers = doc.layers + layer,
                selectedLayerId = layer.id,
                selectionOrder = listOf(layer.id)
            )
        }
    }

    fun addImagesFromPicker(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (!canAddLayer(1)) return
        viewModelScope.launch {
            val doc = _document.value
            val importedLayers = mutableListOf<ImageLayer>()
            val decodedMap = _imageBitmaps.value.toMutableMap()

            for (uri in uris) {
                if (!settings.value.unlimitedLayers && doc.layers.size + importedLayers.size >= 5) {
                    _statusBanner.value = "Layer limit (5) reached."
                    break
                }
                val meta = mediaManager.importImage(
                    sourceUri = uri,
                    projectId = doc.id,
                    keepLocalCopy = settings.value.keepLocalMediaCopy
                )
                val bmp = mediaManager.decodeRgba8888Bitmap(meta.uriString, maxDimension = null)
                if (bmp != null) {
                    decodedMap[meta.uriString] = bmp
                }
                val stagger = importedLayers.size * 0.04f
                importedLayers.add(
                    ImageLayer(
                        name = meta.displayName.substringBeforeLast('.').take(18).ifBlank { "Photo" },
                        uriString = meta.uriString,
                        originalWidth = meta.width,
                        originalHeight = meta.height,
                        offsetXNorm = stagger,
                        offsetYNorm = stagger
                    )
                )
            }

            if (importedLayers.isNotEmpty()) {
                _imageBitmaps.value = decodedMap
                val addedLastId = importedLayers.last().id
                commitChange { current ->
                    current.copy(
                        layers = current.layers + importedLayers,
                        selectedLayerId = addedLastId,
                        selectionOrder = listOf(addedLastId)
                    )
                }
            }
        }
    }

    fun openTextToolDrawer() {
        val current = _document.value
        val selected = findLayerRecursive(current.layers, current.selectedLayerId)
        if (selected is TextLayer) {
            fontRepository.selectFont(selected.fontId)
            _draftTextStyle.value = selected
        } else {
            _draftText.value = ""
            _draftTextStyle.value = _draftTextStyle.value.copy(fontId = fontRepository.selectedFontId.value)
        }
        lastAppliedCaseMode = null
        _activeToolDrawer.value = ToolDrawerTab.TEXT
    }

    fun startNewTextDraft() {
        lastAppliedCaseMode = null
        _draftText.value = ""
        _draftTextStyle.value = TextLayer(
            text = "",
            fontId = fontRepository.selectedFontId.value
        )
        _document.value = _document.value.copy(
            selectedLayerId = null,
            selectionOrder = emptyList()
        )
        _activeToolDrawer.value = ToolDrawerTab.TEXT
    }

    fun updateDraftText(text: String) {
        lastAppliedCaseMode = null
        _draftText.value = text
    }

    fun updateDraftStyle(transform: (TextLayer) -> TextLayer) {
        _draftTextStyle.value = transform(_draftTextStyle.value)
    }

    fun confirmTextBox() {
        val current = _document.value
        val selected = findLayerRecursive(current.layers, current.selectedLayerId)
        if (selected is TextLayer) {
            saveProjectToHistory(silent = true)
            _activeToolDrawer.value = ToolDrawerTab.NONE
            _isMultiSelectMode.value = false
            _document.value = _document.value.copy(
                selectedLayerId = null,
                selectionOrder = emptyList()
            )
            return
        }

        val raw = _draftText.value
        if (raw.isBlank()) {
            // Do not add empty unconfirmed draft text
            _draftText.value = ""
            lastAppliedCaseMode = null
            _activeToolDrawer.value = ToolDrawerTab.NONE
            return
        }

        if (!canAddLayer()) return
        val style = _draftTextStyle.value
        val fontId = fontRepository.selectedFontId.value
        val isEmojiFont = fontRenderEngine.getMetadata(fontId)?.classification?.name?.contains("EMOJI") == true
        val resolvedText = raw.trimEnd()
        val newLayer = style.copy(
            id = UUID.randomUUID().toString(),
            name = resolvedText.take(14).ifBlank { "Text" },
            text = resolvedText,
            fontId = fontId,
            fontSizeSp = if (isEmojiFont && style.fontSizeSp < 48f) 52f else style.fontSizeSp
        )
        commitChange { doc ->
            doc.copy(
                layers = doc.layers + newLayer,
                selectedLayerId = newLayer.id,
                selectionOrder = listOf(newLayer.id)
            )
        }
        _draftText.value = ""
        lastAppliedCaseMode = null
        _activeToolDrawer.value = ToolDrawerTab.NONE
    }

    fun addTextLayer(initialText: String = "New Text"): String? {
        if (!canAddLayer()) return null
        val fontId = fontRepository.selectedFontId.value
        val isEmojiFont = fontRenderEngine.getMetadata(fontId)?.classification?.name?.contains("EMOJI") == true
        val textValue = if (isEmojiFont && initialText == "New Text") "😀🚀🌈✨" else initialText
        val newLayer = TextLayer(
            name = textValue.take(14).ifBlank { "Text" },
            text = textValue,
            fontId = fontId,
            fontSizeSp = if (isEmojiFont) 52f else 38f
        )
        commitChange { current ->
            current.copy(
                layers = current.layers + newLayer,
                selectedLayerId = newLayer.id,
                selectionOrder = listOf(newLayer.id)
            )
        }
        return newLayer.id
    }

    fun updateSelectedTextLayer(transform: (TextLayer) -> TextLayer) {
        val selId = _document.value.selectedLayerId
        val existing = findLayerRecursive(_document.value.layers, selId)
        if (selId == null || existing !is TextLayer) {
            updateDraftStyle(transform)
            return
        }
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, selId) { layer ->
                    if (layer is TextLayer) {
                        val updated = transform(layer)
                        updated.copy(name = updated.text.take(14).ifBlank { "Text" })
                    } else layer
                }
            )
        }
    }

    fun updateSelectedTextLayerTransient(transform: (TextLayer) -> TextLayer) {
        val selId = _document.value.selectedLayerId
        val existing = findLayerRecursive(_document.value.layers, selId)
        if (selId == null || existing !is TextLayer) {
            updateDraftStyle(transform)
            return
        }
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                if (layer is TextLayer) {
                    val updated = transform(layer)
                    updated.copy(name = updated.text.take(14).ifBlank { "Text" })
                } else layer
            }
        )
    }

    fun updateSelectedTextTransient(text: String) {
        lastAppliedCaseMode = null
        val selId = _document.value.selectedLayerId
        val existing = findLayerRecursive(_document.value.layers, selId)
        if (existing !is TextLayer || selId == null) {
            _draftText.value = text
            return
        }
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                if (layer is TextLayer) {
                    layer.copy(text = text, name = text.take(14).ifBlank { "Text" })
                } else layer
            }
        )
    }

    fun cycleTextCase(): TextCaseMode {
        val nextMode = when (lastAppliedCaseMode) {
            null -> TextCaseMode.LOWERCASE
            TextCaseMode.LOWERCASE -> TextCaseMode.UPPERCASE
            TextCaseMode.UPPERCASE -> TextCaseMode.TITLE_CASE
            TextCaseMode.TITLE_CASE -> TextCaseMode.LOWERCASE
        }
        lastAppliedCaseMode = nextMode

        val selId = _document.value.selectedLayerId
        val selected = findLayerRecursive(_document.value.layers, selId)
        if (selected is TextLayer && selId != null) {
            val transformed = TextCaseTransformer.apply(selected.text, nextMode)
            commitChange { doc ->
                doc.copy(
                    layers = mapLayerRecursive(doc.layers, selId) { layer ->
                        if (layer is TextLayer) {
                            layer.copy(
                                text = transformed,
                                name = transformed.take(14).ifBlank { "Text" }
                            )
                        } else layer
                    }
                )
            }
        } else {
            _draftText.value = TextCaseTransformer.apply(_draftText.value, nextMode)
        }
        return nextMode
    }

    fun ensureImageLayerSelectedForAdjust(): ImageLayer? {
        val doc = _document.value
        val current = findLayerRecursive(doc.layers, doc.selectedLayerId) as? ImageLayer
        if (current != null) return current
        val lastPhoto = collectImageLayers(doc.layers).lastOrNull()
        if (lastPhoto != null) {
            _document.value = doc.copy(
                selectedLayerId = lastPhoto.id,
                selectionOrder = listOf(lastPhoto.id)
            )
        }
        return lastPhoto
    }

    fun updateSelectedImageAdjustments(transform: (ImageAdjustments) -> ImageAdjustments) {
        val target = ensureImageLayerSelectedForAdjust() ?: return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, target.id) { layer ->
                if (layer is ImageLayer) {
                    layer.copy(adjustments = transform(layer.adjustments))
                } else layer
            }
        )
    }

    fun resetSelectedImageAdjustments() {
        val target = ensureImageLayerSelectedForAdjust() ?: return
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, target.id) { layer ->
                    if (layer is ImageLayer) {
                        layer.copy(adjustments = ImageAdjustments())
                    } else layer
                }
            )
        }
    }

    fun updateSelectedLayerCropTransient(transform: (ImageCropRect) -> ImageCropRect) {
        val selId = _document.value.selectedLayerId ?: return
        val target = findLayerRecursive(_document.value.layers, selId) ?: return
        if (target.locked) return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                if (!layer.locked) {
                    layer.withLiveCrop(liveCrop = transform(layer.liveCrop).normalized())
                } else layer
            }
        )
    }

    fun updateSelectedLayerCrop(transform: (ImageCropRect) -> ImageCropRect) {
        val selId = _document.value.selectedLayerId ?: return
        val target = findLayerRecursive(_document.value.layers, selId) ?: return
        if (target.locked) return
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, selId) { layer ->
                    if (!layer.locked) {
                        layer.withLiveCrop(liveCrop = transform(layer.liveCrop).normalized())
                    } else layer
                }
            )
        }
    }

    fun resetSelectedLayerCrop() {
        updateSelectedLayerCrop { ImageCropRect() }
    }

    fun updateSelectedImageCropTransient(transform: (ImageCropRect) -> ImageCropRect) {
        val selected = findLayerRecursive(_document.value.layers, _document.value.selectedLayerId)
        if (selected != null) {
            updateSelectedLayerCropTransient(transform)
            return
        }
        val target = ensureImageLayerSelectedForAdjust() ?: return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, target.id) { layer ->
                layer.withLiveCrop(liveCrop = transform(layer.liveCrop).normalized())
            }
        )
    }

    fun updateSelectedImageCrop(transform: (ImageCropRect) -> ImageCropRect) {
        val selected = findLayerRecursive(_document.value.layers, _document.value.selectedLayerId)
        if (selected != null) {
            updateSelectedLayerCrop(transform)
            return
        }
        val target = ensureImageLayerSelectedForAdjust() ?: return
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, target.id) { layer ->
                    layer.withLiveCrop(liveCrop = transform(layer.liveCrop).normalized())
                }
            )
        }
    }

    fun resetSelectedImageCrop() {
        updateSelectedImageCrop { ImageCropRect() }
    }

    fun setSelectedImageOpacity(opacity: Float) {
        val target = ensureImageLayerSelectedForAdjust() ?: return
        val clamped = opacity.coerceIn(0f, 1f)
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, target.id) { layer ->
                if (layer is ImageLayer) layer.copy(opacity = clamped) else layer
            }
        )
    }

    fun setSelectedLayerColorTransient(colorArgb: Long?) {
        val selId = _document.value.selectedLayerId ?: return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = applyColorTransformToLayers(_document.value.layers, selId, colorArgb)
        )
    }

    fun applyColorToSelectedLayer(colorArgb: Long?) {
        val selId = _document.value.selectedLayerId ?: return
        commitChange { doc ->
            doc.copy(layers = applyColorTransformToLayers(doc.layers, selId, colorArgb))
        }
    }

    private fun applyColorTransformToLayers(
        layers: List<EditorLayer>,
        layerId: String,
        colorArgb: Long?
    ): List<EditorLayer> {
        return mapLayerRecursive(layers, layerId) { layer ->
            when (layer) {
                is TextLayer -> layer.copy(colorArgb = colorArgb ?: 0xFFFFFFFFL)
                is ImageLayer -> layer.copy(tintColorArgb = colorArgb)
                is GroupLayer -> layer
            }
        }
    }

    fun applyFontGloballyAndToSelection(fontId: String) {
        fontRepository.selectFont(fontId)
        _draftTextStyle.value = _draftTextStyle.value.copy(fontId = fontId)
        val selId = _document.value.selectedLayerId
        val selected = findLayerRecursive(_document.value.layers, selId)
        if (selected is TextLayer && selId != null) {
            commitChange { doc ->
                doc.copy(
                    layers = mapLayerRecursive(doc.layers, selId) { layer ->
                        if (layer is TextLayer) layer.copy(fontId = fontId) else layer
                    }
                )
            }
        }
        syncPinnedFonts()
        refreshDiagnostics(includeDetails = false)
    }

    fun setEmojiSourceMode(mode: EmojiSourceMode) {
        val appFontId = _document.value.appEmojiFontId
            ?: settings.value.appEmojiFontId
            ?: fontsRecentToOld.value.firstOrNull()?.id
        commitChange { doc ->
            doc.copy(
                emojiSourceMode = mode,
                appEmojiFontId = appFontId
            )
        }
        viewModelScope.launch {
            settingsRepository.setEmojiSource(mode, appFontId)
        }
    }

    fun setAppEmojiFont(fontId: String) {
        commitChange { doc ->
            doc.copy(
                emojiSourceMode = EmojiSourceMode.APPLICATION,
                appEmojiFontId = fontId
            )
        }
        viewModelScope.launch {
            settingsRepository.setEmojiSource(EmojiSourceMode.APPLICATION, fontId)
        }
    }

    fun transformLayerTransient(
        layerId: String,
        deltaXNorm: Float,
        deltaYNorm: Float,
        scaleFactor: Float,
        rotationDelta: Float,
        stretchXFactor: Float = 1f,
        stretchYFactor: Float = 1f
    ) {
        val locks = _transformLocks.value
        val effDx = if (locks.positionLocked) 0f else deltaXNorm
        val effDy = if (locks.positionLocked) 0f else deltaYNorm
        val effScaleFactor = if (locks.zoomLocked) 1f else scaleFactor
        val effRotDelta = if (locks.rotationLocked) 0f else rotationDelta
        val effStretchX = if (locks.stretchLocked) 1f else stretchXFactor
        val effStretchY = if (locks.stretchLocked) 1f else stretchYFactor

        if (effDx == 0f && effDy == 0f && effScaleFactor == 1f && effRotDelta == 0f && effStretchX == 1f && effStretchY == 1f) {
            return
        }
        val targetLayer = findLayerRecursive(_document.value.layers, layerId) ?: return
        if (targetLayer.locked) return

        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, layerId) { layer ->
                if (!layer.locked) {
                    layer.withBaseTransform(
                        offsetXNorm = (layer.offsetXNorm + effDx).coerceIn(-0.98f, 0.98f),
                        offsetYNorm = (layer.offsetYNorm + effDy).coerceIn(-0.98f, 0.98f),
                        scale = (layer.scale * effScaleFactor).coerceIn(0.05f, 100f),
                        stretchX = (layer.stretchX * effStretchX).coerceIn(0.15f, 8f),
                        stretchY = (layer.stretchY * effStretchY).coerceIn(0.15f, 8f),
                        rotationDeg = (layer.rotationDeg + effRotDelta) % 360f
                    )
                } else layer
            }
        )
    }

    fun transformLayerAnchoredStretch(
        layerId: String,
        signX: Float,
        signY: Float,
        screenLocalDeltaX: Float,
        screenLocalDeltaY: Float,
        boxWpx: Float,
        boxHpx: Float,
        canvasWpx: Float,
        canvasHpx: Float
    ) {
        if (_transformLocks.value.stretchLocked) return
        val targetLayer = findLayerRecursive(_document.value.layers, layerId) ?: return
        if (targetLayer.locked) return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, layerId) { layer ->
                if (layer.locked) return@mapLayerRecursive layer
                computeAnchoredStretchTransform(
                    layer = layer,
                    signX = signX,
                    signY = signY,
                    screenLocalDeltaX = screenLocalDeltaX,
                    screenLocalDeltaY = screenLocalDeltaY,
                    boxWpx = boxWpx,
                    boxHpx = boxHpx,
                    canvasWpx = canvasWpx,
                    canvasHpx = canvasHpx
                )
            }
        )
    }

    fun updateSelectedTextWrapWidthTransient(
        layerId: String,
        signX: Float,
        screenLocalDeltaX: Float,
        boxWpx: Float,
        canvasWpx: Float,
        canvasHpx: Float
    ) {
        val targetLayer = findLayerRecursive(_document.value.layers, layerId) as? TextLayer ?: return
        if (targetLayer.locked) return
        val safeCanvasW = canvasWpx.coerceAtLeast(1f)
        val safeCanvasH = canvasHpx.coerceAtLeast(1f)
        val minWrapNorm = (44f / safeCanvasW).coerceIn(0.08f, 0.25f)
        val maxWrapNorm = 0.92f
        val currentWrapWpx = targetLayer.wrapWidthNorm?.let { it * safeCanvasW } ?: boxWpx
        val scaleXEff = (targetLayer.scale * targetLayer.stretchX).coerceAtLeast(0.05f)
        val deltaBoxPx = (screenLocalDeltaX / scaleXEff) * signX
        val nextWrapPx = (currentWrapWpx + deltaBoxPx).coerceIn(minWrapNorm * safeCanvasW, maxWrapNorm * safeCanvasW)
        val actualDeltaDispW = (nextWrapPx - currentWrapWpx) * scaleXEff
        val shiftLocalX = signX * (actualDeltaDispW * 0.5f)
        val rad = Math.toRadians(targetLayer.rotationDeg.toDouble())
        val dxCanvasPx = (shiftLocalX * cos(rad)).toFloat()
        val dyCanvasPx = (shiftLocalX * sin(rad)).toFloat()
        val nextWrapNorm = (nextWrapPx / safeCanvasW).coerceIn(minWrapNorm, maxWrapNorm)
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, layerId) { layer ->
                if (layer is TextLayer && !layer.locked) {
                    layer.withTextWrapWidth(nextWrapNorm).withBaseTransform(
                        offsetXNorm = (layer.offsetXNorm + dxCanvasPx / safeCanvasW).coerceIn(-0.98f, 0.98f),
                        offsetYNorm = (layer.offsetYNorm + dyCanvasPx / safeCanvasH).coerceIn(-0.98f, 0.98f)
                    )
                } else layer
            }
        )
    }

    fun toggleCornerPinAddPointMode() {
        _isCornerPinAddPointMode.value = !_isCornerPinAddPointMode.value
    }

    fun addPuppetPinToLayer(layerId: String, anchorX: Float, anchorY: Float) {
        val targetLayer = findLayerRecursive(_document.value.layers, layerId) ?: return
        if (targetLayer.locked) return
        val newPin = PuppetPinPoint(
            anchorX = anchorX.coerceIn(0.02f, 0.98f),
            anchorY = anchorY.coerceIn(0.02f, 0.98f)
        )
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, layerId) { layer ->
                    if (layer.locked) layer
                    else {
                        val nextPins = (layer.cornerPin.puppetPins + newPin).takeLast(12)
                        layer.withExtraTransforms(cornerPin = layer.cornerPin.copy(puppetPins = nextPins))
                    }
                }
            )
        }
    }

    fun updatePuppetPinDragTransient(
        layerId: String,
        pinId: String,
        deltaDxRatio: Float,
        deltaDyRatio: Float
    ) {
        val targetLayer = findLayerRecursive(_document.value.layers, layerId) ?: return
        if (targetLayer.locked) return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, layerId) { layer ->
                if (layer.locked) layer
                else {
                    val updatedPins = layer.cornerPin.puppetPins.map { pin ->
                        if (pin.id == pinId) {
                            pin.copy(
                                dx = (pin.dx + deltaDxRatio).coerceIn(-1.2f, 1.2f),
                                dy = (pin.dy + deltaDyRatio).coerceIn(-1.2f, 1.2f)
                            )
                        } else pin
                    }
                    layer.withExtraTransforms(cornerPin = layer.cornerPin.copy(puppetPins = updatedPins))
                }
            }
        )
    }

    fun clearPuppetPins(layerId: String) {
        val targetLayer = findLayerRecursive(_document.value.layers, layerId) ?: return
        if (targetLayer.locked || targetLayer.cornerPin.puppetPins.isEmpty()) return
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, layerId) { layer ->
                    layer.withExtraTransforms(cornerPin = layer.cornerPin.copy(puppetPins = emptyList()))
                }
            )
        }
    }

    fun setSelectedLayerBlendMode(mode: LayerBlendMode) {
        val selId = _document.value.selectedLayerId ?: return
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, selId) { layer ->
                    layer.withFilterStyle(blendMode = mode)
                }
            )
        }
    }

    fun updateSelectedLayerEffectStyle(
        transform: (LayerEffectStyle) -> LayerEffectStyle,
        transient: Boolean = false
    ) {
        val selId = _document.value.selectedLayerId ?: return
        if (transient) {
            beginTransformTransaction()
            _document.value = _document.value.copy(
                layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                    layer.withFilterStyle(effectStyle = transform(layer.effectStyle))
                }
            )
        } else {
            commitChange { doc ->
                doc.copy(
                    layers = mapLayerRecursive(doc.layers, selId) { layer ->
                        layer.withFilterStyle(effectStyle = transform(layer.effectStyle))
                    }
                )
            }
        }
    }

    fun setSelectedLayerFillColor(colorArgb: Long?, transient: Boolean = false) {
        val selId = _document.value.selectedLayerId ?: return
        if (transient) {
            beginTransformTransaction()
            _document.value = _document.value.copy(
                layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                    layer.withFilterStyle(fillStyle = layer.fillStyle.copy(fillColorArgb = colorArgb))
                }
            )
        } else {
            commitChange { doc ->
                doc.copy(
                    layers = mapLayerRecursive(doc.layers, selId) { layer ->
                        layer.withFilterStyle(fillStyle = layer.fillStyle.copy(fillColorArgb = colorArgb))
                    }
                )
            }
        }
    }

    fun setEditingFillMedia(editing: Boolean) {
        val selId = _document.value.selectedLayerId
        val selected = findLayerRecursive(_document.value.layers, selId)
        _isEditingFillMedia.value = editing && selected != null && !selected.fillStyle.fillMediaUri.isNullOrBlank()
    }

    fun applyFillMediaFromUri(uri: Uri) {
        val selId = _document.value.selectedLayerId ?: return
        val doc = _document.value
        viewModelScope.launch {
            val meta = mediaManager.importImage(
                sourceUri = uri,
                projectId = doc.id,
                keepLocalCopy = settings.value.keepLocalMediaCopy
            )
            val bmp = mediaManager.decodeRgba8888Bitmap(meta.uriString, maxDimension = null)
            if (bmp != null) {
                val nextMap = _imageBitmaps.value.toMutableMap()
                nextMap[meta.uriString] = bmp
                _imageBitmaps.value = nextMap
            }
            commitChange { current ->
                current.copy(
                    layers = mapLayerRecursive(current.layers, selId) { layer ->
                        layer.withFilterStyle(
                            fillStyle = layer.fillStyle.copy(
                                fillMediaUri = meta.uriString,
                                fillMediaOffsetX = 0f,
                                fillMediaOffsetY = 0f,
                                fillMediaScale = 1f,
                                fillMediaRotationDeg = 0f
                            )
                        )
                    }
                )
            }
            _isEditingFillMedia.value = true
        }
    }

    fun clearSelectedLayerFillMedia() {
        val selId = _document.value.selectedLayerId ?: return
        _isEditingFillMedia.value = false
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, selId) { layer ->
                    layer.withFilterStyle(
                        fillStyle = layer.fillStyle.copy(
                            fillMediaUri = null,
                            fillMediaOffsetX = 0f,
                            fillMediaOffsetY = 0f,
                            fillMediaScale = 1f,
                            fillMediaRotationDeg = 0f
                        )
                    )
                }
            )
        }
    }

    fun transformSelectedLayerFillMediaTransient(
        dxRatio: Float,
        dyRatio: Float,
        scaleFactor: Float,
        rotDeltaDeg: Float
    ) {
        val selId = _document.value.selectedLayerId ?: return
        val locks = _transformLocks.value
        val effDx = if (locks.positionLocked) 0f else dxRatio
        val effDy = if (locks.positionLocked) 0f else dyRatio
        val effScale = if (locks.zoomLocked) 1f else scaleFactor
        val effRot = if (locks.rotationLocked) 0f else rotDeltaDeg
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                val f = layer.fillStyle
                if (f.fillMediaUri.isNullOrBlank()) layer
                else layer.withFilterStyle(
                    fillStyle = f.copy(
                        fillMediaOffsetX = (f.fillMediaOffsetX + effDx).coerceIn(-1.5f, 1.5f),
                        fillMediaOffsetY = (f.fillMediaOffsetY + effDy).coerceIn(-1.5f, 1.5f),
                        fillMediaScale = (f.fillMediaScale * effScale).coerceIn(0.2f, 8f),
                        fillMediaRotationDeg = (f.fillMediaRotationDeg + effRot) % 360f
                    )
                )
            }
        )
    }

    fun updateSelectedLayerCornerPinTransient(
        layerId: String,
        cornerIndex: Int,
        deltaDxRatio: Float,
        deltaDyRatio: Float
    ) {
        val targetLayer = findLayerRecursive(_document.value.layers, layerId) ?: return
        if (targetLayer.locked) return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, layerId) { layer ->
                if (layer.locked) return@mapLayerRecursive layer
                val cp = layer.cornerPin
                val nextCp = when (cornerIndex) {
                    0 -> cp.copy(
                        tlDx = (cp.tlDx + deltaDxRatio).coerceIn(-1.6f, 1.6f),
                        tlDy = (cp.tlDy + deltaDyRatio).coerceIn(-1.6f, 1.6f)
                    )
                    1 -> cp.copy(
                        trDx = (cp.trDx + deltaDxRatio).coerceIn(-1.6f, 1.6f),
                        trDy = (cp.trDy + deltaDyRatio).coerceIn(-1.6f, 1.6f)
                    )
                    2 -> cp.copy(
                        brDx = (cp.brDx + deltaDxRatio).coerceIn(-1.6f, 1.6f),
                        brDy = (cp.brDy + deltaDyRatio).coerceIn(-1.6f, 1.6f)
                    )
                    3 -> cp.copy(
                        blDx = (cp.blDx + deltaDxRatio).coerceIn(-1.6f, 1.6f),
                        blDy = (cp.blDy + deltaDyRatio).coerceIn(-1.6f, 1.6f)
                    )
                    else -> cp
                }
                layer.withExtraTransforms(cornerPin = nextCp)
            }
        )
    }

    fun updateSelectedLayer3DTransient(transform: (Layer3DTransform) -> Layer3DTransform) {
        val selId = _document.value.selectedLayerId ?: return
        val existing = findLayerRecursive(_document.value.layers, selId) ?: return
        if (existing.locked) return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                if (layer.locked) layer else layer.withExtraTransforms(transform3D = transform(layer.transform3D))
            }
        )
    }

    fun moveSelectedLayerXYTransient(deltaXNorm: Float, deltaYNorm: Float) {
        val selId = _document.value.selectedLayerId ?: return
        val existing = findLayerRecursive(_document.value.layers, selId) ?: return
        if (existing.locked) return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                if (layer.locked) layer else layer.withBaseTransform(
                    offsetXNorm = (layer.offsetXNorm + deltaXNorm).coerceIn(-0.98f, 0.98f),
                    offsetYNorm = (layer.offsetYNorm + deltaYNorm).coerceIn(-0.98f, 0.98f)
                )
            }
        )
    }

    fun scaleSelectedLayer3DTransient(
        scaleFactor: Float? = null,
        stretchXFactor: Float? = null,
        stretchYFactor: Float? = null
    ) {
        val selId = _document.value.selectedLayerId ?: return
        val existing = findLayerRecursive(_document.value.layers, selId) ?: return
        if (existing.locked) return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                if (layer.locked) layer else layer.withBaseTransform(
                    scale = if (scaleFactor != null) (layer.scale * scaleFactor).coerceIn(0.05f, 100f) else layer.scale,
                    stretchX = if (stretchXFactor != null) (layer.stretchX * stretchXFactor).coerceIn(0.15f, 8f) else layer.stretchX,
                    stretchY = if (stretchYFactor != null) (layer.stretchY * stretchYFactor).coerceIn(0.15f, 8f) else layer.stretchY
                )
            }
        )
    }

    fun resetSelectedLayer3DMode(modeIndex: Int) {
        val selId = _document.value.selectedLayerId ?: return
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, selId) { layer ->
                    if (layer.locked) return@mapLayerRecursive layer
                    when (modeIndex) {
                        0 -> layer.withBaseTransform(offsetXNorm = 0f, offsetYNorm = 0f)
                            .withExtraTransforms(transform3D = layer.transform3D.copy(positionZ = 0f))
                        1 -> layer.withBaseTransform(rotationDeg = 0f)
                            .withExtraTransforms(transform3D = layer.transform3D.copy(rotationXDeg = 0f, rotationYDeg = 0f))
                        2 -> layer.withBaseTransform(scale = 1f, stretchX = 1f, stretchY = 1f)
                        3 -> layer.withExtraTransforms(transform3D = layer.transform3D.copy(skewXDeg = 0f, skewYDeg = 0f))
                        else -> layer
                    }
                }
            )
        }
    }

    fun centerSelectedLayer() {
        val selId = _document.value.selectedLayerId ?: return
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, selId) { layer ->
                    if (layer.locked) layer else layer.withBaseTransform(offsetXNorm = 0f, offsetYNorm = 0f)
                }
            )
        }
    }

    fun resetAllSelectedLayer3D() {
        val selId = _document.value.selectedLayerId ?: return
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, selId) { layer ->
                    if (layer.locked) layer else layer.withBaseTransform(
                        offsetXNorm = 0f,
                        offsetYNorm = 0f,
                        scale = 1f,
                        stretchX = 1f,
                        stretchY = 1f,
                        rotationDeg = 0f
                    ).withExtraTransforms(transform3D = Layer3DTransform())
                }
            )
        }
    }

    fun setSelectedLayerRotation(deg: Float) {
        val selId = _document.value.selectedLayerId ?: return
        val existing = findLayerRecursive(_document.value.layers, selId) ?: return
        if (existing.locked) return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                if (!layer.locked) layer.withBaseTransform(rotationDeg = deg) else layer
            }
        )
    }

    fun rotateSelectedLayerBy(deltaDeg: Float) {
        val selId = _document.value.selectedLayerId ?: return
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, selId) { layer ->
                    if (!layer.locked) {
                        layer.withBaseTransform(rotationDeg = (layer.rotationDeg + deltaDeg) % 360f)
                    } else layer
                }
            )
        }
    }

    fun setSelectedLayerScale(scale: Float) {
        val selId = _document.value.selectedLayerId ?: return
        val existing = findLayerRecursive(_document.value.layers, selId) ?: return
        if (existing.locked) return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                if (!layer.locked) layer.withBaseTransform(scale = scale.coerceIn(0.05f, 100f)) else layer
            }
        )
    }

    fun setSelectedLayerOpacity(opacity: Float) {
        val selId = _document.value.selectedLayerId ?: return
        findLayerRecursive(_document.value.layers, selId) ?: return
        beginTransformTransaction()
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                layer.withBaseTransform(opacity = opacity.coerceIn(0f, 1f))
            }
        )
    }

    fun toggleLayerVisibility(layerId: String) {
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, layerId) {
                    it.withBaseTransform(visible = !it.visible)
                }
            )
        }
    }

    fun toggleLayerLock(layerId: String) {
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, layerId) {
                    it.withBaseTransform(locked = !it.locked)
                }
            )
        }
    }

    /**
     * Moves a layer forward (+1) or backward (-1) in its stack (top-level or inside its parent GroupLayer).
     */
    fun moveLayerOrder(layerId: String, direction: Int) {
        if (direction == 0) return
        commitChange { doc ->
            doc.copy(layers = moveLayerInList(doc.layers, layerId, direction))
        }
    }

    /**
     * 3-icon Layer Arrangement system based on Position & Selection Order.
     */
    fun arrangeSelectedToTop() {
        commitChange { doc -> applyArrangementToTop(doc) }
    }

    fun arrangeSelectedToBottom() {
        commitChange { doc -> applyArrangementToBottom(doc) }
    }

    fun swapOrReverseSelectedArrangement() {
        commitChange { doc -> applySwapOrReverseArrangement(doc) }
    }

    /**
     * Non-destructive Group / Expand / Collapse:
     * - If 2+ top-level layers are selected -> wraps them into a GroupLayer without flattening/mutating properties.
     * - If a GroupLayer (or child inside an expanded GroupLayer) is selected -> expands/collapses it.
     */
    fun handleGroupAction() {
        val doc = _document.value
        val expandedGroup = doc.layers.filterIsInstance<GroupLayer>().firstOrNull { it.isExpanded }
        if (expandedGroup != null) {
            toggleGroupExpanded(expandedGroup.id)
            return
        }

        val order = doc.effectiveSelectionOrder
        val primary = findLayerRecursive(doc.layers, doc.selectedLayerId)
        if (primary is GroupLayer && order.size <= 1) {
            toggleGroupExpanded(primary.id)
            return
        }

        if (order.size >= 2) {
            val selectedSet = order.toSet()
            val topSelected = doc.layers.filter { it.id in selectedSet }
            if (topSelected.size >= 2) {
                commitChange { current -> createGroupFromSelection(current, selectedSet) }
                _isMultiSelectMode.value = false
                return
            }
        }

        if (primary is GroupLayer) {
            toggleGroupExpanded(primary.id)
        } else {
            _statusBanner.value = "Select 2+ layers to group, or select a Group to expand."
        }
    }

    fun toggleGroupExpanded(groupId: String) {
        val current = _document.value
        val group = current.layers.filterIsInstance<GroupLayer>().firstOrNull { it.id == groupId } ?: return
        val nextExpanded = !group.isExpanded
        _isMultiSelectMode.value = false
        _document.value = current.copy(
            layers = current.layers.map { layer ->
                if (layer is GroupLayer) {
                    if (layer.id == groupId) layer.copy(isExpanded = nextExpanded)
                    else layer.copy(isExpanded = false)
                } else layer
            },
            selectedLayerId = if (nextExpanded) {
                group.children.lastOrNull()?.id ?: groupId
            } else {
                groupId
            },
            selectionOrder = if (nextExpanded) {
                listOfNotNull(group.children.lastOrNull()?.id ?: groupId)
            } else {
                listOf(groupId)
            }
        )
    }

    fun ungroupLayer(groupId: String) {
        val group = _document.value.layers.filterIsInstance<GroupLayer>().firstOrNull { it.id == groupId } ?: return
        commitChange { doc -> ungroupInDocument(doc, group.id) }
        _isMultiSelectMode.value = false
    }

    fun duplicateLayer(layerId: String) {
        if (!canAddLayer()) return
        val target = findLayerRecursive(_document.value.layers, layerId) ?: return
        val copied = cloneLayerWithNewIds(target, shiftOffset = 0.05f)
        commitChange { doc ->
            doc.copy(
                layers = doc.layers + copied,
                selectedLayerId = copied.id,
                selectionOrder = listOf(copied.id)
            )
        }
    }

    private fun cloneLayerWithNewIds(target: EditorLayer, shiftOffset: Float): EditorLayer {
        val copyId = UUID.randomUUID().toString()
        return when (target) {
            is ImageLayer -> target.copy(
                id = copyId,
                offsetXNorm = target.offsetXNorm + shiftOffset,
                offsetYNorm = target.offsetYNorm + shiftOffset
            )
            is TextLayer -> target.copy(
                id = copyId,
                offsetXNorm = target.offsetXNorm + shiftOffset,
                offsetYNorm = target.offsetYNorm + shiftOffset
            )
            is GroupLayer -> target.copy(
                id = copyId,
                children = target.children.map { cloneLayerWithNewIds(it, shiftOffset = 0f) },
                offsetXNorm = target.offsetXNorm + shiftOffset,
                offsetYNorm = target.offsetYNorm + shiftOffset
            )
        }
    }

    fun deleteLayer(layerId: String) {
        commitChange { doc ->
            val remaining = removeLayerRecursive(doc.layers, layerId)
            val newOrder = doc.effectiveSelectionOrder.filter { findLayerRecursive(remaining, it) != null }
            doc.copy(
                layers = remaining,
                selectedLayerId = newOrder.lastOrNull() ?: remaining.lastOrNull()?.id,
                selectionOrder = newOrder.ifEmpty { listOfNotNull(remaining.lastOrNull()?.id) }
            )
        }
    }

    fun selectCanvasPreset(preset: CanvasDimensions) {
        commitChange { doc ->
            doc.copy(canvas = preset)
        }
    }

    fun selectSquareCanvas() {
        selectCanvasPreset(CanvasDimensions.SQUARE_1_1)
    }

    fun applyCustomCanvas(ratioInput: String, pixelInput: String, preferPixels: Boolean): Boolean {
        val parsed = if (preferPixels) {
            CanvasDimensions.fromPixelInput(pixelInput) ?: CanvasDimensions.fromRatioInput(ratioInput)
        } else {
            CanvasDimensions.fromRatioInput(ratioInput) ?: CanvasDimensions.fromPixelInput(pixelInput)
        }
        if (parsed == null) {
            _statusBanner.value = "Invalid format. Use e.g. 9:16 or 1080x1920"
            return false
        }
        commitChange { doc ->
            doc.copy(canvas = parsed.copy(isCustom = true))
        }
        return true
    }

    fun updateBackground(transform: (BackgroundConfig) -> BackgroundConfig) {
        commitChange { doc ->
            doc.copy(background = transform(doc.background))
        }
    }

    fun saveProjectToHistory(silent: Boolean = false) {
        viewModelScope.launch {
            val doc = _document.value
            val thumbPath = mediaManager.saveProjectThumbnail(doc, fontRenderEngine)
            projectRepository.saveDocument(doc, thumbPath)
            if (!silent) {
                _statusBanner.value = "Saved"
            }
        }
    }

    fun exportProjectWithOptions(
        format: ExportImageFormat,
        preset: ExportResolutionPreset,
        customWidth: Int,
        customHeight: Int,
        shareImmediately: Boolean = false
    ) {
        viewModelScope.launch {
            _exportFeedback.value = ExportFeedbackState.Exporting
            val safeCustomW = customWidth.coerceIn(64, 8192)
            val safeCustomH = customHeight.coerceIn(64, 8192)
            settingsRepository.setExportPreferences(
                format = format,
                preset = preset,
                customWidth = safeCustomW,
                customHeight = safeCustomH
            )
            val doc = _document.value
            val (targetW, targetH) = MediaStorageManager.computeTargetResolution(
                canvas = doc.canvas,
                preset = preset,
                customWidth = safeCustomW,
                customHeight = safeCustomH
            )
            val thumbPath = mediaManager.saveProjectThumbnail(doc, fontRenderEngine)
            projectRepository.saveDocument(doc, thumbPath)

            val res = mediaManager.exportImageToAppAndGallery(
                document = doc,
                fontRenderEngine = fontRenderEngine,
                format = format,
                targetWidth = targetW,
                targetHeight = targetH
            )
            res.onSuccess { (file, galleryUri) ->
                val destText = if (galleryUri != null) "Pictures/PixelForge" else "App Exports"
                _exportFeedback.value = ExportFeedbackState.Success(
                    file = file,
                    message = "Saved ${format.label} (${targetW}×${targetH}px) to $destText",
                    shareImmediately = shareImmediately
                )
            }.onFailure { err ->
                _exportFeedback.value = ExportFeedbackState.Error(err.localizedMessage ?: "Export failed")
            }
            refreshDiagnostics(includeDetails = false)
        }
    }

    fun exportProjectPng(shareImmediately: Boolean = false) {
        val s = settings.value
        exportProjectWithOptions(
            format = s.exportFormat,
            preset = s.exportResolutionPreset,
            customWidth = s.exportCustomWidth,
            customHeight = s.exportCustomHeight,
            shareImmediately = shareImmediately
        )
    }

    fun clearExportFeedback() {
        _exportFeedback.value = ExportFeedbackState.Idle
    }

    fun importFontFiles(uris: List<Uri>) {
        viewModelScope.launch {
            fontRepository.importFontUris(uris)
            syncPinnedFonts()
            refreshDiagnostics(includeDetails = false)
        }
    }

    fun importFontArchive(uri: Uri) {
        viewModelScope.launch {
            fontRepository.importArchiveUri(uri)
            syncPinnedFonts()
            refreshDiagnostics(includeDetails = false)
        }
    }

    fun deleteImportedFont(fontId: String) {
        viewModelScope.launch {
            fontRepository.deleteImportedFont(fontId)
            syncPinnedFonts()
            refreshDiagnostics(includeDetails = false)
        }
    }

    companion object {
        fun findLayerRecursive(layers: List<EditorLayer>, id: String?): EditorLayer? {
            if (id == null) return null
            for (layer in layers) {
                if (layer.id == id) return layer
                if (layer is GroupLayer) {
                    findLayerRecursive(layer.children, id)?.let { return it }
                }
            }
            return null
        }

        fun mapLayerRecursive(
            layers: List<EditorLayer>,
            id: String,
            transform: (EditorLayer) -> EditorLayer
        ): List<EditorLayer> {
            return layers.map { layer ->
                if (layer.id == id) {
                    transform(layer)
                } else if (layer is GroupLayer) {
                    layer.copy(children = mapLayerRecursive(layer.children, id, transform))
                } else {
                    layer
                }
            }
        }

        fun removeLayerRecursive(layers: List<EditorLayer>, id: String): List<EditorLayer> {
            return layers.mapNotNull { layer ->
                when {
                    layer.id == id -> null
                    layer is GroupLayer -> {
                        val remainingChildren = removeLayerRecursive(layer.children, id)
                        if (remainingChildren.isEmpty()) null
                        else layer.copy(children = remainingChildren)
                    }
                    else -> layer
                }
            }
        }

        fun moveLayerInList(layers: List<EditorLayer>, layerId: String, direction: Int): List<EditorLayer> {
            val topIdx = layers.indexOfFirst { it.id == layerId }
            if (topIdx >= 0) {
                val mutable = layers.toMutableList()
                val targetIdx = (topIdx + direction).coerceIn(0, mutable.lastIndex)
                if (targetIdx == topIdx) return layers
                val item = mutable.removeAt(topIdx)
                mutable.add(targetIdx, item)
                return mutable
            }
            return layers.map { layer ->
                if (layer is GroupLayer) {
                    layer.copy(children = moveLayerInList(layer.children, layerId, direction))
                } else layer
            }
        }

        fun applyArrangementToTop(doc: ProjectDocument): ProjectDocument {
            val order = doc.effectiveSelectionOrder
            if (order.isEmpty()) return doc
            val expandedGroup = doc.layers.filterIsInstance<GroupLayer>().firstOrNull { it.isExpanded }
            if (expandedGroup != null && order.all { id -> expandedGroup.children.any { it.id == id } }) {
                val newChildren = reorderListToTopBySelectionOrder(expandedGroup.children, order)
                return doc.copy(
                    layers = doc.layers.map { if (it.id == expandedGroup.id) expandedGroup.copy(children = newChildren) else it }
                )
            }
            val topOrder = order.filter { id -> doc.layers.any { it.id == id } }
            if (topOrder.isEmpty()) return doc
            return doc.copy(layers = reorderListToTopBySelectionOrder(doc.layers, topOrder))
        }

        fun applyArrangementToBottom(doc: ProjectDocument): ProjectDocument {
            val order = doc.effectiveSelectionOrder
            if (order.isEmpty()) return doc
            val expandedGroup = doc.layers.filterIsInstance<GroupLayer>().firstOrNull { it.isExpanded }
            if (expandedGroup != null && order.all { id -> expandedGroup.children.any { it.id == id } }) {
                val newChildren = reorderListToBottomBySelectionOrder(expandedGroup.children, order)
                return doc.copy(
                    layers = doc.layers.map { if (it.id == expandedGroup.id) expandedGroup.copy(children = newChildren) else it }
                )
            }
            val topOrder = order.filter { id -> doc.layers.any { it.id == id } }
            if (topOrder.isEmpty()) return doc
            return doc.copy(layers = reorderListToBottomBySelectionOrder(doc.layers, topOrder))
        }

        fun applySwapOrReverseArrangement(doc: ProjectDocument): ProjectDocument {
            val order = doc.effectiveSelectionOrder
            if (order.isEmpty()) return doc
            val expandedGroup = doc.layers.filterIsInstance<GroupLayer>().firstOrNull { it.isExpanded }
            if (expandedGroup != null && order.all { id -> expandedGroup.children.any { it.id == id } }) {
                val (newChildren, newOrder) = swapOrReverseInList(expandedGroup.children, order)
                return doc.copy(
                    layers = doc.layers.map { if (it.id == expandedGroup.id) expandedGroup.copy(children = newChildren) else it },
                    selectionOrder = newOrder,
                    selectedLayerId = newOrder.firstOrNull() ?: doc.selectedLayerId
                )
            }
            val topOrder = order.filter { id -> doc.layers.any { it.id == id } }
            if (topOrder.isEmpty()) return doc
            val (newLayers, newOrder) = swapOrReverseInList(doc.layers, topOrder)
            return doc.copy(
                layers = newLayers,
                selectionOrder = newOrder,
                selectedLayerId = newOrder.firstOrNull() ?: doc.selectedLayerId
            )
        }

        private fun reorderListToTopBySelectionOrder(
            list: List<EditorLayer>,
            selectionOrder: List<String>
        ): List<EditorLayer> {
            val byId = list.associateBy { it.id }
            val selectedSet = selectionOrder.toSet()
            val unselected = list.filterNot { it.id in selectedSet }
            // Bottom -> Top order: later-selected sits beneath earlier-selected, 1st selected is on very top!
            val orderedSelectedBottomToTop = selectionOrder.reversed().mapNotNull { byId[it] }
            return unselected + orderedSelectedBottomToTop
        }

        private fun reorderListToBottomBySelectionOrder(
            list: List<EditorLayer>,
            selectionOrder: List<String>
        ): List<EditorLayer> {
            val byId = list.associateBy { it.id }
            val selectedSet = selectionOrder.toSet()
            val unselected = list.filterNot { it.id in selectedSet }
            val orderedSelectedBottomToTop = selectionOrder.reversed().mapNotNull { byId[it] }
            return orderedSelectedBottomToTop + unselected
        }

        private fun swapOrReverseInList(
            list: List<EditorLayer>,
            selectionOrder: List<String>
        ): Pair<List<EditorLayer>, List<String>> {
            if (list.size < 2) return list to selectionOrder
            if (selectionOrder.size == 1) {
                val id = selectionOrder.first()
                val idx = list.indexOfFirst { it.id == id }
                if (idx < 0) return list to selectionOrder
                val targetIdx = if (idx < list.lastIndex) idx + 1 else idx - 1
                val mutable = list.toMutableList()
                val temp = mutable[idx]
                mutable[idx] = mutable[targetIdx]
                mutable[targetIdx] = temp
                return mutable to selectionOrder
            }

            val reversedSelectionOrder = selectionOrder.reversed()
            val selectedSet = selectionOrder.toSet()
            val byId = list.associateBy { it.id }
            val occupiedIndices = list.indices.filter { list[it].id in selectedSet }
            // Place reversedSelectionOrder so its first element takes the highest occupied slot
            val bottomToTopForSlots = reversedSelectionOrder.reversed().mapNotNull { byId[it] }
            val mutable = list.toMutableList()
            occupiedIndices.forEachIndexed { slotOffset, listIndex ->
                mutable[listIndex] = bottomToTopForSlots[slotOffset]
            }
            return mutable to reversedSelectionOrder
        }

        fun createGroupFromSelection(doc: ProjectDocument, selectedSet: Set<String>): ProjectDocument {
            val topSelected = doc.layers.filter { it.id in selectedSet }
            if (topSelected.size < 2) return doc
            val lastSelectedIdx = doc.layers.indexOfLast { it.id in selectedSet }
            val remainingBefore = doc.layers.subList(0, lastSelectedIdx + 1).filterNot { it.id in selectedSet }
            val remainingAfter = doc.layers.subList(lastSelectedIdx + 1, doc.layers.size).filterNot { it.id in selectedSet }
            val group = GroupLayer(
                name = "Group (${topSelected.size})",
                children = topSelected,
                isExpanded = false
            )
            return doc.copy(
                layers = remainingBefore + group + remainingAfter,
                selectedLayerId = group.id,
                selectionOrder = listOf(group.id)
            )
        }

        fun ungroupInDocument(doc: ProjectDocument, groupId: String): ProjectDocument {
            val idx = doc.layers.indexOfFirst { it.id == groupId }
            if (idx < 0) return doc
            val group = doc.layers[idx] as? GroupLayer ?: return doc
            val transformedChildren = group.children.map { child ->
                child.withBaseTransform(
                    offsetXNorm = (group.offsetXNorm + child.offsetXNorm).coerceIn(-0.95f, 0.95f),
                    offsetYNorm = (group.offsetYNorm + child.offsetYNorm).coerceIn(-0.95f, 0.95f),
                    scale = (group.scale * child.scale).coerceIn(0.05f, 100f),
                    rotationDeg = (group.rotationDeg + child.rotationDeg) % 360f,
                    opacity = (group.opacity * child.opacity).coerceIn(0f, 1f)
                )
            }
            val mutable = doc.layers.toMutableList()
            mutable.removeAt(idx)
            mutable.addAll(idx, transformedChildren)
            val newIds = transformedChildren.map { it.id }.reversed()
            return doc.copy(
                layers = mutable,
                selectedLayerId = transformedChildren.lastOrNull()?.id,
                selectionOrder = newIds
            )
        }

        /**
         * Anchored One-Side Edge Stretch:
         * Dragging edge (signX, signY) changes ONLY the corresponding width/height and shifts
         * center along the rotated local axis by half the size change so the opposite edge stays pinned!
         */
        fun computeAnchoredStretchTransform(
            layer: EditorLayer,
            signX: Float,
            signY: Float,
            screenLocalDeltaX: Float,
            screenLocalDeltaY: Float,
            boxWpx: Float,
            boxHpx: Float,
            canvasWpx: Float,
            canvasHpx: Float
        ): EditorLayer {
            val safeCanvasW = canvasWpx.coerceAtLeast(1f)
            val safeCanvasH = canvasHpx.coerceAtLeast(1f)
            val oldDispW = (boxWpx * layer.scale * layer.stretchX).coerceAtLeast(8f)
            val oldDispH = (boxHpx * layer.scale * layer.stretchY).coerceAtLeast(8f)

            var nextStretchX = layer.stretchX
            var dDispW = 0f
            if (signX != 0f) {
                val desiredNewW = (oldDispW + screenLocalDeltaX * signX).coerceAtLeast(12f)
                val targetStretchX = (layer.stretchX * (desiredNewW / oldDispW)).coerceIn(0.15f, 8f)
                val actualNewW = boxWpx * layer.scale * targetStretchX
                dDispW = actualNewW - oldDispW
                nextStretchX = targetStretchX
            }

            var nextStretchY = layer.stretchY
            var dDispH = 0f
            if (signY != 0f) {
                val desiredNewH = (oldDispH + screenLocalDeltaY * signY).coerceAtLeast(12f)
                val targetStretchY = (layer.stretchY * (desiredNewH / oldDispH)).coerceIn(0.15f, 8f)
                val actualNewH = boxHpx * layer.scale * targetStretchY
                dDispH = actualNewH - oldDispH
                nextStretchY = targetStretchY
            }

            // Local center shift so opposite edge remains completely stationary
            val shiftLocalX = signX * (dDispW * 0.5f)
            val shiftLocalY = signY * (dDispH * 0.5f)
            val rad = Math.toRadians(layer.rotationDeg.toDouble())
            val dxCanvasPx = (shiftLocalX * cos(rad) - shiftLocalY * sin(rad)).toFloat()
            val dyCanvasPx = (shiftLocalX * sin(rad) + shiftLocalY * cos(rad)).toFloat()

            return layer.withBaseTransform(
                offsetXNorm = (layer.offsetXNorm + dxCanvasPx / safeCanvasW).coerceIn(-0.98f, 0.98f),
                offsetYNorm = (layer.offsetYNorm + dyCanvasPx / safeCanvasH).coerceIn(-0.98f, 0.98f),
                stretchX = nextStretchX,
                stretchY = nextStretchY
            )
        }
    }
}
