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
        versionCode = 14
        versionName = "2.6.1"

        manifestPlaceholders["appAuthRedirectScheme"] = "uk.ewancroft.inkwell"

        // Hilt-aware instrumentation runner: it swaps InkwellApp for the
        // generated HiltTestApplication so androidTest can install
        // @TestInstallIn replacements at the DI boundary (see
        // app/src/androidTest/.../testing/). Without this, @HiltAndroidTest
        // fails at runtime with "did not attach the correct application".
        testInstrumentationRunner = "uk.ewancroft.inkwell.HiltTestRunner"
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

    testOptions {
        // Instrumentation tests assert on Compose semantics, not on animation
        // timing. Leaving system animations on makes emulator runs slower and
        // intermittently flaky for no coverage gain.
        animationsDisabled = true
    }

    packaging {
        resources {
            // JUnit/Hilt/ktor test artifacts ship overlapping licence metadata,
            // which otherwise fails the androidTest APK merge.
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
            )
        }
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
    // Deprecated, and retained solely for LegacySessionMigration to read the
    // pre-Keystore-envelope EncryptedSharedPreferences session file once. Session
    // storage itself no longer uses it — see data/auth/SessionEnvelope.kt. Drop
    // both this and LegacySessionMigration when no install can still hold a
    // legacy file.
    implementation(libs.security.crypto)
    implementation(libs.core.ktx)
    implementation("androidx.exifinterface:exifinterface:1.4.2")

    // Dagger 2.57+ unshaded kotlin-metadata-jvm; add explicit version for Kotlin 2.3.0 support
    ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.0")

    // -- JVM unit tests --
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.robolectric:robolectric:4.14.1")

    // -- Instrumentation tests (app/src/androidTest) --
    // Real Activity lifecycle, real Compose, real Hilt graph — the things
    // Robolectric can't speak to: singleTask intent delivery, process
    // recreation, and on-device accessibility semantics.
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    // Substitutes a scripted transport for ktor/OkHttp so no instrumentation
    // test ever reaches a real PDS. See testing/TestOAuthModule.kt.
    androidTestImplementation(libs.ktor.client.mock)
    // InkwellApp (and with it WorkManager.initialize) is replaced by
    // HiltTestApplication under test, and the manifest deliberately removes
    // WorkManager's androidx.startup initializer — so tests initialise it
    // themselves rather than leaving WorkManager.getInstance() to throw.
    androidTestImplementation(libs.work.testing)
    debugImplementation(libs.compose.ui.test.manifest)

    // -- Shared KMP core --
    implementation(project(":shared"))

    // -- Custom lint checks --
    // Registers ComposeHardcodedTextDetector (and any future detectors) into
    // every `./gradlew :app:lint` run without adding to the app's runtime
    // dependency graph. See lint-rules/build.gradle.kts.
    lintChecks(project(":lint-rules"))
}
