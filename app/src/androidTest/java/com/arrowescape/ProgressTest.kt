package com.arrowescape
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class ProgressTest {
 @Test fun diskSessionAndCompletionSurviveDatabaseReopen()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="persistence-verification.db"
  context.deleteDatabase(name)
  val session=SavedSession("level:1",1,"2,3",3,2,"3,2",true)
  var db=Room.databaseBuilder(context,ProgressDb::class.java,name).build()
  try {
   db.dao().complete(session,Profile(unlocked=2,solved=1,collectionSolved=1),null)
   db.close()
   db=Room.databaseBuilder(context,ProgressDb::class.java,name).build()
   assertEquals(session,db.dao().session(session.key))
   assertEquals(2,db.dao().profile()!!.unlocked)
   assertEquals(1,db.dao().awarded(session.key))
   assertEquals(session,PlayRules.restore(db.dao().session(session.key)!!,Engine.generate(1)).snapshot())
  } finally { db.close();context.deleteDatabase(name) }
 }
 @Test fun datastoreSettingsSurviveRepositoryRecreation()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val repository=SettingsRepository(context)
  val original=repository.settings.first()
  try {
   repository.toggle("sound")
   assertEquals(!original.sound,SettingsRepository(context).settings.first().sound)
  } finally { repository.toggle("sound") }
 }
 @Test fun roomRoundTripAndAtomicCompletion()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val db=Room.inMemoryDatabaseBuilder(context,ProgressDb::class.java).build()
  try {
   val session=SavedSession("daily:2026-10-14",123,"2,3",2,1,"3,2",true)
   db.dao().complete(session,Profile(unlocked=4,solved=1),"2026-10-14")
   assertEquals(session,db.dao().session(session.key));assertEquals(4,db.dao().profile()!!.unlocked)
   assertEquals(1,db.dao().awarded(session.key));assertTrue(db.dao().daily().single().completed)
  } finally { db.close() }
 }
}
