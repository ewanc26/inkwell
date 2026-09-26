//
//  ReportReasonType.swift
//  Inkwell
//

import ATProtoKit

/// App-level report reason that maps to ATProtoKit's `ReasonTypeDefinition`.
enum ReportReasonType: String, CaseIterable, Identifiable {
    case spam
    case violation
    case misleading
    case sexual
    case rude
    case other

    var id: String { rawValue }

    /// The wire value forwarded to the AT Protocol moderation endpoint.
    var wireValue: String { atProtoValue.rawValue }

    var displayName: String {
        switch self {
        case .spam:       return "Spam"
        case .violation:  return "Rule violation"
        case .misleading: return "Misleading"
        case .sexual:     return "Unwanted sexual content"
        case .rude:       return "Harassment / rude behaviour"
        case .other:      return "Other"
        }
    }

    var atProtoValue: ComAtprotoLexicon.Moderation.ReasonTypeDefinition {
        switch self {
        case .spam:       return .spam
        case .violation:  return .violation
        case .misleading: return .misleading
        case .sexual:     return .sexual
        case .rude:       return .rude
        case .other:      return .other
        }
    }
}
