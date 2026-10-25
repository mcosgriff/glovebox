package io.cosgriff.glovebox

import android.app.Activity
import android.os.Bundle
import dagger.hilt.android.EntryPointAccessors
import io.cosgriff.glovebox.notify.Destination
import io.cosgriff.glovebox.notify.NotifyEntryPoint
import io.cosgriff.glovebox.notify.appIntent

/**
 * Where the launcher shortcuts, the notifications and the widget point (#235, `res/xml/shortcuts.xml`,
 * `launchIntent`). It hands their destination to [MainActivity] through `PendingDestination` and closes,
 * drawing nothing. It isn't exported, so no other app can start it: [MainActivity] is, as the launcher's,
 * and takes no destination from its intent, so another app can't choose the screen the user lands on.
 *
 * Android starts a static shortcut with `FLAG_ACTIVITY_CLEAR_TASK`, which pointed at [MainActivity] would
 * end the running app and lose whatever was half done in it. This activity has a task of its own (an empty
 * `taskAffinity`), so that flag clears only that one, and the app's own task is brought forward as it was.
 */
class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EntryPointAccessors.fromApplication(applicationContext, NotifyEntryPoint::class.java).pendingDestination().set(Destination.from(intent))
        startActivity(appIntent())
        finish()
    }
}
