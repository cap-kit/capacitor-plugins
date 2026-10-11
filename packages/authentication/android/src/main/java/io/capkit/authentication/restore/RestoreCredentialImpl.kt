package io.capkit.authentication.restore

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import io.capkit.authentication.error.AuthenticationError
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * @file RestoreCredentialImpl.kt
 * Android Restore Credentials (Zero Tap) orchestration via `androidx.credentials`
 * Credential Manager. Thin device adapter: every decision (support gate, input
 * validation, error translation, E2EE retry) is delegated to the JVM-proven pure
 * units in this package, so the adapter stays deterministic and compile-gated.
 *
 * - create: `CreateRestoreCredentialRequest(requestJson, isCloudBackupEnabled)`,
 *   with exactly one retry without cloud backup on E2EE unavailability.
 * - get: `GetCredentialRequest([GetRestoreCredentialOption(requestJson)])`;
 *   a stored credential's `authenticationResponseJson` is surfaced as `requestJson`.
 *   `NoCredentialException` is normal absence → `{available:true}` with no payload.
 * - clear: `ClearCredentialStateRequest(TYPE_CLEAR_RESTORE_CREDENTIAL)`.
 *
 * Unsupported devices (below API 28) report `available=false`, never `unimplemented`.
 */
class RestoreCredentialImpl(
  private val context: Context,
  private val sdkInt: Int = Build.VERSION.SDK_INT,
) {
  private val credentialManager: CredentialManager = CredentialManager.create(context)
  private val executor: Executor = Executors.newSingleThreadExecutor()

  /** Result surface decoupled from the Capacitor bridge (Impl layer rule). */
  interface RestoreCredentialCallback {
    fun onResult(
      available: Boolean,
      requestJson: String? = null,
    )

    fun onError(error: AuthenticationError)
  }

  /**
   * Creates a restore credential, retrying once without cloud backup when the
   * device cannot provide E2EE.
   *
   * @param requestJson WebAuthn-style credential request JSON (validated).
   * @param isCloudBackupEnabled whether the credential may be cloud backed up.
   * @param callback the typed result surface.
   */
  fun create(
    requestJson: String?,
    isCloudBackupEnabled: Boolean,
    callback: RestoreCredentialCallback,
  ) {
    RestoreRequestValidator.validate(requestJson)?.let {
      callback.onError(it)
      return
    }
    if (!RestoreSupport.isRestoreSupported(sdkInt)) {
      callback.onResult(available = false)
      return
    }
    runCreate(requestJson!!, isCloudBackupEnabled, callback, attempt = 0)
  }

  private fun runCreate(
    requestJson: String,
    isCloudBackupEnabled: Boolean,
    callback: RestoreCredentialCallback,
    attempt: Int,
  ) {
    val request = CreateRestoreCredentialRequest(requestJson, isCloudBackupEnabled)
    credentialManager.createCredentialAsync(
      context = context,
      request = request,
      cancellationSignal = CancellationSignal(),
      executor = executor,
      callback =
        object : CredentialManagerCallback<androidx.credentials.CreateCredentialResponse, CreateCredentialException> {
          override fun onResult(result: androidx.credentials.CreateCredentialResponse) {
            callback.onResult(available = true)
          }

          override fun onError(exception: CreateCredentialException) {
            when (val outcome = RestoreErrorMapper.map(exception)) {
              is RestoreErrorOutcome.Reject -> callback.onError(outcome.error)
              RestoreErrorOutcome.RetryWithoutCloudBackup -> {
                if (attempt == 0 && RestoreRetryPolicy.shouldRetryWithoutCloudBackup(exception)) {
                  runCreate(requestJson, isCloudBackupEnabled = false, callback = callback, attempt = 1)
                } else {
                  callback.onError(AuthenticationError.InitFailed("E2EE is unavailable for restore credentials."))
                }
              }
              null -> callback.onError(AuthenticationError.InitFailed("Restore credential creation failed."))
            }
          }
        },
    )
  }

  /**
   * Reads the stored restore credential, if any.
   *
   * @param requestJson the WebAuthn-style request JSON for the restore option.
   * @param callback the typed result surface; no stored credential reports
   *        `{available:true}` with no payload (normal absence, not an error).
   */
  fun get(
    requestJson: String?,
    callback: RestoreCredentialCallback,
  ) {
    RestoreRequestValidator.validate(requestJson)?.let {
      callback.onError(it)
      return
    }
    if (!RestoreSupport.isRestoreSupported(sdkInt)) {
      callback.onResult(available = false)
      return
    }
    val request = GetCredentialRequest(listOf(GetRestoreCredentialOption(requestJson!!)))
    credentialManager.getCredentialAsync(
      context = context,
      request = request,
      cancellationSignal = CancellationSignal(),
      executor = executor,
      callback =
        object : CredentialManagerCallback<androidx.credentials.GetCredentialResponse, GetCredentialException> {
          override fun onResult(result: androidx.credentials.GetCredentialResponse) {
            val credential = result.credentials.firstOrNull()
            callback.onResult(
              available = true,
              requestJson = (credential as? RestoreCredential)?.authenticationResponseJson,
            )
          }

          override fun onError(exception: GetCredentialException) {
            if (exception is NoCredentialException) {
              callback.onResult(available = true)
              return
            }
            when (val outcome = RestoreErrorMapper.map(exception)) {
              is RestoreErrorOutcome.Reject -> callback.onError(outcome.error)
              else -> callback.onError(AuthenticationError.InitFailed("Restore credential retrieval failed."))
            }
          }
        },
    )
  }

  /**
   * Clears the stored restore credential for the provider.
   *
   * @param callback the typed result surface.
   */
  fun clear(callback: RestoreCredentialCallback) {
    if (!RestoreSupport.isRestoreSupported(sdkInt)) {
      callback.onResult(available = false)
      return
    }
    val request = ClearCredentialStateRequest(ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL)
    credentialManager.clearCredentialStateAsync(
      request = request,
      cancellationSignal = CancellationSignal(),
      executor = executor,
      callback =
        object : CredentialManagerCallback<Void?, ClearCredentialException> {
          override fun onResult(result: Void?) {
            callback.onResult(available = true)
          }

          override fun onError(exception: ClearCredentialException) {
            callback.onError(AuthenticationError.InitFailed("Restore credential clearing failed."))
          }
        },
    )
  }
}
