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
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.fonts.FontDiagnosticsSnapshot
import com.example.fonts.FontImportProgress
import com.example.fonts.FontRenderEngine
import com.example.fonts.FontRepository
import com.example.fonts.FontSource
import com.example.model.ImageCropRect
import com.example.model.ImageCropShape
import com.example.model.TextLayer
import com.example.ui.PendingImportPhoto
import com.example.ui.StudioViewModel
import com.example.ui.components.FontLibrarySheet
import com.example.ui.components.PendingPhotoCropOverlay
import com.example.ui.components.TextToolPanel
import com.example.ui.screens.EditorScreen
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
    fun editorScreenTopPagerLocksAndContextSensitiveBottomBarsWorkCorrectly() {
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

        // Top pager Page 0 -> switch to Page 1 -> verify S/Z/R/P locks & defaults
        composeRule.onNodeWithTag("top_delete_button").assertIsDisplayed()
        composeRule.onNodeWithTag("top_pager_dot_1").performClick()
        composeRule.waitForIdle()

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
    fun pendingPhotoCropOverlaySupportsFreeCircleAnd916Modes() {
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
        composeRule.onNodeWithTag("import_crop_mode_circle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("import_crop_done_button").performClick()
        assertEquals(ImageCropShape.CIRCLE, committedShape)
        assertTrue((committedCrop?.right ?: 0f) > (committedCrop?.left ?: 1f))

        composeRule.onNodeWithTag("import_crop_mode_9_16").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("import_crop_done_button").performClick()
        assertEquals(ImageCropShape.RECTANGLE, committedShape)

        composeRule.onNodeWithTag("import_crop_delete_button").performClick()
        assertTrue(deleted)
        bmp.recycle()
    }
}
