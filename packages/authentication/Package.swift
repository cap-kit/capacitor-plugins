// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapKitAuthentication",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapKitAuthentication",
            targets: ["AuthenticationPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.5.1"),
        .package(url: "https://github.com/google/GoogleSignIn-iOS.git", from: "7.1.0"),
        .package(url: "https://github.com/facebook/facebook-ios-sdk.git", from: "18.1.0")
    ],
    targets: [
        .target(
            name: "AuthenticationPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                .product(name: "GoogleSignIn", package: "GoogleSignIn-iOS"),
                .product(name: "FacebookCore", package: "facebook-ios-sdk"),
                .product(name: "FacebookLogin", package: "facebook-ios-sdk")
            ],
            path: "ios/Sources/AuthenticationPlugin"
        ),
        .testTarget(
            name: "AuthenticationPluginTests",
            dependencies: [
                "AuthenticationPlugin",
                .product(name: "FacebookCore", package: "facebook-ios-sdk")
            ],
            path: "ios/Tests/AuthenticationPluginTests"
        )
    ]
)
