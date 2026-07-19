package com.activemap.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(state.centerLat, state.centerLon, state.zoom) {
        launch(Dispatchers.IO) {
            val centerTX = lonToTileX(state.centerLon, state.zoom).toInt()
            val centerTY = latToTileY(state.centerLat, state.zoom).toInt()
            val maxTile = 2.0.pow(state.zoom.toDouble()).toInt()
            val toLoad = mutableListOf<Triple<Int, Int, String>>()

            for (dx in -3..3) {
                for (dy in -3..3) {
                    val tx = centerTX + dx
                    val ty = centerTY + dy
                    if (tx < 0 || ty < 0 || tx >= maxTile || ty >= maxTile) continue
                    val key = "${state.zoom}/$tx/$ty"
                    if (key !in tileBitmaps) {
                        toLoad.add(Triple(tx, ty, key))
                    }
                }
            }

            val newTiles = mutableMapOf<String, ImageBitmap>()
            for ((tx, ty, key) in toLoad) {
                val tile = OsmTileLoader.loadTile(state.zoom, tx, ty)
                if (tile != null) {
                    newTiles[key] = tile
                }
            }
            if (newTiles.isNotEmpty()) {
                tileBitmaps = tileBitmaps + newTiles
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F0EB))
            .pointerInput(state.zoom) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val n = 2.0.pow(state.zoom.toDouble())
                    val pixelsPerLon = tileSize * n / 360.0
                    val lonDelta = -dragAmount.x / pixelsPerLon
                    val latRad = Math.toRadians(state.centerLat)
                    val pixelsPerLat = tileSize * n / (2.0 * PI)
                    val latDelta = dragAmount.y / pixelsPerLat * cos(latRad)
                    onStateChanged(
                        state.copy(
                            centerLon = (state.centerLon + lonDelta).coerceIn(-180.0, 180.0),
                            centerLat = (state.centerLat - latDelta).coerceIn(-85.0, 85.0)
                        )
                    )
                }
            }
    ) {
        val cw = size.width
        val ch = size.height

        val centerTX = lonToTileX(state.centerLon, state.zoom)
        val centerTY = latToTileY(state.centerLat, state.zoom)

        val centerPixelX = centerTX * tileSize
        val centerPixelY = centerTY * tileSize

        val tilesHalfX = (cw / tileSize).toInt() / 2 + 2
        val tilesHalfY = (ch / tileSize).toInt() / 2 + 2

        val startTX = floor(centerTX).toInt() - tilesHalfX
        val endTX = floor(centerTX).toInt() + tilesHalfX
        val startTY = floor(centerTY).toInt() - tilesHalfY
        val endTY = floor(centerTY).toInt() + tilesHalfY
        val maxTile = 2.0.pow(state.zoom.toDouble()).toInt()

        for (tx in startTX..endTX) {
            for (ty in startTY..endTY) {
                if (tx < 0 || ty < 0 || tx >= maxTile || ty >= maxTile) continue

                val screenX = (cw / 2f + (tx * tileSize - centerPixelX)).toFloat()
                val screenY = (ch / 2f + (ty * tileSize - centerPixelY)).toFloat()

                val key = "${state.zoom}/$tx/$ty"
                val tileBitmap = tileBitmaps[key]

                if (tileBitmap != null) {
                    drawImage(
                        image = tileBitmap,
                        dstOffset = IntOffset(screenX.toInt(), screenY.toInt()),
                        dstSize = IntSize(tileSize.toInt(), tileSize.toInt())
                    )
                } else {
                    drawRect(
                        color = Color(0xFFF2F0EB),
                        topLeft = Offset(screenX, screenY),
                        size = Size(tileSize, tileSize)
                    )
                }
            }
        }

        places.forEach { place ->
            val px = lonToTileX(place.longitude, state.zoom)
            val py = latToTileY(place.latitude, state.zoom)
            val markerX = cw / 2f + (px * tileSize - centerPixelX).toFloat()
            val markerY = ch / 2f + (py * tileSize - centerPixelY).toFloat()

            if (markerX in -60f..cw + 60f && markerY in -60f..ch + 60f) {
                val color = when (place.type) {
                    PlaceType.PARK -> Color(0xFF4CAF50)
                    PlaceType.STADIUM -> Color(0xFF2196F3)
                    PlaceType.GYM -> Color(0xFFFF5722)
                    PlaceType.STREET -> Color(0xFF9C27B0)
                }
                drawCircle(color = color.copy(alpha = 0.3f), radius = 22f, center = Offset(markerX, markerY))
                drawCircle(color = color, radius = 13f, center = Offset(markerX, markerY))
                drawCircle(color = Color.White, radius = 5f, center = Offset(markerX, markerY))
            }
        }

        val cx = cw / 2f
        val cy = ch / 2f
        drawCircle(color = Color(0xFFD32F2F).copy(alpha = 0.15f), radius = 26f, center = Offset(cx, cy))
        drawCircle(color = Color(0xFFD32F2F).copy(alpha = 0.8f), radius = 8f, center = Offset(cx, cy))
        drawCircle(color = Color.White, radius = 3f, center = Offset(cx, cy))
    }
}
