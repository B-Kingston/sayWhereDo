package com.example.reminders.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.reminders.R
import com.example.reminders.geocoding.GeocodingCandidate
import com.example.reminders.ui.theme.Spacing

/**
 * Screen shown when geocoding returns multiple ambiguous results.
 *
 * Displays a list of candidate addresses so the user can pick the
 * correct one. After confirmation the user is offered the option to
 * save the resolved location as a [SavedPlace][com.example.reminders.data.model.SavedPlace].
 *
 * @param candidates      The disambiguation candidates from the geocoder.
 * @param onSelect        Callback with the user-selected candidate.
 * @param onBack          Callback when the user aborts the flow.
 * @param onSaveAsPlace   Optional callback to save the selected candidate
 *                        as a named saved place (label provided by user).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeocodingConfirmationScreen(
    candidates: List<GeocodingCandidate>,
    onSelect: (GeocodingCandidate) -> Unit,
    onBack: () -> Unit,
    onSaveAsPlace: ((GeocodingCandidate) -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.confirm_location)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = PaddingValues(Spacing.md)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.multiple_locations_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                }

                items(candidates) { candidate ->
                    CandidateCard(
                        candidate = candidate,
                        onSelect = { onSelect(candidate) },
                        onSaveAsPlace = onSaveAsPlace?.let { callback ->
                            { callback(candidate) }
                        }
                    )
                }
            }
        }
    }
}

/**
 * A styled card representing a single geocoding candidate.
 *
 * Shows the address with a location icon, a primary "Select" button,
 * and an optional "Save as place" text button.
 */
@Composable
private fun CandidateCard(
    candidate: GeocodingCandidate,
    onSelect: () -> Unit,
    onSaveAsPlace: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.weight(0.05f))
                Text(
                    text = candidate.displayAddress,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Button(
                    onClick = onSelect,
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.select_this_location))
                }

                if (onSaveAsPlace != null) {
                    OutlinedButton(
                        onClick = onSaveAsPlace,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.save_as_place))
                    }
                }
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of the geocoding confirmation screen. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun GeocodingConfirmationPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        GeocodingConfirmationScreen(
            candidates = listOf(
                GeocodingCandidate(
                    displayAddress = "123 Main Street, Springfield, IL",
                    latitude = 39.7817,
                    longitude = -89.6501
                ),
                GeocodingCandidate(
                    displayAddress = "456 Oak Avenue, Shelbyville, IL",
                    latitude = 39.8053,
                    longitude = -89.7770
                )
            ),
            onSelect = {},
            onBack = {},
            onSaveAsPlace = {}
        )
    }
}
