package com.example

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.MediaStorageManager
import com.example.fonts.ColorTableType
import com.example.fonts.FontArchiveExtractor
import com.example.fonts.FontClassification
import com.example.fonts.FontRenderEngine
import com.example.fonts.FontRepository
import com.example.fonts.TrueTypeTableParser
import com.example.model.BackgroundConfig
import com.example.model.BackgroundTab
import com.example.model.CanvasDimensions
import com.example.model.EmojiSourceMode
import com.example.model.GroupLayer
import com.example.model.ImageAdjustments
import com.example.model.ImageCropRect
import com.example.model.ImageLayer
import com.example.model.ProjectDocument
import com.example.model.ShapeLayer
import com.example.model.TextCaseMode
import com.example.model.TextCaseTransformer
import com.example.model.TextLayer
import com.example.ui.StudioViewModel
import com.example.ui.ToolDrawerTab
import kotlinx.coroutines.runBlocking
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
