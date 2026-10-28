package com.example.premiere.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Fires [onSwipe] once per gesture as soon as the accumulated horizontal movement passes a small
 * threshold. Observes on the Initial pass so child scrollables can't swallow the swipe.
 */
fun Modifier.onHorizontalSwipe(
    key: Any?,
    enabled: Boolean = true,
    onIntent: (() -> Unit)? = null,
    onSwipe: (dx: Float) -> Unit
): Modifier = if (!enabled) this else pointerInput(key) {
    val threshold = 20.dp.toPx()
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var total = Offset.Zero
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: break
            if (!change.pressed) break
            total += change.positionChange()
            if (onIntent != null && abs(total.x) > 10.dp.toPx() && abs(total.x) > abs(total.y)) onIntent()
            if (abs(total.x) > threshold && abs(total.x) > abs(total.y) * 1.2f) {
                event.changes.forEach { it.consume() }
                onSwipe(total.x)
                break
            }
        }
    }
}
