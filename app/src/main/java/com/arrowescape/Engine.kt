package com.arrowescape

import kotlin.math.*
import kotlin.random.Random

data class Vec(val x: Float, val y: Float) {
 operator fun plus(b: Vec) = Vec(x+b.x,y+b.y)
 operator fun minus(b: Vec) = Vec(x-b.x,y-b.y)
 operator fun times(s: Float) = Vec(x*s,y*s)
 fun dot(b: Vec) = x*b.x+y*b.y
 fun length() = sqrt(dot(this))
 fun unit() = this*(1f/length())
}
fun distance(p: Vec, a: Vec, b: Vec): Float {
 val d=b-a; val t=if(d.dot(d)==0f) 0f else ((p-a).dot(d)/d.dot(d)).coerceIn(0f,1f)
 return (p-(a+d*t)).length()
}
// Curves are flattened once with <= 0.002 board-unit error. Rendering, hits,
// animation and touch use this identical continuous centerline. Grid occupancy
// validates the complete snake escape before animation starts.
fun rounded(points: List<Vec>): List<Vec> {
 val out=mutableListOf(points.first())
 for(i in 1 until points.lastIndex) {
  val p=points[i]
  if((p-points[i-1]).unit()==(points[i+1]-p).unit()) { out.add(p);continue }
  val a=p+(points[i-1]-p).unit()*0.24f; val b=p+(points[i+1]-p).unit()*0.24f
  out.add(a)
  for(j in 1..16) { val t=j/16f; out.add(a*((1-t)*(1-t))+p*(2*t*(1-t))+b*(t*t)) }
 }
 out.add(points.last()); return out
}
data class Arrow(val id: Int, val cells: List<Vec>) {
 val line = rounded(cells)
 private val distances=FloatArray(line.size).also { d -> for(i in 1 until line.size) d[i]=d[i-1]+(line[i]-line[i-1]).length() }
 val occupiedCells get() = cells.toSet()
 val tail get() = line.first()
 fun valid(size: Int): Boolean = cells.size>=2 && cells.distinct().size==cells.size &&
  cells.all { it.x % 1f==0.5f && it.y % 1f==0.5f && it.x in 0.5f..(size-0.5f) && it.y in 0.5f..(size-0.5f) } &&
  cells.zipWithNext().all { (a,b) -> abs(a.x-b.x)+abs(a.y-b.y)==1f } && escapeCells(size).none { it in cells }
 fun escapeCells(size: Int): List<Vec> {
  val result=mutableListOf<Vec>(); var p=cells.last()+direction
  while(p.x in 0f..size.toFloat() && p.y in 0f..size.toFloat()) { result.add(p);p+=direction }
  return result
 }

 val head get()=line.last()
 val direction get()=cells.last()-cells[cells.lastIndex-1]
 val length get()=distances.last()
 fun at(s: Float): Vec {
  if(s<=0f) return tail
  if(s>=length) return head+direction*(s-length)
  for(i in 1 until line.size) if(s<=distances[i]) {
   val t=(s-distances[i-1])/(distances[i]-distances[i-1])
   return line[i-1]+(line[i]-line[i-1])*t
  }
  return head
 }
 fun body(travel: Float): List<Vec> {
  val result=mutableListOf(at(travel))
  for(i in 1 until line.size) if(distances[i]>travel && distances[i]<travel+length) result.add(line[i])
  if(travel<length) result.add(head)
  result.add(at(travel+length)); return result.distinct()
 }
 fun hit(p: Vec, tolerance: Float=0.28f)=line.zipWithNext().minOf { (a,b)-> distance(p,a,b) }<=tolerance
}
data class Puzzle(val size: Int, val arrows: List<Arrow>)
object Engine {
 const val radius=0.085f
 // Every body point follows the same ordered centerline. Its only new occupied
 // cells are the head's forward ray. Checking this complete ray is equivalent
 // to sweeping the entire snake until the tail leaves, without time-step gaps.
 fun blocker(a: Arrow, arrows: List<Arrow>, size: Int): Arrow? {
  val occupied=arrows.filter { it.id!=a.id }.flatMap { other -> other.cells.map { it to other } }.toMap()
  return a.escapeCells(size).firstNotNullOfOrNull { occupied[it] }
 }
 fun validate(p: Puzzle): Boolean = p.arrows.isNotEmpty() && p.arrows.map { it.id }.distinct().size==p.arrows.size &&
  p.arrows.all { it.valid(p.size) } && p.arrows.flatMap { it.cells }.let { it.distinct().size==it.size } && solution(p)!=null
 fun hint(p: Puzzle): Int? = solution(p)?.firstOrNull()
 fun solution(p: Puzzle): List<Int>? {
  val remaining=p.arrows.toMutableList(); val order=mutableListOf<Int>()
  while(remaining.isNotEmpty()) { val a=remaining.firstOrNull { blocker(it,remaining,p.size)==null } ?: return null; order.add(a.id); remaining.remove(a) }
  return order
 }
 fun generate(level: Int): Puzzle {
  val size=(7+level/8).coerceAtMost(11); val rng=Random(level); val arrows=mutableListOf<Arrow>()
  val target=(10+level*2).coerceAtMost(32)
  val occupied=mutableSetOf<Vec>()
  // Deliberately introduce the new shape before random packing. Templates are
  // rotated, translated and validated with exactly the same gameplay rules.
  val template=when {
   level>=30 -> listOf(0 to 0,1 to 0,1 to 1,2 to 1,2 to 2,3 to 2)
   level>=15 -> listOf(0 to 0,1 to 0,2 to 0,2 to 1,1 to 1,0 to 1)
   level>=5 -> listOf(0 to 0,1 to 0,1 to 1)
   else -> listOf(0 to 0,1 to 0)
  }
  val rotation=rng.nextInt(4)
  val shape=template.map { (x,y) -> var p=Vec(x.toFloat(),y.toFloat());repeat(rotation) { p=Vec(-p.y,p.x) };p }
  val minX=shape.minOf { it.x };val minY=shape.minOf { it.y }
  val normalized=shape.map { Vec(it.x-minX,it.y-minY) }
  val dx=rng.nextInt(size-normalized.maxOf { it.x }.toInt()).toFloat()+0.5f
  val dy=rng.nextInt(size-normalized.maxOf { it.y }.toInt()).toFloat()+0.5f
  val first=Arrow(0,normalized.map { it+Vec(dx,dy) });check(first.valid(size));arrows.add(first);occupied.addAll(first.cells)
  repeat(6000) {
   if(arrows.size>=target) return@repeat
   var cell=Vec(rng.nextInt(size).toFloat()+0.5f,rng.nextInt(size).toFloat()+0.5f)
   val points=mutableListOf(cell); var dir=listOf(Vec(1f,0f),Vec(-1f,0f),Vec(0f,1f),Vec(0f,-1f)).random(rng)
   repeat(rng.nextInt(1,if(level<5) 3 else if(level<15) 4 else 7)) { step ->
    if(level>=5 && step>0 && rng.nextBoolean()) dir=if(rng.nextBoolean()) Vec(-dir.y,dir.x) else Vec(dir.y,-dir.x)
    val next=cell+dir
    if(next.x in 0.5f..(size-0.5f) && next.y in 0.5f..(size-0.5f) && next !in points) { points.add(next);cell=next }
   }
   if(points.size<2) return@repeat
   val candidate=Arrow(arrows.size,points)
   // Insertion in reverse removal order guarantees a solution. Existing arrows
   // may be blocked by this candidate; the candidate must escape past all of them.
   if(!candidate.valid(size) || blocker(candidate,arrows,size)!=null) return@repeat
   if(candidate.cells.any { it in occupied }) return@repeat
   arrows.add(candidate);occupied.addAll(candidate.cells)
  }
  return Puzzle(size,arrows).also { check(validate(it)) }
 }
}
