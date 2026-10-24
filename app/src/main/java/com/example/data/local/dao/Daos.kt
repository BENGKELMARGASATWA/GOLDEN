package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DataAuditEntity
import com.example.data.local.entity.MasterKendaraanEntity
import com.example.data.local.entity.MasterPeriodeEntity
import com.example.data.local.entity.MasterPoolEntity
import com.example.data.local.entity.PengaturanNamaPhotoEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY namaPetugas ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE LOWER(TRIM(username)) = LOWER(TRIM(:username)) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(TRIM(username)) = LOWER(TRIM(:query)) OR LOWER(TRIM(nip)) = LOWER(TRIM(:query)) OR LOWER(TRIM(namaPetugas)) = LOWER(TRIM(:query)) LIMIT 1")
    suspend fun findUserForLogin(query: String): UserEntity?

    @Query("SELECT * FROM users WHERE nip = :nip LIMIT 1")
    suspend fun getUserByNip(nip: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE nip = :nip")
    suspend fun deleteUserByNip(nip: String)

    @Query("DELETE FROM users WHERE nip IN (:nips) OR LOWER(TRIM(username)) IN (:usernames)")
    suspend fun deleteUsersByNipOrUsername(nips: List<String>, usernames: List<String>): Int

    @Query("SELECT * FROM users ORDER BY namaPetugas ASC")
    suspend fun getAllUsersList(): List<UserEntity>

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers(): Int

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

@Dao
interface MasterPoolDao {
    @Query("SELECT * FROM master_pool ORDER BY kodePool ASC")
    fun getAllPools(): Flow<List<MasterPoolEntity>>

    @Query("SELECT * FROM master_pool ORDER BY kodePool ASC")
    suspend fun getAllPoolsList(): List<MasterPoolEntity>

    @Query("SELECT * FROM master_pool WHERE id = :id LIMIT 1")
    suspend fun getPoolById(id: Int): MasterPoolEntity?

    @Query("SELECT * FROM master_pool WHERE kodePool = :kodePool LIMIT 1")
    suspend fun getPoolByKode(kodePool: String): MasterPoolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPool(pool: MasterPoolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPools(pools: List<MasterPoolEntity>)

    @Update
    suspend fun updatePool(pool: MasterPoolEntity)

    @Delete
    suspend fun deletePool(pool: MasterPoolEntity)

    @Query("DELETE FROM master_pool WHERE id = :id")
    suspend fun deletePoolById(id: Int)

    @Query("DELETE FROM master_pool")
    suspend fun deleteAllPools(): Int

    @Query("SELECT COUNT(*) FROM master_pool")
    suspend fun getPoolCount(): Int
}

@Dao
interface MasterKendaraanDao {
    @Query("SELECT * FROM master_kendaraan ORDER BY noMobil ASC")
    fun getAllKendaraan(): Flow<List<MasterKendaraanEntity>>

    @Query("SELECT * FROM master_kendaraan ORDER BY noMobil ASC")
    suspend fun getAllKendaraanList(): List<MasterKendaraanEntity>

    @Query("SELECT * FROM master_kendaraan WHERE id = :id LIMIT 1")
    suspend fun getKendaraanById(id: Int): MasterKendaraanEntity?

    @Query("SELECT * FROM master_kendaraan WHERE UPPER(TRIM(noMobil)) = UPPER(TRIM(:noMobil)) LIMIT 1")
    suspend fun getKendaraanByNoMobil(noMobil: String): MasterKendaraanEntity?

    @Query("SELECT * FROM master_kendaraan WHERE noMobil LIKE '%' || :query || '%' OR platNomer LIKE '%' || :query || '%'")
    fun searchKendaraan(query: String): Flow<List<MasterKendaraanEntity>>

    @Query("SELECT * FROM master_kendaraan WHERE pool = :pool ORDER BY noMobil ASC")
    fun getKendaraanByPool(pool: String): Flow<List<MasterKendaraanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKendaraan(kendaraan: MasterKendaraanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKendaraanList(list: List<MasterKendaraanEntity>)

    @Update
    suspend fun updateKendaraan(kendaraan: MasterKendaraanEntity)

    @Delete
    suspend fun deleteKendaraan(kendaraan: MasterKendaraanEntity)

    @Query("DELETE FROM master_kendaraan WHERE id = :id")
    suspend fun deleteKendaraanById(id: Int)

    @Query("DELETE FROM master_kendaraan WHERE noMobil IN ('101', '102', '103', '104', '105', '201', '202', '203', '301', '302', '401', '501')")
    suspend fun deleteDemoKendaraan(): Int

    @Query("DELETE FROM master_kendaraan")
    suspend fun deleteAllKendaraan(): Int

    @Query("SELECT COUNT(*) FROM master_kendaraan")
    suspend fun getKendaraanCount(): Int

    @Query("DELETE FROM master_kendaraan WHERE UPPER(TRIM(pool)) = UPPER(TRIM(:pool))")
    suspend fun deleteKendaraanByPool(pool: String): Int

    @Query("""
        SELECT * FROM master_kendaraan 
        WHERE (:pool = '' OR UPPER(TRIM(pool)) = UPPER(TRIM(:pool)))
          AND (:query = '' OR noMobil LIKE '%' || :query || '%' OR platNomer LIKE '%' || :query || '%' OR tipe LIKE '%' || :query || '%')
        ORDER BY noMobil ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun queryKendaraanPaged(pool: String, query: String, limit: Int, offset: Int): List<MasterKendaraanEntity>

    @Query("""
        SELECT COUNT(*) FROM master_kendaraan 
        WHERE (:pool = '' OR UPPER(TRIM(pool)) = UPPER(TRIM(:pool)))
          AND (:query = '' OR noMobil LIKE '%' || :query || '%' OR platNomer LIKE '%' || :query || '%' OR tipe LIKE '%' || :query || '%')
    """)
    suspend fun countKendaraanFiltered(pool: String, query: String): Int
}

@Dao
interface PengaturanNamaPhotoDao {
    @Query("SELECT * FROM pengaturan_namaphoto ORDER BY id ASC")
    fun getAllNamaPhoto(): Flow<List<PengaturanNamaPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNamaPhoto(item: PengaturanNamaPhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PengaturanNamaPhotoEntity>)

    @Update
    suspend fun updateNamaPhoto(item: PengaturanNamaPhotoEntity)

    @Query("SELECT COUNT(*) FROM pengaturan_namaphoto")
    suspend fun getCount(): Int
}

@Dao
interface DataAuditDao {
    @Query("SELECT * FROM data_audit ORDER BY timestamp DESC")
    fun getAllAudits(): Flow<List<DataAuditEntity>>

    @Query("SELECT * FROM data_audit ORDER BY timestamp DESC")
    suspend fun getAllAuditsList(): List<DataAuditEntity>

    @Query("SELECT * FROM data_audit WHERE nip = :nip ORDER BY timestamp DESC")
    fun getAuditsByNip(nip: String): Flow<List<DataAuditEntity>>

    @Query("SELECT * FROM data_audit WHERE (nip != '' AND nip = :nip) OR (namaPetugas != '' AND namaPetugas = :nama) ORDER BY timestamp DESC")
    fun getAuditsByUser(nip: String, nama: String): Flow<List<DataAuditEntity>>

    @Query("SELECT * FROM data_audit WHERE pool = :pool ORDER BY timestamp DESC")
    fun getAuditsByPool(pool: String): Flow<List<DataAuditEntity>>

    @Query("SELECT * FROM data_audit WHERE id = :id LIMIT 1")
    suspend fun getAuditById(id: Long): DataAuditEntity?

    @Query("SELECT * FROM data_audit WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedAudits(): List<DataAuditEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: DataAuditEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudits(audits: List<DataAuditEntity>)

    @Update
    suspend fun updateAudit(audit: DataAuditEntity)

    @Delete
    suspend fun deleteAudit(audit: DataAuditEntity)

    @Query("DELETE FROM data_audit WHERE id = :id")
    suspend fun deleteAuditById(id: Long)

    @Query("DELETE FROM data_audit WHERE nip = '2001' OR namaPetugas LIKE '%BUDI SANTOSO%' OR (id IN (1, 2, 3) AND noMobil IN ('101', '102', '201'))")
    suspend fun deleteDemoAudits(): Int

    @Query("DELETE FROM data_audit")
    suspend fun deleteAllAudits(): Int

    @Query("UPDATE data_audit SET isSynced = 1, syncTimestamp = :syncTime, syncError = null WHERE id = :id")
    suspend fun markAsSynced(id: Long, syncTime: Long = System.currentTimeMillis())

    @Query("UPDATE data_audit SET isSynced = 1, syncTimestamp = :syncTime, cloudId = :cloudId, syncError = null WHERE id = :id")
    suspend fun markAsSyncedWithCloudId(id: Long, cloudId: Long, syncTime: Long = System.currentTimeMillis())

    @Query("UPDATE data_audit SET cloudId = :cloudId WHERE id = :id")
    suspend fun updateCloudId(id: Long, cloudId: Long)

    @Query("SELECT * FROM data_audit WHERE auditUuid = :uuid LIMIT 1")
    suspend fun getAuditByUuid(uuid: String): DataAuditEntity?

    @Query("SELECT * FROM data_audit WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getAuditByCloudId(cloudId: Long): DataAuditEntity?

    @Query("SELECT COUNT(*) FROM data_audit")
    suspend fun getTotalAuditCount(): Int

    @Query("SELECT COUNT(*) FROM data_audit WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int
}

@Dao
interface MasterPeriodeDao {
    @Query("SELECT * FROM master_periode ORDER BY id ASC")
    fun getAllPeriode(): Flow<List<MasterPeriodeEntity>>

    @Query("SELECT * FROM master_periode ORDER BY id ASC")
    suspend fun getAllPeriodeList(): List<MasterPeriodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriode(periode: MasterPeriodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(periodes: List<MasterPeriodeEntity>)

    @Update
    suspend fun updatePeriode(periode: MasterPeriodeEntity)

    @Delete
    suspend fun deletePeriode(periode: MasterPeriodeEntity)

    @Query("DELETE FROM master_periode")
    suspend fun deleteAllPeriode(): Int

    @Query("SELECT COUNT(*) FROM master_periode")
    suspend fun getCount(): Int
}

