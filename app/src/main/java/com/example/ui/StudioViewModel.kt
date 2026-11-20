package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiChatMessage
import com.example.ai.AiChatModel
import com.example.ai.AiGeneratedImageResult
import com.example.ai.AiImageSize
import com.example.ai.AiRepository
import com.example.ai.AiSystemRole
import com.example.ai.PlaceholderAiRepository
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
import com.example.fonts.ImportHistoryEntry
import com.example.model.BackgroundConfig
import com.example.model.BackgroundTab
import com.example.model.CanvasAspectRatio
import com.example.model.EditorBlendMode
import com.example.model.EditorLayer
import com.example.model.ImageLayer
import com.example.model.LayerShapeType
import com.example.model.ProjectDocument
import com.example.model.ShapeLayer
import com.example.model.TextAlignOption
import com.example.model.TextLayer
import com.example.model.VideoLayer
import com.example.video.VideoEditorEngine
import com.example.video.VideoExportState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed interface AiImageUiState {
    data object Idle : AiImageUiState
    data object Loading : AiImageUiState
    data class Success(val result: AiGeneratedImageResult) : AiImageUiState
    data class Error(val message: String) : AiImageUiState
}

sealed interface ExportFeedbackState {
    data object Idle : ExportFeedbackState
    data object Exporting : ExportFeedbackState
    data class Success(val file: File, val message: String) : ExportFeedbackState
    data class Error(val message: String) : ExportFeedbackState
}

enum class EditorBottomTab(val label: String) {
    CANVAS("Canvas"),
    TEXT("Text"),
    MEDIA("Media"),
    BACKGROUND("Background"),
    LAYERS("Layers")
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    val settingsRepository = SettingsRepository(context)
    private val database = ProjectDatabase.getInstance(context)
    val projectRepository = ProjectRepository(context, database.projectDao())
    val mediaManager = MediaStorageManager(context)
    val fontRenderEngine = FontRenderEngine(context)
    val fontRepository = FontRepository(context, fontRenderEngine)
    val videoEditorEngine = VideoEditorEngine(context)
    val aiRepository: AiRepository = PlaceholderAiRepository()

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val projectHistory: StateFlow<List<ProjectEntity>> = projectRepository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fonts: StateFlow<List<FontMetadata>> = fontRepository.fonts
    val selectedFontId: StateFlow<String> = fontRepository.selectedFontId
    val fontImportProgress: StateFlow<FontImportProgress> = fontRepository.importProgress
    val fontImportHistory: StateFlow<List<ImportHistoryEntry>> = fontRepository.importHistory
    val videoExportState: StateFlow<VideoExportState> = videoEditorEngine.exportState

    private val _document = MutableStateFlow(ProjectDocument())
    val document: StateFlow<ProjectDocument> = _document.asStateFlow()

    private val undoStack = ArrayDeque<ProjectDocument>()
    private val redoStack = ArrayDeque<ProjectDocument>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _activeBottomTab = MutableStateFlow(EditorBottomTab.TEXT)
    val activeBottomTab: StateFlow<EditorBottomTab> = _activeBottomTab.asStateFlow()

    private val _canvasZoom = MutableStateFlow(1f)
    val canvasZoom: StateFlow<Float> = _canvasZoom.asStateFlow()

    private val _showGrid = MutableStateFlow(false)
    val showGrid: StateFlow<Boolean> = _showGrid.asStateFlow()

    private val _isEyedropperActive = MutableStateFlow(false)
    val isEyedropperActive: StateFlow<Boolean> = _isEyedropperActive.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _exportFeedback = MutableStateFlow<ExportFeedbackState>(ExportFeedbackState.Idle)
    val exportFeedback: StateFlow<ExportFeedbackState> = _exportFeedback.asStateFlow()

    private val _blurredBgPreview = MutableStateFlow<Bitmap?>(null)
    val blurredBgPreview: StateFlow<Bitmap?> = _blurredBgPreview.asStateFlow()

    // Cached decoded RGBA8888 bitmaps for fast live canvas rendering
    private val _imageBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val imageBitmaps: StateFlow<Map<String, Bitmap>> = _imageBitmaps.asStateFlow()

    private val _diagnostics = MutableStateFlow(FontDiagnosticsSnapshot())
    val diagnostics: StateFlow<FontDiagnosticsSnapshot> = _diagnostics.asStateFlow()

    // AI Studio State
    private val _aiImageState = MutableStateFlow<AiImageUiState>(AiImageUiState.Idle)
    val aiImageState: StateFlow<AiImageUiState> = _aiImageState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<AiChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatSending = MutableStateFlow(false)
    val isChatSending: StateFlow<Boolean> = _isChatSending.asStateFlow()

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

    fun setBottomTab(tab: EditorBottomTab) {
        _activeBottomTab.value = tab
    }

    fun setCanvasZoom(zoom: Float) {
        _canvasZoom.value = zoom.coerceIn(0.5f, 8f)
    }

    fun toggleGrid() {
        _showGrid.value = !_showGrid.value
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

    /**
     * Opens a fresh blank transparent checkerboard canvas (no automatic image picker!).
     */
    fun createNewProject() {
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _canvasZoom.value = 1f
        _isEyedropperActive.value = false
        _blurredBgPreview.value = null
        val fresh = ProjectDocument(
            id = UUID.randomUUID().toString(),
            title = "Project ${projectHistory.value.size + 1}",
            aspectRatio = CanvasAspectRatio.RATIO_1_1,
            autoImageDetectionEnabled = true,
            manualAspectOverride = false,
            background = BackgroundConfig(activeTab = BackgroundTab.TRANSPARENT),
            layers = emptyList(),
            selectedLayerId = null
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

    private fun canAddLayer(): Boolean {
        if (settings.value.unlimitedLayers) return true
        if (_document.value.layers.size < 5) return true
        _statusBanner.value = "Layer limit (5) reached. Enable Unlimited Layers in Settings."
        return false
    }

    fun setProjectTitle(title: String) {
        commitChange { it.copy(title = title) }
    }

    fun selectLayer(layerId: String?) {
        _document.value = _document.value.copy(selectedLayerId = layerId)
        val layer = _document.value.layers.firstOrNull { it.id == layerId }
        if (layer is TextLayer) {
            fontRepository.selectFont(layer.fontId)
        }
    }

    fun addImageFromPicker(uri: Uri) {
        if (!canAddLayer()) return
        viewModelScope.launch {
            val doc = _document.value
            val meta = mediaManager.importImage(
                sourceUri = uri,
                projectId = doc.id,
                keepLocalCopy = settings.value.keepLocalMediaCopy
            )
            val bmp = mediaManager.decodeRgba8888Bitmap(meta.uriString, maxDimension = 1600)
            if (bmp != null) {
                _imageBitmaps.value = _imageBitmaps.value + (meta.uriString to bmp)
            }
            val newLayer = ImageLayer(
                name = meta.displayName.substringBeforeLast('.').take(18).ifBlank { "Image" },
                uriString = meta.uriString,
                originalWidth = meta.width,
                originalHeight = meta.height
            )
            val autoRatio = if (doc.autoImageDetectionEnabled && !doc.manualAspectOverride) {
                CanvasAspectRatio.closestForDimensions(meta.width, meta.height)
            } else {
                doc.aspectRatio
            }
            commitChange { current ->
                current.copy(
                    aspectRatio = autoRatio,
                    layers = current.layers + newLayer,
                    selectedLayerId = newLayer.id
                )
            }
            if (_document.value.background.activeTab == BackgroundTab.BLUR) {
                regenerateBlurPreview(_document.value)
            }
            _statusBanner.value = "Imported ${meta.width}×${meta.height} image (RGBA8888 preserved)"
        }
    }

    fun addVideoFromPicker(uri: Uri) {
        if (!canAddLayer()) return
        viewModelScope.launch {
            val doc = _document.value
            val meta = mediaManager.importVideo(
                sourceUri = uri,
                projectId = doc.id,
                keepLocalCopy = settings.value.keepLocalMediaCopy
            )
            val newLayer = VideoLayer(
                name = meta.displayName.substringBeforeLast('.').take(18).ifBlank { "Video" },
                uriString = meta.uriString,
                originalWidth = meta.width,
                originalHeight = meta.height,
                durationMs = meta.durationMs,
                trimStartMs = 0L,
                trimEndMs = meta.durationMs
            )
            val autoRatio = if (doc.autoImageDetectionEnabled && !doc.manualAspectOverride) {
                CanvasAspectRatio.closestForDimensions(meta.width, meta.height)
            } else {
                doc.aspectRatio
            }
            commitChange { current ->
                current.copy(
                    aspectRatio = autoRatio,
                    layers = current.layers + newLayer,
                    selectedLayerId = newLayer.id
                )
            }
            _activeBottomTab.value = EditorBottomTab.MEDIA
            _statusBanner.value = "Added video (${meta.width}×${meta.height}, ${meta.durationMs / 1000}s)"
        }
    }

    fun addTextLayer(initialText: String = "Double tap or edit text") {
        if (!canAddLayer()) return
        val fontId = fontRepository.selectedFontId.value
        val isEmojiFont = fontRenderEngine.getMetadata(fontId)?.classification?.name?.contains("EMOJI") == true
        val defaultText = if (isEmojiFont && initialText == "Double tap or edit text") "😀🚀🌈🎨✨" else initialText
        val newLayer = TextLayer(
            name = defaultText.take(14).ifBlank { "Text" },
            text = defaultText,
            fontId = fontId,
            fontSizeSp = if (isEmojiFont) 56f else 38f
        )
        commitChange { current ->
            current.copy(
                layers = current.layers + newLayer,
                selectedLayerId = newLayer.id
            )
        }
        _activeBottomTab.value = EditorBottomTab.TEXT
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

    fun updateSelectedVideoLayer(transform: (VideoLayer) -> VideoLayer) {
        val selId = _document.value.selectedLayerId ?: return
        commitChange { doc ->
            doc.copy(
                layers = doc.layers.map { layer ->
                    if (layer.id == selId && layer is VideoLayer) transform(layer) else layer
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

    fun setLayerOpacity(layerId: String, opacity: Float) {
        commitChange { doc ->
            doc.copy(
                layers = doc.layers.map {
                    if (it.id == layerId) it.withBaseTransform(opacity = opacity.coerceIn(0f, 1f)) else it
                }
            )
        }
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
            is VideoLayer -> target.copy(id = copyId, offsetXNorm = target.offsetXNorm + 0.05f, offsetYNorm = target.offsetYNorm + 0.05f)
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

    fun selectAspectRatioManually(ratio: CanvasAspectRatio) {
        commitChange { doc ->
            doc.copy(
                aspectRatio = ratio,
                manualAspectOverride = true
            )
        }
    }

    fun toggleAutoImageDetection() {
        commitChange { doc ->
            val nextEnabled = !doc.autoImageDetectionEnabled
            val nextRatio = if (nextEnabled) {
                val img = doc.layers.filterIsInstance<ImageLayer>().firstOrNull()
                if (img != null) CanvasAspectRatio.closestForDimensions(img.originalWidth, img.originalHeight)
                else doc.aspectRatio
            } else doc.aspectRatio
            doc.copy(
                autoImageDetectionEnabled = nextEnabled,
                manualAspectOverride = if (nextEnabled) false else doc.manualAspectOverride,
                aspectRatio = nextRatio
            )
        }
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
                _statusBanner.value = "Project saved to History."
            }
        }
    }

    fun exportProjectPng() {
        viewModelScope.launch {
            _exportFeedback.value = ExportFeedbackState.Exporting
            val doc = _document.value
            val thumbPath = mediaManager.saveProjectThumbnail(doc, fontRenderEngine)
            projectRepository.saveDocument(doc, thumbPath)

            val res = mediaManager.exportPngToAppAndGallery(doc, fontRenderEngine)
            res.onSuccess { (file, galleryUri) ->
                val destText = if (galleryUri != null) "Saved to Pictures/PixelForge" else "Saved to app exports"
                _exportFeedback.value = ExportFeedbackState.Success(
                    file = file,
                    message = "RGBA8888 PNG exported ($destText, checkerboard excluded)."
                )
            }.onFailure { err ->
                _exportFeedback.value = ExportFeedbackState.Error(err.localizedMessage ?: "Export failed.")
            }
            refreshDiagnostics()
        }
    }

    fun exportActiveVideo() {
        val videoLayer = _document.value.layers.firstOrNull { it.id == _document.value.selectedLayerId && it is VideoLayer } as? VideoLayer
            ?: _document.value.layers.filterIsInstance<VideoLayer>().firstOrNull()
        if (videoLayer == null) {
            _statusBanner.value = "Add a Video layer first to export MP4."
            return
        }
        viewModelScope.launch {
            val doc = _document.value
            val thumbPath = mediaManager.saveProjectThumbnail(doc, fontRenderEngine)
            projectRepository.saveDocument(doc, thumbPath)
            videoEditorEngine.exportVideo(doc, videoLayer, fontRenderEngine)
        }
    }

    fun clearExportFeedback() {
        _exportFeedback.value = ExportFeedbackState.Idle
        videoEditorEngine.resetExportState()
    }

    // Font Import wrappers
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

    // AI Studio functions
    fun generateAiImage(prompt: String, size: AiImageSize) {
        viewModelScope.launch {
            _aiImageState.value = AiImageUiState.Loading
            try {
                val result = aiRepository.generateImage(prompt, size)
                _aiImageState.value = AiImageUiState.Success(result)
            } catch (e: Exception) {
                _aiImageState.value = AiImageUiState.Error(e.localizedMessage ?: "AI image generation failed.")
            }
        }
    }

    fun addGeneratedAiBitmapToCanvas(bitmap: Bitmap, label: String) {
        if (!canAddLayer()) return
        viewModelScope.launch {
            val doc = _document.value
            val storedPath = withContext(Dispatchers.IO) {
                val dir = File(context.filesDir, "projects_media/${doc.id}")
                dir.mkdirs()
                val file = File(dir, "ai_gen_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                file.absolutePath
            }
            _imageBitmaps.value = _imageBitmaps.value + (storedPath to bitmap)
            val newLayer = ImageLayer(
                name = label.take(18).ifBlank { "AI Art" },
                uriString = storedPath,
                originalWidth = bitmap.width,
                originalHeight = bitmap.height
            )
            commitChange { current ->
                current.copy(
                    layers = current.layers + newLayer,
                    selectedLayerId = newLayer.id
                )
            }
            _statusBanner.value = "Added generated ${bitmap.width}×${bitmap.height} image to canvas."
        }
    }

    fun sendChatMessage(model: AiChatModel, role: AiSystemRole, text: String) {
        if (text.isBlank() || _isChatSending.value) return
        val userMsg = AiChatMessage(isUser = true, text = text.trim())
        val updated = _chatMessages.value + userMsg
        _chatMessages.value = updated
        _isChatSending.value = true

        viewModelScope.launch {
            try {
                val reply = aiRepository.sendChatMessage(model, role, updated, text)
                _chatMessages.value = _chatMessages.value + reply
            } finally {
                _isChatSending.value = false
            }
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = emptyList()
    }
}
