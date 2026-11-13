import Foundation

/**
 Utility to convert EC P-256 public keys to SPKI PEM.

 IMPORTANT:
 - iOS may expose the public key as raw ANSI X9.63 (uncompressed point).
 - Backend verification and storage should prefer SPKI PEM ("BEGIN PUBLIC KEY").
 - This utility is internal-only and does NOT change the JS API.
 */
enum PublicKeyEncoder {
    private static let spkiHeaderP256: [UInt8] = [
        // ASN.1 SubjectPublicKeyInfo for ecPublicKey + prime256v1 (P-256)
        // SEQUENCE {
        //   SEQUENCE { OID ecPublicKey, OID prime256v1 }
        //   BIT STRING (0x00 + uncompressed point)
        // }
        //
        // This is a fixed header for P-256 SPKI with uncompressed point.
        0x30, 0x59,                         // SEQUENCE, length 0x59
        0x30, 0x13,                         // SEQUENCE, length 0x13
        0x06, 0x07, 0x2A, 0x86, 0x48, 0xCE, 0x3D, 0x02, 0x01, // OID 1.2.840.10045.2.1 (ecPublicKey)
        0x06, 0x08, 0x2A, 0x86, 0x48, 0xCE, 0x3D, 0x03, 0x01, 0x07, // OID 1.2.840.10045.3.1.7 (prime256v1)
        0x03, 0x42, 0x00                   // BIT STRING, length 0x42, 0 unused bits
    ]

    /**
     Converts a raw P-256 public key (ANSI X9.63 uncompressed point, 65 bytes)
     into SPKI PEM (SubjectPublicKeyInfo).

     - Parameter rawX963: 65 bytes starting with 0x04.
     - Returns: SPKI PEM string.
     */
    static func p256RawToSpkiPem(_ rawX963: Data) throws -> String {
        // 65 bytes: 0x04 || X(32) || Y(32)
        guard rawX963.count == 65, rawX963.first == 0x04 else {
            throw NSError(domain: "Fortress", code: 1, userInfo: [
                NSLocalizedDescriptionKey: "Invalid P-256 public key format (expected X9.63 uncompressed point)"
            ])
        }

        var spki = Data(spkiHeaderP256)
        spki.append(rawX963)

        let b64 = spki.base64EncodedString(options: [.lineLength64Characters, .endLineWithLineFeed])
        return "-----BEGIN PUBLIC KEY-----\n\(b64)-----END PUBLIC KEY-----\n"
    }
}
