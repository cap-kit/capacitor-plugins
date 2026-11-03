package io.capkit.securestorage

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.securestorage.config.SecureStorageConfig
import io.capkit.securestorage.error.SecureStorageError
import io.capkit.securestorage.error.SecureStorageErrorMessages
import io.capkit.securestorage.logger.SecureStorageLogger
import io.capkit.securestorage.model.GetManyResult
import io.capkit.securestorage.model.GetResult
import io.capkit.securestorage.model.HasResult
import io.capkit.securestorage.model.KeysResult
import io.capkit.securestorage.model.RemoveResult
import io.capkit.securestorage.model.SecureStorageVersionResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Capacitor bridge for the SecureStorage plugin.
 *
 * CONTRACT:
 * - This class is the ONLY entry point from JavaScript.
 * - All PluginCall instances MUST be resolved or rejected exactly once.
 */
@CapacitorPlugin(name = "SecureStorage")
@Suppress("unused")
class SecureStoragePlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration.
   *
   * CONTRACT:
   * - Initialized exactly once in `load()`
   * - Treated as read-only afterwards
   */
  private lateinit var config: SecureStorageConfig

  /**
   * Native implementation layer.
   *
   * CONTRACT:
   * - Owned by the Plugin layer
   * - MUST NOT access PluginCall or Capacitor APIs
   */
  private lateinit var implementation: SecureStorageImpl

  /**
   * Serializer instance configured to encode result models into JSObject string payloads.
   *
   * NOTE: encodeDefaults is intentionally NOT set so nullable/defaulted fields
   * are omitted from the emitted JSON, matching the optional TypeScript types.
   */
  private val json = Json

  /**
   * Converts a serializable result model directly into a Capacitor JSObject.
   */
  private inline fun <reified T> toJSObject(value: T): JSObject {
    val jsonString = json.encodeToString(value)
    return JSObject(jsonString)
  }

  // ---------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   */
  override fun load() {
    super.load()

    config = SecureStorageConfig(this)
    implementation = SecureStorageImpl(context)
    implementation.updateConfig(config)

    SecureStorageLogger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Rejects the call with a message and a standardized error code.
   */
  private fun reject(
    call: PluginCall,
    error: SecureStorageError,
  ) {
    val code =
      when (error) {
        is SecureStorageError.Unavailable -> "UNAVAILABLE"
        is SecureStorageError.Cancelled -> "CANCELLED"
        is SecureStorageError.PermissionDenied -> "PERMISSION_DENIED"
        is SecureStorageError.InitFailed -> "INIT_FAILED"
        is SecureStorageError.InvalidInput -> "INVALID_INPUT"
        is SecureStorageError.UnknownType -> "UNKNOWN_TYPE"
        is SecureStorageError.NotFound -> "NOT_FOUND"
        is SecureStorageError.Conflict -> "CONFLICT"
        is SecureStorageError.Timeout -> "TIMEOUT"
      }

    val message = error.message ?: SecureStorageErrorMessages.INTERNAL_ERROR
    call.reject(message, code)
  }

  /**
   * Centralizes error handling for a native throwable.
   */
  private fun handleError(
    call: PluginCall,
    throwable: Throwable,
  ) {
    if (throwable is SecureStorageError) {
      reject(call, throwable)
    } else {
      val message = throwable.message ?: SecureStorageErrorMessages.UNEXPECTED_NATIVE_ERROR
      reject(call, SecureStorageError.InitFailed(message))
    }
  }

  // ---------------------------------------------------------------------------
  // Key/value operations
  // ---------------------------------------------------------------------------

  /**
   * Stores a value under a key, overwriting any previous entry.
   */
  @PluginMethod
  fun set(call: PluginCall) {
    try {
      val key =
        call.getString("key") ?: throw SecureStorageError.InvalidInput(
          SecureStorageErrorMessages.INVALID_INPUT,
        )
      val value =
        call.getString("value") ?: throw SecureStorageError.InvalidInput(
          SecureStorageErrorMessages.INVALID_INPUT,
        )
      implementation.set(key, value)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reads the value for a key, or null when absent.
   */
  @PluginMethod
  fun get(call: PluginCall) {
    try {
      val key =
        call.getString("key") ?: throw SecureStorageError.InvalidInput(
          SecureStorageErrorMessages.INVALID_INPUT,
        )
      call.resolve(toJSObject(GetResult(value = implementation.get(key))))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Removes the entry for a key.
   */
  @PluginMethod
  fun remove(call: PluginCall) {
    try {
      val key =
        call.getString("key") ?: throw SecureStorageError.InvalidInput(
          SecureStorageErrorMessages.INVALID_INPUT,
        )
      call.resolve(toJSObject(RemoveResult(existed = implementation.remove(key))))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Checks whether an entry exists without reading it.
   */
  @PluginMethod
  fun has(call: PluginCall) {
    try {
      val key =
        call.getString("key") ?: throw SecureStorageError.InvalidInput(
          SecureStorageErrorMessages.INVALID_INPUT,
        )
      call.resolve(toJSObject(HasResult(exists = implementation.has(key))))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Lists every stored key.
   */
  @PluginMethod
  fun keys(call: PluginCall) {
    try {
      call.resolve(toJSObject(KeysResult(keys = implementation.keys())))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reads several keys in one call. Missing keys map to null.
   */
  @PluginMethod
  fun getMany(call: PluginCall) {
    val keys =
      call.getArray("keys")?.toList<String>() ?: run {
        call.reject(SecureStorageErrorMessages.INVALID_INPUT)
        return
      }
    try {
      call.resolve(toJSObject(GetManyResult(values = implementation.getMany(keys))))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Removes every stored entry and rotates the master encryption key.
   */
  @PluginMethod
  fun clear(call: PluginCall) {
    try {
      implementation.clear()
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * iCloud Keychain does not exist on Android; no-op for API parity.
   */
  @PluginMethod
  fun setSynchronize(call: PluginCall) {
    try {
      implementation.setSynchronize(call.data.optBoolean("synchronize", false))
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Always false on Android; iCloud Keychain does not exist here.
   */
  @PluginMethod
  fun getSynchronize(call: PluginCall) {
    try {
      val result = JSObject()
      result.put("synchronize", implementation.isSynchronized())
      call.resolve(result)
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * iOS Keychain accessibility has no Android equivalent; no-op.
   */
  @PluginMethod
  fun setDefaultKeychainAccess(call: PluginCall) {
    try {
      implementation.setDefaultAccess(call.getString("access") ?: "")
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  // ---------------------------------------------------------------------------
  // Insecure tier (plaintext, group-namespaced)
  // ---------------------------------------------------------------------------

  /**
   * Configures the insecure tier at runtime, plus session obfuscation
   * overrides for both tiers. Absent values leave current settings untouched.
   */
  @PluginMethod
  fun configure(call: PluginCall) {
    try {
      val obfuscateKeys =
        if (call.data.has("obfuscateKeys")) {
          call.data.optBoolean("obfuscateKeys", false)
        } else {
          null
        }
      implementation.configure(call.getString("group"), obfuscateKeys, call.getString("obfuscationPrefix"))
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Stores a plaintext value in the insecure tier. For non-sensitive data only.
   */
  @PluginMethod
  fun setInsecure(call: PluginCall) {
    try {
      val key =
        call.getString("key") ?: throw SecureStorageError.InvalidInput(
          SecureStorageErrorMessages.INVALID_INPUT,
        )
      val value =
        call.getString("value") ?: throw SecureStorageError.InvalidInput(
          SecureStorageErrorMessages.INVALID_INPUT,
        )
      implementation.setInsecure(key, value)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reads a plaintext value from the insecure tier, or null when absent.
   */
  @PluginMethod
  fun getInsecure(call: PluginCall) {
    try {
      val key =
        call.getString("key") ?: throw SecureStorageError.InvalidInput(
          SecureStorageErrorMessages.INVALID_INPUT,
        )
      call.resolve(toJSObject(GetResult(value = implementation.getInsecure(key))))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Removes an insecure entry.
   */
  @PluginMethod
  fun removeInsecure(call: PluginCall) {
    try {
      val key =
        call.getString("key") ?: throw SecureStorageError.InvalidInput(
          SecureStorageErrorMessages.INVALID_INPUT,
        )
      call.resolve(toJSObject(RemoveResult(existed = implementation.removeInsecure(key))))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Checks insecure existence without reading.
   */
  @PluginMethod
  fun hasInsecure(call: PluginCall) {
    try {
      val key =
        call.getString("key") ?: throw SecureStorageError.InvalidInput(
          SecureStorageErrorMessages.INVALID_INPUT,
        )
      call.resolve(toJSObject(HasResult(exists = implementation.hasInsecure(key))))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Lists insecure keys of the current group.
   */
  @PluginMethod
  fun keysInsecure(call: PluginCall) {
    try {
      call.resolve(toJSObject(KeysResult(keys = implementation.keysInsecure())))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Removes every insecure entry of the current group.
   */
  @PluginMethod
  fun clearInsecure(call: PluginCall) {
    try {
      implementation.clearInsecure()
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  // ---------------------------------------------------------------------------
  // Version Information
  // ---------------------------------------------------------------------------

  /**
   * Returns the native plugin version synchronized from package.json.
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    try {
      call.resolve(toJSObject(SecureStorageVersionResult(BuildConfig.PLUGIN_VERSION)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }
}
