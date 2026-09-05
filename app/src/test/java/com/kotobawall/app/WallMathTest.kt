package com.kotobawall.app
import org.junit.Assert.*
import org.junit.Test

class WallMathTest {
 @Test fun nextWraps() { assertEquals(0,WallMath.nextIndex(49,50)) }
 @Test fun nextAdvances() { assertEquals(1,WallMath.nextIndex(0,50)) }
 @Test(expected=IllegalArgumentException::class) fun emptyRejected() { WallMath.nextIndex(0,0) }
 @Test fun squareCropToPortrait() {
  val r=WallMath.crop(1000,1000,500,1000,0.5f,0.5f)
  assertArrayEquals(floatArrayOf(250f,0f,500f,1000f),r,0.01f)
 }
 @Test fun cropClamps() {
  val r=WallMath.crop(1000,1000,500,1000,9f,-1f)
  assertEquals(500f,r[0],0.01f); assertEquals(0f,r[1],0.01f)
 }
 @Test fun panelStaysInside() {
  assertEquals(20f,WallMath.top(1000,200f,0f,20f),0.01f)
  assertEquals(780f,WallMath.top(1000,200f,1f,20f),0.01f)
 }
}
