import Foundation

extension Fortress {

    func createSignature(
        payload: String,
        keyAlias: String?,
        promptMessage: String?,
        promptOptions: PromptOptions?,
        completion: @escaping (Result<String, Error>) -> Void
    ) {
        let resolvedAlias: String
        do {
            resolvedAlias = try validateSignaturePreconditions(payload: payload, keyAlias: keyAlias)
        } catch {
            completion(.failure(error))
            return
        }

        let reason = resolvePromptReason(
            promptMessage: promptMessage,
            promptOptions: promptOptions,
            fallback: "Authenticate to sign payload"
        )
        let cancelTitle = promptOptions?.negativeButtonText ?? config?.biometricPromptText ?? "Cancel"
        let allowPasscode = resolveAllowPasscode()

        biometricAuth.authenticate(reason: reason, cancelTitle: cancelTitle, allowPasscode: allowPasscode) { result in
            switch result {
            case .success:
                self.markAuthenticationSuccess()
                do {
                    let signature = try self.signPayload(payload, alias: resolvedAlias)
                    self.sessionManager.touch()
                    completion(.success(signature))
                } catch {
                    completion(.failure(error))
                }
            case .failure(let error):
                self.recordBiometricFailure(error)
                completion(.failure(error))
            }
        }
    }

    func biometricKeysExist(keyAlias: String?) -> Bool {
        let resolvedAlias = keyAlias ?? Fortress.defaultBiometricKeyAlias
        return KeychainHelper.hasKeyPair(alias: resolvedAlias)
    }

    func createKeys(keyAlias: String?) throws -> String {
        if config?.cryptoStrategy == "rsa" {
            throw NativeError.unavailable(ErrorMessages.unavailable)
        }

        let resolvedAlias = keyAlias ?? Fortress.defaultBiometricKeyAlias
        try? KeychainHelper.deleteKeyPair(alias: resolvedAlias)

        let publicKeyData = try KeychainHelper.generateKeyPair(alias: resolvedAlias)
        guard let publicKey = String(data: publicKeyData, encoding: .utf8), !publicKey.isEmpty else {
            throw NativeError.initFailed(ErrorMessages.initFailed)
        }

        return publicKey
    }

    func deleteKeys(keyAlias: String?) throws {
        let resolvedAlias = keyAlias ?? Fortress.defaultBiometricKeyAlias
        try? KeychainHelper.deleteKeyPair(alias: resolvedAlias)
    }

    func registerWithChallenge(
        challenge: String,
        keyAlias: String?,
        promptMessage: String?,
        promptOptions: PromptOptions?,
        completion: @escaping (Result<[String: String], Error>) -> Void
    ) {
        do {
            try assertNotLockedOut()
        } catch {
            completion(.failure(error))
            return
        }

        guard !challenge.isEmpty else {
            completion(.failure(NativeError.invalidInput(ErrorMessages.invalidInput)))
            return
        }

        do {
            let publicKey = try createKeys(keyAlias: keyAlias)
            authenticateWithChallenge(
                challenge: challenge,
                keyAlias: keyAlias,
                promptMessage: promptMessage,
                promptOptions: promptOptions
            ) { result in
                switch result {
                case .success(let signature):
                    completion(.success([
                        "publicKey": publicKey,
                        "signature": signature
                    ]))
                case .failure(let error):
                    completion(.failure(error))
                }
            }
        } catch {
            completion(.failure(error))
        }
    }

    func authenticateWithChallenge(
        challenge: String,
        keyAlias: String?,
        promptMessage: String?,
        promptOptions: PromptOptions?,
        completion: @escaping (Result<String, Error>) -> Void
    ) {
        do {
            try assertNotLockedOut()
        } catch {
            completion(.failure(error))
            return
        }

        guard !challenge.isEmpty else {
            completion(.failure(NativeError.invalidInput(ErrorMessages.invalidInput)))
            return
        }

        let resolvedAlias = keyAlias ?? Fortress.defaultBiometricKeyAlias
        guard KeychainHelper.hasKeyPair(alias: resolvedAlias) else {
            completion(.failure(NativeError.notFound(ErrorMessages.notFound)))
            return
        }

        let reason = resolvePromptReason(
            promptMessage: promptMessage,
            promptOptions: promptOptions,
            fallback: "Authenticate to sign payload"
        )
        let cancelTitle = promptOptions?.negativeButtonText ?? config?.biometricPromptText ?? "Cancel"
        let allowPasscode = resolveAllowPasscode()

        biometricAuth.authenticate(reason: reason, cancelTitle: cancelTitle, allowPasscode: allowPasscode) { result in
            switch result {
            case .success:
                self.markAuthenticationSuccess()
                do {
                    let signatureData = try KeychainHelper.sign(data: Data(challenge.utf8), with: resolvedAlias)
                    completion(.success(signatureData.base64EncodedString()))
                } catch {
                    if let keychainError = error as? KeychainHelper.KeychainError,
                       case .itemNotFound = keychainError {
                        completion(.failure(NativeError.notFound(ErrorMessages.notFound)))
                        return
                    }

                    completion(.failure(NativeError.securityViolation(ErrorMessages.securityViolation)))
                }
            case .failure(let error):
                self.recordBiometricFailure(error)
                completion(.failure(error))
            }
        }
    }

    func generateChallengePayload(nonce: String) throws -> String {
        guard !nonce.isEmpty else {
            throw NativeError.invalidInput(ErrorMessages.invalidInput)
        }

        let currentTimestamp = Int64(Date().timeIntervalSince1970 * 1000)
        let payload = ChallengePayload(
            nonce: nonce,
            timestamp: currentTimestamp,
            deviceIdentifierHash: Utils.deviceIdentifierHash()
        )

        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys, .withoutEscapingSlashes]

        guard let payloadString = String(data: try encoder.encode(payload), encoding: .utf8) else {
            throw NativeError.initFailed(ErrorMessages.initFailed)
        }

        return payloadString
    }

    func setInsecureValue(key: String, value: String) throws {
        try standardStorage.set(key: getObfuscatedKeyValue(key), value: value)
    }

    func getInsecureValue(key: String) throws -> String? {
        for name in insecureNameCandidates(key) {
            if let value = standardStorage.get(key: name) {
                return value
            }
        }
        return nil
    }

    func removeInsecureValue(key: String) throws {
        for name in insecureNameCandidates(key) {
            try standardStorage.remove(key: name)
        }
    }

    func getObfuscatedKey(key: String) throws -> String {
        getObfuscatedKeyValue(key)
    }

    func hasKey(key: String, secure: Bool) throws -> Bool {
        if secure {
            return try secureNameCandidates(for: key).contains { try secureStorage.hasKey(key: $0) }
        }

        return insecureNameCandidates(key).contains { standardStorage.hasKey(key: $0) }
    }
}

extension Fortress {

    func validateSignaturePreconditions(payload: String, keyAlias: String?) throws -> String {
        try assertNotLockedOut()

        guard !payload.isEmpty else {
            throw NativeError.invalidInput(ErrorMessages.invalidInput)
        }

        try ensureSecureVaultAccessible()

        let resolvedAlias = keyAlias ?? Fortress.defaultBiometricKeyAlias
        guard KeychainHelper.hasKeyPair(alias: resolvedAlias) else {
            throw NativeError.notFound(ErrorMessages.notFound)
        }

        return resolvedAlias
    }

    func signPayload(_ payload: String, alias: String) throws -> String {
        do {
            let signatureData = try KeychainHelper.sign(data: Data(payload.utf8), with: alias)
            return signatureData.base64EncodedString()
        } catch {
            if let keychainError = error as? KeychainHelper.KeychainError,
               case .itemNotFound = keychainError {
                throw NativeError.notFound(ErrorMessages.notFound)
            }

            throw NativeError.securityViolation(ErrorMessages.securityViolation)
        }
    }

    func getObfuscatedKeyValue(_ key: String) -> String {
        let obfuscationPrefix = config?.obfuscationPrefix ?? "ftrss_"
        let globalPrefix = config?.prefix ?? ""
        let body = (config?.obfuscateKeys ?? false) ? KeyUtils.encodeB64(key) : key
        return KeyUtils.obfuscate(body, prefix: obfuscationPrefix, globalPrefix: globalPrefix)
    }

    /**
     Candidate insecure stored names, primary first. Reads try each in
     order so toggling the flag never orphans existing entries.
     */
    func insecureNameCandidates(_ key: String) -> [String] {
        let obfuscationPrefix = config?.obfuscationPrefix ?? "ftrss_"
        let globalPrefix = config?.prefix ?? ""
        noteObfuscationPrefix(obfuscationPrefix)
        var prefixes = [obfuscationPrefix]
        for seen in seenObfuscationPrefixes where seen != obfuscationPrefix {
            prefixes.append(seen)
        }
        var names: [String] = []
        for prefix in prefixes {
            let plain = KeyUtils.obfuscate(key, prefix: prefix, globalPrefix: globalPrefix)
            let encoded = KeyUtils.obfuscate(
                KeyUtils.encodeB64(key),
                prefix: prefix,
                globalPrefix: globalPrefix
            )
            names.append(contentsOf: (config?.obfuscateKeys ?? false) ? [encoded, plain] : [plain, encoded])
        }
        return names
    }

    func noteObfuscationPrefix(_ prefix: String) {
        guard !prefix.isEmpty else {
            return
        }
        seenObfuscationPrefixes.insert(prefix)
    }
}
