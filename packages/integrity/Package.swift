// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapKitIntegrity",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapKitIntegrity",
            targets: ["IntegrityPlugin"]
       )
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.2")
    ],
    targets: [
        .target(
            name: "IntegrityPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/IntegrityPlugin"
        )
    ]
)
