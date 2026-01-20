import XCTest
@testable import TestPlugin

/**
 Basic functional tests for the Test plugin native implementation.

 These tests validate the core behavior of the implementation
 independently from the Capacitor bridge.
 */
class TestTests: XCTestCase {
    func testEcho() {
        // This is an example of a functional test case for a plugin.
        // Use XCTAssert and related functions to verify your tests produce the correct results.

        let implementation = TestImpl()
        let value = "Hello, World!"
        let result = implementation.echo(value)

        XCTAssertEqual(value, result)
    }
}
