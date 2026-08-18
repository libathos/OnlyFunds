package compose.demo.onlyfunds

import compose.demo.onlyfunds.application.misc.ScreenOrientation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreenOrientationTest {

    @Test
    fun testScreenOrientationValues() {
        val orientations = ScreenOrientation.values()
        assertEquals(2, orientations.size)
        assertTrue(orientations.contains(ScreenOrientation.PORTRAIT))
        assertTrue(orientations.contains(ScreenOrientation.LANDSCAPE))
    }
}
