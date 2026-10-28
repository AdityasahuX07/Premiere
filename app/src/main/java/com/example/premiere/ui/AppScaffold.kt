package com.example.premiere.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import com.example.premiere.domain.model.Movie
import com.example.premiere.ui.detail.DetailScreen
import com.example.premiere.ui.home.HomeScreen
import com.example.premiere.ui.search.SearchOverlay
import com.example.premiere.ui.search.SearchViewModel
import com.example.premiere.ui.settings.SettingsPanel
import com.example.premiere.ui.history.HistoryScreen
import com.example.premiere.ui.history.HistoryViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import com.example.premiere.ui.settings.SettingsDestination

// Navigation destinations
private sealed class Screen {
    object Home : Screen()
    data class Detail(val movieId: Int) : Screen()
    data class EditTicket(val movieId: Int) : Screen()
}

private val ScreenSaver = Saver<Screen, List<Any>>(
    save = { screen ->
        when (screen) {
            is Screen.Home -> listOf("Home")
            is Screen.Detail -> listOf("Detail", screen.movieId)
            is Screen.EditTicket -> listOf("EditTicket", screen.movieId)
        }
    },
    restore = { list ->
        when (list[0] as String) {
            "Home" -> Screen.Home
            "Detail" -> Screen.Detail(list[1] as Int)
            "EditTicket" -> Screen.EditTicket(list[1] as Int)
            else -> Screen.Home
        }
    }
)

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
    initialShowTickets: Boolean = false,
    onInitialTicketsConsumed: () -> Unit = {}
) {
    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Home) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var isSearchForHistory by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var settingsInitialDestination by remember { mutableStateOf(SettingsDestination.Main) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var lastSwipeAt by remember { mutableLongStateOf(0L) }
    // Compose History/Search/Settings shortly after launch (off the startup path) so first open is instant
    var warm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        warm = true
    }

    LaunchedEffect(initialShowTickets) {
        if (initialShowTickets) {
            showSettings = true
            settingsInitialDestination = SettingsDestination.AddedTickets
            currentScreen = Screen.Home
            showSearch = false
            showHistory = false
            onInitialTicketsConsumed()
        }
    }

    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(searchState.results) {
        searchState.results.take(15).forEach { movie ->
            movie.posterUrl?.let { url ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .build()
                context.imageLoader.enqueue(request)
            }
        }
    }

    BackHandler(enabled = showSettings || showSearch || showHistory || currentScreen !is Screen.Home) {
        when {
            showSettings -> showSettings = false
            showSearch -> {
                showSearch = false
                isSearchForHistory = false
                searchViewModel.forceShowReleased = false
                searchViewModel.loadUpcoming()
            }
            currentScreen is Screen.EditTicket -> {
                currentScreen = Screen.Home
                showHistory = true
            }
            showHistory -> showHistory = false
            currentScreen !is Screen.Home -> currentScreen = Screen.Home
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onHorizontalSwipe(
                key = Triple(currentScreen, showSearch, showHistory),
                enabled = currentScreen is Screen.Home && !showSearch && !showSettings,
                onIntent = { lastSwipeAt = android.os.SystemClock.uptimeMillis() }
            ) { dx ->
                lastSwipeAt = android.os.SystemClock.uptimeMillis()
                if (showHistory) {
                    if (dx < 0) showHistory = false
                } else if (dx < 0) {
                    showSettings = true
                } else {
                    showHistory = true
                }
            }
    ) {
        // ── Home Screen (always underneath) ──────────────────────────────────
        val searchShown = showSearch && currentScreen is Screen.Home
        val searchProgress by animateFloatAsState(
            targetValue = if (searchShown) 1f else 0f,
            animationSpec = if (searchShown) spring(dampingRatio = 0.86f, stiffness = 420f) else tween(160, easing = FastOutSlowInEasing),
            label = "searchProgress"
        )
        val blurModifier = Modifier.graphicsLayer {
            val r = 24.dp.toPx() * searchProgress.coerceIn(0f, 1f)
            renderEffect = if (r > 0.5f) {
                androidx.compose.ui.graphics.BlurEffect(r, r, androidx.compose.ui.graphics.TileMode.Decal)
            } else null
        }
        HomeScreen(
            onMovieTap = { movie ->
                showSearch = false
                currentScreen = Screen.Detail(movie.id)
            },
            onSearchRequested = { 
                // Never open search while History/Settings are up or right after a horizontal swipe
                if (showHistory || showSettings || android.os.SystemClock.uptimeMillis() - lastSwipeAt < 800) return@HomeScreen
                searchViewModel.source = "home"
                searchViewModel.forceShowReleased = false
                searchViewModel.loadUpcoming()
                showSearch = true 
            },
            modifier = Modifier.fillMaxSize().then(blurModifier)
        )

        // ── Detail Screen ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = currentScreen is Screen.Detail,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            label = "detail"
        ) {
            val screen = currentScreen
            if (screen is Screen.Detail) {
                DetailScreen(
                    movieId = screen.movieId,
                    onBack = { currentScreen = Screen.Home },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ── History Screen ───────────────────────────────────────────────────
        val historyProgress by animateFloatAsState(
            targetValue = if (showHistory) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            label = "historyProgress"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = -(1f - historyProgress) * size.width }
                // Hit target so touches on empty areas don't fall through to Home (pull-down opens search there)
                .pointerInput(showHistory) {
                    if (showHistory) awaitPointerEventScope { while (true) awaitPointerEvent() }
                }
        ) {
            if (warm || showHistory) HistoryScreen(
                onAddClick = {
                    searchViewModel.source = "history"
                    searchViewModel.forceShowReleased = true
                    searchViewModel.loadUpcoming()
                    isSearchForHistory = true
                    showSearch = true
                },
                onMovieTap = { movie ->
                    currentScreen = Screen.EditTicket(movie.id)
                },
                modifier = Modifier.fillMaxSize().then(blurModifier)
            )
        }

        // ── Search Overlay ───────────────────────────────────────────────────


        if (warm || searchShown) Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (searchProgress <= 0.001f && !searchShown) {
                        // Fully hidden: park off-screen so it can't receive touches
                        translationY = -size.height * 2f
                        alpha = 0f
                    } else {
                        val p = searchProgress
                        alpha = p.coerceIn(0f, 1f)
                        translationY = -(1f - p) * size.height / 14f
                        val sc = 0.94f + 0.06f * p
                        scaleX = sc
                        scaleY = sc
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                    }
                }
        ) {
            SearchOverlay(
                visible = searchShown,
                viewModel = searchViewModel,
                onDismiss = { 
                    showSearch = false 
                    isSearchForHistory = false
                    searchViewModel.forceShowReleased = false
                    searchViewModel.loadUpcoming()
                },
                onMovieTap = { movie ->
                    showSearch = false
                    if (isSearchForHistory) {
                        historyViewModel.addToHistory(movie)
                        isSearchForHistory = false
                        searchViewModel.forceShowReleased = false
                        searchViewModel.loadUpcoming()
                    } else {
                        currentScreen = Screen.Detail(movie.id)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Edit Ticket Screen ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = currentScreen is Screen.EditTicket,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            label = "editTicket"
        ) {
            val screen = currentScreen
            if (screen is Screen.EditTicket) {
                com.example.premiere.ui.history.EditTicketScreen(
                    movieId = screen.movieId,
                    onDismiss = { 
                        currentScreen = Screen.Home
                        showHistory = true 
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ── Settings Panel ───────────────────────────────────────────────────
        val settingsProgress by animateFloatAsState(
            targetValue = if (showSettings) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            label = "settingsProgress"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = (1f - settingsProgress) * size.width }
        ) {
            if (warm || showSettings) SettingsPanel(
                visible = showSettings,
                onDismiss = { showSettings = false },
                initialDestination = settingsInitialDestination,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
