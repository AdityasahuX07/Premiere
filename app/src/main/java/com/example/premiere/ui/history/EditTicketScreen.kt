package com.example.premiere.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

// ─── Color palette (matches the app dark theme) ──────────────────────────────
private val BgPrimary   = Color(0xFF121212)
private val BgSurface   = Color(0xFF1E1E1E)
private val BgSurface2  = Color(0xFF282828)
private val BorderColor = Color(0xFF333333)
private val TextWhite   = Color(0xFFFFFFFF)
private val TextGray    = Color(0xFF9E9E9E)
private val TextDim     = Color(0xFF616161)
private val AccentRed   = Color(0xFFE53935)
private val StarGold    = Color(0xFFFFB300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTicketScreen(
    movieId: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditTicketViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    val formatPresets = listOf("2D", "3D")
    var formatDropdownExpanded by remember { mutableStateOf(false) }
    var cinemaDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(movieId) {
        viewModel.loadMovie(movieId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Ticket",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = onDismiss) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF252525))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgPrimary)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Remove from History — ghost pill, white stroke
                OutlinedButton(
                    onClick = { viewModel.removeFromHistory(onDismiss) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Remove", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                // Save Ticket — solid white pill
                Button(
                    onClick = { viewModel.saveTicketInfo(onDismiss) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp), tint = Color(0xFF1A1A1A))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Ticket", color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentRed)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {

                // ── 1. Movie Info Card ────────────────────────────────────────
                MovieInfoCard(uiState)

                Spacer(Modifier.height(20.dp))

                // ── 2. Ticket Info Section ────────────────────────────────────
                SectionHeader("TICKET INFO", modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(12.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                    // Cinema dropdown
                    if (uiState.cinemas.isEmpty()) {
                        FormField(
                            value = uiState.ticketCinema,
                            onValueChange = viewModel::onCinemaChanged,
                            label = "Cinema",
                            placeholder = "Add cinemas in Settings"
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = cinemaDropdownExpanded,
                            onExpandedChange = { cinemaDropdownExpanded = !cinemaDropdownExpanded }
                        ) {
                            FormField(
                                value = uiState.ticketCinema,
                                onValueChange = viewModel::onCinemaChanged,
                                label = "Cinema",
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cinemaDropdownExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = cinemaDropdownExpanded,
                                onDismissRequest = { cinemaDropdownExpanded = false },
                                containerColor = BgSurface2
                            ) {
                                uiState.cinemas.forEach { cinema ->
                                    DropdownMenuItem(
                                        text = { Text(cinema.name, color = TextWhite) },
                                        onClick = {
                                            viewModel.onCinemaChanged(cinema.name)
                                            cinemaDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Date & Format row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FormField(
                            value = uiState.ticketDate.ifBlank { uiState.movieReleaseDate },
                            onValueChange = viewModel::onDateChanged,
                            label = "Date",
                            placeholder = "e.g. 2026-08-12",
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Pick Date", tint = TextGray)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        // Format dropdown
                        ExposedDropdownMenuBox(
                            expanded = formatDropdownExpanded,
                            onExpandedChange = { formatDropdownExpanded = !formatDropdownExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            FormField(
                                value = uiState.ticketFormat,
                                onValueChange = viewModel::onFormatChanged,
                                label = "Format",
                                placeholder = "2D",
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatDropdownExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = formatDropdownExpanded,
                                onDismissRequest = { formatDropdownExpanded = false },
                                containerColor = BgSurface2
                            ) {
                                formatPresets.forEach { fmt ->
                                    DropdownMenuItem(
                                        text = { Text(fmt, color = TextWhite) },
                                        onClick = {
                                            viewModel.onFormatChanged(fmt)
                                            formatDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Time & Seat row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FormField(
                            value = uiState.ticketTime,
                            onValueChange = viewModel::onTimeChanged,
                            label = "Time",
                            placeholder = "19:30",
                            modifier = Modifier.weight(1f)
                        )
                        FormField(
                            value = uiState.ticketSeat,
                            onValueChange = viewModel::onSeatChanged,
                            label = "Seat",
                            placeholder = "G-14",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Screen & Lang row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FormField(
                            value = uiState.ticketScreenNo,
                            onValueChange = viewModel::onScreenNoChanged,
                            label = "Screen",
                            placeholder = "1",
                            modifier = Modifier.weight(1f)
                        )
                        FormField(
                            value = uiState.ticketLanguage,
                            onValueChange = viewModel::onLanguageChanged,
                            label = "Language",
                            placeholder = "English",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }


                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Date Picker dialog
    if (showDatePicker) {
        val initialMillis = remember(uiState.ticketDate, uiState.movieReleaseDate) {
            val dateStr = uiState.ticketDate.ifBlank { uiState.movieReleaseDate }
            if (dateStr.isNotBlank()) {
                try {
                    java.time.LocalDate.parse(dateStr)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                } catch (e: Exception) {
                    null
                }
            } else null
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        viewModel.onDateChanged(date.toString())
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

// ── Movie Info Card ────────────────────────────────────────────────────────────

@Composable
private fun MovieInfoCard(uiState: EditTicketUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        // Backdrop
        val backdropUrl = uiState.movieBackdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
            ?: uiState.moviePosterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Gradient overlay — heavier at bottom for logo legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.72f)
                        )
                    )
                )
        )

        // Movie logo — bottom-left
        if (uiState.movieLogoUrl != null) {
            AsyncImage(
                model = uiState.movieLogoUrl,
                contentDescription = uiState.movieTitle,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 14.dp)
                    .widthIn(max = 160.dp)
                    .heightIn(max = 56.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

// ── Star Rating Bar ────────────────────────────────────────────────────────────

@Composable
private fun StarRatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    maxStars: Int = 5
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..maxStars) {
            val filled = i <= rating
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Star $i",
                tint = if (filled) StarGold else TextDim,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onRatingChanged(if (rating == i.toFloat()) 0f else i.toFloat()) }
                    .padding(2.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (rating > 0f) "${rating.toInt()}/$maxStars" else "Tap to rate",
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (rating > 0f) StarGold else TextDim,
                fontWeight = if (rating > 0f) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            color = TextGray,
            fontWeight = FontWeight.Medium
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = TextDim) },
        modifier = modifier.fillMaxWidth(),
        trailingIcon = trailingIcon,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedBorderColor = AccentRed,
            unfocusedBorderColor = BorderColor,
            focusedLabelColor = AccentRed,
            unfocusedLabelColor = TextGray,
            cursorColor = AccentRed
        )
    )
}

@Composable
private fun InfoPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.85f)),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
