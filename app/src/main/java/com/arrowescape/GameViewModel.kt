package com.arrowescape

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

data class PlayState(val key: String="",val seed: Int=1,val puzzle: Puzzle=Puzzle(7,emptyList()),val removed: Set<Int> = emptySet(),val hearts: Int=5,val hints: Int=0,val history: List<Int> = emptyList(),val moving: Int?=null,val warning: Int?=null,val hint: Int?=null,val complete: Boolean=false,val paused: Boolean=false) {
 val arrows get()=puzzle.arrows.filter { it.id !in removed }
 fun snapshot()=SavedSession(key,seed,removed.joinToString(","),hearts,hints,history.joinToString(","),complete)
}
object PlayRules {
 fun attempt(s: PlayState,id: Int): PlayState {
  if(s.paused || s.complete || s.hearts<=0 || s.moving!=null) return s
  val a=s.arrows.firstOrNull { it.id==id } ?: return s
  return if(Engine.blocker(a,s.arrows,s.puzzle.size)!=null) s.copy(hearts=s.hearts-1,warning=id,hint=null)
   else s.copy(moving=id,warning=null,hint=null)
 }

 fun undo(s: PlayState): PlayState = if(s.history.isEmpty()) s else s.copy(removed=s.removed-s.history.last(),history=s.history.dropLast(1),complete=false,hint=null)
 fun restart(s: PlayState)=s.copy(removed=emptySet(),history=emptyList(),hearts=5,hints=0,moving=null,warning=null,hint=null,complete=false,paused=false)
 fun finish(s: PlayState,id: Int): PlayState { val removed=s.removed+id;return s.copy(removed=removed,history=s.history+id,moving=null,complete=removed.size==s.puzzle.arrows.size) }
 fun restore(saved: SavedSession,puzzle: Puzzle)=PlayState(saved.key,saved.seed,puzzle,saved.removed.split(",").mapNotNull { it.toIntOrNull() }.toSet(),saved.hearts,saved.hints,saved.history.split(",").mapNotNull { it.toIntOrNull() },complete=saved.completed)
}
class GameViewModel(app: Application): AndroidViewModel(app) {
 private val dao=ProgressDb.get(app).dao();private val repo=SettingsRepository(app);private val lock=Mutex()
 private val _game=MutableStateFlow(PlayState());val game=_game.asStateFlow()
 private val _profile=MutableStateFlow(Profile());val profile=_profile.asStateFlow()
 private val _daily=MutableStateFlow<List<DailyRecord>>(emptyList());val daily=_daily.asStateFlow()
 private val _ready=MutableStateFlow(false);val ready=_ready.asStateFlow()
 val settings=repo.settings.stateIn(viewModelScope,SharingStarted.Eagerly,Settings())
 init { viewModelScope.launch { _profile.value=dao.profile()?:Profile();_daily.value=dao.daily();_ready.value=true } }
 fun load(level: Int,date: LocalDate?=null,onLoaded: ()->Unit) {
  viewModelScope.launch { lock.withLock {
   val key=date?.let { "daily:$it" }?:"level:$level"; val seed=date?.toEpochDay()?.toInt()?.let { 10000+it }?:level
   val puzzle=withContext(Dispatchers.Default) { Engine.generate(seed) }
   _game.value=dao.session(key)?.let { PlayRules.restore(it,puzzle) }?:PlayState(key,seed,puzzle)
   if(date==null) { _profile.value=_profile.value.copy(current=level);dao.save(_profile.value) }
   dao.save(_game.value.snapshot());onLoaded()
  } }
 }
 fun attempt(id: Int): Boolean {
  val s=_game.value;val next=PlayRules.attempt(s,id);_game.value=next
  if(next.hearts!=s.hearts) persist()
  return next.moving==id && s.moving==null
 }
 fun finish(id: Int) {
  viewModelScope.launch { lock.withLock {
   val s=_game.value;if(s.moving!=id) return@withLock
   val next=PlayRules.finish(s,id);_game.value=next
   val previous=dao.awarded(s.key)
   if(next.complete && previous==0) {
    val dailyDate=s.key.takeIf { it.startsWith("daily:") }?.removePrefix("daily:")
    val p=_profile.value.copy(solved=_profile.value.solved+1,collectionSolved=_profile.value.collectionSolved+(if(dailyDate==null) 1 else 0),unlocked=if(dailyDate==null) maxOf(_profile.value.unlocked,(s.seed+1).coerceAtMost(100)) else _profile.value.unlocked)
    _profile.value=p;dao.complete(next.snapshot(),p,dailyDate);_daily.value=dao.daily()
   } else dao.save(next.snapshot())
  } }
 }
 fun hint() {
  val s=_game.value;if(s.moving!=null || s.hearts<=0 || s.complete) return
  val id=Engine.hint(Puzzle(s.puzzle.size,s.arrows))?:return
  _game.value=s.copy(hint=id,warning=null,hints=s.hints+1);_profile.value=_profile.value.copy(hints=_profile.value.hints+1)
  persist();viewModelScope.launch { dao.save(_profile.value) }
 }
 fun undo() { val s=_game.value;if(s.moving==null && s.hearts>0) { _game.value=PlayRules.undo(s);persist() } }
 fun restart() { if(_game.value.moving==null) { _game.value=PlayRules.restart(_game.value);persist() } }
 fun pause(value: Boolean) { _game.value=_game.value.copy(paused=value) }
 fun clearWarning() { _game.value=_game.value.copy(warning=null) }
 fun toggle(key: String) { viewModelScope.launch { repo.toggle(key) } }
 private fun persist() { val snapshot=_game.value.snapshot();viewModelScope.launch { lock.withLock { dao.save(snapshot) } } }
 fun streak(today: LocalDate=LocalDate.now()): Int {
  val dates=_daily.value.filter { it.completed }.map { it.date }.toSet();var d=if(today.toString() in dates) today else today.minusDays(1);var count=0
  while(d.toString() in dates) { count++;d=d.minusDays(1) };return count
 }
}
