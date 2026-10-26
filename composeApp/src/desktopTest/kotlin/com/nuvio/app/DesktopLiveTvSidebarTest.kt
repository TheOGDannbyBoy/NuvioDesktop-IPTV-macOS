package com.nuvio.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioTheme
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopLiveTvSidebarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun liveTvIsVisibleInSidebarAndOpensFromIt() {
        val liveTvOpen = mutableStateOf(false)
        compose.setContent {
            NuvioTheme {
                Box(Modifier.size(192.dp, 560.dp)) {
                    DesktopHoverSidebar(
                        selectedTab = AppScreenTab.Home,
                        onTabSelected = {},
                        liveTvAvailable = true,
                        liveTvSelected = liveTvOpen.value,
                        onLiveTvSelected = { liveTvOpen.value = true },
                        onProfileSelected = {},
                        onAddProfileRequested = {},
                        sidebarExpanded = true,
                    )
                }
            }
        }

        compose.onNodeWithText("Live TV").assertExists().performTouchInput { click() }
        compose.runOnIdle { assertTrue(liveTvOpen.value) }
    }
}
