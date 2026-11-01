/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.qs.composefragment

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.android.systemui.penguin.CosFeature
import com.android.systemui.penguin.rememberCosFeature
import com.android.systemui.qs.shared.style.LocalGlassBlurAlpha
import com.android.systemui.qs.ui.composable.LocalQsHostTransition
import kotlin.math.PI
import kotlin.math.pow
import kotlinx.coroutines.delay

private const val STAGGER_MS = 16L
private const val HIDE_RESPONSE = 0.1f

/** The panel starts to show at this fraction of its opening, glass and modules together. */
private const val EXPANSION_REVEAL_START = 0.25f

/** The panel is fully shown at this fraction of its opening. */
private const val EXPANSION_REVEAL_END = 0.85f

/**
 * How much of the panel to show at [expansion], 0 when closed and 1 when open. SurfaceFlinger draws
 * the glass from blur regions and the modules fade on springs of their own, so without a shared
 * curve a slow drag shows the content at full strength over glass that has barely started.
 */
internal fun cosExpansionReveal(expansion: Float): Float {
    val t =
        ((expansion - EXPANSION_REVEAL_START) / (EXPANSION_REVEAL_END - EXPANSION_REVEAL_START))
            .coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

@Composable
fun Modifier.cosModuleReveal(index: Int): Modifier {
    if (!rememberCosFeature(CosFeature.Shade)) return this
    var scale by remember { mutableFloatStateOf(0.8f) }
    var alpha by remember { mutableFloatStateOf(0f) }
    val scaleSpring = remember { CosHooks.newSpring(0.8f) { scale = it } }
    val alphaSpring = remember { CosHooks.newSpring(0f) { alpha = it } }
    if (scaleSpring == null || alphaSpring == null) return this

    val leaving = LocalQsHostTransition.current?.leaving == true
    LaunchedEffect(leaving) {
        if (leaving) {
            scaleSpring.animateTo(0.8f, response = HIDE_RESPONSE)
            alphaSpring.animateTo(0f, response = HIDE_RESPONSE)
        } else {
            delay(index * STAGGER_MS)
            scaleSpring.animateTo(1f, response = 0.2f + index * 0.01f)
            alphaSpring.animateTo(1f, response = 0.2f + index * 0.01f)
        }
    }

    val shown = LocalGlassBlurAlpha.current
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha * shown()
    }
}

@Composable
fun Modifier.cosHeaderHide(leaving: Boolean): Modifier {
    if (!rememberCosFeature(CosFeature.Shade)) return this
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(leaving) {
        alpha.animateTo(
            if (leaving) 0f else 1f,
            spring(dampingRatio = 1f, stiffness = (2 * PI / HIDE_RESPONSE).pow(2).toFloat()),
        )
    }
    return graphicsLayer { this.alpha = alpha.value }
}
