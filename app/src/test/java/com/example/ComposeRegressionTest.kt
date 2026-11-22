package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProjectEntity
import com.example.fonts.FontDiagnosticsSnapshot
import com.example.fonts.FontImportProgress
import com.example.fonts.FontRenderEngine
import com.example.fonts.FontRepository
import com.example.fonts.FontSource
import com.example.model.CanvasDimensions
import com.example.model.ImageCropRect
import com.example.model.ImageCropShape
import com.example.model.ImageLayer
import com.example.model.ProjectDocument
import com.example.model.TextLayer
import com.example.ui.PendingImportPhoto
import com.example.ui.StudioViewModel
import com.example.ui.ToolDrawerTab
import com.example.ui.components.CanvasSettingsPanel
import com.example.ui.components.FontLibrarySheet
import com.example.ui.components.PendingPhotoCropOverlay
import com.example.ui.components.TextToolPanel
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.PixelForgeTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ComposeRegressionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun textInputRemainsMountedAndFocusedWhenCompactImeModeToggles() {
        var textState by mutableStateOf("")
        var compactImeMode by mutableStateOf(false)
        var confirmed = false

        composeRule.setContent {
            PixelForgeTheme {
                TextToolPanel(
                    selectedTextLayer = null,
                    draftText = textState,
                    draftStyle = TextLayer(text = ""),
                    activeFont = null,
                    onTextChangeLive = { textState = it },
                    onConfirmText = { confirmed = true },
                    onCycleTextCase = {},
                    onOpenFontLibrary = {},
                    onUpdateTextLayer = {},
                    isCompactImeMode = compactImeMode
                )
            }
        }

        // Focus text field while full panel is visible
        composeRule.onNodeWithTag("text_layer_editor_input").performClick()
        composeRule.onNodeWithTag("text_layer_editor_input").assertIsFocused()

        // Simulate soft keyboard appearing (toggles compact IME mode)
        composeRule.runOnUiThread { compactImeMode = true }
        composeRule.waitForIdle()

        // Input box must stay mounted & focused so Android IME InputConnection is never dropped!
        composeRule.onNodeWithTag("text_layer_editor_input").assertIsDisplayed()
        composeRule.onNodeWithTag("text_layer_editor_input").assertIsFocused()
        composeRule.onNodeWithTag("text_layer_editor_input").performTextInput("Sharp Vector Text")
        composeRule.waitForIdle()

        assertEquals("Sharp Vector Text", textState)
        composeRule.onNodeWithTag("text_layer_editor_input").assertTextContains("Sharp Vector Text")
        composeRule.onNodeWithTag("text_confirm_button").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun fontLibraryVirtualizesLargeCollectionAndOpensVectorZoomInspector() {
        val renderEngine = FontRenderEngine(context)
        val repo = FontRepository(context, renderEngine)
        runBlocking { repo.initialize() }

        val roboto = repo.fonts.value.first { it.fileName == "Roboto.ttf" }
        val manyFonts = listOf(roboto) + (1..1000).map { idx ->
            roboto.copy(
                id = "large_font_$idx",
                displayName = "Designer Font $idx",
                fileName = "DesignerFont$idx.ttf",
                source = FontSource.BUNDLED
            )
        }
        renderEngine.registerFonts(manyFonts)

        composeRule.setContent {
            PixelForgeTheme {
                FontLibrarySheet(
                    fonts = manyFonts,
                    selectedFontId = roboto.id,
                    renderEngine = renderEngine,
                    importProgress = FontImportProgress(),
                    importHistory = emptyList(),
                    diagnostics = FontDiagnosticsSnapshot(totalFonts = manyFonts.size),
                    onSelectFont = {},
                    onImportFontsClick = {},
                    onImportArchiveClick = {},
                    onDeleteFont = {},
                    onClearImportStatus = {},
                    onRefreshDiagnostics = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Fonts (1001)").assertIsDisplayed()
        composeRule.onNodeWithTag("font_card_Roboto.ttf").assertIsDisplayed()

        // Open inspect dialog -> verify continuous vector zoom slider + preview canvas render
        composeRule.onNodeWithContentDescription("Inspect").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("font_detail_zoom_slider").assertIsDisplayed()
        composeRule.onNodeWithTag("font_detail_vector_preview").assertIsDisplayed()
        composeRule.onNodeWithText("1600%").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Zoom: 1600%").assertIsDisplayed()
    }

    @Test
    fun editorScreenTopControlsLocksFloatingCanvasButtonsAndContextBarsWork() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()
        var directPhotoPickCount = 0

        composeRule.setContent {
            PixelForgeTheme {
                EditorScreen(
                    viewModel = vm,
                    onNavigateBack = {},
                    onPickPhotosDirect = { directPhotoPickCount++ },
                    onOpenFontLibrary = {}
                )
            }
        }

        composeRule.waitForIdle()
        // Unselected main screen: Rotate, Shapes, Draw are absent; Photo opens gallery directly
        composeRule.onNodeWithTag("bottom_tool_photo").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_text_plus").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_shapes").assertDoesNotExist()
        composeRule.onNodeWithTag("bottom_tool_draw").assertDoesNotExist()
        composeRule.onNodeWithTag("bottom_tool_rotate").assertDoesNotExist()

        composeRule.onNodeWithTag("bottom_tool_photo").performClick()
        assertEquals(1, directPhotoPickCount)

        // Zoom & Layers are vertically stacked in the canvas corner; top bar Row 2 is unwrapped (no pager dot)
        composeRule.onNodeWithTag("top_zoom_button").assertIsDisplayed()
        composeRule.onNodeWithTag("top_layers_button").assertIsDisplayed()
        composeRule.onNodeWithTag("top_pager_dot_1").assertDoesNotExist()

        composeRule.onNodeWithTag("top_delete_button").assertIsDisplayed()
        composeRule.onNodeWithTag("top_tool_corner_pin").assertIsDisplayed()
        composeRule.onNodeWithTag("top_tool_3d").assertIsDisplayed()
        assertTrue(vm.transformLocks.value.stretchLocked)
        assertFalse(vm.transformLocks.value.zoomLocked)
        composeRule.onNodeWithTag("top_lock_stretch_button").assertIsDisplayed()
        composeRule.onNodeWithTag("top_lock_zoom_button").assertIsDisplayed()
        composeRule.onNodeWithTag("top_lock_rotation_button").assertIsDisplayed()
        composeRule.onNodeWithTag("top_lock_position_button").assertIsDisplayed()

        composeRule.onNodeWithTag("top_lock_stretch_button").performClick()
        assertFalse(vm.transformLocks.value.stretchLocked)

        // Enter Text mode -> + New is gone inside panel, main workspace tools hidden, text tools + Rotate shown
        composeRule.onNodeWithTag("bottom_tool_text_plus").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("text_new_layer_button").assertDoesNotExist()
        composeRule.onNodeWithTag("bottom_tool_edit_text").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_color").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_font").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_rotate").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_photo").assertDoesNotExist()
        composeRule.onNodeWithTag("bottom_tool_shapes").assertDoesNotExist()
        composeRule.onNodeWithTag("bottom_tool_draw").assertDoesNotExist()
    }

    @Test
    fun pendingPhotoCropOverlayExposesCornersAndSingleEdgesWithoutModeButtons() {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val pending = PendingImportPhoto(
            uriString = "/tmp/sample.png",
            displayName = "sample",
            originalWidth = 200,
            originalHeight = 200,
            previewBitmap = bmp
        )
        var committedCrop: ImageCropRect? = null
        var committedShape: ImageCropShape? = null
        var deleted = false

        composeRule.setContent {
            PixelForgeTheme {
                PendingPhotoCropOverlay(
                    pendingPhoto = pending,
                    onDelete = { deleted = true },
                    onDone = { c, s ->
                        committedCrop = c
                        committedShape = s
                    }
                )
            }
        }

        composeRule.waitForIdle()
        // Old Free/Circle/9:16 preset buttons must be gone
        composeRule.onNodeWithTag("import_crop_mode_free").assertDoesNotExist()
        composeRule.onNodeWithTag("import_crop_mode_circle").assertDoesNotExist()
        composeRule.onNodeWithTag("import_crop_mode_9_16").assertDoesNotExist()

        // 4 corner zoom pins + 4 single-edge crop handles exist
        composeRule.onNodeWithTag("import_crop_corner_tl").assertExists()
        composeRule.onNodeWithTag("import_crop_corner_tr").assertExists()
        composeRule.onNodeWithTag("import_crop_corner_bl").assertExists()
        composeRule.onNodeWithTag("import_crop_corner_br").assertExists()
        composeRule.onNodeWithTag("import_crop_edge_top").assertExists()
        composeRule.onNodeWithTag("import_crop_edge_bottom").assertExists()
        composeRule.onNodeWithTag("import_crop_edge_left").assertExists()
        composeRule.onNodeWithTag("import_crop_edge_right").assertExists()

        // Dragging only the right edge leftwards crops ONLY the right edge
        composeRule.onNodeWithTag("import_crop_edge_right").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(-60f, 0f))
            up()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("import_crop_done_button").performClick()
        assertEquals(ImageCropShape.RECTANGLE, committedShape)
        assertEquals(0f, committedCrop?.left ?: -1f, 0.01f)
        assertTrue((committedCrop?.right ?: 1f) < 0.99f)

        composeRule.onNodeWithTag("import_crop_delete_button").performClick()
        assertTrue(deleted)
        bmp.recycle()
    }

    @Test
    fun allLayerTypesExposeTopBarCornerPinAnd3DControllersWithoutBottomPager() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()
        val textId = vm.addTextLayer("Hello 3D")!!
        val secondTextId = vm.addTextLayer("Second 3D")!!
        vm.selectLayer(textId)

        composeRule.setContent {
            PixelForgeTheme {
                EditorScreen(
                    viewModel = vm,
                    onNavigateBack = {},
                    onPickPhotosDirect = {},
                    onOpenFontLibrary = {}
                )
            }
        }

        composeRule.waitForIdle()
        // Bottom bar no longer contains Corner Pin, 3D, or a second pager page
        composeRule.onNodeWithTag("bottom_tool_edit_text").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_rotate").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_pager").assertDoesNotExist()
        composeRule.onNodeWithTag("bottom_tool_corner_pin").assertDoesNotExist()
        composeRule.onNodeWithTag("bottom_tool_3d").assertDoesNotExist()

        // Top Bar exposes Corner Pin & 3D directly
        composeRule.onNodeWithTag("top_tool_corner_pin").assertIsDisplayed()
        composeRule.onNodeWithTag("top_tool_3d").assertIsDisplayed()

        // Open 3D Floating Panel on TextLayer
        composeRule.onNodeWithTag("top_tool_3d").performClick()
        composeRule.waitForIdle()
        assertEquals(ToolDrawerTab.THREE_D, vm.activeToolDrawer.value)
        composeRule.onNodeWithTag("three_d_floating_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_drag_handle").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_undo_button").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_redo_button").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_more_button").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_mode_move").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_mode_rotate").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_mode_scale").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_mode_skew").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_move_axis_xy").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_move_axis_z").assertIsDisplayed()
        composeRule.onNodeWithText("Keyframe").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Keyframe").assertDoesNotExist()

        // Switch 3D mode to Scale & toggle linked/unlinked
        composeRule.onNodeWithTag("three_d_mode_scale").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("three_d_scale_ruler_linked").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_scale_link_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("three_d_scale_ruler_w").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_scale_ruler_h").assertIsDisplayed()

        // Switch to Skew mode
        composeRule.onNodeWithTag("three_d_mode_skew").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("three_d_skew_ruler_x").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_skew_ruler_y").assertIsDisplayed()

        // Select second layer -> Corner Pin & 3D stay active
        composeRule.runOnUiThread { vm.selectLayer(secondTextId) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("three_d_floating_panel").assertIsDisplayed()

        composeRule.onNodeWithTag("top_tool_corner_pin").performClick()
        composeRule.waitForIdle()
        assertEquals(ToolDrawerTab.CORNER_PIN, vm.activeToolDrawer.value)
        assertTrue(vm.transformLocks.value.positionLocked)
        composeRule.onNodeWithTag("corner_pin_undo_button").assertIsDisplayed()
        composeRule.onNodeWithTag("corner_pin_confirm_button").assertIsDisplayed()
        composeRule.onNodeWithTag("three_d_floating_panel").assertDoesNotExist()

        // Confirm closes Corner Pin and restores prior locks
        composeRule.onNodeWithTag("corner_pin_confirm_button").performClick()
        composeRule.waitForIdle()
        assertEquals(ToolDrawerTab.NONE, vm.activeToolDrawer.value)
        assertFalse(vm.transformLocks.value.positionLocked)
    }

    @Test
    fun backgroundSheetShowsTransparentOnlyWithoutColorBlurPickImageOrBlend() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()

        composeRule.setContent {
            PixelForgeTheme {
                EditorScreen(
                    viewModel = vm,
                    onNavigateBack = {},
                    onPickPhotosDirect = {},
                    onOpenFontLibrary = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("three_dot_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2. Background").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("bg_tab_transparent").assertIsDisplayed()
        composeRule.onNodeWithTag("bg_grid_opacity_slider").assertIsDisplayed()
        composeRule.onNodeWithText("Grid Opacity").assertIsDisplayed()
        composeRule.onNodeWithTag("bg_tab_colors").assertDoesNotExist()
        composeRule.onNodeWithTag("bg_tab_blur").assertDoesNotExist()
        composeRule.onNodeWithTag("bg_tab_image").assertDoesNotExist()
        composeRule.onNodeWithText("Blend Mode").assertDoesNotExist()
        composeRule.onNodeWithText("Pick Image").assertDoesNotExist()
    }

    @Test
    fun homeScreenShowsOldestFirstSaveButtonHoldMultiSelectAndBatchDelete() {
        val sampleProjects = listOf(
            ProjectEntity(
                id = "p_new",
                title = "Newest Banner",
                thumbnailPath = null,
                projectJson = "{}",
                aspectLabel = "16:9",
                layerCount = 4,
                createdAtMillis = 2000L,
                updatedAtMillis = 2000L
            ),
            ProjectEntity(
                id = "p_old",
                title = "Oldest Poster",
                thumbnailPath = null,
                projectJson = "{}",
                aspectLabel = "1:1",
                layerCount = 2,
                createdAtMillis = 1000L,
                updatedAtMillis = 1000L
            )
        )
        var savedId: String? = null
        var openedId: String? = null
        var deletedBatch: Set<String> = emptySet()

        composeRule.setContent {
            PixelForgeTheme {
                HomeScreen(
                    projects = sampleProjects,
                    isDarkTheme = true,
                    onNewProjectClick = {},
                    onOpenProject = { openedId = it },
                    onSaveProject = { savedId = it },
                    onDeleteProject = {},
                    onDeleteProjects = { deletedBatch = it },
                    onToggleTheme = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Oldest Poster").assertIsDisplayed()
        composeRule.onNodeWithText("Newest Banner").assertIsDisplayed()
        composeRule.onNodeWithTag("history_save_button_1").assertIsDisplayed().performClick()
        assertEquals("p_old", savedId)

        // 0.5s hold enters multi-select mode without opening the project
        composeRule.onNodeWithTag("history_project_card_1").performTouchInput {
            down(center)
            advanceEventTime(580)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(null, openedId)
        composeRule.onNodeWithText("1 Selected").assertIsDisplayed()

        // Tapping another card in selection mode adds it to the selection
        composeRule.onNodeWithTag("history_project_card_2").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2 Selected").assertIsDisplayed()

        // Tapping delete button beside selected card deletes all selected projects together
        composeRule.onNodeWithTag("history_delete_selected_button_1").performClick()
        assertEquals(setOf("p_old", "p_new"), deletedBatch)
    }

    @Test
    fun threeDotMenuOpensMyProjectsDrawerAndCanvasPresetsSupport169UnwrappedLayout() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()

        composeRule.setContent {
            PixelForgeTheme {
                EditorScreen(
                    viewModel = vm,
                    onNavigateBack = {},
                    onPickPhotosDirect = {},
                    onOpenFontLibrary = {}
                )
            }
        }

        composeRule.waitForIdle()
        // Open 3-dot menu -> 4. My Projects -> opens right-side drawer
        composeRule.onNodeWithTag("three_dot_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_my_projects").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("my_projects_side_drawer").assertIsDisplayed()

        composeRule.onNodeWithTag("close_projects_drawer_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("my_projects_side_drawer").assertDoesNotExist()

        // Open 1. Canvas from 3-dot menu -> 1:1, 16:9, 9:16, Custom visible -> select 16:9
        composeRule.onNodeWithTag("three_dot_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1. Canvas").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("canvas_preset_1_1").assertIsDisplayed()
        composeRule.onNodeWithTag("canvas_preset_16_9").assertIsDisplayed()
        composeRule.onNodeWithTag("canvas_preset_9_16").assertIsDisplayed()
        composeRule.onNodeWithTag("canvas_preset_custom").assertIsDisplayed()

        composeRule.onNodeWithTag("canvas_preset_16_9").performClick()
        composeRule.waitForIdle()
        assertEquals(1920, vm.document.value.canvas.exportWidth)
        assertEquals(1080, vm.document.value.canvas.exportHeight)
        assertEquals("16:9", vm.document.value.canvas.label)

        // Dismiss sheet & confirm unwrapped 2-row top controls & floating corner buttons remain visible
        composeRule.onNodeWithText("16:9 • 1920x1080").assertIsDisplayed()
        composeRule.onNodeWithTag("top_tool_corner_pin").assertExists()
        composeRule.onNodeWithTag("top_lock_stretch_button").assertExists()
    }

    @Test
    fun pendingPhotoCropCornerDotsResizeCropFrameInsteadOfZooming() {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val pending = PendingImportPhoto(
            uriString = "/tmp/sample_corner.png",
            displayName = "sample_corner",
            originalWidth = 200,
            originalHeight = 200,
            previewBitmap = bmp
        )
        var committedCrop: ImageCropRect? = null

        composeRule.setContent {
            PixelForgeTheme {
                PendingPhotoCropOverlay(
                    pendingPhoto = pending,
                    onDelete = {},
                    onDone = { c, _ -> committedCrop = c }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("import_crop_corner_br").performTouchInput {
            down(center)
            moveBy(Offset(-60f, -60f))
            up()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("import_crop_done_button").performClick()
        assertEquals(0f, committedCrop?.left ?: -1f, 0.01f)
        assertEquals(0f, committedCrop?.top ?: -1f, 0.01f)
        assertTrue("BR corner drag inward must crop right edge (< 0.95)", (committedCrop?.right ?: 1f) < 0.95f)
        assertTrue("BR corner drag inward must crop bottom edge (< 0.95)", (committedCrop?.bottom ?: 1f) < 0.95f)
        bmp.recycle()
    }

    @Test
    fun tappingEmptyThemeBackgroundDismissesOpenToolsAndReturnsMainOptionsWithSavedEdits() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()
        val textId = vm.addTextLayer("Background Tap")!!
        vm.openToolDrawer(ToolDrawerTab.COLOR)
        vm.applyColorToSelectedLayer(0xFFFF4D4FL)

        composeRule.setContent {
            PixelForgeTheme {
                EditorScreen(
                    viewModel = vm,
                    onNavigateBack = {},
                    onPickPhotosDirect = {},
                    onOpenFontLibrary = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tool_drawer_container").assertIsDisplayed()

        val bgBounds = composeRule.onNodeWithTag("editor_root_theme_background").getBoundsInRoot()
        val drawerBounds = composeRule.onNodeWithTag("tool_drawer_container").getBoundsInRoot()

        // Tap outer top-left empty theme background outside the canvas and drawer
        composeRule.onNodeWithTag("editor_root_theme_background").performTouchInput {
            click(Offset(6f, 6f))
        }
        composeRule.waitForIdle()

        assertEquals(
            "Expected deselected layer after tap; bg=$bgBounds drawer=$drawerBounds drawerTab=${vm.activeToolDrawer.value}",
            null,
            vm.document.value.selectedLayerId
        )
        assertEquals(ToolDrawerTab.NONE, vm.activeToolDrawer.value)
        composeRule.onNodeWithTag("bottom_tool_photo").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_text_plus").assertIsDisplayed()
        val updated = StudioViewModel.findLayerRecursive(vm.document.value.layers, textId) as TextLayer
        assertEquals(0xFFFF4D4FL, updated.colorArgb)
    }

    @Test
    fun portrait916ActivatesTopPagerWithFullSizeControlsAndNarrowBottomBarPaginates() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()
        vm.selectCanvasPreset(CanvasDimensions.PORTRAIT_9_16)
        vm.addTextLayer("Pager Test")
        vm.openToolDrawer(ToolDrawerTab.TEXT)

        composeRule.setContent {
            PixelForgeTheme {
                Box(modifier = Modifier.width(240.dp)) {
                    EditorScreen(
                        viewModel = vm,
                        onNavigateBack = {},
                        onPickPhotosDirect = {},
                        onOpenFontLibrary = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("top_row2_pager").assertIsDisplayed()
        composeRule.onNodeWithTag("top_pager_dot_0").assertIsDisplayed()
        composeRule.onNodeWithTag("top_pager_dot_1").assertIsDisplayed()

        composeRule.onNodeWithTag("bottom_tool_pager").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_pager_dot_0").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_pager_dot_1").assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun warpedTextLayerShiftsSelectionHandlesWithCornerPinDeformation() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()
        val textId = vm.addTextLayer("Warp Me")!!

        composeRule.setContent {
            PixelForgeTheme {
                EditorScreen(
                    viewModel = vm,
                    onNavigateBack = {},
                    onPickPhotosDirect = {},
                    onOpenFontLibrary = {}
                )
            }
        }

        composeRule.waitForIdle()
        val unwarpedDeleteBounds = composeRule.onNodeWithTag("selection_delete_cross_button").getBoundsInRoot()
        val unwarpedZoomBounds = composeRule.onNodeWithTag("corner_zoom_icon_handle").getBoundsInRoot()

        composeRule.runOnUiThread {
            vm.updateSelectedLayerCornerPinTransient(textId, cornerIndex = 0, deltaDxRatio = 0.30f, deltaDyRatio = 0.25f)
            vm.updateSelectedLayerCornerPinTransient(textId, cornerIndex = 2, deltaDxRatio = -0.25f, deltaDyRatio = -0.20f)
        }
        composeRule.waitForIdle()

        val warpedDeleteBounds = composeRule.onNodeWithTag("selection_delete_cross_button").getBoundsInRoot()
        val warpedZoomBounds = composeRule.onNodeWithTag("corner_zoom_icon_handle").getBoundsInRoot()

        assertTrue(
            "Delete handle must follow warped TL corner inward/downward",
            warpedDeleteBounds.left > unwarpedDeleteBounds.left && warpedDeleteBounds.top > unwarpedDeleteBounds.top
        )
        assertTrue(
            "Zoom handle must follow warped BR corner inward/upward",
            warpedZoomBounds.left < unwarpedZoomBounds.left && warpedZoomBounds.top < unwarpedZoomBounds.top
        )
    }
}


