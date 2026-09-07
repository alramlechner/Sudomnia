package name.lechners.sudomnia.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    @Test
    fun everyAidIsOnByDefault() {
        val d = Settings()
        assertTrue(d.showConflicts)
        assertTrue(d.highlightSameDigit)
        assertTrue(d.highlightPeers)
        assertTrue(d.dimCompletedDigits)
        assertFalse(d.allAidsOff)
    }

    /**
     * The marker in the header must mean what it says: it may only appear when
     * nothing at all is being given away. One aid left on is still help.
     */
    @Test
    fun theNoAidsMarkerNeedsEverySwitchOff() {
        val off = Settings(false, false, false, false)
        assertTrue(off.allAidsOff)

        assertFalse(off.copy(showConflicts = true).allAidsOff)
        assertFalse(off.copy(highlightSameDigit = true).allAidsOff)
        assertFalse(off.copy(highlightPeers = true).allAidsOff)
        assertFalse(off.copy(dimCompletedDigits = true).allAidsOff)
    }
}
