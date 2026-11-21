package com.example.data

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.model.LayerShapeType
import com.example.model.NormalizedPoint
import com.example.model.ShapeLayer
import com.example.model.ShapeVectorParams
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ShapeHandleKind {
    VERTEX_RING,
    CARDINAL_DOT,
    RADIUS_PILL
}

data class ShapeControlHandle(
    val id: String,
    val kind: ShapeHandleKind,
    val xPx: Float,
    val yPx: Float,
    val isHorizontalPill: Boolean = true,
    val contentDescription: String
)

object VectorShapeEngine {

    fun defaultParamsFor(shapeType: LayerShapeType): ShapeVectorParams {
        return when (shapeType) {
            LayerShapeType.RECTANGLE -> ShapeVectorParams(cornerRadiusFrac = 0f)
            LayerShapeType.ROUNDED_RECT -> ShapeVectorParams(cornerRadiusFrac = 0.22f)
            LayerShapeType.STAR -> ShapeVectorParams(starPoints = 5, starInnerRatio = 0.45f)
            LayerShapeType.BADGE -> ShapeVectorParams(starPoints = 8, starInnerRatio = 0.78f)
            LayerShapeType.HEXAGON -> ShapeVectorParams(polygonSides = 6)
            LayerShapeType.PIE -> ShapeVectorParams(pieStartDeg = 45f, pieSweepDeg = 270f)
            else -> ShapeVectorParams()
        }
    }

    /**
     * Builds the Android vector Path for [shapeType] centered around ([cx], [cy])
     * with nominal square unit [baseSizePx].
     */
    fun buildShapePath(
        shapeType: LayerShapeType,
        params: ShapeVectorParams,
        baseSizePx: Float,
        cx: Float = 0f,
        cy: Float = 0f
    ): Path {
        val safeBase = baseSizePx.coerceAtLeast(4f)
        val halfW = safeBase * 0.5f * params.boxWidthScale.coerceIn(0.2f, 3.5f)
        val halfH = safeBase * 0.5f * params.boxHeightScale.coerceIn(0.2f, 3.5f)
        val path = Path()

        when (shapeType) {
            LayerShapeType.CIRCLE -> {
                path.addOval(
                    RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH),
                    Path.Direction.CW
                )
            }
            LayerShapeType.RECTANGLE -> {
                val r = min(halfW, halfH) * 2f * params.cornerRadiusFrac.coerceIn(0f, 0.5f)
                if (r > 0.5f) {
                    path.addRoundRect(
                        RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH),
                        r,
                        r,
                        Path.Direction.CW
                    )
                } else {
                    path.addRect(
                        RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH),
                        Path.Direction.CW
                    )
                }
            }
            LayerShapeType.ROUNDED_RECT -> {
                val r = min(halfW, halfH) * 2f * params.cornerRadiusFrac.coerceIn(0f, 0.5f)
                path.addRoundRect(
                    RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH),
                    r,
                    r,
                    Path.Direction.CW
                )
            }
            LayerShapeType.TRIANGLE -> {
                val ax = cx + params.triangleApexX * safeBase
                val ay = cy + params.triangleApexY * safeBase
                val lx = cx + params.triangleLeftX * safeBase
                val ly = cy + params.triangleLeftY * safeBase
                val rx = cx + params.triangleRightX * safeBase
                val ry = cy + params.triangleRightY * safeBase
                path.moveTo(ax, ay)
                path.lineTo(rx, ry)
                path.lineTo(lx, ly)
                path.close()
            }
            LayerShapeType.STAR, LayerShapeType.BADGE -> {
                val defaultPts = if (shapeType == LayerShapeType.STAR) 5 else 8
                val pts = params.starPoints.coerceIn(3, 16).let { if (it == 5 && shapeType == LayerShapeType.BADGE) defaultPts else it }
                val innerRatio = params.starInnerRatio.coerceIn(0.12f, 0.92f)
                val total = pts * 2
                for (i in 0 until total) {
                    val mult = if (i % 2 == 0) 1f else innerRatio
                    val ang = Math.toRadians((i * 360.0 / total) - 90.0)
                    val px = cx + (halfW * mult * cos(ang)).toFloat()
                    val py = cy + (halfH * mult * sin(ang)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            LayerShapeType.HEXAGON -> {
                val sides = params.polygonSides.coerceIn(3, 12)
                for (i in 0 until sides) {
                    val ang = Math.toRadians((i * 360.0 / sides) - 90.0)
                    val px = cx + (halfW * cos(ang)).toFloat()
                    val py = cy + (halfH * sin(ang)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            LayerShapeType.PIE -> {
                val oval = RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
                val sweep = params.pieSweepDeg.coerceIn(10f, 355f)
                path.moveTo(cx, cy)
                path.arcTo(oval, params.pieStartDeg, sweep, false)
                path.close()
            }
            LayerShapeType.ARROW -> {
                val tx = cx + params.arrowTailX * safeBase
                val ty = cy + params.arrowTailY * safeBase
                val hx = cx + params.arrowTipX * safeBase
                val hy = cy + params.arrowTipY * safeBase
                val dx = hx - tx
                val dy = hy - ty
                val len = hypot(dx, dy).coerceAtLeast(4f)
                val ux = dx / len
                val uy = dy / len
                val nx = -uy
                val ny = ux

                val headLen = (len * params.arrowHeadLengthFrac.coerceIn(0.18f, 0.65f))
                val shaftHalf = (safeBase * params.arrowShaftWidthFrac.coerceIn(0.06f, 0.42f)) * 0.5f
                val headHalf = (shaftHalf * 2.15f).coerceAtLeast(shaftHalf + safeBase * 0.08f)
                val neckX = hx - ux * headLen
                val neckY = hy - uy * headLen

                path.moveTo(tx + nx * shaftHalf, ty + ny * shaftHalf)
                path.lineTo(neckX + nx * shaftHalf, neckY + ny * shaftHalf)
                path.lineTo(neckX + nx * headHalf, neckY + ny * headHalf)
                path.lineTo(hx, hy)
                path.lineTo(neckX - nx * headHalf, neckY - ny * headHalf)
                path.lineTo(neckX - nx * shaftHalf, neckY - ny * shaftHalf)
                path.lineTo(tx - nx * shaftHalf, ty - ny * shaftHalf)
                path.close()
            }
            LayerShapeType.QUAD -> {
                val verts = if (params.quadVertices.size >= 4) {
                    params.quadVertices.take(4)
                } else {
                    ShapeVectorParams().quadVertices
                }
                verts.forEachIndexed { idx, v ->
                    val px = cx + v.x * safeBase
                    val py = cy + v.y * safeBase
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
        }
        return path
    }

    fun drawShapeOnCanvas(
        canvas: Canvas,
        layer: ShapeLayer,
        baseSizePx: Float,
        cx: Float = 0f,
        cy: Float = 0f,
        strokeScaleFactor: Float = 1f
    ) {
        val path = buildShapePath(layer.shapeType, layer.vectorParams, baseSizePx, cx, cy)
        val fillColorInt = layer.fillColorArgb.toInt()
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = fillColorInt
            alpha = ((android.graphics.Color.alpha(fillColorInt) / 255f) * layer.opacity * 255f)
                .roundToInt().coerceIn(0, 255)
        }
        canvas.drawPath(path, fillPaint)

        if (layer.strokeWidth > 0f) {
            val strokeColorInt = layer.strokeColorArgb.toInt()
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = strokeColorInt
                strokeWidth = layer.strokeWidth * strokeScaleFactor
                strokeJoin = Paint.Join.ROUND
                alpha = ((android.graphics.Color.alpha(strokeColorInt) / 255f) * layer.opacity * 255f)
                    .roundToInt().coerceIn(0, 255)
            }
            canvas.drawPath(path, strokePaint)
        }
    }

    /**
     * Computes Alight Motion-inspired minimal control handles in coordinates relative to box top-left (0..baseSizePx).
     * Strictly keeps Circle & Square to 4 cardinal dots (1..4 dots) and exposes per-shape vector handles cleanly.
     */
    fun computeControlHandles(
        layer: ShapeLayer,
        baseSizePx: Float
    ): List<ShapeControlHandle> {
        val safeBase = baseSizePx.coerceAtLeast(4f)
        val cx = safeBase * 0.5f
        val cy = safeBase * 0.5f
        val params = layer.vectorParams
        val halfW = safeBase * 0.5f * params.boxWidthScale.coerceIn(0.2f, 3.5f)
        val halfH = safeBase * 0.5f * params.boxHeightScale.coerceIn(0.2f, 3.5f)

        return when (layer.shapeType) {
            LayerShapeType.CIRCLE, LayerShapeType.RECTANGLE -> listOf(
                ShapeControlHandle("edge_top", ShapeHandleKind.CARDINAL_DOT, cx, cy - halfH, contentDescription = "Top Height Handle"),
                ShapeControlHandle("edge_right", ShapeHandleKind.CARDINAL_DOT, cx + halfW, cy, contentDescription = "Right Width Handle"),
                ShapeControlHandle("edge_bottom", ShapeHandleKind.CARDINAL_DOT, cx, cy + halfH, contentDescription = "Bottom Height Handle"),
                ShapeControlHandle("edge_left", ShapeHandleKind.CARDINAL_DOT, cx - halfW, cy, contentDescription = "Left Width Handle")
            )
            LayerShapeType.ROUNDED_RECT -> {
                val radiusT = (params.cornerRadiusFrac / 0.5f).coerceIn(0f, 1f)
                val pillX = (cx - halfW) + (radiusT * halfW).coerceIn(10f, halfW.coerceAtLeast(10f))
                listOf(
                    ShapeControlHandle("edge_top", ShapeHandleKind.CARDINAL_DOT, cx, cy - halfH, contentDescription = "Top Height Handle"),
                    ShapeControlHandle("edge_right", ShapeHandleKind.CARDINAL_DOT, cx + halfW, cy, contentDescription = "Right Width Handle"),
                    ShapeControlHandle("edge_bottom", ShapeHandleKind.CARDINAL_DOT, cx, cy + halfH, contentDescription = "Bottom Height Handle"),
                    ShapeControlHandle("edge_left", ShapeHandleKind.CARDINAL_DOT, cx - halfW, cy, contentDescription = "Left Width Handle"),
                    ShapeControlHandle("corner_radius", ShapeHandleKind.RADIUS_PILL, pillX, cy - halfH, isHorizontalPill = true, contentDescription = "Corner Radius Handle")
                )
            }
            LayerShapeType.TRIANGLE -> listOf(
                ShapeControlHandle("tri_apex", ShapeHandleKind.VERTEX_RING, cx + params.triangleApexX * safeBase, cy + params.triangleApexY * safeBase, contentDescription = "Triangle Apex Handle"),
                ShapeControlHandle("tri_right", ShapeHandleKind.VERTEX_RING, cx + params.triangleRightX * safeBase, cy + params.triangleRightY * safeBase, contentDescription = "Triangle Right Corner"),
                ShapeControlHandle("tri_left", ShapeHandleKind.VERTEX_RING, cx + params.triangleLeftX * safeBase, cy + params.triangleLeftY * safeBase, contentDescription = "Triangle Left Corner")
            )
            LayerShapeType.STAR, LayerShapeType.BADGE -> {
                val pts = if (layer.shapeType == LayerShapeType.BADGE) 8 else params.starPoints.coerceIn(3, 16)
                val innerAng = Math.toRadians((360.0 / (pts * 2)) - 90.0)
                val innerDistX = halfW * params.starInnerRatio.coerceIn(0.12f, 0.92f)
                val innerDistY = halfH * params.starInnerRatio.coerceIn(0.12f, 0.92f)
                listOf(
                    ShapeControlHandle("star_outer", ShapeHandleKind.VERTEX_RING, cx, cy - halfH, contentDescription = "Star Outer Tip Handle"),
                    ShapeControlHandle("star_right", ShapeHandleKind.CARDINAL_DOT, cx + halfW, cy, contentDescription = "Star Width Handle"),
                    ShapeControlHandle(
                        "star_inner",
                        ShapeHandleKind.RADIUS_PILL,
                        cx + (innerDistX * cos(innerAng)).toFloat(),
                        cy + (innerDistY * sin(innerAng)).toFloat(),
                        isHorizontalPill = true,
                        contentDescription = "Star Inner Radius Handle"
                    )
                )
            }
            LayerShapeType.HEXAGON -> listOf(
                ShapeControlHandle("edge_top", ShapeHandleKind.CARDINAL_DOT, cx, cy - halfH, contentDescription = "Polygon Height Handle"),
                ShapeControlHandle("edge_right", ShapeHandleKind.CARDINAL_DOT, cx + halfW, cy, contentDescription = "Polygon Width Handle"),
                ShapeControlHandle("edge_bottom", ShapeHandleKind.CARDINAL_DOT, cx, cy + halfH, contentDescription = "Polygon Bottom Handle")
            )
            LayerShapeType.PIE -> {
                val startRad = Math.toRadians(params.pieStartDeg.toDouble())
                val endRad = Math.toRadians((params.pieStartDeg + params.pieSweepDeg).toDouble())
                listOf(
                    ShapeControlHandle("pie_start", ShapeHandleKind.VERTEX_RING, cx + (halfW * cos(startRad)).toFloat(), cy + (halfH * sin(startRad)).toFloat(), contentDescription = "Pie Start Angle Handle"),
                    ShapeControlHandle("pie_end", ShapeHandleKind.VERTEX_RING, cx + (halfW * cos(endRad)).toFloat(), cy + (halfH * sin(endRad)).toFloat(), contentDescription = "Pie Sweep Angle Handle")
                )
            }
            LayerShapeType.ARROW -> {
                val tx = cx + params.arrowTailX * safeBase
                val ty = cy + params.arrowTailY * safeBase
                val hx = cx + params.arrowTipX * safeBase
                val hy = cy + params.arrowTipY * safeBase
                val mx = (tx + hx) * 0.5f
                val my = (ty + hy) * 0.5f
                val dx = hx - tx
                val dy = hy - ty
                val len = hypot(dx, dy).coerceAtLeast(4f)
                val nx = -dy / len
                val ny = dx / len
                val shaftOffset = safeBase * params.arrowShaftWidthFrac * 0.5f
                listOf(
                    ShapeControlHandle("arrow_tip", ShapeHandleKind.VERTEX_RING, hx, hy, contentDescription = "Arrow Tip Handle"),
                    ShapeControlHandle("arrow_tail", ShapeHandleKind.VERTEX_RING, tx, ty, contentDescription = "Arrow Tail Handle"),
                    ShapeControlHandle("arrow_width", ShapeHandleKind.RADIUS_PILL, mx + nx * shaftOffset, my + ny * shaftOffset, isHorizontalPill = false, contentDescription = "Arrow Shaft Width Handle")
                )
            }
            LayerShapeType.QUAD -> {
                val verts = if (params.quadVertices.size >= 4) params.quadVertices.take(4) else ShapeVectorParams().quadVertices
                verts.mapIndexed { idx, v ->
                    ShapeControlHandle(
                        id = "quad_$idx",
                        kind = ShapeHandleKind.VERTEX_RING,
                        xPx = cx + v.x * safeBase,
                        yPx = cy + v.y * safeBase,
                        contentDescription = "Quad Corner ${idx + 1}"
                    )
                }
            }
        }
    }

    fun applyHandleDrag(
        layer: ShapeLayer,
        handleId: String,
        dxPx: Float,
        dyPx: Float,
        baseSizePx: Float
    ): ShapeLayer {
        val safeBase = baseSizePx.coerceAtLeast(4f)
        val dxNorm = dxPx / safeBase
        val dyNorm = dyPx / safeBase
        val p = layer.vectorParams

        val nextParams = when (handleId) {
            "edge_left" -> p.copy(boxWidthScale = (p.boxWidthScale - dxNorm * 2f).coerceIn(0.25f, 3.0f))
            "edge_right", "star_right" -> p.copy(boxWidthScale = (p.boxWidthScale + dxNorm * 2f).coerceIn(0.25f, 3.0f))
            "edge_top", "star_outer" -> p.copy(boxHeightScale = (p.boxHeightScale - dyNorm * 2f).coerceIn(0.25f, 3.0f))
            "edge_bottom" -> p.copy(boxHeightScale = (p.boxHeightScale + dyNorm * 2f).coerceIn(0.25f, 3.0f))
            "corner_radius" -> p.copy(cornerRadiusFrac = (p.cornerRadiusFrac + dxNorm).coerceIn(0f, 0.5f))
            "star_inner" -> p.copy(starInnerRatio = (p.starInnerRatio + (dxNorm + dyNorm) * 1.1f).coerceIn(0.15f, 0.88f))
            "tri_apex" -> p.copy(
                triangleApexX = (p.triangleApexX + dxNorm).coerceIn(-1.2f, 1.2f),
                triangleApexY = (p.triangleApexY + dyNorm).coerceIn(-1.2f, 1.2f)
            )
            "tri_left" -> p.copy(
                triangleLeftX = (p.triangleLeftX + dxNorm).coerceIn(-1.2f, 1.2f),
                triangleLeftY = (p.triangleLeftY + dyNorm).coerceIn(-1.2f, 1.2f)
            )
            "tri_right" -> p.copy(
                triangleRightX = (p.triangleRightX + dxNorm).coerceIn(-1.2f, 1.2f),
                triangleRightY = (p.triangleRightY + dyNorm).coerceIn(-1.2f, 1.2f)
            )
            "pie_start" -> {
                val curRad = Math.toRadians(p.pieStartDeg.toDouble())
                val curX = 0.5f * p.boxWidthScale * cos(curRad).toFloat() + dxNorm
                val curY = 0.5f * p.boxHeightScale * sin(curRad).toFloat() + dyNorm
                val newDeg = Math.toDegrees(atan2(curY.toDouble(), curX.toDouble())).toFloat()
                p.copy(pieStartDeg = newDeg)
            }
            "pie_end" -> {
                val curRad = Math.toRadians((p.pieStartDeg + p.pieSweepDeg).toDouble())
                val curX = 0.5f * p.boxWidthScale * cos(curRad).toFloat() + dxNorm
                val curY = 0.5f * p.boxHeightScale * sin(curRad).toFloat() + dyNorm
                val endDeg = Math.toDegrees(atan2(curY.toDouble(), curX.toDouble())).toFloat()
                var sweep = (endDeg - p.pieStartDeg) % 360f
                if (sweep < 0f) sweep += 360f
                p.copy(pieSweepDeg = sweep.coerceIn(10f, 355f))
            }
            "arrow_tip" -> p.copy(
                arrowTipX = (p.arrowTipX + dxNorm).coerceIn(-1.2f, 1.2f),
                arrowTipY = (p.arrowTipY + dyNorm).coerceIn(-1.2f, 1.2f)
            )
            "arrow_tail" -> p.copy(
                arrowTailX = (p.arrowTailX + dxNorm).coerceIn(-1.2f, 1.2f),
                arrowTailY = (p.arrowTailY + dyNorm).coerceIn(-1.2f, 1.2f)
            )
            "arrow_width" -> p.copy(
                arrowShaftWidthFrac = (p.arrowShaftWidthFrac + (absDeltaSigned(dxNorm, dyNorm))).coerceIn(0.06f, 0.42f)
            )
            else -> {
                if (handleId.startsWith("quad_")) {
                    val idx = handleId.removePrefix("quad_").toIntOrNull()
                    val verts = (if (p.quadVertices.size >= 4) p.quadVertices else ShapeVectorParams().quadVertices).toMutableList()
                    if (idx != null && idx in 0 until 4) {
                        val old = verts[idx]
                        verts[idx] = NormalizedPoint(
                            x = (old.x + dxNorm).coerceIn(-1.3f, 1.3f),
                            y = (old.y + dyNorm).coerceIn(-1.3f, 1.3f)
                        )
                    }
                    p.copy(quadVertices = verts)
                } else p
            }
        }
        return layer.copy(vectorParams = nextParams)
    }

    private fun absDeltaSigned(dx: Float, dy: Float): Float {
        return (-dx + dy) * 0.7f
    }
}
