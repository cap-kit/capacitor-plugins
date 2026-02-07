import UIKit
import WebKit

/**
 Dedicated view controller used to present the integrity block page.

 ROLE:
 This view controller is a pure UI component responsible for rendering
 a blocking screen when the host application decides to interrupt
 normal execution due to integrity-related conditions.

 Responsibilities:
 - Display a developer-provided HTML block page
 - Support both remote URLs and bundled local assets
 - Preserve query parameters for diagnostic or UX purposes
 - Optionally allow user dismissal when explicitly enabled

 This controller:
 - is presented explicitly by the Integrity plugin
 - never performs integrity checks
 - never decides when it should be shown

 Security note:
 - By default, the block page is NOT dismissible
 - Dismissal must be explicitly enabled by the host application
 */
final class IntegrityBlockViewController: UIViewController {

    // MARK: - Properties

    /// URL or local asset path provided by the plugin.
    /// Can include optional query parameters.
    private let url: String

    /// Whether the block page can be dismissed by the user.
    /// When false, interactive dismissal is explicitly disabled.
    private let dismissible: Bool

    // MARK: - Initialization

    /**
     Designated initializer.

     - Parameters:
     - url: Remote URL or local asset path to load.
     - dismissible: Whether the user is allowed to dismiss the block page.
     */
    init(url: String, dismissible: Bool) {
        self.url = url
        self.dismissible = dismissible
        super.init(nibName: nil, bundle: nil)

        // Prevent swipe-to-dismiss gestures unless explicitly allowed.
        // This ensures the block page cannot be bypassed unintentionally.
        self.isModalInPresentation = !dismissible
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .systemBackground

        // Navigation bar (dismissible mode only) ------------------------------

        // When dismissal is enabled, expose an explicit Close button.
        // This is the ONLY supported way to dismiss a modal view controller on iOS.
        if dismissible {
            let closeButton = UIBarButtonItem(
                title: "Close",
                style: .done,
                target: self,
                action: #selector(closeTapped)
            )
            navigationItem.rightBarButtonItem = closeButton
        }

        // WebView setup -------------------------------------------------------

        // Create an isolated WKWebView instance to render the block page.
        // No custom message handlers or JS bridges are attached.
        let configuration = WKWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: configuration)

        webView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(webView)

        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.topAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])

        // URL handling --------------------------------------------------------

        // Remote URLs (http / https) are loaded directly via URLRequest.
        if url.starts(with: "http"), let remoteURL = URL(string: url) {
            webView.load(URLRequest(url: remoteURL))
            return
        }

        // Local asset loading with query support ------------------------------

        // Example:
        //   url = "public/integrity-block.html?reason=integrity_failed"

        let parts = url.split(separator: "?", maxSplits: 1)
        let assetPath = String(parts[0])
        let query = parts.count > 1 ? "?\(parts[1])" : ""

        guard
            let fileURL = Bundle.main.url(forResource: assetPath, withExtension: nil),
            let html = try? String(contentsOf: fileURL, encoding: .utf8)
        else {
            // Fail silently if the asset cannot be loaded.
            // The plugin layer is responsible for validating inputs.
            return
        }

        // Create a synthetic base URL to preserve:
        // - window.location.search (query parameters)
        // - relative paths inside the HTML document
        let baseURL = fileURL.deletingLastPathComponent()
        let fakeURL = URL(string: baseURL.absoluteString + "/" + assetPath + query)

        webView.loadHTMLString(html, baseURL: fakeURL)
    }

    // MARK: - Actions

    /**
     Dismisses the block page when dismissal is explicitly allowed.
     */
    @objc private func closeTapped() {
        dismiss(animated: true)
    }
}
