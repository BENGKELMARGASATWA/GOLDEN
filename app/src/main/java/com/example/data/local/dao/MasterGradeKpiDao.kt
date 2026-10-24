package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.MasterGradeKpiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterGradeKpiDao {
    @Query("SELECT * FROM master_grade_kpi ORDER BY minScore ASC")
    fun getAllGradeKpiFlow(): Flow<List<MasterGradeKpiEntity>>

    @Query("SELECT * FROM master_grade_kpi ORDER BY minScore ASC")
    suspend fun getAllGradeKpi(): List<MasterGradeKpiEntity>

    @Query("SELECT * FROM master_grade_kpi WHERE kategori = :kategori LIMIT 1")
    suspend fun getByKategori(kategori: String): MasterGradeKpiEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MasterGradeKpiEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<MasterGradeKpiEntity>)

    @Query("UPDATE master_grade_kpi SET minScore = :minScore, maxScore = :maxScore, keterangan = :keterangan, updatedAt = :updatedAt WHERE kategori = :kategori")
    suspend fun updateGradeKpi(kategori: String, minScore: Double, maxScore: Double, keterangan: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM master_grade_kpi WHERE kategori = :kategori")
    suspend fun deleteByKategori(kategori: String): Int

    @Query("DELETE FROM master_grade_kpi")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM master_grade_kpi")
    suspend fun getCount(): Int
}
