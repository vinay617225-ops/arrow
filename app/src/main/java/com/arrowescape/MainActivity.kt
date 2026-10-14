package com.arrowescape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.*

class MainActivity: ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState);setContent { ArrowApp() } }
}
private val Ink=Color(0xFF203D42)
private val Paper=Color(0xFFF5F4EC)
private val Teal=Color(0xFF317C73)

@Composable
fun ArrowApp(vm: GameViewModel=viewModel()) {
 val game by vm.game.collectAsStateWithLifecycle();val profile by vm.profile.collectAsStateWithLifecycle()
 val ready by vm.ready.collectAsStateWithLifecycle();val settings by vm.settings.collectAsStateWithLifecycle();val daily by vm.daily.collectAsStateWithLifecycle()
 var screen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("home") }
 var dailySelection by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
 var loading by remember { mutableStateOf(false) }
 var tutorialStep by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
 val context=LocalContext.current;val audio=remember { GameAudio(context) }
 val owner=LocalLifecycleOwner.current
 var active by remember { mutableStateOf(true) }
 DisposableEffect(owner) {
  val observer=LifecycleEventObserver { _,event ->
   if(event==Lifecycle.Event.ON_STOP) { active=false;vm.pause(true);audio.update(settings,false) }
   if(event==Lifecycle.Event.ON_START) active=true
  }
  owner.lifecycle.addObserver(observer)
  onDispose { owner.lifecycle.removeObserver(observer);audio.close() }
 }
 LaunchedEffect(settings,active,game.paused) { audio.update(settings,active && !game.paused) }
 // Rehydrate the Room snapshot when a process is recreated on the game screen.
 LaunchedEffect(ready) { if(ready && screen=="game" && game.key.isEmpty()) { loading=true;vm.load(profile.current,dailySelection?.let { LocalDate.parse(it) }) { loading=false } } }
 LaunchedEffect(game.complete) { if(game.complete) audio.effect("complete") }
 LaunchedEffect(game.hearts) { if(game.hearts==0) audio.effect("over") }
 fun open(level: Int,date: LocalDate?=null) { if(!loading) { loading=true;dailySelection=date?.toString();vm.load(level,date) { screen="game";loading=false } } }
 BackHandler(screen!="home") { if(screen=="game") vm.pause(true) else screen="home" }
 MaterialTheme(colorScheme=lightColorScheme(primary=Teal,onPrimary=Color.White,background=Paper,surface=Paper,onSurface=Ink,secondary=Color(0xFFE4AB65)),typography=Typography()) {
  Surface(Modifier.fillMaxSize()) {
   if(!ready) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("↗",style=MaterialTheme.typography.displayLarge,color=Teal);Text("ARROW ESCAPE");CircularProgressIndicator(Modifier.padding(24.dp)) } }
   else AnimatedContent(targetState=screen,label="screen",modifier=Modifier.fillMaxSize()) { route ->
    when(route) {
     "home" -> MenuPage("A little space.\nA clear mind.","ARROW ESCAPE") {
      Text("Untangle connected paths, one arrow at a time.",style=MaterialTheme.typography.bodyLarge)
      Spacer(Modifier.height(24.dp))
      Button(onClick={ open(profile.current) },enabled=!loading,modifier=Modifier.fillMaxWidth().height(58.dp)) { Text("Continue · Level ${profile.current}") }
      MenuButton("Level collection",{ screen="levels" })
      MenuButton("Daily challenge · ${vm.streak()} day streak",{ screen="daily" })
      MenuButton("How to play",{ tutorialStep=0;screen="tutorial" })
      MenuButton("Achievements",{ screen="achievements" })
      MenuButton("Settings",{ screen="settings" })
      Text("100 thoughtfully generated puzzles",style=MaterialTheme.typography.labelMedium,color=Teal)
     }
     "levels" -> Column(Modifier.systemBarsPadding().padding(20.dp)) {
      PageHeader("Your collection",{ screen="home" })
      Text("A new path with every level.",Modifier.padding(bottom=20.dp))
      LazyVerticalGrid(columns=GridCells.Adaptive(72.dp),verticalArrangement=Arrangement.spacedBy(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
       items(100) { i -> val level=i+1
        FilledTonalButton(enabled=level<=profile.unlocked && !loading,onClick={ open(level) },modifier=Modifier.height(62.dp)) { Text(if(level<=profile.unlocked) "$level" else "·") }
       }
      }
     }
     "settings" -> MenuPage("Make it yours.","SETTINGS",{ screen="home" }) {
      ToggleRow("Sound effects",settings.sound) { vm.toggle("sound") }
      ToggleRow("Ambient melody",settings.music) { vm.toggle("music") }
      ToggleRow("Vibration",settings.vibration) { vm.toggle("vibration") }
      Text("Your settings and progress are saved on this device.",Modifier.padding(top=24.dp))
     }
     "daily" -> MenuPage("A fresh path daily.","DAILY CHALLENGES",{ screen="home" }) {
      val today=LocalDate.now();val first=today.withDayOfMonth(1)
      Text(today.format(DateTimeFormatter.ofPattern("MMMM yyyy")),style=MaterialTheme.typography.headlineSmall)
      Text("Current streak: ${vm.streak()} days · ${daily.count { it.completed }} completed",Modifier.padding(vertical=12.dp))
      for(week in 0..4) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
       for(day in 1..7) { val index=week*7+day; val date=first.plusDays((index-1).toLong())
        Box(Modifier.weight(1f)) { if(index<=today.lengthOfMonth()) TextButton(enabled=date<=today && !loading,onClick={ open(1,date) },contentPadding=PaddingValues(0.dp)) { Text(if(daily.any { it.date==date.toString() && it.completed }) "✓" else "$index") } }
       }
      }
      Button(onClick={ open(1,today) },enabled=!loading,modifier=Modifier.fillMaxWidth()) { Text("Play today's puzzle") }
      Text("Choose any past day this month to catch up.",style=MaterialTheme.typography.bodySmall)
     }
     "achievements" -> MenuPage("Small victories.","ACHIEVEMENTS",{ screen="home" }) {
      Achievement("First flight","Complete your first puzzle",profile.solved>=1)
      Achievement("Pathfinder","Complete 10 puzzles",profile.solved>=10)
      Achievement("Escape artist","Unlock level 50",profile.unlocked>=50)
      Achievement("Centurion","Complete the level collection",profile.unlocked==100 && profile.collectionSolved>=100)
      Achievement("Daily ritual","Complete 7 daily challenges",daily.count { it.completed }>=7)
      Text("${profile.solved} puzzle completions · ${profile.hints} hints used")
     }
     "tutorial" -> MenuPage("Follow the arrow.","HOW TO PLAY",{ screen="home" }) {
      val demo=remember { Puzzle(5,listOf(Arrow(0,listOf(Vec(0.5f,2.5f),Vec(1.5f,2.5f),Vec(2.5f,2.5f))),Arrow(1,listOf(Vec(3.5f,3.5f),Vec(3.5f,2.5f),Vec(3.5f,1.5f))))) }
      val removed=when(tutorialStep) { 0->emptySet();1->setOf(1);else->setOf(0,1) }
      Text(when(tutorialStep) { 0->"The horizontal arrow is blocked. Tap the vertical arrow first.";1->"Now tap the horizontal arrow. The way is clear.";else->"All clear! Curved bodies follow their path; the head continues straight." })
      GameBoard(PlayState(puzzle=demo,removed=removed,hint=if(tutorialStep==0) 1 else 0),onTap={ id -> if(id==(if(tutorialStep==0) 1 else 0)) { tutorialStep++;audio.effect("escape") } else audio.effect("collision") },onFinish={},onWarningDone={},fit=0,modifier=Modifier.fillMaxWidth().height(280.dp))
      if(tutorialStep>=2) Button(onClick={ open(1) }) { Text("Start playing") }
      Text("Five hearts per puzzle. Use Hint to find a safe move. Undo restores your last escaped arrow. Pinch or double-tap to zoom, and drag to pan.")
     }
     "game" -> {
      var fit by remember { mutableIntStateOf(0) }
      Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal=16.dp,vertical=8.dp)) {
       Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        TextButton(onClick={ vm.pause(true) }) { Text("Ⅱ Pause") }
        Text(if(game.key.startsWith("daily:")) "DAILY" else "LEVEL ${game.seed}",fontWeight=FontWeight.Bold)
        TextButton(onClick={ fit++ }) { Text("Fit") }
       }
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
        AnimatedContent(game.hearts,label="heart loss") { hearts -> Text("♥ ".repeat(hearts)+"♡ ".repeat(5-hearts),color=Color(0xFFBF6059),modifier=Modifier.testTag("hearts")) }
        Text("${game.removed.size}/${game.puzzle.arrows.size}",Modifier.testTag("progress"))
       }
       GameBoard(game,onTap={ id -> audio.effect("tap");if(vm.attempt(id)) audio.effect("escape") else if(vm.game.value.warning==id) audio.effect("collision") },onFinish=vm::finish,onWarningDone=vm::clearWarning,fit=fit,modifier=Modifier.weight(1f).fillMaxWidth())
       Text(if(game.hint!=null) "Follow the glowing path." else if(game.warning!=null) "That path is blocked." else "Tap a path with a clear exit.",Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.bodyMedium)
       Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick=vm::undo,enabled=game.history.isNotEmpty() && game.moving==null && !game.complete && game.hearts>0,modifier=Modifier.weight(1f).testTag("undo"),contentPadding=PaddingValues(6.dp)) { Text("Undo") }
        OutlinedButton(onClick={ vm.hint();audio.effect("hint") },enabled=game.moving==null && game.hearts>0 && !game.complete,modifier=Modifier.weight(1f).testTag("hint"),contentPadding=PaddingValues(6.dp)) { Text("Hint") }
        OutlinedButton(onClick=vm::restart,enabled=game.moving==null,modifier=Modifier.weight(1f).testTag("restart"),contentPadding=PaddingValues(6.dp)) { Text("Restart") }
       }
      }
      if(game.paused) AlertDialog(onDismissRequest={ vm.pause(false) },title={ Text("Take a breath.") },text={ Text("Your puzzle is safely saved.") },confirmButton={ TextButton(onClick={ vm.pause(false) }) { Text("Resume") } },dismissButton={ TextButton(onClick={ vm.pause(false);screen="home" }) { Text("Home") } })
      if(game.complete && !game.paused) AlertDialog(onDismissRequest={},title={ Text("✦  All clear!") },text={ Column { Celebration();Text("Every path found its way out. ${game.hearts} hearts remaining.") } },confirmButton={ TextButton(onClick={ if(game.key.startsWith("daily:") || game.seed>=100) screen="home" else open(game.seed+1) }) { Text(if(game.key.startsWith("daily:") || game.seed>=100) "Home" else "Next level") } },dismissButton={ TextButton(onClick={ screen="levels" }) { Text("Collection") } })
      if(game.hearts==0 && !game.paused) AlertDialog(onDismissRequest={},title={ Text("Let's try again.") },text={ Text("No hearts left. A fresh start is a new perspective.") },confirmButton={ TextButton(onClick=vm::restart) { Text("Restart puzzle") } },dismissButton={ TextButton(onClick={ screen="home" }) { Text("Home") } })
     }
    }
   }
   if(loading) Box(Modifier.fillMaxSize().background(Paper.copy(alpha=0.8f)),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
  }
 }
}

@Composable private fun MenuPage(title: String,label: String,back: (() -> Unit)?=null,content: @Composable ColumnScope.()->Unit) {
 Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  if(back!=null) TextButton(onClick=back) { Text("← Home") }
  Text(label,color=Teal,style=MaterialTheme.typography.labelLarge)
  Text(title,style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Medium)
  Spacer(Modifier.height(8.dp));content()
 }
}
@Composable private fun MenuButton(text: String,onClick: ()->Unit) { OutlinedButton(onClick=onClick,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp)) { Text(text) } }
@Composable private fun PageHeader(title: String,back: ()->Unit) { TextButton(onClick=back) { Text("← Home") };Text(title,style=MaterialTheme.typography.headlineLarge) }
@Composable private fun ToggleRow(label: String,checked: Boolean,onClick: ()->Unit) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text(label);Switch(checked=checked,onCheckedChange={ onClick() }) } }
@Composable private fun Achievement(title: String,description: String,done: Boolean) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("${if(done) "✓" else "○"} $title",fontWeight=FontWeight.Bold);Text(description,style=MaterialTheme.typography.bodyMedium) } } }
@Composable private fun Celebration() {
 val phase=remember { Animatable(0f) };LaunchedEffect(Unit) { phase.animateTo(1f,tween(1400)) }
 Canvas(Modifier.fillMaxWidth().height(80.dp)) { for(i in 0..25) { val x=(i*37%100)/100f*size.width;val y=size.height*phase.value;drawCircle(if(i%2==0) Teal else Color(0xFFE4AB65),4f,Offset(x,(y+i*7)%size.height),alpha=1-phase.value*0.5f) } }
}

@Composable
fun GameBoard(state: PlayState,onTap: (Int)->Unit,onFinish: (Int)->Unit,onWarningDone: ()->Unit,fit: Int,modifier: Modifier=Modifier) {
 val travel=remember(state.key,state.seed) { Animatable(0f) };val bounce=remember { Animatable(0f) }
 var zoomTarget by remember(state.key) { mutableFloatStateOf(1f) };var pan by remember(state.key) { mutableStateOf(Offset.Zero) }
 val zoom by animateFloatAsState(zoomTarget,tween(120),label="camera")
 val drawnPan by animateOffsetAsState(pan,tween(100),label="pan")
 LaunchedEffect(fit,state.key) { zoomTarget=1f;pan=Offset.Zero }
 val latestFinish by rememberUpdatedState(onFinish);val latestWarningDone by rememberUpdatedState(onWarningDone);val latestTap by rememberUpdatedState(onTap)
 val current by rememberUpdatedState(state);val currentZoom by rememberUpdatedState(zoom)
 val currentPan by rememberUpdatedState(drawnPan)
 var animatedId by remember(state.key,state.seed) { mutableStateOf<Int?>(null) }
 LaunchedEffect(state.moving,state.paused) {
  val id=state.moving
  if(id==null) { animatedId=null;travel.snapTo(0f);return@LaunchedEffect }
  if(state.paused) return@LaunchedEffect
  val arrow=state.arrows.firstOrNull { it.id==id } ?: return@LaunchedEffect
  if(animatedId!=id) { travel.snapTo(0f);animatedId=id }
  val target=state.puzzle.size*2f+arrow.length
  val duration=((850+arrow.cells.size*30)*(1f-travel.value/target)).toInt().coerceAtLeast(1)
  travel.animateTo(target,tween(duration,easing=LinearEasing));latestFinish(id)
 }
 LaunchedEffect(state.warning,state.hearts) { if(state.warning!=null) { bounce.snapTo(0f);bounce.animateTo(0.15f,tween(100));bounce.animateTo(0f,tween(180));latestWarningDone() } }
 val pulse=rememberInfiniteTransition(label="hint").animateFloat(0.35f,0.85f,infiniteRepeatable(tween(700),RepeatMode.Reverse),label="hint glow")
 Canvas(modifier.testTag("board").semantics {
  contentDescription="Puzzle board with ${state.arrows.size} connected arrows"
  customActions=state.arrows.map { a -> CustomAccessibilityAction("Move arrow ${a.id+1}, ${a.cells.size} cells") { onTap(a.id);true } }
 }.pointerInput(state.key) {
  detectTransformGestures { centroid,change,scale,_ ->
   val old=zoomTarget;val next=(old*scale).coerceIn(1f,4f)
   val center=Offset(size.width/2f,size.height/2f)
   pan=(centroid-center)-(centroid-center-pan)*(next/old)+change
   val limit=maxOf(size.width,size.height)*next/2f;pan=Offset(pan.x.coerceIn(-limit,limit),pan.y.coerceIn(-limit,limit));zoomTarget=next
  }
 }.pointerInput(state.key) {
  detectTapGestures(onDoubleTap={ tap ->
   val next=if(zoomTarget>1.5f) 1f else 2.5f
   val center=Offset(size.width/2f,size.height/2f)
   pan=if(next==1f) Offset.Zero else (tap-center)-(tap-center-pan)*(next/zoomTarget);zoomTarget=next
  },onTap={ tap ->
   val s=current;if(s.paused || s.moving!=null || s.complete || s.hearts==0) return@detectTapGestures
   val transform=BoardTransform(size.width.toFloat(),size.height.toFloat(),s.puzzle.size,currentZoom,Vec(currentPan.x,currentPan.y))
   val p=transform.toBoard(Vec(tap.x,tap.y))
   s.arrows.filter { it.hit(p,maxOf(0.22f,16.dp.toPx()/transform.scale)) }.minByOrNull { a -> a.line.zipWithNext().minOf { (u,v)->distance(p,u,v) } }?.let { latestTap(it.id) }
  })
 }) {
  val transform=BoardTransform(size.width,size.height,state.puzzle.size,zoom,Vec(drawnPan.x,drawnPan.y))
  val scale=transform.scale
  fun pos(p: Vec)=transform.toScreen(p).let { Offset(it.x,it.y) }
  clipRect {
   for(x in 0 until state.puzzle.size) for(y in 0 until state.puzzle.size) drawCircle(Color(0xFFE0E3D8),maxOf(1f,scale*0.03f),pos(Vec(x+0.5f,y+0.5f)))
   for(a in state.arrows) {
    val line=if(state.moving==a.id) a.body(travel.value) else if(state.warning==a.id) a.body(bounce.value) else a.line
    val path=Path().apply { val start=pos(line.first());moveTo(start.x,start.y);line.drop(1).forEach { val p=pos(it);lineTo(p.x,p.y) } }
    if(state.hint==a.id) drawPath(path,Teal.copy(alpha=pulse.value),style=Stroke(scale*0.38f,cap=StrokeCap.Round,join=StrokeJoin.Round))
    val color=if(state.warning==a.id) Color(0xFFCE554D) else Ink
    drawPath(path,color,style=Stroke(Engine.radius*2*scale,cap=StrokeCap.Round,join=StrokeJoin.Round))
    val head=line.last();val d=a.direction;val n=Vec(-d.y,d.x)
    val tip=pos(head+d*0.12f);val left=pos(head-d*0.20f+n*0.16f);val right=pos(head-d*0.20f-n*0.16f)
    drawPath(Path().apply { moveTo(tip.x,tip.y);lineTo(left.x,left.y);lineTo(right.x,right.y);close() },color)
   }
  }
 }
}
