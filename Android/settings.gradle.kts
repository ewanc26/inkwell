// ── Gradle Settings ─────────────────────────────────────────────────────
//
// Plugin and dependency resolution repositories. Google and Maven Central
// cover everything in the version catalog — no custom or snapshot repos.
// The Gradle Plugin Portal is needed for the Kotlin Multiplatform plugin
// (even though this project is Android-only, KSP's plugin resolver requires it).

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "inkwell-android"
include(":app")
