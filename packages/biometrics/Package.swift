// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapKitBiometrics",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapKitBiometrics",
            targets: ["BiometricsPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.6.0")
    ],
    targets: [
        .target(
            name: "BiometricsPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/BiometricsPlugin"
        ),
        .testTarget(
            name: "BiometricsPluginTests",
            dependencies: ["BiometricsPlugin"],
            path: "ios/Tests/BiometricsPluginTests"
        )
    ]
)
