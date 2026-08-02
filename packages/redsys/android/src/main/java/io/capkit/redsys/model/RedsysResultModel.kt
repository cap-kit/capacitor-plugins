package io.capkit.redsys.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Canonical result models for the Redsys plugin (Android).
 *
 * These DTOs mirror the public JavaScript payloads returned by the plugin
 * methods and are serialized to a JSObject bridge payload via
 * kotlinx.serialization. Nullable/defaulted fields are omitted when unset
 * (the encoding Json instance does NOT set encodeDefaults), matching the
 * optional TypeScript properties.
 *
 * These models are consumed ONLY by the bridge (RedsysPlugin) layer.
 */

@Serializable
data class RedsysPaymentResponseOK(
  val code: Int,
  val desc: String,
  val amount: String,
  val currency: String,
  val order: String,
  val merchantCode: String,
  val terminal: String,
  val responseCode: String,
  val authorisationCode: String,
  val transactionType: String,
  val securePayment: String,
  val signature: String,
  val cardNumber: String,
  val cardBrand: String,
  val cardCountry: String,
  val cardType: String,
  val expiryDate: String,
  @SerialName("merchantIdentifier")
  val merchantIdentifier: String? = null,
  @SerialName("consumerLanguage")
  val consumerLanguage: String? = null,
  @SerialName("date")
  val date: String? = null,
  @SerialName("hour")
  val hour: String? = null,
  @SerialName("merchantData")
  val merchantData: String? = null,
  @SerialName("extraParams")
  val extraParams: Map<String, String>? = null,
)

@Serializable
data class RedsysWebPaymentInitResult(
  val base64Data: String,
  val signature: String? = null,
)

@Serializable
data class HashResult(
  val signature: String,
)

@Serializable
data class PluginVersionResult(
  val version: String,
)