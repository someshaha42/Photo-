package com.example.data

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.model.DrawPoint
import com.example.model.DrawStroke
import com.example.model.VectorLineCap
import com.example.model.VectorLineJoin
import com.example.model.VectorStrokeAlignment
import com.example.model.VectorStrokePattern
import com.example.model.VectorStrokeStyle
import com.example.model.WidthPointTransition
import com.example.model.WidthProfilePoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object VectorStrokeEngine {

    /**
     * Applies non-destructive, low-latency smoothing to raw captured points while preserving
     * endpoints and interpolating pointer/stylus pressure.
     */
    fun smoothPoints(
        rawPoints: List<DrawPoint>,
        smoothingIntensity: Float
    ): List<DrawPoint> {
        val intensity = smoothingIntensity.coerceIn(0f, 1f)
        if (rawPoints.size < 3 || intensity <= 0.02f) return rawPoints

        val passes = when {
            intensity < 0.35f -> 1
            intensity < 0.72f -> 2
            else -> 3
        }
        val blendWeight = (0.15f + intensity * 0.35f).coerceIn(0.15f, 0.48f)

        var current = rawPoints
        repeat(passes) {
            if (current.size < 3) return@repeat
            val next = ArrayList<DrawPoint>(current.size * 2)
            next.add(current.first())
            if (current.size <= 160 && passes <= 2) {
                // Chaikin-inspired smoothing weighted by intensity
                val qWeight = 1f - blendWeight * 0.55f
                for (i in 0 until current.lastIndex) {
                    val p0 = current[i]
                    val p1 = current[i + 1]
                    if (i > 0) {
                        next.add(
                            DrawPoint(
                                xNorm = p0.xNorm * qWeight + p1.xNorm * (1f - qWeight),
                                yNorm = p0.yNorm * qWeight + p1.yNorm * (1f - qWeight),
                                pressure = p0.pressure * qWeight + p1.pressure * (1f - qWeight)
                            )
                        )
                    }
                    if (i < current.lastIndex - 1) {
                        next.add(
                            DrawPoint(
                                xNorm = p0.xNorm * (1f - qWeight) + p1.xNorm * qWeight,
                                yNorm = p0.yNorm * (1f - qWeight) + p1.yNorm * qWeight,
                                pressure = p0.pressure * (1f - qWeight) + p1.pressure * qWeight
                            )
                        )
                    }
                }
            } else {
                // Fast Laplacian pass for very dense freehand paths
                for (i in 1 until current.lastIndex) {
                    val prev = current[i - 1]
                    val cur = current[i]
                    val nxt = current[i + 1]
                    val avgX = (prev.xNorm + nxt.xNorm) * 0.5f
                    val avgY = (prev.yNorm + nxt.yNorm) * 0.5f
                    val avgP = (prev.pressure + nxt.pressure) * 0.5f
                    next.add(
                        DrawPoint(
                            xNorm = cur.xNorm * (1f - blendWeight) + avgX * blendWeight,
                            yNorm = cur.yNorm * (1f - blendWeight) + avgY * blendWeight,
                            pressure = cur.pressure * (1f - blendWeight) + avgP * blendWeight
                        )
                    )
                }
            }
            next.add(current.last())
            current = next
        }
        return current
    }

    /**
     * Evaluates effective width multiplier at normalized arc position [tNorm] (0..1) combining:
     * - Start/End width factors (linked or unlinked)
     * - Start/End taper length, intensity, and roundness
     * - Variable width profile curve
     * - Individual path width points
     * - Stylus/touch pressure sensitivity
     */
    fun evaluateWidthMultiplier(
        tNorm: Float,
        rawPressure: Float,
        style: VectorStrokeStyle
    ): Float {
        val t = tNorm.coerceIn(0f, 1f)

        // 1. Start/End width blend
        val effectiveEndFactor = if (style.widthsLinked) style.startWidthFactor else style.endWidthFactor
        val baseEndLerp = style.startWidthFactor + (effectiveEndFactor - style.startWidthFactor) * t

        // 2. Start & End Taper
        var taperMult = 1f
        val startLen = style.startTaperLength.coerceIn(0f, 0.5f)
        if (startLen > 0.001f && t < startLen) {
            val u = (t / startLen).coerceIn(0f, 1f)
            val shaped = shapeTaperProgress(u, style.startRoundness)
            val minFrac = (1f - style.startTaperIntensity.coerceIn(0f, 1f)).coerceAtLeast(0.03f)
            taperMult *= minFrac + (1f - minFrac) * shaped
        }
        val endLen = style.endTaperLength.coerceIn(0f, 0.5f)
        if (endLen > 0.001f && t > 1f - endLen) {
            val u = ((1f - t) / endLen).coerceIn(0f, 1f)
            val shaped = shapeTaperProgress(u, style.endRoundness)
            val minFrac = (1f - style.endTaperIntensity.coerceIn(0f, 1f)).coerceAtLeast(0.03f)
            taperMult *= minFrac + (1f - minFrac) * shaped
        }

        // 3. Width Profile curve
        val profileMult = if (style.variableWidthEnabled && style.widthProfile.isNotEmpty()) {
            evaluateWidthCurve(t, style.widthProfile)
        } else {
            1f
        }

        // 4. Individual Width Points along path
        val widthPointsMult = if (style.widthPoints.isNotEmpty()) {
            val anchored = buildAnchoredNodes(style.widthPoints)
            evaluateWidthCurve(t, anchored)
        } else {
            1f
        }

        // 5. Pressure Sensitivity (standard touch devices fallback gracefully to pressure = 1.0f -> 1.0x multiplier)
        val pressureMult = if (style.pressureEnabled && style.pressureSensitivity > 0.01f) {
            val clampedP = rawPressure.coerceIn(0.05f, 1.5f)
            val targetP = (0.25f + clampedP * 0.75f).coerceIn(0.15f, 1.45f)
            1f + (targetP - 1f) * style.pressureSensitivity.coerceIn(0f, 1f)
        } else {
            1f
        }

        return (baseEndLerp * taperMult * profileMult * widthPointsMult * pressureMult).coerceIn(0.03f, 4.0f)
    }

    private fun shapeTaperProgress(u: Float, roundness: Float): Float {
        val r = roundness.coerceIn(0f, 1f)
        val linear = u
        val smooth = u * u * (3f - 2f * u)
        val rounded = sqrt(u)
        return if (r < 0.5f) {
            val k = r * 2f
            linear * (1f - k) + smooth * k
        } else {
            val k = (r - 0.5f) * 2f
            smooth * (1f - k) + rounded * k
        }
    }

    private fun buildAnchoredNodes(points: List<WidthProfilePoint>): List<WidthProfilePoint> {
        val sorted = points.sortedBy { it.positionNorm }
        val result = mutableListOf<WidthProfilePoint>()
        if (sorted.first().positionNorm > 0.01f) {
            result.add(WidthProfilePoint(id = "anchor_start", positionNorm = 0f, widthMultiplier = 1f, transition = sorted.first().transition))
        }
        result.addAll(sorted)
        if (sorted.last().positionNorm < 0.99f) {
            result.add(WidthProfilePoint(id = "anchor_end", positionNorm = 1f, widthMultiplier = 1f, transition = sorted.last().transition))
        }
        return result
    }

    fun evaluateWidthCurve(tNorm: Float, nodes: List<WidthProfilePoint>): Float {
        if (nodes.isEmpty()) return 1f
        val sorted = nodes.sortedBy { it.positionNorm }
        if (tNorm <= sorted.first().positionNorm) return sorted.first().widthMultiplier.coerceIn(0.05f, 3.0f)
        if (tNorm >= sorted.last().positionNorm) return sorted.last().widthMultiplier.coerceIn(0.05f, 3.0f)

        for (i in 0 until sorted.lastIndex) {
            val a = sorted[i]
            val b = sorted[i + 1]
            if (tNorm in a.positionNorm..b.positionNorm) {
                val span = (b.positionNorm - a.positionNorm).coerceAtLeast(0.0001f)
                val localT = ((tNorm - a.positionNorm) / span).coerceIn(0f, 1f)
                val eased = if (a.transition == WidthPointTransition.SHARP && b.transition == WidthPointTransition.SHARP) {
                    localT
                } else {
                    (1f - cos(localT * PI.toFloat())) * 0.5f
                }
                return (a.widthMultiplier + (b.widthMultiplier - a.widthMultiplier) * eased).coerceIn(0.05f, 3.0f)
            }
        }
        return 1f
    }

    /**
     * Draws a single [DrawStroke] identically across live EditorCanvas preview and high-res PNG/JPG export.
     * [originOffsetX] / [originOffsetY] let preview use (0, 0) while centered canvas export uses (-w/2, -h/2).
     */
    fun drawVectorStroke(
        canvas: Canvas,
        stroke: DrawStroke,
        areaWidthPx: Float,
        areaHeightPx: Float,
        layerOpacity: Float = 1f,
        originOffsetX: Float = 0f,
        originOffsetY: Float = 0f
    ) {
        val rawPts = if (stroke.points.isNotEmpty()) stroke.points else stroke.rawPoints
        if (rawPts.isEmpty()) return

        val minDim = min(areaWidthPx, areaHeightPx).coerceAtLeast(1f)
        val style = stroke.style
        val baseStrokePx = (stroke.effectiveOverallWidthNorm * minDim).coerceAtLeast(1.5f)
        val colorInt = stroke.colorArgb.toInt()
        val finalAlpha = ((android.graphics.Color.alpha(colorInt) / 255f) *
            style.strokeOpacity.coerceIn(0f, 1f) *
            layerOpacity.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        if (finalAlpha == 0) return

        val screenPts = rawPts.map { pt ->
            ScreenSample(
                x = originOffsetX + pt.xNorm * areaWidthPx,
                y = originOffsetY + pt.yNorm * areaHeightPx,
                pressure = pt.pressure
            )
        }

        if (screenPts.size == 1) {
            val pt = screenPts.first()
            val radius = (baseStrokePx * evaluateWidthMultiplier(0.5f, pt.pressure, style) * 0.5f).coerceAtLeast(1f)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.FILL
                color = colorInt
                alpha = finalAlpha
            }
            if (style.lineCap == VectorLineCap.SQUARE) {
                canvas.drawRect(pt.x - radius, pt.y - radius, pt.x + radius, pt.y + radius, fillPaint)
            } else {
                canvas.drawCircle(pt.x, pt.y, radius, fillPaint)
            }
            return
        }

        val hasDistinctPressure = style.pressureEnabled &&
            style.pressureSensitivity > 0.01f &&
            screenPts.any { kotlin.math.abs(it.pressure - 1f) > 0.03f }

        val needsRibbon = style.variableWidthEnabled ||
            style.startWidthFactor != 1f ||
            style.endWidthFactor != 1f ||
            style.startTaperLength > 0.001f ||
            style.endTaperLength > 0.001f ||
            style.widthPoints.isNotEmpty() ||
            hasDistinctPressure ||
            style.strokeOffsetNorm != 0f ||
            style.strokeAlignment != VectorStrokeAlignment.CENTER

        if (!needsRibbon) {
            drawUniformNativeStroke(canvas, screenPts, style, baseStrokePx, minDim, colorInt, finalAlpha)
        } else {
            drawVariableRibbonStroke(canvas, screenPts, style, baseStrokePx, minDim, colorInt, finalAlpha)
        }
    }

    private data class ScreenSample(
        val x: Float,
        val y: Float,
        val pressure: Float,
        val tNorm: Float = 0f
    )

    private fun drawUniformNativeStroke(
        canvas: Canvas,
        pts: List<ScreenSample>,
        style: VectorStrokeStyle,
        strokeWidthPx: Float,
        minDim: Float,
        colorInt: Int,
        alphaInt: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            color = colorInt
            alpha = alphaInt
            strokeWidth = strokeWidthPx
            strokeCap = when (style.lineCap) {
                VectorLineCap.BUTT -> Paint.Cap.BUTT
                VectorLineCap.ROUND -> Paint.Cap.ROUND
                VectorLineCap.SQUARE -> Paint.Cap.SQUARE
            }
            strokeJoin = when (style.lineJoin) {
                VectorLineJoin.MITER -> Paint.Join.MITER
                VectorLineJoin.ROUND -> Paint.Join.ROUND
                VectorLineJoin.BEVEL -> Paint.Join.BEVEL
            }
            strokeMiter = style.miterLimit.coerceIn(1f, 20f)
            pathEffect = buildPatternPathEffect(style, strokeWidthPx, minDim)
        }
        val path = Path()
        pts.forEachIndexed { idx, p ->
            if (idx == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        canvas.drawPath(path, paint)
    }

    private fun buildPatternPathEffect(
        style: VectorStrokeStyle,
        strokeWidthPx: Float,
        minDim: Float
    ): DashPathEffect? {
        val extraGap = (style.strokeSpacingNorm * minDim).coerceAtLeast(0f)
        val dashLen = (style.dashLengthNorm * minDim).coerceAtLeast(strokeWidthPx * 0.4f)
        val dashGap = (style.dashSpacingNorm * minDim + extraGap).coerceAtLeast(strokeWidthPx * 0.35f)
        val dotGap = (style.dotSpacingNorm * minDim + extraGap).coerceAtLeast(strokeWidthPx * 0.4f)
        val dotLen = if (style.lineCap == VectorLineCap.ROUND) 1f else max(2f, strokeWidthPx * 0.7f)

        return when (style.pattern) {
            VectorStrokePattern.SOLID -> if (extraGap > 1f) {
                DashPathEffect(floatArrayOf(max(dashLen, strokeWidthPx), extraGap), 0f)
            } else null
            VectorStrokePattern.DASHED -> DashPathEffect(floatArrayOf(dashLen, dashGap), 0f)
            VectorStrokePattern.DOTTED -> DashPathEffect(floatArrayOf(dotLen, dotGap + strokeWidthPx * 0.5f), 0f)
            VectorStrokePattern.DASH_DOT -> DashPathEffect(floatArrayOf(dashLen, dashGap, dotLen, dotGap + strokeWidthPx * 0.5f), 0f)
        }
    }

    private fun drawVariableRibbonStroke(
        canvas: Canvas,
        pts: List<ScreenSample>,
        style: VectorStrokeStyle,
        baseStrokePx: Float,
        minDim: Float,
        colorInt: Int,
        alphaInt: Int
    ) {
        val (arcPts, totalLen) = annotateArcProgress(pts)
        if (totalLen <= 0.5f) {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.FILL
                color = colorInt
                alpha = alphaInt
            }
            val r = (baseStrokePx * evaluateWidthMultiplier(0.5f, arcPts.first().pressure, style) * 0.5f).coerceAtLeast(1f)
            canvas.drawCircle(arcPts.first().x, arcPts.first().y, r, fillPaint)
            return
        }

        // Apply normal offset & Stroke Alignment (Center / Inside / Outside)
        val alignShift = when (style.strokeAlignment) {
            VectorStrokeAlignment.CENTER -> 0f
            VectorStrokeAlignment.INSIDE -> -0.5f
            VectorStrokeAlignment.OUTSIDE -> 0.5f
        }
        val offsetPx = style.strokeOffsetNorm * minDim + alignShift * baseStrokePx
        val shiftedPts = if (kotlin.math.abs(offsetPx) > 0.25f) {
            offsetPolyline(arcPts, offsetPx)
        } else {
            arcPts
        }

        val segments = splitByPattern(shiftedPts, totalLen, style, baseStrokePx, minDim)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            color = colorInt
            alpha = alphaInt
        }

        for (seg in segments) {
            if (seg.size == 1) {
                val p = seg.first()
                val r = (baseStrokePx * evaluateWidthMultiplier(p.tNorm, p.pressure, style) * 0.5f).coerceAtLeast(0.8f)
                if (style.lineCap == VectorLineCap.SQUARE) {
                    canvas.drawRect(p.x - r, p.y - r, p.x + r, p.y + r, fillPaint)
                } else {
                    canvas.drawCircle(p.x, p.y, r, fillPaint)
                }
                continue
            }
            val ribbonPath = buildSegmentRibbonPath(seg, style, baseStrokePx)
            canvas.drawPath(ribbonPath, fillPaint)
        }
    }

    private fun annotateArcProgress(pts: List<ScreenSample>): Pair<List<ScreenSample>, Float> {
        val cum = FloatArray(pts.size)
        var acc = 0f
        for (i in 1 until pts.size) {
            acc += hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
            cum[i] = acc
        }
        val safeTotal = acc.coerceAtLeast(0.001f)
        val out = pts.mapIndexed { i, p ->
            p.copy(tNorm = (cum[i] / safeTotal).coerceIn(0f, 1f))
        }
        return out to acc
    }

    private fun offsetPolyline(pts: List<ScreenSample>, offsetPx: Float): List<ScreenSample> {
        val n = pts.size
        if (n < 2) return pts
        val out = ArrayList<ScreenSample>(n)
        for (i in 0 until n) {
            val (tx, ty) = tangentAt(pts, i)
            val nx = -ty
            val ny = tx
            out.add(pts[i].copy(x = pts[i].x + nx * offsetPx, y = pts[i].y + ny * offsetPx))
        }
        return out
    }

    private fun splitByPattern(
        pts: List<ScreenSample>,
        totalLen: Float,
        style: VectorStrokeStyle,
        baseStrokePx: Float,
        minDim: Float
    ): List<List<ScreenSample>> {
        val extraGap = (style.strokeSpacingNorm * minDim).coerceAtLeast(0f)
        if (style.pattern == VectorStrokePattern.SOLID && extraGap <= 1f) {
            return listOf(pts)
        }
        val dashLen = (style.dashLengthNorm * minDim).coerceAtLeast(baseStrokePx * 0.45f)
        val dashGap = (style.dashSpacingNorm * minDim + extraGap).coerceAtLeast(baseStrokePx * 0.35f)
        val dotGap = (style.dotSpacingNorm * minDim + extraGap).coerceAtLeast(baseStrokePx * 0.4f)
        val dotLen = max(2f, baseStrokePx * 0.35f)

        val patternArray: FloatArray = when (style.pattern) {
            VectorStrokePattern.SOLID -> floatArrayOf(max(dashLen, baseStrokePx), extraGap)
            VectorStrokePattern.DASHED -> floatArrayOf(dashLen, dashGap)
            VectorStrokePattern.DOTTED -> floatArrayOf(dotLen, dotGap + baseStrokePx * 0.45f)
            VectorStrokePattern.DASH_DOT -> floatArrayOf(dashLen, dashGap, dotLen, dotGap + baseStrokePx * 0.45f)
        }

        val result = mutableListOf<List<ScreenSample>>()
        var distCursor = 0f
        var pIdx = 0
        var isOn = true
        while (distCursor < totalLen - 0.5f && pIdx < 800) {
            val segLen = patternArray[pIdx % patternArray.size].coerceAtLeast(1f)
            val segEnd = min(totalLen, distCursor + segLen)
            if (isOn) {
                val startT = (distCursor / totalLen).coerceIn(0f, 1f)
                val endT = (segEnd / totalLen).coerceIn(0f, 1f)
                result.add(extractSubPolyline(pts, startT, endT))
            }
            distCursor = segEnd
            pIdx++
            isOn = !isOn
        }
        return result.ifEmpty { listOf(pts) }
    }

    private fun extractSubPolyline(pts: List<ScreenSample>, startT: Float, endT: Float): List<ScreenSample> {
        val out = mutableListOf<ScreenSample>()
        out.add(sampleAtT(pts, startT))
        for (p in pts) {
            if (p.tNorm > startT && p.tNorm < endT) {
                out.add(p)
            }
        }
        out.add(sampleAtT(pts, endT))
        return out
    }

    private fun sampleAtT(pts: List<ScreenSample>, t: Float): ScreenSample {
        if (t <= pts.first().tNorm) return pts.first()
        if (t >= pts.last().tNorm) return pts.last()
        for (i in 0 until pts.lastIndex) {
            val a = pts[i]
            val b = pts[i + 1]
            if (t in a.tNorm..b.tNorm) {
                val span = (b.tNorm - a.tNorm).coerceAtLeast(0.0001f)
                val k = ((t - a.tNorm) / span).coerceIn(0f, 1f)
                return ScreenSample(
                    x = a.x + (b.x - a.x) * k,
                    y = a.y + (b.y - a.y) * k,
                    pressure = a.pressure + (b.pressure - a.pressure) * k,
                    tNorm = t
                )
            }
        }
        return pts.last()
    }

    private fun buildSegmentRibbonPath(
        seg: List<ScreenSample>,
        style: VectorStrokeStyle,
        baseStrokePx: Float
    ): Path {
        val n = seg.size
        val leftX = FloatArray(n)
        val leftY = FloatArray(n)
        val rightX = FloatArray(n)
        val rightY = FloatArray(n)
        val radii = FloatArray(n)

        for (i in 0 until n) {
            val p = seg[i]
            val r = (baseStrokePx * evaluateWidthMultiplier(p.tNorm, p.pressure, style) * 0.5f).coerceAtLeast(0.6f)
            radii[i] = r
            val (tx, ty) = tangentAt(seg, i)
            val nx = -ty
            val ny = tx

            // Apply miter adjustment for sharp corners respecting miterLimit & LineJoin
            var scaleN = 1f
            if (i in 1 until (n - 1) && style.lineJoin == VectorLineJoin.MITER) {
                val d1x = seg[i].x - seg[i - 1].x
                val d1y = seg[i].y - seg[i - 1].y
                val d2x = seg[i + 1].x - seg[i].x
                val d2y = seg[i + 1].y - seg[i].y
                val l1 = hypot(d1x, d1y).coerceAtLeast(0.001f)
                val l2 = hypot(d2x, d2y).coerceAtLeast(0.001f)
                val n1x = -d1y / l1
                val n1y = d1x / l1
                val dot = (nx * n1x + ny * n1y).coerceIn(0.1f, 1f)
                scaleN = (1f / dot).coerceAtMost(style.miterLimit.coerceIn(1f, 12f))
            }

            leftX[i] = p.x + nx * r * scaleN
            leftY[i] = p.y + ny * r * scaleN
            rightX[i] = p.x - nx * r * scaleN
            rightY[i] = p.y - ny * r * scaleN
        }

        val path = Path()
        val endT0 = tangentAt(seg, 0)
        val endTN = tangentAt(seg, n - 1)

        // Start cap offset for SQUARE cap
        if (style.lineCap == VectorLineCap.SQUARE) {
            val sx = seg.first().x - endT0.first * radii.first()
            val sy = seg.first().y - endT0.second * radii.first()
            val nx = -endT0.second
            val ny = endT0.first
            path.moveTo(sx + nx * radii.first(), sy + ny * radii.first())
        } else {
            path.moveTo(leftX[0], leftY[0])
        }

        for (i in 1 until n) {
            if (style.lineJoin == VectorLineJoin.ROUND && i < n - 1) {
                val midX = (leftX[i - 1] + leftX[i]) * 0.5f
                val midY = (leftY[i - 1] + leftY[i]) * 0.5f
                path.quadTo(leftX[i - 1], leftY[i - 1], midX, midY)
            } else {
                path.lineTo(leftX[i], leftY[i])
            }
        }

        // End Cap (Butt, Square, or Round modulated by endRoundness)
        val endCenter = seg.last()
        val endR = radii.last()
        when (style.lineCap) {
            VectorLineCap.ROUND -> {
                val roundFactor = style.endRoundness.coerceIn(0f, 1f)
                if (roundFactor > 0.08f && endR > 0.8f) {
                    val tipX = endCenter.x + endTN.first * endR * roundFactor
                    val tipY = endCenter.y + endTN.second * endR * roundFactor
                    path.quadTo(
                        leftX.last() + endTN.first * endR * roundFactor,
                        leftY.last() + endTN.second * endR * roundFactor,
                        tipX,
                        tipY
                    )
                    path.quadTo(
                        rightX.last() + endTN.first * endR * roundFactor,
                        rightY.last() + endTN.second * endR * roundFactor,
                        rightX.last(),
                        rightY.last()
                    )
                } else {
                    path.lineTo(rightX.last(), rightY.last())
                }
            }
            VectorLineCap.SQUARE -> {
                val nx = -endTN.second
                val ny = endTN.first
                val ex = endCenter.x + endTN.first * endR
                val ey = endCenter.y + endTN.second * endR
                path.lineTo(ex + nx * endR, ey + ny * endR)
                path.lineTo(ex - nx * endR, ey - ny * endR)
            }
            VectorLineCap.BUTT -> {
                path.lineTo(rightX.last(), rightY.last())
            }
        }

        for (i in (n - 2) downTo 0) {
            if (style.lineJoin == VectorLineJoin.ROUND && i > 0) {
                val midX = (rightX[i + 1] + rightX[i]) * 0.5f
                val midY = (rightY[i + 1] + rightY[i]) * 0.5f
                path.quadTo(rightX[i + 1], rightY[i + 1], midX, midY)
            } else {
                path.lineTo(rightX[i], rightY[i])
            }
        }

        // Start Cap (Butt, Square, or Round modulated by startRoundness)
        val startCenter = seg.first()
        val startR = radii.first()
        when (style.lineCap) {
            VectorLineCap.ROUND -> {
                val roundFactor = style.startRoundness.coerceIn(0f, 1f)
                if (roundFactor > 0.08f && startR > 0.8f) {
                    val tipX = startCenter.x - endT0.first * startR * roundFactor
                    val tipY = startCenter.y - endT0.second * startR * roundFactor
                    path.quadTo(
                        rightX[0] - endT0.first * startR * roundFactor,
                        rightY[0] - endT0.second * startR * roundFactor,
                        tipX,
                        tipY
                    )
                    path.quadTo(
                        leftX[0] - endT0.first * startR * roundFactor,
                        leftY[0] - endT0.second * startR * roundFactor,
                        leftX[0],
                        leftY[0]
                    )
                }
            }
            VectorLineCap.SQUARE -> {
                val sx = startCenter.x - endT0.first * startR
                val sy = startCenter.y - endT0.second * startR
                val nx = -endT0.second
                val ny = endT0.first
                path.lineTo(sx - nx * startR, sy - ny * startR)
            }
            VectorLineCap.BUTT -> Unit
        }

        path.close()
        return path
    }

    private fun tangentAt(pts: List<ScreenSample>, idx: Int): Pair<Float, Float> {
        val prev = pts[(idx - 1).coerceAtLeast(0)]
        val next = pts[(idx + 1).coerceAtMost(pts.lastIndex)]
        val dx = next.x - prev.x
        val dy = next.y - prev.y
        val len = hypot(dx, dy)
        return if (len > 0.0001f) {
            (dx / len) to (dy / len)
        } else {
            1f to 0f
        }
    }
}
