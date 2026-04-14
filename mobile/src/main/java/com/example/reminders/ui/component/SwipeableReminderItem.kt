package com.example.reminders.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.reminders.R
import com.example.reminders.data.model.Reminder
import com.example.reminders.ui.theme.Spacing
import com.example.reminders.ui.theme.UiConstants
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * A list item that reveals action backgrounds when swiped horizontally.
 *
 * - Swipe right (50 % threshold) → green squircle with check icon → completes the reminder.
 * - Swipe left  (50 % threshold) → red squircle with delete icon → deletes the reminder.
 *
 * Uses a custom gesture implementation so that the coloured background
 * and icon badge appear progressively as the user drags, giving
 * immediate visual feedback well before the 50 % threshold.
 *
 * @param reminder    The reminder to display.
 * @param onComplete  Called when the user swipes right past the threshold.
 * @param onDelete    Called when the user swipes left past the threshold.
 * @param onClick     Called when the user taps the card.
 * @param modifier    Optional [Modifier].
 */
@Composable
fun SwipeableReminderItem(
    reminder: Reminder,
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
            MobileSwipeBackground(
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
                            val threshold = cardWidthPx * UiConstants.SWIPE_THRESHOLD_FRACTION
                            coroutineScope.launch {
                                when {
                                    offsetX.value > threshold -> {
                                        offsetX.animateTo(
                                            targetValue = cardWidthPx,
                                            animationSpec = tween(DismissDurationMs)
                                        )
                                        onComplete()
                                    }
                                    offsetX.value < -threshold -> {
                                        offsetX.animateTo(
                                            targetValue = -cardWidthPx,
                                            animationSpec = tween(DismissDurationMs)
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
            ReminderCard(
                reminder = reminder,
                onClick = { if (!isDragging) onClick() }
            )
        }
    }
}

/**
 * Split-colour background revealed behind a card during a swipe.
 *
 * The left half is tinted green (complete), the right half red (delete).
 * As the foreground card slides in either direction, the matching side
 * is progressively revealed together with a squircle icon badge whose
 * opacity scales with swipe progress.
 *
 * @param isCompleteAction `true` when swiping right (complete), `false` for left (delete).
 * @param progress        Fraction of card width swiped (0–1).
 * @param modifier        Optional [Modifier].
 */
@Composable
private fun MobileSwipeBackground(
    isCompleteAction: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    // Reach full opacity at the action threshold so the intent is unmistakable
    val bgAlpha = (progress * 2f).coerceIn(0f, 1f)
    val contentAlpha = (progress * 2.5f).coerceIn(0f, 1f)

    val bgColor = if (isCompleteAction) SwipeCompleteColor else SwipeDeleteColor
    val icon = if (isCompleteAction) Icons.Filled.Check else Icons.Filled.Delete
    val contentDescription = if (isCompleteAction) {
        stringResource(R.string.content_desc_complete_action)
    } else {
        stringResource(R.string.content_desc_delete_action)
    }
    val alignment = if (isCompleteAction) Alignment.CenterStart else Alignment.CenterEnd

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(bgColor.copy(alpha = bgAlpha)),
        contentAlignment = alignment
    ) {
        ActionIconBadge(
            icon = icon,
            contentDescription = contentDescription,
            contentAlpha = contentAlpha
        )
    }
}

/**
 * A circular badge containing a white action icon.
 *
 * Used inside the swipe-revealed background to provide a clear visual
 * cue for the pending action (complete or delete).
 *
 * @param icon               The icon to display.
 * @param contentDescription Accessibility description for the icon.
 * @param contentAlpha       Opacity for the badge and icon (scales with swipe progress).
 */
@Composable
private fun ActionIconBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    contentAlpha: Float = 1f
) {
    Box(
        modifier = Modifier
            .padding(horizontal = Spacing.md)
            .size(UiConstants.SWIPE_ICON_BADGE_SIZE_DP.dp)
            .clip(RoundedCornerShape(percent = UiConstants.SWIPE_ICON_BADGE_CORNER_PERCENT))
            .background(Color.White.copy(alpha = 0.25f * contentAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(UiConstants.SWIPE_ICON_SIZE_DP.dp),
            tint = Color.White.copy(alpha = contentAlpha)
        )
    }
}

/**
 * Material 3 card that displays a single reminder's title and body.
 *
 * @param reminder The data to render.
 * @param onClick  Callback when the card is tapped.
 */
@Composable
private fun ReminderCard(
    reminder: Reminder,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = UiConstants.REMINDER_CARD_ELEVATION_DP.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = reminder.title.ifBlank {
                    stringResource(R.string.no_reminders_yet)
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!reminder.body.isNullOrBlank()) {
                Text(
                    text = reminder.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val SwipeCompleteColor = Color(0xFF1DB954)  // Vibrant emerald green
private val SwipeDeleteColor = Color(0xFFE53935)    // Vivid red
private const val DismissDurationMs = 200
