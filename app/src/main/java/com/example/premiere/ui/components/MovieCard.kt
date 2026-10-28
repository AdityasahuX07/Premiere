package com.example.premiere.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.example.premiere.domain.model.Movie
import com.example.premiere.theme.*
import java.time.LocalDate
import java.util.Locale

// Metallic date gradient: dark/shadowed at top → bright white at bottom
private val MetallicDateBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF555555),
        Color(0xFF999999),
        Color(0xFFDDDDDD),
        Color(0xFFFFFFFF)
    )
)

@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDateHeader: Boolean = true,
    showPills: Boolean = true
) {
    val shape = RoundedCornerShape(28.dp)

    Column(modifier = modifier.clickable(onClick = onClick)) {
        if (showDateHeader) {
            // Parse date for centered metallic header
            val parsed = try { LocalDate.parse(movie.releaseDate) } catch (_: Exception) { null }
            val dayStr = parsed?.dayOfMonth?.toString() ?: ""
            val monStr = parsed?.month?.name?.take(3)
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                ?: ""

            Text(
                text = "$dayStr $monStr",
                textAlign = TextAlign.Center,
                style = TextStyle(
                    brush = MetallicDateBrush,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        // Poster card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.67f)
                .clip(shape)
                .background(SurfaceGlass)
        ) {
            // Poster image
            SubcomposeAsyncImage(
                model = movie.highResPosterUrl ?: movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    val infiniteTransition = rememberInfiniteTransition()
                    val shimmerAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.02f,
                        targetValue = 0.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "shimmer"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = shimmerAlpha))
                    )
                }
            )


            // Top-left pills — only on front card (showPills = true)
            if (showPills) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    movie.formattedRuntime?.let { runtime ->
                        GlassPill(text = runtime)
                    }
                    movie.genres.firstOrNull()?.let { genre ->
                        GlassPill(text = genre)
                    }
                }
            }
        }
    }
}

@Composable
fun GlassPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun MetadataPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SurfaceGlass2)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(color = TextPrimary)
        )
    }
}

fun formatReleaseDate(date: String): String {
    return try {
        val parsed = LocalDate.parse(date)
        "${parsed.dayOfMonth} ${parsed.month.name.take(3)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
    } catch (_: Exception) {
        date
    }
}
