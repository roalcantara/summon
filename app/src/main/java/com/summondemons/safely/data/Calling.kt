package com.summondemons.safely.data

import androidx.annotation.StringRes
import com.summondemons.safely.R

/** What a demon is summoned for (ADR-0011): the badge label and its explanation, as string resources. */
enum class Calling(@param:StringRes val label: Int, @param:StringRes val tooltip: Int) {
    Warlord(R.string.calling_warlord_label, R.string.calling_warlord_tooltip),
    Tutor(R.string.calling_tutor_label, R.string.calling_tutor_tooltip),
    Oracle(R.string.calling_oracle_label, R.string.calling_oracle_tooltip),
    Treasurer(R.string.calling_treasurer_label, R.string.calling_treasurer_tooltip),
    Menace(R.string.calling_menace_label, R.string.calling_menace_tooltip),
}
