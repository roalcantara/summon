package com.summondemons.safely.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object Grimoire

@Serializable
data class Ritual(val demonId: String)

@Serializable
data class Profile(val demonId: String, val isNewSummon: Boolean)
