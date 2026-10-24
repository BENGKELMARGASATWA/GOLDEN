package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AUDIT GOLDEN BIRD", appName)
  }

  @Test
  fun testAuditSchema_parseCloudIdAndUuid() {
    val json = org.json.JSONObject().apply {
      put("ID", 42L)
      put("AUDIT_UUID", "test-uuid-1234")
      put("NO_MOBIL", "MB-101")
      put("PERIODE", "Maret 2026")
      put("TANGGAL", "14/03/2026")
      put("NIP", "12345")
      put("NAMA_PETUGAS", "Budi")
    }

    val parsed = com.example.data.model.AuditDatabaseSchema.parseSheetRowToAudit(json, null)
    assertEquals(42L, parsed.cloudId)
    assertEquals("test-uuid-1234", parsed.auditUuid)
    assertEquals("MB-101", parsed.noMobil)
    assertEquals("Maret 2026", parsed.periode)
  }

  @Test
  fun testAuditSchema_parseWaktuTimestampToTanggal() {
    val json = org.json.JSONObject().apply {
      put("ID", 43L)
      put("AUDIT_UUID", "test-uuid-5678")
      put("NO_MOBIL", "MB-102")
      put("PERIODE", "Maret 2026")
      put("TANGGAL", "14/03/2026")
      put("WAKTU/TIMESTAMP", "14/03/2026 15:45:30")
      put("NIP", "12345")
      put("NAMA_PETUGAS", "Budi")
    }

    val parsed = com.example.data.model.AuditDatabaseSchema.parseSheetRowToAudit(json, null)
    assertEquals("14/03/2026 15:45:30", parsed.tanggal)
  }

  @Test
  fun testAuditSchema_extractColumn3WaktuTimestamp() {
    val jsonWithKolom3 = org.json.JSONObject().apply {
      put("ID", "AUD-123")
      put("TANGGAL", "15/03/2026")
      put("KOLOM_3", "15/03/2026 16:20:10")
    }
    val extracted = com.example.data.model.AuditDatabaseSchema.extractWaktuTimestampFromSheetRow(jsonWithKolom3)
    assertEquals("15/03/2026 16:20:10", extracted)

    val parsed = com.example.data.model.AuditDatabaseSchema.parseSheetRowToAudit(jsonWithKolom3, null)
    assertEquals("15/03/2026 16:20:10", parsed.tanggal)
  }
}
