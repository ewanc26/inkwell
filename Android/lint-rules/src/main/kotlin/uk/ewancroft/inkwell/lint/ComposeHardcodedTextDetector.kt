package uk.ewancroft.inkwell.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement

/**
 * Flags string literals passed as the `text` argument of a Compose `Text()`
 * call (`androidx.compose.material3.Text` / `androidx.compose.material.Text`).
 *
 * Android's built-in `HardcodedText` check ([com.android.tools.lint.checks
 * .HardcodedValuesDetector]) only scans XML layouts' `android:text`
 * attribute — it has no Compose awareness, so a Compose-only app (like
 * Inkwell) gets no first-party hardcoded-string coverage at all. This
 * detector fills that gap for the one composable that carries the vast
 * majority of user-facing copy.
 *
 * Only literals containing at least one letter are flagged, so decorative
 * glyphs ("·", "•"), numbering ("1."), and string templates ("$num.")
 * are left alone — those aren't translatable prose. Brand/product names
 * that must stay untranslated (see LOCALIZATION.md) are suppressed at the
 * call site with `// noinspection ComposeHardcodedText`, not module-wide.
 */
class ComposeHardcodedTextDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                if (node.methodName != "Text") return
                if (!isComposeTextCall(node)) return

                val textArg = node.getArgumentForParameter(0) ?: node.valueArguments.firstOrNull() ?: return

                // Kotlin string literals reach UAST as a
                // KotlinStringTemplateUPolyadicExpression even when they contain
                // no interpolation, not a ULiteralExpression — so a plain `is
                // ULiteralExpression` check silently never matches any Kotlin
                // string. ConstantEvaluator folds both representations (and
                // literal concatenation) down to the resulting String, and
                // correctly returns null for a template with a real `$variable`
                // interpolation, which isn't a constant and shouldn't be flagged.
                val value = ConstantEvaluator.evaluate(context, textArg) as? String ?: return
                if (value.none { it.isLetter() }) return

                context.report(
                    issue = ISSUE,
                    scope = node,
                    location = context.getLocation(textArg),
                    message = "Hardcoded string \"$value\" in Text() — move it to strings.xml and use " +
                        "stringResource(), or suppress with `// noinspection ComposeHardcodedText` if this " +
                        "is a brand/product name or other deliberately untranslated literal.",
                )
            }
        }

    /**
     * Best-effort check that the resolved `Text` call belongs to a Compose
     * package rather than some unrelated function coincidentally named
     * `Text`. Falls back to matching by name alone when resolution fails
     * (e.g. incomplete type information), since this codebase has no other
     * top-level `Text` function.
     */
    private fun isComposeTextCall(node: UCallExpression): Boolean {
        val method = node.resolve() ?: return true
        val qualifiedName = method.containingClass?.qualifiedName ?: return true
        return qualifiedName.startsWith("androidx.compose.material")
    }

    companion object {
        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "ComposeHardcodedText",
            briefDescription = "Hardcoded string in Compose Text()",
            explanation = """
                Hardcoding a string literal in a Compose `Text()` call makes that copy \
                impossible to translate. Move it into `res/values/strings.xml` (or a plurals \
                resource for quantities) and reference it via `stringResource()` / \
                `pluralStringResource()`.

                Protocol identifiers (NSIDs, DIDs, handles, AT-URIs), decorative glyphs, and \
                deliberately untranslated brand/product names (Inkwell, Bluesky, Leaflet, \
                Markpub, Pckt, Offprint, AltStore, F-Droid — see LOCALIZATION.md) are not what \
                this check is for. Suppress those specific lines with \
                `// noinspection ComposeHardcodedText`, not module-wide.
                """,
            category = Category.MESSAGES,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(
                ComposeHardcodedTextDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
