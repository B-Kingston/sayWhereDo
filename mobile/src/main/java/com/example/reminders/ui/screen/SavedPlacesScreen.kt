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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reminders.R
import com.example.reminders.data.model.SavedPlace
import com.example.reminders.geocoding.GeocodingCandidate
import com.example.reminders.ui.theme.Spacing
import com.example.reminders.ui.theme.UiConstants
import com.example.reminders.ui.viewmodel.AddPlaceState
import com.example.reminders.ui.viewmodel.SavedPlacesViewModel

/**
 * Screen that lists saved places and allows adding new ones.
 *
 * Free users are capped at [SavedPlacesViewModel.FREE_TIER_SAVED_PLACES_CAP]
 * saved places. When the cap is reached an upgrade CTA is shown instead of
 * the add dialog.
 *
 * @param viewModel      The [SavedPlacesViewModel] driving UI state.
 * @param onBack         Callback to navigate up.
 * @param onUpgradeToPro Callback to launch the Pro upgrade flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlacesScreen(
    viewModel: SavedPlacesViewModel,
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit
) {
    val savedPlaces by viewModel.savedPlaces.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val addPlaceState by viewModel.addPlaceState.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saved_places_title)) },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_saved_place)
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            if (savedPlaces.isEmpty()) {
                EmptySavedPlaces()
            } else {
                SavedPlacesList(
                    places = savedPlaces,
                    onDelete = { viewModel.deletePlace(it) }
                )
            }
        }
    }

    if (showDialog) {
        AddSavedPlaceDialog(
            addPlaceState = addPlaceState,
            isPro = isPro,
            currentPlaceCount = savedPlaces.size,
            onDismiss = {
                showDialog = false
                viewModel.resetAddPlaceState()
            },
            onAdd = { label, address -> viewModel.addPlace(label, address) },
            onConfirmCandidate = { label, candidate ->
                viewModel.confirmCandidate(label, candidate)
            },
            onUpgrade = onUpgradeToPro
        )
    }
}

/**
 * Beautiful empty-state view for the saved places screen.
 */
@Composable
private fun EmptySavedPlaces() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(UiConstants.EMPTY_STATE_ICON_SIZE_DP.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.no_saved_places),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = stringResource(R.string.saved_places_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * LazyColumn of saved place cards.
 */
@Composable
private fun SavedPlacesList(
    places: List<SavedPlace>,
    onDelete: (SavedPlace) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.sm,
            bottom = UiConstants.SAVED_PLACES_LIST_BOTTOM_PADDING_DP.dp
        )
    ) {
        items(places, key = { it.id }) { place ->
            SavedPlaceCard(
                place = place,
                onDelete = { onDelete(place) }
            )
        }
    }
}

/**
 * A card displaying a saved place with its label, address, and delete action.
 */
@Composable
private fun SavedPlaceCard(
    place: SavedPlace,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.weight(0.04f))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = place.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(
                        R.string.delete_saved_place,
                        place.label
                    ),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Multi-state dialog for adding a new saved place.
 *
 * Handles: Idle form, cap reached, geocoding, ambiguous results,
 * error, and success states.
 */
@Composable
private fun AddSavedPlaceDialog(
    addPlaceState: AddPlaceState,
    isPro: Boolean,
    currentPlaceCount: Int,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onConfirmCandidate: (String, GeocodingCandidate) -> Unit,
    onUpgrade: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    val freeCap = SavedPlacesViewModel.FREE_TIER_SAVED_PLACES_CAP

    when (addPlaceState) {
        is AddPlaceState.CapReached -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.saved_places_limit_reached_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.saved_places_limit_reached_message,
                            freeCap
                        )
                    )
                },
                shape = MaterialTheme.shapes.large,
                confirmButton = {
                    TextButton(onClick = {
                        onDismiss()
                        onUpgrade()
                    }) {
                        Text(stringResource(R.string.upgrade_to_pro))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dismiss))
                    }
                }
            )
        }

        is AddPlaceState.Geocoding -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.geocoding_address)) },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                },
                shape = MaterialTheme.shapes.large,
                confirmButton = {}
            )
        }

        is AddPlaceState.AmbiguousAddress -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.multiple_locations_found)) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        items(addPlaceState.candidates) { candidate ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                TextButton(
                                    onClick = {
                                        onConfirmCandidate(addPlaceState.label, candidate)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = candidate.displayAddress,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                },
                shape = MaterialTheme.shapes.large,
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        is AddPlaceState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.geocoding_error)) },
                text = { Text(addPlaceState.message) },
                shape = MaterialTheme.shapes.large,
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dismiss))
                    }
                }
            )
        }

        is AddPlaceState.Success -> {
            onDismiss()
        }

        is AddPlaceState.Idle -> {
            val isAtCap = !isPro && currentPlaceCount >= freeCap
            if (isAtCap) {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(stringResource(R.string.saved_places_limit_reached_title)) },
                    text = {
                        Text(
                            stringResource(
                                R.string.saved_places_limit_reached_message,
                                freeCap
                            )
                        )
                    },
                    shape = MaterialTheme.shapes.large,
                    confirmButton = {
                        TextButton(onClick = {
                            onDismiss()
                            onUpgrade()
                        }) {
                            Text(stringResource(R.string.upgrade_to_pro))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.dismiss))
                        }
                    }
                )
            } else {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(stringResource(R.string.add_saved_place)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedTextField(
                                value = label,
                                onValueChange = { label = it },
                                label = { Text(stringResource(R.string.place_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            )
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text(stringResource(R.string.place_address)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    },
                    shape = MaterialTheme.shapes.large,
                    confirmButton = {
                        TextButton(
                            onClick = { onAdd(label, address) },
                            enabled = label.isNotBlank() && address.isNotBlank()
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of the saved places screen. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SavedPlacesPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        Surface {
            Text("Saved places screen — requires ViewModel")
        }
    }
}
