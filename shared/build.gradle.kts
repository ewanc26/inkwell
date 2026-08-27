// ── Shared Core — Kotlin Multiplatform Module ─────────────────────────────
//
// Pure business logic shared between Android (JVM) and iOS (XCFramework).
// Platform-specific implementations (WebSocket, file I/O) live in
// androidMain/iosMain.  commonMain contains models, interfaces, and
// business logic only.

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3)
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // ── Targets ─────────────────────────────────────────────────────────

    jvm()

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "InkwellShared"
            isStatic = true
        }
    }

    // ── Source Sets ─────────────────────────────────────────────────────

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websocket)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlin.test)
        }
    }
}

// ── XCFramework Assembly ────────────────────────────────────────────────
//
// Build with: ./gradlew :shared:linkDebugFrameworkIosArm64
//   (and the other ios targets), then combine with:
//   xcodebuild -create-xcframework ...
//
// Or use the convenience task below if the KMP plugin supports it.

tasks.register("assembleXCFramework") {
    group = "build"
    description = "Assembles an XCFramework for iOS consumption."
    dependsOn(
        "linkDebugFrameworkIosArm64",
        "linkDebugFrameworkIosSimulatorArm64",
        "linkDebugFrameworkIosX64",
        "linkReleaseFrameworkIosArm64",
        "linkReleaseFrameworkIosSimulatorArm64",
        "linkReleaseFrameworkIosX64",
    )
}
