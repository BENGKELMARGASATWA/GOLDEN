package com.example.data.model

data class KpiItem(
    val id: String,
    val name: String,
    val maxPoints: Int,
    val defaultBobot: Double = 1.0,
    val photoLabels: List<String> = emptyList()
) {
    fun getLabelForPhoto(index: Int): String {
        return if (photoLabels.isNotEmpty() && index < photoLabels.size) {
            photoLabels[index]
        } else if (maxPoints <= 1) {
            "Photo $name"
        } else {
            "Photo $name ${index + 1}"
        }
    }
}

data class KpiGroup(
    val id: String,
    val title: String,
    val items: List<KpiItem>
)

data class KpiSection(
    val id: String,
    val title: String,
    val groups: List<KpiGroup>
)

object KpiAuditSpec {
    val sections: List<KpiSection> = listOf(
        // SECTION A: EXTERIOR
        KpiSection(
            id = "EXTERIOR",
            title = "A. REKAP TEMUAN KPI AUDIT EXTERIOR",
            groups = listOf(
                KpiGroup(
                    id = "EXT_BODY",
                    title = "1. BODY",
                    items = listOf(
                        KpiItem(
                            id = "body_keropos",
                            name = "Keropos",
                            maxPoints = 1,
                            defaultBobot = 30.0,
                            photoLabels = listOf("Photo keropos")
                        ),
                        KpiItem(
                            id = "body_penyok",
                            name = "PENYOK",
                            maxPoints = 1,
                            defaultBobot = 25.0,
                            photoLabels = listOf("Photo penyok")
                        ),
                        KpiItem(
                            id = "body_dekok",
                            name = "DEKOK",
                            maxPoints = 5,
                            defaultBobot = 4.0,
                            photoLabels = listOf("Photo Dekok 1", "Photo Dekok 2", "Photo Dekok 3", "Photo Dekok 4", "Photo Dekok 5")
                        ),
                        KpiItem(
                            id = "body_baret_halus",
                            name = "BARET HALUS",
                            maxPoints = 5,
                            defaultBobot = 2.0,
                            photoLabels = listOf("Photo Baret Halus 1", "Photo Baret Halus 2", "Photo Baret Halus 3", "Photo Baret Halus 4", "Photo Baret Halus 5")
                        ),
                        KpiItem(
                            id = "body_baret",
                            name = "BARET",
                            maxPoints = 5,
                            defaultBobot = 2.0,
                            photoLabels = listOf("Photo Baret 1", "Photo Baret 2", "Photo Baret 3", "Photo Baret 4", "Photo Baret 5")
                        ),
                        KpiItem(
                            id = "body_jamur",
                            name = "JAMUR BODY",
                            maxPoints = 5,
                            defaultBobot = 1.0,
                            photoLabels = listOf("Photo Jamur body 1", "Photo Jamur body 2", "Photo Jamur Body 3", "Photo Jamur Body 4", "Photo Jamur Body 5")
                        ),
                        KpiItem(
                            id = "body_bumper_rengang",
                            name = "BUMPER RENGANG",
                            maxPoints = 2,
                            defaultBobot = 2.5,
                            photoLabels = listOf("Photo bumper rengang 1", "Photo bumper rengang 2")
                        ),
                        KpiItem(
                            id = "body_cover_spion",
                            name = "COVER SPION Retak/Kusam",
                            maxPoints = 2,
                            defaultBobot = 2.5,
                            photoLabels = listOf("Photo cover spion 1", "Photo cover spion 2")
                        )
                    )
                ),
                KpiGroup(
                    id = "EXT_CAT",
                    title = "2. CAT",
                    items = listOf(
                        KpiItem(
                            id = "cat_belang",
                            name = "BELANG",
                            maxPoints = 5,
                            defaultBobot = 6.0,
                            photoLabels = listOf("Photo Belang 1", "Photo Belang 2", "Photo Belang 3", "Photo Belang 4", "Photo Belang 5")
                        ),
                        KpiItem(
                            id = "cat_kusam",
                            name = "KUSAM",
                            maxPoints = 5,
                            defaultBobot = 5.0,
                            photoLabels = listOf("Photo kusam 1", "Photo kusam 2", "Photo Kusam 3", "Photo Kusam 4", "Photo Kusam 5")
                        ),
                        KpiItem(
                            id = "cat_gugus",
                            name = "GUGUS",
                            maxPoints = 5,
                            defaultBobot = 5.0,
                            photoLabels = listOf("Photo gugus 1", "Photo Gugus 2", "Photo Gugus 3", "Photo Gugus 4", "Photo Gugus 5")
                        ),
                        KpiItem(
                            id = "cat_rusak_lainya",
                            name = "RUSAK LAINYA CAT",
                            maxPoints = 5,
                            defaultBobot = 4.0,
                            photoLabels = listOf("Photo rusak lainya 1", "Photo Rusak lainya 2", "Photo Rusak lainya 3", "Photo Rusak lainya 4", "Photo Rusak lainya 5")
                        )
                    )
                ),
                KpiGroup(
                    id = "EXT_KACA",
                    title = "3. KACA",
                    items = listOf(
                        KpiItem(
                            id = "kaca_depan_berjamur",
                            name = "Kaca Depan Berjamur",
                            maxPoints = 1,
                            defaultBobot = 7.5,
                            photoLabels = listOf("Photo Kaca depan Berjamur 1")
                        ),
                        KpiItem(
                            id = "kaca_depan_pelangi",
                            name = "Kaca Depan Pelangi",
                            maxPoints = 1,
                            defaultBobot = 10.0,
                            photoLabels = listOf("Photo Depan Pelangi 1")
                        ),
                        KpiItem(
                            id = "kaca_depan_baret",
                            name = "Kaca Depan Baret",
                            maxPoints = 1,
                            defaultBobot = 12.5,
                            photoLabels = listOf("Photo Kaca Depan Baret 1")
                        ),
                        KpiItem(
                            id = "kaca_depan_titik_retak",
                            name = "Kaca Depan Titik/Retak",
                            maxPoints = 1,
                            defaultBobot = 15.0,
                            photoLabels = listOf("Photo Depan Titik/Retak 1")
                        ),
                        KpiItem(
                            id = "kaca_depan_samping_berjamur",
                            name = "Kaca Samping Berjamur",
                            maxPoints = 2,
                            defaultBobot = 5.0,
                            photoLabels = listOf("Photo Kaca Samping Berjamur 1", "Photo Kaca Samping Berjamur 2")
                        ),
                        KpiItem(
                            id = "kaca_depan_samping_baret",
                            name = "Kaca Samping Baret",
                            maxPoints = 2,
                            defaultBobot = 10.0,
                            photoLabels = listOf("Photo Kaca Samping Baret 1", "Photo Kaca Samping Baret 2")
                        ),
                        KpiItem(
                            id = "kaca_belakang_berjamur",
                            name = "Kaca Belakang Berjamur",
                            maxPoints = 1,
                            defaultBobot = 4.0,
                            photoLabels = listOf("Photo Belakang Berjamur 1")
                        ),
                        KpiItem(
                            id = "kaca_belakang_baret",
                            name = "Kaca Belakang Baret",
                            maxPoints = 1,
                            defaultBobot = 6.0,
                            photoLabels = listOf("Photo Belakang Baret 1")
                        ),
                        KpiItem(
                            id = "kaca_spion_berjamur",
                            name = "Kaca Spion Berjamur",
                            maxPoints = 2,
                            defaultBobot = 7.5,
                            photoLabels = listOf("Photo Spion Berjamur 1", "Photo Spion Berjamur 2")
                        )
                    )
                ),
                KpiGroup(
                    id = "EXT_LAMPU",
                    title = "4. HEADLAMP & STOPLAMP",
                    items = listOf(
                        KpiItem(
                            id = "headlamp_rusak_retak_pecah",
                            name = "Headlamp Rusak/Retak/Pecah",
                            maxPoints = 2,
                            defaultBobot = 15.0,
                            photoLabels = listOf("Photo Headlamp Rusak/Retak/Pecah 1", "Photo Headlamp Rusak/Retak/Pecah 2")
                        ),
                        KpiItem(
                            id = "headlamp_kusam_buram",
                            name = "Headlamp Kusam/Buram",
                            maxPoints = 2,
                            defaultBobot = 10.0,
                            photoLabels = listOf("Photo Headlamp Kusam/Buram 1", "Photo Headlamp Kusam/Buram 2")
                        ),
                        KpiItem(
                            id = "headlamp_berembun",
                            name = "Headlamp Berembun",
                            maxPoints = 2,
                            defaultBobot = 5.0,
                            photoLabels = listOf("Photo Headlamp Berembun 1", "Photo Headlamp Berembun 2")
                        ),
                        KpiItem(
                            id = "stoplamp_rusak_retak_pecah",
                            name = "Stoplamp Rusak/Retak/Pecah",
                            maxPoints = 2,
                            defaultBobot = 10.0,
                            photoLabels = listOf("Photo Stoplamp Rusak/Retak/Pecah 1", "Photo Stoplamp Rusak/Retak/Pecah 2")
                        ),
                        KpiItem(
                            id = "stoplamp_kusam_berembun",
                            name = "Stoplamp Kusam/Buram",
                            maxPoints = 2,
                            defaultBobot = 7.5,
                            photoLabels = listOf("Photo Stoplamp Kusam/Buram 1", "Photo Stoplamp Kusam/Buram 2")
                        ),
                        KpiItem(
                            id = "stoplamp_berembun",
                            name = "Stoplamp Berembun",
                            maxPoints = 2,
                            defaultBobot = 2.5,
                            photoLabels = listOf("Photo Stoplamp Berembun 1", "Photo Stoplamp Berembun 2")
                        )
                    )
                )
            )
        ),

        // SECTION B: INTERIOR
        KpiSection(
            id = "INTERIOR",
            title = "B. REKAP TEMUAN KPI INTERIOR",
            groups = listOf(
                KpiGroup(
                    id = "INT_PLAFON_KARPET",
                    title = "1. PLAFON & KARPET",
                    items = listOf(
                        KpiItem(
                            id = "plafon_depan_sunvisor_kotor",
                            name = "Plafon Depan/Sunsvisor kotor/Bercak",
                            maxPoints = 1,
                            defaultBobot = 6.0,
                            photoLabels = listOf("Photo Plafon Depan/Sunsvisor kotor/Bercak")
                        ),
                        KpiItem(
                            id = "plafon_tengah_kotor",
                            name = "Plafon Tengah Kotor/Bercak",
                            maxPoints = 1,
                            defaultBobot = 15.0,
                            photoLabels = listOf("Photo Plafon Tengah Kotor/Bercak")
                        ),
                        KpiItem(
                            id = "plafon_handgrip_rusak",
                            name = "Handgrip Rusak",
                            maxPoints = 1,
                            defaultBobot = 3.0,
                            photoLabels = listOf("Photo Handgrip Rusak")
                        ),
                        KpiItem(
                            id = "plafon_belakang_kotor",
                            name = "Plafon Belakang Kotor/Bercak",
                            maxPoints = 1,
                            defaultBobot = 6.0,
                            photoLabels = listOf("Photo Belakang Kotor/Bercak 1")
                        ),
                        KpiItem(
                            id = "plafon_baret_ringan",
                            name = "Plafon Baret Ringan",
                            maxPoints = 1,
                            defaultBobot = 5.0,
                            photoLabels = listOf("Photo Plafon Baret Ringan")
                        ),
                        KpiItem(
                            id = "plafon_baret_parah",
                            name = "Plafon Baret Parah",
                            maxPoints = 1,
                            defaultBobot = 20.0,
                            photoLabels = listOf("Photo Plafon Baret Parah")
                        ),
                        KpiItem(
                            id = "plafon_brudul",
                            name = "Plafon Brudul",
                            maxPoints = 1,
                            defaultBobot = 15.0,
                            photoLabels = listOf("Photo Plafon Brudul")
                        ),
                        KpiItem(
                            id = "karpet_dasar_sobek",
                            name = "Karpet Dasar Sobek",
                            maxPoints = 1,
                            defaultBobot = 15.0,
                            photoLabels = listOf("Photo Karpet Dasar Sobek")
                        )
                    )
                ),
                KpiGroup(
                    id = "INT_SEAT_DASHBOARD",
                    title = "2. JOK,SEATBELT & DASHBORD",
                    items = listOf(
                        KpiItem(
                            id = "kursi_tamu_retak",
                            name = "Kursi Tamu Retak/Bercak",
                            maxPoints = 1,
                            defaultBobot = 10.0,
                            photoLabels = listOf("Photo Kursi Tamu Retak/Bercak")
                        ),
                        KpiItem(
                            id = "kursi_tamu_sobek",
                            name = "Kursi Tamu Sobek",
                            maxPoints = 1,
                            defaultBobot = 20.0,
                            photoLabels = listOf("Photo Kursi Tamu Sobek")
                        ),
                        KpiItem(
                            id = "kursi_pengemudi_retak",
                            name = "Kursi Pengemudi Retak/Bercak",
                            maxPoints = 1,
                            defaultBobot = 7.5,
                            photoLabels = listOf("Photo Kursi Pengemudi Retak/Bercak")
                        ),
                        KpiItem(
                            id = "kursi_pengemudi_sobek",
                            name = "Kursi Pengemudi Sobek",
                            maxPoints = 1,
                            defaultBobot = 12.5,
                            photoLabels = listOf("Photo Kursi Pengemudi Sobek")
                        ),
                        KpiItem(
                            id = "safety_belt_kotor",
                            name = "Safety Belt Kotor",
                            maxPoints = 1,
                            defaultBobot = 10.0,
                            photoLabels = listOf("Photo Safety Belt Kotor")
                        ),
                        KpiItem(
                            id = "safety_belt_rusak",
                            name = "Safety Belt Rusak",
                            maxPoints = 1,
                            defaultBobot = 15.0,
                            photoLabels = listOf("Photo Safety Belt Rusak")
                        ),
                        KpiItem(
                            id = "sarung_verseneling_rusak",
                            name = "Sarung Verseneling Rusak",
                            maxPoints = 1,
                            defaultBobot = 5.0,
                            photoLabels = listOf("Photo Sarung Verseneling Rusak")
                        ),
                        KpiItem(
                            id = "sarung_steer_rusak",
                            name = "Sarung Steer Rusak",
                            maxPoints = 1,
                            defaultBobot = 10.0,
                            photoLabels = listOf("Photo Sarung Steer Rusak")
                        ),
                        KpiItem(
                            id = "dashboard_rusak",
                            name = "Dasboard Rusak/Tidak Rapi",
                            maxPoints = 1,
                            defaultBobot = 10.0,
                            photoLabels = listOf("Photo Dasboard Rusak/Tidak Rapi")
                        )
                    )
                ),
                KpiGroup(
                    id = "INT_ATRIBUT",
                    title = "3. ATRIBUT",
                    items = listOf(
                        KpiItem(
                            id = "kaca_film",
                            name = "Kaca Film",
                            maxPoints = 1,
                            defaultBobot = 30.0,
                            photoLabels = listOf("Photo Kaca Film")
                        ),
                        KpiItem(
                            id = "perangkap_kecoa",
                            name = "Perangkap Kecoa Tidak Ada",
                            maxPoints = 1,
                            defaultBobot = 15.0,
                            photoLabels = listOf("Photo Perangkap Kecoa Tidak Ada")
                        ),
                    )
                ),
                KpiGroup(
                    id = "INT_KECOA_BAU",
                    title = "4. KECOA & BAU",
                    items = listOf(
                        KpiItem(
                            id = "ada_kecoa_kutu",
                            name = "Ada Kecoa,Kutu Busuk",
                            maxPoints = 1,
                            defaultBobot = 50.0,
                            photoLabels = listOf("Photo Ada Kecoa,Kutu Busuk")
                        ),
                        KpiItem(
                            id = "bau_interior_cabin",
                            name = "Bau Interior Cabin",
                            maxPoints = 1,
                            defaultBobot = 50.0,
                            photoLabels = listOf("Photo Bau Interior Cabin")
                        )
                    )
                ),
                KpiGroup(
                    id = "INT_EXS_SERVICE",
                    title = "5. EXS SERVICE",
                    items = listOf(
                        KpiItem(
                            id = "pelumas_engsel",
                            name = "Pelumasan Engsel",
                            maxPoints = 6,
                            defaultBobot = 5.0,
                            photoLabels = listOf(
                                "Photo Pelumasan Engsel 1",
                                "Photo Pelumasan Engsel 2",
                                "Photo Pelumasan Engsel 3",
                                "Photo Pelumasan Engsel 4",
                                "Photo Pelumasan Engsel 5",
                                "Photo Pelumasan Engsel 6"
                            )
                        ),
                        KpiItem(
                            id = "kondisi_ruang_mesin",
                            name = "Kondisi Ruang mesin",
                            maxPoints = 1,
                            defaultBobot = 35.0,
                            photoLabels = listOf("Photo Kondisi Ruang mesin")
                        ),
                        KpiItem(
                            id = "kondisi_kap_mesin_dalam",
                            name = "Kondisi Kap Mesin Dalam",
                            maxPoints = 1,
                            defaultBobot = 20.0,
                            photoLabels = listOf("Photo Kondisi Kap Mesin Dalam")
                        ),
                        KpiItem(
                            id = "kondisi_tutup_bbm",
                            name = "Kondisi Tutup BBM",
                            maxPoints = 1,
                            defaultBobot = 15.0,
                            photoLabels = listOf("Photo Kondisi Tutup BBM")
                        )
                    )
                )
            )
        ),

        // SECTION C: CUCIAN
        KpiSection(
            id = "CUCIAN",
            title = "C. CUCIAN",
            groups = listOf(
                KpiGroup(
                    id = "CUC_EXTERIOR",
                    title = "1. CUCIAN EXTERIOR",
                    items = listOf(
                        KpiItem(
                            id = "cuc_gril_depan_kotor",
                            name = "Gril Depan Kotor",
                            maxPoints = 1,
                            defaultBobot = 25.0,
                            photoLabels = listOf("Photo Gril Depan Kotor")
                        ),
                        KpiItem(
                            id = "cuc_exterior_tidak_bersih",
                            name = "CUCIAN Exterior Tidak Bersih",
                            maxPoints = 5,
                            defaultBobot = 15.0,
                            photoLabels = listOf(
                                "Photo CUCIAN Exterior Tidak Bersih 1",
                                "Photo CUCIAN Exterior Tidak Bersih 2",
                                "Photo CUCIAN Exterior Tidak Bersih 3",
                                "Photo CUCIAN Exterior Tidak Bersih 4",
                                "Photo CUCIAN Exterior Tidak Bersih 5"
                            )
                        )
                    )
                ),
                KpiGroup(
                    id = "CUC_INTERIOR",
                    title = "2. CUCIAN INTERIOR",
                    items = listOf(
                        KpiItem(
                            id = "cuc_vakum_tidak_bersih",
                            name = "Vakum Tidak Bersih",
                            maxPoints = 5,
                            defaultBobot = 8.0,
                            photoLabels = listOf(
                                "Photo Vakum Tidak Bersih 1",
                                "Photo Vakum Tidak Bersih 2",
                                "Photo Vakum Tidak Bersih 3",
                                "Photo Vakum Tidak Bersih 4",
                                "Photo Vakum Tidak Bersih 5"
                            )
                        ),
                        KpiItem(
                            id = "cuc_pengelapan_interior_tidak_bersih",
                            name = "Pengelapan Interior Tidak Bersih",
                            maxPoints = 4,
                            defaultBobot = 15.0,
                            photoLabels = listOf(
                                "Photo Pengelapan Interior Tidak Bersih 1",
                                "Photo Pengelapan Interior Tidak Bersih 2",
                                "Photo Pengelapan Interior Tidak Bersih 3",
                                "Photo Pengelapan Interior Tidak Bersih 4"
                            )
                        )
                    )
                ),
                KpiGroup(
                    id = "CUC_LENGKAP",
                    title = "3. CUCI LENGKAP",
                    items = listOf(
                        KpiItem(
                            id = "cuc_ruang_mesin",
                            name = "Cucian Ruang Mesin",
                            maxPoints = 1,
                            defaultBobot = 60.0,
                            photoLabels = listOf("Photo Cucian Ruang Mesin")
                        ),
                        KpiItem(
                            id = "cuc_kebersihan_engsel",
                            name = "Kebersihan Engsel-Engsel",
                            maxPoints = 5,
                            defaultBobot = 8.0,
                            photoLabels = listOf(
                                "Photo Kebersihan Engsel-Engsel 1",
                                "Photo Kebersihan Engsel-Engsel 2",
                                "Photo Kebersihan Engsel-Engsel 3",
                                "Photo Kebersihan Engsel-Engsel 4",
                                "Photo Kebersihan Engsel-Engsel 5"
                            )
                        )
                    )
                )
            )
        )
    )

    private val allItemsMap: Map<String, KpiItem> by lazy {
        sections.flatMap { it.groups }.flatMap { it.items }.associateBy { it.id }
    }

    fun getAllItems(): List<KpiItem> {
        return sections.flatMap { it.groups }.flatMap { it.items }
    }

    fun findItem(id: String): KpiItem? = allItemsMap[id]
}
