package io.cosgriff.glovebox

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.cosgriff.glovebox.notify.Destination

/**
 * Where the launcher shortcuts point (#235, `res/xml/shortcuts.xml`). It hands the shortcut to
 * [MainActivity] the way a notification does and closes, drawing nothing.
 *
 * Android starts a static shortcut with `FLAG_ACTIVITY_CLEAR_TASK`, which pointed at [MainActivity] would
 * end the running app and lose whatever was half done in it. This activity has a task of its own (an empty
 * `taskAffinity`), so that flag clears only that one, and the app's own task is brought forward as it was.
 */
class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only a shortcut's own task is passed on; anything else opens the app as an ordinary launch.
        val shortcut = Destination.from(intent) as? Destination.Shortcut
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                shortcut?.putInto(this)
            },
        )
        finish()
    }
}
