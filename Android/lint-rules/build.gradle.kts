// ── Lint Rules Module ───────────────────────────────────────────────────
//
// A plain Kotlin/JVM module (not Android) holding Inkwell's custom Android
// Lint checks. Consumed by the app module via the `lintChecks` dependency
// configuration, which makes Gradle include these checks in every
// `./gradlew :app:lint` run without altering the app module's own
// dependency graph.
//
// This is not test-covered by `./gradlew test`/`:app:testDebugUnitTest` —
// run `./gradlew :lint-rules:test` for the detector's own unit tests.

plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.lint.api)

    testImplementation(libs.lint.api)
    testImplementation(libs.lint.tests)
    testImplementation(libs.junit)
}
