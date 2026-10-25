package io.cosgriff.glovebox.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Opens a web page in the browser. Returns false if the phone has nothing that can. */
fun Context.openWebPage(url: String): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (_: ActivityNotFoundException) {
    false
}
