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
import androidx.core.content.FileProvider
import uk.ewancroft.inkwell.shared.theme.SharedReaderTheme
import uk.ewancroft.inkwell.ui.moderation.MutedBlockedDialog
import uk.ewancroft.inkwell.util.AccessibilityPreferences
import uk.ewancroft.inkwell.util.ArticleStatePreferences
import uk.ewancroft.inkwell.util.CustomisationPreferences
import uk.ewancroft.inkwell.util.ImageCacheManager
import uk.ewancroft.inkwell.util.LinkPreferences
import uk.ewancroft.inkwell.util.ReaderPreferences
import uk.ewancroft.inkwell.util.rememberInkwellHaptics
import java.io.File

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
    userLexiconEnabled: Boolean,
    userLexiconBusy: Boolean,
    onUserLexiconEnabledChange: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    var legalDocument by remember { mutableStateOf<LegalDocumentType?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var isConfirmingSignOut by remember { mutableStateOf(false) }
    var showMutedBlocked by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptics = rememberInkwellHaptics()

    var accentColorHex by remember { mutableStateOf(CustomisationPreferences.getAccentColorHex(context)) }
    var fontFamilyOverride by remember { mutableStateOf(CustomisationPreferences.getFontFamilyOverride(context)) }
    var appearanceOverride by remember { mutableStateOf(CustomisationPreferences.getAppearanceOverride(context)) }
    var showCustomisationTipPrompt by remember { mutableStateOf(false) }

    fun promptForTipIfNeeded() {
        if (!CustomisationPreferences.hasShownTipPrompt(context)) {
            showCustomisationTipPrompt = true
        }
    }

    var sortOrder by remember { mutableStateOf(ReaderPreferences.getSortOrder(context)) }

    var fontSizeScale by remember { mutableStateOf(AccessibilityPreferences.getFontSizeScale(context)) }
    var boldText by remember { mutableStateOf(AccessibilityPreferences.getBoldText(context)) }
    var increaseContrast by remember { mutableStateOf(AccessibilityPreferences.getIncreaseContrast(context)) }
    var underlineLinks by remember { mutableStateOf(AccessibilityPreferences.getUnderlineLinks(context)) }
    var hapticsEnabled by remember { mutableStateOf(AccessibilityPreferences.getHapticsEnabled(context)) }

    var openLinksInApp by remember { mutableStateOf(LinkPreferences.getOpenLinksInApp(context)) }

    var cacheSizeBytes by remember { mutableStateOf(ImageCacheManager.currentSizeBytes(context)) }

    legalDocument?.let { documentType ->
        LegalDocumentDialog(documentType = documentType, onDismiss = { legalDocument = null })
    }

    if (showAbout) {
        CreditsView(appVersion = appVersion, onSignOut = onSignOut, onDismiss = { showAbout = false })
    }

    if (showMutedBlocked) {
        MutedBlockedDialog(onDismiss = { showMutedBlocked = false })
    }

    if (isConfirmingSignOut) {
        AlertDialog(
            onDismissRequest = { isConfirmingSignOut = false },
            title = { Text("Sign out of Inkwell?") },
            text = { Text("Your publications and subscriptions stay in your PDS. You can sign back in at any time.") },
            confirmButton = {
                TextButton(onClick = {
                    isConfirmingSignOut = false
                    haptics.medium()
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

    if (showCustomisationTipPrompt) {
        AlertDialog(
            onDismissRequest = {
                showCustomisationTipPrompt = false
                CustomisationPreferences.markTipPromptShown(context)
            },
            title = { Text("Enjoying Customisation?") },
            text = { Text("These overrides are free for everyone. If you find them useful, consider a tip to support ongoing development.") },
            confirmButton = {
                TextButton(onClick = {
                    showCustomisationTipPrompt = false
                    CustomisationPreferences.markTipPromptShown(context)
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ko-fi.com/ewancroft"))
                    context.startActivity(intent)
                }) {
                    Text("Tip Me")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCustomisationTipPrompt = false
                    CustomisationPreferences.markTipPromptShown(context)
                }) {
                    Text("Maybe Later")
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

                    SectionHeader("Reader")
                    Text(
                        "Sort Order",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = sortOrder == ReaderPreferences.SortOrder.NEWEST_FIRST,
                            onClick = {
                                sortOrder = ReaderPreferences.SortOrder.NEWEST_FIRST
                                ReaderPreferences.setSortOrder(context, ReaderPreferences.SortOrder.NEWEST_FIRST)
                            },
                            label = { Text("Newest First") },
                        )
                        FilterChip(
                            selected = sortOrder == ReaderPreferences.SortOrder.OLDEST_FIRST,
                            onClick = {
                                sortOrder = ReaderPreferences.SortOrder.OLDEST_FIRST
                                ReaderPreferences.setSortOrder(context, ReaderPreferences.SortOrder.OLDEST_FIRST)
                            },
                            label = { Text("Oldest First") },
                        )
                    }
                    Text(
                        "Controls the order documents appear in your reader feed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                        title = "Haptics",
                        trailing = {
                            Switch(
                                checked = hapticsEnabled,
                                onCheckedChange = {
                                    hapticsEnabled = it
                                    AccessibilityPreferences.setHapticsEnabled(context, it)
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
                            hapticsEnabled = true
                            AccessibilityPreferences.resetToDefaults(context)
                            haptics.light()
                        },
                    )
                    Text(
                        "These apply on top of your device's own font size and accessibility settings, and are always free.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                                    promptForTipIfNeeded()
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
                                    promptForTipIfNeeded()
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
                                promptForTipIfNeeded()
                            },
                            label = { Text("System") },
                        )
                        FilterChip(
                            selected = appearanceOverride == CustomisationPreferences.AppearanceOverride.LIGHT,
                            onClick = {
                                appearanceOverride = CustomisationPreferences.AppearanceOverride.LIGHT
                                CustomisationPreferences.setAppearanceOverride(context, CustomisationPreferences.AppearanceOverride.LIGHT)
                                promptForTipIfNeeded()
                            },
                            label = { Text("Light") },
                        )
                        FilterChip(
                            selected = appearanceOverride == CustomisationPreferences.AppearanceOverride.DARK,
                            onClick = {
                                appearanceOverride = CustomisationPreferences.AppearanceOverride.DARK
                                CustomisationPreferences.setAppearanceOverride(context, CustomisationPreferences.AppearanceOverride.DARK)
                                promptForTipIfNeeded()
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
                        "Overrides apply everywhere, including publications that set their own theme. Free — if you find it useful, a tip (About → Support) helps keep Inkwell going.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SectionHeader("Storage")
                    SettingsRow(title = "Image Cache", trailing = { Text(formatCacheSize(cacheSizeBytes)) })
                    SettingsRow(
                        title = "Clear Cache",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            ImageCacheManager.clear(context)
                            cacheSizeBytes = ImageCacheManager.currentSizeBytes(context)
                        },
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SectionHeader("Data")
                    SettingsRow(
                        title = "Export Data",
                        onClick = {
                            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
                            val file = File(exportsDir, "inkwell-reading-data.json")
                            file.writeText(ArticleStatePreferences.exportJson(context))
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Reading Data"))
                        },
                    )
                    Text(
                        "Exports your locally tracked read and bookmarked articles as a JSON file. This never leaves your device unless you choose to share it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SectionHeader("Links")
                    SettingsRow(
                        title = "Open Links In-App",
                        trailing = {
                            Switch(
                                checked = openLinksInApp,
                                onCheckedChange = {
                                    openLinksInApp = it
                                    LinkPreferences.setOpenLinksInApp(context, it)
                                },
                            )
                        },
                    )
                    Text(
                        "Article and post links open in an in-app browser instead of leaving Inkwell. This doesn't affect sign-in or the links above, which always open in your default browser.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

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
                        title = "Declare me as an Inkwell user",
                        trailing = {
                            Switch(
                                checked = userLexiconEnabled,
                                enabled = !userLexiconBusy,
                                onCheckedChange = onUserLexiconEnabledChange,
                            )
                        },
                    )
                    Text(
                        "Publishes a small record (uk.ewancroft.inkwell.user) to your own PDS declaring you use Inkwell. The website reads these via Constellation to show a \"people using Inkwell\" carousel. Turn it off to delete the record.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    SettingsRow(
                        title = "Muted & Blocked",
                        onClick = {
                            haptics.light()
                            showMutedBlocked = true
                        },
                    )
                    SettingsRow(
                        title = "Sign Out",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            haptics.light()
                            isConfirmingSignOut = true
                        },
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

private fun formatCacheSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 0.1) "Empty" else "%.1f MB".format(mb)
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
