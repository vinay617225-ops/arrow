package com.arrowescape

import org.junit.Assert.*
import org.junit.Test

class EngineTest {
 private fun arrow(id: Int,vararg p: Pair<Int,Int>)=Arrow(id,p.map { Vec(it.first+0.5f,it.second+0.5f) })
 @Test fun validateEveryInitialLevelAndSolution() {
  for(level in 1..100) {
   val p=Engine.generate(level);assertTrue("level $level",Engine.validate(p));assertEquals(p,Engine.generate(level))
   val solution=Engine.solution(p)!!;val remaining=p.arrows.toMutableList()
   for(id in solution) {
    val a=remaining.single { it.id==id };assertNull(Engine.blocker(a,remaining,p.size))
    // Independent discrete full-body simulation: append one head cell, drop
    // one tail cell, until every cell is outside. Compare every intermediate
    // occupancy against all stationary arrows.
    var body=a.cells
    val occupied=remaining.filter { it.id!=id }.flatMap { it.cells }.toSet()
    repeat(p.size*2+a.cells.size) {
     body=body.drop(1)+(body.last()+a.direction)
     assertTrue("swept collision at level $level",body.none { it in occupied })
    }
    assertTrue(body.none { it.x in 0f..p.size.toFloat() && it.y in 0f..p.size.toFloat() })
    remaining.remove(a)
   }
   assertTrue(remaining.isEmpty())
  }
 }
 @Test fun straightLCurvedUAndWindingGeometry() {
  val shapes=listOf(
   arrow(0,0 to 0,1 to 0,2 to 0),
   arrow(1,0 to 0,1 to 0,1 to 1),
   arrow(2,0 to 0,1 to 0,2 to 0,2 to 1,1 to 1,0 to 1),
   arrow(3,0 to 0,1 to 0,1 to 1,2 to 1,2 to 2,3 to 2)
  )
  shapes.forEach { a -> assertTrue(a.valid(7));assertEquals(a.cells.toSet(),a.occupiedCells);assertEquals(a.cells.first(),a.tail);assertEquals(a.cells.last(),a.head);assertTrue(a.line.zipWithNext().all { (u,v)->(v-u).length()>0f }) }
 }
 @Test fun rejectsDisconnectedDuplicateOffGridAndSelfCrossingExit() {
  assertFalse(arrow(0,0 to 0,2 to 0).valid(7))
  assertFalse(arrow(0,0 to 0,1 to 0,0 to 0).valid(7))
  assertFalse(Arrow(0,listOf(Vec(1f,1f),Vec(2f,1f))).valid(7))
  assertFalse(arrow(0,0 to 0,1 to 0,2 to 0,2 to 1,1 to 1,1 to 2,2 to 2,2 to 3,1 to 3,1 to 2).valid(7))
  assertFalse(arrow(0,0 to 0,1 to 0,2 to 0,2 to 1,2 to 2,1 to 2,1 to 1).valid(7))
 }
 @Test fun blockingChecksEntireRayNotJustFirstCell() {
  val a=arrow(0,1 to 2,2 to 2);val b=arrow(1,5 to 1,5 to 2,5 to 3)
  assertEquals(b,Engine.blocker(a,listOf(a,b),7))
  val behind=arrow(2,0 to 1,0 to 2)
  assertNull(Engine.blocker(a,listOf(a,behind),7))
 }
 @Test fun parallelPathsDoNotBlock() {
  val a=arrow(0,0 to 2,1 to 2);val b=arrow(1,3 to 3,4 to 3)
  assertNull(Engine.blocker(a,listOf(a,b),7))
 }
 @Test fun solverRejectsCyclesAndHintIsNeverBlocked() {
  val a=arrow(0,0 to 1,1 to 1);val b=arrow(1,3 to 1,2 to 1)
  assertNull(Engine.solution(Puzzle(5,listOf(a,b))))
  assertNull(Engine.hint(Puzzle(5,listOf(a,b))))
  for(level in 1..100) { val p=Engine.generate(level);val id=Engine.hint(p)!!;assertNull(Engine.blocker(p.arrows.single { it.id==id },p.arrows,p.size)) }
 }
 @Test fun arcLengthSnakeIsContinuousAndExitsCompletely() {
  val a=arrow(0,0 to 0,1 to 0,1 to 1,1 to 2)
  assertTrue(a.hit(Vec(1f,0.5f)));assertFalse(a.hit(Vec(3f,3f)))
  assertEquals(a.head,a.body(0f).last())
  for(i in 0..100) {
   val s=i*0.07f
   assertTrue((a.at(s+0.01f)-a.at(s)).length()<=0.0101f)
   assertEquals(a.length,absLength(a.body(s)),0.01f)
   assertTrue((a.body(s).last()-(a.head+a.direction*s)).length()<0.00001f)
  }
  assertTrue(a.body(12f).all { it.y>7f })
 }
 private fun absLength(p: List<Vec>)=p.zipWithNext().sumOf { (a,b)->(b-a).length().toDouble() }.toFloat()
}
