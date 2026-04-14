package com.example.reminders.ui.theme

/**
 * Centralised UI constants for the Reminders app.
 *
 * Every magic number that was previously scattered across screen and
 * component composables is extracted here as a named constant. Use
 * these instead of inline literals.
 */
object UiConstants {

    // ── RecordButton ──────────────────────────────────────────────
    /** Diameter of the large circular record button. */
    const val RECORD_BUTTON_SIZE_DP = 88

    /** Diameter of the icon inside the record button. */
    const val RECORD_ICON_SIZE_DP = 36

    /** Scale factor for the pulse animation (1.0 → 1.12). */
    const val PULSE_TARGET_SCALE = 1.12f

    /** Duration in milliseconds for one pulse cycle. */
    const val PULSE_DURATION_MS = 800

    // ── StatusIndicator ───────────────────────────────────────────
    /** Diameter of the status dot. */
    const val STATUS_DOT_SIZE_DP = 8

    /** Spacing between the status dot and its label. */
    const val STATUS_DOT_LABEL_SPACING_DP = 6

    // ── ProBadge ──────────────────────────────────────────────────
    /** Corner radius for the PRO badge. */
    const val PRO_BADGE_CORNER_RADIUS_DP = 4

    /** Horizontal padding inside the PRO badge. */
    const val PRO_BADGE_HORIZONTAL_PADDING_DP = 6

    /** Vertical padding inside the PRO badge. */
    const val PRO_BADGE_VERTICAL_PADDING_DP = 2

    // ── AddNoteFab ────────────────────────────────────────────────
    /** Horizontal spacing between the keyboard and mic FABs. */
    const val FAB_SPACING_DP = 12

    // ── KeyboardInput ─────────────────────────────────────────────
    /** Maximum visible lines in the keyboard input field. */
    const val KEYBOARD_MAX_INPUT_LINES = 4

    // ── ReminderEdit ──────────────────────────────────────────────
    /** Minimum geofence radius in metres. */
    const val MIN_RADIUS_METRES = 50

    /** Maximum geofence radius in metres. */
    const val MAX_RADIUS_METRES = 1000

    // ── Settings ──────────────────────────────────────────────────
    /** Weight multiplier for the feature column in the comparison table. */
    const val FEATURE_TABLE_FEATURE_WEIGHT = 2f

    /** Weight multiplier for the Free/Pro value columns. */
    const val FEATURE_TABLE_VALUE_WEIGHT = 1f

    /** Horizontal spacing between a button and a ProBadge. */
    const val PRO_BADGE_SPACING_DP = 8

    /** Padding between a button icon (e.g. spinner) and its label. */
    const val BUTTON_ICON_PADDING_DP = 4

    // ── ErrorStateView ────────────────────────────────────────────
    /** Size of the error illustration icon. */
    const val ERROR_ICON_SIZE_DP = 48

    // ── Paywall ───────────────────────────────────────────────────
    /** Size of the star icon on the paywall. */
    const val PAYWALL_ICON_SIZE_DP = 48

    /** Size of the check icon for benefit rows. */
    const val PAYWALL_CHECK_ICON_SIZE_DP = 20

    // ── SavedPlaces ───────────────────────────────────────────────
    /** Bottom padding for the LazyColumn to clear the FAB area. */
    const val SAVED_PLACES_LIST_BOTTOM_PADDING_DP = 88

    // ── Skeleton / Loading ────────────────────────────────────────
    /** Number of skeleton rows shown while loading reminders. */
    const val SKELETON_ROW_COUNT = 5

    /** Height of a single skeleton row. */
    const val SKELETON_ROW_HEIGHT_DP = 72

    /** Width fraction for skeleton shimmer animation. */
    const val SKELETON_SHIMMER_WIDTH_FRACTION = 0.6f

    // ── Radio / Toggle ────────────────────────────────────────────
    /** Size of the radio indicator circle. */
    const val RADIO_INDICATOR_SIZE_DP = 20

    /** Border width for an unselected radio indicator. */
    const val RADIO_BORDER_WIDTH_DP = 2

    // ── Empty state ───────────────────────────────────────────────
    /** Size of the empty-state illustration icon. */
    const val EMPTY_STATE_ICON_SIZE_DP = 64

    // ── Swipe actions ─────────────────────────────────────────────
    /** Fraction of card width required to trigger a swipe action. */
    const val SWIPE_THRESHOLD_FRACTION = 0.5f

    /** Size of the action icon badge (circular container). */
    const val SWIPE_ICON_BADGE_SIZE_DP = 52

    /** Corner radius percent for the circular icon badge (50 = perfect circle). */
    const val SWIPE_ICON_BADGE_CORNER_PERCENT = 50

    /** Size of the icon inside the swipe action badge. */
    const val SWIPE_ICON_SIZE_DP = 28

    /** Bottom padding for the reminder list to clear the FAB area. */
    const val REMINDER_LIST_BOTTOM_PADDING_DP = 96

    /** Elevation for reminder cards in the list. */
    const val REMINDER_CARD_ELEVATION_DP = 1
}
