package uk.ewancroft.inkwell.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import uk.ewancroft.inkwell.shared.theme.SharedReaderTheme
import uk.ewancroft.inkwell.util.AccessibilityPreferences
import uk.ewancroft.inkwell.util.CustomisationPreferences

/**
 * The app's actual settings surface: notifications, legal, and about, in
 * one place. Previously scattered -- notifications had no on/off switch
 * anywhere in the app (only the OS permission prompt), and Legal/About
 * were each their own separate dialog with no common home. Mirrors iOS
 * SettingsView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    appVersion: String,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    var legalDocument by remember { mutableStateOf<LegalDocumentType?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var isConfirmingSignOut by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var isUnlocked by remember { mutableStateOf(CustomisationPreferences.isUnlocked(context)) }
    var licenseKeyInput by remember { mutableStateOf("") }
    var licenseKeyError by remember { mutableStateOf(false) }
    var accentColorHex by remember { mutableStateOf(CustomisationPreferences.getAccentColorHex(context)) }
    var fontFamilyOverride by remember { mutableStateOf(CustomisationPreferences.getFontFamilyOverride(context)) }
    var appearanceOverride by remember { mutableStateOf(CustomisationPreferences.getAppearanceOverride(context)) }

    var fontSizeScale by remember { mutableStateOf(AccessibilityPreferences.getFontSizeScale(context)) }
    var boldText by remember { mutableStateOf(AccessibilityPreferences.getBoldText(context)) }
    var increaseContrast by remember { mutableStateOf(AccessibilityPreferences.getIncreaseContrast(context)) }
    var underlineLinks by remember { mutableStateOf(AccessibilityPreferences.getUnderlineLinks(context)) }

    legalDocument?.let { documentType ->
        LegalDocumentDialog(documentType = documentType, onDismiss = { legalDocument = null })
    }

    if (showAbout) {
        CreditsView(appVersion = appVersion, onSignOut = onSignOut, onDismiss = { showAbout = false })
    }

    if (isConfirmingSignOut) {
        AlertDialog(
            onDismissRequest = { isConfirmingSignOut = false },
            title = { Text("Sign out of Inkwell?") },
            text = { Text("Your publications and subscriptions stay in your PDS. You can sign back in at any time.") },
            confirmButton = {
                TextButton(onClick = {
                    isConfirmingSignOut = false
                    onSignOut()
                }) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { isConfirmingSignOut = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(top = 32.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Settings") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())) {
                    SectionHeader("Notifications", modifier = Modifier.padding(top = 8.dp))
                    SettingsRow(
                        title = "New Document Notifications",
                        trailing = {
                            Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsEnabledChange)
                        },
                    )
                    Text(
                        "Inkwell polls your subscriptions in the background and notifies you about new documents. Turning this off keeps the in-app notification list working but stops system banners. The system permission (below) controls whether banners can appear at all.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    SettingsRow(
                        title = "Open System Notification Settings",
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SectionHeader("Accessibility")
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text("Text Size", style = MaterialTheme.typography.bodyLarge)
                        Slider(
                            value = fontSizeScale,
                            onValueChange = {
                                fontSizeScale = it
                                AccessibilityPreferences.setFontSizeScale(context, it)
                            },
                            valueRange = 0.8f..1.5f,
                            steps = 6,
                        )
                    }
                    SettingsRow(
                        title = "Bold Text",
                        trailing = {
                            Switch(
                                checked = boldText,
                                onCheckedChange = {
                                    boldText = it
                                    AccessibilityPreferences.setBoldText(context, it)
                                },
                            )
                        },
                    )
                    SettingsRow(
                        title = "Increase Contrast",
                        trailing = {
                            Switch(
                                checked = increaseContrast,
                                onCheckedChange = {
                                    increaseContrast = it
                                    AccessibilityPreferences.setIncreaseContrast(context, it)
                                },
                            )
                        },
                    )
                    SettingsRow(
                        title = "Underline Links",
                        trailing = {
                            Switch(
                                checked = underlineLinks,
                                onCheckedChange = {
                                    underlineLinks = it
                                    AccessibilityPreferences.setUnderlineLinks(context, it)
                                },
                            )
                        },
                    )
                    SettingsRow(
                        title = "Reset to Defaults",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            fontSizeScale = 1.0f
                            boldText = false
                            increaseContrast = false
                            underlineLinks = true
                            AccessibilityPreferences.resetToDefaults(context)
                        },
                    )
                    Text(
                        "These apply on top of your device's own font size and accessibility settings, and are always free.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (isUnlocked) {
                        SectionHeader("Customisation")
                        Text(
                            "Accent Color",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            for (hex in ACCENT_SWATCHES) {
                                ColorSwatch(
                                    hex = hex,
                                    selected = accentColorHex.equals(hex, ignoreCase = true),
                                    onClick = {
                                        accentColorHex = hex
                                        CustomisationPreferences.setAccentColorHex(context, hex)
                                    },
                                )
                            }
                        }
                        Text(
                            "Reading Font",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (family in SharedReaderTheme.FontFamily.entries) {
                                FilterChip(
                                    selected = fontFamilyOverride == family,
                                    onClick = {
                                        fontFamilyOverride = family
                                        CustomisationPreferences.setFontFamilyOverride(context, family)
                                    },
                                    label = { Text(family.name) },
                                )
                            }
                        }
                        Text(
                            "Appearance",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = appearanceOverride == null,
                                onClick = {
                                    appearanceOverride = null
                                    CustomisationPreferences.setAppearanceOverride(context, null)
                                },
                                label = { Text("System") },
                            )
                            FilterChip(
                                selected = appearanceOverride == CustomisationPreferences.AppearanceOverride.LIGHT,
                                onClick = {
                                    appearanceOverride = CustomisationPreferences.AppearanceOverride.LIGHT
                                    CustomisationPreferences.setAppearanceOverride(context, CustomisationPreferences.AppearanceOverride.LIGHT)
                                },
                                label = { Text("Light") },
                            )
                            FilterChip(
                                selected = appearanceOverride == CustomisationPreferences.AppearanceOverride.DARK,
                                onClick = {
                                    appearanceOverride = CustomisationPreferences.AppearanceOverride.DARK
                                    CustomisationPreferences.setAppearanceOverride(context, CustomisationPreferences.AppearanceOverride.DARK)
                                },
                                label = { Text("Dark") },
                            )
                        }
                        SettingsRow(
                            title = "Reset to Defaults",
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                accentColorHex = null
                                fontFamilyOverride = null
                                appearanceOverride = null
                                CustomisationPreferences.setAccentColorHex(context, null)
                                CustomisationPreferences.setFontFamilyOverride(context, null)
                                CustomisationPreferences.setAppearanceOverride(context, null)
                            },
                        )
                        Text(
                            "Overrides apply everywhere, including publications that set their own theme.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    } else {
                        SectionHeader("Customisation")
                        OutlinedTextField(
                            value = licenseKeyInput,
                            onValueChange = { licenseKeyInput = it },
                            label = { Text("License Key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        SettingsRow(
                            title = "Unlock",
                            onClick = {
                                if (CustomisationPreferences.unlock(context, licenseKeyInput)) {
                                    isUnlocked = true
                                    licenseKeyError = false
                                    licenseKeyInput = ""
                                } else {
                                    licenseKeyError = true
                                }
                            },
                        )
                        if (licenseKeyError) {
                            Text(
                                "That key didn't verify. Check it was copied in full.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        Text(
                            "A one-off £5 unlocks accent colour, reading font, and light/dark overrides that apply everywhere you read, including publications with their own theme. Pay via Ko-fi or GitHub Sponsors (under About → Support) and mention you'd like the customisation unlock — you'll get a key back to paste in here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SectionHeader("Legal")
                    SettingsRow(title = "Privacy Policy", onClick = { legalDocument = LegalDocumentType.PrivacyPolicy })
                    SettingsRow(title = "Terms of Service", onClick = { legalDocument = LegalDocumentType.TermsOfService })

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SectionHeader("About")
                    SettingsRow(title = "About Inkwell", onClick = { showAbout = true })

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SectionHeader("Account")
                    SettingsRow(
                        title = "Sign Out",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { isConfirmingSignOut = true },
                    )

                    Text(
                        "Version $appVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
        trailing?.invoke()
    }
}

/** Compose has no native colour-picker component (unlike SwiftUI's
 *  ColorPicker), so this offers a curated swatch set instead -- the
 *  idiomatic Material approach for a bounded choice like this. */
private val ACCENT_SWATCHES = listOf(
    "#139500", // Inkwell brand green (the app default)
    "#007AFF", // iOS system blue
    "#FF3B30", // red
    "#FF9500", // orange
    "#AF52DE", // purple
    "#FF2D92", // pink
)

@Composable
private fun ColorSwatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    val color = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White)
        }
    }
}
