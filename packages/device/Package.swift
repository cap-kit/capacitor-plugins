// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapKitDevice",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapKitDevice",
            targets: ["DevicePlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.5.0")
    ],
    targets: [
        .target(
            name: "DevicePlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/DevicePlugin"
        ),
        .testTarget(
            name: "DevicePluginTests",
            dependencies: [
                .target(name: "DevicePlugin")
            ],
            path: "ios/Tests/DevicePluginTests"
        )
    ]
)
