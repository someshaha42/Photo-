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
import com.example.model.GroupLayer
import com.example.model.HorizontalAlignOption
import com.example.model.ImageAdjustments
import com.example.model.ImageLayer
import com.example.model.LayerShapeType
import com.example.model.ProjectDocument
import com.example.model.ShapeLayer
import com.example.model.TextAlignOption
import com.example.model.TextCaseMode
import com.example.model.TextCaseTransformer
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
import kotlin.math.roundToInt

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
            _draftTextStyle.value = _draftTextStyle.value.copy(fontId = fontRepository.selectedFontId.value)
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
        _isMultiSelectMode.value = false
        _activeToolDrawer.value = ToolDrawerTab.NONE
        _isEyedropperActive.value = false
        _blurredBgPreview.value = null
        _draftText.value = ""
        lastAppliedCaseMode = null
        val s = settings.value
        val fresh = ProjectDocument(
            id = UUID.randomUUID().toString(),
            title = "Project ${projectHistory.value.size + 1}",
            canvas = CanvasDimensions.SQUARE_1_1,
            background = BackgroundConfig(activeTab = BackgroundTab.TRANSPARENT),
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
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _canvasZoom.value = 1f
        _canvasPan.value = 0f to 0f
        _isZoomMode.value = false
        _isMultiSelectMode.value = false
        _activeToolDrawer.value = ToolDrawerTab.NONE
        _isEyedropperActive.value = false
        _draftText.value = ""
        lastAppliedCaseMode = null
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
            collectImageLayers(doc.layers).forEach { layer ->
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
        lastAppliedCaseMode = null
        if (layerId == null) {
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
                val addedLastId = importedLayers.last().id
                commitChange { current ->
                    current.copy(
                        layers = current.layers + importedLayers,
                        selectedLayerId = addedLastId,
                        selectionOrder = listOf(addedLastId)
                    )
                }
                if (_document.value.background.activeTab == BackgroundTab.BLUR) {
                    regenerateBlurPreview(_document.value)
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
            return
        }

        if (!canAddLayer()) return
        val style = _draftTextStyle.value
        val fontId = fontRepository.selectedFontId.value
        val isEmojiFont = fontRenderEngine.getMetadata(fontId)?.classification?.name?.contains("EMOJI") == true
        val raw = _draftText.value
        val resolvedText = raw.ifBlank {
            if (isEmojiFont) "😀🚀🌈✨" else "New Text"
        }
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

    fun addShapeLayer(shapeType: LayerShapeType) {
        if (!canAddLayer()) return
        val newLayer = ShapeLayer(
            name = shapeType.label,
            shapeType = shapeType
        )
        commitChange { current ->
            current.copy(
                layers = current.layers + newLayer,
                selectedLayerId = newLayer.id,
                selectionOrder = listOf(newLayer.id)
            )
        }
    }

    fun updateSelectedTextLayer(transform: (TextLayer) -> TextLayer) {
        val selId = _document.value.selectedLayerId ?: return
        val existing = findLayerRecursive(_document.value.layers, selId)
        if (existing !is TextLayer) {
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

    fun updateSelectedTextTransient(text: String) {
        lastAppliedCaseMode = null
        val selId = _document.value.selectedLayerId
        val existing = findLayerRecursive(_document.value.layers, selId)
        if (existing !is TextLayer || selId == null) {
            _draftText.value = text
            return
        }
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

    fun updateSelectedShapeLayer(transform: (ShapeLayer) -> ShapeLayer) {
        val selId = _document.value.selectedLayerId ?: return
        commitChange { doc ->
            doc.copy(
                layers = mapLayerRecursive(doc.layers, selId) { layer ->
                    if (layer is ShapeLayer) transform(layer) else layer
                }
            )
        }
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
            layers = mapLayerRecursive(_document.value.layers, layerId) { layer ->
                if (!layer.locked) {
                    layer.withBaseTransform(
                        offsetXNorm = (layer.offsetXNorm + deltaXNorm).coerceIn(-0.95f, 0.95f),
                        offsetYNorm = (layer.offsetYNorm + deltaYNorm).coerceIn(-0.95f, 0.95f),
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
        _document.value = _document.value.copy(
            layers = mapLayerRecursive(_document.value.layers, selId) { layer ->
                if (!layer.locked) layer.withBaseTransform(scale = scale.coerceIn(0.2f, 6f)) else layer
            }
        )
    }

    fun setSelectedLayerOpacity(opacity: Float) {
        val selId = _document.value.selectedLayerId ?: return
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
     * 3-icon Layer Arrangement system based on Position & Selection Order:
     * 1. arrangeSelectedToTop: selected move to Top/Front, 1st selected at highest index, later selected below.
     * 2. swapOrReverseSelectedArrangement: swaps mutual stack positions & reverses selectionOrder.
     * 3. arrangeSelectedToBottom: selected move to Bottom/Back, 1st selected above later selected.
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

    fun alignSelectedLayers(option: HorizontalAlignOption) {
        val order = _document.value.effectiveSelectionOrder
        if (order.isEmpty()) return
        val targets = order.toSet()
        commitChange { doc ->
            doc.copy(
                layers = mapTargetsWithAlignment(doc.layers, targets, option, doc.canvas)
            )
        }
    }

    private fun mapTargetsWithAlignment(
        layers: List<EditorLayer>,
        targets: Set<String>,
        option: HorizontalAlignOption,
        canvas: CanvasDimensions
    ): List<EditorLayer> {
        return layers.map { layer ->
            val updatedSelf = if (layer.id in targets && !layer.locked) {
                val halfW = estimateHalfWidthNorm(layer, canvas)
                val targetX = when (option) {
                    HorizontalAlignOption.CENTER -> 0f
                    HorizontalAlignOption.LEFT -> (-0.47f + halfW).coerceIn(-0.46f, 0f)
                    HorizontalAlignOption.RIGHT -> (0.47f - halfW).coerceIn(0f, 0.46f)
                }
                val baseMoved = layer.withBaseTransform(offsetXNorm = targetX)
                if (baseMoved is TextLayer) {
                    val textAlign = when (option) {
                        HorizontalAlignOption.LEFT -> TextAlignOption.LEFT
                        HorizontalAlignOption.CENTER -> TextAlignOption.CENTER
                        HorizontalAlignOption.RIGHT -> TextAlignOption.RIGHT
                    }
                    baseMoved.copy(align = textAlign)
                } else baseMoved
            } else layer

            if (updatedSelf is GroupLayer) {
                updatedSelf.copy(
                    children = mapTargetsWithAlignment(updatedSelf.children, targets, option, canvas)
                )
            } else updatedSelf
        }
    }

    private fun estimateHalfWidthNorm(layer: EditorLayer, canvas: CanvasDimensions): Float {
        val canvasRatio = canvas.ratioFloat.coerceAtLeast(0.25f)
        val minToW = if (canvasRatio >= 1f) 1f / canvasRatio else 1f
        val half = when (layer) {
            is ImageLayer -> {
                val imgRatio = layer.originalWidth.coerceAtLeast(1).toFloat() / layer.originalHeight.coerceAtLeast(1)
                val wFraction = if (imgRatio >= 1f) 0.85f * minToW else 0.85f * imgRatio * minToW
                wFraction * 0.5f * layer.scale
            }
            is ShapeLayer -> 0.34f * minToW * 0.5f * layer.scale
            is TextLayer -> {
                val bounds = fontRenderEngine.measureTextLayerBounds(
                    layer = layer,
                    effectiveFontSizePx = layer.fontSizeSp,
                    maxWidthPx = 360f * 0.9f,
                    emojiSourceMode = _document.value.emojiSourceMode,
                    appEmojiFontId = _document.value.appEmojiFontId
                )
                ((bounds.widthPx / 360f) * 0.5f * layer.scale)
            }
            is GroupLayer -> 0.30f * layer.scale
        }
        return half.coerceIn(0.06f, 0.45f)
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
            is ShapeLayer -> target.copy(
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
            ?: collectImageLayers(doc.layers).firstOrNull()?.uriString
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
                    scale = (group.scale * child.scale).coerceIn(0.2f, 8f),
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
    }
}
