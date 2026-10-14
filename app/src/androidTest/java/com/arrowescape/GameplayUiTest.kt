package com.arrowescape
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
class GameplayUiTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Test fun homeToGameplayHintRestartPause() {
  compose.waitUntil(10000) { compose.onAllNodesWithText("Continue · Level 1").fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("Continue · Level 1").performClick()
  compose.waitUntil(10000) { compose.onAllNodesWithTag("board").fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithTag("board").assertExists();compose.onNodeWithTag("hearts").assertExists()
  compose.onNodeWithTag("hint").performClick();compose.onNodeWithText("Follow the glowing path.").assertExists()
  compose.onNodeWithTag("restart").performClick();compose.onNodeWithText("Tap a path with a clear exit.").assertExists()
  compose.onNodeWithText("Ⅱ Pause").performClick();compose.onNodeWithText("Resume").performClick()
  compose.onNodeWithTag("board").assertExists()
 }
}
