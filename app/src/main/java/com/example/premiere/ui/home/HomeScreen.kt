package com.example.premiere.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.premiere.domain.model.Movie
import com.example.premiere.theme.*
import com.example.premiere.ui.components.MovieCard
import java.time.LocalDate

// Metallic gradient: reverse-engineered from reference (white top rim, dark upper, bright bottom)
private val MetallicDateBrush = Brush.verticalGradient(
    0.0f to Color(0xFFFFFFFF), // Bright top rim
    0.08f to Color(0xFF777777), // Dark upper shadow (reduced and pushed up)
    0.60f to Color(0xFFCCCCCC), // Light grey mid
    1.0f to Color(0xFFFFFFFF)  // Bright bottom rim
)

@Composable
fun HomeScreen(
    onMovieTap: (Movie) -> Unit,
    onSearchRequested: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 20f) {
                    onSearchRequested()
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinematicBlack)
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                // Pull-down opens search only for a clearly vertical drag; diagonal/horizontal swipes
                // (used to switch History/Settings) must not trigger it.
                val triggerPx = 28.dp.toPx()
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var total = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed || event.changes.size > 1) break
                        total += change.positionChange()
                        if (kotlin.math.abs(total.x) > triggerPx / 3f && kotlin.math.abs(total.x) > kotlin.math.abs(total.y) * 0.5f) break
                        if (total.y > triggerPx && total.y > kotlin.math.abs(total.x) * 2.5f) {
                            onSearchRequested()
                            break
                        }
                    }
                }
            }
    ) {
        if (state.isLoading) {
            // No loading skeleton, show blank background
        } else if (state.movies.isEmpty()) {
            EmptyHomeState()
        } else {
            // Sort by release date — earliest first (front of stack)
            val sorted = remember(state.movies) {
                state.movies.sortedBy { movie ->
                    try { LocalDate.parse(movie.releaseDate) } catch (_: Exception) { LocalDate.MAX }
                }
            }
            ReminderStack(movies = sorted, onMovieTap = onMovieTap, onSearchRequested = onSearchRequested)
        }

    }
}

/**
 * All reminder movies stacked into ONE group.
 * - Earliest movie = front card, full size, no overlay
 * - Back cards: progressively thinner visible sliver AND darker overlay the deeper they go
 */
@Composable
private fun ReminderStack(movies: List<Movie>, onMovieTap: (Movie) -> Unit, onSearchRequested: () -> Unit) {
    val frontMovie = movies.first()
    val peekCount = minOf(movies.size - 1, 4)
    val scaleStep = 0.05f

    var isExpanded by remember { mutableStateOf(false) }
    val canExpand = movies.size > 1
    
    var activePointersCount by remember { mutableIntStateOf(0) }
    var accumulatedSwipe by remember { mutableFloatStateOf(0f) }

    val posterNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If the user is touching with 2 fingers, they are pinching! DO NOT trigger search.
                if (activePointersCount >= 2) {
                    return available // Consume overscroll so parent doesn't get it
                }
                
                // If it's 1 finger, accumulate the drag and trigger search if it's explicitly a downward swipe > 100px
                if (available.y > 0) {
                    if (!isExpanded) {
                        accumulatedSwipe += available.y
                        if (accumulatedSwipe > 40f) {
                            onSearchRequested()
                            accumulatedSwipe = 0f
                        }
                    }
                    return Offset(0f, available.y) // Consume so global nestedScroll doesn't get it (which triggers at 20px)
                }
                return Offset.Zero
            }
        }
    }

    fun peekH(i: Int): Float = (14f - (i - 1) * 2f).coerceAtLeast(4f)
    val totalPeekDp = (1..peekCount).fold(0f) { acc, i -> acc + peekH(i) }.dp
    fun cardY(i: Int): androidx.compose.ui.unit.Dp {
        val sumBehind = ((i + 1)..peekCount).fold(0f) { acc, j -> acc + peekH(j) }
        return sumBehind.dp
    }
    fun darkAlpha(i: Int): Float = (0.20f + (i - 1) * 0.18f).coerceAtMost(0.70f)

    val headerScale by animateFloatAsState(targetValue = if (isExpanded) 0.6f else 1f, label = "headerScale")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val effectiveWidth = if (maxWidth > maxHeight) minOf(maxWidth, maxHeight * 0.8f / 1.5f) else maxWidth
        val cardWidth = effectiveWidth
        val cardHeight = cardWidth * 1.5f
        
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val expandedSpacing = 24.dp
        val expandedTopPadding = expandedSpacing + topPadding
        val headerSpace = 68.dp
        val expandedHeaderSpace = 40.dp
        val expandedGap = 16.dp
        val expandedItemHeight = expandedHeaderSpace + expandedGap + cardHeight + expandedSpacing
        
        val collapsedTotalHeight = headerSpace + cardHeight + totalPeekDp
        val expandedTotalHeight = expandedTopPadding + (expandedItemHeight * movies.size)
        
        val totalHeight by animateDpAsState(
            targetValue = if (isExpanded) expandedTotalHeight else collapsedTotalHeight,
            label = "totalHeight"
        )

        val centeringOffset by animateDpAsState(
            targetValue = if (isExpanded) 0.dp else ((maxHeight - collapsedTotalHeight) / 2f).coerceAtLeast(0.dp),
            label = "centerOffset"
        )
        
        val scrollState = rememberScrollState()
        
        Box(
            modifier = Modifier
                .width(effectiveWidth)
                .offset(y = centeringOffset)
                .height(if (isExpanded) maxHeight else totalHeight)
                .nestedScroll(posterNestedScrollConnection)
                .then(
                    if (isExpanded) {
                        Modifier.verticalScroll(scrollState)
                    } else {
                        Modifier.scrollable(
                            state = rememberScrollableState { 0f },
                            orientation = Orientation.Vertical
                        )
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
                    .pointerInput(canExpand, isExpanded) {
                        if (!canExpand) return@pointerInput
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var accumulatedZoom = 1f
                            
                            do {
                                val event = awaitPointerEvent()
                                val activePointers = event.changes.filter { it.pressed }
                                activePointersCount = activePointers.size
                                
                                if (activePointers.size >= 2) {
                                    val zoomChange = event.calculateZoom()
                                    accumulatedZoom *= zoomChange
                                    
                                    if (!isExpanded && accumulatedZoom > 1.15f) {
                                        isExpanded = true
                                    } else if (isExpanded && accumulatedZoom < 0.85f) {
                                        isExpanded = false
                                    }
                                    
                                    event.changes.forEach { it.consume() }
                                }
                            } while (activePointers.isNotEmpty())
                            
                            activePointersCount = 0
                            accumulatedSwipe = 0f
                        }
                    }
            ) {
                for (i in movies.indices.reversed()) {
                    val movie = movies[i]
                    val parsed = try { LocalDate.parse(movie.releaseDate) } catch (_: Exception) { null }
                    val dayStr = parsed?.dayOfMonth?.toString() ?: ""
                    val monStr = parsed?.month?.name?.take(3) ?: ""

                    val isFront = i == 0
                    val inPeek = i <= peekCount
                    
                    val colScale = if (isFront) 1f else if (inPeek) 1f - (i * scaleStep) else 0.8f
                    val colCardY = if (isFront) headerSpace + totalPeekDp else if (inPeek) headerSpace + cardY(i) else headerSpace
                    val colAlpha = if (isFront) 0f else if (inPeek) darkAlpha(i) else 1f
                    val colOpacity = if (isFront || inPeek) 1f else 0f
                    val colHeaderY = 0.dp
                    
                    val expScale = 1f
                    val expHeaderY = expandedTopPadding + (i * expandedItemHeight.value).dp
                    val expCardY = expHeaderY + expandedHeaderSpace + expandedGap
                    val expAlpha = 0f
                    val expOpacity = 1f
                    
                    val animScale by animateFloatAsState(if (isExpanded) expScale else colScale, label = "scale")
                    val animCardY by animateDpAsState(if (isExpanded) expCardY else colCardY, label = "cY")
                    val animHeaderY by animateDpAsState(if (isExpanded) expHeaderY else colHeaderY, label = "hY")
                    val animAlpha by animateFloatAsState(if (isExpanded) expAlpha else colAlpha, label = "alpha")
                    val animOpacity by animateFloatAsState(if (isExpanded) expOpacity else colOpacity, label = "opacity")
                    val zIdx = (movies.size - i).toFloat()
                    
                    val headerAlpha by animateFloatAsState(if (isExpanded || isFront) 1f else 0f, label = "hA")
                    val headerBias by animateFloatAsState(if (isExpanded) -1f else 0f, label = "hB")
                    val originX by animateFloatAsState(if (isExpanded) 0f else 0.5f, label = "oX")

                    if (animOpacity > 0f) {
                        // Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = animHeaderY)
                                .graphicsLayer { alpha = headerAlpha }
                                .zIndex(zIdx),
                            contentAlignment = androidx.compose.ui.BiasAlignment(horizontalBias = headerBias, verticalBias = 0f)
                        ) {
                            Text(
                                text = "$dayStr $monStr",
                                style = TextStyle(
                                    brush = MetallicDateBrush,
                                    fontSize = 55.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-2).sp
                                ),
                                modifier = Modifier.graphicsLayer {
                                    scaleX = headerScale
                                    scaleY = headerScale
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(originX, 0.5f)
                                }
                            )
                        }

                        // Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cardHeight)
                                .offset(y = animCardY)
                                .zIndex(zIdx)
                                .graphicsLayer { alpha = animOpacity }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth(animScale)
                                    .fillMaxHeight()
                            ) {
                                MovieCard(
                                    movie = movie,
                                    onClick = { onMovieTap(movie) },
                                    showDateHeader = false,
                                    showPills = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (animAlpha > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = animAlpha))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun EmptyHomeState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        Text(
            text = "🎬",
            fontSize = 72.sp,
            modifier = Modifier.graphicsLayer { this.alpha = alpha }
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "No Releases Tracked",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Swipe down to search and\nadd movies to your reminders.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = TextTertiary,
                textAlign = TextAlign.Center
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 12f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "swipeIndicator"
        )
        Text(
            text = "↓",
            fontSize = 28.sp,
            color = TextTertiary,
            modifier = Modifier.graphicsLayer { translationY = offsetY }
        )
    }
}

@Composable
private fun LoadingSkeletons() {
    LazyColumn(
        contentPadding = PaddingValues(
            top = 100.dp, bottom = 24.dp, start = 20.dp, end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        userScrollEnabled = false
    ) {
        items(3) { SkeletonCard() }
    }
}

@Composable
private fun SkeletonCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer"
    )
    Column {
        Box(
            Modifier.width(80.dp).height(20.dp).padding(bottom = 8.dp)
                .background(Color.White.copy(alpha = shimmerAlpha))
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.67f)
                .background(Color.White.copy(alpha = shimmerAlpha))
        )
    }
}
