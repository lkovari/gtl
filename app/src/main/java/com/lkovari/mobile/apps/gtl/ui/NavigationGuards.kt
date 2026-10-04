package com.lkovari.mobile.apps.gtl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController

/**
 * Back action for a NavHost destination that runs only while that destination is RESUMED.
 *
 * After the first pop, the leaving destination stays on screen during its exit transition but is
 * no longer RESUMED. A second tap on its back arrow is dropped here; an unguarded
 * `popBackStack()` would pop the start destination and leave the NavHost empty (blank screen).
 * Must be called inside the destination's `composable { }` block.
 */
@Composable
fun rememberGuardedPop(nav: NavController): () -> Unit = dropUnlessResumed { nav.popBackStack() }

/**
 * One-argument variant of [dropUnlessResumed]: [block] runs only while the current
 * [LocalLifecycleOwner] (the NavHost destination) is at least RESUMED.
 */
@Composable
fun <T> dropUnlessResumedWith(block: (T) -> Unit): (T) -> Unit {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBlock by rememberUpdatedState(block)
    return remember(lifecycleOwner) {
        { value ->
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                currentBlock(value)
            }
        }
    }
}
