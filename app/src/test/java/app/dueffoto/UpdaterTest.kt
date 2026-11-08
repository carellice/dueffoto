package app.dueffoto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test
    fun comparesVersions() {
        assertTrue(Updater.isNewer("1.0.1", "1.0.0"))
        assertTrue(Updater.isNewer("1.10.0", "1.9.9"))
        assertFalse(Updater.isNewer("1.0.0", "1.0.0"))
        assertFalse(Updater.isNewer("0.9", "1.0.0"))
    }
}
