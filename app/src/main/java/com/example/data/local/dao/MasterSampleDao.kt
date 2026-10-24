package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MasterSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterSampleDao {
    @Query("SELECT * FROM master_sample ORDER BY statusArea ASC")
    fun getAllSamplesFlow(): Flow<List<MasterSampleEntity>>

    @Query("SELECT * FROM master_sample ORDER BY statusArea ASC")
    suspend fun getAllSamples(): List<MasterSampleEntity>

    @Query("SELECT * FROM master_sample WHERE statusArea = :statusArea LIMIT 1")
    suspend fun getSampleByStatusArea(statusArea: String): MasterSampleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MasterSampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<MasterSampleEntity>)

    @Update
    suspend fun update(entity: MasterSampleEntity)

    @Query("UPDATE master_sample SET jumlahSample = :jumlahSample, updatedAt = :updatedAt WHERE statusArea = :statusArea")
    suspend fun updateJumlahSample(statusArea: String, jumlahSample: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM master_sample WHERE statusArea = :statusArea")
    suspend fun delete(statusArea: String)

    @Query("DELETE FROM master_sample")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM master_sample")
    suspend fun getCount(): Int
}
