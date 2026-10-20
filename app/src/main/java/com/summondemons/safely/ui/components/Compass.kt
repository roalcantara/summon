package com.summondemons.safely.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import com.summondemons.safely.R
import com.summondemons.safely.ritual.compassIndex

/** Name of the nearest compass point, e.g. for "face {direction}". */
@Composable
fun headingLabel(degrees: Float): String = stringArrayResource(R.array.compass_points)[compassIndex(degrees)]

/** Abbreviation of [headingLabel] ("N", "NE"…), for the Direction badge glyph. */
@Composable
fun headingLetter(degrees: Float): String = stringArrayResource(R.array.compass_letters)[compassIndex(degrees)]
