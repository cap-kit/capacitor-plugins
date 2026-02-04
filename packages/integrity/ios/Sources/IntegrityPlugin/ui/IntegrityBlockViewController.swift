import UIKit
import WebKit

/**
 Dedicated view controller used to present the integrity block page.
 *
 Responsibilities:
 - Display a developer-provided HTML block page
 - Support query parameters (e.g. "reason")
 - Optionally allow dismissal when explicitly enabled
 *
 This controller:
 - is presented explicitly by the Integrity plugin
 - never performs integrity checks
 - never decides when it should be shown
 *
 Security note:
 - By default, the block page is NOT dismissible.
 * - Dismissal must be explicitly enabled by the host application.
 */
final class IntegrityBlockViewController: UIViewController {

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /// URL or local asset path passed by the plugin.
    private let url: String

    /// Whether the block page can be dismissed by the user.
    private let dismissible: Bool

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    init(url: String, dismissible: Bool) {
        self.url = url
        self.dismissible = dismissible
        super.init(nibName: nil, bundle: nil)

        // Prevent swipe-to-dismiss unless explicitly allowed
        self.isModalInPresentation = !dismissible
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .systemBackground

        // ---------------------------------------------------------------------
        // Navigation bar (demo / dismissible mode only)
        // ---------------------------------------------------------------------

        // When dismissible is enabled, provide a native Close button.
        // This is the ONLY correct way to dismiss a native modal on iOS.
        if dismissible {
            let closeButton = UIBarButtonItem(
                title: "Close",
                style: .done,
                target: self,
                action: #selector(closeTapped)
            )
            navigationItem.rightBarButtonItem = closeButton
        }

        // ---------------------------------------------------------------------
        // WebView setup
        // ---------------------------------------------------------------------

        let configuration = WKWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: configuration)

        webView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(webView)

        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.topAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
        ])

        // ---------------------------------------------------------------------
        // URL handling
        // ---------------------------------------------------------------------

        // Remote URLs (http / https)
        if url.starts(with: "http"), let remoteURL = URL(string: url) {
            webView.load(URLRequest(url: remoteURL))
            return
        }

        // ---------------------------------------------------------------------
        // Local asset loading with query support
        // ---------------------------------------------------------------------

        // Example:
        //   url = "public/integrity-block.html?reason=integrity_failed"

        let parts = url.split(separator: "?", maxSplits: 1)
        let assetPath = String(parts[0])
        let query = parts.count > 1 ? "?\(parts[1])" : ""

        guard
            let fileURL = Bundle.main.url(forResource: assetPath, withExtension: nil),
            let html = try? String(contentsOf: fileURL, encoding: .utf8)
        else {
            return
        }

        // Create a synthetic base URL to preserve:
        // - window.location.search (query parameters)
        // - relative paths inside the HTML
        let baseURL = fileURL.deletingLastPathComponent()
        let fakeURL = URL(string: baseURL.absoluteString + "/" + assetPath + query)

        webView.loadHTMLString(html, baseURL: fakeURL)
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     Dismisses the block page when allowed.
     */
    @objc private func closeTapped() {
        dismiss(animated: true)
    }
}
