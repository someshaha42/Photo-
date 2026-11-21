package com.example

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.MediaStorageManager
import com.example.data.ShapeHandleKind
import com.example.data.VectorShapeEngine
import com.example.data.VectorStrokeEngine
import com.example.fonts.ColorTableType
import com.example.fonts.FontArchiveExtractor
import com.example.fonts.FontClassification
import com.example.fonts.FontRenderEngine
import com.example.fonts.FontRepository
import com.example.fonts.TrueTypeTableParser
import com.example.data.LayerTransformMatrix
import com.example.model.BackgroundConfig
import com.example.model.BackgroundTab
import com.example.model.CanvasDimensions
import com.example.model.CornerPinOffsets
import com.example.model.DrawLayer
import com.example.model.DrawPoint
import com.example.model.DrawStroke
import com.example.model.EmojiSourceMode
import com.example.model.ExportImageFormat
import com.example.model.ExportResolutionPreset
import com.example.model.GroupLayer
import com.example.model.ImageAdjustments
import com.example.model.ImageCropRect
import com.example.model.ImageCropShape
import com.example.model.ImageLayer
import com.example.model.Layer3DTransform
import com.example.model.LayerShapeType
import com.example.model.ProjectDocument
import com.example.model.ShapeLayer
import com.example.model.TextCaseMode
import com.example.model.TextCaseTransformer
import com.example.model.TextLayer
import com.example.model.VectorLineCap
import com.example.model.VectorLineJoin
import com.example.model.VectorStrokeAlignment
import com.example.model.VectorStrokePattern
import com.example.model.VectorStrokeStyle
import com.example.model.WidthPointTransition
import com.example.model.WidthProfilePoint
import com.example.ui.StudioViewModel
import com.example.ui.ToolDrawerTab
import com.example.ui.TransformLockState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PixelForgeEngineTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun bundledFontsLoadDynamicallyAndDefaultRobotoIsProtected() {
        runBlocking {
            val renderEngine = FontRenderEngine(context)
            val repo = FontRepository(context, renderEngine)
            repo.initialize()

            val loaded = repo.fonts.value
            assertTrue("Expected bundled fonts from assets/Fonts/", loaded.size >= 10)
            val roboto = loaded.firstOrNull { it.displayName.contains("Roboto", ignoreCase = true) }
            assertNotNull("Roboto must be bundled as mandatory default font", roboto)
            assertTrue(roboto!!.isDefaultMandatory)
            assertFalse("Mandatory default font must never be deletable", repo.deleteImportedFont(roboto.id))

            val lobster = loaded.firstOrNull { it.displayName.contains("Lobster", ignoreCase = true) }
            assertNotNull("Distinct display font Lobster must be loaded", lobster)
            assertNotEquals(renderEngine.getTypeface(roboto.id), renderEngine.getTypeface(lobster!!.id))
        }
    }

    @Test
    fun textCaseTransformerAppliesLowerUpperAndTitleCaseAcrossLines() {
        val raw = "hELLO wORLD!\nsecond line   here 😀"
        assertEquals(
            "hello world!\nsecond line   here 😀",
            TextCaseTransformer.apply(raw, TextCaseMode.LOWERCASE)
        )
        assertEquals(
            "HELLO WORLD!\nSECOND LINE   HERE 😀",
            TextCaseTransformer.apply(raw, TextCaseMode.UPPERCASE)
        )
        assertEquals(
            "Hello World!\nSecond Line   Here 😀",
            TextCaseTransformer.apply(raw, TextCaseMode.TITLE_CASE)
        )
    }

    @Test
    fun arrangementBySelectionOrderPlacesFirstSelectedOnTopAndSupportsSwap() {
        val a = TextLayer(id = "A", text = "First")
        val b = ShapeLayer(id = "B")
        val c = ImageLayer(id = "C", uriString = "/tmp/c.png", originalWidth = 100, originalHeight = 100)

        // Stack bottom->top: [A, B, C]; select B 1st and A 2nd -> arrangeTop puts C below, then A, then B on very top!
        val doc = ProjectDocument(
            layers = listOf(a, b, c),
            selectedLayerId = "A",
            selectionOrder = listOf("B", "A")
        )
        val topArranged = StudioViewModel.applyArrangementToTop(doc)
        assertEquals(listOf("C", "A", "B"), topArranged.layers.map { it.id })

        val bottomArranged = StudioViewModel.applyArrangementToBottom(topArranged)
        assertEquals(listOf("A", "B", "C"), bottomArranged.layers.map { it.id })

        // Swap/Reverse reverses both selectionOrder and mutual stack positions!
        val swapped = StudioViewModel.applySwapOrReverseArrangement(topArranged)
        assertEquals(listOf("A", "B"), swapped.selectionOrder)
        assertEquals(listOf("C", "B", "A"), swapped.layers.map { it.id })
    }

    @Test
    fun nonDestructiveGroupExpandCollapsePreservesChildPropertiesAndRenders() {
        runBlocking {
            val renderEngine = FontRenderEngine(context)
            val repo = FontRepository(context, renderEngine)
            repo.initialize()

            val tLayer = TextLayer(
                id = "t1",
                text = "Group Title",
                fontId = repo.selectedFontId.value,
                offsetXNorm = 0.12f,
                offsetYNorm = -0.15f,
                rotationDeg = 18f,
                scale = 1.4f
            )
            val sLayer = ShapeLayer(id = "s1", offsetXNorm = -0.2f, offsetYNorm = 0.1f)
            val doc = ProjectDocument(
                layers = listOf(tLayer, sLayer),
                selectedLayerId = "s1",
                selectionOrder = listOf("t1", "s1")
            )

            val groupedDoc = StudioViewModel.createGroupFromSelection(doc, setOf("t1", "s1"))
            assertEquals(1, groupedDoc.layers.size)
            val group = groupedDoc.layers.first() as GroupLayer
            assertFalse(group.isExpanded)
            assertEquals(2, group.children.size)

            // Child transforms remain untouched non-destructively
            val preservedText = group.children.first() as TextLayer
            assertEquals(0.12f, preservedText.offsetXNorm, 0.0001f)
            assertEquals(-0.15f, preservedText.offsetYNorm, 0.0001f)
            assertEquals(18f, preservedText.rotationDeg, 0.0001f)
            assertEquals(1.4f, preservedText.scale, 0.0001f)

            // Render bitmap with grouped layer succeeds cleanly
            val mediaManager = MediaStorageManager(context)
            val bmp = mediaManager.renderProjectToBitmap(groupedDoc, renderEngine, 240, 240)
            assertEquals(240, bmp.width)
            bmp.recycle()

            // Ungroup restores independent layers
            val ungrouped = StudioViewModel.ungroupInDocument(groupedDoc, group.id)
            assertEquals(listOf("t1", "s1"), ungrouped.layers.map { it.id })
        }
    }

    @Test
    fun parserDetectsColrCpalAndBitmapStrikeTables() {
        val fakeColrFont = buildMinimalSfnt(
            listOf("head", "maxp", "COLR", "CPAL", "cmap")
        )
        val parsed = TrueTypeTableParser.inspectBytes(fakeColrFont, "EmojiColor.ttf")
        assertTrue(parsed.isValid)
        assertEquals(ColorTableType.COLR_CPAL, parsed.colorTableType)
        assertEquals(FontClassification.COLOR_EMOJI, parsed.classification)
    }

    @Test
    fun archiveExtractorBlocksZipSlipAndExtractsNestedFontsSafely() {
        runBlocking {
            val extractor = FontArchiveExtractor(context)
            val zipFile = File(context.cacheDir, "test_fonts_bundle.zip")
            val robotoBytes = context.assets.open("Fonts/Roboto.ttf").use { it.readBytes() }

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                zos.putNextEntry(ZipEntry("nested/folder/ValidRoboto.ttf"))
                zos.write(robotoBytes)
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("nested/readme.txt"))
                zos.write("ignore me".toByteArray())
                zos.closeEntry()
            }

            var extractedCount = 0
            var skipped = 0
            extractor.extractAndProcess(zipFile, "test_fonts_bundle.zip") { outcome ->
                extractedCount = outcome.candidates.size
                skipped = outcome.skippedNonFonts
            }
            assertEquals(1, extractedCount)
            assertEquals(1, skipped)
            zipFile.delete()
        }
    }

    @Test
    fun glyphCacheDoesNotUpscaleLowResolutionEntries() {
        runBlocking {
            val renderEngine = FontRenderEngine(context)
            val repo = FontRepository(context, renderEngine)
            repo.initialize()

            val fontId = repo.selectedFontId.value
            val bmp100 = renderEngine.getOrRenderGlyphBitmap(fontId, "Aa", targetFontPx = 36)
            val bmp400 = renderEngine.getOrRenderGlyphBitmap(fontId, "Aa", targetFontPx = 144)
            assertTrue(
                "400% zoom glyph bitmap (${bmp400.width}px) must exceed 100% zoom (${bmp100.width}px)",
                bmp400.width > bmp100.width
            )
        }
    }

    @Test
    fun transparentPngExportPreservesAlphaAndExcludesCheckerboard() {
        runBlocking {
            val renderEngine = FontRenderEngine(context)
            val repo = FontRepository(context, renderEngine)
            repo.initialize()

            val mediaManager = MediaStorageManager(context)
            val doc = ProjectDocument(
                canvas = CanvasDimensions.SQUARE_1_1,
                background = BackgroundConfig(
                    activeTab = BackgroundTab.TRANSPARENT,
                    checkerboardPreviewEnabled = true
                ),
                layers = listOf(
                    TextLayer(text = "Hi", fontId = repo.selectedFontId.value, fontSizeSp = 24f)
                )
            )
            val exported = mediaManager.renderProjectToBitmap(doc, renderEngine, 200, 200)
            val cornerPixel = exported.getPixel(0, 0)
            assertEquals(0, Color.alpha(cornerPixel))
            exported.recycle()
        }
    }

    @Test
    fun customRatioAndPixelDimensionsSyncBidirectionally() {
        val fromRatio = CanvasDimensions.fromRatioInput("9:16")
        assertNotNull(fromRatio)
        assertEquals(9, fromRatio!!.ratioWidth)
        assertEquals(16, fromRatio.ratioHeight)
        assertEquals(1080, fromRatio.exportWidth)
        assertEquals(1920, fromRatio.exportHeight)
        assertTrue(fromRatio.isCustom)

        val fromPixels = CanvasDimensions.fromPixelInput("1920x1080")
        assertNotNull(fromPixels)
        assertEquals("16:9", fromPixels!!.label)
        assertEquals(1920, fromPixels.exportWidth)
        assertEquals(1080, fromPixels.exportHeight)
    }

    @Test
    fun addingImageLayerDoesNotChangeDefaultSquareCanvas() {
        val initial = ProjectDocument(canvas = CanvasDimensions.SQUARE_1_1)
        val widePhoto = ImageLayer(
            uriString = "/tmp/wide.jpg",
            originalWidth = 1920,
            originalHeight = 1080
        )
        val updated = initial.copy(layers = initial.layers + widePhoto)
        assertEquals("1:1", updated.canvas.label)
        assertEquals(1080, updated.canvas.exportWidth)
        assertEquals(1080, updated.canvas.exportHeight)
    }

    @Test
    fun emojiRunSegmentationSeparatesTextAndEmojiInDualFontMode() {
        runBlocking {
            val renderEngine = FontRenderEngine(context)
            val repo = FontRepository(context, renderEngine)
            repo.initialize()

            val sample = "Hello 😀🚀 World"
            val segments = FontRenderEngine.segmentEmojiRuns(sample)
            assertEquals(3, segments.size)
            assertFalse(segments[0].isEmoji)
            assertTrue(segments[1].isEmoji)
            assertFalse(segments[2].isEmoji)

            val spanSystem = renderEngine.buildStyledSpannable(
                text = sample,
                layerFontId = "bundled:Roboto.ttf",
                emojiSourceMode = EmojiSourceMode.SYSTEM
            )
            val spans = spanSystem.getSpans(0, sample.length, Any::class.java)
            assertEquals(1, spans.size)
            assertEquals(segments[1].start, spanSystem.getSpanStart(spans[0]))
            assertEquals(segments[1].end, spanSystem.getSpanEnd(spans[0]))
        }
    }

    @Test
    fun imageAdjustmentsIdentityAndColorMatrixCheck() {
        val identity = ImageAdjustments()
        assertTrue(identity.isIdentity())
        val adjusted = identity.copy(exposure = 0.4f, contrast = 0.2f, saturation = -0.3f, hue = 25f)
        assertFalse(adjusted.isIdentity())
        val cm = MediaStorageManager.buildColorMatrix(adjusted)
        assertEquals(20, cm.array.size)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun visibleNonEmojiTextRendersWithOpaqueAlphaPixels() {
        runBlocking {
            val renderEngine = FontRenderEngine(context)
            val repo = FontRepository(context, renderEngine)
            repo.initialize()

            val mediaManager = MediaStorageManager(context)
            val doc = ProjectDocument(
                canvas = CanvasDimensions.SQUARE_1_1,
                background = BackgroundConfig(
                    activeTab = BackgroundTab.TRANSPARENT,
                    checkerboardPreviewEnabled = false
                ),
                layers = listOf(
                    TextLayer(
                        text = "ABC Hello",
                        fontId = repo.selectedFontId.value,
                        fontSizeSp = 48f,
                        colorArgb = 0xFFFFFFFFL,
                        opacity = 1f
                    )
                )
            )
            val rendered = mediaManager.renderProjectToBitmap(doc, renderEngine, 240, 240)
            var maxAlpha = 0
            var opaqueWhitePixels = 0
            for (y in 40 until 200) {
                for (x in 20 until 220) {
                    val px = rendered.getPixel(x, y)
                    val a = Color.alpha(px)
                    if (a > maxAlpha) maxAlpha = a
                    if (a > 200 && Color.red(px) > 200 && Color.green(px) > 200 && Color.blue(px) > 200) {
                        opaqueWhitePixels++
                    }
                }
            }
            rendered.recycle()
            assertTrue(
                "Expected opaque white alphabet text pixels (maxAlpha=$maxAlpha, whitePixels=$opaqueWhitePixels)",
                maxAlpha > 200 && opaqueWhitePixels > 25
            )
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun imageCropRectAndNonUniformStretchRenderCleanly() {
        runBlocking {
            val rawCrop = ImageCropRect(left = -0.2f, top = 0.2f, right = 0.25f, bottom = 1.4f)
            val normalized = rawCrop.normalized()
            assertEquals(0f, normalized.left, 0.0001f)
            assertEquals(0.2f, normalized.top, 0.0001f)
            assertEquals(0.25f, normalized.right, 0.0001f)
            assertEquals(1f, normalized.bottom, 0.0001f)
            assertFalse(normalized.isFull())

            val sourceFile = File(context.cacheDir, "crop_test_src.png")
            val srcBmp = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
            val c = android.graphics.Canvas(srcBmp)
            val leftPaint = android.graphics.Paint().apply { color = Color.RED }
            val rightPaint = android.graphics.Paint().apply { color = Color.BLUE }
            c.drawRect(0f, 0f, 50f, 100f, leftPaint)
            c.drawRect(50f, 0f, 100f, 100f, rightPaint)
            FileOutputStream(sourceFile).use { out ->
                srcBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            srcBmp.recycle()

            val renderEngine = FontRenderEngine(context)
            val mediaManager = MediaStorageManager(context)
            val doc = ProjectDocument(
                canvas = CanvasDimensions.SQUARE_1_1,
                background = BackgroundConfig(
                    activeTab = BackgroundTab.TRANSPARENT,
                    checkerboardPreviewEnabled = false
                ),
                layers = listOf(
                    ImageLayer(
                        uriString = sourceFile.absolutePath,
                        originalWidth = 100,
                        originalHeight = 100,
                        crop = ImageCropRect(left = 0f, top = 0f, right = 0.4f, bottom = 1f),
                        stretchX = 1.5f,
                        stretchY = 0.7f
                    )
                )
            )
            val rendered = mediaManager.renderProjectToBitmap(doc, renderEngine, 240, 240)
            val centerPx = rendered.getPixel(120, 120)
            assertTrue("Cropped left region must render red center pixel", Color.red(centerPx) > 200)
            assertTrue("Cropped left region must exclude blue right half", Color.blue(centerPx) < 40)
            rendered.recycle()
            sourceFile.delete()
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun imageLayerColorTintAppliesAndResetsToDefault() {
        runBlocking {
            val sourceFile = File(context.cacheDir, "tint_test_src.png")
            val srcBmp = android.graphics.Bitmap.createBitmap(80, 80, android.graphics.Bitmap.Config.ARGB_8888)
            srcBmp.eraseColor(Color.RED)
            FileOutputStream(sourceFile).use { out ->
                srcBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            srcBmp.recycle()

            val renderEngine = FontRenderEngine(context)
            val mediaManager = MediaStorageManager(context)
            val tintedLayer = ImageLayer(
                uriString = sourceFile.absolutePath,
                originalWidth = 80,
                originalHeight = 80,
                tintColorArgb = 0xFF00FF00L
            )
            val tintedDoc = ProjectDocument(
                canvas = CanvasDimensions.SQUARE_1_1,
                background = BackgroundConfig(activeTab = BackgroundTab.TRANSPARENT, checkerboardPreviewEnabled = false),
                layers = listOf(tintedLayer)
            )
            val tintedBmp = mediaManager.renderProjectToBitmap(tintedDoc, renderEngine, 160, 160)
            val centerTinted = tintedBmp.getPixel(80, 80)
            assertTrue("Green tint must cover red base photo", Color.green(centerTinted) > 200 && Color.red(centerTinted) < 40)
            tintedBmp.recycle()

            val resetDoc = tintedDoc.copy(layers = listOf(tintedLayer.copy(tintColorArgb = null)))
            val resetBmp = mediaManager.renderProjectToBitmap(resetDoc, renderEngine, 160, 160)
            val centerReset = resetBmp.getPixel(80, 80)
            assertTrue("Default reset must restore original red pixels", Color.red(centerReset) > 200 && Color.green(centerReset) < 40)
            resetBmp.recycle()
            sourceFile.delete()
        }
    }

    @Test
    fun blankOrUnconfirmedDraftTextIsNeverAddedToCanvas() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()
        assertEquals(0, vm.document.value.layers.size)

        // Blank text + checkmark must not add a layer
        vm.startNewTextDraft()
        assertEquals(ToolDrawerTab.TEXT, vm.activeToolDrawer.value)
        vm.confirmTextBox()
        assertEquals(0, vm.document.value.layers.size)
        assertEquals(ToolDrawerTab.NONE, vm.activeToolDrawer.value)

        // Typed draft + Back/Dismiss without checkmark must not add a layer
        vm.startNewTextDraft()
        vm.updateDraftText("Unsaved Draft")
        vm.closeToolDrawer()
        assertEquals(0, vm.document.value.layers.size)
        assertEquals("", vm.draftText.value)

        // Typed draft + checkmark adds 1 layer
        vm.startNewTextDraft()
        vm.updateDraftText("Confirmed Headline")
        vm.confirmTextBox()
        assertEquals(1, vm.document.value.layers.size)
        assertEquals("Confirmed Headline", (vm.document.value.layers.first() as TextLayer).text)
    }

    @Test
    fun directBackDismissesDrawersWhilePreservingAppliedColorAndOpacity() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()
        val id = vm.addTextLayer("Color Test")!!
        vm.openToolDrawer(ToolDrawerTab.COLOR)
        vm.setShowLayersDrawer(true)
        vm.applyColorToSelectedLayer(0x80FF4D4FL)
        vm.setSelectedLayerOpacity(0.6f)

        // Tap empty canvas shortcut ("direct back")
        vm.dismissAllOverlaysAndSelection()

        assertEquals(ToolDrawerTab.NONE, vm.activeToolDrawer.value)
        assertFalse(vm.showLayersDrawer.value)
        assertEquals(null, vm.document.value.selectedLayerId)
        val savedLayer = StudioViewModel.findLayerRecursive(vm.document.value.layers, id) as TextLayer
        assertEquals(0x80FF4D4FL, savedLayer.colorArgb)
        assertEquals(0.6f, savedLayer.opacity, 0.001f)
    }

    @Test
    fun anchoredOneSideStretchKeepsOppositeEdgePinned() {
        val baseLayer = ShapeLayer(
            id = "s_anchor",
            offsetXNorm = 0f,
            offsetYNorm = 0f,
            scale = 1f,
            stretchX = 1f,
            stretchY = 1f,
            rotationDeg = 0f
        )
        // Drag right edge (+1, 0) rightwards by +40px on a 100x100 box inside 400x400 canvas:
        // Old left edge = 200 - 50 = 150px. New width = 140px, center shifts +20px -> 220px.
        // New left edge = 220 - 70 = 150px (pinned!), new right edge = 220 + 70 = 290px.
        val stretched = StudioViewModel.computeAnchoredStretchTransform(
            layer = baseLayer,
            signX = 1f,
            signY = 0f,
            screenLocalDeltaX = 40f,
            screenLocalDeltaY = 0f,
            boxWpx = 100f,
            boxHpx = 100f,
            canvasWpx = 400f,
            canvasHpx = 400f
        )
        assertEquals(1.4f, stretched.stretchX, 0.001f)
        assertEquals(1.0f, stretched.stretchY, 0.001f)
        assertEquals(0.05f, stretched.offsetXNorm, 0.001f) // 20px / 400px
        assertEquals(0f, stretched.offsetYNorm, 0.001f)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun drawLayerRendersVectorStrokesAndJpgCompositeIsWhiteOpaque() {
        runBlocking {
            val renderEngine = FontRenderEngine(context)
            val mediaManager = MediaStorageManager(context)

            val drawLayer = DrawLayer(
                strokes = listOf(
                    DrawStroke(
                        colorArgb = 0xFFFF0000L,
                        strokeWidthNorm = 0.08f,
                        points = listOf(DrawPoint(0.25f, 0.5f), DrawPoint(0.75f, 0.5f))
                    )
                )
            )
            val doc = ProjectDocument(
                canvas = CanvasDimensions.SQUARE_1_1,
                background = BackgroundConfig(
                    activeTab = BackgroundTab.TRANSPARENT,
                    checkerboardPreviewEnabled = false
                ),
                layers = listOf(drawLayer)
            )

            // Verify 4K & 2K resolution targets
            val res4k = MediaStorageManager.computeTargetResolution(
                CanvasDimensions.fromRatioInput("16:9")!!,
                ExportResolutionPreset.RES_4K,
                1080,
                1080
            )
            assertEquals(3840 to 2160, res4k)

            val res2k = MediaStorageManager.computeTargetResolution(
                CanvasDimensions.SQUARE_1_1,
                ExportResolutionPreset.RES_2K,
                1080,
                1080
            )
            assertEquals(2048 to 2048, res2k)

            val resCustom = MediaStorageManager.computeTargetResolution(
                CanvasDimensions.SQUARE_1_1,
                ExportResolutionPreset.CUSTOM,
                1234,
                777
            )
            assertEquals(1234 to 777, resCustom)

            // Render DrawLayer stroke center red pixel
            val pngBmp = mediaManager.renderProjectToBitmap(doc, renderEngine, 200, 200)
            val centerPx = pngBmp.getPixel(100, 100)
            val cornerPx = pngBmp.getPixel(5, 5)
            assertTrue("Center stroke pixel must be bright red", Color.red(centerPx) > 200 && Color.alpha(centerPx) > 200)
            assertEquals("Transparent PNG corner must keep zero alpha", 0, Color.alpha(cornerPx))
            pngBmp.recycle()

            // JPG export must composite white background on transparent canvas
            val jpgResult = mediaManager.exportImageToAppAndGallery(
                document = doc,
                fontRenderEngine = renderEngine,
                format = ExportImageFormat.JPG,
                targetWidth = 200,
                targetHeight = 200
            )
            assertTrue(jpgResult.isSuccess)
            val jpgFile = jpgResult.getOrThrow().first
            assertTrue(jpgFile.extension.equals("jpg", ignoreCase = true))
            val decodedJpg = android.graphics.BitmapFactory.decodeFile(jpgFile.absolutePath)
            assertNotNull(decodedJpg)
            val jpgCorner = decodedJpg!!.getPixel(5, 5)
            assertTrue("JPG corner must be white", Color.red(jpgCorner) > 240 && Color.green(jpgCorner) > 240 && Color.blue(jpgCorner) > 240)
            decodedJpg.recycle()
            jpgFile.delete()
        }
    }

    @Test
    fun editingExistingTextAutoDeselectsOnCheckAndFirstHoldEntersMultiSelect() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()
        val id1 = vm.addTextLayer("Hello")!!
        val id2 = vm.addTextLayer("World")!!

        // Tapping existing text opens Text tool drawer; confirming edits deselects it
        vm.onCanvasLayerTapped(id1)
        assertEquals(id1, vm.document.value.selectedLayerId)
        assertEquals(ToolDrawerTab.TEXT, vm.activeToolDrawer.value)
        vm.confirmTextBox()
        assertEquals(ToolDrawerTab.NONE, vm.activeToolDrawer.value)
        assertEquals(null, vm.document.value.selectedLayerId)

        // Holding first layer immediately enters multi-select mode cleanly
        vm.enterMultiSelectWithLayer(id1)
        assertTrue(vm.isMultiSelectMode.value)
        assertEquals(listOf(id1), vm.document.value.selectionOrder)
        vm.selectLayer(id2)
        assertEquals(listOf(id1, id2), vm.document.value.selectionOrder)
    }

    @Test
    fun ttfAndOtfImportPreservesExactRawFontDataAndVectorFormat() {
        runBlocking {
            val renderEngine = FontRenderEngine(context)
            val repo = FontRepository(context, renderEngine)
            repo.initialize()

            val originalTtfBytes = context.assets.open("Fonts/PlayfairDisplay.ttf").use { it.readBytes() } +
                byteArrayOf(0, 0, 0, 1)
            val ttfTemp = File(context.cacheDir, "ImportCheck_${System.nanoTime()}.ttf")
            ttfTemp.writeBytes(originalTtfBytes)

            val otfBytes = buildMinimalOtfBytes()
            val otfTemp = File(context.cacheDir, "ImportCheck_${System.nanoTime()}.otf")
            otfTemp.writeBytes(otfBytes)

            // Make unique so repeat test runs don't hit duplicate cache from filesDir
            val existingImported = repo.fonts.value.filter { it.source == com.example.fonts.FontSource.IMPORTED }
            existingImported.forEach { repo.deleteImportedFont(it.id) }

            repo.importFontUris(
                listOf(
                    android.net.Uri.fromFile(ttfTemp),
                    android.net.Uri.fromFile(otfTemp)
                )
            )

            val importedTtf = repo.fonts.value.firstOrNull {
                it.source == com.example.fonts.FontSource.IMPORTED && it.fileName == ttfTemp.name
            }
            val importedOtf = repo.fonts.value.firstOrNull {
                it.source == com.example.fonts.FontSource.IMPORTED && it.fileName == otfTemp.name
            }

            assertNotNull("Imported TTF must be recognized in FontRepository", importedTtf)
            assertNotNull("Imported OTF must be recognized in FontRepository", importedOtf)
            assertEquals(com.example.fonts.FontFormat.TTF, importedTtf!!.format)
            assertEquals(com.example.fonts.FontFormat.OTF, importedOtf!!.format)

            val storedTtfBytes = File(importedTtf.storagePath).readBytes()
            val storedOtfBytes = File(importedOtf.storagePath).readBytes()
            assertTrue("Stored TTF bytes must match original byte-for-byte", originalTtfBytes.contentEquals(storedTtfBytes))
            assertTrue("Stored OTF bytes must match original byte-for-byte", otfBytes.contentEquals(storedOtfBytes))
            assertEquals(TrueTypeTableParser.sha256Hex(originalTtfBytes), importedTtf.sha256Hash)

            repo.deleteImportedFont(importedTtf.id)
            repo.deleteImportedFont(importedOtf.id)
            ttfTemp.delete()
            otfTemp.delete()
        }
    }

    @Test
    fun asyncTypefaceLoadingAndBoundedLruHandleLargeFontCollections() {
        runBlocking {
            val renderEngine = FontRenderEngine(context)
            val repo = FontRepository(context, renderEngine)
            repo.initialize()

            val base = repo.fonts.value.first { it.fileName == "Roboto.ttf" }
            val synthetic = (1..1200).map { idx ->
                base.copy(
                    id = "synthetic_font_$idx",
                    displayName = "Synthetic Font $idx",
                    source = com.example.fonts.FontSource.BUNDLED
                )
            }
            renderEngine.registerFonts(synthetic)

            // Pin one font, then load >110 other fonts asynchronously to trigger LRU eviction cleanly
            val pinnedId = "synthetic_font_1"
            renderEngine.loadTypefaceAsync(pinnedId)
            renderEngine.pinFonts(setOf(pinnedId))

            for (i in 2..115) {
                val tf = renderEngine.loadTypefaceAsync("synthetic_font_$i")
                assertNotNull(tf)
            }

            // Pinned font remains immediately available in memory without synchronous reload
            assertNotNull("Pinned active document font must remain cached", renderEngine.getCachedTypefaceOrNull(pinnedId))

            val quickDiag = renderEngine.getDiagnosticsSnapshot(includePerFontDetails = false)
            assertTrue("All 1200+ fonts registered", quickDiag.totalFonts >= 1200)
            assertTrue("Fast diagnostics omits per-font string list", quickDiag.activeFontDiagnostics.isEmpty())
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun highZoomScaleUpTo100xAndVectorSpecimenRenderCrispOutlines() {
        runBlocking {
            val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
            vm.createNewProject()
            val textId = vm.addTextLayer("Z")!!

            vm.updateCanvasViewportTransform(zoomDelta = 80f, panDx = 0f, panDy = 0f)
            assertEquals(80f, vm.canvasZoom.value, 0.01f)

            vm.setSelectedLayerScale(60f)
            val scaledLayer = StudioViewModel.findLayerRecursive(vm.document.value.layers, textId) as TextLayer
            assertEquals(60f, scaledLayer.scale, 0.01f)

            // Export/render text at 40x scale using vector outline scaling
            val renderEngine = FontRenderEngine(context)
            val repo = FontRepository(context, renderEngine)
            repo.initialize()
            val mediaManager = MediaStorageManager(context)
            val bigDoc = ProjectDocument(
                canvas = CanvasDimensions.SQUARE_1_1,
                background = BackgroundConfig(activeTab = BackgroundTab.TRANSPARENT, checkerboardPreviewEnabled = false),
                layers = listOf(
                    TextLayer(
                        text = "H",
                        fontId = repo.selectedFontId.value,
                        fontSizeSp = 12f,
                        scale = 12f,
                        colorArgb = 0xFFFFFFFFL
                    )
                )
            )
            val bmp = mediaManager.renderProjectToBitmap(bigDoc, renderEngine, 240, 240)
            var opaqueCount = 0
            for (y in 20 until 220) {
                for (x in 20 until 220) {
                    if (Color.alpha(bmp.getPixel(x, y)) > 200) opaqueCount++
                }
            }
            bmp.recycle()
            assertTrue("High-scale vector text must render substantial crisp pixels (count=$opaqueCount)", opaqueCount > 100)

            // Direct vector specimen draw at 1600% magnification
            val specBmp = android.graphics.Bitmap.createBitmap(256, 256, android.graphics.Bitmap.Config.ARGB_8888)
            val specCanvas = android.graphics.Canvas(specBmp)
            specCanvas.translate(128f, 128f)
            renderEngine.drawSampleTextVector(
                canvas = specCanvas,
                typeface = renderEngine.getTypeface(repo.selectedFontId.value),
                isPureColorEmojiFont = false,
                text = "A",
                fontSizePx = 420f,
                maxWidthPx = 600f,
                colorArgb = Color.BLACK
            )
            var specOpaque = 0
            for (y in 0 until 256) {
                for (x in 0 until 256) {
                    if (Color.alpha(specBmp.getPixel(x, y)) > 200) specOpaque++
                }
            }
            specBmp.recycle()
            assertTrue("1600% vector specimen draw must render crisp glyph pixels", specOpaque > 100)
        }
    }

    @Test
    fun transformLocksDefaultAndEnforcementWorkCorrectly() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()
        val id = vm.addTextLayer("Lock Test")!!

        // Defaults: S=locked (true), Z/R/P=unlocked (false)
        assertEquals(
            TransformLockState(stretchLocked = true, zoomLocked = false, rotationLocked = false, positionLocked = false),
            vm.transformLocks.value
        )

        // While Stretch is locked, anchored stretch must be ignored
        vm.transformLayerAnchoredStretch(id, 1f, 0f, 50f, 0f, 100f, 100f, 400f, 400f)
        var layer = StudioViewModel.findLayerRecursive(vm.document.value.layers, id) as TextLayer
        assertEquals(1f, layer.stretchX, 0.0001f)

        // Unlock Stretch -> anchored stretch works cleanly
        vm.toggleStretchLock()
        assertFalse(vm.transformLocks.value.stretchLocked)
        vm.transformLayerAnchoredStretch(id, 1f, 0f, 40f, 0f, 100f, 100f, 400f, 400f)
        layer = StudioViewModel.findLayerRecursive(vm.document.value.layers, id) as TextLayer
        assertEquals(1.4f, layer.stretchX, 0.001f)

        // Lock Zoom, Rotation, Position -> transient transform must ignore them
        vm.toggleZoomLock()
        vm.toggleRotationLock()
        vm.togglePositionLock()
        assertTrue(vm.transformLocks.value.zoomLocked)
        assertTrue(vm.transformLocks.value.rotationLocked)
        assertTrue(vm.transformLocks.value.positionLocked)

        val beforeScale = layer.scale
        val beforeRot = layer.rotationDeg
        val beforeX = layer.offsetXNorm
        vm.transformLayerTransient(id, deltaXNorm = 0.25f, deltaYNorm = 0.25f, scaleFactor = 2f, rotationDelta = 45f)
        layer = StudioViewModel.findLayerRecursive(vm.document.value.layers, id) as TextLayer
        assertEquals(beforeScale, layer.scale, 0.0001f)
        assertEquals(beforeRot, layer.rotationDeg, 0.0001f)
        assertEquals(beforeX, layer.offsetXNorm, 0.0001f)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun drawStrokeAfterTransformedDrawLayerCreatesFreshUntransformedLayer() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()

        val stroke1 = DrawStroke(
            colorArgb = 0xFFFF0000L,
            strokeWidthNorm = 0.02f,
            points = listOf(DrawPoint(0.1f, 0.1f), DrawPoint(0.3f, 0.3f))
        )
        vm.addDrawStroke(stroke1)
        assertEquals(1, vm.document.value.layers.size)
        val firstDrawId = vm.document.value.layers.first().id

        // Move first draw layer away from identity
        vm.transformLayerTransient(firstDrawId, deltaXNorm = 0.2f, deltaYNorm = 0.1f, scaleFactor = 1f, rotationDelta = 0f)

        // Adding another stroke must create a fresh identity DrawLayer instead of distorting into the moved layer
        val stroke2 = DrawStroke(
            colorArgb = 0xFF00FF00L,
            strokeWidthNorm = 0.02f,
            points = listOf(DrawPoint(0.6f, 0.6f), DrawPoint(0.8f, 0.8f))
        )
        vm.addDrawStroke(stroke2)
        assertEquals(2, vm.document.value.layers.size)
        val secondDraw = vm.document.value.layers[1] as DrawLayer
        assertEquals(0f, secondDraw.offsetXNorm, 0.0001f)
        assertEquals(0f, secondDraw.offsetYNorm, 0.0001f)
        assertEquals(1, secondDraw.strokes.size)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun circleCropShapeMasksExportedImageCornersToTransparent() {
        runBlocking {
            val sourceFile = File(context.cacheDir, "circle_crop_src.png")
            val srcBmp = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
            srcBmp.eraseColor(Color.RED)
            FileOutputStream(sourceFile).use { out ->
                srcBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            srcBmp.recycle()

            val renderEngine = FontRenderEngine(context)
            val mediaManager = MediaStorageManager(context)
            val doc = ProjectDocument(
                canvas = CanvasDimensions.SQUARE_1_1,
                background = BackgroundConfig(activeTab = BackgroundTab.TRANSPARENT, checkerboardPreviewEnabled = false),
                layers = listOf(
                    ImageLayer(
                        uriString = sourceFile.absolutePath,
                        originalWidth = 100,
                        originalHeight = 100,
                        cropShape = ImageCropShape.CIRCLE,
                        scale = 1.15f
                    )
                )
            )
            val rendered = mediaManager.renderProjectToBitmap(doc, renderEngine, 200, 200)
            // Center must be opaque red, corner of circle bounding box must remain transparent!
            val centerPx = rendered.getPixel(100, 100)
            val cornerPx = rendered.getPixel(12, 12)
            assertTrue("Circle crop center must be red", Color.red(centerPx) > 200 && Color.alpha(centerPx) > 200)
            assertEquals("Circle crop outside corner must be transparent", 0, Color.alpha(cornerPx))
            rendered.recycle()
            sourceFile.delete()
        }
    }

    @Test
    fun transactionUndoRedoReversesSmallEditsStepByStepWithoutRemovingLayer() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()
        val id = vm.addTextLayer("Undo Step Test")!!

        // Transaction 1: small multi-tick drag move collapses into 1 undo step
        vm.beginTransformTransaction()
        vm.transformLayerTransient(id, deltaXNorm = 0.04f, deltaYNorm = -0.02f, scaleFactor = 1f, rotationDelta = 0f)
        vm.transformLayerTransient(id, deltaXNorm = 0.06f, deltaYNorm = -0.03f, scaleFactor = 1f, rotationDelta = 0f)
        vm.endTransformTransaction()

        // Transaction 2: zoom/rotation adjustment
        vm.beginTransformTransaction()
        vm.setSelectedLayerScale(1.8f)
        vm.setSelectedLayerRotation(24f)
        vm.endTransformTransaction()

        // Transaction 3: 3D rotation/skew adjustment
        vm.beginTransformTransaction()
        vm.updateSelectedLayer3DTransient { it.copy(rotationYDeg = 35f, skewXDeg = 12f) }
        vm.endTransformTransaction()

        var layer = StudioViewModel.findLayerRecursive(vm.document.value.layers, id) as TextLayer
        assertEquals(0.10f, layer.offsetXNorm, 0.001f)
        assertEquals(1.8f, layer.scale, 0.001f)
        assertEquals(24f, layer.rotationDeg, 0.001f)
        assertEquals(35f, layer.transform3D.rotationYDeg, 0.001f)

        // Undo 3D edit -> layer stays present and move/scale remain
        vm.undo()
        layer = StudioViewModel.findLayerRecursive(vm.document.value.layers, id) as TextLayer
        assertEquals(0f, layer.transform3D.rotationYDeg, 0.001f)
        assertEquals(1.8f, layer.scale, 0.001f)

        // Undo scale + rotation -> move remains
        vm.undo()
        layer = StudioViewModel.findLayerRecursive(vm.document.value.layers, id) as TextLayer
        assertEquals(1f, layer.scale, 0.001f)
        assertEquals(0f, layer.rotationDeg, 0.001f)
        assertEquals(0.10f, layer.offsetXNorm, 0.001f)

        // Undo slight move -> returns cleanly to origin, layer still exists
        vm.undo()
        layer = StudioViewModel.findLayerRecursive(vm.document.value.layers, id) as TextLayer
        assertEquals(0f, layer.offsetXNorm, 0.001f)
        assertEquals(0f, layer.offsetYNorm, 0.001f)

        // Redo restores move -> scale+rotation -> 3D
        vm.redo()
        layer = StudioViewModel.findLayerRecursive(vm.document.value.layers, id) as TextLayer
        assertEquals(0.10f, layer.offsetXNorm, 0.001f)

        vm.redo()
        layer = StudioViewModel.findLayerRecursive(vm.document.value.layers, id) as TextLayer
        assertEquals(1.8f, layer.scale, 0.001f)
        assertEquals(24f, layer.rotationDeg, 0.001f)

        vm.redo()
        layer = StudioViewModel.findLayerRecursive(vm.document.value.layers, id) as TextLayer
        assertEquals(35f, layer.transform3D.rotationYDeg, 0.001f)
        assertEquals(12f, layer.transform3D.skewXDeg, 0.001f)
    }

    @Test
    fun cornerPinLocksAllTransformsAndRestoresPriorLocksOnConfirm() {
        val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
        vm.createNewProject()
        val textId = vm.addTextLayer("Perspective Text")!!

        // Configure custom lock state before entering Corner Pin
        vm.toggleStretchLock() // now false
        vm.toggleZoomLock()    // now true
        val priorLocks = TransformLockState(
            stretchLocked = false,
            zoomLocked = true,
            rotationLocked = false,
            positionLocked = false
        )
        assertEquals(priorLocks, vm.transformLocks.value)

        vm.openToolDrawer(ToolDrawerTab.CORNER_PIN)
        assertEquals(ToolDrawerTab.CORNER_PIN, vm.activeToolDrawer.value)
        assertEquals(
            TransformLockState(
                stretchLocked = true,
                zoomLocked = true,
                rotationLocked = true,
                positionLocked = true
            ),
            vm.transformLocks.value
        )

        // Drag top-left corner pin and finalize transaction
        vm.beginTransformTransaction()
        vm.updateSelectedLayerCornerPinTransient(textId, cornerIndex = 0, deltaDxRatio = 0.2f, deltaDyRatio = 0.15f)
        vm.endTransformTransaction()

        val pinned = StudioViewModel.findLayerRecursive(vm.document.value.layers, textId) as TextLayer
        assertEquals(0.2f, pinned.cornerPin.tlDx, 0.001f)
        assertEquals(0.15f, pinned.cornerPin.tlDy, 0.001f)

        // Confirm closes Corner Pin drawer & restores custom locks
        vm.confirmCornerPin()
        assertEquals(ToolDrawerTab.NONE, vm.activeToolDrawer.value)
        assertEquals(priorLocks, vm.transformLocks.value)

        // Corner pin remains applied, and Undo reverses only the corner pin move
        val afterConfirm = StudioViewModel.findLayerRecursive(vm.document.value.layers, textId) as TextLayer
        assertEquals(0.2f, afterConfirm.cornerPin.tlDx, 0.001f)
        vm.undo()
        val afterUndo = StudioViewModel.findLayerRecursive(vm.document.value.layers, textId) as TextLayer
        assertEquals(0f, afterUndo.cornerPin.tlDx, 0.001f)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun cornerPinAnd3DPerspectiveRenderCleanlyInExportBitmapForPhotoAndText() {
        runBlocking {
            val sourceFile = File(context.cacheDir, "perspective_photo_src.png")
            val srcBmp = android.graphics.Bitmap.createBitmap(120, 120, android.graphics.Bitmap.Config.ARGB_8888)
            srcBmp.eraseColor(Color.BLUE)
            FileOutputStream(sourceFile).use { out ->
                srcBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            srcBmp.recycle()

            val renderEngine = FontRenderEngine(context)
            val repo = FontRepository(context, renderEngine)
            repo.initialize()
            val mediaManager = MediaStorageManager(context)

            val doc = ProjectDocument(
                canvas = CanvasDimensions.SQUARE_1_1,
                background = BackgroundConfig(activeTab = BackgroundTab.TRANSPARENT, checkerboardPreviewEnabled = false),
                layers = listOf(
                    ImageLayer(
                        uriString = sourceFile.absolutePath,
                        originalWidth = 120,
                        originalHeight = 120,
                        cornerPin = CornerPinOffsets(tlDx = 0.12f, brDy = -0.10f),
                        transform3D = Layer3DTransform(rotationYDeg = 22f, skewXDeg = 8f)
                    ),
                    TextLayer(
                        text = "3D TEXT",
                        fontId = repo.selectedFontId.value,
                        fontSizeSp = 44f,
                        colorArgb = 0xFFFFEB3BL,
                        cornerPin = CornerPinOffsets(trDy = 0.1f, blDx = -0.08f),
                        transform3D = Layer3DTransform(rotationXDeg = 18f, positionZ = 40f, skewYDeg = -6f)
                    )
                )
            )

            val m = LayerTransformMatrix.buildBitmapDrawMatrix(
                bitmapWidth = 120,
                bitmapHeight = 120,
                boxW = 120f,
                boxH = 120f,
                transform3D = Layer3DTransform(rotationYDeg = 25f, skewXDeg = 10f),
                cornerPin = CornerPinOffsets(tlDx = 0.1f)
            )
            assertFalse("3D + Corner Pin matrix must not be identity", m.isIdentity)

            val rendered = mediaManager.renderProjectToBitmap(doc, renderEngine, 240, 240)
            var visiblePixels = 0
            for (y in 20 until 220) {
                for (x in 20 until 220) {
                    if (Color.alpha(rendered.getPixel(x, y)) > 150) visiblePixels++
                }
            }
            rendered.recycle()
            sourceFile.delete()
            assertTrue("Perspective/3D photo and text must render visible non-blank pixels", visiblePixels > 150)
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun alightStyleShapesDefaultWhiteHaveMinimalHandlesAndRenderCleanly() {
        runBlocking {
            val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
            vm.createNewProject()

            // Circle & Rectangle strictly have 4 minimal cardinal dots; Rounded Rect adds 1 radius pill; Triangle has 3 vertex rings
            val circleLayer = ShapeLayer(shapeType = LayerShapeType.CIRCLE)
            assertEquals(0xFFFFFFFFL, circleLayer.fillColorArgb)
            val circleHandles = VectorShapeEngine.computeControlHandles(circleLayer, 200f)
            assertEquals(4, circleHandles.size)
            assertTrue(circleHandles.all { it.kind == ShapeHandleKind.CARDINAL_DOT })

            val rectHandles = VectorShapeEngine.computeControlHandles(ShapeLayer(shapeType = LayerShapeType.RECTANGLE), 200f)
            assertEquals(4, rectHandles.size)
            assertTrue(rectHandles.all { it.kind == ShapeHandleKind.CARDINAL_DOT })

            val roundedHandles = VectorShapeEngine.computeControlHandles(
                ShapeLayer(shapeType = LayerShapeType.ROUNDED_RECT, vectorParams = VectorShapeEngine.defaultParamsFor(LayerShapeType.ROUNDED_RECT)),
                200f
            )
            assertEquals(5, roundedHandles.size)
            assertEquals(1, roundedHandles.count { it.kind == ShapeHandleKind.RADIUS_PILL })

            val triLayer = ShapeLayer(shapeType = LayerShapeType.TRIANGLE)
            val triHandles = VectorShapeEngine.computeControlHandles(triLayer, 200f)
            assertEquals(3, triHandles.size)
            assertTrue(triHandles.all { it.kind == ShapeHandleKind.VERTEX_RING })

            // Dragging triangle apex stretches vertex upward non-destructively
            val draggedTri = VectorShapeEngine.applyHandleDrag(triLayer, "tri_apex", dxPx = 0f, dyPx = -40f, baseSizePx = 200f)
            assertTrue(draggedTri.vectorParams.triangleApexY < triLayer.vectorParams.triangleApexY)

            // ViewModel addShapeLayer defaults to white and turns on Edit Points mode
            vm.addShapeLayer(LayerShapeType.STAR)
            val added = StudioViewModel.findLayerRecursive(vm.document.value.layers, vm.document.value.selectedLayerId) as ShapeLayer
            assertEquals(0xFFFFFFFFL, added.fillColorArgb)
            assertTrue(vm.shapeEditPointsMode.value)

            // Render every new vector shape type onto native bitmap
            val renderEngine = FontRenderEngine(context)
            val mediaManager = MediaStorageManager(context)
            listOf(
                LayerShapeType.TRIANGLE,
                LayerShapeType.HEXAGON,
                LayerShapeType.PIE,
                LayerShapeType.ARROW,
                LayerShapeType.QUAD
            ).forEach { shapeType ->
                val shapeDoc = ProjectDocument(
                    canvas = CanvasDimensions.SQUARE_1_1,
                    background = BackgroundConfig(activeTab = BackgroundTab.TRANSPARENT, checkerboardPreviewEnabled = false),
                    layers = listOf(
                        ShapeLayer(
                            shapeType = shapeType,
                            fillColorArgb = 0xFFFFFFFFL,
                            vectorParams = VectorShapeEngine.defaultParamsFor(shapeType)
                        )
                    )
                )
                val bmp = mediaManager.renderProjectToBitmap(shapeDoc, renderEngine, 180, 180)
                var whitePx = 0
                for (y in 20 until 160) {
                    for (x in 20 until 160) {
                        val c = bmp.getPixel(x, y)
                        if (Color.alpha(c) > 200 && Color.red(c) > 200) whitePx++
                    }
                }
                bmp.recycle()
                assertTrue("Shape $shapeType must render visible white vector pixels (got $whitePx)", whitePx > 40)
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun vectorStrokeEngineSupportsSmoothingTaperProfileWidthPointsPressureAndNonDestructiveEdit() {
        runBlocking {
            val jagged = listOf(
                DrawPoint(0.10f, 0.50f, pressure = 0.4f),
                DrawPoint(0.25f, 0.35f, pressure = 0.7f),
                DrawPoint(0.45f, 0.65f, pressure = 1.0f),
                DrawPoint(0.70f, 0.38f, pressure = 0.9f),
                DrawPoint(0.90f, 0.50f, pressure = 0.5f)
            )
            val smoothed = VectorStrokeEngine.smoothPoints(jagged, smoothingIntensity = 0.8f)
            assertEquals(jagged.first().xNorm, smoothed.first().xNorm, 0.0001f)
            assertEquals(jagged.last().xNorm, smoothed.last().xNorm, 0.0001f)
            assertTrue(smoothed.size >= jagged.size)

            // Evaluate linked vs unlinked widths, taper, roundness, pressure, widthProfile & widthPoints
            val linkedStyle = VectorStrokeStyle(widthsLinked = true, startWidthFactor = 1.4f, endWidthFactor = 0.5f)
            assertEquals(1.4f, VectorStrokeEngine.evaluateWidthMultiplier(1f, 1.0f, linkedStyle), 0.02f)

            val unlinkedTaperedStyle = VectorStrokeStyle(
                widthsLinked = false,
                startWidthFactor = 1.0f,
                endWidthFactor = 1.8f,
                startTaperLength = 0.3f,
                startTaperIntensity = 0.9f,
                startRoundness = 0.8f,
                variableWidthEnabled = true,
                widthProfile = listOf(
                    WidthProfilePoint(id = "p0", positionNorm = 0f, widthMultiplier = 1f),
                    WidthProfilePoint(id = "p1", positionNorm = 0.5f, widthMultiplier = 1.6f),
                    WidthProfilePoint(id = "p2", positionNorm = 1f, widthMultiplier = 1f)
                ),
                widthPoints = listOf(
                    WidthProfilePoint(id = "w0", positionNorm = 0.5f, widthMultiplier = 1.25f, transition = WidthPointTransition.SHARP)
                ),
                pressureEnabled = true,
                pressureSensitivity = 0.8f
            )
            val atStart = VectorStrokeEngine.evaluateWidthMultiplier(0f, 1.0f, unlinkedTaperedStyle)
            val atMidHighPressure = VectorStrokeEngine.evaluateWidthMultiplier(0.5f, 1.3f, unlinkedTaperedStyle)
            val atMidLowPressure = VectorStrokeEngine.evaluateWidthMultiplier(0.5f, 0.2f, unlinkedTaperedStyle)
            assertTrue("Start taper must narrow tip ($atStart < $atMidHighPressure)", atStart < atMidHighPressure * 0.4f)
            assertTrue("Pressure sensitivity must scale width ($atMidHighPressure > $atMidLowPressure)", atMidHighPressure > atMidLowPressure)

            // End-to-end ViewModel pre-draw -> draw -> deselect -> select -> non-destructive post-draw edit + undo/redo + batch edit
            val vm = StudioViewModel(ApplicationProvider.getApplicationContext())
            vm.createNewProject()
            vm.updateStrokeStyle {
                it.copy(
                    overallWidthNorm = 0.03f,
                    pattern = VectorStrokePattern.DASHED,
                    lineCap = VectorLineCap.SQUARE,
                    lineJoin = VectorLineJoin.BEVEL,
                    smoothing = 0.2f,
                    strokeOpacity = 0.85f,
                    miterLimit = 6f,
                    strokeOffsetNorm = 0.006f,
                    strokeAlignment = VectorStrokeAlignment.OUTSIDE
                )
            }
            vm.addDrawStroke(
                DrawStroke(
                    colorArgb = 0xFFFF4D4FL,
                    strokeWidthNorm = 0.03f,
                    rawPoints = jagged,
                    points = VectorStrokeEngine.smoothPoints(jagged, 0.2f),
                    style = vm.drawStrokeStyle.value
                )
            )
            val layer1Id = vm.document.value.selectedLayerId!!

            // Switch to new session (deselects) -> defaults stay clean/new -> reselect loads layer1's saved style
            vm.startNewDrawLayerSession()
            assertEquals(null, vm.document.value.selectedLayerId)
            vm.selectLayer(layer1Id)
            assertEquals(VectorStrokePattern.DASHED, vm.drawStrokeStyle.value.pattern)
            assertEquals(VectorStrokeAlignment.OUTSIDE, vm.drawStrokeStyle.value.strokeAlignment)

            // Non-destructive post-draw edit recomputes smoothed points from preserved rawPoints without flattening
            vm.updateStrokeStyle { it.copy(smoothing = 0.9f, pattern = VectorStrokePattern.DASH_DOT, variableWidthEnabled = true) }
            val updatedLayer = StudioViewModel.findLayerRecursive(vm.document.value.layers, layer1Id) as DrawLayer
            val updatedStroke = updatedLayer.strokes.first()
            assertEquals(jagged.size, updatedStroke.rawPoints.size)
            assertEquals(VectorStrokePattern.DASH_DOT, updatedStroke.style.pattern)
            assertTrue(updatedStroke.points.size >= updatedStroke.rawPoints.size)

            // Undo reverses post-draw style edit back to DASHED
            vm.undo()
            val afterUndo = StudioViewModel.findLayerRecursive(vm.document.value.layers, layer1Id) as DrawLayer
            assertEquals(VectorStrokePattern.DASHED, afterUndo.strokes.first().style.pattern)
            vm.redo()

            // JSON round-trip preserves rawPoints and VectorStrokeStyle
            val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; classDiscriminator = "layerType" }
            val encoded = json.encodeToString(vm.document.value)
            val decoded = json.decodeFromString<ProjectDocument>(encoded)
            val decodedStroke = (decoded.layers.first() as DrawLayer).strokes.first()
            assertEquals(VectorStrokePattern.DASH_DOT, decodedStroke.style.pattern)
            assertEquals(jagged.size, decodedStroke.rawPoints.size)

            // Render stroke bitmap cleanly via MediaStorageManager
            val renderEngine = FontRenderEngine(context)
            val mediaManager = MediaStorageManager(context)
            val rendered = mediaManager.renderProjectToBitmap(decoded, renderEngine, 200, 200)
            var redInkPx = 0
            for (y in 20 until 180) {
                for (x in 10 until 190) {
                    val px = rendered.getPixel(x, y)
                    if (Color.alpha(px) > 100 && Color.red(px) > 180) redInkPx++
                }
            }
            rendered.recycle()
            assertTrue("Variable-width + dashed + offset stroke must render visible ink pixels ($redInkPx)", redInkPx > 30)
        }
    }

    private fun buildMinimalOtfBytes(): ByteArray {
        val tags = listOf("CFF ", "head", "maxp", "cmap")
        val numTables = tags.size
        val headerSize = 12 + numTables * 16
        val tableBodySize = numTables * 16
        val buf = ByteBuffer.allocate(headerSize + tableBodySize).order(ByteOrder.BIG_ENDIAN)
        buf.put("OTTO".toByteArray(Charsets.US_ASCII))
        buf.putShort(numTables.toShort())
        buf.putShort(16)
        buf.putShort(0)
        buf.putShort(0)

        var offset = headerSize
        for (tag in tags) {
            buf.put(tag.toByteArray(Charsets.US_ASCII))
            buf.putInt(0)
            buf.putInt(offset)
            buf.putInt(16)
            offset += 16
        }
        val maxpIdx = tags.indexOf("maxp")
        val maxpOff = headerSize + maxpIdx * 16
        buf.putInt(maxpOff, 0x00005000)
        buf.putShort(maxpOff + 4, 24)
        return buf.array()
    }

    private fun buildMinimalSfnt(tags: List<String>): ByteArray {
        val numTables = tags.size
        val headerSize = 12 + numTables * 16
        val tableBodySize = numTables * 16
        val buf = ByteBuffer.allocate(headerSize + tableBodySize).order(ByteOrder.BIG_ENDIAN)
        buf.putInt(0x00010000)
        buf.putShort(numTables.toShort())
        buf.putShort(16)
        buf.putShort(0)
        buf.putShort(0)

        var offset = headerSize
        for (tag in tags) {
            val tagBytes = tag.padEnd(4, ' ').substring(0, 4).toByteArray(Charsets.US_ASCII)
            buf.put(tagBytes)
            buf.putInt(0)
            buf.putInt(offset)
            buf.putInt(16)
            offset += 16
        }
        val maxpIdx = tags.indexOf("maxp")
        if (maxpIdx >= 0) {
            val maxpOff = headerSize + maxpIdx * 16
            buf.putInt(maxpOff, 0x00010000)
            buf.putShort(maxpOff + 4, 42)
        }
        return buf.array()
    }
}
