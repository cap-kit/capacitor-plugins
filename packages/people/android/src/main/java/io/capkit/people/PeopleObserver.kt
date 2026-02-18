package io.capkit.people

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.ContactsContract

/**
 * ContentObserver implementation for monitoring changes in the Android Contacts database.
 * * This class observes the ContactsContract URI and triggers a callback when insertions,
 * updates, or deletions are detected.
 */
class PeopleObserver(
  private val onChangeCallback: (type: String, ids: List<String>?) -> Unit,
) : ContentObserver(Handler(Looper.getMainLooper())) {
  companion object {
    private const val SETTLE_WINDOW_MS = 350L

    /**
     * The base URI for contacts that this observer targets.
     */
    val CONTACTS_URI: Uri = ContactsContract.Contacts.CONTENT_URI
  }

  private val debounceHandler = Handler(Looper.getMainLooper())
  private val pendingIds = linkedSetOf<String>()
  private var hasGeneralChange = false
  private var lastChangeAtMs = 0L

  private val dispatchRunnable =
    object : Runnable {
      override fun run() {
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - lastChangeAtMs
        if (elapsed < SETTLE_WINDOW_MS) {
          debounceHandler.postDelayed(this, SETTLE_WINDOW_MS - elapsed)
          return
        }

        val ids = if (hasGeneralChange) null else pendingIds.toList()
        pendingIds.clear()
        hasGeneralChange = false
        onChangeCallback("update", ids)
      }
    }

  // -----------------------------------------------------------------------------
  // Lifecycle / Callbacks
  // -----------------------------------------------------------------------------

  /**
   * Called when a content change occurs.
   *
   * @param selfChange True if this is a self-triggered change.
   * @param uri The URI of the changed content.
   */
  override fun onChange(
    selfChange: Boolean,
    uri: Uri?,
  ) {
    lastChangeAtMs = SystemClock.elapsedRealtime()

    // If URI is null, emit a coalesced general update without specific IDs.
    if (uri == null) {
      hasGeneralChange = true
    } else {
      // Attempt to extract the specific contact ID from the URI segment.
      uri.lastPathSegment?.let { pendingIds.add(it) }
    }

    debounceHandler.removeCallbacks(dispatchRunnable)
    debounceHandler.postDelayed(dispatchRunnable, SETTLE_WINDOW_MS)
  }

  fun dispose() {
    debounceHandler.removeCallbacks(dispatchRunnable)
  }
}
