package com.example.premiere.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.premiere.ui.onHorizontalSwipe
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.premiere.BuildConfig
import com.example.premiere.theme.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.painter.Painter
import com.example.premiere.R

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

enum class SettingsDestination {
    AddedTickets,
    AddCinemas,
    Main,
    SearchResult,
    TmdbApi,
    ImdbApi,
    TicketCustomization,
    TicketDefaults,
    BackupExport,
    ForceRefresh
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsPanel(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    viewModel: SettingsViewModel = hiltViewModel(),
    initialDestination: SettingsDestination = SettingsDestination.Main
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentDestination by rememberSaveable { mutableStateOf(initialDestination) }
    var slideDirection by remember { mutableStateOf(AnimatedContentTransitionScope.SlideDirection.Left) }

    LaunchedEffect(initialDestination, visible) {
        if (visible && initialDestination != SettingsDestination.Main) {
            currentDestination = initialDestination
        }
    }

    // Panel stays composed while hidden; reset to the main page once it has slid away
    LaunchedEffect(visible) {
        if (!visible) {
            kotlinx.coroutines.delay(400)
            currentDestination = SettingsDestination.Main
        }
    }

    val navigateTo: (SettingsDestination) -> Unit = { dest ->
        slideDirection = AnimatedContentTransitionScope.SlideDirection.Left
        currentDestination = dest
    }

    val navigateBack: () -> Unit = {
        slideDirection = AnimatedContentTransitionScope.SlideDirection.Right
        currentDestination = SettingsDestination.Main
    }

    BackHandler(enabled = visible && currentDestination != SettingsDestination.Main) {
        navigateBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // Settings panel (full screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CinematicBlack)
                .onHorizontalSwipe(key = currentDestination, enabled = visible) { dx ->
                    if (dx > 0) {
                        if (currentDestination == SettingsDestination.Main) {
                            onDismiss()
                        } else {
                            navigateBack()
                        }
                    }
                }
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    slideIntoContainer(towards = slideDirection) togetherWith
                    slideOutOfContainer(towards = slideDirection)
                },
                label = "settings_navigation"
            ) { targetState ->
                when (targetState) {
                    SettingsDestination.Main -> MainSettingsScreen(
                        state = state,
                        onNavigate = navigateTo,
                        onDismiss = onDismiss
                    )
                    SettingsDestination.AddedTickets -> AddedTicketsScreen(
                        state = state,
                        onBack = navigateBack
                    )
                    SettingsDestination.SearchResult -> SearchResultScreen(
                        state = state,
                        viewModel = viewModel,
                        onBack = navigateBack
                    )
                    SettingsDestination.TmdbApi -> TmdbApiScreen(
                        state = state,
                        viewModel = viewModel,
                        onBack = navigateBack
                    )
                    SettingsDestination.ImdbApi -> ImdbApiScreen(
                        state = state,
                        viewModel = viewModel,
                        onBack = navigateBack
                    )
                    SettingsDestination.AddCinemas -> AddCinemasScreen(state, viewModel, { currentDestination = SettingsDestination.Main })
                    SettingsDestination.TicketCustomization -> TicketCustomizationScreen(
                        state = state,
                        viewModel = viewModel,
                        onBack = navigateBack
                    )
                    SettingsDestination.TicketDefaults -> TicketDefaultsScreen(
                        state = state,
                        viewModel = viewModel,
                        onBack = navigateBack
                    )
                    SettingsDestination.BackupExport -> BackupExportScreen(
                        state = state,
                        viewModel = viewModel,
                        onBack = navigateBack
                    )
                    SettingsDestination.ForceRefresh -> ForceRefreshScreen(
                        state = state,
                        viewModel = viewModel,
                        onBack = navigateBack
                    )
                }
            }
        }
    }
}

@Composable
fun MainSettingsScreen(
    state: SettingsUiState,
    onNavigate: (SettingsDestination) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }

        Spacer(Modifier.height(32.dp))

        // GENERAL Section
        SettingsSectionHeader("GENERAL")
        SectionContainer {
            SettingsListItem(
                iconPainter = painterResource(R.drawable.ic_your_tickets),
                iconTint = Color.White,
                title = "Your Tickets",
                subtitle = "View added tickets",
                onClick = { onNavigate(SettingsDestination.AddedTickets) }
            )
            SettingsDivider(modifier = Modifier.padding(start = 68.dp))
            SettingsListItem(
                iconPainter = painterResource(R.drawable.ic_cinemas),
                iconTint = Color.White,
                iconSize = 28.dp,
                title = "Cinemas",
                subtitle = "Add cinemas and logos",
                onClick = { onNavigate(SettingsDestination.AddCinemas) }
            )
            SettingsDivider(modifier = Modifier.padding(start = 68.dp))
            SettingsListItem(
                icon = Icons.Rounded.Search,
                title = "Search History",
                subtitle = "Manage search settings",
                onClick = { onNavigate(SettingsDestination.SearchResult) }
            )
        }

        Spacer(Modifier.height(24.dp))

        // INTEGRATION Section
        SettingsSectionHeader("INTEGRATION")
        SectionContainer {
            SettingsListItem(
                iconPainter = painterResource(R.drawable.ic_tmdb),
                iconTint = Color.White,
                title = "TMDB API",
                subtitle = if (state.apiKey.isBlank()) "⚠ API key not set" else "Manage TMDB API key",
                onClick = { onNavigate(SettingsDestination.TmdbApi) }
            )
            SettingsDivider(modifier = Modifier.padding(start = 68.dp))
            SettingsListItem(
                iconPainter = painterResource(R.drawable.ic_imdb),
                iconTint = Color.White,
                iconSize = 28.dp,
                title = "IMDB / OMDB API",
                subtitle = if (state.omdbApiKey.isBlank()) "Optional — OMDB not configured" else "Manage OMDB API key",
                onClick = { onNavigate(SettingsDestination.ImdbApi) }
            )
        }

        Spacer(Modifier.height(24.dp))

        // HISTORY Section
        SettingsSectionHeader("HISTORY")
        SectionContainer {
            SettingsListItem(
                iconPainter = painterResource(R.drawable.ic_wand_stars),
                iconTint = Color.White,
                title = "Ticket Customization",
                subtitle = "Configure your generated tickets",
                onClick = { onNavigate(SettingsDestination.TicketCustomization) }
            )
            SettingsDivider(modifier = Modifier.padding(start = 68.dp))
            SettingsListItem(
                iconPainter = painterResource(R.drawable.ic_ticket_defaults),
                iconTint = Color.White,
                title = "Ticket Defaults",
                subtitle = "Set default cinema and format",
                onClick = { onNavigate(SettingsDestination.TicketDefaults) }
            )
        }

        Spacer(Modifier.height(24.dp))

        // DATA Section
        SettingsSectionHeader("DATA")
        SectionContainer {
            SettingsListItem(
                icon = Icons.Rounded.Refresh,
                title = "Force Refresh",
                subtitle = "Refresh API metadata",
                onClick = { onNavigate(SettingsDestination.ForceRefresh) }
            )
            SettingsDivider(modifier = Modifier.padding(start = 68.dp))
            SettingsListItem(
                iconPainter = painterResource(R.drawable.ic_backup_export),
                iconTint = Color.White,
                title = "Backup & Export",
                subtitle = "Export or restore your data",
                onClick = { onNavigate(SettingsDestination.BackupExport) }
            )
        }

        Spacer(Modifier.height(48.dp))

        Text(
            "PREMIÈRE",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 4.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Light
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun SearchResultScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        SubScreenHeader(title = "Search Result", onBack = onBack)
        
        Spacer(Modifier.height(24.dp))
        
        SectionContainer {
            SettingsListItem(
                icon = Icons.Rounded.Search,
                title = "Show Released Titles",
                subtitle = "Only show movies that are already released",
                onClick = null,
                rightContent = {
                    Switch(
                        checked = state.showReleasedTitles,
                        onCheckedChange = viewModel::toggleShowReleasedTitles,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentRed,
                            checkedTrackColor = AccentRed.copy(alpha = 0.5f)
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun TmdbApiScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var showApiKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        SubScreenHeader(title = "TMDB API", onBack = onBack)
        
        Spacer(Modifier.height(24.dp))
        
        SectionContainer {
            Box(modifier = Modifier.padding(16.dp)) {
                Column {
                    ApiKeyField(
                        value = state.apiKeyInput,
                        onValueChange = viewModel::onApiKeyInputChanged,
                        showKey = showApiKey,
                        onToggleVisibility = { showApiKey = !showApiKey }
                    )
                    Spacer(Modifier.height(16.dp))
                    SaveButtonRow(
                        isSaved = state.isSaved,
                        onSave = viewModel::saveApiKey,
                        helpText = "Get your free API key at themoviedb.org"
                    )
                }
            }
        }
    }
}

@Composable
fun ImdbApiScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var showApiKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        SubScreenHeader(title = "IMDB API", onBack = onBack)
        
        Spacer(Modifier.height(24.dp))
        
        SectionContainer {
            Box(modifier = Modifier.padding(16.dp)) {
                Column {
                    ApiKeyField(
                        value = state.omdbApiKeyInput,
                        onValueChange = viewModel::onOmdbApiKeyInputChanged,
                        showKey = showApiKey,
                        onToggleVisibility = { showApiKey = !showApiKey },
                        placeholder = "Enter your OMDB API key…"
                    )
                    Spacer(Modifier.height(16.dp))
                    SaveButtonRow(
                        isSaved = state.isOmdbSaved,
                        onSave = viewModel::saveOmdbApiKey,
                        helpText = "Get your free API key at omdbapi.com"
                    )
                }
            }
        }
    }
}

@Composable
fun TicketCustomizationScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val dummyMovie = remember {
        com.example.premiere.domain.model.Movie(
            id = 12345,
            title = "PREMIÈRE",
            originalTitle = "PREMIÈRE",
            overview = "",
            posterPath = null,
            backdropPath = null,
            releaseDate = "2026-03-26",
            voteAverage = 0.0,
            voteCount = 0,
            ticketCinema = "PREMIÈRE CINEMAS",
            ticketFormat = "IMAX 3D",
            ticketScreenNo = "1",
            ticketSeat = "G-14",
            ticketTime = "19:30"
        )
    }
    
    val previewMovie = state.previewMovie ?: dummyMovie

    val previewBgColor = remember(state.ticketColor) {
        val hex = state.ticketColor
        if (hex == "RANDOM") {
            Color(0xFF673AB7) // Default for random preview
        } else if (hex == "POSTER") {
            null
        } else {
            try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFF673AB7) }
        }
    }

    val previewCinemaLogoUrl = remember(state.cinemas, previewMovie) {
        state.cinemas.find { it.name == previewMovie.ticketCinema }?.logoUrl
            ?: state.cinemas.firstOrNull()?.logoUrl
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        SubScreenHeader(title = "Ticket Customization", onBack = onBack)
        
        Spacer(Modifier.height(24.dp))
        
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val exactTicketWidth = configuration.screenWidthDp.dp - 32.dp
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.ticketOrientation == com.example.premiere.data.preferences.TicketOrientation.HORIZONTAL) {
                com.example.premiere.ui.history.HorizontalTicketItem(
                    movie = previewMovie,
                    cinemaLogoUrl = previewCinemaLogoUrl,
                    backgroundColor = previewBgColor,
                    ticketSize = state.ticketSize,
                    onClick = {},
                    modifier = Modifier.requiredWidth(exactTicketWidth)
                )
            } else {
                com.example.premiere.ui.history.VerticalTicketItem(
                    movie = previewMovie,
                    cinemaLogoUrl = previewCinemaLogoUrl,
                    backgroundColor = previewBgColor,
                    ticketSize = state.ticketSize,
                    onClick = {},
                    modifier = Modifier.requiredWidth(exactTicketWidth)
                )
            }
        }
        
        // Ticket Size Card
        SectionContainer {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Ticket Size",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsOption(
                        title = "Small",
                        isSelected = state.ticketSize == com.example.premiere.data.preferences.TicketSize.SMALL,
                        onClick = { viewModel.setTicketSize(com.example.premiere.data.preferences.TicketSize.SMALL) },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsOption(
                        title = "Medium",
                        isSelected = state.ticketSize == com.example.premiere.data.preferences.TicketSize.MEDIUM,
                        onClick = { viewModel.setTicketSize(com.example.premiere.data.preferences.TicketSize.MEDIUM) },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsOption(
                        title = "Large",
                        isSelected = state.ticketSize == com.example.premiere.data.preferences.TicketSize.LARGE,
                        onClick = { viewModel.setTicketSize(com.example.premiere.data.preferences.TicketSize.LARGE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Ticket Orientation Card
        SectionContainer {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Ticket Orientation",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsOption(
                        title = "Horizontal",
                        isSelected = state.ticketOrientation == com.example.premiere.data.preferences.TicketOrientation.HORIZONTAL,
                        onClick = { viewModel.setTicketOrientation(com.example.premiere.data.preferences.TicketOrientation.HORIZONTAL) },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsOption(
                        title = "Vertical",
                        isSelected = state.ticketOrientation == com.example.premiere.data.preferences.TicketOrientation.VERTICAL,
                        onClick = { viewModel.setTicketOrientation(com.example.premiere.data.preferences.TicketOrientation.VERTICAL) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Ticket Colors Card
        SectionContainer {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Ticket Colors",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsOption(
                        title = "Poster",
                        isSelected = state.ticketColor == "POSTER",
                        onClick = { viewModel.setTicketColor("POSTER") },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsOption(
                        title = "Random",
                        isSelected = state.ticketColor == "RANDOM",
                        onClick = { viewModel.setTicketColor("RANDOM") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                val presetColors = listOf(
                    "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3",
                    "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#FF9800", "#FF5722"
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(80.dp)
                ) {
                    items(presetColors) { hexColor ->
                        val color = try { Color(android.graphics.Color.parseColor(hexColor)) } catch (e: Exception) { Color.Transparent }
                        val isSelected = state.ticketColor == hexColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setTicketColor(hexColor) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(36.dp).clip(CircleShape).background(SurfaceGlass2)
        ) {
            Icon(
                Icons.Rounded.ArrowBack, 
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
    }
}

@Composable
private fun SectionContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceGlass2)
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
private fun SettingsListItem(
    icon: ImageVector,
    iconBackgroundColor: Color = SurfaceGlass3,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    rightContent: @Composable () -> Unit = {
        Icon(
            Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(24.dp)
        )
    }
) {
    SettingsListItemBase(
        iconSlot = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        },
        iconBackgroundColor = iconBackgroundColor,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        rightContent = rightContent
    )
}

@Composable
private fun SettingsListItem(
    iconPainter: Painter,
    iconBackgroundColor: Color = SurfaceGlass3,
    iconTint: Color = Color.White,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    rightContent: @Composable () -> Unit = {
        Icon(
            Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(24.dp)
        )
    }
) {
    SettingsListItemBase(
        iconSlot = {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        },
        iconBackgroundColor = iconBackgroundColor,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        rightContent = rightContent
    )
}

@Composable
private fun SettingsListItemBase(
    iconSlot: @Composable () -> Unit,
    iconBackgroundColor: Color = SurfaceGlass3,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    rightContent: @Composable () -> Unit = {
        Icon(
            Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(24.dp)
        )
    }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            iconSlot()
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary
                )
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        rightContent()
    }
}


@Composable
private fun SaveButtonRow(
    isSaved: Boolean,
    onSave: () -> Unit,
    helpText: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSaved) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Saved",
                        style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF30D158))
                    )
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            Button(
                onClick = onSave,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Save Key", style = MaterialTheme.typography.labelLarge)
            }
        }

        Text(
            helpText,
            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    showKey: Boolean,
    onToggleVisibility: () -> Unit,
    placeholder: String = "Enter your API key…"
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CinematicBlack)
            .border(1.dp, GlassBorder, shape)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                cursorBrush = SolidColor(AccentRed),
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None
                else PasswordVisualTransformation(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextDisabled)
                        )
                    }
                    inner()
                }
            )
            TextButton(onClick = onToggleVisibility, contentPadding = PaddingValues(4.dp)) {
                Text(
                    if (showKey) "Hide" else "Show",
                    style = MaterialTheme.typography.labelMedium.copy(color = AccentRed)
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            color = TextTertiary,
            fontWeight = FontWeight.Medium
        ),
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsDivider(modifier: Modifier = Modifier) {
    Divider(color = DividerColor, thickness = 0.5.dp, modifier = modifier)
}

@Composable
private fun SettingsOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) AccentRed.copy(alpha = 0.2f) else CinematicBlack
    val borderColor = if (isSelected) AccentRed else GlassBorder
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isSelected) AccentRed else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCinemasScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var cinemaName by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        SubScreenHeader(title = "Add Cinemas", onBack = onBack)
        
        Spacer(Modifier.height(24.dp))
        
        SectionContainer {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = cinemaName,
                    onValueChange = { cinemaName = it },
                    label = { Text("Cinema Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentRed,
                        unfocusedBorderColor = GlassBorder
                    )
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = logoUrl,
                    onValueChange = { logoUrl = it },
                    label = { Text("Logo PNG URL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentRed,
                        unfocusedBorderColor = GlassBorder
                    )
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { 
                        viewModel.addCinema(cinemaName.trim(), logoUrl.trim())
                        cinemaName = ""
                        logoUrl = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cinemaName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Add Cinema", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        if (state.cinemas.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SettingsSectionHeader("ADDED CINEMAS")
            SectionContainer {
                Column {
                    state.cinemas.forEachIndexed { index, cinema ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cinema.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                                if (cinema.logoUrl.isNotBlank()) {
                                    Text("Has Logo URL", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteCinema(cinema.id) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Delete", tint = AccentRed)
                            }
                        }
                        if (index < state.cinemas.size - 1) {
                            SettingsDivider(modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Backup & Export Screen ───────────────────────────────────────────────────

@Composable
fun BackupExportScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it, context) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it, context) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        SubScreenHeader(title = "Backup & Export", onBack = onBack)

        Spacer(Modifier.height(8.dp))

        Text(
            "Export your theatrical history, cinemas, and settings to a JSON file. " +
            "Import to restore on a new device or after reinstall.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // Result message
        state.backupMessage?.let { msg ->
            val isSuccess = msg.startsWith("✓")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSuccess) Color(0xFF1A3A2A) else Color(0xFF3A1A1A))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                    contentDescription = null,
                    tint = if (isSuccess) Color(0xFF30D158) else AccentRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    msg.removePrefix("✓ ").removePrefix("✗ "),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isSuccess) Color(0xFF30D158) else AccentRed
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = viewModel::clearBackupMessage, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = TextTertiary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Export Card
        SectionContainer {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A2A3A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, tint = Color(0xFF4DA6FF), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export Backup", style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
                        Text("Save history + cinemas to a .json file", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val timestamp = java.time.LocalDate.now().toString()
                        exportLauncher.launch("premiere_backup_$timestamp.json")
                    },
                    enabled = !state.isExporting && !state.isImporting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4A8A))
                ) {
                    if (state.isExporting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.isExporting) "Exporting…" else "Choose Save Location", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Import Card
        SectionContainer {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2A1A3A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = Color(0xFFBB88FF), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Import Backup", style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
                        Text("Merge from a .json backup file", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Movies already in your history won't be duplicated. Cinemas with the same name are skipped.",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    enabled = !state.isExporting && !state.isImporting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A1A5A))
                ) {
                    if (state.isImporting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.isImporting) "Importing…" else "Choose Backup File", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Ticket Defaults Screen ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDefaultsScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val formatPresets = listOf("2D", "3D")
    var cinemaDropdownExpanded by remember { mutableStateOf(false) }
    var formatDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        SubScreenHeader(title = "Ticket Defaults", onBack = onBack)

        Text(
            "These values auto-fill when you add a movie to history for the first time.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        SectionContainer {
            Column(modifier = Modifier.padding(16.dp)) {

                // Default Cinema
                Text("Default Cinema", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(8.dp))

                if (state.cinemas.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = cinemaDropdownExpanded,
                        onExpandedChange = { cinemaDropdownExpanded = !cinemaDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = state.defaultCinemaInput,
                            onValueChange = viewModel::onDefaultCinemaInputChanged,
                            label = { Text("Cinema") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cinemaDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentRed,
                                unfocusedBorderColor = GlassBorder,
                                focusedLabelColor = AccentRed,
                                unfocusedLabelColor = TextSecondary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = cinemaDropdownExpanded,
                            onDismissRequest = { cinemaDropdownExpanded = false }
                        ) {
                            state.cinemas.forEach { cinema ->
                                DropdownMenuItem(
                                    text = { Text(cinema.name) },
                                    onClick = {
                                        viewModel.onDefaultCinemaInputChanged(cinema.name)
                                        cinemaDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = state.defaultCinemaInput,
                        onValueChange = viewModel::onDefaultCinemaInputChanged,
                        label = { Text("Cinema Name") },
                        placeholder = { Text("e.g. PVR IMAX", color = TextDisabled) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = AccentRed,
                            unfocusedLabelColor = TextSecondary
                        )
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Default Format
                Text("Default Projection Type", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = formatDropdownExpanded,
                    onExpandedChange = { formatDropdownExpanded = !formatDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = state.defaultFormatInput,
                        onValueChange = viewModel::onDefaultFormatInputChanged,
                        label = { Text("Projection Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = AccentRed,
                            unfocusedLabelColor = TextSecondary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = formatDropdownExpanded,
                        onDismissRequest = { formatDropdownExpanded = false }
                    ) {
                        formatPresets.forEach { fmt ->
                            DropdownMenuItem(
                                text = { Text(fmt) },
                                onClick = {
                                    viewModel.onDefaultFormatInputChanged(fmt)
                                    formatDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Save
                Button(
                    onClick = viewModel::saveTicketDefaults,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    if (state.isDefaultsSaved) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Saved!", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Save Defaults", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@Composable
fun AddedTicketsScreen(
    state: SettingsUiState,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            SubScreenHeader(title = "Your Tickets", onBack = onBack)
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(state.activeTickets, key = { it.id }) { movie ->
                val cinemaLogoUrl = state.cinemas.find { it.name == movie.ticketCinema }?.logoUrl
                    ?: state.cinemas.firstOrNull()?.logoUrl

                com.example.premiere.ui.history.VerticalTicketItem(
                    movie = movie,
                    cinemaLogoUrl = cinemaLogoUrl,
                    backgroundColor = null,
                    ticketSize = com.example.premiere.data.preferences.TicketSize.LARGE,
                    qrImageUri = movie.ticketQrImagePath?.let { android.net.Uri.parse(it) },
                    onClick = { },
                    onLongClick = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
    }
}

// =================================================================================================
// Force Refresh Screen
// =================================================================================================

@Composable
fun ForceRefreshScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        SubScreenHeader(title = "Force Refresh", onBack = onBack)
        
        Spacer(Modifier.height(32.dp))

        // Message banner
        state.forceRefreshMessage?.let { msg ->
            val isSuccess = msg.startsWith("Refreshed")
            val containerColor = if (isSuccess) Color(0xFF4CAF50).copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.2f)
            val contentColor = if (isSuccess) Color(0xFF4CAF50) else AccentRed

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerColor)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium.copy(color = contentColor),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = viewModel::clearForceRefreshMessage, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, null, tint = contentColor, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Info card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceGlass)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "About Force Refresh",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "This will re-fetch API metadata (posters, ratings, release dates) for all movies saved locally in your History, Reminders, and Tickets.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your personal data (ticket details, watch status) will not be disrupted.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Action button
        Button(
            onClick = { viewModel.forceRefreshMetadata() },
            enabled = !state.isForceRefreshing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                disabledContainerColor = SurfaceGlass
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (state.isForceRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(12.dp))
                Text("Refreshing...", color = Color.White, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Start Force Refresh", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
