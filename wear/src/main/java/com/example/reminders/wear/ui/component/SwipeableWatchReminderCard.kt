package com.example.reminders.wear.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import com.example.reminders.wear.R
import com.example.reminders.wear.data.WatchReminder
import com.example.reminders.wear.ui.theme.WearConstants
import com.example.reminders.wear.ui.theme.WearSpacing
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * A reminder card that supports swipe-to-complete and swipe-to-delete on Wear OS.
 *
 * Swipe right (past 50 % of card width) to complete the reminder — reveals a
 * green squircle with a check icon.  Swipe left (past 50 %) to delete — reveals
 * a red squircle with a trash icon.
 *
 * Because Wear Compose Material3 does not provide [SwipeToDismissBox],
 * this component implements the gesture manually with [detectHorizontalDragGestures]
 * and an [Animatable] offset.
 *
 * @param reminder   The reminder data to display.
 * @param onComplete Called once the card is swiped right past the threshold.
 * @param onDelete   Called once the card is swiped left past the threshold.
 * @param onClick    Called when the card is tapped without a horizontal drag.
 * @param modifier   Optional [Modifier].
 */
@Composable
fun SwipeableWatchReminderCard(
    reminder: WatchReminder,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var cardWidthPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size -> cardWidthPx = size.width.toFloat() }
    ) {
        val currentOffset = offsetX.value

        if (abs(currentOffset) > 0.5f) {
            WatchSwipeBackground(
                isCompleteAction = currentOffset > 0f,
                progress = (abs(currentOffset) / cardWidthPx).coerceIn(0f, 1f),
                modifier = Modifier.matchParentSize()
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(currentOffset.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(cardWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragCancel = {
                            coroutineScope.launch { offsetX.animateTo(0f) }
                            isDragging = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = (offsetX.value + dragAmount)
                                .coerceIn(-cardWidthPx, cardWidthPx)
                            coroutineScope.launch { offsetX.snapTo(newOffset) }
                        },
                        onDragEnd = {
                            val threshold = cardWidthPx * WearConstants.SwipeThresholdFraction
                            coroutineScope.launch {
                                when {
                                    offsetX.value > threshold -> {
                                        offsetX.animateTo(
                                            targetValue = cardWidthPx,
                                            animationSpec = tween(WearConstants.SwipeDismissDurationMs)
                                        )
                                        onComplete()
                                    }
                                    offsetX.value < -threshold -> {
                                        offsetX.animateTo(
                                            targetValue = -cardWidthPx,
                                            animationSpec = tween(WearConstants.SwipeDismissDurationMs)
                                        )
                                        onDelete()
                                    }
                                    else -> {
                                        offsetX.animateTo(0f)
                                        isDragging = false
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            TitleCard(
                onClick = { if (!isDragging) onClick() },
                title = {
                    Text(
                        text = reminder.title.ifBlank {
                            stringResource(R.string.no_title)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = WearConstants.MaxTitleLines,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                content = {
                    if (!reminder.body.isNullOrBlank()) {
                        Text(
                            text = reminder.body,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Background layer revealed behind a reminder card during a swipe gesture.
 *
 * - Swipe right: vivid green background with a white check icon.
 * - Swipe left:  vivid red background with a white delete icon.
 *
 * The background fades from transparent to full colour as the card slides away,
 * reaching full opacity right at the action threshold (50 % of card width).
 *
 * @param isCompleteAction `true` when swiping right (complete), `false` for left (delete).
 * @param progress        Fraction of the card width that has been swiped (0–1).
 * @param modifier        Optional [Modifier].
 */
@Composable
private fun WatchSwipeBackground(
    isCompleteAction: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    // Reaches full opacity at the action threshold (progress = 0.5 → 1.0 after coerce)
    val bgAlpha = (progress * 2f).coerceIn(0f, 1f)
    val iconAlpha = (progress * 2.5f).coerceIn(0f, 1f)

    val bgColor = if (isCompleteAction) CompleteBackground else DeleteBackground
    val icon = if (isCompleteAction) Icons.Filled.Check else Icons.Filled.Delete
    val contentDescription = if (isCompleteAction) {
        stringResource(R.string.complete)
    } else {
        stringResource(R.string.delete)
    }
    val alignment = if (isCompleteAction) Alignment.CenterStart else Alignment.CenterEnd

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(bgColor.copy(alpha = bgAlpha)),
        contentAlignment = alignment
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .padding(horizontal = WearSpacing.Lg)
                .size(WearConstants.SwipeIconSize),
            tint = Color.White.copy(alpha = iconAlpha)
        )
    }
}

private val CompleteBackground = Color(0xFF1DB954)  // Vibrant emerald green
private val DeleteBackground = Color(0xFFE53935)    // Vivid red
