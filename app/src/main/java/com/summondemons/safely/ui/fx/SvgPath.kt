package com.summondemons.safely.ui.fx

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

private val cache = HashMap<String, Path>()

/** Parses SVG path data once (glyphs, compass rose, grimoire). Main thread only. */
fun svgPath(data: String): Path = cache.getOrPut(data) { PathParser().parsePathString(data).toPath() }
