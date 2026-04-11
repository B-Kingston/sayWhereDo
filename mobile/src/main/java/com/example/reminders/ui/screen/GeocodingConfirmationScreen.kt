package com.example.reminders.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reminders.R
import com.example.reminders.geocoding.GeocodingCandidate

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
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.multiple_locations_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
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

@Composable
private fun CandidateCard(
    candidate: GeocodingCandidate,
    onSelect: () -> Unit,
    onSaveAsPlace: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = candidate.displayAddress,
                style = MaterialTheme.typography.bodyLarge
            )

            if (onSaveAsPlace != null) {
                TextButton(onClick = onSaveAsPlace) {
                    Text(stringResource(R.string.save_as_place))
                }
            }

            TextButton(onClick = onSelect) {
                Text(stringResource(R.string.select_this_location))
            }
        }
    }
}
