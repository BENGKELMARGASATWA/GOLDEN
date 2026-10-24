package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.MasterBobotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterBobotDao {
    @Query("SELECT * FROM master_bobot ORDER BY rowid ASC")
    fun getAllBobotFlow(): Flow<List<MasterBobotEntity>>

    @Query("SELECT * FROM master_bobot ORDER BY rowid ASC")
    suspend fun getAllBobot(): List<MasterBobotEntity>

    @Query("SELECT * FROM master_bobot WHERE itemId = :itemId LIMIT 1")
    suspend fun getBobotByItemId(itemId: String): MasterBobotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MasterBobotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<MasterBobotEntity>)

    @Query("UPDATE master_bobot SET bobot = :bobot, updatedAt = :updatedAt WHERE itemId = :itemId")
    suspend fun updateBobot(itemId: String, bobot: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM master_bobot WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: String): Int

    @Query("DELETE FROM master_bobot")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM master_bobot")
    suspend fun getCount(): Int
}
