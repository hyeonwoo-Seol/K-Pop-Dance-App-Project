package com.example.kpopdancepracticeai.ui.motion

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.abs

/**
 * iOS 감성의 스크롤/플링 감쇠를 흉내 내기 위한 physics-based motion preset.
 * - Normal: 리스트처럼 멀리 미끄러지며 천천히 감속
 * - Fast: picker / page snapping처럼 더 빨리 멈춤
 */
enum class IosDecelerationPreset(
    internal val frictionMultiplier: Float,
    val dampingRatio: Float,
    val stiffness: Float
) {
    Normal(
        frictionMultiplier = 0.72f,
        dampingRatio = 0.90f,
        stiffness = Spring.StiffnessLow
    ),
    Fast(
        frictionMultiplier = 1.18f,
        dampingRatio = 0.98f,
        stiffness = Spring.StiffnessMediumLow
    )
}

@Composable
fun rememberIosLikeFlingBehavior(
    preset: IosDecelerationPreset = IosDecelerationPreset.Normal
): FlingBehavior {
    val decaySpec = remember(preset) {
        exponentialDecay<Float>(frictionMultiplier = preset.frictionMultiplier)
    }

    return remember(decaySpec) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                if (abs(initialVelocity) < 1f) return initialVelocity

                var remainingVelocity = initialVelocity
                val animationState = AnimationState(
                    initialValue = 0f,
                    initialVelocity = initialVelocity
                )

                animationState.animateDecay(decaySpec) {
                    val delta = value - previousValue
                    val consumed = scrollBy(delta)
                    val unconsumed = delta - consumed
                    remainingVelocity = velocity

                    if (abs(unconsumed) > 0.5f || abs(velocity) < 1f) {
                        cancelAnimation()
                    }
                }

                return remainingVelocity
            }
        }
    }
}

fun iosSpringSpec(
    preset: IosDecelerationPreset = IosDecelerationPreset.Normal
): SpringSpec<Float> = spring(
    dampingRatio = preset.dampingRatio,
    stiffness = preset.stiffness
)
