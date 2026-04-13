package com.example.reminders.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reminders.R
import com.example.reminders.data.preferences.UserPreferences
import com.example.reminders.formatting.AiProviderPresets
import kotlinx.coroutines.launch

/**
 * Unified AI model settings section for the Settings screen.
 *
 * Lets the user choose between cloud-based and on-device formatting backends,
 * select a cloud provider preset (or configure a custom endpoint), and manage
 * their API key. On-device formatting is shown as "coming soon".
 *
 * @param userPreferences The [UserPreferences] instance used to read and write
 *                        all AI-related preference values.
 * @param modifier        Optional modifier applied to the root column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiModelSettingsSection(
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val formattingBackend by userPreferences.formattingBackend
        .collectAsStateWithLifecycle(initialValue = "cloud")
    val aiProviderId by userPreferences.aiProviderId
        .collectAsStateWithLifecycle(initialValue = "gemini")
    val aiBaseUrl by userPreferences.aiBaseUrl
        .collectAsStateWithLifecycle(initialValue = "")
    val aiModelName by userPreferences.aiModelName
        .collectAsStateWithLifecycle(initialValue = "")
    val apiKey by userPreferences.apiKey
        .collectAsStateWithLifecycle(initialValue = null)

    var apiKeyInput by rememberSaveable { mutableStateOf(apiKey ?: "") }
    var isKeyVisible by rememberSaveable { mutableStateOf(false) }
    var isProviderExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.ai_model_settings_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = SECTION_TITLE_BOTTOM_PADDING)
        )

        // ---------- Backend selector ----------
        Row(
            horizontalArrangement = Arrangement.spacedBy(BACKEND_OPTION_SPACING),
            modifier = Modifier.fillMaxWidth()
        ) {
            BackendOption(
                label = stringResource(R.string.ai_model_settings_cloud),
                isSelected = formattingBackend == BACKEND_CLOUD,
                onClick = {
                    scope.launch { userPreferences.setFormattingBackend(BACKEND_CLOUD) }
                },
                modifier = Modifier.weight(BACKEND_OPTION_WEIGHT)
            )
            BackendOption(
                label = stringResource(R.string.ai_model_settings_local),
                isSelected = formattingBackend == BACKEND_LOCAL,
                onClick = {
                    scope.launch { userPreferences.setFormattingBackend(BACKEND_LOCAL) }
                },
                modifier = Modifier.weight(BACKEND_OPTION_WEIGHT)
            )
        }

        Spacer(Modifier.height(SECTION_SPACING))

        // ---------- Cloud API configuration ----------
        if (formattingBackend == BACKEND_CLOUD) {
            CloudApiSection(
                userPreferences = userPreferences,
                scope = scope,
                aiProviderId = aiProviderId,
                aiBaseUrl = aiBaseUrl,
                aiModelName = aiModelName,
                apiKeyInput = apiKeyInput,
                isKeyVisible = isKeyVisible,
                isProviderExpanded = isProviderExpanded,
                onApiKeyInputChange = { apiKeyInput = it },
                onKeyVisibilityToggle = { isKeyVisible = !isKeyVisible },
                onProviderExpandedChange = { isProviderExpanded = it }
            )
        }

        // ---------- On-device model ----------
        if (formattingBackend == BACKEND_LOCAL) {
            LocalModelSection()
        }
    }
}

/**
 * A single selectable option in the backend toggle row.
 *
 * Renders a label with a filled or outlined circle indicator depending on
 * whether [isSelected] is true.
 */
@Composable
private fun BackendOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val indicatorModifier = if (isSelected) {
        Modifier
            .size(RADIO_SIZE)
            .clip(CircleShape)
            .background(indicatorColor)
    } else {
        Modifier
            .size(RADIO_SIZE)
            .clip(CircleShape)
            .border(RADIO_BORDER_WIDTH, indicatorColor, CircleShape)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(RADIO_ROW_PADDING)
    ) {
        Box(modifier = indicatorModifier)
        Spacer(Modifier.width(RADIO_LABEL_SPACING))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Cloud API configuration fields: provider dropdown, base URL, model name,
 * API key, and a privacy info card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudApiSection(
    userPreferences: UserPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    aiProviderId: String,
    aiBaseUrl: String,
    aiModelName: String,
    apiKeyInput: String,
    isKeyVisible: Boolean,
    isProviderExpanded: Boolean,
    onApiKeyInputChange: (String) -> Unit,
    onKeyVisibilityToggle: () -> Unit,
    onProviderExpandedChange: (Boolean) -> Unit
) {
    val currentProvider = AiProviderPresets.getById(aiProviderId)
        ?: AiProviderPresets.GEMINI

    // ---------- Provider dropdown ----------
    ExposedDropdownMenuBox(
        expanded = isProviderExpanded,
        onExpandedChange = onProviderExpandedChange
    ) {
        OutlinedTextField(
            value = currentProvider.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.ai_model_settings_provider_label)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProviderExpanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = isProviderExpanded,
            onDismissRequest = { onProviderExpandedChange(false) }
        ) {
            AiProviderPresets.ALL.forEach { provider ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(provider.name) },
                    onClick = {
                        scope.launch {
                            userPreferences.setAiProviderId(provider.id)
                            userPreferences.setAiBaseUrl(provider.baseUrl)
                            userPreferences.setAiModelName(provider.defaultModel)
                        }
                        onProviderExpandedChange(false)
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(FIELD_SPACING))

    // ---------- Base URL ----------
    OutlinedTextField(
        value = aiBaseUrl,
        onValueChange = { newUrl ->
            scope.launch { userPreferences.setAiBaseUrl(newUrl) }
        },
        label = { Text(stringResource(R.string.ai_model_settings_url_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(FIELD_SPACING))

    // ---------- Model name ----------
    OutlinedTextField(
        value = aiModelName,
        onValueChange = { newModel ->
            scope.launch { userPreferences.setAiModelName(newModel) }
        },
        label = { Text(stringResource(R.string.ai_model_settings_model_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(FIELD_SPACING))

    // ---------- API key (hidden for providers that don't require one) ----------
    if (currentProvider.requiresApiKey) {
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = onApiKeyInputChange,
            label = { Text(stringResource(R.string.ai_model_settings_api_key_label)) },
            placeholder = { Text(stringResource(R.string.settings_api_key_placeholder)) },
            singleLine = true,
            visualTransformation = if (isKeyVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = onKeyVisibilityToggle) {
                    Icon(
                        imageVector = if (isKeyVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (isKeyVisible) {
                            stringResource(R.string.settings_hide_api_key)
                        } else {
                            stringResource(R.string.settings_show_api_key)
                        }
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    scope.launch {
                        userPreferences.setApiKey(apiKeyInput.trim())
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(FIELD_SPACING))
    }

    // ---------- Privacy info card ----------
    PrivacyInfoCard(provider = currentProvider)
}

/**
 * Card displaying privacy metadata for the selected cloud provider.
 */
@Composable
private fun PrivacyInfoCard(provider: com.example.reminders.formatting.AiProvider) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(PRIVACY_CARD_PADDING)) {
            Text(
                text = stringResource(R.string.ai_model_settings_privacy_title),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = PRIVACY_CARD_INNER_SPACING)
            )
            Text(
                text = provider.privacyNote,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(PRIVACY_CARD_INNER_SPACING))
            Text(
                text = "Retention: ${provider.dataRetention}",
                style = MaterialTheme.typography.bodySmall
            )
            if (provider.hasFreeTier) {
                Spacer(Modifier.height(PRIVACY_CARD_INNER_SPACING))
                Text(
                    text = "Free tier available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Placeholder section shown when the on-device backend is selected.
 */
@Composable
private fun LocalModelSection() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(COMING_SOON_INDICATOR_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(COMING_SOON_TEXT_SPACING))
        Text(
            text = stringResource(R.string.ai_model_settings_local_coming_soon),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(Modifier.height(FIELD_SPACING))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(PRIVACY_CARD_PADDING)) {
            Text(
                text = stringResource(R.string.ai_model_settings_local_privacy),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/** Backend preference constants. */
private const val BACKEND_CLOUD = "cloud"
private const val BACKEND_LOCAL = "local"

/** Dimension constants. */
private val SECTION_TITLE_BOTTOM_PADDING = 8.dp
private val SECTION_SPACING = 16.dp
private val BACKEND_OPTION_SPACING = 16.dp
private const val BACKEND_OPTION_WEIGHT = 1f
private val RADIO_SIZE = 20.dp
private val RADIO_BORDER_WIDTH = 2.dp
private val RADIO_ROW_PADDING = 8.dp
private val RADIO_LABEL_SPACING = 8.dp
private val FIELD_SPACING = 12.dp
private val PRIVACY_CARD_PADDING = 12.dp
private val PRIVACY_CARD_INNER_SPACING = 4.dp
private val COMING_SOON_INDICATOR_SIZE = 12.dp
private val COMING_SOON_TEXT_SPACING = 8.dp
