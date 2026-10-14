package com.arrowescape

import org.junit.Assert.*
import org.junit.Test

class BoardTransformTest {
 @Test fun hitCoordinatesMatchDisplayedCameraAtEveryZoom() {
  for(zoom in listOf(1f,1.5f,2.5f,4f)) for(pan in listOf(Vec(0f,0f),Vec(125f,-80f))) {
   val transform=BoardTransform(1080f,1600f,11,zoom,pan)
   val arrow=Arrow(0,listOf(Vec(0.5f,0.5f),Vec(1.5f,0.5f),Vec(1.5f,1.5f)))
   for(point in arrow.line) {
    val restored=transform.toBoard(transform.toScreen(point))
    assertEquals(point.x,restored.x,0.00001f)
    assertEquals(point.y,restored.y,0.00001f)
    assertTrue(arrow.hit(restored))
   }
  }
 }
}
