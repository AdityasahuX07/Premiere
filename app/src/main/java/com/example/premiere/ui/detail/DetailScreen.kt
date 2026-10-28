package com.example.premiere.ui.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.example.premiere.domain.model.CastMember
import com.example.premiere.domain.model.Movie
import com.example.premiere.theme.*
import com.example.premiere.ui.components.MetadataPill
import com.example.premiere.ui.components.formatReleaseDate
import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged



@Composable
fun DetailScreen(
    movieId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(movieId) {
        viewModel.initWithMovieId(movieId)
    }


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinematicBlack)
    ) {
            if (state.error != null && state.movie == null) {
                DetailErrorState(state.error!!, onRetry = viewModel::loadMovieDetail)
            } else if (!state.isLoading && state.movie != null) {
                val cinemaName = state.movie!!.ticketCinema.ifBlank { state.defaultCinema }
                val cinemaLogoUrl = state.cinemas.find { it.name == cinemaName }?.logoUrl
                DetailContent(
                    movie = state.movie!!,
                    cinemaLogoUrl = cinemaLogoUrl,
                    onBack = onBack,
                    onToggleReminder = viewModel::toggleReminder,
                    onRemoveFromApp = viewModel::removeFromApp,
                    reminderToggling = state.reminderToggling,
                    cinemas = state.cinemas,
                    defaultCinema = state.defaultCinema,
                    defaultFormat = state.defaultFormat,
                    onSaveTicket = { cinema, date, time, seat, screenNo, format, language, qrPath ->
                        viewModel.saveQuickTicket(cinema, date, time, seat, screenNo, format, language, qrPath)
                    },
                    onDeleteTicket = viewModel::deleteTicket
                )
            }
        }

        if (state.isLoading) {
            DetailLoadingState(onBack = onBack)
        }


    }


@Composable
private fun DetailContent(
    movie: Movie,
    cinemaLogoUrl: String?,
    onBack: () -> Unit,
    onToggleReminder: () -> Unit,
    onRemoveFromApp: () -> Unit,
    reminderToggling: Boolean,
    cinemas: List<com.example.premiere.domain.model.Cinema>,
    defaultCinema: String,
    defaultFormat: String,
    onSaveTicket: (cinema: String, date: String, time: String, seat: String, screenNo: String, format: String, language: String, qrPath: String?) -> Unit,
    onDeleteTicket: () -> Unit
) {
    var isEditingTicket by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred backdrop behind EVERYTHING, filling the entire screen
        if (movie.backdropUrl != null) {
            AsyncImage(
                model = movie.backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
                    .graphicsLayer { alpha = 0.3f }
            )
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {

            // Hero poster â€” padded so the card appears to float with margins
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 88.dp), // leave room for top bar
                contentAlignment = Alignment.Center
            ) {
                val posterShape = RoundedCornerShape(28.dp)

                Box(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(posterShape)
                ) {
                    // Main poster image (TopCenter aligned to crop bottom)
                    AsyncImage(
                        model = movie.highResPosterUrl ?: movie.posterUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bottom gradient for "Watch Trailer" button legibility
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.35f)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                )
                            )
                    )

                    // Watch Trailer button â€” bottom-end inside the rounded card
                    if (movie.trailerKey != null) {
                        Surface(
                            onClick = {
                                val url = "https://www.youtube.com/watch?v=${movie.trailerKey}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            shape = RoundedCornerShape(50),
                            color = Color.Black,
                            border = BorderStroke(0.5.dp, GlassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    "Watch Trailer",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Content below the poster
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = movie.title.uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(4.dp))

                val metaTexts = mutableListOf<String>()
                movie.genres.firstOrNull()?.let { metaTexts.add(it) }
                movie.formattedRuntime?.let { metaTexts.add(it) }
                if (!movie.imdbRating.isNullOrBlank() && movie.imdbRating != "N/A") {
                    metaTexts.add("IMDb ${movie.imdbRating}")
                } else if (movie.voteAverage > 0) {
                    metaTexts.add("★ ${movie.formattedRating}/10")
                }
                
                if (metaTexts.isNotEmpty()) {
                    Text(
                        text = metaTexts.joinToString(" • "),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextTertiary,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Overview with expand/collapse
                var expanded by remember { mutableStateOf(false) }
                Text(
                    text = movie.overview,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextSecondary,
                        lineHeight = 20.sp
                    ),
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                if (movie.overview.length > 180) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (expanded) "Show Less" else "Read More",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .clickable { expanded = !expanded }
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (movie.cast.isNotEmpty()) {
                    Text(
                        text = "Cast",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    CastRow(cast = movie.cast)
                    Spacer(Modifier.height(24.dp))
                }

                // Ticket section — only visible when reminder is set and not yet watched
                if (movie.hasReminder && !movie.isWatchedInTheater) {
                    Spacer(Modifier.height(4.dp))

                    val hasTicket = movie.ticketCinema.isNotBlank() || movie.ticketDate.isNotBlank()
                    if (isEditingTicket) {
                        // ── Inline Editable Form ──
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            EditableTicketForm(
                                movie = movie,
                                cinemas = cinemas,
                                defaultCinema = defaultCinema,
                                defaultFormat = defaultFormat,
                                onSave = { cinema, date, time, seat, screenNo, format, language, qrPath ->
                                    onSaveTicket(cinema, date, time, seat, screenNo, format, language, qrPath)
                                    isEditingTicket = false
                                },
                                onCancel = {
                                    isEditingTicket = false
                                }
                            )
                        }
                        Spacer(Modifier.height(28.dp))
                    } else if (hasTicket) {
                        // ── Saved ticket: header with edit + delete, then card ───
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Ticket",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Edit button
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .clickable { isEditingTicket = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ConfirmationNumber,
                                        contentDescription = "Edit Ticket",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                // Delete button
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53935).copy(alpha = 0.15f))
                                        .clickable { showDeleteConfirm = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete Ticket",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        // Ticket card — real QR is shown inside the stub when available
                        com.example.premiere.ui.history.VerticalTicketItem(
                            movie = movie,
                            cinemaLogoUrl = cinemaLogoUrl,
                            backgroundColor = null,
                            ticketSize = com.example.premiere.data.preferences.TicketSize.LARGE,
                            qrImageUri = movie.ticketQrImagePath?.let { android.net.Uri.parse(it) },
                            onClick = { }, // No action on normal click
                            onLongClick = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.height(28.dp))

                    } else {
                        // ── No ticket yet: Add Ticket pill button ──
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .clickable { isEditingTicket = true }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Add Ticket",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color(0xFF1A1A1A),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(28.dp))
                    }
                }

                Spacer(Modifier.height(5.dp))
            }
        }

        // Top bar overlay — slides off screen as user scrolls down
        val topBarOffsetPx = minOf(scrollState.value.toFloat(), with(androidx.compose.ui.platform.LocalDensity.current) { 80.dp.toPx() })
        DetailTopBar(
            movie = movie,
            onBack = onBack,
            onToggleReminder = onToggleReminder,
            onRemoveFromApp = onRemoveFromApp,
            reminderToggling = reminderToggling,
            modifier = Modifier.graphicsLayer { translationY = -topBarOffsetPx }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    "Delete Ticket?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    "This will permanently remove the ticket for \"${movie.title}\". This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTicket()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailTopBar(
    movie: Movie,
    onBack: () -> Unit,
    onToggleReminder: () -> Unit,
    onRemoveFromApp: () -> Unit,
    reminderToggling: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SurfaceGlass2)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Trash Icon (Only show if movie is watched/in history, since they can't see the normal ticket trash can)
            if (movie.isWatchedInTheater) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SurfaceGlass2)
                        .clickable(onClick = onRemoveFromApp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Remove from App",
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Bell / Watched icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (movie.isWatchedInTheater) Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else if (movie.hasReminder) BellActive.copy(alpha = 0.2f) 
                        else SurfaceGlass2
                    )
                    .clickable(enabled = !reminderToggling && !movie.isWatchedInTheater, onClick = onToggleReminder),
                contentAlignment = Alignment.Center
            ) {
                if (reminderToggling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = BellActive,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (movie.isWatchedInTheater)
                            Icons.Rounded.Check
                        else if (movie.hasReminder)
                            androidx.compose.material.icons.Icons.Rounded.NotificationsActive
                        else
                            androidx.compose.material.icons.Icons.Rounded.Notifications,
                        contentDescription = if (movie.isWatchedInTheater) "Watched" else if (movie.hasReminder) "Remove reminder" else "Add reminder",
                        tint = if (movie.isWatchedInTheater) Color(0xFF4CAF50) else if (movie.hasReminder) BellActive else TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CastRow(cast: List<CastMember>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(cast, key = { it.id }) { member ->
            CastCard(member = member)
        }
    }
}

@Composable
private fun CastCard(member: CastMember) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceGlass)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
            val shimmerAlpha by infiniteTransition.animateFloat(
                initialValue = 0.1f, targetValue = 0.25f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "shimmer"
            )
            SubcomposeAsyncImage(
                model = member.profileUrl,
                contentDescription = member.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = shimmerAlpha)))
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = member.character,
            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailLoadingState(onBack: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer"
    )
    val shimmerColor = Color.White.copy(alpha = shimmerAlpha)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CinematicBlack)
            .pointerInput(Unit) {} // block touches
            .verticalScroll(rememberScrollState())
    ) {
        // Top padding to match the top bar space
        Spacer(modifier = Modifier.height(88.dp))

        // Poster skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(shimmerColor)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Title skeleton
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(0.6f)
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerColor)
        )

        Spacer(Modifier.height(10.dp))

        // Meta texts skeleton
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(0.4f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerColor)
        )

        Spacer(Modifier.height(24.dp))

        // Overview skeleton
        repeat(3) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(if (it == 2) 0.7f else 0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Cast title skeleton
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(0.3f)
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerColor)
        )

        Spacer(Modifier.height(14.dp))

        // Cast Row skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(4) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(shimmerColor)
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor)
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        }

        // Top bar overlay for loading state
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(shimmerColor)
                    .clickable(onClick = onBack)
            )
            
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(shimmerColor)
            )
        }
    }
}

@Composable
private fun DetailErrorState(message: String, onRetry: () -> Unit) {
    Box(
        Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

// â”€â”€â”€ Add Ticket Dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// ────────────────────────────────────────────────────────────────────────────────
private val FORMAT_OPTIONS = listOf("2D", "3D")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableTicketForm(
    movie: Movie,
    cinemas: List<com.example.premiere.domain.model.Cinema>,
    defaultCinema: String,
    defaultFormat: String,
    onCancel: () -> Unit,
    onSave: (cinema: String, date: String, time: String, seat: String, screenNo: String, format: String, language: String, qrPath: String?) -> Unit
) {
    val context = LocalContext.current

    // Pre-fill with existing data
    var cinema   by remember { mutableStateOf(movie.ticketCinema.ifBlank { defaultCinema }) }
    var date     by remember { mutableStateOf(movie.ticketDate) }
    var time     by remember { mutableStateOf(movie.ticketTime) }
    var seat     by remember { mutableStateOf(movie.ticketSeat) }
    var screenNo by remember { mutableStateOf(movie.ticketScreenNo) }
    var format   by remember { mutableStateOf(movie.ticketFormat.ifBlank { defaultFormat }) }
    var language by remember { mutableStateOf(
        movie.ticketLanguage.ifBlank {
            val rawLang = movie.originalLanguage.ifBlank { "en" }
            val langName = if (rawLang.length == 2) java.util.Locale(rawLang).displayLanguage else rawLang
            langName.replaceFirstChar { ch -> ch.titlecase() }
        }
    ) }
    var qrUri    by remember { mutableStateOf<android.net.Uri?>(
        movie.ticketQrImagePath?.let { android.net.Uri.parse(it) }
    ) }

    var showDatePicker by remember { mutableStateOf(false) }
    var cinemaExpanded by remember { mutableStateOf(false) }
    var formatExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) { /* non-persistable provider */ }
            qrUri = uri
        }
    }

    // Derive live logo URL from selected cinema
    val liveCinemaLogoUrl = remember(cinema) {
        cinemas.find { it.name.equals(cinema, ignoreCase = true) }?.logoUrl
    }

    // Live subtitle
    val cert = if (movie.certification.isNullOrBlank()) "U/A" else movie.certification.uppercase()
    val liveSubtitle = "${language.ifBlank { "—" }} | ${format.ifBlank { "—" }.uppercase()} | $cert"

    // Ticket shape params (matches VerticalTicketItem LARGE, increased to fit bottom buttons)
    val stubHeightDp = 260.dp
    val ticketShape = remember {
        com.example.premiere.ui.history.VerticalTicketShape(
            stubHeightDp = stubHeightDp,
            cornerCutDp = 16.dp,
            topNotchRadiusDp = 16.dp,
            sideNotchRadiusDp = 8.dp
        )
    }

    // Colors for inputs (dark-on-light since ticket is white)
    val surfaceMid = Color(0xFF000000).copy(alpha = 0.07f)
    val surfaceLow = Color(0xFF000000).copy(alpha = 0.04f)
    val borderCol  = Color(0xFF000000).copy(alpha = 0.12f)
    val accentRed  = Color(0xFFE53935)
    val textPri    = Color(0xFF1A1A1A)
    val textSec    = Color(0xFF777777)
    val textDim    = Color(0xFFAAAAAA)

    Column {
        // ── Ticket card ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 8.dp, shape = ticketShape, spotColor = Color.Black.copy(alpha = 0.4f))
                .clip(ticketShape)
                .background(Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Movie Title Header (same as VerticalTicketItem) ──────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = movie.title.uppercase(),
                        color = Color.Black,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = com.example.premiere.ui.history.BebasNeue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    // Live subtitle — updates as user edits language/format
                    Text(
                        text = liveSubtitle,
                        color = Color.Black.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // ── Editable Fields Area (replaces the backdrop image) ───────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cinema dropdown
                    Column {
                        TicketFieldLabel("Cinema", accentRed, textSec, isFocused = cinemaExpanded)
                        Spacer(Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = cinemaExpanded,
                            onExpandedChange = { cinemaExpanded = it }
                        ) {
                            TicketInputField(
                                value = cinema, onValueChange = { cinema = it; cinemaExpanded = true },
                                placeholder = "Select or type cinema name",
                                surfaceLow = surfaceLow, borderCol = borderCol,
                                accentRed = accentRed, textPri = textPri, textDim = textDim,
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                trailingContent = { 
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = textDim,
                                        modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = if (cinemaExpanded) 180f else 0f)
                                    ) 
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = cinemaExpanded,
                                onDismissRequest = { cinemaExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                if (cinemas.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No cinemas saved — add in Settings", color = textSec) },
                                        onClick = {}
                                    )
                                }
                                cinemas.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name, color = textPri) },
                                        onClick = { cinema = c.name; cinemaExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    // Date & Format row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            TicketFieldLabel("Date", accentRed, textSec)
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(surfaceLow)
                                    .border(0.6.dp, borderCol, RoundedCornerShape(12.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.DateRange, contentDescription = null, tint = accentRed, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (date.isNotBlank())
                                            com.example.premiere.ui.history.formatTicketDate(date)
                                        else if (movie.releaseDate.isNotBlank())
                                            com.example.premiere.ui.history.formatTicketDate(movie.releaseDate)
                                        else "Select Date",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = if (date.isBlank()) textDim else textPri
                                        ),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            TicketFieldLabel("Format", accentRed, textSec, isFocused = formatExpanded)
                            Spacer(Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = formatExpanded,
                                onExpandedChange = { formatExpanded = it }
                            ) {
                                TicketInputField(
                                    value = format, onValueChange = { format = it; formatExpanded = true },
                                    placeholder = "2D / 3D",
                                    surfaceLow = surfaceLow, borderCol = borderCol,
                                    accentRed = accentRed, textPri = textPri, textDim = textDim,
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    trailingContent = { 
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = textDim,
                                            modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = if (formatExpanded) 180f else 0f)
                                        ) 
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = formatExpanded,
                                    onDismissRequest = { formatExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    FORMAT_OPTIONS.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt, color = textPri) },
                                            onClick = { format = opt; formatExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Time & Seat row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            TicketFieldLabel("Time", accentRed, textSec)
                            Spacer(Modifier.height(4.dp))
                            TicketInputField(value = time, onValueChange = { time = it },
                                placeholder = "19:30",
                                surfaceLow = surfaceLow, borderCol = borderCol,
                                accentRed = accentRed, textPri = textPri, textDim = textDim)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            TicketFieldLabel("Seat", accentRed, textSec)
                            Spacer(Modifier.height(4.dp))
                            TicketInputField(value = seat, onValueChange = { seat = it },
                                placeholder = "G12",
                                surfaceLow = surfaceLow, borderCol = borderCol,
                                accentRed = accentRed, textPri = textPri, textDim = textDim)
                        }
                    }

                    // Screen & Lang row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            TicketFieldLabel("Screen", accentRed, textSec)
                            Spacer(Modifier.height(4.dp))
                            TicketInputField(value = screenNo, onValueChange = { screenNo = it },
                                placeholder = "1",
                                surfaceLow = surfaceLow, borderCol = borderCol,
                                accentRed = accentRed, textPri = textPri, textDim = textDim)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            TicketFieldLabel("Language", accentRed, textSec)
                            Spacer(Modifier.height(4.dp))
                            TicketInputField(value = language, onValueChange = { language = it },
                                placeholder = "English",
                                surfaceLow = surfaceLow, borderCol = borderCol,
                                accentRed = accentRed, textPri = textPri, textDim = textDim)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                // ── Dashed perforation line (drawn via Canvas same as real ticket) ──
                val sideNotchRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                ) {
                    drawLine(
                        color = Color.LightGray,
                        start = androidx.compose.ui.geometry.Offset(sideNotchRadiusPx + 10f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width - sideNotchRadiusPx - 10f, 0f),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                    )
                }

                // ── Stub: live cinema logo + QR preview (clickable to pick QR) ───
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(stubHeightDp)
                        .padding(top = 20.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Live cinema logo
                    if (!liveCinemaLogoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = liveCinemaLogoUrl,
                            contentDescription = "Cinema Logo",
                            modifier = Modifier
                                .height(40.dp)
                                .widthIn(min = 40.dp, max = 140.dp)
                                .padding(bottom = 8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // QR preview — real image if set, otherwise Add QR pill
                    if (qrUri != null) {
                        // Real QR: show image + tap-to-replace hint
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = qrUri,
                                    contentDescription = "Entry QR Code",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().background(Color.White)
                                )
                            }
                            Text(
                                text = "Tap to replace",
                                color = Color.Black.copy(alpha = 0.4f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // No QR: simple pill button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFE53935).copy(alpha = 0.10f))
                                .border(0.8.dp, Color(0xFFE53935).copy(alpha = 0.4f), RoundedCornerShape(50))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Add QR",
                                    color = Color(0xFFE53935),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ── Cancel / Save — at the very bottom of the stub ──────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cancel — outlined pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, Color.Black.copy(alpha = 0.18f), RoundedCornerShape(50))
                                .clickable(onClick = onCancel)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Cancel",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        // Save — solid dark pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF1A1A1A))
                                .clickable { onSave(cinema, date, time, seat, screenNo, format, language, qrUri?.toString()) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Save Ticket",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }


    // Date Picker
    if (showDatePicker) {
        val initialMillis = remember(date, movie.releaseDate) {
            val d = date.ifBlank { movie.releaseDate }
            if (d.isNotBlank()) {
                try {
                    java.time.LocalDate.parse(d)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant().toEpochMilli()
                } catch (e: Exception) { null }
            } else null
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val d = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        date = d.toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
@Composable
private fun TicketFieldLabel(
    label: String,
    accentRed: Color,
    textSec: Color,
    isFocused: Boolean = false
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.8.sp,
            color = if (isFocused) accentRed else textSec,
            fontWeight = FontWeight.Medium
        )
    )
}

// ── Helper: text input field ──────────────────────────────────────────────────
@Composable
private fun TicketInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    surfaceLow: androidx.compose.ui.graphics.Color,
    borderCol: androidx.compose.ui.graphics.Color,
    accentRed: androidx.compose.ui.graphics.Color,
    textPri: androidx.compose.ui.graphics.Color,
    textDim: androidx.compose.ui.graphics.Color,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val isFocused = remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceLow)
            .border(
                width = if (isFocused.value) 1.dp else 0.6.dp,
                color = if (isFocused.value) accentRed.copy(alpha = 0.7f) else borderCol,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = textPri, fontWeight = FontWeight.Normal
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp)
                    .onFocusChanged { isFocused.value = it.isFocused },
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(color = textDim))
                    }
                    inner()
                }
            )
            if (trailingContent != null) {
                Spacer(Modifier.width(8.dp))
                trailingContent()
            }
        }
    }
}

