// ── Root Project ────────────────────────────────────────────────────────
//
// Declares all plugins used by subprojects without applying them.
// Version numbers are sourced from the version catalog (gradle/libs.versions.toml)
// so every module uses the same dependency versions.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
