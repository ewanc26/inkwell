// ── App Module ──────────────────────────────────────────────────────────
//
// Build configuration for the Inkwell Android app. Targets API 26 (Android 8.0)
// as the minimum, which covers the kotlinx.serialization and Compose runtime
// requirements while still allowing access to ~95% of active devices.

import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release signing credentials live in a gitignored keystore.properties
// (keystore path + passwords), never in this file or in version control.
// Absent locally (e.g. on a fresh checkout or in CI without the secret),
// release builds simply stay unsigned rather than failing the build.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseSigning = keystorePropertiesFile.exists()
if (hasReleaseSigning) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "uk.ewancroft.inkwell"
    compileSdk = 36

    defaultConfig {
        applicationId = "uk.ewancroft.inkwell"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "2.1.0"

        manifestPlaceholders["appAuthRedirectScheme"] = "uk.ewancroft.inkwell"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isDebuggable = true
            // Separate install from release builds for side-by-side testing
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

// ── Dependencies ────────────────────────────────────────────────────────

dependencies {
    // -- Compose BOM --
    // Single BOM import controls all Compose library versions
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // -- Activity & Navigation --
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // -- Lifecycle --
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.ktor.client.cio)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.coil.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    implementation(libs.work.runtime.ktx)

    implementation(libs.atproto.runtime)
    implementation(libs.atproto.models)
    implementation(libs.atproto.oauth)
    implementation(libs.atproto.compose.material3)

    implementation(libs.browser)
    implementation(libs.security.crypto)

    // Dagger 2.57+ unshaded kotlin-metadata-jvm; add explicit version for Kotlin 2.3.0 support
    ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.0")

    // -- JVM unit tests --
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // -- Shared KMP core --
    implementation(project(":shared"))
}
