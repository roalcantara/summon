package com.summondemons.safely.ui.fx

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Rising heat streaks tinted with the accent, strongest near the bottom. Premultiplied output. */
private const val HAZE_AGSL = """
uniform float2 size;
uniform float time;
layout(color) uniform half4 tint;

half4 main(float2 p) {
    float2 uv = p / size;
    float wave = 0.5 * sin(uv.x * 18.0 + time * 1.3) + 0.5 * sin(uv.x * 7.0 - time * 0.7 + uv.y * 4.0);
    float rise = fract(uv.y * 3.0 + time * 0.15 + wave * 0.08);
    float streak = smoothstep(0.0, 0.5, rise) * (1.0 - smoothstep(0.5, 1.0, rise));
    float a = streak * 0.22 * uv.y;
    return half4(tint.rgb * a, a);
}
"""

/** Wobbles whatever it is applied to, like air over a fire. */
private const val DISTORT_AGSL = """
uniform shader content;
uniform float time;
uniform float strength;

half4 main(float2 p) {
    float dx = sin(p.y * 0.08 + time * 3.1) * strength * 0.6;
    float dy = sin(p.y * 0.05 + time * 2.0) * strength * 0.3;
    return content.eval(p + float2(dx, dy));
}
"""

private const val BREATH_SECONDS = 4f
private const val FALLBACK_ALPHA = 0.18f
private val DistortionStrength = 2.dp

/** Accent heat haze overlay: AGSL on API 33+, a slowly breathing gradient below. */
@Composable
fun HeatShimmer(color: Color, modifier: Modifier = Modifier) {
    if (LocalReduceMotion.current) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ShaderHaze(color, modifier)
    } else {
        val time by rememberFrameTimeMs()
        Canvas(modifier) {
            val breath = 0.5f + 0.5f * sin(time / 1_000f * 2f * PI.toFloat() / BREATH_SECONDS)
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = FALLBACK_ALPHA * breath))))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderHaze(color: Color, modifier: Modifier) {
    val shader = remember { RuntimeShader(HAZE_AGSL) }
    val time by rememberFrameTimeMs()
    Canvas(modifier) {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("time", time / 1_000f)
        shader.setColorUniform("tint", color.toArgb())
        drawRect(ShaderBrush(shader))
    }
}

/** Applies a gentle heat distortion to [content] on API 33+; plain content elsewhere or with reduced motion. */
@Composable
fun HeatDistorted(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !LocalReduceMotion.current) {
        Distorted(modifier, content)
    } else {
        Box(modifier) { content() }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Distorted(modifier: Modifier, content: @Composable () -> Unit) {
    val shader = remember { RuntimeShader(DISTORT_AGSL) }
    val time by rememberFrameTimeMs()
    val strength = with(LocalDensity.current) { DistortionStrength.toPx() }
    Box(
        modifier.graphicsLayer {
            shader.setFloatUniform("time", time / 1_000f)
            shader.setFloatUniform("strength", strength)
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        },
    ) { content() }
}
