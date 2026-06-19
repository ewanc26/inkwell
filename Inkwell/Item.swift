//
//  Item.swift
//  Inkwell
//
//  Created by Ewan Croft on 19/06/2026.
//

import Foundation
import SwiftData

@Model
final class Item {
    var timestamp: Date
    
    init(timestamp: Date) {
        self.timestamp = timestamp
    }
}
