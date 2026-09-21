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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import uk.ewancroft.inkwell.shared.theme.SharedReaderTheme
import uk.ewancroft.inkwell.ui.moderation.ModerationSettingsDialog
import uk.ewancroft.inkwell.ui.moderation.MutedBlockedDialog
import uk.ewancroft.inkwell.R
import uk.ewancroft.inkwell.util.AccessibilityPreferences
import uk.ewancroft.inkwell.util.ArticleStatePreferences
import uk.ewancroft.inkwell.util.CustomisationPreferences
import uk.ewancroft.inkwell.util.ImageCacheManager
import uk.ewancroft.inkwell.util.LinkPreferences
import uk.ewancroft.inkwell.util.OfflineContentCacheManager
import uk.ewancroft.inkwell.util.ReaderPreferences
import uk.ewancroft.inkwell.util.formatCacheSize
import uk.ewancroft.inkwell.util.rememberInkwellHaptics
import java.io.File
import kotlinx.coroutines.launch

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
    onModerationChanged: () -> Unit,
    pendingSyncCount: Int,
    isSyncingPendingChanges: Boolean,
    canSyncPendingChanges: Boolean,
    onSyncPendingChanges: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    var legalDocument by remember { mutableStateOf<LegalDocumentType?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var isConfirmingSignOut by remember { mutableStateOf(false) }
    var showMutedBlocked by remember { mutableStateOf(false) }
    var showModerationSettings by remember { mutableStateOf(false) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var pendingImportText by remember { mutableStateOf<String?>(null) }
    var pendingImportCount by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberInkwellHaptics()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: null
        }.getOrNull()
        if (text == null) {
            importMessage = "This file is not a valid Inkwell reading-data export."
        } else when (val result = ArticleStatePreferences.previewImportJson(context, text)) {
            is ArticleStatePreferences.ImportResult.Success -> {
                pendingImportText = text
                pendingImportCount = result.changes
            }
            else -> importMessage = when (result) {
                ArticleStatePreferences.ImportResult.UnsupportedVersion -> "This reading-data export uses an unsupported version."
                else -> "This file is not a valid Inkwell reading-data export."
            }
        }
    }

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

    if (showModerationSettings) {
        ModerationSettingsDialog(
            onDismiss = { showModerationSettings = false },
            onPreferencesChanged = onModerationChanged,
        )
    }

    if (isConfirmingSignOut) {
        AlertDialog(
            onDismissRequest = { isConfirmingSignOut = false },
            title = { Text(stringResource(R.string.settings_sign_out_title)) },
            text = { Text(stringResource(R.string.settings_sign_out_message)) },
            confirmButton = {
                TextButton(onClick = {
                    isConfirmingSignOut = false
                    haptics.medium()
                    onSignOut()
                }) {
                    Text(stringResource(R.string.settings_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { isConfirmingSignOut = false }) {
                    Text(stringResource(R.string.cancel))
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
            title = { Text(stringResource(R.string.settings_customisation_tip_title)) },
            text = { Text(stringResource(R.string.settings_customisation_tip_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showCustomisationTipPrompt = false
                    CustomisationPreferences.markTipPromptShown(context)
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ko-fi.com/ewancroft"))
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.settings_tip_me))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCustomisationTipPrompt = false
                    CustomisationPreferences.markTipPromptShown(context)
                }) {
                    Text(stringResource(R.string.settings_maybe_later))
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
                        title = { Text(stringResource(R.string.reader_settings)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.reader_back))
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())) {
                    SectionHeader(stringResource(R.string.reader_notifications), modifier = Modifier.padding(top = 8.dp))
                    SettingsRow(
                        title = stringResource(R.string.settings_new_document_notifications),
                        trailing = {
                            Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsEnabledChange)
                        },
                    )
                    Text(
                        stringResource(R.string.settings_notifications_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    SettingsRow(
                        title = stringResource(R.string.settings_open_system_notifications),
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SectionHeader(stringResource(R.string.reader_title))
                    Text(
                        stringResource(R.string.settings_sort_order),
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
                            label = { Text(stringResource(R.string.settings_newest_first)) },
                        )
                        FilterChip(
                            selected = sortOrder == ReaderPreferences.SortOrder.OLDEST_FIRST,
                            onClick = {
                                sortOrder = ReaderPreferences.SortOrder.OLDEST_FIRST
                                ReaderPreferences.setSortOrder(context, ReaderPreferences.SortOrder.OLDEST_FIRST)
                            },
                            label = { Text(stringResource(R.string.settings_oldest_first)) },
                        )
                    }
                    Text(
                        stringResource(R.string.settings_sort_order_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SectionHeader(stringResource(R.string.moderation_content_filters))
                    SettingsRow(
                        title = stringResource(R.string.settings_content_warnings_keywords),
                        onClick = { showModerationSettings = true },
                        trailing = { Text(stringResource(R.string.settings_manage), color = MaterialTheme.colorScheme.primary) },
                    )
                    Text(
                        stringResource(R.string.settings_content_filters_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SectionHeader(stringResource(R.string.settings_accessibility))
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(stringResource(R.string.settings_text_size), style = MaterialTheme.typography.bodyLarge)
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
                        title = stringResource(R.string.settings_bold_text),
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
                        title = stringResource(R.string.settings_increase_contrast),
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
                        title = stringResource(R.string.settings_underline_links),
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
                        title = stringResource(R.string.settings_haptics),
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
                        title = stringResource(R.string.settings_reset_defaults),
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

                    SectionHeader(stringResource(R.string.settings_customisation))
                    Text(
                        stringResource(R.string.settings_accent_color),
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
                        stringResource(R.string.settings_reading_font),
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
                        stringResource(R.string.settings_appearance),
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
                            label = { Text(stringResource(R.string.settings_system)) },
                        )
                        FilterChip(
                            selected = appearanceOverride == CustomisationPreferences.AppearanceOverride.LIGHT,
                            onClick = {
                                appearanceOverride = CustomisationPreferences.AppearanceOverride.LIGHT
                                CustomisationPreferences.setAppearanceOverride(context, CustomisationPreferences.AppearanceOverride.LIGHT)
                                promptForTipIfNeeded()
                            },
                            label = { Text(stringResource(R.string.settings_light)) },
                        )
                        FilterChip(
                            selected = appearanceOverride == CustomisationPreferences.AppearanceOverride.DARK,
                            onClick = {
                                appearanceOverride = CustomisationPreferences.AppearanceOverride.DARK
                                CustomisationPreferences.setAppearanceOverride(context, CustomisationPreferences.AppearanceOverride.DARK)
                                promptForTipIfNeeded()
                            },
                            label = { Text(stringResource(R.string.settings_dark)) },
                        )
                    }
                    SettingsRow(
                        title = stringResource(R.string.settings_reset_defaults),
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

                    SectionHeader(stringResource(R.string.settings_storage))
                    SettingsRow(title = stringResource(R.string.settings_image_cache), trailing = { Text(formatCacheSize(cacheSizeBytes)) })
                    SettingsRow(
                        title = stringResource(R.string.settings_clear_cached_content),
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            ImageCacheManager.clear(context)
                            cacheSizeBytes = ImageCacheManager.currentSizeBytes(context)
                            coroutineScope.launch { OfflineContentCacheManager.clear(context) }
                        },
                    )
                    Text(
                        stringResource(R.string.settings_clear_cached_content_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SectionHeader(stringResource(R.string.settings_pending_changes))
                    SettingsRow(
                        title = stringResource(R.string.settings_saved_changes),
                        trailing = {
                            Text(
                                pluralStringResource(
                                    R.plurals.settings_pending_sync_count,
                                    pendingSyncCount,
                                    pendingSyncCount,
                                ),
                            )
                        },
                    )
                    if (pendingSyncCount > 0) {
                        TextButton(
                            onClick = onSyncPendingChanges,
                            enabled = canSyncPendingChanges && !isSyncingPendingChanges,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Text(
                                if (isSyncingPendingChanges) stringResource(R.string.settings_syncing)
                                else stringResource(R.string.settings_sync_saved_changes_now),
                            )
                        }
                    }
                    Text(
                        "Recommendations, subscriptions, and comments saved while offline sync automatically when you reconnect. They stay attached to the account that made them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                    SettingsRow(
                        title = "Import Data",
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/json")) },
                    )
                    Text(
                        "Exports your locally tracked read and bookmarked articles as a versioned JSON file. This never leaves your device unless you choose to share it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    importMessage?.let { message ->
                        AlertDialog(
                            onDismissRequest = { importMessage = null },
                            title = { Text("Import Data") },
                            text = { Text(message) },
                            confirmButton = { TextButton(onClick = { importMessage = null }) { Text("OK") } },
                        )
                    }
                    pendingImportText?.let { text ->
                        AlertDialog(
                            onDismissRequest = { pendingImportText = null },
                            title = { Text("Import reading data?") },
                            text = {
                                Text(
                                    context.resources.getQuantityString(
                                        R.plurals.settings_pending_import_message,
                                        pendingImportCount,
                                        pendingImportCount,
                                    ),
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    importMessage = when (val result = ArticleStatePreferences.importJson(context, text)) {
                                        is ArticleStatePreferences.ImportResult.Success -> context.resources.getQuantityString(
                                            R.plurals.settings_imported_change_count,
                                            result.changes,
                                            result.changes,
                                        )
                                        ArticleStatePreferences.ImportResult.UnsupportedVersion -> "This reading-data export uses an unsupported version."
                                        ArticleStatePreferences.ImportResult.Invalid -> "This file is not a valid Inkwell reading-data export."
                                    }
                                    pendingImportText = null
                                }) { Text("Import") }
                            },
                            dismissButton = { TextButton(onClick = { pendingImportText = null }) { Text("Cancel") } },
                        )
                    }

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
            Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.selected),
                tint = Color.White,
            )
        }
    }
}
