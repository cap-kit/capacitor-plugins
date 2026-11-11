import Foundation
import Compression

/**
 Minimal dotLottie (zip) reader.

 A dotLottie archive is a zip container holding a `manifest.json` plus one or
 more animation JSONs under `animations/`. Lottie's parsers only accept raw
 JSON, so this reader parses the zip central directory directly, extracts the
 selected animation entry (see `selectAnimationEntryName`) and inflates it
 (raw deflate via the Compression framework, matching zip entry encoding).

 Pure Foundation code: no third-party zip dependency. Extraction from real
 archives is verified on-device via the example app; the selection logic is
 covered by unit tests.
 */
enum SplashMotionZipReader {

    // MARK: - Public API

    /**
     Extracts the JSON payload of the selected animation entry.

     - Parameter data: raw bytes of a `.lottie` zip archive.
     - Returns: the raw JSON bytes of the animation, or nil when the archive
     is unreadable or contains no animation JSON.
     */
    static func extractAnimationJSON(from data: Data) -> Data? {
        guard let centralDirectory = readCentralDirectoryEntries(from: data) else {
            return nil
        }

        let names = centralDirectory.map { $0.name }
        guard let selectedName = selectAnimationEntryName(names),
              let entry = centralDirectory.first(where: { $0.name == selectedName }) else {
            return nil
        }

        guard let payload = entryPayload(entry: entry, in: data) else {
            return nil
        }

        switch entry.compressionMethod {
        case 0:
            // Stored (no compression).
            return payload
        case 8:
            // Deflate (raw), the standard dotLottie compression.
            return inflate(payload, expectedSize: entry.uncompressedSize)
        default:
            return nil
        }
    }

    /**
     Selects the animation JSON entry inside a dotLottie archive.

     Selection rule, matching the dotLottie spec layout:
     1. the first entry under `animations/` ending in `.json`;
     2. otherwise the first `.json` entry that is not `manifest.json`.

     - Returns: the selected entry name, or nil when no candidate exists.
     */
    static func selectAnimationEntryName(_ names: [String]) -> String? {
        if let animationEntry = names.first(where: { $0.hasPrefix("animations/") && $0.hasSuffix(".json") }) {
            return animationEntry
        }
        return names.first {
            $0.hasSuffix(".json") && $0.lowercased() != "manifest.json"
        }
    }

    // MARK: - Zip structure

    struct Entry {
        let name: String
        let compressionMethod: UInt16
        let compressedSize: Int
        let uncompressedSize: Int
        let localHeaderOffset: Int
    }

    private static let eocdSignature: UInt32 = 0x06054b50
    private static let centralDirectorySignature: UInt32 = 0x02014b50
    private static let localHeaderSignature: UInt32 = 0x04034b50
    private static let maxEndSearchBytes = 65_557 // 65535 max comment + 22 fixed

    private static func readCentralDirectoryEntries(from data: Data) -> [Entry]? {
        let bytes = [UInt8](data)
        guard let eocdOffset = findEOCDOffset(in: bytes) else { return nil }

        let entryCount = Int(readUInt16(bytes, eocdOffset + 10))
        var offset = Int(readUInt32(bytes, eocdOffset + 16))

        var entries: [Entry] = []
        entries.reserveCapacity(entryCount)

        for _ in 0..<entryCount {
            guard offset + 46 <= bytes.count,
                  readUInt32(bytes, offset) == centralDirectorySignature else {
                break
            }

            let compressionMethod = readUInt16(bytes, offset + 10)
            let compressedSize = Int(readUInt32(bytes, offset + 20))
            let uncompressedSize = Int(readUInt32(bytes, offset + 24))
            let nameLength = Int(readUInt16(bytes, offset + 28))
            let extraLength = Int(readUInt16(bytes, offset + 30))
            let commentLength = Int(readUInt16(bytes, offset + 32))
            let localHeaderOffset = Int(readUInt32(bytes, offset + 42))

            let nameStart = offset + 46
            guard nameStart + nameLength <= bytes.count else { break }
            let name = String(bytes: bytes[nameStart..<(nameStart + nameLength)], encoding: .utf8)

            if let name {
                entries.append(
                    Entry(
                        name: name,
                        compressionMethod: compressionMethod,
                        compressedSize: compressedSize,
                        uncompressedSize: uncompressedSize,
                        localHeaderOffset: localHeaderOffset
                    )
                )
            }

            offset = nameStart + nameLength + extraLength + commentLength
        }

        return entries.isEmpty ? nil : entries
    }

    private static func findEOCDOffset(in bytes: [UInt8]) -> Int? {
        let lowerBound = max(0, bytes.count - maxEndSearchBytes)
        guard bytes.count >= 22 else { return nil }

        var offset = bytes.count - 22
        while offset >= lowerBound {
            if readUInt32(bytes, offset) == eocdSignature {
                return offset
            }
            offset -= 1
        }
        return nil
    }

    private static func entryPayload(entry: Entry, in data: Data) -> Data? {
        let bytes = [UInt8](data)
        let headerOffset = entry.localHeaderOffset

        guard headerOffset + 30 <= bytes.count,
              readUInt32(bytes, headerOffset) == localHeaderSignature else {
            return nil
        }

        let nameLength = Int(readUInt16(bytes, headerOffset + 26))
        let extraLength = Int(readUInt16(bytes, headerOffset + 28))
        let dataStart = headerOffset + 30 + nameLength + extraLength

        guard dataStart + entry.compressedSize <= bytes.count else {
            return nil
        }

        return Data(bytes[dataStart..<(dataStart + entry.compressedSize)])
    }

    // MARK: - Inflation

    /**
     Inflates a raw-deflate payload (zip entry encoding) via the
     Compression framework. `COMPRESSION_ZLIB` in this framework processes
     raw deflate streams without the zlib container header, which is exactly
     what zip entries store.
     */
    private static func inflate(_ data: Data, expectedSize: Int) -> Data? {
        guard expectedSize > 0 else { return nil }

        let destination = UnsafeMutablePointer<UInt8>.allocate(capacity: expectedSize)
        defer { destination.deallocate() }

        let result = data.withUnsafeBytes { (source: UnsafeRawBufferPointer) -> Int in
            guard let baseAddress = source.baseAddress else { return 0 }
            return compression_decode_buffer(
                destination,
                expectedSize,
                baseAddress.assumingMemoryBound(to: UInt8.self),
                data.count,
                nil,
                COMPRESSION_ZLIB
            )
        }

        guard result > 0 else { return nil }
        return Data(bytes: destination, count: result)
    }

    // MARK: - Little-endian reads

    private static func readUInt16(_ bytes: [UInt8], _ offset: Int) -> UInt16 {
        guard offset + 2 <= bytes.count else { return 0 }
        return UInt16(bytes[offset]) | (UInt16(bytes[offset + 1]) << 8)
    }

    private static func readUInt32(_ bytes: [UInt8], _ offset: Int) -> UInt32 {
        guard offset + 4 <= bytes.count else { return 0 }
        return UInt32(bytes[offset])
            | (UInt32(bytes[offset + 1]) << 8)
            | (UInt32(bytes[offset + 2]) << 16)
            | (UInt32(bytes[offset + 3]) << 24)
    }
}
