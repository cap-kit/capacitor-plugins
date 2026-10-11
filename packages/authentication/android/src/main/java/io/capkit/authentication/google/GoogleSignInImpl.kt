package io.capkit.authentication.google

import android.app.PendingIntent
import android.content.Context
import android.os.CancellationSignal
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.capkit.authentication.error.AuthenticationError
import io.capkit.authentication.vault.TokenBundle
import io.capkit.authentication.vault.TokenVault
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * @file GoogleSignInImpl.kt
 * Android Google sign-in orchestration via Credential Manager (One Tap) followed,
 * when the [AuthorizationPolicy] requires it, by the Google authorization chain
 * (Capawesome-proven) that surfaces the FULL token set: `AuthorizationRequest` with
 * requested scopes + offline access -> `Identity.getAuthorizationClient(...).authorize(...)`
 * -> on `hasResolution()` launch the pending intent and recover the result via
 * `getAuthorizationResultFromIntent` -> extract `accessToken` + `serverAuthCode`.
 *
 * Thin device adapter: UX mode selection, fallback policy, the single-flight gate
 * and all authorization mapping/merge decisions are the JVM-proven pure units in
 * this package; this class only maps them onto `androidx.credentials` + `googleid
 * 1.2.0` + `play-services-auth` and persists the result through [TokenVault].
 *
 * - AUTO_SELECT: silent `GetGoogleIdOption` for returning accounts; user
 *   cancellation or "no credentialed account" falls back to the full chooser.
 * - FULL_BUTTON: direct `GetSignInWithGoogleOption` chooser, always followed by
 *   the authorization step (MUST: full token set on the interactive path).
 *
 * The interactive context MUST be the plugin's Activity context (Credential
 * Manager requirement); the executor thread only receives callbacks.
 */
class GoogleSignInImpl(
  private val context: Context,
) {
  private val credentialManager: CredentialManager = CredentialManager.create(context)
  private val executor: Executor = Executors.newSingleThreadExecutor()
  private val gate = SignInGate()

  /**
   * Bridge hook that delivers an authorization UI resolution to the plugin's
   * Activity (which owns the `ActivityResultLauncher`). Decouples this Impl from
   * the Capacitor bridge while keeping the intent launch in the Activity layer.
   */
  fun interface AuthorizationLauncher {
    fun launch(pendingIntent: PendingIntent)
  }

  private var authorizationLauncher: AuthorizationLauncher? = null

  /**
   * State preserved while the authorization pending intent is in flight: the
   * configured bundle (Credential Manager idToken) and the pending callback.
   */
  private var pendingAuthorization: PendingAuthorization? = null

  private data class PendingAuthorization(
    val vault: TokenVault,
    val bundle: TokenBundle,
    val callback: SignInCallback,
  )

  /** Result surface decoupled from the Capacitor bridge (Impl layer rule). */
  interface SignInCallback {
    fun onResult(bundle: TokenBundle)

    fun onError(error: AuthenticationError)
  }

  /**
   * Runs the Google sign-in flow for the configured mode, persisting the merged
   * token bundle on success. Exactly one interactive flow may run.
   *
   * @param config the validated Google configuration.
   * @param vault the per-provider secure vault to persist into.
   * @param mode the One Tap UX mode ([GoogleSignInModeResolver] result).
   * @param callback the typed result surface.
   */
  fun signIn(
    config: GoogleConfig,
    vault: TokenVault,
    mode: GoogleSignInMode = GoogleSignInModeResolver.resolve(config.autoSelect),
    callback: SignInCallback,
  ) {
    if (!gate.tryAcquire()) {
      callback.onError(AuthenticationError.Conflict("A Google sign-in is already in progress."))
      return
    }
    runSignIn(config, vault, mode, callback)
  }

  /**
   * Attaches the bridge hook that launches the authorization pending intent.
   * MUST be called during plugin `load()` before any interactive flow may run.
   */
  fun attachAuthorizationLauncher(launcher: AuthorizationLauncher) {
    authorizationLauncher = launcher
  }

  /**
   * Completes the flow with an `AuthorizationResult` recovered from the launched
   * intent (`getAuthorizationResultFromIntent`), merging access token and server
   * auth code over the pending Credential Manager bundle.
   */
  fun handleAuthorizationResult(authResult: AuthorizationResult) {
    val pending = pendingAuthorization
    if (pending == null) {
      gate.release()
      return
    }
    pendingAuthorization = null
    val payload =
      AuthorizationResultMapper.extract(
        accessToken = authResult.accessToken,
        serverAuthCode = authResult.serverAuthCode,
        hasResolution = false,
      )
    completeAuthorization(pending, payload)
  }

  /** Completes the flow as user-cancelled when the launched intent returns non-OK. */
  fun handleAuthorizationCanceled() {
    val pending = pendingAuthorization
    pendingAuthorization = null
    gate.release()
    pending?.callback?.onError(AuthenticationError.UserCancelled("Google authorization was cancelled."))
  }

  /** Completes the flow as failed when the launched intent cannot be recovered. */
  fun handleAuthorizationFailed(throwable: Throwable) {
    val pending = pendingAuthorization
    pendingAuthorization = null
    gate.release()
    pending?.callback?.onError(
      AuthenticationError.InitFailed(throwable.message ?: "Google authorization failed."),
    )
  }

  private fun runSignIn(
    config: GoogleConfig,
    vault: TokenVault,
    mode: GoogleSignInMode,
    callback: SignInCallback,
  ) {
    val request = GetCredentialRequest(listOf(buildOption(config, mode)))
    credentialManager.getCredentialAsync(
      context = context,
      request = request,
      cancellationSignal = CancellationSignal(),
      executor = executor,
      callback =
        object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
          override fun onResult(result: GetCredentialResponse) {
            val idToken = extractGoogleIdToken(result.credentials)
            if (idToken == null) {
              gate.release()
              callback.onError(AuthenticationError.InitFailed("Google sign-in returned no Google ID credential."))
              return
            }
            val merged = GoogleIdTokenMapper.mergeSignInResult(vault.read(), idToken)
            if (AuthorizationPolicy.shouldAuthorize(config, mode)) {
              runAuthorization(config, vault, merged, callback)
            } else {
              vault.store(merged)
              gate.release()
              callback.onResult(merged)
            }
          }

          override fun onError(exception: GetCredentialException) {
            if (SignInFallbackPolicy.shouldFallbackToFullButton(mode, exception)) {
              runSignIn(config, vault, GoogleSignInMode.FULL_BUTTON, callback)
              return
            }
            gate.release()
            callback.onError(mapSignInError(exception))
          }
        },
    )
  }

  private fun buildOption(
    config: GoogleConfig,
    mode: GoogleSignInMode,
  ): androidx.credentials.CredentialOption =
    when (mode) {
      GoogleSignInMode.AUTO_SELECT ->
        GetGoogleIdOption
          .Builder()
          .setServerClientId(config.serverClientId)
          .setFilterByAuthorizedAccounts(true)
          .setAutoSelectEnabled(true)
          .apply { config.nonce.takeIf { !it.isNullOrBlank() }?.let { setNonce(it) } }
          .build()
      GoogleSignInMode.FULL_BUTTON ->
        GetSignInWithGoogleOption
          .Builder(config.serverClientId)
          .apply { config.nonce.takeIf { !it.isNullOrBlank() }?.let { setNonce(it) } }
          .build()
    }

  /**
   * Runs the Google authorization chain that surfaces the full token set
   * (Capawesome-proven). On `hasResolution()` the pending intent is handed to the
   * bridge launcher; the recovered result arrives via [handleAuthorizationResult]
   * (or the canceled/failed variants) and completes the flow.
   */
  private fun runAuthorization(
    config: GoogleConfig,
    vault: TokenVault,
    bundle: TokenBundle,
    callback: SignInCallback,
  ) {
    val spec = AuthorizationRequestSpecMapper.buildSpec(config)
    if (spec == null) {
      gate.release()
      callback.onError(
        AuthenticationError.InitFailed("Google authorization is not configured (missing server client id)."),
      )
      return
    }
    val request =
      AuthorizationRequest
        .builder()
        .setRequestedScopes(spec.scopes.map { scope -> Scope(scope) })
        .requestOfflineAccess(spec.offlineAccessClientId)
        .build()
    Identity
      .getAuthorizationClient(context)
      .authorize(request)
      .addOnSuccessListener { authResult ->
        val payload =
          AuthorizationResultMapper.extract(
            accessToken = authResult.accessToken,
            serverAuthCode = authResult.serverAuthCode,
            hasResolution = authResult.hasResolution(),
          )
        if (AuthorizationResultMapper.shouldLaunchResolution(payload)) {
          val pendingIntent = authResult.pendingIntent
          if (pendingIntent == null) {
            gate.release()
            callback.onError(AuthenticationError.InitFailed("Google authorization resolution has no pending intent."))
            return@addOnSuccessListener
          }
          pendingAuthorization = PendingAuthorization(vault, bundle, callback)
          val launcher = authorizationLauncher
          if (launcher == null) {
            pendingAuthorization = null
            gate.release()
            callback.onError(AuthenticationError.InitFailed("Google authorization launcher is not attached."))
            return@addOnSuccessListener
          }
          launcher.launch(pendingIntent)
        } else {
          completeAuthorization(PendingAuthorization(vault, bundle, callback), payload)
        }
      }.addOnFailureListener { exception ->
        gate.release()
        callback.onError(AuthenticationError.InitFailed(exception.message ?: "Google authorization failed."))
      }
  }

  /** Persists the full token set and completes the pending sign-in. */
  private fun completeAuthorization(
    pending: PendingAuthorization,
    payload: AuthorizationResultMapper.AuthorizationPayload,
  ) {
    val finalBundle = AuthorizationResultMapper.mergeTokens(pending.bundle, payload)
    pending.vault.store(finalBundle)
    gate.release()
    pending.callback.onResult(finalBundle)
  }

  private fun extractGoogleIdToken(credentials: List<Credential>): String? =
    credentials
      .firstOrNull { credential ->
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
          credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
      }?.let { GoogleIdTokenCredential.createFrom(it.data).idToken }
      ?.takeIf { it.isNotBlank() }

  private fun mapSignInError(exception: GetCredentialException): AuthenticationError =
    when (exception) {
      is GetCredentialCancellationException ->
        AuthenticationError.UserCancelled("Google sign-in was cancelled.")
      is NoCredentialException ->
        AuthenticationError.UserCancelled("No Google account was available.")
      else ->
        AuthenticationError.InitFailed(exception.message ?: "Google sign-in failed.")
    }
}
