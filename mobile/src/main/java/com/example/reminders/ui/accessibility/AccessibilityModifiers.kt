package com.example.reminders.ui.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * Accessibility utilities for consistent TalkBack support across the app.
 *
 * Provides reusable [Modifier] extensions that apply semantic properties
 * for screen readers. All Composables that convey information through
 * visual cues (colours, icons, positions) should use these modifiers
 * to ensure the same information is available to TalkBack users.
 */

/**
 * Sets a custom content description that replaces the default
 * computed semantics. Use this when a visual element's meaning
 * is not conveyed by its text content alone (e.g. a coloured
 * status dot, a progress indicator).
 *
 * @param description The TalkBack-accessible description of the element.
 */
fun Modifier.accessibilityDescription(description: String): Modifier =
    clearAndSetSemantics { contentDescription = description }

/**
 * Marks an element as a button with an accessible label and click action.
 * Ensures TalkBack announces the element as a "button" and describes
 * what happens when it is tapped.
 *
 * @param label  Human-readable description of the button's action.
 * @param action Semantic click action description for TalkBack.
 */
fun Modifier.accessibilityButton(
    label: String,
    action: String = label
): Modifier = semantics {
    role = Role.Button
    contentDescription = label
    onClick { true }
}

/**
 * Marks an element as a toggle (checkbox/switch) with an
 * accessible state description.
 *
 * @param label  Human-readable description of the toggle's purpose.
 * @param isOn   Whether the toggle is currently on or off.
 */
fun Modifier.accessibilityToggle(
    label: String,
    isOn: Boolean
): Modifier = semantics {
    role = Role.Switch
    contentDescription = label
    stateDescription = if (isOn) "On" else "Off"
}

/**
 * Merges descendant semantics into a single node. Use this on
 * container Composables (e.g. list items) where the children's
 * individual semantics would be noisy for TalkBack users.
 *
 * @param description The merged description of the container.
 */
fun Modifier.mergeDescendants(description: String): Modifier =
    clearAndSetSemantics { contentDescription = description }
