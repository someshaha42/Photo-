package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.StudioViewModel
import com.example.ui.components.FontLibrarySheet
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.PixelForgeTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private object HomeRoute

@Serializable
private object EditorRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelForgeApp()
        }
    }
}

@Composable
fun PixelForgeApp(viewModel: StudioViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val projects by viewModel.projectHistory.collectAsStateWithLifecycle()
    val fonts by viewModel.fonts.collectAsStateWithLifecycle()
    val selectedFontId by viewModel.selectedFontId.collectAsStateWithLifecycle()
    val fontImportProgress by viewModel.fontImportProgress.collectAsStateWithLifecycle()
    val fontImportHistory by viewModel.fontImportHistory.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()

    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFontLibrarySheet by remember { mutableStateOf(false) }

    // Direct Android Photo Picker (single or multiple photos, zero permissions, no file manager)
    val photosPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.stagePickedPhotosForCrop(uris)
    }

    // Single Photo Picker for Filter -> Fill Media (clips inside selected layer)
    val fillMediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.applyFillMediaFromUri(uri)
    }

    // SAF Multi-Document Picker for Custom Fonts (TTF, OTF, TTC, OTC, WOFF, WOFF2)
    val fontFilesLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importFontFiles(uris)
    }

    // SAF Document Picker for Compressed Font Archives (ZIP, RAR, 7Z, TAR, GZ, BZ2, XZ)
    val fontArchiveLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importFontArchive(uri)
    }

    PixelForgeTheme(darkTheme = settings.isDarkTheme) {
        NavHost(
            navController = navController,
            startDestination = HomeRoute
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    projects = projects,
                    isDarkTheme = settings.isDarkTheme,
                    statusBanner = statusBanner,
                    onClearBanner = { viewModel.clearBanner() },
                    onNewProjectClick = {
                        viewModel.createNewProject()
                        navController.navigate(EditorRoute)
                    },
                    onOpenProject = { projectId ->
                        scope.launch {
                            if (viewModel.openProjectFromHistory(projectId)) {
                                navController.navigate(EditorRoute)
                            }
                        }
                    },
                    onSaveProject = { viewModel.saveHistoryProjectToDevice(it) },
                    onDeleteProject = { viewModel.deleteHistoryProject(it) },
                    onDeleteProjects = { viewModel.deleteHistoryProjects(it) },
                    onToggleTheme = { viewModel.toggleTheme() },
                    onOpenSettings = { showSettingsDialog = true }
                )
            }

            composable<EditorRoute> {
                EditorScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onPickPhotosDirect = {
                        photosPickerLauncher.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                        )
                    },
                    onPickFillMediaDirect = {
                        fillMediaPickerLauncher.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                        )
                    },
                    onOpenFontLibrary = { showFontLibrarySheet = true },
                    onImportFontsClick = {
                        fontFilesLauncher.launch(arrayOf("*/*"))
                    },
                    onImportArchiveClick = {
                        fontArchiveLauncher.launch(arrayOf("*/*"))
                    }
                )
            }
        }

        if (showSettingsDialog) {
            SettingsDialog(
                settings = settings,
                onToggleKeepLocalMediaCopy = { viewModel.setKeepLocalMediaCopy(it) },
                onToggleUnlimitedLayers = { viewModel.setUnlimitedLayers(it) },
                onToggleDarkTheme = { scope.launch { viewModel.settingsRepository.setDarkTheme(it) } },
                onDismiss = { showSettingsDialog = false }
            )
        }

        if (showFontLibrarySheet) {
            FontLibrarySheet(
                fonts = fonts,
                selectedFontId = selectedFontId,
                renderEngine = viewModel.fontRenderEngine,
                importProgress = fontImportProgress,
                importHistory = fontImportHistory,
                diagnostics = diagnostics,
                onSelectFont = { fontId ->
                    viewModel.applyFontGloballyAndToSelection(fontId)
                },
                onImportFontsClick = {
                    fontFilesLauncher.launch(arrayOf("*/*"))
                },
                onImportArchiveClick = {
                    fontArchiveLauncher.launch(arrayOf("*/*"))
                },
                onDeleteFont = { viewModel.deleteImportedFont(it) },
                onClearImportStatus = { viewModel.fontRepository.clearImportProgressMessage() },
                onRefreshDiagnostics = { viewModel.refreshDiagnostics() },
                onDismiss = { showFontLibrarySheet = false }
            )
        }
    }
}
