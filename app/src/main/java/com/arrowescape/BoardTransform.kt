package com.arrowescape

/** The displayed camera transform, shared by Canvas and pointer hit testing. */
data class BoardTransform(
 val width: Float,
 val height: Float,
 val boardSize: Int,
 val zoom: Float = 1f,
 val pan: Vec = Vec(0f,0f),
) {
 val scale = (minOf(width,height)/(boardSize+1f)*zoom).coerceAtLeast(0.0001f)
 private val origin = Vec(width/2f,height/2f)-Vec(boardSize*scale/2f,boardSize*scale/2f)+pan
 fun toScreen(point: Vec) = origin+point*scale
 fun toBoard(point: Vec) = (point-origin)*(1f/scale)
}
