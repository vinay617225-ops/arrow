package com.arrowescape

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*

// Original synthesized effects; no copied audio assets.
class GameAudio(private val context: Context) {
 private var tone: ToneGenerator?=null
 private val handler=Handler(Looper.getMainLooper())
 private var music=false
 private var settings=Settings()
 private val melody=intArrayOf(ToneGenerator.TONE_DTMF_1,ToneGenerator.TONE_DTMF_3,ToneGenerator.TONE_DTMF_5,ToneGenerator.TONE_DTMF_3)
 private var note=0
 private val tick=object: Runnable { override fun run() { if(music) { player()?.startTone(melody[note++%melody.size],150);handler.postDelayed(this,1600) } } }
 private fun player(): ToneGenerator? { if(tone==null) tone=runCatching { ToneGenerator(AudioManager.STREAM_MUSIC,25) }.getOrNull();return tone }
 fun update(s: Settings,active: Boolean) { settings=s;val enabled=s.music && active;if(!active) tone?.stopTone();if(enabled!=music) { music=enabled;handler.removeCallbacks(tick);if(enabled) handler.post(tick) } }
 fun effect(kind: String) {
  if(settings.sound) player()?.startTone(when(kind) { "collision"->ToneGenerator.TONE_PROP_NACK;"complete"->ToneGenerator.TONE_PROP_ACK;"over"->ToneGenerator.TONE_SUP_ERROR;"hint"->ToneGenerator.TONE_PROP_PROMPT;else->ToneGenerator.TONE_PROP_BEEP },if(kind=="complete") 300 else 100)
  if(settings.vibration && kind=="collision") {
   val vibrator=if(Build.VERSION.SDK_INT>=31) context.getSystemService(VibratorManager::class.java).defaultVibrator else context.getSystemService(Vibrator::class.java)
   if(vibrator?.hasVibrator()==true) vibrator.vibrate(VibrationEffect.createOneShot(65,VibrationEffect.DEFAULT_AMPLITUDE))
  }
 }
 fun close() { music=false;handler.removeCallbacks(tick);tone?.release();tone=null }
}
