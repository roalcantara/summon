package com.summondemons.safely.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class GameClockTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(hour: Int, minute: Int = 0, second: Int = 0) = ZonedDateTime.of(2026, 10, 16, hour, minute, second, 0, zone)

    @Test
    fun epochDayIsTheLocalCalendarDay() {
        assertEquals(LocalDate.of(2026, 10, 16).toEpochDay(), at(0).epochDay())
        assertEquals(LocalDate.of(2026, 10, 16).toEpochDay(), at(23, 59, 59).epochDay())
        assertEquals(at(0).epochDay() + 1, at(23, 59, 59).plusSeconds(1).epochDay())
    }

    @Test
    fun hoursToMidnightRoundUpAndAreAtLeastOne() {
        assertEquals(24, hoursToMidnight(at(0)))
        assertEquals(7, hoursToMidnight(at(17)))
        assertEquals(8, hoursToMidnight(at(16, 30))) // 7.5 h rounds up
        assertEquals(1, hoursToMidnight(at(23, 30)))
        assertEquals(1, hoursToMidnight(at(23, 59, 59)))
    }
}
