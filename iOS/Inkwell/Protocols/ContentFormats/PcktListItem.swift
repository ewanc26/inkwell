//
//  PcktListItem.swift
//  Inkwell
//

import Foundation

// MARK: - PcktListItem

public struct PcktListItem: Codable, Equatable, Hashable, Sendable {
    public let type: String
    public let content: [PcktBlock]?
    /// Set for `taskItem`s only; absent for plain `listItem`s.
    public let checked: Bool?

    public init(type: String, content: [PcktBlock]? = nil, checked: Bool? = nil) {
        self.type = type
        self.content = content
        self.checked = checked
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case content
        case checked
    }
}

// MARK: - PcktAspectRatio

public struct PcktAspectRatio: Codable, Equatable, Hashable, Sendable {
    public let width: Int
    public let height: Int

    public init(width: Int, height: Int) {
        self.width = width
        self.height = height
    }
}
