package com.example.premiere.ui.search

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.premiere.domain.model.Movie
import com.example.premiere.theme.*
import com.example.premiere.ui.components.GlassPill
import com.example.premiere.ui.components.formatReleaseDate

@Composable
fun SearchOverlay(
    onDismiss: () -> Unit,
    visible: Boolean = true,
    onMovieTap: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = androidx.compose.ui.platform.LocalView.current
    val hideKeyboard = {
        val imm = view.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Overlay stays composed while hidden; only load/focus when it is actually shown
    LaunchedEffect(visible) {
        if (visible) {
            viewModel.loadUpcoming()
            // Let the entrance animation start smoothly before the keyboard rises
            kotlinx.coroutines.delay(120)
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    // Full-screen overlay
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        hideKeyboard()
                        onDismiss()
                    }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Main)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            var dragAmount = 0f
                            do {
                                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                val change = event.changes.firstOrNull()
                                if (change != null && change.pressed) {
                                    dragAmount += (change.position.y - change.previousPosition.y)
                                    if (dragAmount > 20f) {
                                        change.consume()
                                        hideKeyboard()
                                        onDismiss()
                                        break
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                Spacer(Modifier.height(16.dp))

                // Search bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    SearchBar(
                        query = state.query,
                        onQueryChanged = viewModel::onQueryChanged,
                        focusRequester = focusRequester,
                        onSearchAction = { viewModel.saveCurrentQuery() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            // Results
            when {
                state.query.isBlank() && state.searchHistory.isNotEmpty() -> SearchHistoryList(
                    history = state.searchHistory,
                    onHistoryTap = {
                        viewModel.onQueryChanged(it)
                        viewModel.saveCurrentQuery()
                    },
                    onRemoveHistory = { viewModel.removeSearchHistory(it) }
                )
                state.showNoApiKey -> NoApiKeyMessage()
                state.isLoading && state.results.isEmpty() -> SearchSkeletonGrid()
                state.error != null && state.results.isEmpty() -> ErrorMessage(state.error!!)
                state.results.isEmpty() && state.query.isNotBlank() && !state.isLoading -> EmptyResults()
                state.results.isNotEmpty() -> SearchResults(
                    movies = state.results,
                    onMovieTap = {
                        viewModel.saveCurrentQuery()
                        hideKeyboard()
                        onMovieTap(it)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    focusRequester: FocusRequester,
    onSearchAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val isImeVisible = WindowInsets.isImeVisible
    val shape = androidx.compose.foundation.shape.CircleShape
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    var textFieldValue by remember { 
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(text = query, selection = androidx.compose.ui.text.TextRange(query.length))) 
    }

    LaunchedEffect(query) {
        if (query != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = query, selection = androidx.compose.ui.text.TextRange(query.length))
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(SurfaceGlass2)
            .border(1.dp, GlassBorder, shape)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. Placeholder and Search Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
            if (query.isEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "Search",
                    style = MaterialTheme.typography.bodyLarge.copy(color = TextDisabled)
                )
            }
        }
        
        // 2. Input Field and Clear Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(28.dp)) // Offset for the search icon
            
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                factory = { context ->
                    android.widget.EditText(context).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setSingleLine()
                        imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                        setSelectAllOnFocus(true)
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 16f
                        // Remove default padding of EditText to align with Compose Box
                        setPadding(0, 0, 0, 0)
                        
                        post {
                            requestFocus()
                            val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                            imm.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                        }
                        
                        addTextChangedListener(
                            object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                    if (s.toString() != query) {
                                        onQueryChanged(s.toString())
                                    }
                                }
                                override fun afterTextChanged(s: android.text.Editable?) {}
                            }
                        )
                        
                        setOnEditorActionListener { _, actionId, _ ->
                            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onSearchAction()
                                true
                            } else false
                        }
                    }
                },
                update = { editText ->
                    if (editText.text.toString() != query) {
                        val selectionStart = editText.selectionStart
                        val selectionEnd = editText.selectionEnd
                        editText.setText(query)
                        if (selectionStart in 0..query.length && selectionEnd in 0..query.length) {
                            editText.setSelection(selectionStart, selectionEnd)
                        } else {
                            editText.setSelection(query.length)
                        }
                    }
                }
            )
            
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Rounded.Clear,
                    contentDescription = "Clear",
                    tint = TextTertiary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onQueryChanged("") }
                )
            }
        }
    }
}

@Composable
private fun SearchResults(movies: List<Movie>, onMovieTap: (Movie) -> Unit) {
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    if (gridState.isScrollInProgress) {
        LaunchedEffect(gridState.isScrollInProgress) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(movies, key = { it.id }) { movie ->
            SearchResultGridItem(
                movie = movie, 
                onClick = { onMovieTap(movie) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun SearchResultGridItem(movie: Movie, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .clip(shape)
                .background(SurfaceGlass)
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(8.dp))
        
        Text(
            text = movie.title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun NoApiKeyMessage() {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔑", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "TMDB API Key Required",
                style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Swipe from the right edge to open Settings and add your TMDB API key.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SearchSkeletonGrid() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false
    ) {
        items(15) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Poster Skeleton
                Box(
                    modifier = Modifier
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = shimmerAlpha))
                )
                Spacer(Modifier.height(8.dp))
                // Title Skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = shimmerAlpha))
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium.copy(color = AccentRed))
    }
}

@Composable
private fun EmptyResults() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            "No movies found",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary)
        )
    }
}

@Composable
private fun SearchHistoryList(
    history: List<String>, 
    onHistoryTap: (String) -> Unit,
    onRemoveHistory: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            count = history.size,
            key = { history[it] }
        ) { index ->
            val query = history[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHistoryTap(query) }
                    .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemoveHistory(query) }) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Remove",
                        tint = TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
