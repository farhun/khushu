package com.kaizen.khushu.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaizen.khushu.ui.theme.Antonio
import com.kaizen.khushu.ui.theme.BeVietnamPro
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay


// --- Bezier Arc Math ---
private val P0 = Offset(0f, 130f)
private val P1 = Offset(166f, -18f)
private val P2 = Offset(332f, 130f)

private fun bpt(t: Float): Offset {
    val u = 1 - t
    return Offset(
        u * u * P0.x + 2 * u * t * P1.x + t * t * P2.x,
        u * u * P0.y + 2 * u * t * P1.y + t * t * P2.y
    )
}

private fun insetArcMarkerT(t: Float): Float {
    return (0.06f + t.coerceIn(0f, 1f) * 0.88f).coerceIn(0f, 1f)
}


@Composable
fun SunArcCard(
    sunT: Float,
    nextT: Float?,
    nextName: String,
    makruhZones: List<MakruhZone>,
    darkTheme: Boolean,
    sunriseTime: String = "",
    sunsetTime: String = "",
    pastPrayerTs: List<Float> = emptyList(),
    modifier: Modifier = Modifier
) {
    var activeMk: Int? by remember { mutableStateOf(null) }
    val effectiveNextT =
        when {
            nextT == null -> sunT.coerceIn(0f, 1f)
            nextT < sunT -> 1f
            else -> nextT
        }


    Surface(
        modifier = modifier.size(161.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 6.dp,
        tonalElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val inMakruh = makruhZones.find { sunT >= it.tStart && sunT <= it.tEnd }
            val makruhOverlayColor = Color(0xFFD98A24).copy(alpha = 0.10f)

            Column(
                modifier = Modifier.padding(top = 6.dp, start = 13.dp, end = 13.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "SUN PATH",
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                letterSpacing = 0.09.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                        color =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.5f
                            )
                    )
                    if (inMakruh != null) {
                        Box(
                            modifier =
                                Modifier.background(
                                    Color.Red.copy(
                                        alpha =
                                            0.15f
                                    ),
                                    RoundedCornerShape(
                                        4.dp
                                    )
                                )
                                    .padding(
                                        horizontal = 5.dp,
                                        vertical = 2.dp
                                    )
                        ) {
                            Text(
                                text =
                                    "${inMakruh.label.uppercase()} NOW",
                                style =
                                    MaterialTheme.typography
                                        .labelSmall.copy(
                                            fontSize = 7.sp,
                                            fontWeight =
                                                FontWeight
                                                    .Bold,
                                            letterSpacing =
                                                0.07.sp
                                        ),
                                color =
                                    Color(
                                        0xFFE04030
                                    ) // Hardcoded Makruh red
                            )
                        }
                    } else {
                        Text(
                            text =
                                if (darkTheme) "Night"
                                else "Morning",
                            style =
                                MaterialTheme.typography.labelSmall
                                    .copy(fontSize = 8.sp),
                            color =
                                MaterialTheme.colorScheme.onSurface
                                    .copy(alpha = 0.4f)
                        )
                    }
                }
            }

            if (inMakruh != null) {
                Box(
                    modifier =
                        Modifier.matchParentSize()
                            .background(makruhOverlayColor)
                )
            }

            // --- Canvas Drawing ---
            val arcBaseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            val arcSolidColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            val arcNextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            val mkruhColor = Color(0xFFE04030) // Hardcoded Makruh red
            val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
            val primaryColor = MaterialTheme.colorScheme.primary

            Canvas(
                modifier =
                    Modifier.fillMaxSize().pointerInput(
                        makruhZones,
                        sunT,
                        effectiveNextT
                    ) {
                        detectTapGestures { offset ->
                            val pad = 13f.dp.toPx()
                            val svgW =
                                size.width.toFloat() - 26f.dp.toPx()
                            val svgH =
                                size.height.toFloat() -
                                        58f.dp.toPx()

                            val tS = max(0f, sunT - 0.17f)
                            val tE = min(1f, effectiveNextT + 0.12f)
                            val steps = 60
                            val rawPts =
                                (0..steps).map {
                                    bpt(
                                        tS +
                                                (tE - tS) *
                                                (it.toFloat() /
                                                        steps)
                                    )
                                }

                            val xs = rawPts.map { it.x }
                            val ys = rawPts.map { it.y }
                            val x0 = xs.minOrNull() ?: 0f
                            val x1 = xs.maxOrNull() ?: 1f
                            val y0 = ys.minOrNull() ?: 0f
                            val y1 = ys.maxOrNull() ?: 1f

                            val sx =
                                svgW /
                                        (if (x1 == x0) 1f
                                        else x1 - x0)
                            val sy =
                                svgH /
                                        (if (y1 == y0) 1f
                                        else y1 - y0)
                            val s = min(sx, sy)

                            val ox = pad + (svgW - (x1 - x0) * s) / 2
                            val oy = pad + (svgH - (y1 - y0) * s) / 2

                            fun mapPt(p: Offset) =
                                Offset(
                                    ox + (p.x - x0) * s,
                                    oy + (p.y - y0) * s
                                )

                            val hitX = offset.x
                            val hitY = offset.y - 26f.dp.toPx()
                            val tapPt = Offset(hitX, hitY)
                            val hitRadius =
                                40f.dp.toPx() // Increased touch
                            // target for Makruh
                            // zones

                            var found = false
                            for (i in makruhZones.indices) {
                                val mk = makruhZones[i]
                                if (mk.tEnd >= tS && mk.tStart <= tE
                                ) {
                                    val mS = max(mk.tStart, tS)
                                    val mE = min(mk.tEnd, tE)
                                    val mpts =
                                        (0..15).map {
                                            mapPt(
                                                bpt(
                                                    mS +
                                                            (mE -
                                                                    mS) *
                                                            (it /
                                                                    15f)
                                                )
                                            )
                                        }
                                    if (mpts.any {
                                            (it - tapPt)
                                                .getDistance() <
                                                    hitRadius
                                        }
                                    ) {
                                        activeMk =
                                            if (activeMk ==
                                                i
                                            )
                                                null
                                            else i
                                        found = true
                                        break
                                    }
                                }
                            }

                            if (!found) {
                                activeMk = null
                            }
                        }
                    }
            ) {
                val pad = 13f.dp.toPx()
                val svgW = size.width - 26f.dp.toPx()
                val svgH = size.height - 58f.dp.toPx()

                val tS = max(0f, sunT - 0.17f)
                val tE = min(1f, effectiveNextT + 0.12f)
                val steps = 60
                val rawPts =
                    (0..steps).map {
                        bpt(tS + (tE - tS) * (it.toFloat() / steps))
                    }

                val xs = rawPts.map { it.x }
                val ys = rawPts.map { it.y }
                val x0 = xs.minOrNull() ?: 0f
                val x1 = xs.maxOrNull() ?: 1f
                val y0 = ys.minOrNull() ?: 0f
                val y1 = ys.maxOrNull() ?: 1f

                val sx = svgW / (if (x1 == x0) 1f else x1 - x0)
                val sy = svgH / (if (y1 == y0) 1f else y1 - y0)
                val s = min(sx, sy)

                val ox = pad + (svgW - (x1 - x0) * s) / 2
                val oy = pad + (svgH - (y1 - y0) * s) / 2

                fun mapPt(p: Offset) =
                    Offset(ox + (p.x - x0) * s, oy + (p.y - y0) * s)

                translate(left = 0f, top = 26f.dp.toPx()) {


                    val sunM = mapPt(bpt(insetArcMarkerT(sunT)))
                    val nextM = mapPt(bpt(insetArcMarkerT(effectiveNextT)))

                    // Sun glow
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.1f),
                        radius = 30f.dp.toPx(),
                        center = sunM
                    )

                    // Base arc path
                    val pathAll =
                        Path().apply {
                            rawPts.forEachIndexed { i, p ->
                                val mp = mapPt(p)
                                if (i == 0) moveTo(mp.x, mp.y)
                                else lineTo(mp.x, mp.y)
                            }
                        }
                    drawPath(
                        path = pathAll,
                        color = arcBaseColor,
                        style =
                            Stroke(
                                width = 1.3f.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect =
                                    androidx.compose.ui.graphics
                                        .PathEffect
                                        .dashPathEffect(
                                            floatArrayOf(
                                                4f.dp.toPx(),
                                                6f.dp.toPx()
                                            )
                                        )
                            )
                    )

                    // Past trail path
                    val progIdx =
                        ((sunT - tS) / (tE - tS) * steps)
                            .toInt()
                            .coerceIn(0, steps)
                    if (progIdx > 0) {
                        val pathDone =
                            Path().apply {
                                rawPts.take(progIdx + 1)
                                    .forEachIndexed { i, p ->
                                        val mp = mapPt(p)
                                        if (i == 0)
                                            moveTo(
                                                mp.x,
                                                mp.y
                                            )
                                        else
                                            lineTo(
                                                mp.x,
                                                mp.y
                                            )
                                    }
                            }
                        drawPath(
                            path = pathDone,
                            color = arcSolidColor,
                            style =
                                Stroke(
                                    width = 1.8f.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                        )
                    }

                    // Makruh segments
                    makruhZones.forEachIndexed { index, mk ->
                        if (mk.tEnd >= tS && mk.tStart <= tE) {
                            val mS = max(mk.tStart, tS)
                            val mE = min(mk.tEnd, tE)
                            val mpts =
                                (0..7).map {
                                    mapPt(
                                        bpt(
                                            mS +
                                                    (mE -
                                                            mS) *
                                                    (it /
                                                            7f)
                                        )
                                    )
                                }
                            val mdPath =
                                Path().apply {
                                    mpts.forEachIndexed { j, p
                                        ->
                                        if (j == 0)
                                            moveTo(
                                                p.x,
                                                p.y
                                            )
                                        else
                                            lineTo(
                                                p.x,
                                                p.y
                                            )
                                    }
                                }
                            val isA = activeMk == index
                            drawPath(
                                path = mdPath,
                                color = mkruhColor,
                                style =
                                    Stroke(
                                        width =
                                            if (isA)
                                                9f.dp.toPx()
                                            else
                                                5f.dp.toPx(),
                                        cap =
                                            StrokeCap
                                                .Round
                                    ),
                                alpha = if (isA) 0.7f else 0.36f
                            )
                        }
                    }

                    // Past prayer dots — same size as next prayer filled dot,
                    // higher opacity
                    pastPrayerTs.forEach { pastT ->
                        val pastM = mapPt(bpt(insetArcMarkerT(pastT)))
                        drawCircle(
                            color = arcNextColor,
                            radius = 3.8f.dp.toPx(),
                            center = pastM,
                            alpha = 0.55f
                        )
                    }

                    // Next prayer dot (ring + filled)
                    drawCircle(
                        color = arcNextColor,
                        radius = 8.5f.dp.toPx(),
                        center = nextM,
                        style = Stroke(width = 1.1f.dp.toPx()),
                        alpha = 0.4f
                    )
                    drawCircle(
                        color = arcNextColor,
                        radius = 3.8f.dp.toPx(),
                        center = nextM
                    )

                    // Sun / Moon drawing
                    if (!darkTheme) {
                        drawCircle(
                            color =
                                Color(0xFFFAC82D)
                                    .copy(alpha = 0.18f),
                            radius = 17f.dp.toPx(),
                            center = sunM
                        )
                        drawCircle(
                            color =
                                Color(0xFFFFC328)
                                    .copy(alpha = 0.32f),
                            radius = 12f.dp.toPx(),
                            center = sunM
                        )
                        drawCircle(
                            color =
                                Color(0xFFF8C832)
                                    .copy(alpha = 0.85f),
                            radius = 9f.dp.toPx(),
                            center = sunM,
                            style = Stroke(width = 2.5f.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFFFDE03C),
                            radius = 5.5f.dp.toPx(),
                            center = sunM
                        )
                    } else {
                        val craterColor = surfaceColor
                        drawCircle(
                            color =
                                Color(0xFFB4A2FF)
                                    .copy(alpha = 0.15f),
                            radius = 15f.dp.toPx(),
                            center = sunM
                        )
                        drawCircle(
                            color =
                                Color(0xFFB9A8FF)
                                    .copy(alpha = 0.25f),
                            radius = 10f.dp.toPx(),
                            center = sunM
                        )
                        drawCircle(
                            color =
                                Color(0xFFD2CCFC)
                                    .copy(alpha = 0.9f),
                            radius = 8.5f.dp.toPx(),
                            center = sunM
                        )
                        drawCircle(
                            color = craterColor,
                            radius = 7f.dp.toPx(),
                            center =
                                Offset(
                                    sunM.x + 3.2f.dp.toPx(),
                                    sunM.y - 0.8f.dp.toPx()
                                )
                        )
                    }
                }
            }

            // Sunrise / Sunset corner labels with optional times
            Column(
                modifier =
                    Modifier.align(Alignment.BottomStart)
                        .padding(start = 13.dp, bottom = 10.dp)
            ) {
                Text(
                    text = "Sunrise",
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                    color =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                )
                if (sunriseTime.isNotBlank()) {
                    Text(
                        text = sunriseTime,
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                        color =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            )
                    )
                }
            }
            Column(
                modifier =
                    Modifier.align(Alignment.BottomEnd)
                        .padding(end = 13.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Sunset",
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                    color =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                )
                if (sunsetTime.isNotBlank()) {
                    Text(
                        text = sunsetTime,
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                        color =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun shimmerBrush(): Brush {
    val colors =
        listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by
    transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
        label = "shimmer_x"
    )
    return Brush.linearGradient(
        colors = colors,
        start = androidx.compose.ui.geometry.Offset(x - 300f, 0f),
        end = androidx.compose.ui.geometry.Offset(x, 0f),
    )
}

@Composable
fun PrayerSunMergedCardShimmer(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 6.dp,
        tonalElevation = 4.dp
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme
                                    .surfaceContainer
                            )
                            .padding(
                                start = 18.dp,
                                top = 13.dp,
                                bottom = 8.dp,
                                end = 14.dp
                            ),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        Modifier.width(74.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Box(
                        Modifier.fillMaxWidth(0.46f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                    Box(
                        Modifier.width(46.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
                Box(
                    Modifier.width(1.dp)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.08f
                            )
                        )
                )
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme
                                    .surfaceContainerLow
                            )
                            .padding(
                                start = 18.dp,
                                top = 13.dp,
                                bottom = 8.dp,
                                end = 14.dp
                            ),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        Modifier.width(72.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Box(
                        Modifier.fillMaxWidth(0.44f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                    Box(
                        Modifier.width(46.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
            }
            Box(
                Modifier.fillMaxWidth()
                    .height(1.dp)
                    .background(
                        MaterialTheme.colorScheme.outline.copy(
                            alpha = 0.06f
                        )
                    )
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(100.dp)
                        .background(
                            MaterialTheme.colorScheme
                                .surfaceContainerLow
                        ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val hPad = 22.dp.toPx()
                    val lineY = 38.dp.toPx()
                    val curveHeight = 9.dp.toPx()
                    val path =
                        Path().apply {
                            moveTo(hPad, lineY)
                            quadraticTo(
                                size.width / 2f,
                                lineY - curveHeight,
                                size.width - hPad,
                                lineY
                            )
                        }
                    drawPath(
                        path = path,
                        brush = brush,
                        style =
                            Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerSunMergedCard(
    currentPrayer: PrayerInfo?,
    nextPrayer: PrayerInfo?,
    sunT: Float,
    allPrayers: List<PrayerInfo>,
    makruhZones: List<MakruhZone>,
    darkTheme: Boolean,
    sunriseTime: String = "",
    sunsetTime: String = "",
    modifier: Modifier = Modifier,
) {
    var activeMk: Int? by remember { mutableStateOf(null) }

    val arcBaseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val arcSolidColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val arcNextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
    val mkruhColor = Color(0xFFE04030)
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
    val primaryColor = MaterialTheme.colorScheme.primary

    val pastPrayerTs =
        allPrayers.filterNot { it.isExtra }.filter { sunT > it.arcT }.map { it.arcT }

    val currentMkZone = makruhZones.find { sunT >= it.tStart && sunT <= it.tEnd }

    // Dynamic background colors for active Makruh zones
    val makruhOverlayColor =
        when {
            currentMkZone == null -> Color.Transparent
            currentMkZone.label.contains("Sunrise", ignoreCase = true) ||
                    currentMkZone.label.contains("Sunset", ignoreCase = true) ->
                Color(0xFFFFD54F).copy(alpha = 0.12f) // Subtle Yellow
            else -> Color(0xFFE57373).copy(alpha = 0.12f) // Subtle Red (Peak)
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 6.dp,
        tonalElevation = 4.dp
    ) {
        Box {
            Column {
                // ── Dual-tone prayer section ───────────────────────
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    // Left — Current prayer
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme
                                        .surfaceContainerLow
                                )
                                .padding(
                                    start = 18.dp,
                                    top = 13.dp,
                                    bottom = 8.dp,
                                    end = 14.dp
                                )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement =
                                Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    letterSpacing = 0.09.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = currentPrayer?.name
                                        ?: "—",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .displaySmall
                                            .copy(
                                                fontSize =
                                                    20.sp
                                            ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = currentPrayer?.time
                                        ?: "--:--",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .displaySmall
                                            .copy(
                                                fontFamily =
                                                    Antonio,
                                                fontSize =
                                                    17.sp
                                            ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                            .copy(
                                                alpha =
                                                    0.50f
                                            ),
                                    modifier =
                                        Modifier.padding(
                                            bottom =
                                                3.dp
                                        )
                                )
                            }
                        }
                    }
                    // Vertical divider
                    Box(
                        Modifier.width(1.dp)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.outline
                                    .copy(alpha = 0.08f)
                            )
                    )
                    // Right — Next prayer
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme
                                        .surfaceContainer
                                )
                                .padding(
                                    start = 18.dp,
                                    top = 13.dp,
                                    bottom = 8.dp,
                                    end = 14.dp
                                )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "NEXT PRAYER",
                                style =
                                    MaterialTheme.typography
                                        .labelSmall.copy(
                                            fontSize = 8.5.sp,
                                            letterSpacing =
                                                0.09.sp,
                                            fontWeight =
                                                FontWeight
                                                    .SemiBold
                                        ),
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface.copy(
                                            alpha = 0.45f
                                        )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = nextPrayer?.name
                                        ?: "—",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .displaySmall
                                            .copy(
                                                fontSize =
                                                    17.sp
                                            ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = nextPrayer?.time
                                        ?: "--:--",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .displaySmall
                                            .copy(
                                                fontFamily =
                                                    Antonio,
                                                fontSize =
                                                    13.sp
                                            ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                            .copy(
                                                alpha =
                                                    0.50f
                                            ),
                                    modifier =
                                        Modifier.padding(
                                            bottom =
                                                3.dp
                                        )
                                )
                            }
                        }
                    }
                }
                // ── Horizontal separator
                // ──────────────────────────────────────────────
                Box(
                    Modifier.fillMaxWidth()
                        .height(1.dp)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.06f
                            )
                        )
                )

                // The prayer header above is retained; the sun timeline is hidden.
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(0.dp)
                            .clipToBounds()
                            .background(
                                MaterialTheme.colorScheme
                                    .surfaceContainerLow
                                    .copy(alpha = if (currentMkZone != null) 0.5f else 1f)
                            )
                            .background(makruhOverlayColor)
                            .pointerInput(makruhZones) {
                                detectTapGestures { offset
                                    ->
                                    val hPad =
                                        20.dp.toPx()
                                    val lineLen =
                                        size.width -
                                                2 *
                                                hPad
                                    val lineY =
                                        38.dp.toPx()
                                    val tapped =
                                        ((offset.x -
                                                hPad) /
                                                lineLen)
                                            .coerceIn(
                                                0f,
                                                1f
                                            )
                                    var found = false
                                    for (i in
                                    makruhZones
                                        .indices) {
                                        val mk =
                                            makruhZones[
                                                i]
                                        if (tapped >=
                                            mk.tStart -
                                            0.04f &&
                                            tapped <=
                                            mk.tEnd +
                                            0.04f &&
                                            kotlin.math
                                                .abs(
                                                    offset.y -
                                                            lineY
                                                ) <
                                            38.dp.toPx()
                                        ) {
                                            activeMk =
                                                if (activeMk ==
                                                    i
                                                )
                                                    null
                                                else
                                                    i
                                            found =
                                                true
                                            break
                                        }
                                    }
                                    if (!found)
                                        activeMk =
                                            null
                                }
                            }
                ) {
                    val onSurfaceColor =
                        MaterialTheme.colorScheme.onSurface.toArgb()
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val hPad = 22.dp.toPx()
                        val lineY = 40.dp.toPx()
                        val lineStartX = hPad
                        val lineEndX = size.width - hPad
                        val lineLen = lineEndX - lineStartX

                        // Night zone boundaries derived from Makruh
                        // zones
                        val dayStartT =
                            makruhZones
                                .firstOrNull {
                                    it.label.contains(
                                        "Sunrise",
                                        ignoreCase =
                                            true
                                    )
                                }
                                ?.tStart
                                ?: 0.25f
                        val dayEndT =
                            makruhZones
                                .firstOrNull {
                                    it.label.contains(
                                        "Sunset",
                                        ignoreCase =
                                            true
                                    )
                                }
                                ?.tEnd
                                ?: 0.75f
                        val dayDurationT = dayEndT - dayStartT
                        val nightDurationT = 1.0f - dayDurationT

                        fun tToX(t: Float) =
                            lineStartX + t * lineLen

                        /**
                         * Continuous Sinusoidal Horizon:
                         * - Day Arc: sin wave above horizon between
                         * Sunrise and Sunset.
                         * - Night Arc: sin wave below horizon
                         * between Sunset and Sunrise.
                         */
                        fun curveY(t: Float): Float {
                            return if (t in dayStartT..dayEndT
                            ) {
                                val normalizedDayT =
                                    (t - dayStartT) /
                                            dayDurationT
                                lineY -
                                        14.dp.toPx() *
                                        kotlin.math
                                            .sin(
                                                kotlin.math
                                                    .PI *
                                                        normalizedDayT
                                            )
                                            .toFloat()
                            } else {
                                val normalizedNightT =
                                    if (t > dayEndT) {
                                        (t -
                                                dayEndT) /
                                                nightDurationT
                                    } else {
                                        (t +
                                                (1.0f -
                                                        dayEndT)) /
                                                nightDurationT
                                    }
                                lineY +
                                        9.dp.toPx() *
                                        kotlin.math
                                            .sin(
                                                kotlin.math
                                                    .PI *
                                                        normalizedNightT
                                            )
                                            .toFloat()
                            }
                        }

                        fun pointAt(t: Float): Offset =
                            Offset(tToX(t), curveY(t))

                        val sunX = tToX(sunT)


                        // ── 2. The Horizon Baseline
                        drawLine(
                            color =
                                arcBaseColor.copy(
                                    alpha = 0.08f
                                ),
                            start = Offset(lineStartX, lineY),
                            end = Offset(lineEndX, lineY),
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // ── 3. The Full 24h Path (Dashed Base)
                        val fullPath =
                            Path().apply {
                                val steps = 60
                                moveTo(
                                    lineStartX,
                                    curveY(0f)
                                )
                                for (i in 1..steps) {
                                    val t =
                                        i.toFloat() /
                                                steps
                                    val p = pointAt(t)
                                    lineTo(p.x, p.y)
                                }
                            }
                        drawPath(
                            fullPath,
                            color = arcBaseColor,
                            style =
                                Stroke(
                                    width =
                                        1.5.dp
                                            .toPx(),
                                    pathEffect =
                                        PathEffect
                                            .dashPathEffect(
                                                floatArrayOf(
                                                    4.dp.toPx(),
                                                    6.dp.toPx()
                                                )
                                            )
                                )
                        )

                        // ── 4. The Past Trail (Solid Sinusoid)
                        if (sunT > 0f) {
                            val pastPath =
                                Path().apply {
                                    val steps =
                                        (sunT * 60)
                                            .toInt()
                                            .coerceAtLeast(
                                                1
                                            )
                                    moveTo(
                                        lineStartX,
                                        curveY(0f)
                                    )
                                    for (i in
                                    1..steps) {
                                        val t =
                                            (i.toFloat() /
                                                    steps) *
                                                    sunT
                                        val p =
                                            pointAt(
                                                t
                                            )
                                        lineTo(
                                            p.x,
                                            p.y
                                        )
                                    }
                                }
                            drawPath(
                                pastPath,
                                color = arcSolidColor,
                                style =
                                    Stroke(
                                        width =
                                            2.5.dp
                                                .toPx(),
                                        cap =
                                            StrokeCap
                                                .Round
                                    )
                            )
                        }

                        // ── 5. Makruh Zones (Thermal Shift
                        // Highlights)
                        makruhZones.forEach { mk ->
                            val mkPath =
                                Path().apply {
                                    val steps = 15
                                    val startP =
                                        pointAt(
                                            mk.tStart
                                        )
                                    moveTo(
                                        startP.x,
                                        startP.y
                                    )
                                    for (i in
                                    1..steps) {
                                        val t =
                                            mk.tStart +
                                                    (mk.tEnd -
                                                            mk.tStart) *
                                                    (i.toFloat() /
                                                            steps)
                                        val p =
                                            pointAt(
                                                t
                                            )
                                        lineTo(
                                            p.x,
                                            p.y
                                        )
                                    }
                                }
                            drawPath(
                                mkPath,
                                color = mkruhColor,
                                style =
                                    Stroke(
                                        width =
                                            4.dp.toPx(),
                                        cap =
                                            StrokeCap
                                                .Round
                                    ),
                                alpha = 0.8f
                            )
                        }

                        // ── 6. Prayer Dots
                        allPrayers
                            .filterNot { it.isExtra }
                            .forEach { prayer ->
                                val pPos =
                                    pointAt(prayer.arcT)
                                val isNext =
                                    prayer.name ==
                                            nextPrayer
                                                ?.name
                                val isPast =
                                    sunT > prayer.arcT

                                if (isNext) {
                                    drawCircle(
                                        arcNextColor,
                                        9.dp.toPx(),
                                        pPos,
                                        style =
                                            Stroke(
                                                1.2.dp
                                                    .toPx()
                                            ),
                                        alpha = 0.4f
                                    )
                                    drawCircle(
                                        arcNextColor,
                                        4.dp.toPx(),
                                        pPos
                                    )
                                } else {
                                    drawCircle(
                                        color =
                                            arcNextColor,
                                        radius =
                                            3.5.dp
                                                .toPx(),
                                        center =
                                            pPos,
                                        alpha =
                                            if (isPast
                                            )
                                                0.6f
                                            else
                                                0.25f
                                    )
                                }
                            }

                        // ── 7. The Sun/Moon Celestial Body
                        val sunPos = pointAt(sunT)
                        val glowColor =
                            if (sunT in dayStartT..dayEndT)
                                Color(0xFFFAC82D)
                            else Color(0xFFB4A2FF)

                        // Outer Glow
                        drawCircle(
                            glowColor.copy(alpha = 0.15f),
                            20.dp.toPx(),
                            sunPos
                        )

                        if (sunT in dayStartT..dayEndT) {
                            // Sun Core
                            drawCircle(
                                glowColor,
                                6.dp.toPx(),
                                sunPos
                            )
                        } else {
                            // Moon Crescent
                            drawCircle(
                                glowColor.copy(
                                    alpha = 0.3f
                                ),
                                6.dp.toPx(),
                                sunPos
                            )
                            drawCircle(
                                surfaceColor,
                                5.dp.toPx(),
                                Offset(
                                    sunPos.x +
                                            2.5.dp
                                                .toPx(),
                                    sunPos.y -
                                            1.dp.toPx()
                                )
                            )
                        }

                        // ── 8. Text Labels (Native Canvas)
                        val labelPaint =
                            android.graphics.Paint().apply {
                                isAntiAlias = true
                                textSize = 7.sp.toPx()
                                textAlign =
                                    android.graphics
                                        .Paint.Align
                                        .CENTER
                                color = onSurfaceColor
                                alpha =
                                    (255 * 0.45f)
                                        .toInt()
                            }
                        val nextLabelPaint =
                            android.graphics.Paint().apply {
                                isAntiAlias = true
                                textSize = 7.5.sp.toPx()
                                textAlign =
                                    android.graphics
                                        .Paint.Align
                                        .CENTER
                                color = onSurfaceColor
                                alpha = (255 * 0.9f).toInt()
                                isFakeBoldText = true
                            }

                        drawIntoCanvas { canvas ->
                            allPrayers
                                .filterNot { it.isExtra }
                                .forEach { prayer ->
                                    val pPos =
                                        pointAt(
                                            prayer.arcT
                                        )
                                    val paint =
                                        if (prayer.name ==
                                            nextPrayer
                                                ?.name
                                        )
                                            nextLabelPaint
                                        else
                                            labelPaint
                                    canvas.nativeCanvas
                                        .drawText(
                                            prayer.name,
                                            pPos.x,
                                            pPos.y +
                                                    18.dp.toPx(),
                                            paint
                                        )
                                }
                        }
                    }

                    // Sunrise label: WbTwilight + arrow-up prefix +
                    // time
                    if (currentMkZone != null) {
                        val badgeColor =
                            if (currentMkZone.label.contains(
                                    "Sunrise",
                                    ignoreCase = true
                                ) ||
                                currentMkZone.label
                                    .contains(
                                        "Sunset",
                                        ignoreCase =
                                            true
                                    )
                            )
                                Color(0xFFFFB300)
                            else Color(0xFFE53935)
                        Box(
                            modifier =
                                Modifier.align(
                                    Alignment
                                        .TopEnd
                                )
                                    .padding(
                                        top = 6.dp,
                                        end = 10.dp
                                    )
                                    .background(
                                        badgeColor
                                            .copy(
                                                alpha =
                                                    0.18f
                                            ),
                                        RoundedCornerShape(
                                            4.dp
                                        )
                                    )
                                    .padding(
                                        horizontal =
                                            5.dp,
                                        vertical =
                                            2.dp
                                    )
                        ) {
                            Text(
                                text =
                                    "MAKRUH · ${currentMkZone.label.uppercase()}",
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall
                                        .copy(
                                            fontSize =
                                                7.sp,
                                            fontWeight =
                                                FontWeight
                                                    .Bold,
                                            letterSpacing =
                                                0.07.sp
                                        ),
                                color = badgeColor
                            )
                        }
                    }
                    Row(
                        modifier =
                            Modifier.align(
                                Alignment
                                    .BottomStart
                            )
                                .padding(
                                    start = 18.dp,
                                    bottom = 8.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.WbTwilight,
                            contentDescription = null,
                            tint =
                                Color(0xFFFFB74D)
                                    .copy(
                                        alpha =
                                            0.85f
                                    ),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                "Sunrise",
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall
                                        .copy(
                                            fontSize =
                                                9.sp,
                                            fontWeight =
                                                FontWeight
                                                    .Medium
                                        ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface
                                        .copy(
                                            alpha =
                                                0.45f
                                        )
                            )
                            if (sunriseTime.isNotBlank())
                                Text(
                                    sunriseTime,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall
                                            .copy(
                                                fontFamily =
                                                    BeVietnamPro,
                                                fontSize =
                                                    9.sp,
                                                fontWeight =
                                                    FontWeight
                                                        .Medium
                                            ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                )
                        }
                    }

                    // Sunset label: WbTwilight + arrow-down prefix +
                    // time
                    Row(
                        modifier =
                            Modifier.align(Alignment.BottomEnd)
                                .padding(
                                    end = 18.dp,
                                    bottom = 8.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "Sunset",
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall
                                        .copy(
                                            fontSize =
                                                9.sp,
                                            fontWeight =
                                                FontWeight
                                                    .Medium
                                        ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface
                                        .copy(
                                            alpha =
                                                0.45f
                                        )
                            )
                            if (sunsetTime.isNotBlank())
                                Text(
                                    sunsetTime,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall
                                            .copy(
                                                fontFamily =
                                                    BeVietnamPro,
                                                fontSize =
                                                    9.sp,
                                                fontWeight =
                                                    FontWeight
                                                        .Medium
                                            ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                )
                        }
                        Icon(
                            Icons.Filled.WbTwilight,
                            contentDescription = null,
                            tint =
                                Color(0xFF7986CB)
                                    .copy(
                                        alpha =
                                            0.70f
                                    ),
                            modifier =
                                Modifier.size(18.dp)
                                    .graphicsLayer {
                                        rotationZ =
                                            180f
                                    }
                        )
                    }
                }
                // ── Makruh info overlay ──────────────────────────────
                val mkOverlayAlpha by animateFloatAsState(
                    targetValue = if (activeMk != null) 1f else 0f,
                    animationSpec = tween(220),
                    label = "mk_overlay"
                )
                if (mkOverlayAlpha > 0f) {
                    val mk = makruhZones[activeMk ?: 0]
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .graphicsLayer {
                                    alpha =
                                        mkOverlayAlpha
                                }
                                .background(
                                    Color.Black
                                        .copy(
                                            alpha =
                                                0.86f
                                        )
                                )
                                .clickable {
                                    activeMk =
                                        null
                                }
                                .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment =
                                Alignment
                                    .CenterHorizontally
                        ) {
                            Text(
                                text =
                                    "Makruh · ${mk.label}"
                                        .uppercase(),
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall
                                        .copy(
                                            fontSize =
                                                7.5.sp,
                                            fontWeight =
                                                FontWeight
                                                    .Bold,
                                            letterSpacing =
                                                0.09.sp
                                        ),
                                color =
                                    Color(
                                        0xFFF06045
                                    ),
                                modifier =
                                    Modifier
                                        .padding(
                                            bottom =
                                                6.dp
                                        )
                            )
                            Text(
                                text =
                                    mk.description,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                                        .copy(
                                            fontSize =
                                                10.5.sp,
                                            fontWeight =
                                                FontWeight
                                                    .Light,
                                            lineHeight =
                                                16.sp
                                        ),
                                color =
                                    Color.White
                                        .copy(
                                            alpha =
                                                0.8f
                                        ),
                                textAlign =
                                    androidx
                                        .compose
                                        .ui
                                        .text
                                        .style
                                        .TextAlign
                                        .Center
                            )
                            Text(
                                text =
                                    "Tap to dismiss",
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall
                                        .copy(
                                            fontSize =
                                                8.sp
                                        ),
                                color =
                                    Color.White
                                        .copy(
                                            alpha =
                                                0.3f
                                        ),
                                modifier =
                                    Modifier
                                        .padding(
                                            top =
                                                10.dp
                                        )
                            )
                        }
                    }
                }
            } // end sunpath Box
        } // end Column


    } // end outer Box
} // end Surface

@Composable
fun NextPrayerCard(
    currentPrayer: PrayerInfo?,
    nextPrayer: PrayerInfo?,
    locationLabel: String,
    doneCount: Int,
    source: CalculationSource = CalculationSource.LOCAL,
    usingPreviewTime: Boolean = false,
    onTimeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeClock by remember { mutableStateOf("--:--") }
    val sourceLabel =
        when (source) {
            CalculationSource.LOCAL -> "Local"
            CalculationSource.API -> "API"
        }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            var hour = now.get(Calendar.HOUR)
            if (hour == 0) hour = 12
            val minute = now.get(Calendar.MINUTE)
            activeClock = String.format("%02d:%02d", hour, minute)
            delay(1_000L)
        }
    }

    Surface(
        modifier = modifier.size(161.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = 6.dp,
        tonalElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme
                                    .surfaceContainer
                            )
                )
                Box(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme
                                    .surfaceContainerLow
                            )
                )
            }
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.Center)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.08f
                            )
                        )
            )
            Box(
                modifier =
                    Modifier.padding(
                        top = 14.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 13.dp
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text(
                                text = locationLabel.uppercase(),
                                style =
                                    MaterialTheme.typography
                                        .labelSmall.copy(
                                            fontSize = 8.5.sp,
                                            letterSpacing =
                                                0.09.sp,
                                            fontWeight =
                                                FontWeight
                                                    .SemiBold
                                        ),
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface.copy(
                                            alpha = 0.5f
                                        ),
                                modifier =
                                    Modifier.padding(
                                        bottom = 6.dp
                                    )
                            )
                            // Source Indicator
                            Box(
                                modifier =
                                    Modifier.background(
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                                .copy(
                                                    alpha =
                                                        0.1f
                                                ),
                                        shape =
                                            RoundedCornerShape(
                                                4.dp
                                            )
                                    )
                                        .padding(
                                            horizontal =
                                                4.dp,
                                            vertical =
                                                2.dp
                                        )
                            ) {
                                Text(
                                    text = sourceLabel,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall
                                            .copy(
                                                fontSize =
                                                    7.sp,
                                                fontWeight =
                                                    FontWeight
                                                        .Bold
                                            ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                            .copy(
                                                alpha =
                                                    0.6f
                                            )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(), // Take
                            // full
                            // width
                            horizontalArrangement =
                                Arrangement.Center, // Center
                            // horizontally
                            verticalAlignment =
                                Alignment.CenterVertically // Center
                            // vertically
                        ) {
                            Text(
                                text = currentPrayer?.name
                                    ?: "Prayer",
                                style =
                                    MaterialTheme.typography
                                        .displaySmall.copy(
                                            fontSize = 24.sp
                                        ),
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = activeClock,
                                style =
                                    MaterialTheme.typography
                                        .titleLarge.copy(
                                            fontSize = 24.sp,
                                            fontWeight =
                                                FontWeight
                                                    .Medium
                                        ),
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface.copy(
                                            alpha = 0.62f
                                        ),
                                modifier =
                                    Modifier.clip(
                                        RoundedCornerShape(
                                            6.dp
                                        )
                                    )
                                        .clickable {
                                            onTimeClick()
                                        }
                                //
                                //  .padding(horizontal = 4.dp,
                                // vertical = 2.dp)
                            )
                        }
                        //                    Text(
                        //                            text =
                        // currentPrayer?.time ?: "Current prayer",
                        //                            style =
                        //
                        // MaterialTheme.typography.bodySmall.copy(
                        //
                        // fontSize = 12.sp,
                        //
                        // fontStyle = FontStyle.Italic
                        //                                    ),
                        //                            color =
                        // MaterialTheme.colorScheme.onSurface.copy(alpha =
                        // 0.5f),
                        //                            modifier =
                        //
                        // Modifier.padding(top = 3.dp)
                        //
                        // .clip(RoundedCornerShape(6.dp))
                        //
                        // .clickable { onTimeClick() }
                        //
                        // .padding(horizontal = 4.dp, vertical = 2.dp)
                        //                    )
                    }

                    Spacer(Modifier.height(14.dp))
                    Column {
                        Text(
                            text = "NEXT PRAYER",
                            style =
                                MaterialTheme.typography.labelSmall
                                    .copy(
                                        fontSize = 8.5.sp,
                                        letterSpacing =
                                            0.09.sp,
                                        fontWeight =
                                            FontWeight
                                                .SemiBold
                                    ),
                            color =
                                MaterialTheme.colorScheme.onSurface
                                    .copy(alpha = 0.5f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(), // Take
                            // full
                            // width
                            horizontalArrangement =
                                Arrangement.SpaceBetween, // Center
                            // horizontally
                            verticalAlignment =
                                Alignment.CenterVertically // Center
                            // vertically
                        ) {
                            Text(
                                text = nextPrayer?.name
                                    ?: "Next prayer",
                                style =
                                    MaterialTheme.typography
                                        .titleLarge.copy(
                                            fontSize = 16.sp,
                                            fontWeight =
                                                FontWeight
                                                    .Medium
                                        ),
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface,
                                modifier =
                                    Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = nextPrayer?.time ?: "--:--",
                                style =
                                    MaterialTheme.typography
                                        .titleLarge.copy(
                                            fontSize = 16.sp,
                                            //
                                            //
                                            //        fontStyle
                                            // =
                                            // FontStyle.Italic
                                        ),
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface.copy(
                                            alpha = 0.42f
                                        ),
                                //
                                // modifier = Modifier.padding(top =
                                // 2.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 9.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${doneCount}/5",
                                style =
                                    MaterialTheme.typography
                                        .labelSmall.copy(
                                            fontSize = 8.sp
                                        ),
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface.copy(
                                            alpha = 0.4f
                                        )
                            )
                            repeat(5) { i ->
                                Box(
                                    modifier =
                                        Modifier.padding(
                                            start =
                                                5.dp
                                        )
                                            .size(
                                                if (i <
                                                    doneCount
                                                )
                                                    7.dp
                                                else
                                                    5.5.dp
                                            )
                                            .clip(
                                                CircleShape
                                            )
                                            .background(
                                                if (i <
                                                    doneCount
                                                )
                                                    MaterialTheme
                                                        .colorScheme
                                                        .primary
                                                else
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurface
                                                        .copy(
                                                            alpha =
                                                                0.15f
                                                        )
                                            )
                                )
                            }
                        }

                        if (usingPreviewTime) {
                            Text(
                                text = "Preview time active",
                                style =
                                    MaterialTheme.typography
                                        .labelSmall.copy(
                                            fontSize = 8.sp
                                        ),
                                color =
                                    MaterialTheme.colorScheme
                                        .primary,
                                modifier =
                                    Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerSlab(
    prayers: List<PrayerInfo>,
    extraTimings: List<PrayerInfo>,
    activePrayerName: String?,
    onQiblaClick: () -> Unit,
    ayahRef: String,
    darkTheme: Boolean,
    locationLabel: String,
    source: CalculationSource,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier
) {

    val sourceLabel = when (source) {
        CalculationSource.LOCAL -> "Local"
        CalculationSource.API -> "API"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        start = 22.dp,
                        end = 22.dp,
                        top = 22.dp,
                        bottom = 32.dp + bottomPadding
                    )
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S PRAYERS",
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.09.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                        color =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.6f
                            )
                    )

                }

                if (locationLabel.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = locationLabel.uppercase(),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Prayer List
                prayers.forEachIndexed { i, p ->
                    val isActivePrayer = activePrayerName == p.name

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Name
                        Text(
                            text = p.name,
                            style =
                                MaterialTheme.typography.bodyLarge
                                    .copy(
                                        fontSize = 14.sp,
                                        fontWeight = if (isActivePrayer) FontWeight.SemiBold else FontWeight.Light
                                    ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )


                        // Arabic Name
                        Text(
                            text = p.ar,
                            style =
                                MaterialTheme.typography.bodySmall
                                    .copy(fontSize = 14.sp),
                            color =
                                MaterialTheme.colorScheme.onSurface
                                    .copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Time
                        Text(
                            text = p.time,
                            style =
                                MaterialTheme.typography.labelSmall
                                    .copy(
                                        fontSize = 13.sp,
                                        fontWeight =
                                            FontWeight
                                                .Medium
                                    ),
                            color =
                                MaterialTheme.colorScheme.onSurface
                                    .copy(alpha = 0.7f),
                            textAlign = TextAlign.End,
                            modifier =
                                Modifier.defaultMinSize(
                                    minWidth = 60.dp
                                )
                        )
                    }

                    if (i < prayers.size - 1) {
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                            .copy(
                                                alpha =
                                                    0.05f
                                            )
                                    )
                        )
                    }
                }

                //            Spacer(modifier = Modifier.height(14.dp))
                //            Box(
                //                modifier = Modifier
                //                    .fillMaxWidth()
                //                    .height(1.dp)
                //
                // .background(MaterialTheme.colorScheme.onSurface.copy(alpha =
                // 0.05f))
                //            )
                //            Spacer(modifier = Modifier.height(10.dp))

                // Ayah quote
                //            Text(
                //                text = "\"$ayahText\" — $ayahRef",
                //                style = MaterialTheme.typography.bodyMedium.copy(
                //                    fontSize = 11.5.sp,
                //                    fontStyle = FontStyle.Italic,
                //                    lineHeight = 16.5.sp
                //                ),
                //                color =
                // MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                //            )

                if (extraTimings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    MaterialTheme.colorScheme
                                        .onSurface.copy(
                                            alpha = 0.05f
                                        )
                                )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "EXTRA TIMINGS",
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 0.09.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                        color =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.5f
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    extraTimings.forEach { timing ->
                        val dotColor =
                            if (darkTheme) timing.dotColorDark
                            else timing.dotColorLight
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(vertical = 7.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            dotColor.copy(
                                                alpha =
                                                    0.92f
                                            )
                                        )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = timing.name,
                                style =
                                    MaterialTheme.typography
                                        .bodyLarge.copy(
                                            fontSize = 13.sp,
                                            fontWeight =
                                                FontWeight
                                                    .Light
                                        ),
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface.copy(
                                            alpha = 0.88f
                                        ),
                                modifier = Modifier.weight(1f)
                            )

                            if (timing.ar.isNotBlank()) {
                                Text(
                                    text = timing.ar,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                            .copy(
                                                fontSize =
                                                    13.sp
                                            ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                            .copy(
                                                alpha =
                                                    0.45f
                                            )
                                )
                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )
                            }

                            Text(
                                text = timing.time,
                                style =
                                    MaterialTheme.typography
                                        .bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight =
                                                FontWeight
                                                    .Medium
                                        ),
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface.copy(
                                            alpha = 0.55f
                                        )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                QuickDirectoryRow(onQiblaClick = onQiblaClick)
            }
        }
    }
}

private object LucideIcons {
    val Compass: ImageVector by lazy {
        ImageVector.Builder(
            name = "Compass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .apply {
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12f, 12f)
                    drawCircle(10f)
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(16.24f, 7.76f)
                    lineTo(14.436f, 13.171f)
                    curveToRelative(
                        -0.16f,
                        0.481f,
                        -0.553f,
                        0.875f,
                        -1.034f,
                        1.034f
                    )
                    lineTo(7.76f, 16.24f)
                    lineToRelative(1.804f, -5.411f)
                    curveToRelative(
                        0.16f,
                        -0.481f,
                        0.553f,
                        -0.875f,
                        1.034f,
                        -1.034f
                    )
                    close()
                }
            }
            .build()
    }

    private fun ImageVector.Builder.drawCircle(radius: Float) {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 12f - radius)
            arcToRelative(
                radius,
                radius,
                0f,
                isMoreThanHalf = true,
                isPositiveArc = true,
                0f,
                radius * 2
            )
            arcToRelative(
                radius,
                radius,
                0f,
                isMoreThanHalf = true,
                isPositiveArc = true,
                0f,
                -radius * 2
            )
            close()
        }
    }
}

@Composable
private fun QuickDirectoryRow(
    onQiblaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        ExploreMiniCard(
            icon = LucideIcons.Compass,
            label = "Qibla",
            supportLabel = null,
            modifier = Modifier.weight(1f),
            onClick = onQiblaClick
        )
    }
}

@Composable
private fun ExploreMiniCard(
    icon: ImageVector,
    label: String,
    supportLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier.height(84.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (supportLabel != null) {
                Text(
                    text = supportLabel,
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontFamily = BeVietnamPro,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        ),
                    color =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.45f
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
