# ── R8 / ProGuard Rules ──────────────────────────────────────────────────
#
# Inkwell Android — release minification rules.
# The default proguard-android-optimize.txt handles most cases; these rules
# cover transitive dependencies that R8 can't resolve automatically.

# ── errorprone annotations ────────────────────────────────────────────────
# Tink and other crypto libraries reference errorprone annotations at
# compile time. R8 reports these as missing classes since the annotations
# are not runtime dependencies.
-dontwarn com.google.errorprone.annotations.**

# ── AT Protocol (atproto-kotlin) ──────────────────────────────────────────
# kotlinx.serialization models are accessed via reflection.
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }

# Keep all serializable model classes.
-keep class uk.ewancroft.inkwell.data.model.** { *; }

# ── Hilt / Dagger ────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }

# ── OkHttp ───────────────────────────────────────────────────────────────
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
