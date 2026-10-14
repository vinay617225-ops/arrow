package com.arrowescape
import org.junit.Assert.*
import org.junit.Test
class PlayRulesTest {
 @Test fun blockedMovesLoseHeartsAndGameOverPreventsMoves() {
  val a=Arrow(0,listOf(Vec(0.5f,1.5f),Vec(1.5f,1.5f)))
  val b=Arrow(1,listOf(Vec(3.5f,2.5f),Vec(3.5f,1.5f),Vec(3.5f,0.5f)))
  var s=PlayState(puzzle=Puzzle(5,listOf(a,b)))
  repeat(5) { s=PlayRules.attempt(s,0) }
  assertEquals(0,s.hearts);assertNull(s.moving);assertEquals(s,PlayRules.attempt(s,1))
  assertEquals(5,PlayRules.restart(s).hearts)
 }
 @Test fun pauseBlocksMovesAndValidMoveDoesNotLoseHeart() {
  val s=PlayState(puzzle=Engine.generate(1));val id=Engine.hint(s.puzzle)!!
  assertEquals(s.copy(paused=true),PlayRules.attempt(s.copy(paused=true),id))
  val next=PlayRules.attempt(s,id);assertEquals(5,next.hearts);assertEquals(id,next.moving)
 }
 private val p=Engine.generate(1)
 @Test fun undoRestoresLastArrowAndCompletion() {
  var s=PlayState(key="level:1",puzzle=p)
  p.arrows.forEach { s=PlayRules.finish(s,it.id) }
  assertTrue(s.complete);s=PlayRules.undo(s);assertFalse(s.complete);assertEquals(p.arrows.size-1,s.removed.size)
 }
 @Test fun restartResetsHeartsHintsAndHistory() {
  val s=PlayState(key="level:1",puzzle=p,removed=setOf(0),hearts=1,hints=3,history=listOf(0))
  val reset=PlayRules.restart(s);assertEquals(5,reset.hearts);assertEquals(0,reset.hints);assertTrue(reset.removed.isEmpty());assertTrue(reset.history.isEmpty())
 }
 @Test fun snapshotRestoresAllDurableGameState() {
  val s=PlayState(key="daily:2026-10-14",seed=42,puzzle=p,removed=setOf(1,2),hearts=3,hints=4,history=listOf(2,1),complete=false)
  val restored=PlayRules.restore(s.snapshot(),p);assertEquals(s,restored)
 }
}
