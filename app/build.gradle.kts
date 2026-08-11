// ── App Module ──────────────────────────────────────────────────────────
//
// Build configuration for the Inkwell Android app. Targets API 26 (Android 8.0)
// as the minimum, which covers the kotlinx.serialization and Compose runtime
// requirements while still allowing access to ~95% of active devices.

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "uk.ewancroft.inkwell"
    compileSdk = 36

    defaultConfig {
        applicationId = "uk.ewancroft.inkwell"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        manifestPlaceholders["appAuthRedirectScheme"] = "uk.ewancroft.inkwell"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}
