package com.summondemons.safely.progress

import com.summondemons.safely.data.House
import com.summondemons.safely.time.CourtOverride

/** Court Hours (ADR-0012): fixed local sunrise/sunset, no weather. */
object CourtHours {
    /** True when the local [hour] (0..23) falls in one of the House's court ranges. */
    fun inCourt(house: House, hour: Int): Boolean = house.court.any { hour in it }

    /** Evaluates court membership with an optional debug override. */
    fun inCourt(house: House, hour: Int, court: CourtOverride): Boolean = when (court) {
        CourtOverride.Auto -> inCourt(house, hour)
        CourtOverride.In -> true
        CourtOverride.Out -> false
    }
}
