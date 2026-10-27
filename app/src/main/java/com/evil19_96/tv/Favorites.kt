package com.evil19_96.tv

import android.content.Context
import androidx.room.*

@Entity(tableName = "favorites", primaryKeys = ["url"])
data class FavoriteEntity(
    val url: String,
    val name: String,
    val groupName: String
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY name COLLATE NOCASE")
    suspend fun all(): List<FavoriteEntity>
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE url = :url)")
    suspend fun contains(url: String): Boolean
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(item: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE url = :url")
    suspend fun remove(url: String)
}

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class FavoriteDatabase : RoomDatabase() {
    abstract fun favorites(): FavoriteDao
    companion object {
        @Volatile private var INSTANCE: FavoriteDatabase? = null
        fun get(context: Context): FavoriteDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext,
                FavoriteDatabase::class.java, "evil19_favorites.db").build().also { INSTANCE = it }
        }
    }
}
