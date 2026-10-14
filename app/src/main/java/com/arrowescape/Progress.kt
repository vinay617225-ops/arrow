package com.arrowescape

import android.content.Context
import androidx.room.*
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*

@Entity(tableName="sessions")
data class SavedSession(@PrimaryKey val key: String, val seed: Int, val removed: String="", val hearts: Int=5, val hints: Int=0, val history: String="", val completed: Boolean=false)
@Entity(tableName="profile")
data class Profile(@PrimaryKey val id: Int=1, val current: Int=1, val unlocked: Int=1, val solved: Int=0, val collectionSolved: Int=0, val hints: Int=0)
@Entity(tableName="daily")
data class DailyRecord(@PrimaryKey val date: String, val completed: Boolean)
@Entity(tableName="completions")
data class Completion(@PrimaryKey val key: String)
@Dao
interface ProgressDao {
 @Query("SELECT * FROM sessions WHERE `key`=:key") suspend fun session(key: String): SavedSession?
 @Query("SELECT * FROM profile WHERE id=1") suspend fun profile(): Profile?
 @Query("SELECT COUNT(*) FROM completions WHERE `key`=:key") suspend fun awarded(key: String): Int
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun save(record: Completion)
 @Query("SELECT * FROM daily ORDER BY date DESC") suspend fun daily(): List<DailyRecord>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun save(session: SavedSession)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun save(profile: Profile)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun save(record: DailyRecord)
 @Transaction suspend fun complete(session: SavedSession, profile: Profile, date: String?) {
  save(session);save(profile);save(Completion(session.key));if(date!=null) save(DailyRecord(date,true))
 }
}
@Database(entities=[SavedSession::class,Profile::class,DailyRecord::class,Completion::class],version=1,exportSchema=false)
abstract class ProgressDb : RoomDatabase() {
 abstract fun dao(): ProgressDao
 companion object {
  @Volatile private var instance: ProgressDb?=null
  fun get(context: Context): ProgressDb = instance ?: synchronized(this) {
   instance ?: Room.databaseBuilder(context.applicationContext,ProgressDb::class.java,"arrow-progress.db").build().also { instance=it }
  }
 }
}
val Context.settingsStore by preferencesDataStore("settings")
data class Settings(val sound: Boolean=true,val music: Boolean=false,val vibration: Boolean=true)
class SettingsRepository(private val context: Context) {
 val settings: Flow<Settings> = context.settingsStore.data.map { p -> Settings(p[booleanPreferencesKey("sound")]?:true,p[booleanPreferencesKey("music")]?:false,p[booleanPreferencesKey("vibration")]?:true) }
 suspend fun toggle(key: String) { context.settingsStore.edit { p -> val k=booleanPreferencesKey(key);p[k]=!(p[k] ?: (key!="music")) } }
}
