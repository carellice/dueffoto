package app.dueffoto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeTest {
    @Test
    fun everyFilmStaysInRange() {
        val out = FloatArray(3)
        val steps = listOf(0f, 0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1f)
        for (film in Films.all) {
            val g = Grade(film)
            for (r in steps) for (gr in steps) for (b in steps) {
                g.apply(r, gr, b, out)
                out.forEach { assertTrue("${film.id} esce da 0..1: $it", it in 0f..1f) }
            }
        }
    }

    @Test
    fun zeroStrengthKeepsTheOriginal() {
        val out = FloatArray(3)
        Grade(Films.analog.first(), strength = 0f).apply(0.2f, 0.5f, 0.8f, out)
        assertEquals(0.2f, out[0], 1e-6f)
        assertEquals(0.5f, out[1], 1e-6f)
        assertEquals(0.8f, out[2], 1e-6f)
    }

    @Test
    fun blackAndWhiteHasNoColor() {
        val out = FloatArray(3)
        Grade(Films.byId("fm2")!!).apply(0.9f, 0.2f, 0.1f, out)
        assertEquals(out[0], out[1], 1e-6f)
        assertEquals(out[1], out[2], 1e-6f)
    }

    @Test
    fun idsAreUnique() {
        assertEquals(Films.all.size, Films.all.map { it.id }.toSet().size)
        assertFalse(Films.analog.any { it.kind != Kind.ANALOG })
    }
}
