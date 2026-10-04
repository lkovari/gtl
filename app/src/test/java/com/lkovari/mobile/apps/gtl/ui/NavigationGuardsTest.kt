package com.lkovari.mobile.apps.gtl.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for the blank-screen bug (white-crash.md): a fast double tap on a secondary
 * screen's back arrow popped the start destination and left the NavHost empty.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class NavigationGuardsTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var nav: NavHostController

    private enum class Back { Unguarded, Guarded, GuardedWithArgument }

    private fun setHost(back: Back) {
        rule.setContent {
            nav = rememberNavController()
            NavHost(navController = nav, startDestination = MAIN) {
                composable(MAIN) {
                    Text("main", modifier = Modifier.testTag(MAIN))
                }
                composable(SECOND) {
                    val onBack: () -> Unit = when (back) {
                        Back.Unguarded -> {
                            { nav.popBackStack() }
                        }
                        Back.Guarded -> rememberGuardedPop(nav)
                        Back.GuardedWithArgument -> {
                            val withArgument = dropUnlessResumedWith<Long> { nav.popBackStack() }
                            val action: () -> Unit = { withArgument(42L) }
                            action
                        }
                    }
                    Button(onClick = onBack, modifier = Modifier.testTag(BACK)) {
                        Text("back")
                    }
                }
            }
        }
        rule.runOnIdle { nav.navigate(SECOND) }
        rule.waitForIdle()
    }

    /** Two taps on the back arrow, the second one while the exit transition is still running. */
    private fun doubleTapBack() {
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(BACK).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(TRANSITION_MIDPOINT_MS)
        rule.onNodeWithTag(BACK).performClick()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
    }

    @Test
    fun unguardedDoubleBack_emptiesBackStack_reproducesBug() {
        setHost(Back.Unguarded)

        doubleTapBack()

        rule.runOnIdle { assertNull(nav.currentBackStackEntry) }
        rule.onNodeWithTag(MAIN).assertDoesNotExist()
    }

    @Test
    fun guardedDoubleBack_keepsStartDestination() {
        setHost(Back.Guarded)

        doubleTapBack()

        rule.runOnIdle { assertEquals(MAIN, nav.currentDestination?.route) }
        rule.onNodeWithTag(MAIN).assertExists()
    }

    @Test
    fun guardedWithArgumentDoubleBack_keepsStartDestination() {
        setHost(Back.GuardedWithArgument)

        doubleTapBack()

        rule.runOnIdle { assertEquals(MAIN, nav.currentDestination?.route) }
        rule.onNodeWithTag(MAIN).assertExists()
    }

    @Test
    fun guardedSingleBack_stillNavigatesBack() {
        setHost(Back.Guarded)

        rule.onNodeWithTag(BACK).performClick()
        rule.waitForIdle()

        rule.runOnIdle { assertEquals(MAIN, nav.currentDestination?.route) }
        rule.onNodeWithTag(BACK).assertDoesNotExist()
    }

    private companion object {
        const val MAIN = "main"
        const val SECOND = "second"
        const val BACK = "back"
        const val TRANSITION_MIDPOINT_MS = 150L
    }
}
