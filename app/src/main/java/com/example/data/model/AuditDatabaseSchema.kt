package com.example.data.model

import com.example.data.local.entity.DataAuditEntity
import com.example.util.UniqueCodeGenerator
import org.json.JSONObject

/**
 * Skema Database INPUT AUDIT (A1 s/d LK1 = 325 Kolom)
 */
object AuditDatabaseSchema {

    data class ColumnRule(
        val index: Int,
        val headerName: String,
        val type: String,
        val key: String = "",
        val slot: Int = 0
    )

    val INPUT_AUDIT_COLUMNS: List<String> = listOf(
        "ID",
        "TANGGAL",
        "WAKTU/TIMESTAMP",
        "NIP",
        "NAMA_AUDITOR",
        "PERIODE",
        "NO_KENDARAAN",
        "PLAT_NOMER",
        "TIPE",
        "WARNA",
        "TAHUN",
        "POOL",
        "FOTO_KENDARAAN_DEPAN",
        "FOTO_KENDARAAN_SAMPING_KIRI",
        "FOTO_KENDARAAN_INTERIOR_DEPAN_KIRI",
        "FOTO_KENDARAAN_INTERIOR_BELAKANG_KIRI",
        "FOTO_KENDARAAN_BELAKANG",
        "FOTO_KENDARAAN_INTERIOR_BELAKANG",
        "FOTO_KENDARAAN_SAMPING_KANAN",
        "JENIS_SAMPLE",
        "TANGGAL_SERVICE",
        "PEKET_SERVICE",
        "TOTAL_POIN_TEMUAN",
        "KEROPOS_POINT",
        "FOTO_KEROPOS",
        "CATATAN_KEROPOS",
        "PENYOK_POINT",
        "FOTO_PENYOK_1",
        "CATATAN_PENYOK_1",
        "DEKOK_POINT",
        "FOTO_DEKOK_1",
        "CATATAN_DEKOK_1",
        "FOTO_DEKOK_2",
        "CATATAN_DEKOK_2",
        "FOTO_DEKOK_3",
        "CATATAN_DEKOK_3",
        "FOTO_DEKOK_4",
        "CATATAN_DEKOK_4",
        "FOTO_DEKOK_5",
        "CATATAN_DEKOK_5",
        "BARET_HALUS_POINT",
        "PHOTO_BARET_HALUS_1",
        "CATATAN_BARET_HALUS_1",
        "PHOTO_BARET_HALUS_2",
        "CATATAN_BARET_HALUS_2",
        "PHOTO_BARET_HALUS_3",
        "CATATAN_BARET_HALUS_3",
        "PHOTO_BARET_HALUS_4",
        "CATATAN_BARET_HALUS_4",
        "PHOTO_BARET_HALUS_5",
        "CATATAN_BARET_HALUS_5",
        "BARET_POINT",
        "PHOTO_BARET_1",
        "CATATAN_BARET_1",
        "PHOTO_BARET_2",
        "CATATAN_BARET_2",
        "PHOTO_BARET_3",
        "CATATAN_BARET_3",
        "PHOTO_BARET_4",
        "CATATAN_BARET_4",
        "PHOTO_BARET_5",
        "CATATAN_BARET_5",
        "JAMUR_BODY_POINT",
        "FOTO_JAMUR_BODY_1",
        "CATATAN_JAMUR_BODY_1",
        "FOTO_JAMUR_BODY_2",
        "CATATAN_JAMUR_BODY_2",
        "FOTO_JAMUR_BODY_3",
        "CATATAN_JAMUR_BODY_3",
        "FOTO_JAMUR_BODY_4",
        "CATATAN_JAMUR_BODY_4",
        "FOTO_JAMUR_BODY_5",
        "CATATAN_JAMUR_BODY_5",
        "BUMPER_RENGGANG_POINT",
        "FOTO_BUMPER_RENGGANG_1",
        "CATATAN_BUMPER_RENGGANG_1",
        "FOTO_BUMPER_RENGGANG_2",
        "CATATAN_BUMPER_RENGGANG_2",
        "COVER_SPION_POINT",
        "FOTO_COVER_SPION_1",
        "CATATAN_COVER_SPION_1",
        "FOTO_COVER_SPION_2",
        "CATATAN_COVER_SPION_2",
        "CAT_BELANG_POINT",
        "FOTO_CAT_BELANG_1",
        "CATATAN_CAT_BELANG_1",
        "FOTO_CAT_BELANG_2",
        "CATATAN_CAT_BELANG_2",
        "FOTO_CAT_BELANG_3",
        "CATATAN_CAT_BELANG_3",
        "FOTO_CAT_BELANG_4",
        "CATATAN_CAT_BELANG_4",
        "FOTO_CAT_BELANG_4",
        "CATATAN_CAT_BELANG_4",
        "FOTO_CAT_BELANG_5",
        "CATATAN_CAT_BELANG_5",
        "CAT_KUSAM_POINT",
        "FOTO_CAT_KUSAM_1",
        "CATATAN_CAT_KUSAM_1",
        "FOTO_CAT_KUSAM_2",
        "CATATAN_CAT_KUSAM_2",
        "FOTO_CAT_KUSAM_3",
        "CATATAN_CAT_KUSAM_3",
        "FOTO_CAT_KUSAM_4",
        "CATATAN_CAT_KUSAM_4",
        "FOTO_CAT_KUSAM_5",
        "CATATAN_CAT_KUSAM_5",
        "CAT_GUGUS_POINT",
        "FOTO_CAT_GUGUS_1",
        "CATATAN_CAT_GUGUS_1",
        "FOTO_CAT_GUGUS_2",
        "CATATAN_CAT_GUGUS_2",
        "FOTO_CAT_GUGUS_3",
        "CATATAN_CAT_GUGUS_3",
        "FOTO_CAT_GUGUS_4",
        "CATATAN_CAT_GUGUS_4",
        "FOTO_CAT_GUGUS_5",
        "CATATAN_CAT_GUGUS_5",
        "RUSAK_LAINNYA_CAT_POINT",
        "FOTO_RUSAK_LAINYA_CAT_1",
        "CATATAN_RUSAK_LAINYA_CAT_1",
        "FOTO_RUSAK_LAINYA_CAT_2",
        "CATATAN_RUSAK_LAINYA_CAT_2",
        "FOTO_RUSAK_LAINYA_CAT_3",
        "CATATAN_RUSAK_LAINYA_CAT_3",
        "FOTO_RUSAK_LAINYA_CAT_4",
        "CATATAN_RUSAK_LAINYA_CAT_4",
        "FOTO_RUSAK_LAINYA_CAT_5",
        "CATATAN_RUSAK_LAINYA_CAT_5",
        "KACA_DEPAN_BERJAMUR_POINT",
        "FOTO_KACA_DEPAN_BERJAMUR",
        "CATATAN_KACA_DEPAN_BERJAMUR",
        "KACA_DEPAN_PELANGI_POINT",
        "FOTO_KACA_DEPAN_PELANGI",
        "CATATAN_KACA_DEPAN_PELANGI",
        "KACA_DEPAN_BARET_POINT",
        "FOTO_KACA_DEPAN_BARET",
        "CATATAN_KACA_DEPAN_BARET",
        "KACA_DEPAN_TITIK_POINT",
        "FOTO_KACA_DEPAN_TITIK",
        "CATATAN_KACA_DEPAN_TITIK",
        "KACA_SAMPING_BERJAMUR_POINT",
        "FOTO_KACA_SAMPING_BERJAMUR_1",
        "CATATAN_KACA_SAMPING_BERJAMUR_1",
        "FOTO_KACA_SAMPING_BERJAMUR_2",
        "CATATAN_KACA_SAMPING_BERJAMUR_2",
        "KACA_SAMPING_BARET_POINT",
        "FOTO_KACA_SAMPING_BARET_1",
        "CATATAN_KACA_SAMPING_BARET_1",
        "FOTO_KACA_SAMPING_BARET_2",
        "CATATAN_KACA_SAMPING_BARET_2",
        "KACA_BELAKANG_BERJAMUR_POINT",
        "FOTO_KACA_BELAKANG_BERJAMUR",
        "CATATAN_KACA_BELAKANG_BERJAMUR",
        "KACA_BELAKANG_BARET_POINT",
        "FOTO_KACA_BELAKANG_BARET",
        "CATATAN_KACA_BELAKANG_BARET",
        "KACA_SPION_BERJAMUR_POINT",
        "FOTO_KACA_SPION_BERJAMUR_1",
        "CATATAN_KACA_SPION_BERJAMUR_1",
        "FOTO_KACA_SPION_BERJAMUR_2",
        "CATATAN_KACA_SPION_BERJAMUR_2",
        "HEADLAMP_RUSAK_POINT",
        "FOTO_HEADLAMP_RUSAK_1",
        "CATATAN_HEADLAMP_RUSAK_1",
        "FOTO_HEADLAMP_RUSAK_2",
        "CATATAN_HEADLAMP_RUSAK_2",
        "HEADLAMP_KUSAM_POINT",
        "FOTO_HEADLAMP_KUSAM_1",
        "CATATAN_HEADLAMP_KUSAM_1",
        "FOTO_HEADLAMP_KUSAM_2",
        "CATATAN_HEADLAMP_KUSAM_2",
        "HEADLAMP_BEREMBUN_POINT",
        "FOTO_HEADLAMP_BEREMBUN_1",
        "CATATAN_HEADLAMP_BEREMBUN_1",
        "FOTO_HEADLAMP_BEREMBUN_2",
        "CATATAN_HEADLAMP_BEREMBUN_2",
        "STOPLAMP_RUSAK_POINT",
        "FOTO_STOPLAMP_RUSAK_1",
        "CATATAN_STOPLAMP_RUSAK_1",
        "FOTO_STOPLAMP_RUSAK_2",
        "CATATAN_STOPLAMP_RUSAK_2",
        "STOPLAMP_KUSAM_POINT",
        "FOTO_STOPLAMP_KUSAM_1",
        "CATATAN_STOPLAMP_KUSAM_1",
        "FOTO_STOPLAMP_KUSAM_2",
        "CATATAN_STOPLAMP_KUSAM_2",
        "STOPLAMP_BEREMBUN_POINT",
        "FOTO_STOPLAMP_BEREMBUN_1",
        "CATATAN_STOPLAMP_BEREMBUN_1",
        "FOTO_STOPLAMP_BEREMBUN_2",
        "CATATAN_STOPLAMP_BEREMBUN_2",
        "PLAFON_DEPAN_BERCAK_POINT",
        "FOTO_PLAPON_DEPAN_BERCAK",
        "CATATAN_PLAPON_DEPAN_BERCAK",
        "PLAFON_TENGAH_KOTOR_POINT",
        "FOTO_PLAFON_TENGAH_KOTOR",
        "CATATAN_PLAFON_TENGAH_KOTOR",
        "HANDGRIP_RUSAK_POINT",
        "FOTO_HANDGRIP_RUSAK",
        "CATATAN_HANDGRIP_RUSAK",
        "PLAFON_BELAKANG_KOTOR_POINT",
        "FOTO_PLAFON_BELAKANG_KOTOR",
        "CATATAN_PLAFON_BELAKANG_KOTOR",
        "PLAFON_BARET_RINGAN_POINT",
        "FOTO_PLAFON_BARET_RINGAN",
        "CATATAN_PLAFON_BARET_RINGAN",
        "PLAFON_BARET_PARAH_POINT",
        "FOTO_PALFON_BARET_PARAH",
        "CATATAN_PALFON_BARET_PARAH",
        "PLAFON_BRUDUL_POINT",
        "FOTO_PLAFON_BRUDUL",
        "CATATAN_PLAFON_BRUDUL",
        "KARPET_DASAR_SOBEK_POINT",
        "FOTO_KARPET_DASAR_SOBEK",
        "CATATAN_KARPET_DASAR_SOBEK",
        "KURSI_TAMU_RETAK_POINT",
        "FOTO_KURSI_TAMU_RETAK",
        "CATATAN_KURSI_TAMU_RETAK",
        "KURSI_TAMU_SOBEK_POINT",
        "FOTO_KURSI_TAMU_SOBEK",
        "CATATAN_KURSI_TAMU_SOBEK",
        "KURSI_PENGEMUDI_RETAK_POINT",
        "FOTO_KURSI_PENGEMUDI_RETAK",
        "CATATAN_KURSI_PENGEMUDI_RETAK",
        "KURSI_PENGEMUDI_SOBEK_POINT",
        "FOTO_KURSI_PENGEMUDI_SOBEK",
        "CATATAN_KURSI_PENGEMUDI_SOBEK",
        "SAFETY_BELT_KOTOR_POINT",
        "FOTO_SAFETY_BELT_KOTOR",
        "CATATAN_SAFETY_BELT_KOTOR",
        "SAFETY_BELT_RUSAK_POINT",
        "FOTO_SAFETY_BELT_RUSAK",
        "CATATAN_SAFETY_BELT_RUSAK",
        "SARUNG_VERSENELING_RUSAK_POINT",
        "FOTO_SARUNG_VERSENELING_RUSAK",
        "CATATAN_SARUNG_VERSENELING_RUSAK",
        "SARUNG_STEER_RUSAK_POINT",
        "FOTO_SARUNG_STEER_RUSAK",
        "CATATAN_SARUNG_STEER_RUSAK",
        "DASBOARD_RUSAK_POINT",
        "FOTO_DASBOARD_RUSAK",
        "CATATAN_DASBOARD_RUSAK",
        "KACA_FILM_POINT",
        "FOTO_KACA_FILM",
        "CATATAN_KACA_FILM",
        "PERANGKAP_KECOA_POINT",
        "FOTO_PERANGKAP_KECOA",
        "CATATAN_PERANGKAP_KECOA",
        "ADA_KECOA,KUTU_BUSUK_POINT",
        "FOTO_ADA_KECOA,KUTU_BUSUK",
        "CATATAN_ADA_KECOA,KUTU_BUSUK",
        "BAU_INTERIOR_CABIN_POINT",
        "FOTO_BAU_INTERIOR_CABIN",
        "CATATAN_BAU_INTERIOR_CABIN",
        "PELUMAS_ENGSEL_POINT",
        "FOTO_PELUMAS_ENGSEL_1",
        "CATATAN_PELUMAS_ENGSEL_1",
        "FOTO_PELUMAS_ENGSEL_2",
        "CATATAN_PELUMAS_ENGSEL_2",
        "FOTO_PELUMAS_ENGSEL_3",
        "CATATAN_PELUMAS_ENGSEL_3",
        "FOTO_PELUMAS_ENGSEL_4",
        "CATATAN_PELUMAS_ENGSEL_4",
        "FOTO_PELUMAS_ENGSEL_5",
        "CATATAN_PELUMAS_ENGSEL_5",
        "FOTO_PELUMAS_ENGSEL_6",
        "CATATAN_PELUMAS_ENGSEL_6",
        "KONDISI_RUANG_MESIN_POINT",
        "FOTO_KONDISI_RUANG_MESIN",
        "CATATAN_KONDISI_RUANG_MESIN",
        "KONDISI_KAP_MESIN_DALAM_POINT",
        "FOTO_KONDISI_KAP_MESIN_DALAM",
        "CATATAN_KONDISI_KAP_MESIN_DALAM",
        "KONDISI_TUTUP_BBM_POINT",
        "FOTO_KONDISI_TUTUP_BBM",
        "CATATAN_KONDISI_TUTUP_BBM",
        "GRIL_DEPAN_KOTOR_POINT",
        "FOTO_GRIL_DEPAN_KOTOR",
        "CATATAN_GRIL_DEPAN_KOTOR",
        "CUCIAN_EXTERIOR_TIDAK_BERSIH_POINT",
        "FOTO_CUCIAN_EXTERIOR_TIDAK_BERSIH_1",
        "CATATAN_CUCIAN_EXTERIOR_TIDAK_BERSIH_1",
        "FOTO_CUCIAN_EXTERIOR_TIDAK_BERSIH_2",
        "CATATAN_CUCIAN_EXTERIOR_TIDAK_BERSIH_2",
        "FOTO_CUCIAN_EXTERIOR_TIDAK_BERSIH_3",
        "CATATAN_CUCIAN_EXTERIOR_TIDAK_BERSIH_3",
        "FOTO_CUCIAN_EXTERIOR_TIDAK_BERSIH_4",
        "CATATAN_CUCIAN_EXTERIOR_TIDAK_BERSIH_4",
        "FOTO_CUCIAN_EXTERIOR_TIDAK_BERSIH_5",
        "CATATAN_CUCIAN_EXTERIOR_TIDAK_BERSIH_5",
        "VAKUM_TIDAK_BERSIH_POINT",
        "FOTO_VAKUM_TIDAK_BERSIH_1",
        "CATATAN_VAKUM_TIDAK_BERSIH_1",
        "FOTO_VAKUM_TIDAK_BERSIH_2",
        "CATATAN_VAKUM_TIDAK_BERSIH_2",
        "FOTO_VAKUM_TIDAK_BERSIH_3",
        "CATATAN_VAKUM_TIDAK_BERSIH_3",
        "FOTO_VAKUM_TIDAK_BERSIH_4",
        "CATATAN_VAKUM_TIDAK_BERSIH_4",
        "FOTO_VAKUM_TIDAK_BERSIH_5",
        "CATATAN_VAKUM_TIDAK_BERSIH_5",
        "PENGELAPAN_INTERIOR_POINT",
        "FOTO_PENGELAPAN_INTERIOR_1",
        "CATATAN_PENGELAPAN_INTERIOR_1",
        "FOTO_PENGELAPAN_INTERIOR_2",
        "CATATAN_PENGELAPAN_INTERIOR_2",
        "FOTO_PENGELAPAN_INTERIOR_3",
        "CATATAN_PENGELAPAN_INTERIOR_3",
        "FOTO_PENGELAPAN_INTERIOR_4",
        "CATATAN_PENGELAPAN_INTERIOR_4",
        "CUCIAN_RUANG_MESIN_POINT",
        "FOTO_CUCIAN_RUANG_MESIN",
        "CATATAN_CUCIAN_RUANG_MESIN",
        "KEBERSIHAN_ENGESEL-ENGSEL_POINT",
        "FOTO_KEBERSIHAN_ENGESEL-ENGSEL_1",
        "CATATAN_KEBERSIHAN_ENGESEL-ENGSEL_1",
        "FOTO_KEBERSIHAN_ENGESEL-ENGSEL_2",
        "CATATAN_KEBERSIHAN_ENGESEL-ENGSEL_2",
        "FOTO_KEBERSIHAN_ENGESEL-ENGSEL_3",
        "CATATAN_KEBERSIHAN_ENGESEL-ENGSEL_3",
        "FOTO_KEBERSIHAN_ENGESEL-ENGSEL_4",
        "CATATAN_KEBERSIHAN_ENGESEL-ENGSEL_4",
        "FOTO_KEBERSIHAN_ENGESEL-ENGSEL_5",
        "CATATAN_KEBERSIHAN_ENGESEL-ENGSEL_5",
    )

    val RULES: List<ColumnRule> = listOf(
        ColumnRule(0, "ID", "id", "", 0),
        ColumnRule(1, "TANGGAL", "field", "tanggal", 0),
        ColumnRule(2, "WAKTU/TIMESTAMP", "field", "waktu", 0),
        ColumnRule(3, "NIP", "field", "nip", 0),
        ColumnRule(4, "NAMA_AUDITOR", "field", "namaPetugas", 0),
        ColumnRule(5, "PERIODE", "field", "periode", 0),
        ColumnRule(6, "NO_KENDARAAN", "field", "noMobil", 0),
        ColumnRule(7, "PLAT_NOMER", "field", "platNomer", 0),
        ColumnRule(8, "TIPE", "field", "tipe", 0),
        ColumnRule(9, "WARNA", "field", "warna", 0),
        ColumnRule(10, "TAHUN", "field", "tahun", 0),
        ColumnRule(11, "POOL", "field", "pool", 0),
        ColumnRule(12, "FOTO_KENDARAAN_DEPAN", "v_photo", "kendaraan_depan", 0),
        ColumnRule(13, "FOTO_KENDARAAN_SAMPING_KIRI", "v_photo", "kendaraan_samping_kiri", 0),
        ColumnRule(14, "FOTO_KENDARAAN_INTERIOR_DEPAN_KIRI", "v_photo", "kendaraan_interior_depan_kiri", 0),
        ColumnRule(15, "FOTO_KENDARAAN_INTERIOR_BELAKANG_KIRI", "v_photo", "kendaraan_interior_belakang_kiri", 0),
        ColumnRule(16, "FOTO_KENDARAAN_BELAKANG", "v_photo", "kendaraan_belakang", 0),
        ColumnRule(17, "FOTO_KENDARAAN_INTERIOR_BELAKANG", "v_photo", "kendaraan_interior_belakang", 0),
        ColumnRule(18, "FOTO_KENDARAAN_SAMPING_KANAN", "v_photo", "kendaraan_samping_kanan", 0),
        ColumnRule(19, "JENIS_SAMPLE", "field", "jenisSample", 0),
        ColumnRule(20, "TANGGAL_SERVICE", "field", "tanggalService", 0),
        ColumnRule(21, "PEKET_SERVICE", "field", "paketService", 0),
        ColumnRule(22, "TOTAL_POIN_TEMUAN", "field", "totalPoinTemuan", 0),
        ColumnRule(23, "KEROPOS_POINT", "item_point", "body_keropos", 0),
        ColumnRule(24, "FOTO_KEROPOS", "item_photo", "body_keropos_0", 0),
        ColumnRule(25, "CATATAN_KEROPOS", "item_note", "body_keropos_0", 0),
        ColumnRule(26, "PENYOK_POINT", "item_point", "body_penyok", 0),
        ColumnRule(27, "FOTO_PENYOK_1", "item_photo", "body_penyok_0", 0),
        ColumnRule(28, "CATATAN_PENYOK_1", "item_note", "body_penyok_0", 0),
        ColumnRule(29, "DEKOK_POINT", "item_point", "body_dekok", 0),
        ColumnRule(30, "FOTO_DEKOK_1", "item_photo", "body_dekok_0", 0),
        ColumnRule(31, "CATATAN_DEKOK_1", "item_note", "body_dekok_0", 0),
        ColumnRule(32, "FOTO_DEKOK_2", "item_photo", "body_dekok_1", 1),
        ColumnRule(33, "CATATAN_DEKOK_2", "item_note", "body_dekok_1", 1),
        ColumnRule(34, "FOTO_DEKOK_3", "item_photo", "body_dekok_2", 2),
        ColumnRule(35, "CATATAN_DEKOK_3", "item_note", "body_dekok_2", 2),
        ColumnRule(36, "FOTO_DEKOK_4", "item_photo", "body_dekok_3", 3),
        ColumnRule(37, "CATATAN_DEKOK_4", "item_note", "body_dekok_3", 3),
        ColumnRule(38, "FOTO_DEKOK_5", "item_photo", "body_dekok_4", 4),
        ColumnRule(39, "CATATAN_DEKOK_5", "item_note", "body_dekok_4", 4),
        ColumnRule(40, "BARET_HALUS_POINT", "item_point", "body_baret_halus", 0),
        ColumnRule(41, "PHOTO_BARET_HALUS_1", "item_photo", "body_baret_halus_0", 0),
        ColumnRule(42, "CATATAN_BARET_HALUS_1", "item_note", "body_baret_halus_0", 0),
        ColumnRule(43, "PHOTO_BARET_HALUS_2", "item_photo", "body_baret_halus_1", 1),
        ColumnRule(44, "CATATAN_BARET_HALUS_2", "item_note", "body_baret_halus_1", 1),
        ColumnRule(45, "PHOTO_BARET_HALUS_3", "item_photo", "body_baret_halus_2", 2),
        ColumnRule(46, "CATATAN_BARET_HALUS_3", "item_note", "body_baret_halus_2", 2),
        ColumnRule(47, "PHOTO_BARET_HALUS_4", "item_photo", "body_baret_halus_3", 3),
        ColumnRule(48, "CATATAN_BARET_HALUS_4", "item_note", "body_baret_halus_3", 3),
        ColumnRule(49, "PHOTO_BARET_HALUS_5", "item_photo", "body_baret_halus_4", 4),
        ColumnRule(50, "CATATAN_BARET_HALUS_5", "item_note", "body_baret_halus_4", 4),
        ColumnRule(51, "BARET_POINT", "item_point", "body_baret", 0),
        ColumnRule(52, "PHOTO_BARET_1", "item_photo", "body_baret_0", 0),
        ColumnRule(53, "CATATAN_BARET_1", "item_note", "body_baret_0", 0),
        ColumnRule(54, "PHOTO_BARET_2", "item_photo", "body_baret_1", 1),
        ColumnRule(55, "CATATAN_BARET_2", "item_note", "body_baret_1", 1),
        ColumnRule(56, "PHOTO_BARET_3", "item_photo", "body_baret_2", 2),
        ColumnRule(57, "CATATAN_BARET_3", "item_note", "body_baret_2", 2),
        ColumnRule(58, "PHOTO_BARET_4", "item_photo", "body_baret_3", 3),
        ColumnRule(59, "CATATAN_BARET_4", "item_note", "body_baret_3", 3),
        ColumnRule(60, "PHOTO_BARET_5", "item_photo", "body_baret_4", 4),
        ColumnRule(61, "CATATAN_BARET_5", "item_note", "body_baret_4", 4),
        ColumnRule(62, "JAMUR_BODY_POINT", "item_point", "body_jamur", 0),
        ColumnRule(63, "FOTO_JAMUR_BODY_1", "item_photo", "body_jamur_0", 0),
        ColumnRule(64, "CATATAN_JAMUR_BODY_1", "item_note", "body_jamur_0", 0),
        ColumnRule(65, "FOTO_JAMUR_BODY_2", "item_photo", "body_jamur_1", 1),
        ColumnRule(66, "CATATAN_JAMUR_BODY_2", "item_note", "body_jamur_1", 1),
        ColumnRule(67, "FOTO_JAMUR_BODY_3", "item_photo", "body_jamur_2", 2),
        ColumnRule(68, "CATATAN_JAMUR_BODY_3", "item_note", "body_jamur_2", 2),
        ColumnRule(69, "FOTO_JAMUR_BODY_4", "item_photo", "body_jamur_3", 3),
        ColumnRule(70, "CATATAN_JAMUR_BODY_4", "item_note", "body_jamur_3", 3),
        ColumnRule(71, "FOTO_JAMUR_BODY_5", "item_photo", "body_jamur_4", 4),
        ColumnRule(72, "CATATAN_JAMUR_BODY_5", "item_note", "body_jamur_4", 4),
        ColumnRule(73, "BUMPER_RENGGANG_POINT", "item_point", "body_bumper_rengang", 0),
        ColumnRule(74, "FOTO_BUMPER_RENGGANG_1", "item_photo", "body_bumper_rengang_0", 0),
        ColumnRule(75, "CATATAN_BUMPER_RENGGANG_1", "item_note", "body_bumper_rengang_0", 0),
        ColumnRule(76, "FOTO_BUMPER_RENGGANG_2", "item_photo", "body_bumper_rengang_1", 1),
        ColumnRule(77, "CATATAN_BUMPER_RENGGANG_2", "item_note", "body_bumper_rengang_1", 1),
        ColumnRule(78, "COVER_SPION_POINT", "item_point", "body_cover_spion", 0),
        ColumnRule(79, "FOTO_COVER_SPION_1", "item_photo", "body_cover_spion_0", 0),
        ColumnRule(80, "CATATAN_COVER_SPION_1", "item_note", "body_cover_spion_0", 0),
        ColumnRule(81, "FOTO_COVER_SPION_2", "item_photo", "cat_belang_4", 4),
        ColumnRule(82, "CATATAN_COVER_SPION_2", "item_note", "cat_belang_4", 4),
        ColumnRule(83, "CAT_BELANG_POINT", "item_point", "cat_belang", 0),
        ColumnRule(84, "FOTO_CAT_BELANG_1", "item_photo", "cat_belang_0", 0),
        ColumnRule(85, "CATATAN_CAT_BELANG_1", "item_note", "cat_belang_0", 0),
        ColumnRule(86, "FOTO_CAT_BELANG_2", "item_photo", "cat_belang_1", 1),
        ColumnRule(87, "CATATAN_CAT_BELANG_2", "item_note", "cat_belang_1", 1),
        ColumnRule(88, "FOTO_CAT_BELANG_3", "item_photo", "cat_belang_2", 2),
        ColumnRule(89, "CATATAN_CAT_BELANG_3", "item_note", "cat_belang_2", 2),
        ColumnRule(90, "FOTO_CAT_BELANG_4", "item_photo", "cat_belang_3", 3),
        ColumnRule(91, "CATATAN_CAT_BELANG_4", "item_note", "cat_belang_3", 3),
        ColumnRule(92, "FOTO_CAT_BELANG_4", "item_photo", "cat_belang_3", 3),
        ColumnRule(93, "CATATAN_CAT_BELANG_4", "item_note", "cat_belang_3", 3),
        ColumnRule(94, "FOTO_CAT_BELANG_5", "item_photo", "cat_belang_5", 5),
        ColumnRule(95, "CATATAN_CAT_BELANG_5", "item_note", "cat_belang_5", 5),
        ColumnRule(96, "CAT_KUSAM_POINT", "item_point", "cat_kusam", 0),
        ColumnRule(97, "FOTO_CAT_KUSAM_1", "item_photo", "cat_kusam_0", 0),
        ColumnRule(98, "CATATAN_CAT_KUSAM_1", "item_note", "cat_kusam_0", 0),
        ColumnRule(99, "FOTO_CAT_KUSAM_2", "item_photo", "cat_kusam_1", 1),
        ColumnRule(100, "CATATAN_CAT_KUSAM_2", "item_note", "cat_kusam_1", 1),
        ColumnRule(101, "FOTO_CAT_KUSAM_3", "item_photo", "cat_kusam_2", 2),
        ColumnRule(102, "CATATAN_CAT_KUSAM_3", "item_note", "cat_kusam_2", 2),
        ColumnRule(103, "FOTO_CAT_KUSAM_4", "item_photo", "cat_kusam_3", 3),
        ColumnRule(104, "CATATAN_CAT_KUSAM_4", "item_note", "cat_kusam_3", 3),
        ColumnRule(105, "FOTO_CAT_KUSAM_5", "item_photo", "cat_kusam_4", 4),
        ColumnRule(106, "CATATAN_CAT_KUSAM_5", "item_note", "cat_kusam_4", 4),
        ColumnRule(107, "CAT_GUGUS_POINT", "item_point", "cat_gugus", 0),
        ColumnRule(108, "FOTO_CAT_GUGUS_1", "item_photo", "cat_gugus_0", 0),
        ColumnRule(109, "CATATAN_CAT_GUGUS_1", "item_note", "cat_gugus_0", 0),
        ColumnRule(110, "FOTO_CAT_GUGUS_2", "item_photo", "cat_gugus_1", 1),
        ColumnRule(111, "CATATAN_CAT_GUGUS_2", "item_note", "cat_gugus_1", 1),
        ColumnRule(112, "FOTO_CAT_GUGUS_3", "item_photo", "cat_gugus_2", 2),
        ColumnRule(113, "CATATAN_CAT_GUGUS_3", "item_note", "cat_gugus_2", 2),
        ColumnRule(114, "FOTO_CAT_GUGUS_4", "item_photo", "cat_gugus_3", 3),
        ColumnRule(115, "CATATAN_CAT_GUGUS_4", "item_note", "cat_gugus_3", 3),
        ColumnRule(116, "FOTO_CAT_GUGUS_5", "item_photo", "cat_gugus_4", 4),
        ColumnRule(117, "CATATAN_CAT_GUGUS_5", "item_note", "cat_gugus_4", 4),
        ColumnRule(118, "RUSAK_LAINNYA_CAT_POINT", "item_point", "cat_rusak_lainya", 0),
        ColumnRule(119, "FOTO_RUSAK_LAINYA_CAT_1", "item_photo", "cat_rusak_lainya_0", 0),
        ColumnRule(120, "CATATAN_RUSAK_LAINYA_CAT_1", "item_note", "cat_rusak_lainya_0", 0),
        ColumnRule(121, "FOTO_RUSAK_LAINYA_CAT_2", "item_photo", "cat_rusak_lainya_1", 1),
        ColumnRule(122, "CATATAN_RUSAK_LAINYA_CAT_2", "item_note", "cat_rusak_lainya_1", 1),
        ColumnRule(123, "FOTO_RUSAK_LAINYA_CAT_3", "item_photo", "cat_rusak_lainya_2", 2),
        ColumnRule(124, "CATATAN_RUSAK_LAINYA_CAT_3", "item_note", "cat_rusak_lainya_2", 2),
        ColumnRule(125, "FOTO_RUSAK_LAINYA_CAT_4", "item_photo", "cat_rusak_lainya_3", 3),
        ColumnRule(126, "CATATAN_RUSAK_LAINYA_CAT_4", "item_note", "cat_rusak_lainya_3", 3),
        ColumnRule(127, "FOTO_RUSAK_LAINYA_CAT_5", "item_photo", "cat_rusak_lainya_4", 4),
        ColumnRule(128, "CATATAN_RUSAK_LAINYA_CAT_5", "item_note", "cat_rusak_lainya_4", 4),
        ColumnRule(129, "KACA_DEPAN_BERJAMUR_POINT", "item_point", "kaca_depan_berjamur", 0),
        ColumnRule(130, "FOTO_KACA_DEPAN_BERJAMUR", "item_photo", "kaca_depan_berjamur_0", 0),
        ColumnRule(131, "CATATAN_KACA_DEPAN_BERJAMUR", "item_note", "kaca_depan_berjamur_0", 0),
        ColumnRule(132, "KACA_DEPAN_PELANGI_POINT", "item_point", "kaca_depan_pelangi", 0),
        ColumnRule(133, "FOTO_KACA_DEPAN_PELANGI", "item_photo", "kaca_depan_pelangi_0", 0),
        ColumnRule(134, "CATATAN_KACA_DEPAN_PELANGI", "item_note", "kaca_depan_pelangi_0", 0),
        ColumnRule(135, "KACA_DEPAN_BARET_POINT", "item_point", "kaca_depan_baret", 0),
        ColumnRule(136, "FOTO_KACA_DEPAN_BARET", "item_photo", "kaca_depan_baret_0", 0),
        ColumnRule(137, "CATATAN_KACA_DEPAN_BARET", "item_note", "kaca_depan_baret_0", 0),
        ColumnRule(138, "KACA_DEPAN_TITIK_POINT", "item_point", "kaca_depan_titik_retak", 0),
        ColumnRule(139, "FOTO_KACA_DEPAN_TITIK", "item_photo", "kaca_depan_titik_retak_0", 0),
        ColumnRule(140, "CATATAN_KACA_DEPAN_TITIK", "item_note", "kaca_depan_titik_retak_0", 0),
        ColumnRule(141, "KACA_SAMPING_BERJAMUR_POINT", "item_point", "kaca_depan_samping_berjamur", 0),
        ColumnRule(142, "FOTO_KACA_SAMPING_BERJAMUR_1", "item_photo", "kaca_depan_samping_berjamur_0", 0),
        ColumnRule(143, "CATATAN_KACA_SAMPING_BERJAMUR_1", "item_note", "kaca_depan_samping_berjamur_0", 0),
        ColumnRule(144, "FOTO_KACA_SAMPING_BERJAMUR_2", "item_photo", "kaca_depan_samping_berjamur_1", 1),
        ColumnRule(145, "CATATAN_KACA_SAMPING_BERJAMUR_2", "item_note", "kaca_depan_samping_berjamur_1", 1),
        ColumnRule(146, "KACA_SAMPING_BARET_POINT", "item_point", "kaca_depan_samping_baret", 0),
        ColumnRule(147, "FOTO_KACA_SAMPING_BARET_1", "item_photo", "kaca_depan_samping_baret_0", 0),
        ColumnRule(148, "CATATAN_KACA_SAMPING_BARET_1", "item_note", "kaca_depan_samping_baret_0", 0),
        ColumnRule(149, "FOTO_KACA_SAMPING_BARET_2", "item_photo", "kaca_depan_samping_baret_1", 1),
        ColumnRule(150, "CATATAN_KACA_SAMPING_BARET_2", "item_note", "kaca_depan_samping_baret_1", 1),
        ColumnRule(151, "KACA_BELAKANG_BERJAMUR_POINT", "item_point", "kaca_belakang_berjamur", 0),
        ColumnRule(152, "FOTO_KACA_BELAKANG_BERJAMUR", "item_photo", "kaca_belakang_berjamur_0", 0),
        ColumnRule(153, "CATATAN_KACA_BELAKANG_BERJAMUR", "item_note", "kaca_belakang_berjamur_0", 0),
        ColumnRule(154, "KACA_BELAKANG_BARET_POINT", "item_point", "kaca_belakang_baret", 0),
        ColumnRule(155, "FOTO_KACA_BELAKANG_BARET", "item_photo", "kaca_belakang_baret_0", 0),
        ColumnRule(156, "CATATAN_KACA_BELAKANG_BARET", "item_note", "kaca_belakang_baret_0", 0),
        ColumnRule(157, "KACA_SPION_BERJAMUR_POINT", "item_point", "kaca_spion_berjamur", 0),
        ColumnRule(158, "FOTO_KACA_SPION_BERJAMUR_1", "item_photo", "kaca_spion_berjamur_0", 0),
        ColumnRule(159, "CATATAN_KACA_SPION_BERJAMUR_1", "item_note", "kaca_spion_berjamur_0", 0),
        ColumnRule(160, "FOTO_KACA_SPION_BERJAMUR_2", "item_photo", "kaca_spion_berjamur_1", 1),
        ColumnRule(161, "CATATAN_KACA_SPION_BERJAMUR_2", "item_note", "kaca_spion_berjamur_1", 1),
        ColumnRule(162, "HEADLAMP_RUSAK_POINT", "item_point", "headlamp_rusak_retak_pecah", 0),
        ColumnRule(163, "FOTO_HEADLAMP_RUSAK_1", "item_photo", "headlamp_rusak_retak_pecah_0", 0),
        ColumnRule(164, "CATATAN_HEADLAMP_RUSAK_1", "item_note", "headlamp_rusak_retak_pecah_0", 0),
        ColumnRule(165, "FOTO_HEADLAMP_RUSAK_2", "item_photo", "headlamp_rusak_retak_pecah_1", 1),
        ColumnRule(166, "CATATAN_HEADLAMP_RUSAK_2", "item_note", "headlamp_rusak_retak_pecah_1", 1),
        ColumnRule(167, "HEADLAMP_KUSAM_POINT", "item_point", "headlamp_kusam_buram", 0),
        ColumnRule(168, "FOTO_HEADLAMP_KUSAM_1", "item_photo", "headlamp_kusam_buram_0", 0),
        ColumnRule(169, "CATATAN_HEADLAMP_KUSAM_1", "item_note", "headlamp_kusam_buram_0", 0),
        ColumnRule(170, "FOTO_HEADLAMP_KUSAM_2", "item_photo", "headlamp_kusam_buram_1", 1),
        ColumnRule(171, "CATATAN_HEADLAMP_KUSAM_2", "item_note", "headlamp_kusam_buram_1", 1),
        ColumnRule(172, "HEADLAMP_BEREMBUN_POINT", "item_point", "headlamp_berembun", 0),
        ColumnRule(173, "FOTO_HEADLAMP_BEREMBUN_1", "item_photo", "headlamp_berembun_0", 0),
        ColumnRule(174, "CATATAN_HEADLAMP_BEREMBUN_1", "item_note", "headlamp_berembun_0", 0),
        ColumnRule(175, "FOTO_HEADLAMP_BEREMBUN_2", "item_photo", "headlamp_berembun_1", 1),
        ColumnRule(176, "CATATAN_HEADLAMP_BEREMBUN_2", "item_note", "headlamp_berembun_1", 1),
        ColumnRule(177, "STOPLAMP_RUSAK_POINT", "item_point", "stoplamp_rusak_retak_pecah", 0),
        ColumnRule(178, "FOTO_STOPLAMP_RUSAK_1", "item_photo", "stoplamp_rusak_retak_pecah_0", 0),
        ColumnRule(179, "CATATAN_STOPLAMP_RUSAK_1", "item_note", "stoplamp_rusak_retak_pecah_0", 0),
        ColumnRule(180, "FOTO_STOPLAMP_RUSAK_2", "item_photo", "stoplamp_rusak_retak_pecah_1", 1),
        ColumnRule(181, "CATATAN_STOPLAMP_RUSAK_2", "item_note", "stoplamp_rusak_retak_pecah_1", 1),
        ColumnRule(182, "STOPLAMP_KUSAM_POINT", "item_point", "stoplamp_kusam_berembun", 0),
        ColumnRule(183, "FOTO_STOPLAMP_KUSAM_1", "item_photo", "stoplamp_kusam_berembun_0", 0),
        ColumnRule(184, "CATATAN_STOPLAMP_KUSAM_1", "item_note", "stoplamp_kusam_berembun_0", 0),
        ColumnRule(185, "FOTO_STOPLAMP_KUSAM_2", "item_photo", "stoplamp_kusam_berembun_1", 1),
        ColumnRule(186, "CATATAN_STOPLAMP_KUSAM_2", "item_note", "stoplamp_kusam_berembun_1", 1),
        ColumnRule(187, "STOPLAMP_BEREMBUN_POINT", "item_point", "stoplamp_berembun", 0),
        ColumnRule(188, "FOTO_STOPLAMP_BEREMBUN_1", "item_photo", "stoplamp_berembun_0", 0),
        ColumnRule(189, "CATATAN_STOPLAMP_BEREMBUN_1", "item_note", "stoplamp_berembun_0", 0),
        ColumnRule(190, "FOTO_STOPLAMP_BEREMBUN_2", "item_photo", "stoplamp_berembun_1", 1),
        ColumnRule(191, "CATATAN_STOPLAMP_BEREMBUN_2", "item_note", "stoplamp_berembun_1", 1),
        ColumnRule(192, "PLAFON_DEPAN_BERCAK_POINT", "item_point", "plafon_depan_sunvisor_kotor", 0),
        ColumnRule(193, "FOTO_PLAPON_DEPAN_BERCAK", "item_photo", "plafon_depan_sunvisor_kotor_0", 0),
        ColumnRule(194, "CATATAN_PLAPON_DEPAN_BERCAK", "item_note", "plafon_depan_sunvisor_kotor_0", 0),
        ColumnRule(195, "PLAFON_TENGAH_KOTOR_POINT", "item_point", "plafon_tengah_kotor", 0),
        ColumnRule(196, "FOTO_PLAFON_TENGAH_KOTOR", "item_photo", "plafon_tengah_kotor_0", 0),
        ColumnRule(197, "CATATAN_PLAFON_TENGAH_KOTOR", "item_note", "plafon_tengah_kotor_0", 0),
        ColumnRule(198, "HANDGRIP_RUSAK_POINT", "item_point", "plafon_handgrip_rusak", 0),
        ColumnRule(199, "FOTO_HANDGRIP_RUSAK", "item_photo", "plafon_handgrip_rusak_0", 0),
        ColumnRule(200, "CATATAN_HANDGRIP_RUSAK", "item_note", "plafon_handgrip_rusak_0", 0),
        ColumnRule(201, "PLAFON_BELAKANG_KOTOR_POINT", "item_point", "plafon_belakang_kotor", 0),
        ColumnRule(202, "FOTO_PLAFON_BELAKANG_KOTOR", "item_photo", "plafon_belakang_kotor_0", 0),
        ColumnRule(203, "CATATAN_PLAFON_BELAKANG_KOTOR", "item_note", "plafon_belakang_kotor_0", 0),
        ColumnRule(204, "PLAFON_BARET_RINGAN_POINT", "item_point", "plafon_baret_ringan", 0),
        ColumnRule(205, "FOTO_PLAFON_BARET_RINGAN", "item_photo", "plafon_baret_ringan_0", 0),
        ColumnRule(206, "CATATAN_PLAFON_BARET_RINGAN", "item_note", "plafon_baret_ringan_0", 0),
        ColumnRule(207, "PLAFON_BARET_PARAH_POINT", "item_point", "plafon_baret_parah", 0),
        ColumnRule(208, "FOTO_PALFON_BARET_PARAH", "item_photo", "plafon_baret_parah_0", 0),
        ColumnRule(209, "CATATAN_PALFON_BARET_PARAH", "item_note", "plafon_baret_parah_0", 0),
        ColumnRule(210, "PLAFON_BRUDUL_POINT", "item_point", "plafon_brudul", 0),
        ColumnRule(211, "FOTO_PLAFON_BRUDUL", "item_photo", "plafon_brudul_0", 0),
        ColumnRule(212, "CATATAN_PLAFON_BRUDUL", "item_note", "plafon_brudul_0", 0),
        ColumnRule(213, "KARPET_DASAR_SOBEK_POINT", "item_point", "karpet_dasar_sobek", 0),
        ColumnRule(214, "FOTO_KARPET_DASAR_SOBEK", "item_photo", "karpet_dasar_sobek_0", 0),
        ColumnRule(215, "CATATAN_KARPET_DASAR_SOBEK", "item_note", "karpet_dasar_sobek_0", 0),
        ColumnRule(216, "KURSI_TAMU_RETAK_POINT", "item_point", "kursi_tamu_retak", 0),
        ColumnRule(217, "FOTO_KURSI_TAMU_RETAK", "item_photo", "kursi_tamu_retak_0", 0),
        ColumnRule(218, "CATATAN_KURSI_TAMU_RETAK", "item_note", "kursi_tamu_retak_0", 0),
        ColumnRule(219, "KURSI_TAMU_SOBEK_POINT", "item_point", "kursi_tamu_sobek", 0),
        ColumnRule(220, "FOTO_KURSI_TAMU_SOBEK", "item_photo", "kursi_tamu_sobek_0", 0),
        ColumnRule(221, "CATATAN_KURSI_TAMU_SOBEK", "item_note", "kursi_tamu_sobek_0", 0),
        ColumnRule(222, "KURSI_PENGEMUDI_RETAK_POINT", "item_point", "kursi_pengemudi_retak", 0),
        ColumnRule(223, "FOTO_KURSI_PENGEMUDI_RETAK", "item_photo", "kursi_pengemudi_retak_0", 0),
        ColumnRule(224, "CATATAN_KURSI_PENGEMUDI_RETAK", "item_note", "kursi_pengemudi_retak_0", 0),
        ColumnRule(225, "KURSI_PENGEMUDI_SOBEK_POINT", "item_point", "kursi_pengemudi_sobek", 0),
        ColumnRule(226, "FOTO_KURSI_PENGEMUDI_SOBEK", "item_photo", "kursi_pengemudi_sobek_0", 0),
        ColumnRule(227, "CATATAN_KURSI_PENGEMUDI_SOBEK", "item_note", "kursi_pengemudi_sobek_0", 0),
        ColumnRule(228, "SAFETY_BELT_KOTOR_POINT", "item_point", "safety_belt_kotor", 0),
        ColumnRule(229, "FOTO_SAFETY_BELT_KOTOR", "item_photo", "safety_belt_kotor_0", 0),
        ColumnRule(230, "CATATAN_SAFETY_BELT_KOTOR", "item_note", "safety_belt_kotor_0", 0),
        ColumnRule(231, "SAFETY_BELT_RUSAK_POINT", "item_point", "safety_belt_rusak", 0),
        ColumnRule(232, "FOTO_SAFETY_BELT_RUSAK", "item_photo", "safety_belt_rusak_0", 0),
        ColumnRule(233, "CATATAN_SAFETY_BELT_RUSAK", "item_note", "safety_belt_rusak_0", 0),
        ColumnRule(234, "SARUNG_VERSENELING_RUSAK_POINT", "item_point", "sarung_verseneling_rusak", 0),
        ColumnRule(235, "FOTO_SARUNG_VERSENELING_RUSAK", "item_photo", "sarung_verseneling_rusak_0", 0),
        ColumnRule(236, "CATATAN_SARUNG_VERSENELING_RUSAK", "item_note", "sarung_verseneling_rusak_0", 0),
        ColumnRule(237, "SARUNG_STEER_RUSAK_POINT", "item_point", "sarung_steer_rusak", 0),
        ColumnRule(238, "FOTO_SARUNG_STEER_RUSAK", "item_photo", "sarung_steer_rusak_0", 0),
        ColumnRule(239, "CATATAN_SARUNG_STEER_RUSAK", "item_note", "sarung_steer_rusak_0", 0),
        ColumnRule(240, "DASBOARD_RUSAK_POINT", "item_point", "dashboard_rusak", 0),
        ColumnRule(241, "FOTO_DASBOARD_RUSAK", "item_photo", "dashboard_rusak_0", 0),
        ColumnRule(242, "CATATAN_DASBOARD_RUSAK", "item_note", "dashboard_rusak_0", 0),
        ColumnRule(243, "KACA_FILM_POINT", "item_point", "kaca_film", 0),
        ColumnRule(244, "FOTO_KACA_FILM", "item_photo", "kaca_film_0", 0),
        ColumnRule(245, "CATATAN_KACA_FILM", "item_note", "kaca_film_0", 0),
        ColumnRule(246, "PERANGKAP_KECOA_POINT", "item_point", "perangkap_kecoa", 0),
        ColumnRule(247, "FOTO_PERANGKAP_KECOA", "item_photo", "perangkap_kecoa_0", 0),
        ColumnRule(248, "CATATAN_PERANGKAP_KECOA", "item_note", "perangkap_kecoa_0", 0),
        ColumnRule(249, "ADA_KECOA,KUTU_BUSUK_POINT", "item_point", "ada_kecoa_kutu", 0),
        ColumnRule(250, "FOTO_ADA_KECOA,KUTU_BUSUK", "item_photo", "ada_kecoa_kutu_0", 0),
        ColumnRule(251, "CATATAN_ADA_KECOA,KUTU_BUSUK", "item_note", "ada_kecoa_kutu_0", 0),
        ColumnRule(252, "BAU_INTERIOR_CABIN_POINT", "item_point", "bau_interior_cabin", 0),
        ColumnRule(253, "FOTO_BAU_INTERIOR_CABIN", "item_photo", "bau_interior_cabin_0", 0),
        ColumnRule(254, "CATATAN_BAU_INTERIOR_CABIN", "item_note", "bau_interior_cabin_0", 0),
        ColumnRule(255, "PELUMAS_ENGSEL_POINT", "item_point", "pelumas_engsel", 0),
        ColumnRule(256, "FOTO_PELUMAS_ENGSEL_1", "item_photo", "pelumas_engsel_0", 0),
        ColumnRule(257, "CATATAN_PELUMAS_ENGSEL_1", "item_note", "pelumas_engsel_0", 0),
        ColumnRule(258, "FOTO_PELUMAS_ENGSEL_2", "item_photo", "pelumas_engsel_1", 1),
        ColumnRule(259, "CATATAN_PELUMAS_ENGSEL_2", "item_note", "pelumas_engsel_1", 1),
        ColumnRule(260, "FOTO_PELUMAS_ENGSEL_3", "item_photo", "pelumas_engsel_2", 2),
        ColumnRule(261, "CATATAN_PELUMAS_ENGSEL_3", "item_note", "pelumas_engsel_2", 2),
        ColumnRule(262, "FOTO_PELUMAS_ENGSEL_4", "item_photo", "pelumas_engsel_3", 3),
        ColumnRule(263, "CATATAN_PELUMAS_ENGSEL_4", "item_note", "pelumas_engsel_3", 3),
        ColumnRule(264, "FOTO_PELUMAS_ENGSEL_5", "item_photo", "pelumas_engsel_4", 4),
        ColumnRule(265, "CATATAN_PELUMAS_ENGSEL_5", "item_note", "pelumas_engsel_4", 4),
        ColumnRule(266, "FOTO_PELUMAS_ENGSEL_6", "item_photo", "pelumas_engsel_5", 5),
        ColumnRule(267, "CATATAN_PELUMAS_ENGSEL_6", "item_note", "pelumas_engsel_5", 5),
        ColumnRule(268, "KONDISI_RUANG_MESIN_POINT", "item_point", "kondisi_ruang_mesin", 0),
        ColumnRule(269, "FOTO_KONDISI_RUANG_MESIN", "item_photo", "kondisi_ruang_mesin_0", 0),
        ColumnRule(270, "CATATAN_KONDISI_RUANG_MESIN", "item_note", "kondisi_ruang_mesin_0", 0),
        ColumnRule(271, "KONDISI_KAP_MESIN_DALAM_POINT", "item_point", "kondisi_kap_mesin_dalam", 0),
        ColumnRule(272, "FOTO_KONDISI_KAP_MESIN_DALAM", "item_photo", "kondisi_kap_mesin_dalam_0", 0),
        ColumnRule(273, "CATATAN_KONDISI_KAP_MESIN_DALAM", "item_note", "kondisi_kap_mesin_dalam_0", 0),
        ColumnRule(274, "KONDISI_TUTUP_BBM_POINT", "item_point", "kondisi_tutup_bbm", 0),
        ColumnRule(275, "FOTO_KONDISI_TUTUP_BBM", "item_photo", "kondisi_tutup_bbm_0", 0),
        ColumnRule(276, "CATATAN_KONDISI_TUTUP_BBM", "item_note", "kondisi_tutup_bbm_0", 0),
        ColumnRule(277, "GRIL_DEPAN_KOTOR_POINT", "item_point", "cuc_gril_depan_kotor", 0),
        ColumnRule(278, "FOTO_GRIL_DEPAN_KOTOR", "item_photo", "cuc_gril_depan_kotor_0", 0),
        ColumnRule(279, "CATATAN_GRIL_DEPAN_KOTOR", "item_note", "cuc_gril_depan_kotor_0", 0),
        ColumnRule(280, "CUCIAN_EXTERIOR_TIDAK_BERSIH_POINT", "item_point", "cuc_exterior_tidak_bersih", 0),
        ColumnRule(281, "FOTO_CUCIAN_EXTERIOR_TIDAK_BERSIH_1", "item_photo", "cuc_exterior_tidak_bersih_0", 0),
        ColumnRule(282, "CATATAN_CUCIAN_EXTERIOR_TIDAK_BERSIH_1", "item_note", "cuc_exterior_tidak_bersih_0", 0),
        ColumnRule(283, "FOTO_CUCIAN_EXTERIOR_TIDAK_BERSIH_2", "item_photo", "cuc_exterior_tidak_bersih_1", 1),
        ColumnRule(284, "CATATAN_CUCIAN_EXTERIOR_TIDAK_BERSIH_2", "item_note", "cuc_exterior_tidak_bersih_1", 1),
        ColumnRule(285, "FOTO_CUCIAN_EXTERIOR_TIDAK_BERSIH_3", "item_photo", "cuc_exterior_tidak_bersih_2", 2),
        ColumnRule(286, "CATATAN_CUCIAN_EXTERIOR_TIDAK_BERSIH_3", "item_note", "cuc_exterior_tidak_bersih_2", 2),
        ColumnRule(287, "FOTO_CUCIAN_EXTERIOR_TIDAK_BERSIH_4", "item_photo", "cuc_exterior_tidak_bersih_3", 3),
        ColumnRule(288, "CATATAN_CUCIAN_EXTERIOR_TIDAK_BERSIH_4", "item_note", "cuc_exterior_tidak_bersih_3", 3),
        ColumnRule(289, "FOTO_CUCIAN_EXTERIOR_TIDAK_BERSIH_5", "item_photo", "cuc_exterior_tidak_bersih_4", 4),
        ColumnRule(290, "CATATAN_CUCIAN_EXTERIOR_TIDAK_BERSIH_5", "item_note", "cuc_exterior_tidak_bersih_4", 4),
        ColumnRule(291, "VAKUM_TIDAK_BERSIH_POINT", "item_point", "cuc_vakum_tidak_bersih", 0),
        ColumnRule(292, "FOTO_VAKUM_TIDAK_BERSIH_1", "item_photo", "cuc_vakum_tidak_bersih_0", 0),
        ColumnRule(293, "CATATAN_VAKUM_TIDAK_BERSIH_1", "item_note", "cuc_vakum_tidak_bersih_0", 0),
        ColumnRule(294, "FOTO_VAKUM_TIDAK_BERSIH_2", "item_photo", "cuc_vakum_tidak_bersih_1", 1),
        ColumnRule(295, "CATATAN_VAKUM_TIDAK_BERSIH_2", "item_note", "cuc_vakum_tidak_bersih_1", 1),
        ColumnRule(296, "FOTO_VAKUM_TIDAK_BERSIH_3", "item_photo", "cuc_vakum_tidak_bersih_2", 2),
        ColumnRule(297, "CATATAN_VAKUM_TIDAK_BERSIH_3", "item_note", "cuc_vakum_tidak_bersih_2", 2),
        ColumnRule(298, "FOTO_VAKUM_TIDAK_BERSIH_4", "item_photo", "cuc_vakum_tidak_bersih_3", 3),
        ColumnRule(299, "CATATAN_VAKUM_TIDAK_BERSIH_4", "item_note", "cuc_vakum_tidak_bersih_3", 3),
        ColumnRule(300, "FOTO_VAKUM_TIDAK_BERSIH_5", "item_photo", "cuc_vakum_tidak_bersih_4", 4),
        ColumnRule(301, "CATATAN_VAKUM_TIDAK_BERSIH_5", "item_note", "cuc_vakum_tidak_bersih_4", 4),
        ColumnRule(302, "PENGELAPAN_INTERIOR_POINT", "item_point", "cuc_pengelapan_interior_tidak_bersih", 0),
        ColumnRule(303, "FOTO_PENGELAPAN_INTERIOR_1", "item_photo", "cuc_pengelapan_interior_tidak_bersih_0", 0),
        ColumnRule(304, "CATATAN_PENGELAPAN_INTERIOR_1", "item_note", "cuc_pengelapan_interior_tidak_bersih_0", 0),
        ColumnRule(305, "FOTO_PENGELAPAN_INTERIOR_2", "item_photo", "cuc_pengelapan_interior_tidak_bersih_1", 1),
        ColumnRule(306, "CATATAN_PENGELAPAN_INTERIOR_2", "item_note", "cuc_pengelapan_interior_tidak_bersih_1", 1),
        ColumnRule(307, "FOTO_PENGELAPAN_INTERIOR_3", "item_photo", "cuc_pengelapan_interior_tidak_bersih_2", 2),
        ColumnRule(308, "CATATAN_PENGELAPAN_INTERIOR_3", "item_note", "cuc_pengelapan_interior_tidak_bersih_2", 2),
        ColumnRule(309, "FOTO_PENGELAPAN_INTERIOR_4", "item_photo", "cuc_pengelapan_interior_tidak_bersih_3", 3),
        ColumnRule(310, "CATATAN_PENGELAPAN_INTERIOR_4", "item_note", "cuc_pengelapan_interior_tidak_bersih_3", 3),
        ColumnRule(311, "CUCIAN_RUANG_MESIN_POINT", "item_point", "cuc_ruang_mesin", 0),
        ColumnRule(312, "FOTO_CUCIAN_RUANG_MESIN", "item_photo", "cuc_ruang_mesin_0", 0),
        ColumnRule(313, "CATATAN_CUCIAN_RUANG_MESIN", "item_note", "cuc_ruang_mesin_0", 0),
        ColumnRule(314, "KEBERSIHAN_ENGESEL-ENGSEL_POINT", "item_point", "cuc_kebersihan_engsel", 0),
        ColumnRule(315, "FOTO_KEBERSIHAN_ENGESEL-ENGSEL_1", "item_photo", "cuc_kebersihan_engsel_0", 0),
        ColumnRule(316, "CATATAN_KEBERSIHAN_ENGESEL-ENGSEL_1", "item_note", "cuc_kebersihan_engsel_0", 0),
        ColumnRule(317, "FOTO_KEBERSIHAN_ENGESEL-ENGSEL_2", "item_photo", "cuc_kebersihan_engsel_1", 1),
        ColumnRule(318, "CATATAN_KEBERSIHAN_ENGESEL-ENGSEL_2", "item_note", "cuc_kebersihan_engsel_1", 1),
        ColumnRule(319, "FOTO_KEBERSIHAN_ENGESEL-ENGSEL_3", "item_photo", "cuc_kebersihan_engsel_2", 2),
        ColumnRule(320, "CATATAN_KEBERSIHAN_ENGESEL-ENGSEL_3", "item_note", "cuc_kebersihan_engsel_2", 2),
        ColumnRule(321, "FOTO_KEBERSIHAN_ENGESEL-ENGSEL_4", "item_photo", "cuc_kebersihan_engsel_3", 3),
        ColumnRule(322, "CATATAN_KEBERSIHAN_ENGESEL-ENGSEL_4", "item_note", "cuc_kebersihan_engsel_3", 3),
        ColumnRule(323, "FOTO_KEBERSIHAN_ENGESEL-ENGSEL_5", "item_photo", "cuc_kebersihan_engsel_4", 4),
        ColumnRule(324, "CATATAN_KEBERSIHAN_ENGESEL-ENGSEL_5", "item_note", "cuc_kebersihan_engsel_4", 4),
    )


    fun isValidPhotoUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val trimmed = url.trim()
        return trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("content://") || trimmed.startsWith("/")
    }

    /**
     * Mengekstrak nilai waktu/timestamp dari baris Google Sheets.
     * Mengutamakan kolom ke-3 atau kolom dengan header "WAKTU/TIMESTAMP".
     */
    fun extractWaktuTimestampFromSheetRow(obj: JSONObject): String {
        val candidates = listOf(
            "WAKTU/TIMESTAMP",
            "WAKTU / TIMESTAMP",
            "waktu/timestamp",
            "WAKTU_TIMESTAMP",
            "waktu_timestamp",
            "KOLOM_3",
            "kolom_3",
            "kolom3",
            "_col_3",
            "col_3",
            "TIMESTAMP",
            "timestamp",
            "WAKTU",
            "waktu"
        )
        for (key in candidates) {
            if (obj.has(key) && !obj.isNull(key)) {
                val v = obj.optString(key, "").trim()
                if (v.isNotEmpty()) return v
            }
        }

        // Cek secara case-insensitive & abaikan tanda baca
        val iter = obj.keys()
        var keyIndex = 0
        var thirdColValue = ""
        while (iter.hasNext()) {
            val k = iter.next()
            val cleanKey = k.lowercase().replace("[^a-z0-9]".toRegex(), "")
            val v = obj.optString(k, "").trim()
            if (cleanKey == "waktutimestamp" || cleanKey == "timestamp" || cleanKey == "waktu" || cleanKey == "kolom3" || cleanKey == "col3") {
                if (v.isNotEmpty()) return v
            }
            if (keyIndex == 2 && v.isNotEmpty()) {
                thirdColValue = v
            }
            keyIndex++
        }

        if (thirdColValue.isNotEmpty()) {
            return thirdColValue
        }

        return ""
    }

    fun parseSheetRowToAudit(obj: JSONObject, existing: DataAuditEntity? = null): DataAuditEntity {
        var noMobil = obj.optString("NO_KENDARAAN").ifBlank { obj.optString("NO_MOBIL") }.uppercase().trim()
        val platNomer = obj.optString("PLAT_NOMER").ifBlank { obj.optString("PLAT_NOMOR") }.uppercase().trim()
        if (noMobil.isBlank() && platNomer.isNotBlank()) {
            noMobil = platNomer
        }

        val sheetId = obj.optLong("ID", existing?.id ?: 0L)
        val tanggal = obj.optString("TANGGAL").ifBlank { existing?.tanggal ?: "" }
        val waktu = extractWaktuTimestampFromSheetRow(obj)

        val parsedTimestamp = com.example.util.DateUtils.parseToMillis(waktu, tanggal)
            ?: (if (existing != null && existing.timestamp > 0L) existing.timestamp else com.example.util.DateUtils.parseToMillis(tanggal, ""))
            ?: System.currentTimeMillis()

        val finalTanggal = if (waktu.isNotBlank()) {
            waktu
        } else if (tanggal.isNotBlank()) {
            tanggal
        } else {
            existing?.tanggal ?: com.example.util.DateUtils.formatTimestampWithSeconds(parsedTimestamp)
        }
        val periode = obj.optString("PERIODE").ifBlank { existing?.periode ?: "" }
        val nip = obj.optString("NIP").ifBlank { existing?.nip ?: "" }
        val namaAuditor = obj.optString("NAMA_AUDITOR").ifBlank { obj.optString("NAMA_PETUGAS").ifBlank { existing?.namaPetugas ?: "" } }
        val tipe = obj.optString("TIPE").ifBlank { existing?.tipe ?: "" }
        val warna = obj.optString("WARNA").ifBlank { existing?.warna ?: "" }
        val tahun = obj.optString("TAHUN").ifBlank { existing?.tahun ?: "" }
        val pool = obj.optString("POOL").ifBlank { existing?.pool ?: "" }
        val jenisSample = obj.optString("JENIS_SAMPLE").ifBlank { existing?.jenisSample ?: "" }
        val tanggalService = obj.optString("TANGGAL_SERVICE").ifBlank {
            obj.optString("SAMPLE_TANGGAL").ifBlank {
                obj.optString("SAMPLE TANGGAL").ifBlank { existing?.tanggalService ?: (existing?.sampleTanggal ?: "") }
            }
        }
        val sampleTanggal = obj.optString("SAMPLE_TANGGAL").ifBlank {
            obj.optString("SAMPLE TANGGAL").ifBlank {
                if (tanggalService.isNotBlank()) tanggalService else (existing?.sampleTanggal ?: "")
            }
        }
        val paketService = obj.optString("PEKET_SERVICE").ifBlank { obj.optString("PAKET_SERVICE").ifBlank { obj.optString("SERVICE").ifBlank { existing?.paketService ?: (existing?.service ?: "") } } }
        val totalPoin = when {
            obj.has("TOTAL_POIN_TEMUAN") -> obj.optInt("TOTAL_POIN_TEMUAN", 0)
            obj.has("POINT") -> obj.optInt("POINT", 0)
            obj.has("POIN") -> obj.optInt("POIN", 0)
            obj.has("TOTAL_POIN") -> obj.optInt("TOTAL_POIN", 0)
            obj.has("TOTAL_POINT") -> obj.optInt("TOTAL_POINT", 0)
            obj.has("TOTAL POIN") -> obj.optInt("TOTAL POIN", 0)
            obj.has("TOTAL POINT") -> obj.optInt("TOTAL POINT", 0)
            obj.has("TOTAL POIN TEMUAN") -> obj.optInt("TOTAL POIN TEMUAN", 0)
            else -> existing?.totalPoinTemuan ?: 0
        }

        val kpiScores = mutableMapOf<String, Int>()
        val kpiPhotos = mutableMapOf<String, String>()
        val kpiNotes = mutableMapOf<String, String>()

        // Fallback dari RAW_JSON / kpiScoresJson jika ada
        val rawJsonStr = obj.optString("RAW_JSON").ifBlank { obj.optString("kpiScoresJson") }
        if (rawJsonStr.isNotBlank() && rawJsonStr.startsWith("{")) {
            try {
                val rawObj = JSONObject(rawJsonStr)
                val scoresObj = if (rawObj.has("kpiScores")) rawObj.getJSONObject("kpiScores") else rawObj
                val sKeys = scoresObj.keys()
                while (sKeys.hasNext()) {
                    val k = sKeys.next()
                    kpiScores[k] = scoresObj.optInt(k, 0)
                }
                if (rawObj.has("kpiPhotos")) {
                    val pObj = rawObj.getJSONObject("kpiPhotos")
                    val pKeys = pObj.keys()
                    while (pKeys.hasNext()) {
                        val pk = pKeys.next()
                        val pv = pObj.optString(pk, "")
                        if (pv.isNotBlank()) kpiPhotos[pk] = pv
                    }
                }
                if (rawObj.has("kpiNotes")) {
                    val nObj = rawObj.getJSONObject("kpiNotes")
                    val nKeys = nObj.keys()
                    while (nKeys.hasNext()) {
                        val nk = nKeys.next()
                        val nv = nObj.optString(nk, "")
                        if (nv.isNotBlank()) kpiNotes[nk] = nv
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        // Ekstraksi 7 Foto Kendaraan
        val vMap = mapOf(
            "FOTO_KENDARAAN_DEPAN" to "kendaraan_depan",
            "FOTO_KENDARAAN_SAMPING_KIRI" to "kendaraan_samping_kiri",
            "FOTO_KENDARAAN_INTERIOR_DEPAN_KIRI" to "kendaraan_interior_depan_kiri",
            "FOTO_KENDARAAN_INTERIOR_BELAKANG_KIRI" to "kendaraan_interior_belakang_kiri",
            "FOTO_KENDARAAN_BELAKANG" to "kendaraan_belakang",
            "FOTO_KENDARAAN_INTERIOR_BELAKANG" to "kendaraan_interior_belakang",
            "FOTO_KENDARAAN_SAMPING_KANAN" to "kendaraan_samping_kanan"
        )
        for ((col, k) in vMap) {
            val vUrl = obj.optString(col).trim()
            if (isValidPhotoUrl(vUrl)) {
                kpiPhotos[k] = vUrl
            }
        }

        // Ekstraksi kolom KPI Points, Photos, & Notes dari 325 kolom
        for (rule in RULES) {
            when (rule.type) {
                "item_point" -> {
                    if (obj.has(rule.headerName)) {
                        val pt = obj.optInt(rule.headerName, 0)
                        kpiScores[rule.key] = pt
                    }
                }
                "item_photo" -> {
                    val pUrl = obj.optString(rule.headerName).trim()
                    if (isValidPhotoUrl(pUrl)) {
                        kpiPhotos[rule.key] = pUrl
                    }
                }
                "item_note" -> {
                    val nVal = obj.optString(rule.headerName).trim()
                    if (nVal.isNotBlank() && !nVal.startsWith("Upload Error", ignoreCase = true)) {
                        kpiNotes[rule.key] = nVal
                    }
                }
            }
        }

        // Fallback untuk baris spreadsheet berformat lama (PHOTO_1 s/d PHOTO_20 & CATATAN_1 s/d CATATAN_20)
        val legacyVehicleKeys = listOf(
            "kendaraan_depan", "kendaraan_samping_kiri", "kendaraan_interior_depan_kiri",
            "kendaraan_interior_belakang_kiri", "kendaraan_belakang", "kendaraan_interior_belakang",
            "kendaraan_samping_kanan"
        )
        for (i in 1..20) {
            val pOld = obj.optString("PHOTO_$i").ifBlank { obj.optString("photo$i") }.ifBlank { obj.optString("FOTO_$i") }.trim()
            val nOld = obj.optString("CATATAN_$i").ifBlank { obj.optString("catatanPhoto$i") }.ifBlank { obj.optString("NOTE_$i") }.trim()
            if (isValidPhotoUrl(pOld)) {
                if (i <= 7) {
                    val vk = legacyVehicleKeys[i - 1]
                    if (!kpiPhotos.containsKey(vk)) {
                        kpiPhotos[vk] = pOld
                        if (nOld.isNotBlank() && !kpiNotes.containsKey(vk) && !nOld.startsWith("Upload Error", ignoreCase = true)) {
                            kpiNotes[vk] = nOld
                        }
                    }
                }
            }
        }

        // Jika point pada item bernilai 0, pastikan foto dan catatan temuan terkait dibersihkan
        for ((itemId, pt) in kpiScores) {
            if (pt == 0) {
                val pKeys = kpiPhotos.keys.filter { it == itemId || it.startsWith("${itemId}_") }
                pKeys.forEach { kpiPhotos.remove(it) }
                val nKeys = kpiNotes.keys.filter { it == itemId || it.startsWith("${itemId}_") }
                nKeys.forEach { kpiNotes.remove(it) }
            }
        }

        // Susun legacy photo1..photo20
        val legacyPhotos = MutableList(20) { "" }
        val legacyNotes = MutableList(20) { "" }
        val vehicleKeys = listOf(
            "kendaraan_depan" to "Foto Kendaraan - Depan",
            "kendaraan_samping_kiri" to "Foto Kendaraan - Samping Kiri",
            "kendaraan_interior_depan_kiri" to "Foto Kendaraan - Interior Depan Kiri",
            "kendaraan_interior_belakang_kiri" to "Foto Kendaraan - Interior Belakang Kiri",
            "kendaraan_belakang" to "Foto Kendaraan - Belakang",
            "kendaraan_interior_belakang" to "Foto Kendaraan - Interior Belakang",
            "kendaraan_samping_kanan" to "Foto Kendaraan - Samping Kanan"
        )
        vehicleKeys.forEachIndexed { vIdx, (vKey, vTitle) ->
            kpiPhotos[vKey]?.let { legacyPhotos[vIdx] = it }
            legacyNotes[vIdx] = kpiNotes[vKey]?.ifBlank { vTitle } ?: vTitle
        }

        var findingIdx = 7
        for ((k, pPath) in kpiPhotos) {
            if (!k.startsWith("kendaraan_") && findingIdx < 20) {
                legacyPhotos[findingIdx] = pPath
                legacyNotes[findingIdx] = kpiNotes[k] ?: ""
                findingIdx++
            }
        }

        val finalTotalPoin = if (totalPoin > 0) totalPoin else kpiScores.values.sum()

        val extractedUuid = obj.optString("AUDIT_UUID").ifBlank {
            obj.optString("auditUuid").ifBlank {
                try {
                    val rawObj = JSONObject(rawJsonStr)
                    rawObj.optString("auditUuid").ifBlank { rawObj.optString("uuid") }
                } catch (e: Exception) { "" }
            }
        }

        val rawIdStr = obj.optString("ID").ifBlank { obj.optString("id") }.trim()
        val extractedKodeUnik = if (rawIdStr.isNotBlank()) {
            rawIdStr
        } else if (existing?.kodeUnik?.isNotBlank() == true) {
            existing.kodeUnik
        } else if (extractedUuid.isNotBlank() && extractedUuid.startsWith("AUD-")) {
            extractedUuid
        } else {
            UniqueCodeGenerator.generateAuditCode()
        }

        return DataAuditEntity(
            id = existing?.id ?: 0L,
            cloudId = if (sheetId > 0L) sheetId else (existing?.cloudId ?: 0L),
            auditUuid = existing?.auditUuid?.ifBlank { null } ?: extractedUuid.ifBlank { extractedKodeUnik },
            kodeUnik = extractedKodeUnik,
            timestamp = parsedTimestamp,
            tanggal = finalTanggal,
            periode = periode,
            nip = nip,
            namaPetugas = namaAuditor,
            noMobil = noMobil,
            platNomer = platNomer,
            tipe = tipe,
            warna = warna,
            tahun = tahun,
            tanggalService = tanggalService,
            service = paketService,
            pool = pool,
            totalPoinTemuan = finalTotalPoin,
            jenisSample = jenisSample,
            sampleTanggal = sampleTanggal,
            paketService = paketService,
            kpiScoresJson = JSONObject(kpiScores as Map<*, *>).toString(),
            kpiPhotosJson = JSONObject(kpiPhotos as Map<*, *>).toString(),
            kpiNotesJson = JSONObject(kpiNotes as Map<*, *>).toString(),
            photo1 = legacyPhotos[0], catatanPhoto1 = legacyNotes[0],
            photo2 = legacyPhotos[1], catatanPhoto2 = legacyNotes[1],
            photo3 = legacyPhotos[2], catatanPhoto3 = legacyNotes[2],
            photo4 = legacyPhotos[3], catatanPhoto4 = legacyNotes[3],
            photo5 = legacyPhotos[4], catatanPhoto5 = legacyNotes[4],
            photo6 = legacyPhotos[5], catatanPhoto6 = legacyNotes[5],
            photo7 = legacyPhotos[6], catatanPhoto7 = legacyNotes[6],
            photo8 = legacyPhotos[7], catatanPhoto8 = legacyNotes[7],
            photo9 = legacyPhotos[8], catatanPhoto9 = legacyNotes[8],
            photo10 = legacyPhotos[9], catatanPhoto10 = legacyNotes[9],
            photo11 = legacyPhotos[10], catatanPhoto11 = legacyNotes[10],
            photo12 = legacyPhotos[11], catatanPhoto12 = legacyNotes[11],
            photo13 = legacyPhotos[12], catatanPhoto13 = legacyNotes[12],
            photo14 = legacyPhotos[13], catatanPhoto14 = legacyNotes[13],
            photo15 = legacyPhotos[14], catatanPhoto15 = legacyNotes[14],
            photo16 = legacyPhotos[15], catatanPhoto16 = legacyNotes[15],
            photo17 = legacyPhotos[16], catatanPhoto17 = legacyNotes[16],
            photo18 = legacyPhotos[17], catatanPhoto18 = legacyNotes[17],
            photo19 = legacyPhotos[18], catatanPhoto19 = legacyNotes[18],
            photo20 = legacyPhotos[19], catatanPhoto20 = legacyNotes[19],
            isSynced = true,
            syncTimestamp = System.currentTimeMillis()
        )
    }
}

