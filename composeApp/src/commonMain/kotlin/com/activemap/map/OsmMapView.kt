package com.activemap.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.activemap.model.Place
import com.activemap.model.PlaceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.*

data class OsmState(
    val centerLat: Double = 55.7558,
    val centerLon: Double = 37.6173,
    val zoom: Int = 13
)

fun lonToTileX(lon: Double, zoom: Int): Double = (lon + 180.0) / 360.0 * 2.0.pow(zoom.toDouble())

fun latToTileY(lat: Double, zoom: Int): Double {
    val latRad = Math.toRadians(lat)
    return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * 2.0.pow(zoom.toDouble())
}

fun tileXToLon(tileX: Double, zoom: Int): Double = tileX / 2.0.pow(zoom.toDouble()) * 360.0 - 180.0

fun tileYToLat(tileY: Double, zoom: Int): Double {
    val n = PI - 2.0 * PI * tileY / 2.0.pow(zoom.toDouble())
    return Math.toDegrees(atan(sinh(n)))
}

private fun placeIcon(type: PlaceType): String = when (type) {
    PlaceType.PARK -> "🌳"
    PlaceType.STADIUM -> "🏟"
    PlaceType.GYM -> "💪"
    PlaceType.STREET -> "🏃"
}

@Composable
fun OsmMap(
    state: OsmState,
    onStateChanged: (OsmState) -> Unit,
    places: List<Place>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var tileBitmaps by remember { mutableStateOf(mapOf<String, ImageBitmap>()) }
    val tileSize = 256f
    var boxSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(state.centerLat, state.centerLon, state.zoom) {
        scope.launch(Dispatchers.IO) {
            val centerTX = lonToTileX(state.centerLon, state.zoom).toInt()
            val centerTY = latToTileY(state.centerLat, state.zoom).toInt()
            val maxTile = 2.0.pow(state.zoom.toDouble()).toInt()
            val toLoad = mutableListOf<Triple<Int, Int, String>>()
            for (dx in -3..3) for (dy in -3..3) {
                val tx = centerTX + dx; val ty = centerTY + dy
                if (tx < 0 || ty < 0 || tx >= maxTile || ty >= maxTile) continue
                val key = "${state.zoom}/$tx/$ty"
                if (key !in tileBitmaps) toLoad.add(Triple(tx, ty, key))
            }
            val newTiles = mutableMapOf<String, ImageBitmap>()
            for ((tx, ty, key) in toLoad) {
                val tile = OsmTileLoader.loadTile(state.zoom, tx, ty)
                if (tile != null) newTiles[key] = tile
            }
            if (newTiles.isNotEmpty()) tileBitmaps = tileBitmaps + newTiles
        }
    }

    val centerTX = remember(state.centerLon, state.zoom) { lonToTileX(state.centerLon, state.zoom) }
    val centerTY = remember(state.centerLat, state.zoom) { latToTileY(state.centerLat, state.zoom) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .onGloballyPositioned { boxSize = it.size }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F0EB))) {
            val cw = size.width; val ch = size.height
            val cpx = centerTX * tileSize
            val cpy = centerTY * tileSize
            val cy = ch * 0.35f
            val halfX = (cw / tileSize).toInt() / 2 + 2
            val halfY = (ch / tileSize).toInt() / 2 + 2
            val maxTile = 2.0.pow(state.zoom.toDouble()).toInt()

            for (tx in floor(centerTX).toInt() - halfX..floor(centerTX).toInt() + halfX) {
                for (ty in floor(centerTY).toInt() - halfY..floor(centerTY).toInt() + halfY) {
                    if (tx < 0 || ty < 0 || tx >= maxTile || ty >= maxTile) continue
                    val sx = (cw / 2f + (tx * tileSize - cpx)).toFloat()
                    val sy = (cy + (ty * tileSize - cpy)).toFloat()
                    val bitmap = tileBitmaps["${state.zoom}/$tx/$ty"]
                    if (bitmap != null) {
                        drawImage(bitmap, dstOffset = IntOffset(sx.toInt(), sy.toInt()), dstSize = IntSize(tileSize.toInt(), tileSize.toInt()))
                    } else {
                        drawRect(Color(0xFFF2F0EB), topLeft = Offset(sx, sy), size = Size(tileSize, tileSize))
                    }
                }
            }

            val cx = cw / 2f
            drawCircle(Color(0xFFD32F2F).copy(alpha = 0.15f), radius = 26f, center = Offset(cx, cy))
            drawCircle(Color(0xFFD32F2F), radius = 8f, center = Offset(cx, cy))
            drawCircle(Color.White, radius = 3f, center = Offset(cx, cy))

            // Place markers with icons
            places.forEach { place ->
                val px = lonToTileX(place.longitude, state.zoom)
                val py = latToTileY(place.latitude, state.zoom)
                val mx = cx + ((px - centerTX) * tileSize).toFloat()
                val my = cy + ((py - centerTY) * tileSize).toFloat()
                val color = when (place.type) {
                    PlaceType.PARK -> Color(0xFF4CAF50)
                    PlaceType.STADIUM -> Color(0xFF2196F3)
                    PlaceType.GYM -> Color(0xFFFF5722)
                    PlaceType.STREET -> Color(0xFF9C27B0)
                }
                if (mx in -60f..cw + 60f && my in -60f..ch + 60f) {
                    drawPlaceMarker(mx, my, color, placeIcon(place.type), textMeasurer)
                }
            }
        }
    }
}

private fun DrawScope.drawPlaceMarker(mx: Float, my: Float, color: Color, icon: String, textMeasurer: androidx.compose.ui.text.TextMeasurer) {
    // Outer glow
    drawCircle(color.copy(alpha = 0.25f), radius = 40f, center = Offset(mx, my))
    // White border ring
    drawCircle(Color.White, radius = 24f, center = Offset(mx, my - 6f))
    drawCircle(Color.White, radius = 24f, center = Offset(mx, my - 6f))
    // Pin body
    drawCircle(color, radius = 20f, center = Offset(mx, my - 6f))
    // Pin tip (triangle)
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(mx - 10f, my + 10f)
            lineTo(mx + 10f, my + 10f)
            lineTo(mx, my + 28f)
            close()
        },
        color = color
    )
    // Inner white circle
    drawCircle(Color.White, radius = 13f, center = Offset(mx, my - 6f))
    // Icon text
    val style = TextStyle(fontSize = 16.sp)
    val measured = textMeasurer.measure(icon, style)
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(mx - measured.size.width / 2f, my - 6f - measured.size.height / 2f)
    )
}
