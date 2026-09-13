# Redsys — Security Considerations

## ⚠️ Security Warning: Merchant Secret Key

**Never store your Merchant Secret Key inside the mobile application.** Doing so exposes your credentials to decompilation. Always generate signatures on a secure backend.

The WebView (3-D Secure) payment flow requires a server-side signature to ensure transaction integrity. The plugin's `computeHash()` helper exists **ONLY FOR TESTING** and development/rapid prototyping — never use it to sign production transactions.

## Security Notes

- The plugin never exposes full card numbers.
- HMAC signature computation is performed natively.
- Sensitive configuration should never be hardcoded in production builds.
- Always validate server-side responses before fulfilling orders.

## Limitations

- Card numbers are automatically masked before being returned to JavaScript; raw card data is never exposed to the JS layer.
- The official Redsys SDK binaries are not bundled with this package for licensing and compliance reasons — you must obtain them and configure your native projects yourself (see [Native SDK Distribution & Setup](configuration.md#native-sdk-distribution--setup)).
