package me.retrodaredevil.clapdetector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternTest {
    @Test
    fun `pattern test`(){
        val time = arrayOf<Long>(0)
        val patternRecorder = PatternRecorder({ time[0] }, 1200)
        patternRecorder.clap()

        time[0] = 1000
        patternRecorder.clap()

        time[0] = 1500
        patternRecorder.clap()

        assertEquals(listOf<Long>(1000, 500), patternRecorder.currentIntervals)
        time[0] = 1500 + 1300
        patternRecorder.clap()
        assertTrue(patternRecorder.currentIntervals.isEmpty())
    }
}