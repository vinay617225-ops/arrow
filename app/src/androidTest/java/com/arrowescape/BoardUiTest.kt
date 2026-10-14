package com.arrowescape

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BoardUiTest {
 @get:Rule val compose=createComposeRule()
 private val curved=Arrow(0,listOf(Vec(0.5f,0.5f),Vec(1.5f,0.5f),Vec(1.5f,1.5f),Vec(1.5f,2.5f)))
 private fun board(puzzle: Puzzle) {
  compose.setContent {
   var s by remember { mutableStateOf(PlayState(key="test",puzzle=puzzle)) }
   MaterialTheme {
    Column {
     GameBoard(s,onTap={ id -> s=PlayRules.attempt(s,id) },onFinish={ id -> s=PlayRules.finish(s,id) },onWarningDone={ s=s.copy(warning=null) },fit=0,modifier=Modifier.size(300.dp))
     Text(if(s.complete) "All escaped" else "In play")
     Text("${s.hearts} hearts · ${s.removed.size} escaped")
     Button(onClick={ s=PlayRules.restart(s) }) { Text("Restart fixture") }
     Button(onClick={ s=PlayRules.undo(s) }) { Text("Undo fixture") }
     Button(onClick={ s=s.copy(paused=!s.paused) }) { Text("Pause fixture") }
    }
   }
  }
 }
 private fun tapTail() {
  compose.onNodeWithTag("board").performTouchInput { click(Offset(width/6f,height/6f)) }
 }
 @Test fun tappingCurvedBodyEscapesWholeArrow() {
  board(Puzzle(5,listOf(curved)))
  tapTail()
  compose.mainClock.advanceTimeBy(3000)
  compose.onNodeWithText("All escaped").assertExists()
 }
 @Test fun curvedPathHasNoRenderingGaps() {
  board(Puzzle(5,listOf(curved)))
  val pixels=compose.onNodeWithTag("board").captureToImage().toPixelMap()
  val camera=BoardTransform(pixels.width.toFloat(),pixels.height.toFloat(),5)
  for(point in curved.line) {
   val p=camera.toScreen(point);val color=pixels[p.x.toInt(),p.y.toInt()]
   assertTrue("Path gap at $point: $color",color.red<0.3f && color.green<0.4f && color.blue<0.4f)
  }
 }
 @Test fun blockedCurveCannotEscapeThroughAnotherArrow() {
  val blocker=Arrow(1,listOf(Vec(1.5f,3.5f),Vec(2.5f,3.5f)))
  board(Puzzle(5,listOf(curved,blocker)))
  tapTail();compose.mainClock.advanceTimeBy(3000)
  compose.onNodeWithText("4 hearts · 0 escaped").assertExists()
  compose.onNodeWithText("In play").assertExists()
 }
 @Test fun undoAndRestartReplaySameArrowWithFreshAnimation() {
  compose.mainClock.autoAdvance=false
  board(Puzzle(5,listOf(curved)))
  compose.mainClock.advanceTimeByFrame()
  tapTail();compose.mainClock.advanceTimeBy(3000)
  compose.onNodeWithText("All escaped").assertExists()
  for(control in listOf("Undo fixture","Restart fixture")) {
   compose.onNodeWithText(control).performClick();compose.mainClock.advanceTimeByFrame()
   compose.onNodeWithText("5 hearts · 0 escaped").assertExists()
   tapTail();compose.mainClock.advanceTimeBy(500)
   compose.onNodeWithText("In play").assertExists()
   compose.mainClock.advanceTimeBy(2500)
   compose.onNodeWithText("All escaped").assertExists()
  }
 }
 @Test fun pausedEscapeResumesWithoutCommittingEarly() {
  compose.mainClock.autoAdvance=false
  board(Puzzle(5,listOf(curved)));compose.mainClock.advanceTimeByFrame()
  tapTail();compose.mainClock.advanceTimeBy(500)
  compose.onNodeWithText("Pause fixture").performClick();compose.mainClock.advanceTimeBy(3000)
  compose.onNodeWithText("In play").assertExists()
  compose.onNodeWithText("Pause fixture").performClick();compose.mainClock.advanceTimeBy(2500)
  compose.onNodeWithText("All escaped").assertExists()
 }
}
