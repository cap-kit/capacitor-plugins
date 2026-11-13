// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapKitSplashMotion",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapKitSplashMotion",
            targets: ["SplashMotionPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.6.2"),
        .package(url: "https://github.com/airbnb/lottie-ios.git", from: "4.5.1")
    ],
    targets: [
        .target(
            name: "SplashMotionPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                .product(name: "Lottie", package: "lottie-ios")
            ],
            path: "ios/Sources/SplashMotionPlugin"
        ),
        .testTarget(
            name: "SplashMotionPluginTests",
            dependencies: ["SplashMotionPlugin"],
            path: "ios/Tests/SplashMotionPluginTests"
        )
    ]
)
