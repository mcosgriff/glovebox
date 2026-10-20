package io.cosgriff.glovebox.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import io.cosgriff.glovebox.R

private const val IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

/**
 * Puts [text] on the clipboard. A [sensitive] one (the VIN) is marked so Android hides it in its clipboard
 * preview. Android 13 and later say "Copied" themselves; earlier versions get a short message here.
 */
internal fun copyText(context: Context, label: String, text: String, sensitive: Boolean) {
    val clip = ClipData.newPlainText(label, text).apply {
        // ClipDescription.EXTRA_IS_SENSITIVE is API 33; before that the same key, spelled out, is what keyboards
        // and clipboard tools read.
        if (sensitive) description.extras = PersistableBundle().apply { putBoolean(IS_SENSITIVE, true) }
    }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, context.getString(R.string.copied, label), Toast.LENGTH_SHORT).show()
}
