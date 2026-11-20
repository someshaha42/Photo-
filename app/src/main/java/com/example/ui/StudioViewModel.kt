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
import com.example.model.BackgroundTab
import com.example.model.CanvasDimensions
import com.example.model.EditorLayer
import com.example.model.EmojiSourceMode
import com.example.model.ImageAdjustments
import com.example.model.ImageLayer
import com.example.model.LayerShapeType
import com.example.model.ProjectDocument
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed interface ExportFeedbackState {
    data object Idle : ExportFeedbackState
    data object Exporting : ExportFeedbackState
    data class Success(val file: File, val message: String, val shareImmediately: Boolean = false) : ExportFeedbackState
    data class Error(val message: String) : ExportFeedbackState
}

enum class ToolDrawerTab(val label: String) {
    NONE(""),
    TEXT("Text"),
    ADJUST("Adjust"),
    SHAPES("Shapes"),
    ROTATE("Rotate")
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    val settingsRepository = SettingsRepository(context)
    private val database = ProjectDatabase.getInstance(context)
    val projectRepository = ProjectRepository(context, database.projectDao())
    val mediaManager = MediaStorageManager(context)
    val fontRenderEngine = FontRenderEngine(context)
    val fontRepository = FontRepository(context, fontRenderEngine)

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

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

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _activeToolDrawer = MutableStateFlow(ToolDrawerTab.NONE)
    val activeToolDrawer: StateFlow<ToolDrawerTab> = _activeToolDrawer.asStateFlow()

    private val _showLayersStrip = MutableStateFlow(false)
    val showLayersStrip: StateFlow<Boolean> = _showLayersStrip.asStateFlow()

    private val _isZoomMode = MutableStateFlow(false)
    val isZoomMode: StateFlow<Boolean> = _isZoomMode.asStateFlow()

    private val _canvasZoom = MutableStateFlow(1f)
    val canvasZoom: StateFlow<Float> = _canvasZoom.asStateFlow()

    private val _canvasPan = MutableStateFlow(0f to 0f)
    val canvasPan: StateFlow<Pair<Float, Float>> = _canvasPan.asStateFlow()

    private val _isEyedropperActive = MutableStateFlow(false)
    val isEyedropperActive: StateFlow<Boolean> = _isEyedropperActive.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _exportFeedback = MutableStateFlow<ExportFeedbackState>(ExportFeedbackState.Idle)
    val exportFeedback: StateFlow<ExportFeedbackState> = _exportFeedback.asStateFlow()

    private val _blurredBgPreview = MutableStateFlow<Bitmap?>(null)
    val blurredBgPreview: StateFlow<Bitmap?> = _blurredBgPreview.asStateFlow()

    private val _imageBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val imageBitmaps: StateFlow<Map<String, Bitmap>> = _imageBitmaps.asStateFlow()

    private val _diagnostics = MutableStateFlow(FontDiagnosticsSnapshot())
    val diagnostics: StateFlow<FontDiagnosticsSnapshot> = _diagnostics.asStateFlow()

    init {
        viewModelScope.launch {
            fontRepository.initialize()
            refreshDiagnostics()
        }
    }

    fun refreshDiagnostics() {
        _diagnostics.value = fontRenderEngine.getDiagnosticsSnapshot()
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun openToolDrawer(tab: ToolDrawerTab) {
        _activeToolDrawer.value = if (_activeToolDrawer.value == tab) ToolDrawerTab.NONE else tab
    }

    fun closeToolDrawer() {
        _activeToolDrawer.value = ToolDrawerTab.NONE
    }

    fun toggleLayersStrip() {
        _showLayersStrip.value = !_showLayersStrip.value
    }

    fun toggleZoomMode() {
        _isZoomMode.value = !_isZoomMode.value
    }

    fun setZoomMode(enabled: Boolean) {
        _isZoomMode.value = enabled
    }

    fun updateCanvasViewportTransform(zoomDelta: Float, panDx: Float, panDy: Float) {
        _canvasZoom.value = (_canvasZoom.value * zoomDelta).coerceIn(0.5f, 8f)
        val (px, py) = _canvasPan.value
        _canvasPan.value = (px + panDx) to (py + panDy)
    }

    fun resetCanvasViewport() {
        _canvasZoom.value = 1f
        _canvasPan.value = 0f to 0f
    }

    fun setEyedropperActive(active: Boolean) {
        _isEyedropperActive.value = active
    }

    fun toggleTheme() {
        viewModelScope.launch { settingsRepository.toggleTheme() }
    }

    fun setKeepLocalMediaCopy(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepLocalMediaCopy(enabled) }
    }

    fun setUnlimitedLayers(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUnlimitedLayers(enabled) }
    }

    fun createNewProject() {
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _canvasZoom.value = 1f
        _canvasPan.value = 0f to 0f
        _isZoomMode.value = false
        _showLayersStrip.value = false
        _activeToolDrawer.value = ToolDrawerTab.NONE
        _isEyedropperActive.value = false
        _blurredBgPreview.value = null
        val s = settings.value
        val fresh = ProjectDocument(
            id = UUID.randomUUID().toString(),
            title = "Project ${projectHistory.value.size + 1}",
            canvas = CanvasDimensions.SQUARE_1_1,
            background = BackgroundConfig(activeTab = BackgroundTab.TRANSPARENT),
            layers = emptyList(),
            selectedLayerId = null,
            emojiSourceMode = s.emojiSourceMode,
            appEmojiFontId = s.appEmojiFontId
        )
        _document.value = fresh
    }

    suspend fun openProjectFromHistory(projectId: String): Boolean {
        val loaded = projectRepository.loadDocument(projectId) ?: return false
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _canvasZoom.value = 1f
        _canvasPan.value = 0f to 0f
        _isZoomMode.value = false
        _showLayersStrip.value = false
        _activeToolDrawer.value = ToolDrawerTab.NONE
        _isEyedropperActive.value = false
        _document.value = loaded
        preloadProjectAssets(loaded)
        return true
    }

    fun deleteHistoryProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
        }
    }

    private fun preloadProjectAssets(doc: ProjectDocument) {
        viewModelScope.launch {
            val map = _imageBitmaps.value.toMutableMap()
            doc.layers.filterIsInstance<ImageLayer>().forEach { layer ->
                if (!map.containsKey(layer.uriString)) {
                    mediaManager.decodeRgba8888Bitmap(layer.uriString, maxDimension = 1600)?.let {
                        map[layer.uriString] = it
                    }
                }
            }
            doc.background.backgroundImageUri?.let { bgUri ->
                if (!map.containsKey(bgUri)) {
                    mediaManager.decodeRgba8888Bitmap(bgUri, maxDimension = 1400)?.let {
                        map[bgUri] = it
                    }
                }
            }
            _imageBitmaps.value = map
            if (doc.background.activeTab == BackgroundTab.BLUR) {
                regenerateBlurPreview(doc)
            }
        }
    }

    private fun commitChange(transform: (ProjectDocument) -> ProjectDocument) {
        val prev = _document.value
        val next = transform(prev)
        if (prev == next) return
        undoStack.addLast(prev)
        if (undoStack.size > 40) undoStack.removeFirst()
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
        _document.value = next
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _document.value
        val prev = undoStack.removeLast()
        redoStack.addLast(current)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _document.value = prev
        if (prev.background.activeTab == BackgroundTab.BLUR) {
            viewModelScope.launch { regenerateBlurPreview(prev) }
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _document.value
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _document.value = next
        if (next.background.activeTab == BackgroundTab.BLUR) {
            viewModelScope.launch { regenerateBlurPreview(next) }
        }
    }

    private fun canAddLayer(extraCount: Int = 1): Boolean {
        if (settings.value.unlimitedLayers) return true
        if (_document.value.layers.size + extraCount <= 5) return true
        _statusBanner.value = "Layer limit (5) reached. Enable Unlimited Layers in Settings."
        return false
    }

    fun selectLayer(layerId: String?) {
        _document.value = _document.value.copy(selectedLayerId = layerId)
        val layer = _document.value.layers.firstOrNull { it.id == layerId }
        if (layer is TextLayer) {
            fontRepository.selectFont(layer.fontId)
        }
    }

    /**
     * Imports single or multiple photos from Android Photo Picker.
     * STRICT BUG FIX: Never alters the current canvas aspect ratio!
     */
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
                val bmp = mediaManager.decodeRgba8888Bitmap(meta.uriString, maxDimension = 1600)
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
                commitChange { current ->
                    // Canvas aspect ratio stays strictly untouched!
                    current.copy(
                        layers = current.layers + importedLayers,
                        selectedLayerId = importedLayers.last().id
                    )
                }
                if (_document.value.background.activeTab == BackgroundTab.BLUR) {
                    regenerateBlurPreview(_document.value)
                }
            }
        }
    }

    fun ensureTextLayerAndOpenDrawer() {
        val current = _document.value
        val selected = current.layers.firstOrNull { it.id == current.selectedLayerId }
        if (selected !is TextLayer) {
            addTextLayer("New Text")
        }
        _activeToolDrawer.value = ToolDrawerTab.TEXT
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
                selectedLayerId = newLayer.id
            )
        }
        return newLayer.id
    }

    fun addShapeLayer(shapeType: LayerShapeType) {
        if (!canAddLayer()) return
        val newLayer = ShapeLayer(
            name = shapeType.label,
            shapeType = shapeType
        )
        commitChange { current ->
            current.copy(
                layers = current.layers + newLayer,
                selectedLayerId = newLayer.id
            )
        }
    }

    fun updateSelectedTextLayer(transform: (TextLayer) -> TextLayer) {
        val selId = _document.value.selectedLayerId ?: return
        commitChange { doc ->
            doc.copy(
                layers = doc.layers.map { layer ->
                    if (layer.id == selId && layer is TextLayer) {
                        val updated = transform(layer)
                        updated.copy(name = updated.text.take(14).ifBlank { "Text" })
                    } else layer
                }
            )
        }
    }

    fun updateSelectedTextTransient(text: String) {
        val selId = _document.value.selectedLayerId ?: return
        _document.value = _document.value.copy(
            layers = _document.value.layers.map { layer ->
                if (layer.id == selId && layer is TextLayer) {
                    layer.copy(text = text, name = text.take(14).ifBlank { "Text" })
                } else layer
            }
        )
    }

    fun updateSelectedShapeLayer(transform: (ShapeLayer) -> ShapeLayer) {
        val selId = _document.value.selectedLayerId ?: return
        commitChange { doc ->
            doc.copy(
                layers = doc.layers.map { layer ->
                    if (layer.id == selId && layer is ShapeLayer) transform(layer) else layer
                }
            )
        }
    }

    fun ensureImageLayerSelectedForAdjust(): ImageLayer? {
        val doc = _document.value
        val current = doc.layers.firstOrNull { it.id == doc.selectedLayerId } as? ImageLayer
        if (current != null) return current
        val lastPhoto = doc.layers.filterIsInstance<ImageLayer>().lastOrNull()
        if (lastPhoto != null) {
            _document.value = doc.copy(selectedLayerId = lastPhoto.id)
        }
        return lastPhoto
    }

    fun updateSelectedImageAdjustments(transform: (ImageAdjustments) -> ImageAdjustments) {
        val target = ensureImageLayerSelectedForAdjust() ?: return
        _document.value = _document.value.copy(
            layers = _document.value.layers.map { layer ->
                if (layer.id == target.id && layer is ImageLayer) {
                    layer.copy(adjustments = transform(layer.adjustments))
                } else layer
            }
        )
    }

    fun resetSelectedImageAdjustments() {
        val target = ensureImageLayerSelectedForAdjust() ?: return
        commitChange { doc ->
            doc.copy(
                layers = doc.layers.map { layer ->
                    if (layer.id == target.id && layer is ImageLayer) {
                        layer.copy(adjustments = ImageAdjustments())
                    } else layer
                }
            )
        }
    }

    fun applyFontGloballyAndToSelection(fontId: String) {
        fontRepository.selectFont(fontId)
        val selId = _document.value.selectedLayerId
        val selected = _document.value.layers.firstOrNull { it.id == selId }
        if (selected is TextLayer) {
            commitChange { doc ->
                doc.copy(
                    layers = doc.layers.map { layer ->
                        if (layer.id == selId && layer is TextLayer) {
                            layer.copy(fontId = fontId)
                        } else layer
                    }
                )
            }
        }
        refreshDiagnostics()
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
        rotationDelta: Float
    ) {
        _document.value = _document.value.copy(
            layers = _document.value.layers.map { layer ->
                if (layer.id == layerId && !layer.locked) {
                    layer.withBaseTransform(
                        offsetXNorm = (layer.offsetXNorm + deltaXNorm).coerceIn(-0.9f, 0.9f),
                        offsetYNorm = (layer.offsetYNorm + deltaYNorm).coerceIn(-0.9f, 0.9f),
                        scale = (layer.scale * scaleFactor).coerceIn(0.2f, 8f),
                        rotationDeg = (layer.rotationDeg + rotationDelta) % 360f
                    )
                } else layer
            }
        )
    }

    fun setSelectedLayerRotation(deg: Float) {
        val selId = _document.value.selectedLayerId ?: return
        _document.value = _document.value.copy(
            layers = _document.value.layers.map { layer ->
                if (layer.id == selId && !layer.locked) {
                    layer.withBaseTransform(rotationDeg = deg)
                } else layer
            }
        )
    }

    fun rotateSelectedLayerBy(deltaDeg: Float) {
        val selId = _document.value.selectedLayerId ?: return
        commitChange { doc ->
            doc.copy(
                layers = doc.layers.map { layer ->
                    if (layer.id == selId && !layer.locked) {
                        layer.withBaseTransform(rotationDeg = (layer.rotationDeg + deltaDeg) % 360f)
                    } else layer
                }
            )
        }
    }

    fun setSelectedLayerScale(scale: Float) {
        val selId = _document.value.selectedLayerId ?: return
        _document.value = _document.value.copy(
            layers = _document.value.layers.map { layer ->
                if (layer.id == selId && !layer.locked) {
                    layer.withBaseTransform(scale = scale.coerceIn(0.2f, 6f))
                } else layer
            }
        )
    }

    fun setSelectedLayerOpacity(opacity: Float) {
        val selId = _document.value.selectedLayerId ?: return
        _document.value = _document.value.copy(
            layers = _document.value.layers.map { layer ->
                if (layer.id == selId) layer.withBaseTransform(opacity = opacity.coerceIn(0f, 1f))
                else layer
            }
        )
    }

    fun toggleLayerVisibility(layerId: String) {
        commitChange { doc ->
            doc.copy(
                layers = doc.layers.map {
                    if (it.id == layerId) it.withBaseTransform(visible = !it.visible) else it
                }
            )
        }
    }

    fun toggleLayerLock(layerId: String) {
        commitChange { doc ->
            doc.copy(
                layers = doc.layers.map {
                    if (it.id == layerId) it.withBaseTransform(locked = !it.locked) else it
                }
            )
        }
    }

    fun moveLayerOrder(layerId: String, direction: Int) {
        commitChange { doc ->
            val list = doc.layers.toMutableList()
            val idx = list.indexOfFirst { it.id == layerId }
            if (idx < 0) return@commitChange doc
            val targetIdx = (idx + direction).coerceIn(0, list.lastIndex)
            if (targetIdx == idx) return@commitChange doc
            val item = list.removeAt(idx)
            list.add(targetIdx, item)
            doc.copy(layers = list)
        }
    }

    fun duplicateLayer(layerId: String) {
        if (!canAddLayer()) return
        val target = _document.value.layers.firstOrNull { it.id == layerId } ?: return
        val copyId = UUID.randomUUID().toString()
        val copied: EditorLayer = when (target) {
            is ImageLayer -> target.copy(id = copyId, offsetXNorm = target.offsetXNorm + 0.05f, offsetYNorm = target.offsetYNorm + 0.05f)
            is TextLayer -> target.copy(id = copyId, offsetXNorm = target.offsetXNorm + 0.05f, offsetYNorm = target.offsetYNorm + 0.05f)
            is ShapeLayer -> target.copy(id = copyId, offsetXNorm = target.offsetXNorm + 0.05f, offsetYNorm = target.offsetYNorm + 0.05f)
        }
        commitChange { doc ->
            doc.copy(layers = doc.layers + copied, selectedLayerId = copyId)
        }
    }

    fun deleteLayer(layerId: String) {
        commitChange { doc ->
            val remaining = doc.layers.filterNot { it.id == layerId }
            doc.copy(
                layers = remaining,
                selectedLayerId = if (doc.selectedLayerId == layerId) remaining.lastOrNull()?.id else doc.selectedLayerId
            )
        }
    }

    fun selectSquareCanvas() {
        commitChange { doc ->
            doc.copy(canvas = CanvasDimensions.SQUARE_1_1)
        }
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
        val current = _document.value
        if (current.background.activeTab == BackgroundTab.BLUR) {
            viewModelScope.launch { regenerateBlurPreview(current) }
        }
    }

    fun pickBackgroundImage(uri: Uri) {
        viewModelScope.launch {
            val doc = _document.value
            val meta = mediaManager.importImage(
                sourceUri = uri,
                projectId = doc.id,
                keepLocalCopy = settings.value.keepLocalMediaCopy
            )
            val bmp = mediaManager.decodeRgba8888Bitmap(meta.uriString, maxDimension = 1400)
            if (bmp != null) {
                _imageBitmaps.value = _imageBitmaps.value + (meta.uriString to bmp)
            }
            updateBackground { bg ->
                bg.copy(
                    activeTab = BackgroundTab.IMAGE,
                    backgroundImageUri = meta.uriString
                )
            }
        }
    }

    private suspend fun regenerateBlurPreview(doc: ProjectDocument) {
        val targetUri = doc.background.backgroundImageUri
            ?: doc.layers.filterIsInstance<ImageLayer>().firstOrNull()?.uriString
            ?: return
        val src = _imageBitmaps.value[targetUri]
            ?: mediaManager.decodeRgba8888Bitmap(targetUri, maxDimension = 900)
            ?: return
        val blurred = mediaManager.createBlurredBitmap(src, doc.background.blurStrength)
        _blurredBgPreview.value = blurred
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

    fun exportProjectPng(shareImmediately: Boolean = false) {
        viewModelScope.launch {
            _exportFeedback.value = ExportFeedbackState.Exporting
            val doc = _document.value
            val thumbPath = mediaManager.saveProjectThumbnail(doc, fontRenderEngine)
            projectRepository.saveDocument(doc, thumbPath)

            val res = mediaManager.exportPngToAppAndGallery(doc, fontRenderEngine)
            res.onSuccess { (file, galleryUri) ->
                val destText = if (galleryUri != null) "Pictures/PixelForge" else "App Exports"
                _exportFeedback.value = ExportFeedbackState.Success(
                    file = file,
                    message = "Saved PNG ($destText)",
                    shareImmediately = shareImmediately
                )
            }.onFailure { err ->
                _exportFeedback.value = ExportFeedbackState.Error(err.localizedMessage ?: "Export failed")
            }
            refreshDiagnostics()
        }
    }

    fun clearExportFeedback() {
        _exportFeedback.value = ExportFeedbackState.Idle
    }

    fun importFontFiles(uris: List<Uri>) {
        viewModelScope.launch {
            fontRepository.importFontUris(uris)
            refreshDiagnostics()
        }
    }

    fun importFontArchive(uri: Uri) {
        viewModelScope.launch {
            fontRepository.importArchiveUri(uri)
            refreshDiagnostics()
        }
    }

    fun deleteImportedFont(fontId: String) {
        viewModelScope.launch {
            fontRepository.deleteImportedFont(fontId)
            refreshDiagnostics()
        }
    }
}
