package com.example.reminders.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reminders.R
import com.example.reminders.data.model.SavedPlace
import com.example.reminders.geocoding.GeocodingCandidate
import com.example.reminders.ui.viewmodel.AddPlaceState
import com.example.reminders.ui.viewmodel.SavedPlacesViewModel

/**
 * Screen that lists saved places and allows adding new ones.
 *
 * Free users are capped at [SavedPlacesViewModel.FREE_TIER_SAVED_PLACES_CAP]
 * saved places. When the cap is reached an upgrade CTA is shown instead of
 * the add dialog.
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_saved_place)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

@Composable
private fun EmptySavedPlaces() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_saved_places),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SavedPlacesList(
    places: List<SavedPlace>,
    onDelete: (SavedPlace) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 88.dp
        )
    ) {
        items(places, key = { it.id }) { place ->
            SavedPlaceRow(
                place = place,
                onDelete = { onDelete(place) }
            )
        }
    }
}

@Composable
private fun SavedPlaceRow(
    place: SavedPlace,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                contentDescription = stringResource(R.string.delete_saved_place, place.label),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

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
                        CircularProgressIndicator()
                    }
                },
                confirmButton = {}
            )
        }

        is AddPlaceState.AmbiguousAddress -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.multiple_locations_found)) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(addPlaceState.candidates) { candidate ->
                            TextButton(onClick = {
                                onConfirmCandidate(addPlaceState.label, candidate)
                            }) {
                                Text(
                                    text = candidate.displayAddress,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = label,
                                onValueChange = { label = it },
                                label = { Text(stringResource(R.string.place_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text(stringResource(R.string.place_address)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
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
