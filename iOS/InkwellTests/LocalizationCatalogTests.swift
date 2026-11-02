import XCTest

/// Locale regression coverage for the `Localizable.xcstrings` String Catalog (#88).
///
/// Today the catalog is source-locale only (`en`), so these tests guard the
/// foundation a future translation will build on: every key resolves to a
/// real English string, and format specifiers are extracted with a single
/// shared rule so that when localizations land, per-locale consistency can be
/// asserted the same way Android's `LocaleResourcesParityTest` does.
///
/// The catalog is read from the source tree relative to this file
/// (`#filePath`), not from the test bundle, so no Xcode project resource
/// wiring is needed and the test runs wherever the sources are checked out.
final class LocalizationCatalogTests: XCTestCase {

    private struct Catalog: Decodable {
        var sourceLanguage: String
        var strings: [String: CatalogEntry]
        var version: String
    }

    private struct CatalogEntry: Decodable {
        var comment: String?
        var localizations: [String: Localization]?
    }

    private struct Localization: Decodable {
        var stringUnit: StringUnit?
    }

    private struct StringUnit: Decodable {
        var state: String?
        var value: String?
    }

    private func loadCatalog() throws -> Catalog {
        let testsDir = URL(filePath: #filePath).deletingLastPathComponent()
        let catalogURL = testsDir
            .deletingLastPathComponent()
            .appending(path: "Inkwell/Localizable.xcstrings")
        let data = try Data(contentsOf: catalogURL)
        return try JSONDecoder().decode(Catalog.self, from: data)
    }

    /// Format specifiers (`%@`, `%d`, `%1$@`, …) in a catalog value, ignoring
    /// the escaped literal `%%`. Returned sorted so values compare directly.
    private func specifiers(in value: String) -> [String] {
        let stripped = value.replacingOccurrences(of: "%%", with: "")
        // Length modifiers (`%lld`) included: the catalog uses them for counts.
        let pattern = #"%(?:\d+\$)?[0-9.\-]*(?:ll|l|h|z)?[@dfisuFeEgGxXo]"#
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
        let range = NSRange(stripped.startIndex..., in: stripped)
        return regex.matches(in: stripped, range: range).compactMap { match in
            Range(match.range, in: stripped).map { String(stripped[$0]) }
        }.sorted()
    }

    func testCatalogSourceLanguageIsEnglish() throws {
        let catalog = try loadCatalog()
        XCTAssertEqual(catalog.sourceLanguage, "en")
    }

    func testEveryKeyHasANonEmptyEnglishValue() throws {
        let catalog = try loadCatalog()
        var missing: [String] = []
        var blank: [String] = []
        for (key, entry) in catalog.strings {
            guard !key.isEmpty else { continue } // Xcode bookkeeping entry, not a string.
            let value = entry.localizations?["en"]?.stringUnit?.value
            if value == nil { missing.append(key) }
            else if value!.isEmpty { blank.append(key) }
        }
        XCTAssertTrue(missing.isEmpty, "keys with no English value: \(missing.sorted())")
        XCTAssertTrue(blank.isEmpty, "keys with a blank English value: \(blank.sorted())")
    }

    func testFormatSpecifiersMatchAcrossLocalizations() throws {
        let catalog = try loadCatalog()
        var mismatches: [String] = []
        for (key, entry) in catalog.strings {
            guard !key.isEmpty, let localizations = entry.localizations else { continue }
            let byLocale = localizations.compactMapValues { $0.stringUnit?.value }
            guard let reference = byLocale["en"] else { continue }
            let expected = specifiers(in: reference)
            for (locale, value) in byLocale where locale != "en" {
                let actual = specifiers(in: value)
                if actual != expected {
                    mismatches.append("\(key) [\(locale)]: en \(expected) vs \(locale) \(actual)")
                }
            }
        }
        XCTAssertTrue(mismatches.isEmpty, "specifier drift:\n\(mismatches.sorted().joined(separator: "\n"))")
    }

    func testEnglishValuesHaveNoDanglingPercentSigns() throws {
        let catalog = try loadCatalog()
        var dangling: [String] = []
        for (key, entry) in catalog.strings {
            guard !key.isEmpty else { continue }
            guard let value = entry.localizations?["en"]?.stringUnit?.value else { continue }
            let stripped = value.replacingOccurrences(of: "%%", with: "")
            // A `%` that does not start a recognised specifier will crash
            // String(format:) at runtime or render literally.
            let pattern = #"%(?!(?:\d+\$)?[0-9.\-]*(?:ll|l|h|z)?[@dfisuFeEgGxXo])"#
            if let regex = try? NSRegularExpression(pattern: pattern),
               regex.firstMatch(in: stripped, range: NSRange(stripped.startIndex..., in: stripped)) != nil {
                dangling.append(key)
            }
        }
        XCTAssertTrue(dangling.isEmpty, "values with a dangling %: \(dangling.sorted())")
    }
}
