// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapKitSecureStorage",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapKitSecureStorage",
            targets: ["SecureStoragePlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.6.0")
    ],
    targets: [
        .target(
            name: "SecureStoragePlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/SecureStoragePlugin"
        ),
        .testTarget(
            name: "SecureStoragePluginTests",
            dependencies: ["SecureStoragePlugin"],
            path: "ios/Tests/SecureStoragePluginTests"
        )
    ]
)
