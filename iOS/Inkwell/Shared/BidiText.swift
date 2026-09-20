import Foundation

extension String {
    /// Keeps user-controlled bidirectional text from reordering its surrounding label.
    var bidiIsolated: String {
        "\u{2068}\(self)\u{2069}"
    }
}
