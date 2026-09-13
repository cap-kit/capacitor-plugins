# TLS Fingerprint — Security Considerations

This plugin validates fingerprint equality only.

## What this means

- The plugin compares the server's leaf certificate SHA-256 fingerprint against expected values
- It does NOT replace TLS validation
- It does NOT override trust evaluation
- Expired or self-signed certificates will validate if the fingerprint matches

## Limitations

- Fingerprint validation requires active maintenance
- Certificate rotation requires configuration updates
- Misconfiguration may result in loss of network connectivity

This plugin is provided as-is, without warranty. Always test thoroughly before production deployment.
