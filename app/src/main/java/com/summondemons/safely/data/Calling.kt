package com.summondemons.safely.data

/** What a demon is summoned for (ADR-0011): the badge label and its explanation. */
enum class Calling(val label: String, val tooltip: String) {
    Warlord("Warlord", "Summon him for an army."),
    Tutor("Tutor", "Summon him to learn things."),
    Oracle("Oracle", "Summon him to know things."),
    Treasurer("Treasurer", "Summon him for loot."),
    Menace("Menace", "Summon him at your own risk."),
}
