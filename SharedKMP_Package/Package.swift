// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SharedKMP",
    platforms: [
        .iOS(.v26),
    ],
    products: [
        .library(
            name: "SharedKMP",
            targets: ["SharedKMP"]),
    ],
    targets: [
        .binaryTarget(
            name: "InkwellShared",
            path: "../shared/InkwellShared.xcframework"
        ),
        .target(
            name: "SharedKMP",
            dependencies: ["InkwellShared"],
            path: "Sources/SharedKMP"
        ),
    ]
)
