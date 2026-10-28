package com.example.premiere.ui.history

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.material.icons.rounded.Movie as MovieIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.premiere.domain.model.Movie
import com.example.premiere.data.preferences.TicketSize
import com.example.premiere.data.preferences.TicketOrientation

val BebasNeue = FontFamily(
    Font(com.example.premiere.R.font.bebas_neue)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onAddClick: () -> Unit,
    onMovieTap: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val cinemas by viewModel.cinemas.collectAsStateWithLifecycle()
    val ticketColor by viewModel.ticketColor.collectAsStateWithLifecycle()
    val randomTicketColors by viewModel.randomTicketColors.collectAsStateWithLifecycle()
    val ticketSize by viewModel.ticketSize.collectAsStateWithLifecycle()
    val ticketOrientation by viewModel.ticketOrientation.collectAsStateWithLifecycle()
    val defaultCinema by viewModel.defaultCinema.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Theatrical History", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(com.example.premiere.theme.SurfaceGlass2)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null,
                                onClick = onAddClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Add History",
                            tint = com.example.premiere.theme.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No watched history yet.\nTap + to add one.",
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = (16).dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = (32).dp, top = (8).dp)
            ) {
                items(history, key = { it.id }) { movie ->
                    val colorString = if (ticketColor == "RANDOM") {
                        randomTicketColors[movie.id]
                    } else if (ticketColor == "POSTER") {
                        null
                    } else {
                        ticketColor
                    }
                    val bgColor = colorString?.let {
                        try {
                            Color(android.graphics.Color.parseColor(it))
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val cinemaName = movie.ticketCinema.ifBlank { defaultCinema }
                    val cinemaLogoUrl = cinemas.find { it.name == cinemaName }?.logoUrl

                    if (ticketOrientation == TicketOrientation.HORIZONTAL) {
                        HorizontalTicketItem(
                            movie = movie,
                            cinemaLogoUrl = cinemaLogoUrl,
                            backgroundColor = bgColor,
                            ticketSize = ticketSize,
                            onClick = { onMovieTap(movie) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        VerticalTicketItem(
                            movie = movie,
                            cinemaLogoUrl = cinemaLogoUrl,
                            backgroundColor = bgColor,
                            ticketSize = ticketSize,
                            onClick = { onMovieTap(movie) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalTicketItem(
    movie: Movie,
    cinemaLogoUrl: String? = null,
    backgroundColor: Color? = null,
    ticketSize: TicketSize = TicketSize.LARGE,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sizeMultiplier = when (ticketSize) {
        TicketSize.SMALL -> 0.77f
        TicketSize.MEDIUM -> 0.88f
        TicketSize.LARGE -> 1f
    }
    val stubWidthDp = (95 * sizeMultiplier).dp
    val ticketShape = remember(sizeMultiplier) {
        HorizontalTicketShape(
            stubWidthDp = stubWidthDp,
            cornerCutDp = (12 * sizeMultiplier).dp,
            notchRadiusDp = (12 * sizeMultiplier).dp
        )
    }
    val contentColor = Color(0xFFF4F0E6) // Cream/off-white for text on dark background
    val mainBgColor = backgroundColor ?: Color(0xFF161616) // Fallback dark gray
    
    val random = kotlin.random.Random(movie.id)
    val fallbackFormat = "2D"
    val fallbackScreen = random.nextInt(1, 5).toString().padStart(2, '0')
    val fallbackTime = run {
        val hour = random.nextInt(10, 24).toString().padStart(2, '0')
        val min = listOf("00", "15", "30", "45")[random.nextInt(4)]
        "$hour:$min"
    }
    val fallbackSeat = run {
        val alphabet = ('A'..'Z').elementAt(random.nextInt(26))
        val seatNum = random.nextInt(1, 27).toString().padStart(2, '0')
        "$alphabet$seatNum"
    }
    val heightDp = (180 * sizeMultiplier).dp
    
    Box(
        modifier = modifier
            .height(heightDp)
            .clip(ticketShape)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // MAIN TICKET BODY (LEFT)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(mainBgColor)
            ) {
                if (backgroundColor == null) {
                    // Show poster as background
                    AsyncImage(
                        model = movie.backdropUrl ?: movie.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Dark scrim for readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    )
                }
                
                // Content
                val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
                val shimmerAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.1f, targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000), 
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "shimmer"
                )
                val shimmerColor = Color.White.copy(alpha = shimmerAlpha)

                Column(
                    modifier = Modifier.fillMaxSize().padding((16 * sizeMultiplier).dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Row: Logo & Format
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(0.6f).height((36 * sizeMultiplier).dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (movie.fullLogoUrl != null) {
                                AsyncImage(
                                    model = movie.fullLogoUrl,
                                    contentDescription = movie.title,
                                    modifier = Modifier.fillMaxSize(),
                                    alignment = Alignment.TopStart,
                                    contentScale = ContentScale.Fit
                                )
                            } else if (movie.logoUrl == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .fillMaxHeight(0.8f)
                                        .background(shimmerColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                )
                            } else {
                                Text(
                                    text = movie.title.uppercase(),
                                    color = contentColor,
                                    fontSize = (32 * sizeMultiplier).sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BebasNeue,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        // Right Side Top (CBFC Age Rating)
                        if (movie.certification == null) {
                            Box(
                                modifier = Modifier
                                    .size((36 * sizeMultiplier).dp)
                                    .background(shimmerColor, androidx.compose.foundation.shape.CircleShape)
                            )
                        } else if (movie.certification.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .border(1.dp, contentColor, androidx.compose.foundation.shape.CircleShape)
                                    .size((36 * sizeMultiplier).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val cert = movie.certification
                                if (cert.startsWith("U/A") && cert.length > 3) {
                                    val numberPart = cert.substring(3).trim()
                                    if (numberPart.isNotEmpty()) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "U/A",
                                                color = contentColor,
                                                fontSize = (11 * sizeMultiplier).sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = (11 * sizeMultiplier).sp
                                            )
                                            Text(
                                                text = numberPart,
                                                color = contentColor,
                                                fontSize = (8 * sizeMultiplier).sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = (8 * sizeMultiplier).sp
                                            )
                                        }
                                    } else {
                                        Text(text = cert, color = contentColor, fontSize = (12 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        text = cert,
                                        color = contentColor,
                                        fontSize = if (cert.length > 3) (10 * sizeMultiplier).sp else (12 * sizeMultiplier).sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            } }
                    }
                    
                    // Bottom Row: Organized Info (Left) and Seat (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Stacked Info: Format, Screen, Time above Date
                        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(end = (24 * sizeMultiplier).dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy((16 * sizeMultiplier).dp)) {
                                // FORMAT
                                Column(verticalArrangement = Arrangement.spacedBy((-8 * sizeMultiplier).dp)) {
                                    Text(text = "PROJECTION", color = contentColor.copy(alpha = 0.5f), fontSize = (8 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                                    Text(text = movie.ticketFormat.ifBlank { fallbackFormat }.uppercase(), color = contentColor, fontSize = (12 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                                }
                                // SCREEN
                                Column(verticalArrangement = Arrangement.spacedBy((-8 * sizeMultiplier).dp)) {
                                    Text(text = "SCREEN", color = contentColor.copy(alpha = 0.5f), fontSize = (8 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                                    Text(text = movie.ticketScreenNo.ifBlank { fallbackScreen }, color = contentColor, fontSize = (12 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                                }
                                // TIME
                                Column(verticalArrangement = Arrangement.spacedBy((-8 * sizeMultiplier).dp)) {
                                    Text(text = "TIME", color = contentColor.copy(alpha = 0.5f), fontSize = (8 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                                    Text(text = movie.ticketTime.ifBlank { fallbackTime }, color = contentColor, fontSize = (12 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // DATE
                            Column(verticalArrangement = Arrangement.spacedBy((-8 * sizeMultiplier).dp)) {
                                Text(text = "DATE", color = contentColor.copy(alpha = 0.5f), fontSize = (8 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                                Text(text = formatTicketDate(movie.ticketDate.ifBlank { movie.releaseDate.ifBlank { "2026-03-26" } }), color = contentColor, fontSize = (12 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        // Seat Name (most right)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((-8 * sizeMultiplier).dp)
                        ) {
                            Text("SEAT", color = contentColor.copy(alpha = 0.5f), fontSize = (8 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = movie.ticketSeat.ifBlank { fallbackSeat },
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                lineHeight = 26.sp
                            )
                        }
                    }
                }
            }
            
            // PERFORATION LINE (Dashed)
            Canvas(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(contentColor) // Background to match stub
            ) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.2f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = (1 * sizeMultiplier).dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }
            
            // STUB (RIGHT)
            Box(
                modifier = Modifier
                    .width(stubWidthDp - (1 * sizeMultiplier).dp) // Subtract 1dp for the perforation line
                    .fillMaxHeight()
                    .background(contentColor)
                    .padding(top = (12 * sizeMultiplier).dp, bottom = (12 * sizeMultiplier).dp, start = (8 * sizeMultiplier).dp, end = (4 * sizeMultiplier).dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cinema Logo
                    if (!cinemaLogoUrl.isNullOrBlank()) {
                        Image(
                            painter = coil3.compose.rememberAsyncImagePainter(
                                model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(cinemaLogoUrl)
                                    .build()
                            ),
                            contentDescription = "Cinema Logo",
                            modifier = Modifier
                                .graphicsLayer { rotationZ = -90f }
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(
                                        constraints.copy(
                                            minWidth = constraints.minHeight,
                                            maxWidth = Int.MAX_VALUE,
                                            minHeight = constraints.minWidth,
                                            maxHeight = constraints.maxWidth
                                        )
                                    )
                                    layout(placeable.height, placeable.width) {
                                        placeable.placeRelative(
                                            -(placeable.width / 2 - placeable.height / 2),
                                            -(placeable.height / 2 - placeable.width / 2)
                                        )
                                    }
                                }
                                .height((24 * sizeMultiplier).dp),
                            contentScale = ContentScale.Fit
                        )
                        
                        Spacer(modifier = Modifier.width((0 * sizeMultiplier).dp))
                    }

                    // Rotated text
                    Text(
                        text = "ADMISSION NO. ${movie.id.toString().padStart(6, '0')}",
                        color = Color.Black,
                        fontSize = (10 * sizeMultiplier).sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (1 * sizeMultiplier).sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .graphicsLayer { rotationZ = -90f }
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    constraints.copy(
                                        minWidth = constraints.minHeight,
                                        maxWidth = Int.MAX_VALUE, // Remove constraint to prevent squishing the logo
                                        minHeight = constraints.minWidth,
                                        maxHeight = constraints.maxWidth
                                    )
                                )
                                layout(placeable.height, placeable.width) {
                                    placeable.placeRelative(
                                        -(placeable.width / 2 - placeable.height / 2),
                                        -(placeable.height / 2 - placeable.width / 2)
                                    )
                                }
                            }
                    )
                    
                    // Barcode
                    val barcodePattern = remember(movie.id) {
                        val r = kotlin.random.Random(movie.id * 31)
                        val pattern = mutableListOf<Pair<Float, Boolean>>()
                        
                        // Start guard
                        pattern.add(2f to true)
                        pattern.add(1f to false)
                        pattern.add(2f to true)
                        
                        var currentIsBlack = false
                        
                        // Generate a high number of segments for a dense, thin, realistic barcode
                        for (i in 0 until 90) {
                            // Realistic barcodes often have widths of 1, 2, or 3 modules (thinner)
                            val width = r.nextInt(1, 4).toFloat()
                            pattern.add(width to currentIsBlack)
                            currentIsBlack = !currentIsBlack // alternate color
                        }
                        
                        // End guard
                        if (!currentIsBlack) {
                            pattern.add(1f to false)
                        }
                        pattern.add(2f to true)
                        pattern.add(1f to false)
                        pattern.add(2f to true)
                        
                        pattern
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(42.dp) // Slightly taller than original 36dp
                            .offset(x = (-4).dp)
                    ) {
                        val totalUnits = barcodePattern.fold(0f) { acc, pair -> acc + pair.first }
                        val unitHeight = size.height / totalUnits
                        var currentY = 0f
                        
                        for ((w, isBlack) in barcodePattern) {
                            val h = w * unitHeight
                            if (isBlack) {
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(0f, currentY),
                                    size = androidx.compose.ui.geometry.Size(size.width, h)
                                )
                            }
                            currentY += h
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PillBox(text: String, color: Color) {
    Box(
        modifier = Modifier
            .border(1.dp, color, androidx.compose.foundation.shape.RoundedCornerShape(50))
            .padding(horizontal = (12).dp, vertical = (6).dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = (10).sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (1).sp
        )
    }
}

class HorizontalTicketShape(
    private val stubWidthDp: androidx.compose.ui.unit.Dp,
    private val cornerCutDp: androidx.compose.ui.unit.Dp,
    private val notchRadiusDp: androidx.compose.ui.unit.Dp
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val stubWidth = with(density) { stubWidthDp.toPx() }
        val cornerCut = with(density) { cornerCutDp.toPx() }
        val notchRadius = with(density) { notchRadiusDp.toPx() }
        val width = size.width
        val height = size.height
        val dividerX = width - stubWidth
        
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cornerCut, 0f)
            lineTo(dividerX - notchRadius * 0.65f, 0f)
            lineTo(dividerX, notchRadius * 1.5f)
            lineTo(dividerX + notchRadius * 0.65f, 0f)
            lineTo(width - cornerCut, 0f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(width - cornerCut, -cornerCut, width + cornerCut, cornerCut),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            lineTo(width, height - cornerCut)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(width - cornerCut, height - cornerCut, width + cornerCut, height + cornerCut),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            lineTo(dividerX + notchRadius * 0.65f, height)
            lineTo(dividerX, height - notchRadius * 1.5f)
            lineTo(dividerX - notchRadius * 0.65f, height)
            lineTo(cornerCut, height)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(-cornerCut, height - cornerCut, cornerCut, height + cornerCut),
                startAngleDegrees = 0f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            lineTo(0f, cornerCut)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(-cornerCut, -cornerCut, cornerCut, cornerCut),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

fun formatTicketDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val year = parts[0].takeLast(2)
            val monthIndex = parts[1].toIntOrNull() ?: 1
            val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
            val month = months.getOrNull(monthIndex - 1) ?: "JAN"
            val day = parts[2]
            "$day $month $year"
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}


class VerticalTicketShape(
    private val stubHeightDp: Dp,
    private val cornerCutDp: Dp = 12.dp,
    private val topNotchRadiusDp: Dp = 12.dp,
    private val sideNotchRadiusDp: Dp = 8.dp
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val stubHeight = stubHeightDp.value * density.density
        val cornerCut = cornerCutDp.value * density.density
        val topNotchRadius = topNotchRadiusDp.value * density.density
        val sideNotchRadius = sideNotchRadiusDp.value * density.density
        val width = size.width
        val height = size.height
        val dividerY = height - stubHeight
        
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(topNotchRadius, 0f)
            lineTo((width / 2) - topNotchRadius, 0f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect((width / 2) - topNotchRadius, -topNotchRadius, (width / 2) + topNotchRadius, topNotchRadius),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(width - topNotchRadius, 0f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(width - topNotchRadius, -topNotchRadius, width + topNotchRadius, topNotchRadius),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            lineTo(width, dividerY - sideNotchRadius)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(width - sideNotchRadius, dividerY - sideNotchRadius, width + sideNotchRadius, dividerY + sideNotchRadius),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(width, height - cornerCut)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(width - cornerCut * 2, height - cornerCut * 2, width, height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(cornerCut, height)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(0f, height - cornerCut * 2, cornerCut * 2, height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(0f, dividerY + sideNotchRadius)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(-sideNotchRadius, dividerY - sideNotchRadius, sideNotchRadius, dividerY + sideNotchRadius),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(0f, topNotchRadius)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(-topNotchRadius, -topNotchRadius, topNotchRadius, topNotchRadius),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VerticalTicketItem(
    movie: Movie,
    cinemaLogoUrl: String? = null,
    backgroundColor: Color? = null,
    ticketSize: TicketSize = TicketSize.LARGE,
    qrImageUri: android.net.Uri? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sizeMultiplier = when (ticketSize) {
        TicketSize.SMALL -> 0.7f
        TicketSize.MEDIUM -> 0.85f
        TicketSize.LARGE -> 1f
    }
    val horizontalPaddingDp = when (ticketSize) {
        TicketSize.SMALL -> 48.dp
        TicketSize.MEDIUM -> 24.dp
        TicketSize.LARGE -> 0.dp
    }
    val stubHeightDp = (190 * sizeMultiplier).dp
    
    val random = kotlin.random.Random(movie.id)
    val fallbackFormat = "2D"
    val fallbackScreen = random.nextInt(1, 5).toString().padStart(2, '0')
    val fallbackTime = run {
        val hour = random.nextInt(10, 24).toString().padStart(2, '0')
        val min = listOf("00", "15", "30", "45")[random.nextInt(4)]
        "$hour:$min"
    }
    val fallbackSeat = run {
        val alphabet = ('A'..'Z').elementAt(random.nextInt(26))
        val seatNum = random.nextInt(1, 27).toString().padStart(2, '0')
        "$alphabet$seatNum"
    }

    val ticketShape = remember(sizeMultiplier) {
        VerticalTicketShape(
            stubHeightDp = stubHeightDp,
            cornerCutDp = (16 * sizeMultiplier).dp,
            topNotchRadiusDp = (16 * sizeMultiplier).dp,
            sideNotchRadiusDp = (8 * sizeMultiplier).dp
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPaddingDp)
            .wrapContentHeight()
            .shadow(
                elevation = 8.dp,
                shape = ticketShape,
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(ticketShape)
            .combinedClickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(Color.White)
            .drawBehind {
                val stubHeightPx = stubHeightDp.toPx()
                val topHeight = size.height - stubHeightPx
                if (backgroundColor != null && backgroundColor != Color.White) {
                    drawRect(
                        color = backgroundColor,
                        topLeft = androidx.compose.ui.geometry.Offset.Zero,
                        size = androidx.compose.ui.geometry.Size(size.width, topHeight)
                    )
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = (28 * sizeMultiplier).dp, bottom = (16 * sizeMultiplier).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = movie.title.uppercase(),
                    color = Color.Black,
                    fontSize = (26 * sizeMultiplier).sp, // slightly increased for Bebas Neue
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = BebasNeue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (1.5 * sizeMultiplier).sp,
                    modifier = Modifier.padding(horizontal = (24 * sizeMultiplier).dp)
                )
                Spacer(modifier = Modifier.height((4 * sizeMultiplier).dp))
                val rawLang = movie.ticketLanguage.ifBlank { movie.originalLanguage.ifBlank { "hi" } }
                val langName = if (rawLang.length == 2) java.util.Locale(rawLang).displayLanguage else rawLang
                val lang = langName.replaceFirstChar { it.titlecase() }
                val format = movie.ticketFormat.ifBlank { fallbackFormat }.uppercase()
                val cert = if (movie.certification.isNullOrBlank()) "U/A" else movie.certification.uppercase()
                val subtitle = "$lang | $format | $cert"
                Text(
                    text = subtitle,
                    color = Color.Black.copy(alpha = 0.7f),
                    fontSize = (12 * sizeMultiplier).sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Image Backdrop
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                AsyncImage(
                    model = movie.backdropUrl ?: movie.posterUrl ?: movie.logoUrl,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Details Row (moved from header)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((80 * sizeMultiplier).dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = (24 * sizeMultiplier).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val contentColor = Color.Black
                    
                    // DATE
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((-4 * sizeMultiplier).dp)) {
                        Text(text = "DATE", color = contentColor.copy(alpha = 0.5f), fontSize = (8 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                        Text(text = formatTicketDate(movie.ticketDate.ifBlank { movie.releaseDate.ifBlank { "2026-03-26" } }), color = contentColor, fontSize = (12 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                    }
                    // SCREEN
                       Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((-4 * sizeMultiplier).dp)) {
                        Text(text = "SCREEN", color = contentColor.copy(alpha = 0.5f), fontSize = (8 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                        Text(text = movie.ticketScreenNo.ifBlank { fallbackScreen }, color = contentColor, fontSize = (12 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                    }
                    // TIME
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((-4 * sizeMultiplier).dp)) {
                        Text(text = "TIME", color = contentColor.copy(alpha = 0.5f), fontSize = (8 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                        Text(text = movie.ticketTime.ifBlank { fallbackTime }, color = contentColor, fontSize = (12 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                    }
                    // SEAT
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((-4 * sizeMultiplier).dp)) {
                        Text(text = "SEAT", color = contentColor.copy(alpha = 0.5f), fontSize = (8 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                        Text(text = movie.ticketSeat.ifBlank { fallbackSeat }, color = contentColor, fontSize = (12 * sizeMultiplier).sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Ticket Disclaimer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (24 * sizeMultiplier).dp)
                    .padding(bottom = (16 * sizeMultiplier).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Present this ticket at the cinema entrance. This ticket is non-transferable and non-refundable. Please arrive 15 minutes before showtime.",
                    color = Color.Black.copy(alpha = 0.5f),
                    fontSize = (9 * sizeMultiplier).sp,
                    lineHeight = (12 * sizeMultiplier).sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            
            // Bottom Stub (Admission No)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stubHeightDp)
                    // Offset padding to compensate for font descent and corner cuts, making it visually perfectly centered
                    .padding(top = (20 * sizeMultiplier).dp, bottom = (12 * sizeMultiplier).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (!cinemaLogoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = cinemaLogoUrl,
                                error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_dialog_alert),
                                placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                        contentDescription = "Cinema Logo",
                        modifier = Modifier
                            .height((40 * sizeMultiplier).dp)
                            .widthIn(min = (40 * sizeMultiplier).dp, max = (140 * sizeMultiplier).dp)
                            .padding(bottom = (8 * sizeMultiplier).dp),
                        contentScale = ContentScale.Fit
                    )
                }

                if (qrImageUri != null) {
                    coil3.compose.AsyncImage(
                        model = qrImageUri,
                        contentDescription = "Ticket QR Code",
                        modifier = Modifier
                            .size((100 * sizeMultiplier).dp)
                            .background(Color.White)
                            .padding((4 * sizeMultiplier).dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Generated placeholder if no QR provided
                    Canvas(
                        modifier = Modifier
                            .size((90 * sizeMultiplier).dp)
                            .background(Color.White)
                            .padding((8 * sizeMultiplier).dp)
                    ) {
                        val gridSize = 21
                        val cellSize = size.width / gridSize
                        val r = kotlin.random.Random(movie.id * 7)

                        fun drawFinder(startX: Int, startY: Int) {
                            drawRect(Color.Black, androidx.compose.ui.geometry.Offset(startX * cellSize, startY * cellSize), androidx.compose.ui.geometry.Size(7 * cellSize, 7 * cellSize))
                            drawRect(Color.White, androidx.compose.ui.geometry.Offset((startX + 1) * cellSize, (startY + 1) * cellSize), androidx.compose.ui.geometry.Size(5 * cellSize, 5 * cellSize))
                            drawRect(Color.Black, androidx.compose.ui.geometry.Offset((startX + 2) * cellSize, (startY + 2) * cellSize), androidx.compose.ui.geometry.Size(3 * cellSize, 3 * cellSize))
                        }

                        for (x in 0 until gridSize) {
                            for (y in 0 until gridSize) {
                                val isTopLeft = x < 8 && y < 8
                                val isTopRight = x > gridSize - 9 && y < 8
                                val isBottomLeft = x < 8 && y > gridSize - 9
                                if (!isTopLeft && !isTopRight && !isBottomLeft) {
                                    if (r.nextBoolean()) {
                                        drawRect(Color.Black, androidx.compose.ui.geometry.Offset(x * cellSize, y * cellSize), androidx.compose.ui.geometry.Size(cellSize, cellSize))
                                    }
                                }
                            }
                        }
                        drawFinder(0, 0)
                        drawFinder(gridSize - 7, 0)
                        drawFinder(0, gridSize - 7)
                    }
                }

                val pnr = remember(movie.id) {
                    val r = kotlin.random.Random(movie.id)
                    val s = StringBuilder()
                    for (i in 0 until 10) s.append(r.nextInt(0, 10))
                    s.toString()
                }
                Text(
                    text = pnr,
                    color = Color.Black,
                    fontSize = (16 * sizeMultiplier).sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (6 * sizeMultiplier).sp,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = (16 * sizeMultiplier).dp)
                )
            }
        }
        
        // Dashed perforation line
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .offset(y = -stubHeightDp)
        ) {
            val notchRadius = (8 * sizeMultiplier).dp.toPx()
            drawLine(
                color = Color.LightGray,
                start = androidx.compose.ui.geometry.Offset(notchRadius + 10f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width - notchRadius - 10f, 0f),
                strokeWidth = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
            )
        }
    }
}



