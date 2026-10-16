package com.summondemons.safely.progress

import com.summondemons.safely.data.House
import org.junit.Assert.assertEquals
import org.junit.Test

class CourtHoursTest {

    private fun hours(house: House) = (0..23).filter { CourtHours.inCourt(house, it) }

    @Test
    fun solHoldsCourtNineToNoonAndThreeToSunset() {
        assertEquals((9..11) + (15..17), hours(House.Sol))
    }

    @Test
    fun venusHoldsCourtFromSunriseToNoon() {
        assertEquals((6..11).toList(), hours(House.Venus))
    }

    @Test
    fun rangeEndsAreExclusive() {
        assertEquals(false, CourtHours.inCourt(House.Sol, 12))
        assertEquals(false, CourtHours.inCourt(House.Sol, 18))
        assertEquals(false, CourtHours.inCourt(House.Venus, 12))
        assertEquals(true, CourtHours.inCourt(House.Venus, 6))
    }
}
