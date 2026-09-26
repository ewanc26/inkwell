package uk.ewancroft.inkwell.shared.oauth

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Checks the scope definitions in this module against the three checked-in copies of
 * `client-metadata.json` and against the Swift mirror in the iOS app.
 *
 * The invariant these tests exist for: an Authorization Server rejects an authorization
 * request whose `scope` contains any token not *literally* present in the `scope` field of
 * the client metadata document it fetched. Metadata must therefore always be a superset of
 * the runtime request. A subset breaks every login, for every user, immediately.
 *
 * This runs on the JVM target rather than in `commonTest` because it reads repository
 * files, which `commonMain`'s multiplatform file APIs cannot do.
 */
class ClientMetadataScopeAlignmentTest {

    private val repoRoot: File by lazy {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, IOS_METADATA).isFile && File(dir, WEBSITE_METADATA).isFile) return@lazy dir
            dir = dir.parentFile
        }
        error(
            "could not locate the repository root above ${System.getProperty("user.dir")}; " +
                "expected to find $IOS_METADATA and $WEBSITE_METADATA",
        )
    }

    private fun read(relativePath: String): String {
        val file = File(repoRoot, relativePath)
        assertTrue(file.isFile, "missing $relativePath")
        return file.readText()
    }

    /** The `scope` field of a `client-metadata.json` document. */
    private fun jsonScopeTokens(relativePath: String): List<String> =
        Json.parseToJsonElement(read(relativePath))
            .jsonObject
            .getValue("scope")
            .jsonPrimitive
            .content
            .split(" ")
            .filter { it.isNotEmpty() }

    /**
     * The `scope` field of the SvelteKit endpoint, which builds the value by concatenating
     * adjacent string literals.
     */
    private fun websiteScopeTokens(): List<String> {
        val source = read(WEBSITE_METADATA)
        val assignment = Regex("""scope:\s*((?:"(?:[^"\\]|\\.)*"\s*\+\s*)*"(?:[^"\\]|\\.)*")""")
            .find(source)
            ?: error("could not find the scope assignment in $WEBSITE_METADATA")
        return Regex(""""((?:[^"\\]|\\.)*)"""")
            .findAll(assignment.groupValues[1])
            .joinToString("") { it.groupValues[1] }
            .split(" ")
            .filter { it.isNotEmpty() }
    }

    private fun publishedScopeSets(): Map<String, List<String>> = mapOf(
        IOS_METADATA to jsonScopeTokens(IOS_METADATA),
        ANDROID_METADATA to jsonScopeTokens(ANDROID_METADATA),
        WEBSITE_METADATA to websiteScopeTokens(),
    )

    @Test
    fun everyPublishedMetadataDocumentDeclaresExactlyTheSharedScopeSet() {
        val expected = InkwellOAuthScopes.clientMetadataScopes().toSet()
        for ((path, tokens) in publishedScopeSets()) {
            assertEquals(
                expected,
                tokens.toSet(),
                "$path drifted from InkwellOAuthScopes.clientMetadataScopes()",
            )
        }
    }

    @Test
    fun everyPublishedMetadataDocumentIsASupersetOfEveryPossibleRuntimeRequest() {
        for ((path, tokens) in publishedScopeSets()) {
            val declared = tokens.toSet()
            for (usePermissionSet in listOf(false, true)) {
                for (scope in InkwellOAuthScopes.runtimeScopes(usePermissionSet)) {
                    assertTrue(
                        scope in declared,
                        "$path does not declare runtime scope \"$scope\" (usePermissionSet=$usePermissionSet)",
                    )
                }
            }
        }
    }

    @Test
    fun noPublishedMetadataDocumentTripsAuthorizationServerMetadataValidation() {
        for ((path, tokens) in publishedScopeSets()) {
            assertTrue(OAuthScopes.ATPROTO in tokens, "$path is missing the required \"atproto\" scope")
            assertEquals(tokens.size, tokens.toSet().size, "$path declares a duplicate scope token")
        }
    }

    @Test
    fun thePermissionSetIsAlreadyDeclaredSoEnablingItNeedsNoMetadataRedeploy() {
        for ((path, tokens) in publishedScopeSets()) {
            assertTrue(
                StandardSitePermissionSets.INCLUDE_AUTH_FULL in tokens,
                "$path does not declare ${StandardSitePermissionSets.INCLUDE_AUTH_FULL}",
            )
            assertTrue(
                StandardSitePermissionSets.AUTH_FULL_EXPANSION.all { it in tokens },
                "$path stopped declaring the granular scopes the runtime still requests",
            )
        }
    }

    @Test
    fun theSwiftMirrorAgreesWithTheKotlinPermissionSetSwitch() {
        val swift = read(IOS_SCOPES_SWIFT)
        val declared = Regex("""static\s+let\s+usePermissionSet\s*(?::\s*Bool\s*)?=\s*(true|false)""")
            .find(swift)
            ?: error("could not find `usePermissionSet` in $IOS_SCOPES_SWIFT")
        assertEquals(
            StandardSitePermissionSets.USE_PERMISSION_SET.toString(),
            declared.groupValues[1],
            "$IOS_SCOPES_SWIFT disagrees with StandardSitePermissionSets.USE_PERMISSION_SET; " +
                "iOS mirrors this value as a literal until the checked-in XCFramework exports it",
        )
    }

    @Test
    fun theSwiftMirrorStillHardcodesTheScopesSharedKmpDoesNotExportToIt() {
        val swift = read(IOS_SCOPES_SWIFT)
        val hardcoded = InkwellOAuthScopes.MODERATION_SCOPES + OAuthScopes.REPO_USER
        for (scope in hardcoded) {
            assertTrue(
                "\"$scope\"" in swift,
                "$IOS_SCOPES_SWIFT no longer requests \"$scope\"; iOS and Android would request different scopes",
            )
        }
    }

    private companion object {
        const val IOS_METADATA = "iOS/oauth/client-metadata.json"
        const val ANDROID_METADATA = "Android/docs/oauth/client-metadata.json"
        const val WEBSITE_METADATA = "website/src/routes/client-metadata.json/+server.ts"
        const val IOS_SCOPES_SWIFT = "iOS/Inkwell/Authentication/InkwellOAuthScopes.swift"
    }
}
