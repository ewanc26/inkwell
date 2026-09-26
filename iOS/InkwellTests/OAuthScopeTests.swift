import XCTest
import OAuthenticator
@testable import Inkwell

/// Guards the OAuth scope request. The superset invariant these tests protect: an
/// Authorization Server rejects an authorization request containing any scope token not
/// literally present in the `scope` field of the fetched `client-metadata.json`, so a
/// mismatch here does not degrade a feature — it stops everyone logging in.
///
/// The Kotlin side of this lives in
/// `shared/src/commonTest/.../InkwellOAuthScopesTest.kt`, and
/// `ClientMetadataScopeAlignmentTest` checks both against the published metadata
/// documents.
final class OAuthScopeTests: XCTestCase {

    /// The exact scope list Inkwell has been shipping, in order.
    private let shippedScopes = [
        "atproto",
        "blob:*/*",
        "repo:site.standard.publication",
        "repo:site.standard.document",
        "repo:site.standard.graph.subscription",
        "repo:site.standard.graph.recommend",
        "repo:pub.leaflet.comment",
        "repo:app.userinput.discussion",
        "repo:uk.ewancroft.inkwell.user",
        "repo:app.bsky.graph.block?action=create&action=delete",
        "rpc:app.bsky.graph.muteActor?aud=did:web:api.bsky.app%23bsky_appview",
        "rpc:app.bsky.graph.unmuteActor?aud=did:web:api.bsky.app%23bsky_appview",
        "rpc:app.bsky.graph.getMutes?aud=did:web:api.bsky.app%23bsky_appview",
        "rpc:app.bsky.graph.getBlocks?aud=did:web:api.bsky.app%23bsky_appview",
        "rpc:com.atproto.moderation.createReport?aud=did:web:api.bsky.app%23bsky_appview",
    ]

    func testGranularRequestMatchesTheShippedScopeList() {
        XCTAssertEqual(InkwellOAuthScopes.requested(usePermissionSet: false), shippedScopes)
    }

    func testPermissionSetIsOffByDefaultSoTheLiveRequestIsUnchanged() {
        // Flipping this is a deliberate act that must also update this test, and must stay
        // in sync with StandardSitePermissionSets.USE_PERMISSION_SET in shared KMP.
        XCTAssertFalse(InkwellOAuthScopes.usePermissionSet)
        XCTAssertEqual(InkwellOAuthScopes.requested(), shippedScopes)
    }

    func testPermissionSetRequestReplacesExactlyTheFourStandardSiteScopes() {
        let withSet = InkwellOAuthScopes.requested(usePermissionSet: true)

        XCTAssertTrue(withSet.contains("include:site.standard.authFull"))
        XCTAssertTrue(withSet.allSatisfy { !$0.hasPrefix("repo:site.standard.") })
        XCTAssertEqual(
            withSet.filter { $0 != "include:site.standard.authFull" },
            shippedScopes.filter { !InkwellOAuthScopes.standardSiteGranular.contains($0) }
        )
    }

    func testEveryRequestedScopeIsALiteralMemberOfDeclaredMetadata() {
        let declared = Set(InkwellOAuthScopes.clientMetadataDeclared)
        for usePermissionSet in [false, true] {
            for scope in InkwellOAuthScopes.requested(usePermissionSet: usePermissionSet) {
                XCTAssertTrue(
                    declared.contains(scope),
                    "requested scope \"\(scope)\" (usePermissionSet=\(usePermissionSet)) is not declared in client metadata"
                )
            }
        }
    }

    func testDeclaredMetadataSatisfiesAuthorizationServerValidation() {
        let declared = InkwellOAuthScopes.clientMetadataDeclared

        // The reference provider requires `atproto` and rejects duplicate tokens.
        XCTAssertTrue(declared.contains("atproto"))
        XCTAssertEqual(declared.count, Set(declared).count, "duplicate scope token in client metadata")
    }

    func testRequestHasNoDuplicateOrSplittableTokens() {
        for usePermissionSet in [false, true] {
            let scopes = InkwellOAuthScopes.requested(usePermissionSet: usePermissionSet)
            XCTAssertEqual(scopes.count, Set(scopes).count, "duplicate requested scope token")
            XCTAssertTrue(scopes.allSatisfy { !$0.isEmpty && !$0.contains(" ") })
        }
    }

    func testSwitchingToThePermissionSetGrantsExactlyTheSamePermissions() {
        // The reason no existing user has to re-authorize when the switch is made.
        XCTAssertEqual(
            InkwellOAuthScopes.effectivePermissions(InkwellOAuthScopes.requested(usePermissionSet: false)),
            InkwellOAuthScopes.effectivePermissions(InkwellOAuthScopes.requested(usePermissionSet: true))
        )
    }

    func testScopesThatCannotLiveInAPermissionSetAreAlwaysRequestedDirectly() {
        XCTAssertEqual(InkwellOAuthScopes.alwaysExplicit, ["atproto", "blob:*/*"])
        for usePermissionSet in [false, true] {
            let scopes = InkwellOAuthScopes.requested(usePermissionSet: usePermissionSet)
            XCTAssertTrue(scopes.contains("atproto"))
            XCTAssertTrue(scopes.contains("blob:*/*"))
        }
    }

    func testScopesOutsideTheStandardSiteNamespaceAreNeverDelegatedToAPermissionSet() {
        // A permission set may only grant resources under its own NSID namespace.
        let outside = InkwellOAuthScopes.appRecordScopes + InkwellOAuthScopes.moderationScopes
        XCTAssertTrue(outside.allSatisfy { !$0.hasPrefix("repo:site.standard.") })
        for usePermissionSet in [false, true] {
            let scopes = InkwellOAuthScopes.requested(usePermissionSet: usePermissionSet)
            XCTAssertTrue(outside.allSatisfy { scopes.contains($0) })
        }
    }

    func testExpansionMatchesThePublishedLexicons() {
        XCTAssertEqual(
            InkwellOAuthScopes.expand("include:site.standard.authFull"),
            [
                "repo:site.standard.publication",
                "repo:site.standard.document",
                "repo:site.standard.graph.subscription",
                "repo:site.standard.graph.recommend",
            ]
        )
        XCTAssertEqual(
            InkwellOAuthScopes.expand("include:site.standard.authSocial"),
            [
                "repo:site.standard.graph.subscription",
                "repo:site.standard.graph.recommend",
            ]
        )
    }

    func testExpansionIsOnlyClaimedForPermissionSetsInkwellModels() {
        XCTAssertNil(InkwellOAuthScopes.expand("include:com.example.someoneElsesSet"))
        XCTAssertNil(InkwellOAuthScopes.expand("repo:site.standard.document"))
        XCTAssertNil(InkwellOAuthScopes.expand("site.standard.authFull"), "bare NSID is not an include: scope")
        XCTAssertEqual(
            InkwellOAuthScopes.effectivePermissions(["include:com.example.someoneElsesSet", "blob:*/*"]),
            ["include:com.example.someoneElsesSet", "blob:*/*"]
        )
    }

    /// The credentials handed to OAuthenticator pre-encode the characters its PAR form-body
    /// builder does not escape. Decoding them once must return the declared scope tokens
    /// exactly, or the PDS compares a mangled string against its client metadata.
    @MainActor
    func testLoginCredentialsCarryTheRequestedScopesAndSurviveFormEncoding() {
        let credentials = LoginStateManager(
            defaults: UserDefaults(suiteName: "OAuthScopeTests")!
        ).appCredentials

        XCTAssertEqual(credentials.clientId, "https://inkwell.ewancroft.uk/client-metadata.json")
        XCTAssertEqual(credentials.callbackURL.absoluteString, "uk.ewancroft.inkwell:/callback")
        XCTAssertEqual(
            credentials.scopes.map { $0.removingPercentEncoding ?? $0 },
            InkwellOAuthScopes.requested()
        )
    }
}
