package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProjectEntity
import com.example.fonts.FontDiagnosticsSnapshot
import com.example.fonts.FontImportProgress
import com.example.fonts.FontRenderEngine
import com.example.fonts.FontRepository
import com.example.fonts.FontSource
import com.example.model.CanvasDimensions
import com.example.model.DrawPoint
import com.example.model.DrawStroke
import com.example.model.ImageCropRect
import com.example.model.ImageCropShape
import com.example.model.ImageLayer
import com.example.model.LayerShapeType
import com.example.model.ProjectDocument
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import com.example.model.VectorStrokePattern
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
                    onPickBackgroundImageDirect = {},
                    onOpenFontLibrary = {}
                )
            }
        }

        composeRule.waitForIdle()
        // Unselected main screen: Rotate is hidden; Photo opens gallery directly
        composeRule.onNodeWithTag("bottom_tool_photo").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_text_plus").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_shapes").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_draw").assertIsDisplayed()
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
        vm.addShapeLayer(LayerShapeType.STAR)
        val shapeId = vm.document.value.selectedLayerId!!
        vm.selectLayer(textId)

        composeRule.setContent {
            PixelForgeTheme {
                EditorScreen(
                    viewModel = vm,
                    onNavigateBack = {},
                    onPickPhotosDirect = {},
                    onPickBackgroundImageDirect = {},
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

        // Select ShapeLayer -> Corner Pin & 3D now also work for Shapes!
        composeRule.runOnUiThread { vm.selectLayer(shapeId) }
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
    fun shapesTabDisplaysAlightWhiteShapeGridTogglesEditPointsAndOpensVectorControls() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()

        composeRule.setContent {
            PixelForgeTheme {
                EditorScreen(
                    viewModel = vm,
                    onNavigateBack = {},
                    onPickPhotosDirect = {},
                    onPickBackgroundImageDirect = {},
                    onOpenFontLibrary = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_tool_shapes").performClick()
        composeRule.waitForIdle()

        // Shape picker grid shows white silhouette tiles
        composeRule.onNodeWithTag("shape_picker_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("shape_pick_circle").assertIsDisplayed()
        composeRule.onNodeWithTag("shape_pick_triangle").assertIsDisplayed()
        composeRule.onNodeWithTag("shape_pick_star").assertIsDisplayed()

        // Tapping Triangle adds a white Triangle shape and switches bottom bar + Edit Points ON
        composeRule.onNodeWithTag("shape_pick_triangle").performClick()
        composeRule.waitForIdle()
        val added = StudioViewModel.findLayerRecursive(vm.document.value.layers, vm.document.value.selectedLayerId) as ShapeLayer
        assertEquals(LayerShapeType.TRIANGLE, added.shapeType)
        assertEquals(0xFFFFFFFFL, added.fillColorArgb)
        assertTrue(vm.shapeEditPointsMode.value)
        composeRule.onNodeWithTag("shape_edit_points_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tool_shape_points").assertIsDisplayed()

        // Toggle between Edit Points and Bounds mode
        composeRule.onNodeWithTag("shape_edit_points_toggle").performClick()
        composeRule.waitForIdle()
        assertFalse(vm.shapeEditPointsMode.value)
    }

    @Test
    fun drawStrokeControlsPanelSupportsAllNineSectionsContextSyncAndWidthPoints() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()

        composeRule.setContent {
            PixelForgeTheme {
                EditorScreen(
                    viewModel = vm,
                    onNavigateBack = {},
                    onPickPhotosDirect = {},
                    onPickBackgroundImageDirect = {},
                    onOpenFontLibrary = {}
                )
            }
        }

        composeRule.waitForIdle()
        // Open Draw tool panel -> Size dots + Stroke controls toggle visible
        composeRule.onNodeWithTag("bottom_tool_draw").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("draw_size_dot_0").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("draw_stroke_controls_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("stroke_controls_panel").assertIsDisplayed()
        composeRule.onNodeWithText("New Stroke Defaults").assertIsDisplayed()

        // All 9 collapsible section headers exist
        listOf(
            "stroke_section_thickness",
            "stroke_section_taper",
            "stroke_section_cap_and_join",
            "stroke_section_stroke_style",
            "stroke_section_opacity",
            "stroke_section_smoothing",
            "stroke_section_width_profile",
            "stroke_section_pressure",
            "stroke_section_advanced"
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).assertExists()
        }

        // Toggle linked/unlinked in Thickness (initially expanded)
        composeRule.onNodeWithTag("stroke_link_widths_toggle").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(vm.drawStrokeStyle.value.widthsLinked)

        // Expand Stroke Style and choose Dashed
        composeRule.onNodeWithTag("stroke_section_stroke_style").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("stroke_pattern_dashed").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(VectorStrokePattern.DASHED, vm.drawStrokeStyle.value.pattern)

        // Draw a stroke -> badge automatically switches to Editing Selected Layer and adds non-destructively
        composeRule.runOnUiThread {
            vm.addDrawStroke(
                DrawStroke(
                    colorArgb = 0xFFFF4D4FL,
                    strokeWidthNorm = 0.02f,
                    rawPoints = listOf(DrawPoint(0.2f, 0.2f), DrawPoint(0.8f, 0.8f)),
                    points = listOf(DrawPoint(0.2f, 0.2f), DrawPoint(0.8f, 0.8f)),
                    style = vm.drawStrokeStyle.value
                )
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Editing Selected Layer").performScrollTo().assertIsDisplayed()

        // Expand Width Profile section & add/remove an individual width point
        composeRule.onNodeWithTag("stroke_section_width_profile").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_width_point_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, vm.drawStrokeStyle.value.widthPoints.size)

        composeRule.onNodeWithTag("remove_width_point_0").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(0, vm.drawStrokeStyle.value.widthPoints.size)
    }

    @Test
    fun homeScreenShowsOldestFirstSaveButtonHoldMultiSelectAndBatchDelete() {
        val sampleProjects = listOf(
            ProjectEntity(
                id = "p_old",
                title = "Oldest Poster",
                thumbnailPath = null,
                projectJson = "{}",
                aspectLabel = "1:1",
                layerCount = 2,
                createdAtMillis = 1000L,
                updatedAtMillis = 1000L
            ),
            ProjectEntity(
                id = "p_new",
                title = "Newest Banner",
                thumbnailPath = null,
                projectJson = "{}",
                aspectLabel = "16:9",
                layerCount = 4,
                createdAtMillis = 2000L,
                updatedAtMillis = 2000L
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
                    onPickBackgroundImageDirect = {},
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
}

