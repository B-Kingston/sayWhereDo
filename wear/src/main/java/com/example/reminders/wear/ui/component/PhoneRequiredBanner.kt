package com.example.reminders.wear.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.example.reminders.wear.R
import com.example.reminders.wear.ui.theme.WearConstants

/**
 * A subtle banner shown when the companion phone is not connected.
 *
 * Uses the theme surface-container colours for a consistent dark
 * appearance with rounded corners and a small border effect.
 */
@Composable
fun PhoneRequiredBanner(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.shapes.small.topStart))
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(MaterialTheme.shapes.small.topStart)
            )
            .padding(WearConstants.BannerInnerPadding)
    ) {
        Text(
            text = stringResource(R.string.phone_required_banner),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
