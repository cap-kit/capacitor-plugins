package io.capkit.rank.utils

import android.net.Uri

/**
 * Pure utility helpers for Rank Android.
 *
 * This object contains only deterministic URI builders and does not perform I/O.
 */
object RankUtils {
  fun marketDetailsUri(id: String): Uri = Uri.parse("market://details?id=$id")

  fun playStoreDetailsHttpsUri(id: String): Uri = Uri.parse("https://play.google.com/store/apps/details?id=$id")

  fun marketSearchUri(terms: String): Uri = Uri.parse("market://search?q=$terms")

  fun playStoreDeveloperUri(devId: String): Uri = Uri.parse("https://play.google.com/store/apps/dev?id=$devId")

  fun playStoreCollectionUri(name: String): Uri = Uri.parse("https://play.google.com/store/apps/collection/$name")
}
