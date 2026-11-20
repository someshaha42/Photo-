package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioEmerald
import kotlin.math.roundToInt

/**
 * Full-screen floating Pencil Eyedropper overlay across the entire UI (canvas, photos, objects, color palette).
 * - Quick tap anywhere on screen immediately samples that pixel and applies it.
 * - Dragging moves the magnified pencil crosshair live over the UI so you can inspect any pixel, then tap ✓ to apply.
 */
@Composable
fun FloatingEyedropperOverlay(
    onPickColor: (argbInt: Int) -> Unit,
    onCancel: () -> Unit
) {
    val view = LocalView.current
    var snapshot by remember { mutableStateOf<Bitmap?>(null) }
    var overlayOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    var overlayWidth by remember { mutableIntStateOf(1) }
    var overlayHeight by remember { mutableIntStateOf(1) }
    var cursorLocal by remember { mutableStateOf(Offset(260f, 420f)) }
    var sampledColor by remember { mutableIntStateOf(android.graphics.Color.WHITE) }

    fun sampleAtLocal(local: Offset, bmp: Bitmap?): Int {
        if (bmp == null || bmp.width <= 0 || bmp.height <= 0) return sampledColor
        val winX = (overlayOriginInWindow.x + local.x).roundToInt().coerceIn(0, bmp.width - 1)
        val winY = (overlayOriginInWindow.y + local.y).roundToInt().coerceIn(0, bmp.height - 1)
        return runCatching { bmp.getPixel(winX, winY) }.getOrDefault(sampledColor)
    }

    LaunchedEffect(view) {
        captureRootWindowBitmap(view) { captured ->
            snapshot = captured
            if (captured != null) {
                val initPos = Offset(overlayWidth * 0.5f, overlayHeight * 0.45f)
                cursorLocal = initPos
                sampledColor = sampleAtLocal(initPos, captured)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            snapshot?.recycle()
            snapshot = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                overlayOriginInWindow = coords.positionInWindow()
                overlayWidth = coords.size.width.coerceAtLeast(1)
                overlayHeight = coords.size.height.coerceAtLeast(1)
                if (cursorLocal == Offset(260f, 420f) && coords.size.width > 0) {
                    val center = Offset(coords.size.width * 0.5f, coords.size.height * 0.45f)
                    cursorLocal = center
                    sampledColor = sampleAtLocal(center, snapshot)
                }
            }
            .pointerInput(snapshot) {
                detectTapGestures { tapOffset ->
                    val clamped = Offset(
                        tapOffset.x.coerceIn(0f, overlayWidth.toFloat()),
                        tapOffset.y.coerceIn(0f, overlayHeight.toFloat())
                    )
                    cursorLocal = clamped
                    val color = sampleAtLocal(clamped, snapshot)
                    sampledColor = color
                    onPickColor(color)
                }
            }
            .pointerInput(snapshot) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val next = Offset(
                        (cursorLocal.x + dragAmount.x).coerceIn(0f, overlayWidth.toFloat()),
                        (cursorLocal.y + dragAmount.y).coerceIn(0f, overlayHeight.toFloat())
                    )
                    cursorLocal = next
                    sampledColor = sampleAtLocal(next, snapshot)
                }
            }
            .testTag("floating_eyedropper_overlay")
    ) {
        // Floating Crosshair + Color Loupe Ring
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (cursorLocal.x - 42f).roundToInt(),
                        (cursorLocal.y - 42f).roundToInt()
                    )
                }
                .size(84.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerR = size.minDimension / 2f - 4.dp.toPx()
                drawCircle(
                    color = Color(sampledColor),
                    radius = outerR,
                    center = center,
                    style = Stroke(width = 7.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = outerR + 3.5.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = Color.Black,
                    radius = 5.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            Icon(
                imageVector = Icons.Rounded.Colorize,
                contentDescription = "Eyedropper Crosshair",
                tint = StudioAzure,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(3.dp)
            )
        }

        // Compact Floating Control Bar at Top
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 10.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
                .border(1.5.dp, StudioAzure, RoundedCornerShape(999.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(sampledColor))
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Text(
                    text = String.format("#%06X", sampledColor and 0xFFFFFF),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Glide like mouse • ✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { onPickColor(sampledColor) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = StudioEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("eyedropper_confirm_button")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Apply Color", modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("eyedropper_cancel_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancel Eyedropper", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun captureRootWindowBitmap(view: View, onCaptured: (Bitmap?) -> Unit) {
    val root = view.rootView
    val w = root.width
    val h = root.height
    if (w <= 0 || h <= 0) {
        onCaptured(null)
        return
    }
    val window = view.context.findActivityWindow()
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && window != null) {
        val location = IntArray(2)
        root.getLocationInWindow(location)
        val rect = Rect(location[0], location[1], location[0] + w, location[1] + h)
        runCatching {
            PixelCopy.request(
                window,
                rect,
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        onCaptured(bitmap)
                    } else {
                        softwareDrawFallback(root, bitmap)
                        onCaptured(bitmap)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }.onFailure {
            softwareDrawFallback(root, bitmap)
            onCaptured(bitmap)
        }
    } else {
        softwareDrawFallback(root, bitmap)
        onCaptured(bitmap)
    }
}

private fun softwareDrawFallback(view: View, bitmap: Bitmap) {
    runCatching {
        val canvas = AndroidCanvas(bitmap)
        view.draw(canvas)
    }
}

private tailrec fun Context.findActivityWindow(): Window? = when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.findActivityWindow()
    else -> null
}
