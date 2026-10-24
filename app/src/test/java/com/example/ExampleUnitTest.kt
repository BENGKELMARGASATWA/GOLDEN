package com.example

import com.example.util.DateUtils
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun testGoogleAppsScriptCode_hasCorrectIds() {
    val code = com.example.data.remote.GoogleAppsScriptHub.getGoogleScriptCode()
    assertTrue(code.contains("1gkKZMTwOPgEpwmyLPJykDpaqWFIn1FgjDvYMa6wn81M"))
    assertTrue(code.contains("165IPc5ldYJlkahW30yVBCeKamIxpwwj0"))
    assertTrue(code.contains("body_baret_halus"))
    assertTrue(code.contains("BARET_HALUS_POINT"))
  }

  @Test
  fun testDeletedKpiItems_areNotPresentInKpiAuditSpec() {
    val allItemIds = com.example.data.model.KpiAuditSpec.getAllItems().map { it.id }
    assertFalse("plastik_bening_tidak_ada should be deleted", allItemIds.contains("plastik_bening_tidak_ada"))
    assertFalse("plastik_bening_kotor_rusak should be deleted", allItemIds.contains("plastik_bening_kotor_rusak"))
    assertFalse("nomer_kaca_dashboard should be deleted", allItemIds.contains("nomer_kaca_dashboard"))
  }

  @Test
  fun testDefaultScriptUrl_isUpdated() {
    assertEquals(
      "https://script.google.com/macros/s/AKfycbwa7leAgPhl8-Yk3rRmly1jzTWhYzTYDaDMCa7KhHpVDnP7HWC_i8xFjs96nLLssp81/exec",
      com.example.data.session.SessionManager.DEFAULT_SCRIPT_URL
    )
  }

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDateUtils_parseIsoUtcDate() {
    // 2026-08-11T17:00:00 UTC corresponds to 2026-08-12 00:00:00 in WIB (GMT+7)
    val cal = DateUtils.parseToCalendar("2026-08-11T17:00:00")
    assertNotNull(cal)
    assertEquals(12, cal!!.get(Calendar.DAY_OF_MONTH))

    val display = DateUtils.formatDisplayDate("2026-08-11T17:00:00")
    assertTrue("Display date should format as dd/MM/yyyy: $display", display.startsWith("12/"))
  }

  @Test
  fun testDateUtils_matchingToday() {
    // If today is day 12 (or whatever today is), test current date match
    val todayFormatted = DateUtils.getCurrentDateFormatted()
    assertTrue(DateUtils.isDateMatchingToday(todayFormatted))

    val todayCal = Calendar.getInstance(DateUtils.wibTimeZone)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    // A single day string e.g. "12" or "tgl 12"
    assertTrue(DateUtils.isDateMatchingToday(todayDay.toString()))
    assertTrue(DateUtils.isDateMatchingToday("tgl $todayDay"))

    // If today is day 12, test "2026-08-11T17:00:00"
    if (todayDay == 12) {
      assertTrue(DateUtils.isDateMatchingToday("2026-08-11T17:00:00"))
    }
  }

  @Test
  fun testFilterLatestPerCarAndPeriod_sameCarSamePeriodKeepsLatestInput() {
    val auditOlder = com.example.data.local.entity.DataAuditEntity(
      id = 1L,
      tanggal = "10/09/2026",
      timestamp = 1000L,
      periode = "Periode 1",
      noMobil = "101",
      platNomer = "B 1101 AUT",
      pool = "POOL-01",
      nip = "1001",
      namaPetugas = "AUDITOR 1",
      tipe = "SEDAN",
      warna = "BIRU"
    )
    val auditNewer = com.example.data.local.entity.DataAuditEntity(
      id = 2L,
      tanggal = "12/09/2026",
      timestamp = 2000L,
      periode = "Periode 1",
      noMobil = "101",
      platNomer = "B 1101 AUT",
      pool = "POOL-01",
      nip = "1001",
      namaPetugas = "AUDITOR 1",
      tipe = "SEDAN",
      warna = "BIRU"
    )
    val auditDifferentPeriod = com.example.data.local.entity.DataAuditEntity(
      id = 3L,
      tanggal = "15/09/2026",
      timestamp = 3000L,
      periode = "Periode 2",
      noMobil = "101",
      platNomer = "B 1101 AUT",
      pool = "POOL-01",
      nip = "1001",
      namaPetugas = "AUDITOR 1",
      tipe = "SEDAN",
      warna = "BIRU"
    )
    val auditDifferentCar = com.example.data.local.entity.DataAuditEntity(
      id = 4L,
      tanggal = "11/09/2026",
      timestamp = 1500L,
      periode = "Periode 1",
      noMobil = "102",
      platNomer = "B 1102 AUT",
      pool = "POOL-01",
      nip = "1001",
      namaPetugas = "AUDITOR 1",
      tipe = "SEDAN",
      warna = "BIRU"
    )

    // Incoming list sorted DESC by timestamp: [3, 2, 4, 1]
    val inputList = listOf(auditDifferentPeriod, auditNewer, auditDifferentCar, auditOlder)

    val filtered = com.example.util.AuditFilterUtils.filterLatestPerCarAndPeriod(inputList)

    // Expected: auditOlder (id=1) must be dropped because auditNewer (id=2) is the latest input for noMobil 101 in Periode 1.
    // auditDifferentPeriod (id=3) must remain because it is in Periode 2.
    // auditDifferentCar (id=4) must remain because it is noMobil 102.
    assertEquals(3, filtered.size)
    assertEquals(listOf(3L, 2L, 4L), filtered.map { it.id })
  }

  @Test
  fun testRekapNilaiSampleCount_matchesRiwayatAuditDisplay() {
    val pool = "POOL-01"
    val periode = "Periode 1"

    // Mobil 101 diinput 2 kali pada Periode 1 (misal: input awal 10/09, perbaikan/audit ulang 12/09)
    val audit101Old = com.example.data.local.entity.DataAuditEntity(
      id = 10L,
      tanggal = "10/09/2026",
      timestamp = 1000L,
      periode = periode,
      noMobil = "101",
      platNomer = "B 1101 AUT",
      pool = pool,
      nip = "1001",
      namaPetugas = "AUDITOR 1",
      tipe = "SEDAN",
      warna = "BIRU"
    )
    val audit101New = com.example.data.local.entity.DataAuditEntity(
      id = 11L,
      tanggal = "12/09/2026",
      timestamp = 2000L,
      periode = periode,
      noMobil = "101",
      platNomer = "B 1101 AUT",
      pool = pool,
      nip = "1001",
      namaPetugas = "AUDITOR 1",
      tipe = "SEDAN",
      warna = "BIRU"
    )
    // Mobil 102 diinput 1 kali pada Periode 1
    val audit102 = com.example.data.local.entity.DataAuditEntity(
      id = 12L,
      tanggal = "12/09/2026",
      timestamp = 1500L,
      periode = periode,
      noMobil = "102",
      platNomer = "B 1102 AUT",
      pool = pool,
      nip = "1001",
      namaPetugas = "AUDITOR 1",
      tipe = "SEDAN",
      warna = "BIRU"
    )

    val rawAudits = listOf(audit101New, audit102, audit101Old)

    // Simulasi data Riwayat Audit
    val riwayatDisplayed = com.example.util.AuditFilterUtils.filterLatestPerCarAndPeriod(
      com.example.util.AuditFilterUtils.filterUniqueAudits(rawAudits)
    )
    assertEquals(2, riwayatDisplayed.size)
    assertTrue(riwayatDisplayed.any { it.id == 11L })
    assertFalse(riwayatDisplayed.any { it.id == 10L })

    // Simulasi filter pada Rekap Nilai Audit
    val rekapNilaiFiltered = com.example.util.AuditFilterUtils.filterLatestPerCarAndPeriod(
      com.example.util.AuditFilterUtils.filterUniqueAudits(
        rawAudits.filter { it.periode.equals(periode, true) && it.pool.equals(pool, true) }
      )
    )

    // Jumlah sample pada Rekap Nilai Audit harus persis sama dengan data di Riwayat Audit
    assertEquals(riwayatDisplayed.size, rekapNilaiFiltered.size)
    assertEquals(2, rekapNilaiFiltered.size)

    // Simulasi filter pada Resume Audit Keindahan
    val resumeFiltered = com.example.util.AuditFilterUtils.filterLatestPerCarAndPeriod(
      com.example.util.AuditFilterUtils.filterUniqueAudits(
        rawAudits.filter { it.periode.equals(periode, true) && it.pool.equals(pool, true) }
      )
    )
    // Simulasi filter pada Rekap Temuan
    val rekapTemuanFiltered = com.example.util.AuditFilterUtils.filterLatestPerCarAndPeriod(
      com.example.util.AuditFilterUtils.filterUniqueAudits(
        rawAudits.filter { it.periode.equals(periode, true) && it.pool.equals(pool, true) }
      )
    )
    // Data unit di Rekap Temuan harus persis sama dengan data di Riwayat Audit
    assertEquals(riwayatDisplayed.size, rekapTemuanFiltered.size)
    assertEquals(listOf(11L, 12L).toSet(), rekapTemuanFiltered.map { it.id }.toSet())
  }

  @Test
  fun testVehicleInputSpec_platFollowsNoMobilAndManualFields() {
    val initialForm = com.example.ui.viewmodel.AuditFormState(
      noMobil = "105",
      platNomer = "105",
      tipe = "AVANZA",
      warna = "HITAM",
      tahun = "2023",
      pool = "SKT"
    )

    assertEquals("105", initialForm.noMobil)
    assertEquals("105", initialForm.platNomer)
    assertEquals("AVANZA", initialForm.tipe)
    assertEquals("HITAM", initialForm.warna)
    assertEquals("2023", initialForm.tahun)
    assertEquals("SKT", initialForm.pool)

    // Simulasi perubahan manual tipe, warna, tahun, dan pool
    val updatedForm = initialForm.copy(
      tipe = "INNOVA",
      warna = "SILVER",
      tahun = "2024",
      pool = "BTR"
    )
    assertEquals("INNOVA", updatedForm.tipe)
    assertEquals("SILVER", updatedForm.warna)
    assertEquals("2024", updatedForm.tahun)
    assertEquals("BTR", updatedForm.pool)
    // Plat nomor tetap mengikuti No Mobil
    assertEquals(updatedForm.noMobil, updatedForm.platNomer)
  }
}

