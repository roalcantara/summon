package com.summondemons.safely.progress

import com.summondemons.safely.data.House

/** Court Hours (ADR-0012): fixed local sunrise/sunset, no weather. */
object CourtHours {
    /** True when the local [hour] (0..23) falls in one of the House's court ranges. */
    fun inCourt(house: House, hour: Int): Boolean = house.court.any { hour in it }
}
