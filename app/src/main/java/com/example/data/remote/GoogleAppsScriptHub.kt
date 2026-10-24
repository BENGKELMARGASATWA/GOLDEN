package com.example.data.remote

import com.example.data.model.AuditDatabaseSchema
import com.example.data.model.KpiAuditSpec

import android.content.Context
import com.example.data.local.entity.DataAuditEntity
import com.example.data.session.SessionManager
import com.example.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GoogleAppsScriptHub {

    val SPREADSHEET_ID = SessionManager.DEFAULT_SHEET_ID
    val DRIVE_FOLDER_ID = SessionManager.DEFAULT_FOLDER_ID

    fun getGoogleScriptCode(sheetId: String = SPREADSHEET_ID, folderId: String = DRIVE_FOLDER_ID): String {
        val inputAuditHeadersJs = AuditDatabaseSchema.INPUT_AUDIT_COLUMNS.joinToString(", ") { "\"$it\"" }
        val kpiItemIdsJs = KpiAuditSpec.getAllItems().joinToString(", ") { "\"${it.id}\"" }
        return """
/**
 * GOOGLE APPS SCRIPT - SISTEM AUDIT PERAWATAN MOBIL (DATABASE TERPUSAT)
 * Google Sheet ID: $sheetId
 * Google Drive Folder ID: $folderId
 */

const SHEET_ID = '$sheetId';
const FOLDER_ID = '$folderId';
const INPUT_AUDIT_COLUMNS = [ $inputAuditHeadersJs ];
const KPI_ITEM_IDS = [ $kpiItemIdsJs ];

function findSheetFlexible(ss, ...possibleNames) {
  if (!ss) return null;
  for (let i = 0; i < possibleNames.length; i++) {
    const s = ss.getSheetByName(possibleNames[i]);
    if (s) return s;
  }
  const allSheets = ss.getSheets();
  const targets = possibleNames.map(function(n) { return String(n).toLowerCase().replace(/[^a-z0-9]/g, ''); });
  for (let i = 0; i < allSheets.length; i++) {
    const norm = allSheets[i].getName().toLowerCase().replace(/[^a-z0-9]/g, '');
    if (targets.indexOf(norm) !== -1) {
      return allSheets[i];
    }
  }
  return null;
}

function doGet(e) {
  const page = (e && e.parameter && e.parameter.page) ? e.parameter.page : 'index';
  const action = (e && e.parameter && e.parameter.action) ? e.parameter.action : '';

  // JSON API untuk Sinkronisasi Aplikasi Android (Optimasi 80.000 data: format kompak & filter pool)
  if (action === 'getKendaraan' || action === 'fetchKendaraan') {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    const poolParam = (e && e.parameter && e.parameter.pool) ? e.parameter.pool : '';
    const offsetParam = (e && e.parameter && e.parameter.offset) ? parseInt(e.parameter.offset) : 0;
    const limitParam = (e && e.parameter && e.parameter.limit) ? parseInt(e.parameter.limit) : 0;
    const formatParam = (e && e.parameter && e.parameter.format) ? e.parameter.format : 'compact';

    if (formatParam === 'legacy') {
      const sheetKendaraan = findSheetFlexible(ss, 'MASTER_KENDARAAN', 'MASTER KENDARAAN', 'Master Kendaraan', 'Kendaraan', 'Armada');
      const data = getSheetDataAsObjects(sheetKendaraan);
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', kendaraan: data, count: data.length }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    const result = fetchFastKendaraanData(ss, poolParam, offsetParam, limitParam, formatParam);
    return ContentService.createTextOutput(JSON.stringify(result))
      .setMimeType(ContentService.MimeType.JSON);
  }
  if (action === 'getAuditorUsers' || action === 'fetchAuditorUsers' || action === 'getUsers' || action === 'fetchUsers') {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    const sheetUser = findSheetFlexible(ss, 'USER', 'USERS', 'User', 'Master User', 'MASTER_USER');
    let usersData = getSheetDataAsObjects(sheetUser);
    const roleParam = (e && e.parameter && e.parameter.role) ? String(e.parameter.role).trim().toUpperCase() : '';
    const isAuditorOnly = action === 'getAuditorUsers' || action === 'fetchAuditorUsers' || roleParam === 'AUDITOR';
    if (isAuditorOnly) {
      usersData = usersData.filter(function(u) {
        const r = String(u.LEVELAKSES || u.levelAkses || u.ROLE || u.role || u.LEVEL || u.level || '').trim().toUpperCase();
        return r === 'AUDITOR' || r.indexOf('AUDITOR') !== -1;
      });
    }
    return ContentService.createTextOutput(JSON.stringify({ status: 'success', users: usersData, count: usersData.length, auditorOnly: isAuditorOnly }))
      .setMimeType(ContentService.MimeType.JSON);
  }
  if (action === 'getMasterData') {
    return ContentService.createTextOutput(JSON.stringify(fetchAllMasterData()))
      .setMimeType(ContentService.MimeType.JSON);
  }
  if (action === 'getSamples' || action === 'getSample') {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    let sheet = findSheetFlexible(ss, 'MASTER_SAMPLE', 'MASTER SAMPLE', 'Master Sample', 'Sample', 'SAMPLE', 'DATA_SAMPLE', 'DATA SAMPLE');
    if (!sheet || sheet.getLastRow() <= 1) {
      setupSpreadsheet();
      sheet = findSheetFlexible(ss, 'MASTER_SAMPLE', 'MASTER SAMPLE', 'Master Sample', 'Sample', 'SAMPLE', 'DATA_SAMPLE', 'DATA SAMPLE');
    }
    return ContentService.createTextOutput(JSON.stringify({ status: 'success', sample: getSheetDataAsObjects(sheet) }))
      .setMimeType(ContentService.MimeType.JSON);
  }
  if (action === 'getAudits') {
    return ContentService.createTextOutput(JSON.stringify(fetchAuditData()))
      .setMimeType(ContentService.MimeType.JSON);
  }
  if (action === 'initInputAudit') {
    const res = initInputAudit();
    return ContentService.createTextOutput(JSON.stringify(res))
      .setMimeType(ContentService.MimeType.JSON);
  }
  if (action === 'initGradeKpi') {
    const res = initGradeKpi();
    return ContentService.createTextOutput(JSON.stringify(res))
      .setMimeType(ContentService.MimeType.JSON);
  }
  if (action === 'setupSpreadsheet' || action === 'initSheets') {
    setupSpreadsheet();
    return ContentService.createTextOutput(JSON.stringify({ status: 'success', message: 'Semua Sheet (termasuk MASTER_BOBOT dan INPUT_AUDIT) berhasil diinisialisasi.' }))
      .setMimeType(ContentService.MimeType.JSON);
  }

  // Web Portal Rendering
  return HtmlService.createTemplateFromFile(page)
    .evaluate()
    .setTitle('Audit Perawatan Mobil')
    .addMetaTag('viewport', 'width=device-width, initial-scale=1.0')
    .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
}

// Helper Mutex Lock untuk mencegah tumpang tindih data saat banyak user mengakses bersamaan
function withScriptLock(fn, timeoutMs) {
  const lock = LockService.getScriptLock();
  const waitMs = (timeoutMs && timeoutMs > 0) ? timeoutMs : 45000;
  const hasLock = lock.tryLock(waitMs);
  if (!hasLock) {
    return { status: 'error', busy: true, message: 'Server Google Sheets sedang sibuk melayani user lain. Silakan coba kembali dalam beberapa detik.' };
  }
  try {
    const result = fn();
    SpreadsheetApp.flush();
    return result;
  } catch (err) {
    return { status: 'error', message: err.toString() };
  } finally {
    try { SpreadsheetApp.flush(); } catch (e) {}
    try { lock.releaseLock(); } catch (e) {}
  }
}

function doPost(e) {
  try {
    const postData = JSON.parse(e.postData.contents);
    const action = postData.action || 'submitAudit';

    if (action === 'getKendaraan' || action === 'fetchKendaraan') {
      const ss = SpreadsheetApp.openById(SHEET_ID);
      const poolParam = postData.pool || '';
      const offsetParam = postData.offset ? parseInt(postData.offset) : 0;
      const limitParam = postData.limit ? parseInt(postData.limit) : 0;
      const formatParam = postData.format || 'compact';

      if (formatParam === 'legacy') {
        const sheetKendaraan = findSheetFlexible(ss, 'MASTER_KENDARAAN', 'MASTER KENDARAAN', 'Master Kendaraan', 'Kendaraan', 'Armada');
        const data = getSheetDataAsObjects(sheetKendaraan);
        return ContentService.createTextOutput(JSON.stringify({ status: 'success', kendaraan: data, count: data.length }))
          .setMimeType(ContentService.MimeType.JSON);
      }

      const result = fetchFastKendaraanData(ss, poolParam, offsetParam, limitParam, formatParam);
      return ContentService.createTextOutput(JSON.stringify(result))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'getAuditorUsers' || action === 'fetchAuditorUsers' || action === 'getUsers' || action === 'fetchUsers') {
      const ss = SpreadsheetApp.openById(SHEET_ID);
      const sheetUser = findSheetFlexible(ss, 'USER', 'USERS', 'User', 'Master User', 'MASTER_USER');
      let usersData = getSheetDataAsObjects(sheetUser);
      const roleParam = (data && data.role) ? String(data.role).trim().toUpperCase() : '';
      const isAuditorOnly = action === 'getAuditorUsers' || action === 'fetchAuditorUsers' || roleParam === 'AUDITOR';
      if (isAuditorOnly) {
        usersData = usersData.filter(function(u) {
          const r = String(u.LEVELAKSES || u.levelAkses || u.ROLE || u.role || u.LEVEL || u.level || '').trim().toUpperCase();
          return r === 'AUDITOR' || r.indexOf('AUDITOR') !== -1;
        });
      }
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', users: usersData, count: usersData.length, auditorOnly: isAuditorOnly }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'getMasterData') {
      return ContentService.createTextOutput(JSON.stringify(fetchAllMasterData()))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'getSamples' || action === 'getSample') {
      const ss = SpreadsheetApp.openById(SHEET_ID);
      let sheet = findSheetFlexible(ss, 'MASTER_SAMPLE', 'MASTER SAMPLE', 'Master Sample', 'Sample', 'SAMPLE', 'DATA_SAMPLE', 'DATA SAMPLE');
      if (!sheet || sheet.getLastRow() <= 1) {
        setupSpreadsheet();
        sheet = findSheetFlexible(ss, 'MASTER_SAMPLE', 'MASTER SAMPLE', 'Master Sample', 'Sample', 'SAMPLE', 'DATA_SAMPLE', 'DATA SAMPLE');
      }
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', sample: getSheetDataAsObjects(sheet) }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'getAudits') {
      return ContentService.createTextOutput(JSON.stringify(fetchAuditData()))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'submitAudit') {
      const result = saveAuditRecord(postData.audit);
      if (result && (result.status === 'error' || result.busy)) {
        return ContentService.createTextOutput(JSON.stringify({ status: 'error', busy: !!result.busy, message: result.message || 'Server sibuk', data: result }))
          .setMimeType(ContentService.MimeType.JSON);
      }
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: result }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'deleteAudit') {
      const res = withScriptLock(function() {
        return deleteAuditRecord(postData.id, postData.noMobil, postData.periode, postData.kodeUnik);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'saveKendaraan') {
      const res = withScriptLock(function() {
        return saveMasterKendaraan(postData.kendaraan);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'deleteKendaraan') {
      const res = withScriptLock(function() {
        return deleteMasterKendaraan(postData.id, postData.noMobil);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'savePool') {
      const res = withScriptLock(function() {
        return saveMasterPool(postData.pool);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'deletePool') {
      const res = withScriptLock(function() {
        return deleteMasterPool(postData.id, postData.kodePool);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'saveUser') {
      const res = withScriptLock(function() {
        return saveMasterUser(postData.user);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'deleteUser') {
      const res = withScriptLock(function() {
        return deleteMasterUser(postData.nip);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'saveBobot') {
      const res = withScriptLock(function() {
        return saveMasterBobot(postData.bobot);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'saveAllBobot') {
      const res = withScriptLock(function() {
        return saveAllMasterBobot(postData.bobotList);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'deleteBobot') {
      const res = withScriptLock(function() {
        return deleteMasterBobot(postData.itemId);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'saveSample') {
      const res = withScriptLock(function() {
        return saveMasterSample(postData.sample);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'deleteSample') {
      const res = withScriptLock(function() {
        return deleteMasterSample(postData.statusArea);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'savePeriode') {
      const res = withScriptLock(function() {
        return saveMasterPeriode(postData.periode);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'deletePeriode') {
      const res = withScriptLock(function() {
        return deleteMasterPeriode(postData.id, postData.periode);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'fixPhotoPermissions') {
      const res = fixPhotoPermissions();
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'initInputAudit') {
      const res = initInputAudit();
      return ContentService.createTextOutput(JSON.stringify(res))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'initGradeKpi') {
      const res = initGradeKpi();
      return ContentService.createTextOutput(JSON.stringify(res))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'saveGradeKpi') {
      const res = withScriptLock(function() {
        return saveMasterGradeKpi(postData.grade);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'saveAllGradeKpi') {
      const res = withScriptLock(function() {
        return saveAllMasterGradeKpi(postData.gradeList);
      });
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', data: res }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (action === 'setupSpreadsheet' || action === 'initSheets') {
      setupSpreadsheet();
      return ContentService.createTextOutput(JSON.stringify({ status: 'success', message: 'Semua Sheet (termasuk MASTER_BOBOT dan INPUT_AUDIT) berhasil diinisialisasi.' }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    return ContentService.createTextOutput(JSON.stringify({ status: 'error', message: 'Unknown action: ' + action }))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ status: 'error', message: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function include(filename) {
  return HtmlService.createHtmlOutputFromFile(filename).getContent();
}

/**
 * Inisialisasi Database Terpusat Google Sheets
 * Menyiapkan semua Sheet: USER, MASTER_POOL, MASTER_KENDARAAN, MASTER_BOBOT, MASTER_SAMPLE, MASTER_PERIODE, DATA_AUDIT
 */
function setupSpreadsheet() {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  
  // 1. USER (Struktur header bersih tanpa user dummy lokal)
  let sheetUser = findSheetFlexible(ss, 'USER', 'USERS', 'User') || ss.insertSheet('USER');
  if (sheetUser.getLastRow() === 0) {
    sheetUser.appendRow(['NIP', 'NAMA_PETUGAS', 'USERNAMA', 'PASSWORD', 'LEVELAKSES', 'KODEPOOL', 'STATUS', 'TANGGAL_AKTIF']);
  }

  // 2. MASTER_POOL (Header bersih tanpa baris dummy)
  let sheetPool = findSheetFlexible(ss, 'MASTER_POOL', 'MASTER POOL', 'Master Pool', 'Pool') || ss.insertSheet('MASTER_POOL');
  if (sheetPool.getLastRow() === 0) {
    sheetPool.appendRow(['ID', 'KODE_POOL', 'NAMA_POOL', 'ALAMAT_POOL', 'STATUS_AREA']);
  }

  // 3. MASTER_KENDARAAN (Struktur header bersih tanpa data dummy)
  let sheetMobil = findSheetFlexible(ss, 'MASTER_KENDARAAN', 'MASTER KENDARAAN', 'Master Kendaraan', 'Kendaraan') || ss.insertSheet('MASTER_KENDARAAN');
  if (sheetMobil.getLastRow() === 0) {
    sheetMobil.appendRow(['ID', 'NO_MOBIL', 'PLAT_NOMER', 'TIPE', 'WARNA', 'POOL', 'TAHUN', 'TANGGAL_SERVICE', 'SERVICE']);
  }

  // 4. MASTER_BOBOT
  let sheetBobot = findSheetFlexible(ss, 'MASTER_BOBOT', 'MASTER BOBOT', 'Master Bobot', 'Bobot') || ss.insertSheet('MASTER_BOBOT');
  if (sheetBobot.getLastRow() === 0) {
    sheetBobot.appendRow(['ITEM_ID', 'ITEM_NAME', 'SECTION_TITLE', 'GROUP_TITLE', 'POINT_MAX', 'BOBOT']);
    const defaultBobotItems = [
      ['body_keropos', 'Keropos', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '1. BODY', 1, 30.0],
      ['body_penyok', 'PENYOK', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '1. BODY', 1, 25.0],
      ['body_dekok', 'DEKOK', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '1. BODY', 1, 15.0],
      ['body_baret_halus', 'BARET HALUS', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '1. BODY', 5, 10.0],
      ['body_baret', 'BARET', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '1. BODY', 1, 10.0],
      ['body_kusam', 'KUSAM', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '1. BODY', 1, 10.0],
      ['kaca_depan_baret', 'Kaca Depan Baret', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '4. KACA', 1, 12.5],
      ['kaca_depan_samping_berjamur', 'Kaca Samping Berjamur', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '4. KACA', 2, 5.0],
      ['kaca_depan_samping_baret', 'Kaca Samping Baret', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '4. KACA', 2, 10.0],
      ['kaca_belakang_berjamur', 'Kaca Belakang Berjamur', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '4. KACA', 1, 5.0],
      ['kaca_spion_berjamur', 'Kaca Spion Berjamur', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '4. KACA', 2, 7.5],
      ['stoplamp_kusam_berembun', 'Stoplamp Kusam/Buram', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '5. HEADLAMP & STOPLAMP', 2, 7.5],
      ['stoplamp_berembun', 'Stoplamp Berembun', 'A. REKAP TEMUAN KPI AUDIT EXTERIOR', '5. HEADLAMP & STOPLAMP', 2, 2.5],
      ['kebersihan_jok', 'Kebersihan Jok Mobil', 'B. REKAP TEMUAN KPI AUDIT INTERIOR', '1. JOK & KARPET', 1, 10.0],
      ['kebersihan_plafon', 'Kebersihan Plafon', 'B. REKAP TEMUAN KPI AUDIT INTERIOR', '2. PLAFON & DASHBOARD', 1, 15.0],
      ['kebersihan_dashboard', 'Kebersihan Dashboard', 'B. REKAP TEMUAN KPI AUDIT INTERIOR', '2. PLAFON & DASHBOARD', 1, 10.0],
      ['aroma_interior', 'Aroma & Bau Tidak Sedap', 'B. REKAP TEMUAN KPI AUDIT INTERIOR', '3. AROMA KABIN', 1, 20.0],
      ['kebersihan_kolong', 'Kebersihan Kolong Mobil', 'C. REKAP TEMUAN CUCI & FINISHING', '1. CUCI & FINISHING', 1, 15.0],
      ['kebersihan_velg_ban', 'Kebersihan Velg & Semir Ban', 'C. REKAP TEMUAN CUCI & FINISHING', '1. CUCI & FINISHING', 1, 10.0]
    ];
    defaultBobotItems.forEach(row => sheetBobot.appendRow(row));
  }

  // 5. MASTER_SAMPLE
  let sheetSample = findSheetFlexible(ss, 'MASTER_SAMPLE', 'MASTER SAMPLE', 'Master Sample', 'Sample') || ss.insertSheet('MASTER_SAMPLE');
  if (sheetSample.getLastRow() === 0) {
    sheetSample.appendRow(['STATUS_AREA', 'JUMLAH_SAMPLE', 'KETERANGAN']);
    sheetSample.appendRow(['JABODETABEK', 20, 'Standar sample armada area Jabodetabek']);
    sheetSample.appendRow(['JABODETABEK SATELIT', 15, 'Standar sample armada area Jabodetabek Satelit']);
    sheetSample.appendRow(['DAERAH', 10, 'Standar sample armada area Daerah']);
    sheetSample.appendRow(['DAERAH SATELIT', 5, 'Standar sample armada area Daerah Satelit']);
  }

  // 6. MASTER_PERIODE
  let sheetPeriode = findSheetFlexible(ss, 'MASTER_PERIODE', 'MASTER PERIODE', 'Master Periode', 'Periode') || ss.insertSheet('MASTER_PERIODE');
  if (sheetPeriode.getLastRow() === 0) {
    sheetPeriode.appendRow(['ID', 'PERIODE', 'KETERANGAN']);
    sheetPeriode.appendRow([1, 'Periode 1', 'Audit Periode 1 (Minggu 1)']);
    sheetPeriode.appendRow([2, 'Periode 2', 'Audit Periode 2 (Minggu 2)']);
    sheetPeriode.appendRow([3, 'Periode 3', 'Audit Periode 3 (Minggu 3)']);
    sheetPeriode.appendRow([4, 'Periode 4', 'Audit Periode 4 (Minggu 4)']);
    sheetPeriode.appendRow([5, 'Mingguan', 'Audit Rutin Mingguan']);
    sheetPeriode.appendRow([6, 'Bulanan', 'Audit Rutin Bulanan']);
  }

  // 7. MASTER_GRADE_KPI (Struktur kolom KATEGORI, MIN_SCORE, MAX_SCORE, KETERANGAN tanpa menimpa nilai kustom)
  initGradeKpiSheet(ss);

  // 8. INPUT_AUDIT (A1 s/d LK1 = 325 Kolom Lengkap & Terformat)
  initInputAuditSheet(ss);
}

/**
 * Inisialisasi Lembar MASTER_GRADE_KPI
 * Menyiapkan sheet MASTER_GRADE_KPI dengan kolom KATEGORI, MIN_SCORE, MAX_SCORE, KETERANGAN.
 * Nilai defaultGrade dihapus agar nilai kustom di sheet tidak berubah / tertimpa secara otomatis.
 */
function initGradeKpi() {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  return initGradeKpiSheet(ss);
}

function initGradeKpiSheet(ss) {
  let sheet = findSheetFlexible(ss, 'MASTER_GRADE_KPI', 'MASTER GRADE KPI', 'GRADE_KPI', 'GRADE KPI', 'Grade KPI', 'Grade');
  if (!sheet) {
    sheet = ss.insertSheet('MASTER_GRADE_KPI');
  }

  // Jika lembar masih kosong, tambahkan baris header dan konfigurasi default Grade KPI
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(['KATEGORI', 'MIN_SCORE', 'MAX_SCORE', 'KETERANGAN']);
    sheet.appendRow(['A', 70.00, 80.00, 'Sangat Baik (Grade A)']);
    sheet.appendRow(['B', 60.00, 69.99, 'Baik (Grade B)']);
    sheet.appendRow(['C', 50.00, 59.99, 'Cukup (Grade C)']);
    sheet.appendRow(['D', 30.00, 49.99, 'Kurang (Grade D)']);
    sheet.appendRow(['E', 0.00, 29.99, 'Kritis / Perlu Perbaikan (Grade E)']);

    try {
      const headerRange = sheet.getRange(1, 1, 1, 4);
      headerRange.setFontWeight('bold');
      headerRange.setFontColor('#FFFFFF');
      headerRange.setBackground('#0D47A1'); // Deep Blue Modern
      headerRange.setHorizontalAlignment('center');
      headerRange.setVerticalAlignment('middle');
      sheet.setRowHeight(1, 36);
      sheet.setFrozenRows(1);
      sheet.setColumnWidth(1, 120); // KATEGORI
      sheet.setColumnWidth(2, 140); // MIN_SCORE
      sheet.setColumnWidth(3, 140); // MAX_SCORE
      sheet.setColumnWidth(4, 280); // KETERANGAN

      sheet.getRange(2, 2, 5, 2).setNumberFormat('0.00');
      sheet.getRange(2, 1, 5, 1).setHorizontalAlignment('center');
    } catch (e) {}
  }

  return {
    status: 'success',
    sheetName: sheet.getName(),
    totalRows: Math.max(0, sheet.getLastRow() - 1),
    message: 'Lembar MASTER_GRADE_KPI siap (struktur kolom KATEGORI, MIN_SCORE, MAX_SCORE, KETERANGAN dipelihara tanpa menimpa data).'
  };
}

/**
 * Inisialisasi Lembar INPUT_AUDIT
 * Membuat atau memperbarui sheet INPUT_AUDIT dengan 325 kolom standar (A1 s/d LK1),
 * melakukan styling header profesional, freeze baris 1 & kolom 1..7, serta mengatur lebar kolom.
 */
function initInputAudit() {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  return initInputAuditSheet(ss);
}

function initInputAuditSheet(ss) {
  let sheet = findSheetFlexible(ss, 'INPUT_AUDIT', 'INPUT AUDIT', 'Input Audit', 'DATA_AUDIT', 'DATA AUDIT', 'Data Audit', 'Audit');
  if (!sheet) {
    sheet = ss.insertSheet('INPUT_AUDIT');
  }

  // 1. Pastikan kapasitas sheet cukup untuk 325 kolom dan minimal 100 baris
  ensureSheetCapacity(sheet, INPUT_AUDIT_COLUMNS.length, 100);

  // 2. Pasang baris header (A1 s/d LK1)
  const headerRange = sheet.getRange(1, 1, 1, INPUT_AUDIT_COLUMNS.length);
  headerRange.setValues([INPUT_AUDIT_COLUMNS]);

  // 3. Styling Header
  try {
    headerRange.setFontWeight('bold');
    headerRange.setFontColor('#FFFFFF');
    headerRange.setBackground('#1E293B'); // Navy Slate Modern
    headerRange.setHorizontalAlignment('center');
    headerRange.setVerticalAlignment('middle');
    headerRange.setWrap(false);
    sheet.setRowHeight(1, 38);
  } catch (styleErr) {}

  // 4. Freeze baris header & identitas armada agar nyaman saat scroll ke kanan
  try {
    sheet.setFrozenRows(1);
    sheet.setFrozenColumns(7); // Membekukan kolom ID s/d NO_KENDARAAN
  } catch (freezeErr) {}

  // 5. Atur lebar kolom utama agar mudah dibaca
  try {
    sheet.setColumnWidth(1, 65);   // ID
    sheet.setColumnWidth(2, 105);  // TANGGAL
    sheet.setColumnWidth(3, 150);  // TIMESTAMP
    sheet.setColumnWidth(4, 90);   // NIP
    sheet.setColumnWidth(5, 150);  // NAMA_PETUGAS
    sheet.setColumnWidth(6, 110);  // PERIODE
    sheet.setColumnWidth(7, 110);  // NO_KENDARAAN
    sheet.setColumnWidth(8, 120);  // PLAT_NOMER
    sheet.setColumnWidth(9, 110);  // TIPE
    sheet.setColumnWidth(10, 85);  // WARNA
    sheet.setColumnWidth(11, 80);  // TAHUN
    sheet.setColumnWidth(12, 90);  // POOL

    // Kolom Foto Kendaraan (13 s/d 19)
    for (let col = 13; col <= 19; col++) {
      sheet.setColumnWidth(col, 200);
    }

    sheet.setColumnWidth(20, 130); // JENIS_SAMPLE
    sheet.setColumnWidth(21, 130); // TANGGAL_SERVICE
    sheet.setColumnWidth(22, 130); // PAKET_SERVICE
    sheet.setColumnWidth(23, 120); // TOTAL_POIN_TEMUAN
  } catch (widthErr) {}

  return {
    status: 'success',
    sheetName: sheet.getName(),
    totalColumns: INPUT_AUDIT_COLUMNS.length,
    lastColumnLetter: 'NB',
    message: 'Lembar INPUT_AUDIT berhasil diinisialisasi dengan ' + INPUT_AUDIT_COLUMNS.length + ' kolom (A1 s/d LK1), styling header, dan pembekuan kolom (freeze).'
  };
}

function ensureSheetCapacity(targetSheet, minCols, minRows) {
  if (!targetSheet) return;
  const currentCols = targetSheet.getMaxColumns();
  if (minCols && currentCols < minCols) {
    targetSheet.insertColumnsAfter(currentCols, minCols - currentCols);
  }
  const currentRows = targetSheet.getMaxRows();
  if (minRows && currentRows < minRows) {
    targetSheet.insertRowsAfter(currentRows, minRows - currentRows);
  }
}

/**
 * Menyimpan / Memperbarui Record Audit di INPUT_AUDIT (A1 s/d LK1)
 * Baris lama di-update secara in-place jika ID atau No Kendaraan + Periode cocok.
 */
function saveAuditRecord(audit) {
  let folder = null;
  try {
    if (FOLDER_ID && String(FOLDER_ID).trim() !== '') {
      folder = DriveApp.getFolderById(String(FOLDER_ID).trim());
    }
  } catch (fe) {
    folder = null;
  }
  if (!folder) {
    try {
      const existingFolders = DriveApp.getFoldersByName('FOTO_AUDIT_KENDARAAN');
      if (existingFolders.hasNext()) {
        folder = existingFolders.next();
      } else {
        folder = DriveApp.createFolder('FOTO_AUDIT_KENDARAAN');
      }
    } catch (fe2) {
      folder = null;
    }
  }
  if (folder) {
    try {
      folder.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    } catch (fErr1) {
      try { folder.setSharing(DriveApp.Access.ANYONE, DriveApp.Permission.VIEW); } catch (fErr2) {}
    }
  }

  const targetNoMobil = (audit.noKendaraan || audit.noMobil) ? String(audit.noKendaraan || audit.noMobil).trim().toUpperCase() : '';
  const targetPeriode = audit.periode ? String(audit.periode).trim().toUpperCase() : '';
  const targetTanggal = audit.tanggal ? String(audit.tanggal).trim() : '';

  function uploadImage(base64Data, filename) {
    if (!base64Data || String(base64Data).trim().length < 50) return '';
    if (String(base64Data).startsWith('http')) return base64Data;
    try {
      const cleanB64 = String(base64Data).replace(/^data:image\/[a-z]+;base64,/, '').trim();
      const decoded = Utilities.base64Decode(cleanB64);
      const blob = Utilities.newBlob(decoded, 'image/jpeg', filename);
      const file = folder ? folder.createFile(blob) : DriveApp.createFile(blob);
      try {
        file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      } catch (shareErr1) {
        try {
          file.setSharing(DriveApp.Access.ANYONE, DriveApp.Permission.VIEW);
        } catch (shareErr2) {
          try {
            file.setSharing(DriveApp.Access.DOMAIN_WITH_LINK, DriveApp.Permission.VIEW);
          } catch (shareErr3) {}
        }
      }
      const fileId = file.getId();
      return 'https://drive.google.com/uc?export=view&id=' + fileId;
    } catch (e) {
      return 'Upload Error: ' + e.message;
    }
  }

  let kpiScores = {};
  if (typeof audit.kpiScoresJson === 'string' && audit.kpiScoresJson.startsWith('{')) {
    try { kpiScores = JSON.parse(audit.kpiScoresJson); } catch (e) {}
  } else if (typeof audit.kpiScores === 'object' && audit.kpiScores !== null) {
    kpiScores = audit.kpiScores;
  }

  let kpiNotes = {};
  if (typeof audit.kpiNotesJson === 'string' && audit.kpiNotesJson.startsWith('{')) {
    try { kpiNotes = JSON.parse(audit.kpiNotesJson); } catch (e) {}
  } else if (typeof audit.kpiNotes === 'object' && audit.kpiNotes !== null) {
    kpiNotes = audit.kpiNotes;
  }

  let kpiPhotos = {};
  if (typeof audit.kpiPhotosJson === 'string' && audit.kpiPhotosJson.startsWith('{')) {
    try { kpiPhotos = JSON.parse(audit.kpiPhotosJson); } catch (e) {}
  } else if (typeof audit.kpiPhotos === 'object' && audit.kpiPhotos !== null) {
    kpiPhotos = audit.kpiPhotos;
  }

  const photosMap = (audit.photosMap && typeof audit.photosMap === 'object') ? audit.photosMap : ((audit.photos && typeof audit.photos === 'object') ? audit.photos : {});
  const uploadedUrls = {};

  const vKeys = [
    'kendaraan_depan', 'kendaraan_samping_kiri', 'kendaraan_interior_depan_kiri',
    'kendaraan_interior_belakang_kiri', 'kendaraan_belakang', 'kendaraan_interior_belakang',
    'kendaraan_samping_kanan'
  ];
  for (let vi = 0; vi < vKeys.length; vi++) {
    const vk = vKeys[vi];
    const data = photosMap[vk] || kpiPhotos[vk] || audit['photo' + (vi + 1)] || '';
    if (data && data.length > 50 && !data.startsWith('http')) {
      uploadedUrls[vk] = uploadImage(data, 'KENDARAAN_' + (targetNoMobil || 'ARMADA') + '_' + vk + '_' + Date.now() + '.jpg');
    } else if (data && data.startsWith('http')) {
      uploadedUrls[vk] = data;
    }
  }

  // Pembersihan otomatis jika poin 0
  for (let ki = 0; ki < KPI_ITEM_IDS.length; ki++) {
    const itId = KPI_ITEM_IDS[ki];
    const itPt = (kpiScores[itId] !== undefined && kpiScores[itId] !== null) ? Number(kpiScores[itId]) : 0;
    if (itPt === 0) {
      for (const pk in photosMap) {
        if (pk === itId || pk.indexOf(itId + '_') === 0) {
          delete photosMap[pk];
        }
      }
      for (const pk in kpiPhotos) {
        if (pk === itId || pk.indexOf(itId + '_') === 0) {
          delete kpiPhotos[pk];
        }
      }
      for (const nk in kpiNotes) {
        if (nk === itId || nk.indexOf(itId + '_') === 0) {
          delete kpiNotes[nk];
        }
      }
    }
  }

  const allPhotoKeys = Object.keys(photosMap).concat(Object.keys(kpiPhotos));
  const seenKeys = {};
  for (let pi = 0; pi < allPhotoKeys.length; pi++) {
    const pk = allPhotoKeys[pi];
    if (seenKeys[pk]) continue;
    seenKeys[pk] = true;
    if (vKeys.indexOf(pk) !== -1) continue;
    const pData = photosMap[pk] || kpiPhotos[pk] || '';
    if (pData && pData.length > 50 && !pData.startsWith('http')) {
      uploadedUrls[pk] = uploadImage(pData, 'TEMUAN_' + (targetNoMobil || 'ARMADA') + '_' + pk + '_' + Date.now() + '.jpg');
    } else if (pData && pData.startsWith('http')) {
      uploadedUrls[pk] = pData;
    }
  }

  for (let li = 8; li <= 20; li++) {
    const lData = audit['photo' + li];
    if (lData && lData.length > 50 && !lData.startsWith('http')) {
      const up = uploadImage(lData, 'LEGACY_' + (targetNoMobil || 'ARMADA') + '_P' + li + '_' + Date.now() + '.jpg');
      uploadedUrls['legacy_' + li] = up;
    } else if (lData && lData.startsWith('http')) {
      uploadedUrls['legacy_' + li] = lData;
    }
  }

  // KUNCI SCRIPT (LOCK SERVICE) UNTUK PENULISAN DATABASE GOOGLE SHEETS
  // Memastikan transaksi database atomik: banyak user yang submit serentak tidak saling tumpang tindih
  return withScriptLock(function() {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    const sheet = findSheetFlexible(ss, 'INPUT_AUDIT', 'INPUT AUDIT', 'Input Audit', 'DATA_AUDIT', 'DATA AUDIT', 'Data Audit', 'Audit') || ss.insertSheet('INPUT_AUDIT');

    // 1. Pastikan Header A1 s/d LK1 tersedia
    if (sheet.getLastRow() === 0) {
      sheet.appendRow(INPUT_AUDIT_COLUMNS);
    } else if (sheet.getLastColumn() < INPUT_AUDIT_COLUMNS.length) {
      sheet.getRange(1, 1, 1, INPUT_AUDIT_COLUMNS.length).setValues([INPUT_AUDIT_COLUMNS]);
    }

    const currentLastCol = sheet.getLastColumn();
    const currentLastRow = sheet.getLastRow();
    const existingHeaders = (currentLastCol > 0 && currentLastRow > 0)
      ? sheet.getRange(1, 1, 1, currentLastCol).getValues()[0].map(function(h) { return String(h || '').trim(); })
      : [];

    const totalCols = Math.max(existingHeaders.length, INPUT_AUDIT_COLUMNS.length);

    const targetKodeUnik = String(audit.kodeUnik || audit.id || '').trim();
    const isExplicitKodeUnik = targetKodeUnik !== '' && !targetKodeUnik.match(/^\d+$/);
    const targetCloudId = (audit.cloudId && !isNaN(Number(audit.cloudId)) && Number(audit.cloudId) > 0)
      ? Number(audit.cloudId)
      : ((audit.id && !isNaN(Number(audit.id)) && Number(audit.id) > 0 && audit.isEdit) ? Number(audit.id) : 0);
    const targetAuditUuid = String(audit.auditUuid || audit.uuid || '').trim();
    const targetTimestamp = String(audit.waktu || audit.timestamp || '').trim();
    const targetNip = String(audit.nip || '').trim();
    const isExplicitEdit = audit.isEdit === true && (targetCloudId > 0 || isExplicitKodeUnik);

    let existingRowIndex = -1;
    let existingRowValues = null;

    // 2. Cari baris yang sudah ada jika ini adalah update/edit atau retry idempotent
    if (currentLastRow > 1) {
      const dataColCount = Math.min(currentLastCol, 15);
      const dataRange = sheet.getRange(2, 1, currentLastRow - 1, dataColCount).getValues();

      for (let i = 0; i < dataRange.length; i++) {
        const row = dataRange[i];
        const rIndex = i + 2;
        const rowIdRaw = String(row[0] || '').trim();
        const rowIdNum = Number(row[0]) || 0;
        const rowTanggal = String(row[1] || '').trim();
        const rowWaktu = String(row[2] || '').trim();
        const rowNip = String(row[3] || '').trim();
        const rowNoMobil = String(row[6] || '').trim().toUpperCase();

        // Cocokkan dengan Kode Unik
        if (isExplicitKodeUnik && rowIdRaw.toUpperCase() === targetKodeUnik.toUpperCase()) {
          existingRowIndex = rIndex;
          existingRowValues = row;
          break;
        }

        // Cocokkan dengan Cloud ID eksplisit jika user memang sedang mengedit audit lama
        if (isExplicitEdit && targetCloudId > 0 && rowIdNum === targetCloudId) {
          existingRowIndex = rIndex;
          existingRowValues = row;
          break;
        }

        // Idempotensi: deteksi pengiriman ulang yang sama (timestamp, nip, noMobil cocok persis)
        if (targetTimestamp && rowWaktu === targetTimestamp && targetNip && rowNip === targetNip && targetNoMobil && rowNoMobil === targetNoMobil) {
          existingRowIndex = rIndex;
          existingRowValues = row;
          break;
        }
      }
    }

    // 3. Tentukan baris target dan ID record (Kode Unik)
    const isNewRecord = existingRowIndex <= 0;
    const targetRow = isNewRecord ? (currentLastRow + 1) : existingRowIndex;
    ensureSheetCapacity(sheet, totalCols, targetRow + 1);

    let recordUniqueId = '';
    if (isExplicitKodeUnik) {
      recordUniqueId = targetKodeUnik;
    } else if (!isNewRecord && existingRowValues && String(existingRowValues[0] || '').trim() !== '' && !String(existingRowValues[0]).trim().match(/^\d+$/)) {
      recordUniqueId = String(existingRowValues[0]).trim();
    } else if (targetAuditUuid && targetAuditUuid.indexOf('AUD-') === 0) {
      recordUniqueId = targetAuditUuid;
    } else {
      const cleanPrefix = 'AUD';
      const timeBase = (Date.now() % 10000000).toString(36).toUpperCase();
      const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
      let randPart = '';
      for (let i = 0; i < 4; i++) {
        randPart += chars.charAt(Math.floor(Math.random() * chars.length));
      }
      recordUniqueId = cleanPrefix + '-' + timeBase + randPart;
    }

    const recordNumericId = isNewRecord ? (targetRow - 1) : (existingRowValues && !isNaN(Number(existingRowValues[0])) ? Number(existingRowValues[0]) : (targetCloudId || (targetRow - 1)));

    let timestamp = '';
    if (audit.waktu && String(audit.waktu).indexOf(':') !== -1) {
      timestamp = String(audit.waktu);
    } else if (audit.timestamp && typeof audit.timestamp === 'string' && audit.timestamp.indexOf(':') !== -1) {
      timestamp = audit.timestamp;
    } else if (audit.timestamp && !isNaN(Number(audit.timestamp)) && Number(audit.timestamp) > 1000000000) {
      timestamp = Utilities.formatDate(new Date(Number(audit.timestamp)), 'GMT+7', 'dd/MM/yyyy HH:mm:ss');
    } else if (!isNewRecord && existingRowValues && existingRowValues[2]) {
      timestamp = String(existingRowValues[2]);
    } else {
      timestamp = Utilities.formatDate(new Date(), 'GMT+7', 'dd/MM/yyyy HH:mm:ss');
    }

    let tanggal = audit.tanggal || (!isNewRecord && existingRowValues ? existingRowValues[1] : Utilities.formatDate(new Date(), 'GMT+7', 'dd/MM/yyyy'));
    if (tanggal) {
      const strTgl = String(tanggal).trim();
      if (strTgl.indexOf(' ') > 0) {
        tanggal = strTgl.split(' ')[0];
      } else if (strTgl.indexOf('T') > 0) {
        tanggal = strTgl.split('T')[0];
      }
    }
    const serviceDate = audit.tanggalService || audit.sampleTanggal || (!isNewRecord && existingRowValues ? existingRowValues[20] : '');
    const paketVal = audit.paketService || audit.service || (!isNewRecord && existingRowValues ? existingRowValues[21] : '');

    let calculatedPoints = 0;
    for (let k in kpiScores) {
      if (kpiScores.hasOwnProperty(k)) {
        const v = Number(kpiScores[k]);
        if (!isNaN(v) && v > 0) calculatedPoints += v;
      }
    }

    const totalPoints = (audit.totalPoinTemuan !== undefined && audit.totalPoinTemuan !== null && !isNaN(Number(audit.totalPoinTemuan)))
      ? Number(audit.totalPoinTemuan)
      : ((audit.point !== undefined && audit.point !== null && !isNaN(Number(audit.point)))
          ? Number(audit.point)
          : ((audit.poin !== undefined && audit.poin !== null && !isNaN(Number(audit.poin)))
              ? Number(audit.poin)
              : calculatedPoints));

    const rowData = new Array(totalCols);
    for (let ci = 0; ci < rowData.length; ci++) rowData[ci] = '';

    rowData[0] = recordUniqueId;
    rowData[1] = tanggal;
    rowData[2] = timestamp;
    rowData[3] = audit.nip || (!isNewRecord && existingRowValues ? existingRowValues[3] : '');
    rowData[4] = audit.namaPetugas || audit.namaAuditor || (!isNewRecord && existingRowValues ? existingRowValues[4] : '');
    rowData[5] = audit.periode || (!isNewRecord && existingRowValues ? existingRowValues[5] : '');
    rowData[6] = targetNoMobil || (!isNewRecord && existingRowValues ? existingRowValues[6] : '');
    rowData[7] = audit.platNomer || (!isNewRecord && existingRowValues ? existingRowValues[7] : '');
    rowData[8] = audit.tipe || (!isNewRecord && existingRowValues ? existingRowValues[8] : '');
    rowData[9] = audit.warna || (!isNewRecord && existingRowValues ? existingRowValues[9] : '');
    rowData[10] = audit.tahun || (!isNewRecord && existingRowValues ? existingRowValues[10] : '');
    rowData[11] = audit.pool || (!isNewRecord && existingRowValues ? existingRowValues[11] : '');

    for (let vi = 0; vi < vKeys.length; vi++) {
      const vk = vKeys[vi];
      const colIdx = 12 + vi;
      const existingVal = (!isNewRecord && existingRowValues && existingRowValues.length > colIdx) ? existingRowValues[colIdx] : '';
      const finalVal = uploadedUrls[vk] || photosMap[vk] || kpiPhotos[vk] || audit['photo' + (vi + 1)] || existingVal || '';
      rowData[colIdx] = finalVal;
    }

    rowData[19] = audit.jenisSample || (!isNewRecord && existingRowValues ? existingRowValues[19] : '');
    rowData[20] = serviceDate;
    rowData[21] = paketVal;
    rowData[22] = totalPoints;

    let kpiItemIdx = -1;
    let photoCounter = 0;
    let currentItemPoint = 0;

    for (let ci = 23; ci < INPUT_AUDIT_COLUMNS.length; ci++) {
      const colName = INPUT_AUDIT_COLUMNS[ci];
      const existingVal = (!isNewRecord && existingRowValues && existingRowValues.length > ci) ? existingRowValues[ci] : '';

      if (colName.endsWith('_POINT')) {
        kpiItemIdx++;
        photoCounter = 0;
        const curItemId = KPI_ITEM_IDS[kpiItemIdx];
        const pt = (kpiScores[curItemId] !== undefined && kpiScores[curItemId] !== null) ? Number(kpiScores[curItemId]) : (existingVal !== '' ? Number(existingVal) || 0 : 0);
        currentItemPoint = pt;
        rowData[ci] = pt;
      } else if (colName.startsWith('FOTO_') || colName.startsWith('PHOTO_')) {
        const curItemId = KPI_ITEM_IDS[kpiItemIdx];
        const slotKey = curItemId + '_' + photoCounter;
        if (currentItemPoint === 0 || photoCounter >= currentItemPoint) {
          rowData[ci] = '';
        } else {
          const url = uploadedUrls[slotKey] || photosMap[slotKey] || kpiPhotos[slotKey] || '';
          rowData[ci] = url;
        }
      } else if (colName.startsWith('CATATAN_') || colName.startsWith('CATATN_')) {
        const curItemId = KPI_ITEM_IDS[kpiItemIdx];
        const slotKey = curItemId + '_' + photoCounter;
        if (currentItemPoint === 0 || photoCounter >= currentItemPoint) {
          rowData[ci] = '';
        } else {
          const note = (kpiNotes[slotKey] !== undefined && kpiNotes[slotKey] !== null) ? kpiNotes[slotKey] : '';
          rowData[ci] = note;
        }
        photoCounter++;
      }
    }

    const rawPayload = {
      auditUuid: targetAuditUuid,
      kpiScores: kpiScores,
      kpiNotes: kpiNotes,
      kpiPhotos: kpiPhotos,
      rincian: audit.rincianPoinTemuan || ''
    };
    const rawJsonIdx = existingHeaders.indexOf('RAW_JSON');
    if (rawJsonIdx !== -1) {
      rowData[rawJsonIdx] = JSON.stringify(rawPayload);
    }
    const uuidIdx = existingHeaders.indexOf('AUDIT_UUID');
    if (uuidIdx !== -1) {
      rowData[uuidIdx] = targetAuditUuid;
    }

    // Dynamic Header Matching
    for (let hi = 0; hi < existingHeaders.length; hi++) {
      const rawH = existingHeaders[hi];
      if (!rawH) continue;
      const cleanH = rawH.toLowerCase().replace(/[^a-z0-9]/g, '');

      if (cleanH === 'id' || cleanH === 'kode' || cleanH === 'kodeunik') {
        rowData[hi] = recordUniqueId;
      } else if (cleanH === 'point' || cleanH === 'poin' || cleanH === 'totalpoin' || cleanH === 'totalpoint' ||
          cleanH === 'totalpointemuan' || cleanH === 'pointemuan' || cleanH === 'nilaipoint' || cleanH === 'nilaipoin') {
        rowData[hi] = totalPoints;
      } else if (cleanH === 'rincian' || cleanH === 'rincianpoint' || cleanH === 'rincianpoin' || cleanH === 'rincianpointemuan') {
        rowData[hi] = audit.rincianPoinTemuan || '';
      } else if (cleanH === 'jenissample' || cleanH === 'sample') {
        rowData[hi] = audit.jenisSample || rowData[hi] || '';
      } else if (cleanH === 'tanggalservice' || cleanH === 'tglservice' || cleanH === 'sampletanggal' || cleanH === 'tanggalsample') {
        rowData[hi] = serviceDate || rowData[hi] || '';
      } else if (cleanH === 'paketservice' || cleanH === 'peketservice' || cleanH === 'service') {
        rowData[hi] = paketVal || rowData[hi] || '';
      } else if (cleanH === 'nokendaraan' || cleanH === 'nomobil' || cleanH === 'nopolisi' || cleanH === 'armada') {
        rowData[hi] = targetNoMobil || rowData[hi] || '';
      } else if (cleanH === 'platnomer' || cleanH === 'platnomor') {
        rowData[hi] = audit.platNomer || rowData[hi] || '';
      } else if (cleanH === 'periode') {
        rowData[hi] = audit.periode || rowData[hi] || '';
      } else if (cleanH === 'nip') {
        rowData[hi] = audit.nip || rowData[hi] || '';
      } else if (cleanH === 'namaauditor' || cleanH === 'namapetugas') {
        rowData[hi] = audit.namaPetugas || audit.namaAuditor || rowData[hi] || '';
      } else if (cleanH === 'tipe') {
        rowData[hi] = audit.tipe || rowData[hi] || '';
      } else if (cleanH === 'warna') {
        rowData[hi] = audit.warna || rowData[hi] || '';
      } else if (cleanH === 'tahun') {
        rowData[hi] = audit.tahun || rowData[hi] || '';
      } else if (cleanH === 'pool') {
        rowData[hi] = audit.pool || rowData[hi] || '';
      } else if (cleanH === 'waktu' || cleanH === 'timestamp' || cleanH === 'waktutimestamp') {
        rowData[hi] = timestamp || rowData[hi] || '';
      } else if (cleanH === 'tanggal') {
        rowData[hi] = tanggal || rowData[hi] || '';
      } else if (cleanH === 'audituuid' || cleanH === 'uuid') {
        rowData[hi] = targetAuditUuid || rowData[hi] || '';
      }
    }

    sheet.getRange(targetRow, 1, 1, rowData.length).setValues([rowData]);
    SpreadsheetApp.flush();
    return {
      status: 'success',
      id: recordUniqueId,
      kodeUnik: recordUniqueId,
      cloudId: recordNumericId,
      auditUuid: targetAuditUuid || recordUniqueId,
      timestamp: timestamp,
      mode: isNewRecord ? 'inserted' : 'updated',
      row: targetRow
    };
  }, 45000);
}

/**
 * Memperbaiki hak akses seluruh foto di Google Drive agar dapat dilihat secara publik
 */
function fixPhotoPermissions() {
  let count = 0;
  try {
    let folder = null;
    if (FOLDER_ID && String(FOLDER_ID).trim() !== '') {
      try { folder = DriveApp.getFolderById(String(FOLDER_ID).trim()); } catch (e) {}
    }
    if (!folder) {
      const folders = DriveApp.getFoldersByName('FOTO_AUDIT_KENDARAAN');
      if (folders.hasNext()) folder = folders.next();
    }
    if (folder) {
      try {
        folder.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      } catch (fe) {}
      const files = folder.getFiles();
      while (files.hasNext()) {
        const file = files.next();
        try {
          file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
          count++;
        } catch (shareErr) {}
      }
    }
  } catch (err) {}
  return { status: 'success', sharedCount: count };
}

/**
 * Hapus Data Audit dari INPUT_AUDIT
 */
function deleteAuditRecord(id, noMobil, periode, kodeUnik) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'INPUT_AUDIT', 'INPUT AUDIT', 'Input Audit', 'DATA_AUDIT', 'DATA AUDIT', 'Data Audit', 'Audit');
  if (!sheet) return { deleted: false, message: 'Sheet INPUT_AUDIT tidak ditemukan' };
  const lastRow = sheet.getLastRow();
  if (lastRow <= 1) return { deleted: false, message: 'Data kosong' };

  const targetId = id ? String(id).trim() : '';
  const targetKodeUnik = kodeUnik ? String(kodeUnik).trim() : '';
  const targetNoMobil = noMobil ? String(noMobil).trim().toUpperCase() : '';
  const targetPeriode = periode ? String(periode).trim().toUpperCase() : '';

  const dataRange = sheet.getRange(2, 1, lastRow - 1, Math.min(sheet.getLastColumn(), 15)).getValues();
  for (let i = 0; i < dataRange.length; i++) {
    const rowId = String(dataRange[i][0]).trim();
    const rowNoMobil = String(dataRange[i][6]).trim().toUpperCase();
    const rowPeriodeNew = (dataRange[i].length > 5) ? String(dataRange[i][5]).trim().toUpperCase() : '';
    const rowPeriodeOld = (dataRange[i].length > 3) ? String(dataRange[i][3]).trim().toUpperCase() : '';

    if ((targetId && rowId === targetId) || (targetKodeUnik && rowId === targetKodeUnik) || (targetNoMobil && rowNoMobil === targetNoMobil && targetPeriode && (rowPeriodeNew === targetPeriode || rowPeriodeOld === targetPeriode))) {
      sheet.deleteRow(i + 2);
      return { deleted: true, row: i + 2 };
    }
  }
  return { deleted: false, message: 'Data tidak ditemukan' };
}

function saveMasterKendaraan(v) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'MASTER_KENDARAAN', 'MASTER KENDARAAN', 'Master Kendaraan', 'Kendaraan') || ss.insertSheet('MASTER_KENDARAAN');

  const lastRow = sheet.getLastRow();
  const targetId = v.id ? String(v.id).trim() : '';
  const targetNoMobil = v.noMobil ? String(v.noMobil).trim().toUpperCase() : '';

  let existingRow = -1;
  if (lastRow > 1) {
    const data = sheet.getRange(2, 1, lastRow - 1, 2).getValues();
    for (let i = 0; i < data.length; i++) {
      const rowId = String(data[i][0]).trim();
      const rowNoMobil = String(data[i][1]).trim().toUpperCase();
      if ((targetId && targetId !== '0' && rowId === targetId) || (targetNoMobil && rowNoMobil === targetNoMobil)) {
        existingRow = i + 2;
        break;
      }
    }
  }

  const id = existingRow > 0 ? (sheet.getRange(existingRow, 1).getValue() || targetId) : (targetId && targetId !== '0' ? targetId : lastRow);
  const rowData = [
    id,
    v.noMobil || '',
    v.platNomer || '',
    v.tipe || '',
    v.warna || '',
    v.pool || '',
    v.tahun || '',
    v.tanggalService || '',
    v.service || ''
  ];

  if (existingRow > 0) {
    sheet.getRange(existingRow, 1, 1, rowData.length).setValues([rowData]);
    return { mode: 'updated', row: existingRow, id: id };
  } else {
    sheet.appendRow(rowData);
    return { mode: 'inserted', row: sheet.getLastRow(), id: id };
  }
}

function deleteMasterKendaraan(id, noMobil) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'MASTER_KENDARAAN', 'MASTER KENDARAAN', 'Master Kendaraan', 'Kendaraan');
  if (!sheet) return { deleted: false };
  const lastRow = sheet.getLastRow();
  if (lastRow <= 1) return { deleted: false };

  const targetId = id ? String(id).trim() : '';
  const targetNoMobil = noMobil ? String(noMobil).trim().toUpperCase() : '';
  const data = sheet.getRange(2, 1, lastRow - 1, 2).getValues();
  for (let i = 0; i < data.length; i++) {
    const rowId = String(data[i][0]).trim();
    const rowNoMobil = String(data[i][1]).trim().toUpperCase();
    if ((targetId && rowId === targetId) || (targetNoMobil && rowNoMobil === targetNoMobil)) {
      sheet.deleteRow(i + 2);
      return { deleted: true, row: i + 2 };
    }
  }
  return { deleted: false };
}

/**
 * Simpan / Update Master Pool
 */
function saveMasterPool(p) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'MASTER_POOL', 'MASTER POOL', 'Master Pool', 'Pool') || ss.insertSheet('MASTER_POOL');

  const lastRow = sheet.getLastRow();
  const targetId = p.id ? String(p.id).trim() : '';
  const targetKode = p.kodePool ? String(p.kodePool).trim().toUpperCase() : '';

  let existingRow = -1;
  if (lastRow > 1) {
    const data = sheet.getRange(2, 1, lastRow - 1, 2).getValues();
    for (let i = 0; i < data.length; i++) {
      const rowId = String(data[i][0]).trim();
      const rowKode = String(data[i][1]).trim().toUpperCase();
      if ((targetId && targetId !== '0' && rowId === targetId) || (targetKode && rowKode === targetKode)) {
        existingRow = i + 2;
        break;
      }
    }
  }

  const id = existingRow > 0 ? (sheet.getRange(existingRow, 1).getValue() || targetId) : (targetId && targetId !== '0' ? targetId : lastRow);
  const rowData = [
    id,
    p.kodePool || '',
    p.namaPool || '',
    p.alamatPool || '',
    p.statusArea || 'JABODETABEK'
  ];

  if (existingRow > 0) {
    sheet.getRange(existingRow, 1, 1, rowData.length).setValues([rowData]);
    return { mode: 'updated', row: existingRow, id: id };
  } else {
    sheet.appendRow(rowData);
    return { mode: 'inserted', row: sheet.getLastRow(), id: id };
  }
}

function deleteMasterPool(id, kodePool) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'MASTER_POOL', 'MASTER POOL', 'Master Pool', 'Pool');
  if (!sheet) return { deleted: false };
  const lastRow = sheet.getLastRow();
  if (lastRow <= 1) return { deleted: false };

  const targetId = id ? String(id).trim() : '';
  const targetKode = kodePool ? String(kodePool).trim().toUpperCase() : '';
  const data = sheet.getRange(2, 1, lastRow - 1, 2).getValues();
  for (let i = 0; i < data.length; i++) {
    const rowId = String(data[i][0]).trim();
    const rowKode = String(data[i][1]).trim().toUpperCase();
    if ((targetId && rowId === targetId) || (targetKode && rowKode === targetKode)) {
      sheet.deleteRow(i + 2);
      return { deleted: true, row: i + 2 };
    }
  }
  return { deleted: false };
}

/**
 * Simpan / Update User
 */
function saveMasterUser(u) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'USER', 'USERS', 'User') || ss.insertSheet('USER');

  const lastRow = sheet.getLastRow();
  const targetNip = u.nip ? String(u.nip).trim() : '';
  const targetUsername = u.username ? String(u.username).trim().toLowerCase() : '';

  let existingRow = -1;
  if (lastRow > 1) {
    const data = sheet.getRange(2, 1, lastRow - 1, 3).getValues();
    for (let i = 0; i < data.length; i++) {
      const rowNip = String(data[i][0]).trim();
      const rowUsername = String(data[i][2]).trim().toLowerCase();
      if ((targetNip && rowNip === targetNip) || (targetUsername && rowUsername === targetUsername)) {
        existingRow = i + 2;
        break;
      }
    }
  }

  const rowData = [
    targetNip || (existingRow > 0 ? sheet.getRange(existingRow, 1).getValue() : String(Date.now())),
    u.namaPetugas || '',
    u.username || '',
    u.password || '123',
    u.levelAkses || 'AUDITOR',
    u.kodePool || 'SKT',
    u.status || 'AKTIF',
    u.tanggalAktif || ''
  ];

  if (existingRow > 0) {
    sheet.getRange(existingRow, 1, 1, rowData.length).setValues([rowData]);
    return { mode: 'updated', row: existingRow, nip: targetNip };
  } else {
    sheet.appendRow(rowData);
    return { mode: 'inserted', row: sheet.getLastRow(), nip: targetNip };
  }
}

function deleteMasterUser(nip) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'USER', 'USERS', 'User');
  if (!sheet) return { deleted: false };
  const lastRow = sheet.getLastRow();
  if (lastRow <= 1) return { deleted: false };

  const targetNip = nip ? String(nip).trim() : '';
  const data = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
  for (let i = 0; i < data.length; i++) {
    const rowNip = String(data[i][0]).trim();
    if (targetNip && rowNip === targetNip) {
      sheet.deleteRow(i + 2);
      return { deleted: true, row: i + 2 };
    }
  }
  return { deleted: false };
}

/**
 * Simpan / Update Bobot KPI
 * Jika Sheet MASTER_BOBOT belum ada, otomatis dibuat dengan header yang sesuai.
 */
function saveMasterBobot(b) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  let sheet = findSheetFlexible(ss, 'MASTER_BOBOT', 'MASTER BOBOT', 'Master Bobot', 'Bobot');
  if (!sheet) {
    sheet = ss.insertSheet('MASTER_BOBOT');
    sheet.appendRow(['ITEM_ID', 'ITEM_NAME', 'SECTION_TITLE', 'GROUP_TITLE', 'POINT_MAX', 'BOBOT']);
    sheet.getRange(1, 1, 1, 6).setFontWeight("bold").setBackground("#e2e8f0");
  }

  const lastRow = sheet.getLastRow();
  const targetItemId = b.itemId ? String(b.itemId).trim() : '';
  let existingRow = -1;
  if (lastRow > 1) {
    const data = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
    for (let i = 0; i < data.length; i++) {
      if (String(data[i][0]).trim() === targetItemId) {
        existingRow = i + 2;
        break;
      }
    }
  }

  const rowData = [
    targetItemId,
    b.itemName || '',
    b.sectionTitle || '',
    b.groupTitle || '',
    b.pointMax || 1,
    b.bobot !== undefined ? Number(b.bobot) : 1.0
  ];

  if (existingRow > 0) {
    sheet.getRange(existingRow, 1, 1, rowData.length).setValues([rowData]);
    return { mode: 'updated', row: existingRow, itemId: targetItemId };
  } else {
    sheet.appendRow(rowData);
    return { mode: 'inserted', row: sheet.getLastRow(), itemId: targetItemId };
  }
}

/**
 * Simpan / Update Seluruh Master Bobot Sekaligus (Batch Sync)
 * Jika Sheet MASTER_BOBOT belum ada atau kosong, sheet dibuat dan diisi langsung.
 */
function saveAllMasterBobot(bobotList) {
  if (!bobotList || !bobotList.length) return { status: 'error', message: 'Daftar bobot kosong' };

  const ss = SpreadsheetApp.openById(SHEET_ID);
  let sheet = findSheetFlexible(ss, 'MASTER_BOBOT', 'MASTER BOBOT', 'Master Bobot', 'Bobot');
  if (!sheet) {
    sheet = ss.insertSheet('MASTER_BOBOT');
  }

  const lastRow = sheet.getLastRow();
  if (lastRow <= 1) {
    sheet.clear();
    const rows = [
      ['ITEM_ID', 'ITEM_NAME', 'SECTION_TITLE', 'GROUP_TITLE', 'POINT_MAX', 'BOBOT']
    ];
    bobotList.forEach(function(b) {
      rows.push([
        b.itemId ? String(b.itemId).trim() : '',
        b.itemName || '',
        b.sectionTitle || '',
        b.groupTitle || '',
        b.pointMax || 1,
        b.bobot !== undefined ? Number(b.bobot) : 1.0
      ]);
    });
    sheet.getRange(1, 1, rows.length, 6).setValues(rows);
    sheet.getRange(1, 1, 1, 6).setFontWeight("bold").setBackground("#e2e8f0");
    return { mode: 'created', count: bobotList.length };
  }

  const existingValues = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
  const existingMap = {};
  for (let i = 0; i < existingValues.length; i++) {
    existingMap[String(existingValues[i][0]).trim()] = i + 2;
  }

  let updatedCount = 0;
  const insertedRows = [];

  bobotList.forEach(function(b) {
    const itemId = b.itemId ? String(b.itemId).trim() : '';
    if (!itemId) return;

    const rowData = [
      itemId,
      b.itemName || '',
      b.sectionTitle || '',
      b.groupTitle || '',
      b.pointMax || 1,
      b.bobot !== undefined ? Number(b.bobot) : 1.0
    ];

    if (existingMap[itemId]) {
      const targetRow = existingMap[itemId];
      sheet.getRange(targetRow, 1, 1, 6).setValues([rowData]);
      updatedCount++;
    } else {
      insertedRows.push(rowData);
    }
  });

  if (insertedRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, insertedRows.length, 6).setValues(insertedRows);
  }

  return { mode: 'merged', updated: updatedCount, inserted: insertedRows.length, total: bobotList.length };
}

function deleteMasterBobot(itemId) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'MASTER_BOBOT', 'MASTER BOBOT', 'Master Bobot', 'Bobot');
  if (!sheet) return { deleted: false };
  const lastRow = sheet.getLastRow();
  if (lastRow <= 1) return { deleted: false };
  const target = itemId ? String(itemId).trim() : '';
  const data = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
  for (let i = 0; i < data.length; i++) {
    if (String(data[i][0]).trim() === target) {
      sheet.deleteRow(i + 2);
      return { deleted: true, row: i + 2 };
    }
  }
  return { deleted: false };
}

/**
 * Simpan / Update Sample Area
 */
function saveMasterSample(s) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  let sheet = findSheetFlexible(ss, 'MASTER_SAMPLE', 'MASTER SAMPLE', 'Master Sample', 'Sample', 'SAMPLE', 'DATA_SAMPLE', 'DATA SAMPLE') || ss.insertSheet('MASTER_SAMPLE');

  if (sheet.getLastRow() === 0) {
    sheet.appendRow(['STATUS_AREA', 'JUMLAH_SAMPLE', 'KETERANGAN']);
  }

  const lastRow = sheet.getLastRow();
  const targetArea = s.statusArea ? String(s.statusArea).trim().toUpperCase() : '';
  let existingRow = -1;
  if (lastRow > 1) {
    const data = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
    for (let i = 0; i < data.length; i++) {
      if (String(data[i][0]).trim().toUpperCase() === targetArea) {
        existingRow = i + 2;
        break;
      }
    }
  }

  const rowData = [
    targetArea,
    parseInt(s.jumlahSample || 10, 10),
    s.keterangan || ('Standar sample armada area ' + targetArea)
  ];

  if (existingRow > 0) {
    sheet.getRange(existingRow, 1, 1, rowData.length).setValues([rowData]);
    return { mode: 'updated', row: existingRow, statusArea: targetArea };
  } else {
    sheet.appendRow(rowData);
    return { mode: 'inserted', row: sheet.getLastRow(), statusArea: targetArea };
  }
}

function deleteMasterSample(statusArea) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'MASTER_SAMPLE', 'MASTER SAMPLE', 'Master Sample', 'Sample', 'SAMPLE', 'DATA_SAMPLE', 'DATA SAMPLE');
  if (!sheet) return { deleted: false };
  const lastRow = sheet.getLastRow();
  if (lastRow <= 1) return { deleted: false };

  const targetArea = statusArea ? String(statusArea).trim().toUpperCase() : '';
  const data = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
  for (let i = 0; i < data.length; i++) {
    if (String(data[i][0]).trim().toUpperCase() === targetArea) {
      sheet.deleteRow(i + 2);
      return { deleted: true, row: i + 2 };
    }
  }
  return { deleted: false };
}

/**
 * Simpan / Update Periode
 */
function saveMasterPeriode(p) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'MASTER_PERIODE', 'MASTER PERIODE', 'Master Periode', 'Periode') || ss.insertSheet('MASTER_PERIODE');

  const lastRow = sheet.getLastRow();
  const targetId = p.id ? String(p.id).trim() : '';
  const targetPeriode = p.periode ? String(p.periode).trim() : '';

  let existingRow = -1;
  if (lastRow > 1) {
    const data = sheet.getRange(2, 1, lastRow - 1, 2).getValues();
    for (let i = 0; i < data.length; i++) {
      const rowId = String(data[i][0]).trim();
      const rowPeriode = String(data[i][1]).trim();
      if ((targetId && targetId !== '0' && rowId === targetId) || (targetPeriode && rowPeriode.toLowerCase() === targetPeriode.toLowerCase())) {
        existingRow = i + 2;
        break;
      }
    }
  }

  const id = existingRow > 0 ? (sheet.getRange(existingRow, 1).getValue() || targetId) : (targetId && targetId !== '0' ? targetId : lastRow);
  const rowData = [
    id,
    p.periode || '',
    p.keterangan || ''
  ];

  if (existingRow > 0) {
    sheet.getRange(existingRow, 1, 1, rowData.length).setValues([rowData]);
    return { mode: 'updated', row: existingRow, id: id };
  } else {
    sheet.appendRow(rowData);
    return { mode: 'inserted', row: sheet.getLastRow(), id: id };
  }
}

function deleteMasterPeriode(id, periode) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'MASTER_PERIODE', 'MASTER PERIODE', 'Master Periode', 'Periode');
  if (!sheet) return { deleted: false };
  const lastRow = sheet.getLastRow();
  if (lastRow <= 1) return { deleted: false };

  const targetId = id ? String(id).trim() : '';
  const targetPeriode = periode ? String(periode).trim().toLowerCase() : '';
  const data = sheet.getRange(2, 1, lastRow - 1, 2).getValues();
  for (let i = 0; i < data.length; i++) {
    const rowId = String(data[i][0]).trim();
    const rowPeriode = String(data[i][1]).trim().toLowerCase();
    if ((targetId && rowId === targetId) || (targetPeriode && rowPeriode === targetPeriode)) {
      sheet.deleteRow(i + 2);
      return { deleted: true, row: i + 2 };
    }
  }
  return { deleted: false };
}

function saveMasterGradeKpi(g) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'MASTER_GRADE_KPI', 'MASTER GRADE KPI', 'GRADE_KPI', 'GRADE KPI', 'Grade KPI', 'Grade') || ss.insertSheet('MASTER_GRADE_KPI');

  if (sheet.getLastRow() === 0) {
    sheet.appendRow(['KATEGORI', 'MIN_SCORE', 'MAX_SCORE', 'KETERANGAN']);
  }

  const lastRow = sheet.getLastRow();
  const targetKategori = g.kategori ? String(g.kategori).trim().toUpperCase() : '';
  let existingRow = -1;
  if (lastRow > 1) {
    const data = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
    for (let i = 0; i < data.length; i++) {
      if (String(data[i][0]).trim().toUpperCase() === targetKategori) {
        existingRow = i + 2;
        break;
      }
    }
  }

  const parseNum = function(val, defVal) {
    if (val === undefined || val === null || val === '') return defVal;
    if (typeof val === 'number') return isNaN(val) ? defVal : val;
    const clean = String(val).replace(/\s/g, '').replace(',', '.');
    const n = parseFloat(clean);
    return isNaN(n) ? defVal : n;
  };

  const rowData = [
    targetKategori,
    parseNum(g.minScore, 0.0),
    parseNum(g.maxScore, 80.0),
    g.keterangan || ''
  ];

  if (existingRow > 0) {
    sheet.getRange(existingRow, 1, 1, rowData.length).setValues([rowData]);
    try { sheet.getRange(existingRow, 2, 1, 2).setNumberFormat('0.00'); } catch (e) {}
    return { mode: 'updated', row: existingRow, kategori: targetKategori };
  } else {
    sheet.appendRow(rowData);
    try { sheet.getRange(sheet.getLastRow(), 2, 1, 2).setNumberFormat('0.00'); } catch (e) {}
    return { mode: 'inserted', row: sheet.getLastRow(), kategori: targetKategori };
  }
}

function saveAllMasterGradeKpi(gradeList) {
  if (!gradeList || !Array.isArray(gradeList) || gradeList.length === 0) {
    return { count: 0 };
  }
  const ss = SpreadsheetApp.openById(SHEET_ID);
  let sheet = findSheetFlexible(ss, 'MASTER_GRADE_KPI', 'MASTER GRADE KPI', 'GRADE_KPI', 'GRADE KPI', 'Grade KPI', 'Grade');
  if (!sheet) {
    sheet = ss.insertSheet('MASTER_GRADE_KPI');
  }

  sheet.clearContents();
  sheet.appendRow(['KATEGORI', 'MIN_SCORE', 'MAX_SCORE', 'KETERANGAN']);
  try {
    const headerRange = sheet.getRange(1, 1, 1, 4);
    headerRange.setFontWeight('bold');
    headerRange.setFontColor('#FFFFFF');
    headerRange.setBackground('#0D47A1');
    headerRange.setHorizontalAlignment('center');
    sheet.setRowHeight(1, 36);
    sheet.setFrozenRows(1);
    sheet.setColumnWidth(1, 120);
    sheet.setColumnWidth(2, 140);
    sheet.setColumnWidth(3, 140);
    sheet.setColumnWidth(4, 280);
  } catch (e) {}

  const parseNum = function(val, defVal) {
    if (val === undefined || val === null || val === '') return defVal;
    if (typeof val === 'number') return isNaN(val) ? defVal : val;
    const clean = String(val).replace(/\s/g, '').replace(',', '.');
    const n = parseFloat(clean);
    return isNaN(n) ? defVal : n;
  };

  const rows = gradeList.map(function(g) {
    return [
      g.kategori ? String(g.kategori).trim().toUpperCase() : '',
      parseNum(g.minScore, 0.0),
      parseNum(g.maxScore, 80.0),
      g.keterangan || ''
    ];
  });

  if (rows.length > 0) {
    sheet.getRange(2, 1, rows.length, 4).setValues(rows);
    try {
      sheet.getRange(2, 2, rows.length, 2).setNumberFormat('0.00');
      sheet.getRange(2, 1, rows.length, 1).setHorizontalAlignment('center');
    } catch (e) {}
  }
  return { count: rows.length };
}

function fetchAllMasterData() {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  let sheetBobot = findSheetFlexible(ss, 'MASTER_BOBOT', 'MASTER BOBOT', 'Master Bobot', 'Bobot');
  if (!sheetBobot || sheetBobot.getLastRow() <= 1) {
    setupSpreadsheet();
    sheetBobot = findSheetFlexible(ss, 'MASTER_BOBOT', 'MASTER BOBOT', 'Master Bobot', 'Bobot');
  }
  let sheetGrade = findSheetFlexible(ss, 'MASTER_GRADE_KPI', 'MASTER GRADE KPI', 'GRADE_KPI', 'GRADE KPI', 'Grade KPI', 'Grade');
  if (!sheetGrade) {
    initGradeKpiSheet(ss);
    sheetGrade = findSheetFlexible(ss, 'MASTER_GRADE_KPI', 'MASTER GRADE KPI', 'GRADE_KPI', 'GRADE KPI', 'Grade KPI', 'Grade');
  }
  let sheetSample = findSheetFlexible(ss, 'MASTER_SAMPLE', 'MASTER SAMPLE', 'Master Sample', 'Sample', 'SAMPLE', 'DATA_SAMPLE', 'DATA SAMPLE');
  if (!sheetSample || sheetSample.getLastRow() <= 1) {
    setupSpreadsheet();
    sheetSample = findSheetFlexible(ss, 'MASTER_SAMPLE', 'MASTER SAMPLE', 'Master Sample', 'Sample', 'SAMPLE', 'DATA_SAMPLE', 'DATA SAMPLE');
  }
  const sheetKendaraan = findSheetFlexible(ss, 'MASTER_KENDARAAN', 'MASTER KENDARAAN', 'Master Kendaraan', 'Kendaraan', 'Armada', 'Mobil', 'DATA_KENDARAAN', 'DATA KENDARAAN', 'DAFTAR_KENDARAAN', 'DAFTAR KENDARAAN', 'MASTER_ARMADA', 'DATA_ARMADA', 'UNIT', 'DATA_MOBIL', 'MASTER MOBIL');
  // Untuk sheet kendaraan dengan dataset besar (> 300 baris), sinkronisasi dilakukan via 'getKendaraan'
  // dengan format matriks kompak & filter pool agar tidak membebani memori dan kuota eksekusi Google Apps Script.
  const kendaraanList = (sheetKendaraan && sheetKendaraan.getLastRow() <= 300) ? getSheetDataAsObjects(sheetKendaraan) : [];

  return {
    users: getSheetDataAsObjects(findSheetFlexible(ss, 'USER', 'USERS', 'User', 'Master User', 'MASTER_USER')),
    pools: getSheetDataAsObjects(findSheetFlexible(ss, 'MASTER_POOL', 'MASTER POOL', 'Master Pool', 'Pool', 'POOLS')),
    kendaraan: kendaraanList,
    bobot: getSheetDataAsObjects(sheetBobot),
    sample: getSheetDataAsObjects(sheetSample),
    periode: getSheetDataAsObjects(findSheetFlexible(ss, 'MASTER_PERIODE', 'MASTER PERIODE', 'Master Periode', 'Periode')),
    grade_kpi: getSheetDataAsObjects(sheetGrade)
  };
}

function fetchAuditData() {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const rawData = getSheetDataAsObjects(findSheetFlexible(ss, 'INPUT_AUDIT', 'INPUT AUDIT', 'Input Audit', 'DATA_AUDIT', 'DATA AUDIT', 'Data Audit', 'Audit', 'Audits', 'Hasil Audit'));
  // Aturan: Data hasil INPUT AUDIT tidak akan di tampilkan jika di temukan ID yang sama
  const seenIds = {};
  const uniqueData = [];
  for (let i = 0; i < rawData.length; i++) {
    const item = rawData[i];
    const rawId = String(item.ID || item.id || item.KODE_UNIK || item.kodeUnik || item.AUDIT_UUID || item.uuid || '').trim();
    if (rawId !== '') {
      if (seenIds[rawId]) {
        // ID yang sama ditemukan! Tidak akan ditampilkan
        continue;
      }
      seenIds[rawId] = true;
    }
    uniqueData.push(item);
  }
  return uniqueData;
}

function getSheetDataAsObjects(sheet) {
  if (!sheet || sheet.getLastRow() <= 1) return [];
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return [];

  // Deteksi cerdas baris header jika baris pertama adalah judul/kosong
  let headerRowIndex = 0;
  let headers = values[0].map(function(h) { return String(h || '').trim(); });
  let nonBlankCount = headers.filter(function(h) { return h.length > 0; }).length;

  if (values.length > 2 && nonBlankCount <= 2) {
    const nextHeaders = values[1].map(function(h) { return String(h || '').trim(); });
    const nextNonBlank = nextHeaders.filter(function(h) { return h.length > 0; }).length;
    if (nextNonBlank > nonBlankCount) {
      headerRowIndex = 1;
      headers = nextHeaders;
    }
  }

  const results = [];
  for (let r = headerRowIndex + 1; r < values.length; r++) {
    const row = values[r];
    let hasContent = false;
    for (let c = 0; c < headers.length; c++) {
      if (row[c] !== '' && row[c] !== null && row[c] !== undefined) {
        hasContent = true;
        break;
      }
    }
    if (!hasContent) continue;

    const obj = {};
    for (let c = 0; c < headers.length; c++) {
      if (!headers[c]) continue;
      let val = row[c];
      if (val instanceof Date) {
        // Kolom ke 3 (index 2) dengan nama kolom WAKTU/TIMESTAMP menggunakan format tanggal & waktu lengkap
        if (c === 2 || String(headers[c]).toUpperCase().indexOf('WAKTU') !== -1 || String(headers[c]).toUpperCase().indexOf('TIMESTAMP') !== -1) {
          val = Utilities.formatDate(val, 'GMT+7', 'dd/MM/yyyy HH:mm:ss');
        } else {
          val = Utilities.formatDate(val, 'GMT+7', 'dd/MM/yyyy');
        }
      }
      obj[headers[c]] = (val !== undefined && val !== null) ? val : '';
      if (c === 2) {
        obj['WAKTU/TIMESTAMP'] = (val !== undefined && val !== null) ? val : '';
        obj['KOLOM_3'] = (val !== undefined && val !== null) ? val : '';
        obj['_col_3'] = (val !== undefined && val !== null) ? val : '';
      }

      // Normalisasi alias nama kolom kendaraan agar kompatibel maksimal
      const cleanHeader = String(headers[c]).toUpperCase().replace(/[^A-Z0-9]/g, '');
      if (cleanHeader === 'NOKENDARAAN' || cleanHeader === 'NOMORKENDARAAN' || cleanHeader === 'NOMOBIL' || cleanHeader === 'NOMORMOBIL' || cleanHeader === 'NOBODY' || cleanHeader === 'NOMORBODY' || cleanHeader === 'BODY' || cleanHeader === 'ARMADA' || cleanHeader === 'NOARMADA') {
        if (!obj['NO_MOBIL'] || obj['NO_MOBIL'] === '') {
          obj['NO_MOBIL'] = val;
        }
      }
      if (cleanHeader === 'PLATNOMER' || cleanHeader === 'PLATNOMOR' || cleanHeader === 'NOPOL' || cleanHeader === 'NOPOLISI' || cleanHeader === 'NOMORPOLISI' || cleanHeader === 'PLAT') {
        if (!obj['PLAT_NOMER'] || obj['PLAT_NOMER'] === '') {
          obj['PLAT_NOMER'] = val;
        }
      }
    }
    if (row.length > 2 && (obj['WAKTU/TIMESTAMP'] === undefined || obj['WAKTU/TIMESTAMP'] === '')) {
      let v3 = row[2];
      if (v3 instanceof Date) {
        v3 = Utilities.formatDate(v3, 'GMT+7', 'dd/MM/yyyy HH:mm:ss');
      }
      if (v3 !== undefined && v3 !== null && String(v3).trim() !== '') {
        obj['WAKTU/TIMESTAMP'] = String(v3);
        obj['KOLOM_3'] = String(v3);
        obj['_col_3'] = String(v3);
      }
    }
    results.push(obj);
  }
  return results;
}

/**
 * Pengambilan Khusus Kendaraan Super Cepat untuk Dataset Besar (80.000+ Baris)
 * 1. Menggunakan format Matrix/Kompak (hanya 1 list header + baris tanpa duplikasi key JSON, memotong ukuran payload 85%)
 * 2. Mendukung filter Pool langsung di server (misal Pool SKT ~1 detik)
 * 3. Mendukung chunking baris sheet langsung (getRange spesifik tanpa membaca seluruh 80.000 baris jika tanpa filter)
 */
function fetchFastKendaraanData(ss, poolFilter, offset, limit, format) {
  const sheetKendaraan = findSheetFlexible(
    ss,
    'MASTER_KENDARAAN', 'MASTER KENDARAAN', 'Master Kendaraan', 'Kendaraan',
    'Armada', 'Mobil', 'DATA_KENDARAAN', 'DATA KENDARAAN', 'DAFTAR_KENDARAAN',
    'DAFTAR KENDARAAN', 'MASTER_ARMADA', 'DATA_ARMADA', 'UNIT', 'DATA_MOBIL', 'MASTER MOBIL'
  );
  if (!sheetKendaraan || sheetKendaraan.getLastRow() <= 1) {
    return { status: 'success', format: 'compact', columns: ['NO_MOBIL', 'PLAT_NOMER', 'TIPE', 'WARNA', 'POOL', 'TAHUN'], data: [], count: 0, total: 0 };
  }

  const lastRow = sheetKendaraan.getLastRow();
  const lastCol = Math.min(sheetKendaraan.getLastColumn(), 20);

  // 1. Baca hanya baris header (1-2 baris pertama, instan < 5ms)
  const headerCheckRows = Math.min(lastRow, 2);
  const headerRange = sheetKendaraan.getRange(1, 1, headerCheckRows, lastCol).getValues();

  let headerRowIndex = 0;
  let rawHeaders = headerRange[0].map(function(h) { return String(h || '').trim(); });
  let nonBlankCount = rawHeaders.filter(function(h) { return h.length > 0; }).length;
  if (headerCheckRows > 1 && nonBlankCount <= 2) {
    const nextHeaders = headerRange[1].map(function(h) { return String(h || '').trim(); });
    const nextNonBlank = nextHeaders.filter(function(h) { return h.length > 0; }).length;
    if (nextNonBlank > nonBlankCount) {
      headerRowIndex = 1;
      rawHeaders = nextHeaders;
    }
  }

  let idxNoMobil = -1, idxPlat = -1, idxTipe = -1, idxWarna = -1, idxPool = -1, idxTahun = -1, idxTglService = -1, idxService = -1;
  for (let c = 0; c < rawHeaders.length; c++) {
    const h = rawHeaders[c].toUpperCase().replace(/[^A-Z0-9]/g, '');
    if (idxNoMobil === -1 && (h === 'NOMOBIL' || h === 'NOMORMOBIL' || h === 'NOKENDARAAN' || h === 'NOMORKENDARAAN' || h === 'NOBODY' || h === 'NOMORBODY' || h === 'BODY' || h === 'ARMADA' || h === 'NOARMADA' || h === 'NO' || h === 'NOMOR')) {
      idxNoMobil = c;
    }
    if (idxPlat === -1 && (h === 'PLATNOMER' || h === 'PLATNOMOR' || h === 'NOPOL' || h === 'NOPOLISI' || h === 'NOMORPOLISI' || h === 'PLAT')) {
      idxPlat = c;
    }
    if (idxTipe === -1 && (h === 'TIPE' || h === 'TYPE' || h === 'MODEL' || h === 'MERK' || h === 'JENIS')) {
      idxTipe = c;
    }
    if (idxWarna === -1 && (h === 'WARNA' || h === 'COLOR')) {
      idxWarna = c;
    }
    if (idxPool === -1 && (h === 'POOL' || h === 'KODEPOOL' || h === 'LOKASI' || h === 'LOKASIPOOL')) {
      idxPool = c;
    }
    if (idxTahun === -1 && (h === 'TAHUN' || h === 'YEAR')) {
      idxTahun = c;
    }
    if (idxTglService === -1 && (h === 'TANGGALSERVICE' || h === 'TGLSERVICE')) {
      idxTglService = c;
    }
    if (idxService === -1 && (h === 'SERVICE' || h === 'PAKETSERVICE')) {
      idxService = c;
    }
  }

  if (idxNoMobil === -1) idxNoMobil = 0;
  if (idxPlat === -1) idxPlat = (rawHeaders.length > 1) ? 1 : idxNoMobil;

  // Batasi pembacaan kolom hanya sampai kolom terjauh yang dibutuhkan (hemat memori & waktu 3x lipat)
  const maxNeededCol = Math.max(idxNoMobil, idxPlat, idxTipe, idxWarna, idxPool, idxTahun, idxTglService, idxService, 5) + 1;
  const readColCount = Math.min(lastCol, maxNeededCol);

  const targetPool = poolFilter ? String(poolFilter).trim().toUpperCase() : '';
  const isFilterPool = targetPool && targetPool !== 'SEMUA' && targetPool !== 'ALL';
  const filterOffset = (offset && offset > 0) ? parseInt(offset) : 0;
  const filterLimit = (limit && limit > 0) ? parseInt(limit) : 0;
  const startRow = headerRowIndex + 2; // Baris data pertama (1-based)
  const totalAvailableDataRows = Math.max(0, lastRow - startRow + 1);

  const rows = [];
  let totalMatched = 0;

  // Optimasi 1: Jika TANPA filter Pool dan menggunakan pagination chunk (offset & limit),
  // BACA HANYA RANGE BARIS YANG DIBUTUHKAN (Super Cepat < 1 detik untuk 80.000 baris!)
  if (!isFilterPool && filterLimit > 0) {
    const chunkStart = startRow + filterOffset;
    if (chunkStart <= lastRow) {
      const chunkCount = Math.min(filterLimit, lastRow - chunkStart + 1);
      const chunkValues = sheetKendaraan.getRange(chunkStart, 1, chunkCount, readColCount).getValues();
      for (let r = 0; r < chunkValues.length; r++) {
        const row = chunkValues[r];
        const noMobil = (idxNoMobil >= 0 && row[idxNoMobil] !== undefined && row[idxNoMobil] !== null) ? String(row[idxNoMobil]).trim() : '';
        const platNomer = (idxPlat >= 0 && row[idxPlat] !== undefined && row[idxPlat] !== null) ? String(row[idxPlat]).trim() : '';
        if (!noMobil && !platNomer) continue;
        const poolVal = (idxPool >= 0 && row[idxPool] !== undefined && row[idxPool] !== null) ? String(row[idxPool]).trim().toUpperCase() : 'SKT';
        const effectiveNoMobil = noMobil || platNomer;
        const effectivePlat = platNomer || effectiveNoMobil;
        const tipe = (idxTipe >= 0 && row[idxTipe] !== undefined && row[idxTipe] !== null) ? String(row[idxTipe]).trim() : '';
        const warna = (idxWarna >= 0 && row[idxWarna] !== undefined && row[idxWarna] !== null) ? String(row[idxWarna]).trim() : '';
        const tahun = (idxTahun >= 0 && row[idxTahun] !== undefined && row[idxTahun] !== null) ? String(row[idxTahun]).trim() : '';
        rows.push([effectiveNoMobil, effectivePlat, tipe, warna, poolVal, tahun]);
      }
    }
    return {
      status: 'success',
      format: 'compact',
      columns: ['NO_MOBIL', 'PLAT_NOMER', 'TIPE', 'WARNA', 'POOL', 'TAHUN'],
      data: rows,
      count: rows.length,
      total: totalAvailableDataRows,
      offset: filterOffset,
      hasMore: (filterOffset + rows.length) < totalAvailableDataRows
    };
  }

  // Optimasi 2: Dengan Filter Pool atau Ambil Sekaligus (hanya membaca kolom yang diperlukan)
  const rawValues = sheetKendaraan.getRange(startRow, 1, totalAvailableDataRows, readColCount).getValues();
  for (let r = 0; r < rawValues.length; r++) {
    const row = rawValues[r];
    const noMobil = (idxNoMobil >= 0 && row[idxNoMobil] !== undefined && row[idxNoMobil] !== null) ? String(row[idxNoMobil]).trim() : '';
    const platNomer = (idxPlat >= 0 && row[idxPlat] !== undefined && row[idxPlat] !== null) ? String(row[idxPlat]).trim() : '';
    if (!noMobil && !platNomer) continue;

    const poolVal = (idxPool >= 0 && row[idxPool] !== undefined && row[idxPool] !== null) ? String(row[idxPool]).trim().toUpperCase() : 'SKT';

    if (isFilterPool) {
      if (poolVal !== targetPool && poolVal.indexOf(targetPool) === -1 && targetPool.indexOf(poolVal) === -1) {
        continue;
      }
    }

    totalMatched++;
    if (filterOffset > 0 && totalMatched <= filterOffset) {
      continue;
    }

    const effectiveNoMobil = noMobil || platNomer;
    const effectivePlat = platNomer || effectiveNoMobil;
    const tipe = (idxTipe >= 0 && row[idxTipe] !== undefined && row[idxTipe] !== null) ? String(row[idxTipe]).trim() : '';
    const warna = (idxWarna >= 0 && row[idxWarna] !== undefined && row[idxWarna] !== null) ? String(row[idxWarna]).trim() : '';
    const tahun = (idxTahun >= 0 && row[idxTahun] !== undefined && row[idxTahun] !== null) ? String(row[idxTahun]).trim() : '';

    rows.push([effectiveNoMobil, effectivePlat, tipe, warna, poolVal, tahun]);

    if (filterLimit > 0 && rows.length >= filterLimit) {
      break;
    }
  }

  return {
    status: 'success',
    format: 'compact',
    columns: ['NO_MOBIL', 'PLAT_NOMER', 'TIPE', 'WARNA', 'POOL', 'TAHUN'],
    data: rows,
    count: rows.length,
    total: isFilterPool ? totalMatched : totalAvailableDataRows,
    offset: filterOffset,
    hasMore: filterLimit > 0 && (filterOffset + rows.length < (isFilterPool ? totalMatched : totalAvailableDataRows))
  };
}
""".trimIndent()
    }

    fun getIndexHtmlCode(): String {
        return """
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Portal Audit Perawatan Mobil</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
  <style>
    :root {
      --primary-blue: #0d47a1;
      --secondary-blue: #1976d2;
      --light-blue: #e3f2fd;
      --bg-slate: #f8fafc;
      --sidebar-width: 270px;
    }
    body {
      background-color: var(--bg-slate);
      font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
      margin: 0;
      color: #1e293b;
    }
    #sidebar {
      width: var(--sidebar-width);
      height: 100vh;
      position: fixed;
      left: 0;
      top: 0;
      background: #ffffff;
      border-right: 1px solid #e2e8f0;
      transition: all 0.3s ease;
      z-index: 1040;
      overflow-y: auto;
    }
    #main-content {
      margin-left: var(--sidebar-width);
      padding: 24px;
      transition: all 0.3s ease;
    }
    .sidebar-brand {
      padding: 20px 24px;
      background: var(--primary-blue);
      color: white;
    }
    .nav-link {
      color: #475569;
      padding: 12px 20px;
      border-radius: 8px;
      margin: 4px 12px;
      display: flex;
      align-items: center;
      gap: 12px;
      font-weight: 500;
      text-decoration: none;
    }
    .nav-link:hover, .nav-link.active {
      background: var(--light-blue);
      color: var(--primary-blue);
      font-weight: 600;
    }
    .card-kpi {
      border: none;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.04);
      background: white;
      border-left: 4px solid var(--secondary-blue);
    }
    @media (max-width: 992px) {
      #sidebar { left: calc(-1 * var(--sidebar-width)); }
      #sidebar.show { left: 0; }
      #main-content { margin-left: 0; }
      #sidebar-overlay {
        display: none;
        position: fixed;
        inset: 0;
        background: rgba(0,0,0,0.5);
        z-index: 1030;
      }
      #sidebar-overlay.show { display: block; }
    }
  </style>
</head>
<body>
  <div id="sidebar-overlay" onclick="toggleSidebar()"></div>
  
  <!-- SIDEBAR KIRI -->
  <div id="sidebar">
    <div class="sidebar-brand">
      <h5 class="mb-0 fw-bold"><i class="bi bi-shield-check me-2"></i>AUDIT MOBIL</h5>
      <small class="text-white-50">Sistem Perawatan Armada</small>
    </div>
    
    <div class="p-3">
      <div class="text-muted small fw-bold px-3 py-1">MENU AUDITOR</div>
      <a href="?page=audit" class="nav-link active"><i class="bi bi-pencil-square"></i> INPUT AUDIT</a>
      <a href="?page=riwayat" class="nav-link"><i class="bi bi-clock-history"></i> RIWAYAT AUDIT</a>
      
      <hr class="my-3">
      
      <div class="text-muted small fw-bold px-3 py-1">MENU ADMIN</div>
      <a href="?page=monitoring" class="nav-link"><i class="bi bi-speedometer2"></i> Monitoring ALL POOL</a>
      <a href="?page=hasil" class="nav-link"><i class="bi bi-file-earmark-bar-graph"></i> HASIL AUDIT ALL POOL</a>
      
      <hr class="my-3">
      
      <div class="text-muted small fw-bold px-3 py-1">PENGATURAN</div>
      <a href="?page=master_kendaraan" class="nav-link"><i class="bi bi-truck"></i> MASTER KENDARAAN</a>
      <a href="?page=master_pool" class="nav-link"><i class="bi bi-geo-alt"></i> MASTER POOL</a>
      <a href="?page=master_user" class="nav-link"><i class="bi bi-people"></i> MASTER USER</a>
      <a href="?page=pengaturan" class="nav-link"><i class="bi bi-gear"></i> PENGATURAN FOTO</a>
    </div>
  </div>

  <!-- KONTEN UTAMA -->
  <div id="main-content">
    <header class="d-flex justify-content-between align-items-center mb-4 pb-3 border-bottom">
      <div class="d-flex align-items-center gap-3">
        <button class="btn btn-outline-primary d-lg-none" onclick="toggleSidebar()"><i class="bi bi-list fs-5"></i></button>
        <h4 class="mb-0 fw-bold text-primary">Monitoring & Dashboard Audit Mobil</h4>
      </div>
      <div class="d-flex align-items-center gap-2">
        <span class="badge bg-primary px-3 py-2">Role: SUPER ADMIN</span>
      </div>
    </header>

    <div class="row g-3 mb-4">
      <div class="col-md-3">
        <div class="card card-kpi p-3">
          <div class="text-muted small">Total Pool Terdaftar</div>
          <h3 class="fw-bold my-1 text-primary">-</h3>
          <small class="text-success"><i class="bi bi-check-circle"></i> Sesuai Master Pool</small>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card card-kpi p-3">
          <div class="text-muted small">Total Unit Kendaraan</div>
          <h3 class="fw-bold my-1 text-primary">-</h3>
          <small class="text-info"><i class="bi bi-truck"></i> Sesuai Master Kendaraan</small>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card card-kpi p-3">
          <div class="text-muted small">Audit Selesai Hari Ini</div>
          <h3 class="fw-bold my-1 text-primary">-</h3>
          <small class="text-success"><i class="bi bi-arrow-up"></i> Terdata di INPUT_AUDIT</small>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card card-kpi p-3">
          <div class="text-muted small">Status Sinkronisasi</div>
          <h3 class="fw-bold my-1 text-success">Google Sheets</h3>
          <small class="text-muted">Siap Export BigQuery</small>
        </div>
      </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3 p-4">
      <h5 class="fw-bold mb-3"><i class="bi bi-info-circle me-2 text-primary"></i>Instruksi Integrasi Google Script</h5>
      <p class="text-muted">Gunakan URL Web App yang di-deploy dari script ini untuk menghubungkan aplikasi Android secara otomatis.</p>
    </div>
  </div>

  <script>
    function toggleSidebar() {
      document.getElementById('sidebar').classList.toggle('show');
      document.getElementById('sidebar-overlay').classList.toggle('show');
    }
  </script>
</body>
</html>
""".trimIndent()
    }

    /**
     * Menghasilkan Skrip Standalone Apps Script khusus untuk Inisialisasi Sheet INPUT_AUDIT
     * Dapat langsung disalin dan dijalankan di editor Apps Script Google Sheets.
     */
    fun getInputAuditInitScript(sheetId: String = SPREADSHEET_ID): String {
        val inputAuditHeadersJs = AuditDatabaseSchema.INPUT_AUDIT_COLUMNS.joinToString(", ") { "\"$it\"" }
        val effectiveSheetId = sheetId.ifBlank { SPREADSHEET_ID }
        return """
// ============================================================================
// SKRIP INISIALISASI TABEL "INPUT_AUDIT" GOOGLE SHEETS
// ============================================================================
// Menyiapkan 325 Kolom (A1 s/d LK1), styling header navy modern,
// membekukan baris 1 & kolom 1..7 (ID s/d NO_KENDARAAN), serta mengatur lebar kolom.
// ============================================================================

const SHEET_ID = '$effectiveSheetId';

const INPUT_AUDIT_COLUMNS = [ $inputAuditHeadersJs ];

/**
 * Jalankan fungsi ini sekali di Google Apps Script (Run -> initInputAudit)
 */
function initInputAudit() {
  const ss = (SHEET_ID && SHEET_ID !== 'MASUKKAN_SHEET_ID_DISINI')
    ? SpreadsheetApp.openById(SHEET_ID)
    : SpreadsheetApp.getActiveSpreadsheet();

  let sheet = ss.getSheetByName('INPUT_AUDIT') || ss.getSheetByName('DATA_AUDIT');
  if (!sheet) {
    sheet = ss.insertSheet('INPUT_AUDIT');
  }

  // 1. Pastikan kapasitas sheet mencukupi (minimal 325 kolom dan 100 baris awal)
  const currentCols = sheet.getMaxColumns();
  if (currentCols < INPUT_AUDIT_COLUMNS.length) {
    sheet.insertColumnsAfter(currentCols, INPUT_AUDIT_COLUMNS.length - currentCols);
  }
  const currentRows = sheet.getMaxRows();
  if (currentRows < 100) {
    sheet.insertRowsAfter(currentRows, 100 - currentRows);
  }

  // 2. Tulis Header Lengkap (Kolom A1 s/d LK1 = 325 Kolom)
  const headerRange = sheet.getRange(1, 1, 1, INPUT_AUDIT_COLUMNS.length);
  headerRange.setValues([INPUT_AUDIT_COLUMNS]);

  // 3. Styling Header Navy Modern
  try {
    headerRange.setFontWeight('bold');
    headerRange.setFontColor('#FFFFFF');
    headerRange.setBackground('#1E293B'); // Navy Slate
    headerRange.setHorizontalAlignment('center');
    headerRange.setVerticalAlignment('middle');
    headerRange.setWrap(false);
    sheet.setRowHeight(1, 38);
  } catch (e) {
    Logger.log('Style warning: ' + e);
  }

  // 4. Freeze Header & Identitas Utama (Kolom 1 s/d 7)
  try {
    sheet.setFrozenRows(1);
    sheet.setFrozenColumns(7); // Freeze sampai kolom NO_KENDARAAN
  } catch (e) {
    Logger.log('Freeze warning: ' + e);
  }

  // 5. Sesuaikan Lebar Kolom Utama
  try {
    sheet.setColumnWidth(1, 65);   // ID
    sheet.setColumnWidth(2, 105);  // TANGGAL
    sheet.setColumnWidth(3, 150);  // TIMESTAMP
    sheet.setColumnWidth(4, 90);   // NIP
    sheet.setColumnWidth(5, 150);  // NAMA_PETUGAS / AUDITOR
    sheet.setColumnWidth(6, 110);  // PERIODE
    sheet.setColumnWidth(7, 110);  // NO_KENDARAAN
    sheet.setColumnWidth(8, 120);  // PLAT_NOMER
    sheet.setColumnWidth(9, 110);  // TIPE
    sheet.setColumnWidth(10, 85);  // WARNA
    sheet.setColumnWidth(11, 80);  // TAHUN
    sheet.setColumnWidth(12, 90);  // POOL

    // Kolom Foto Utama Kendaraan (13 s/d 19)
    for (let c = 13; c <= 19; c++) {
      sheet.setColumnWidth(c, 200);
    }

    sheet.setColumnWidth(20, 130); // JENIS_SAMPLE
    sheet.setColumnWidth(21, 130); // TANGGAL_SERVICE
    sheet.setColumnWidth(22, 130); // PAKET_SERVICE
    sheet.setColumnWidth(23, 120); // TOTAL_POIN_TEMUAN
  } catch (e) {
    Logger.log('Column width warning: ' + e);
  }

  Logger.log('Sukses: Tabel INPUT_AUDIT berhasil diinisialisasi dengan ' + INPUT_AUDIT_COLUMNS.length + ' kolom (A1 s/d LK1).');
  return 'Sukses: Tabel INPUT_AUDIT berhasil diinisialisasi dengan ' + INPUT_AUDIT_COLUMNS.length + ' kolom (A1 s/d LK1).';
}
""".trimIndent()
    }

    /**
     * Menghasilkan cuplikan kode skrip Apps Script standalone khusus untuk inisialisasi tabel MASTER_GRADE_KPI
     */
    fun getGradeKpiInitScript(sheetId: String = SPREADSHEET_ID): String {
        val effectiveSheetId = sheetId.ifBlank { SPREADSHEET_ID }
        return """
// ============================================================================
// SKRIP INISIALISASI TABEL "MASTER_GRADE_KPI" GOOGLE SHEETS
// ============================================================================
// Menyiapkan kolom KATEGORI, MIN_SCORE, MAX_SCORE, KETERANGAN tanpa menimpa data
// ============================================================================

const SHEET_ID = '$effectiveSheetId';

function initGradeKpi() {
  const ss = (SHEET_ID && SHEET_ID !== 'MASUKKAN_SHEET_ID_DISINI')
    ? SpreadsheetApp.openById(SHEET_ID)
    : SpreadsheetApp.getActiveSpreadsheet();

  let sheet = ss.getSheetByName('MASTER_GRADE_KPI') || ss.getSheetByName('GRADE_KPI');
  if (!sheet) {
    sheet = ss.insertSheet('MASTER_GRADE_KPI');
  }

  if (sheet.getLastRow() === 0) {
    sheet.appendRow(['KATEGORI', 'MIN_SCORE', 'MAX_SCORE', 'KETERANGAN']);
    sheet.appendRow(['A', 70.00, 80.00, 'Sangat Baik (Grade A)']);
    sheet.appendRow(['B', 60.00, 69.99, 'Baik (Grade B)']);
    sheet.appendRow(['C', 50.00, 59.99, 'Cukup (Grade C)']);
    sheet.appendRow(['D', 30.00, 49.99, 'Kurang (Grade D)']);
    sheet.appendRow(['E', 0.00, 29.99, 'Kritis / Perlu Perbaikan (Grade E)']);

    try {
      const headerRange = sheet.getRange(1, 1, 1, 4);
      headerRange.setFontWeight('bold');
      headerRange.setFontColor('#FFFFFF');
      headerRange.setBackground('#0D47A1');
      headerRange.setHorizontalAlignment('center');
      headerRange.setVerticalAlignment('middle');
      sheet.setRowHeight(1, 36);
      sheet.setFrozenRows(1);
      sheet.setColumnWidth(1, 120);
      sheet.setColumnWidth(2, 140);
      sheet.setColumnWidth(3, 140);
      sheet.setColumnWidth(4, 280);

      sheet.getRange(2, 2, 5, 2).setNumberFormat('0.00');
      sheet.getRange(2, 1, 5, 1).setHorizontalAlignment('center');
    } catch (e) {
      Logger.log('Header format warning: ' + e);
    }
  }

  Logger.log('Sukses: Tabel MASTER_GRADE_KPI siap tanpa menimpa nilai grade yang ada.');
  return 'Sukses: Tabel MASTER_GRADE_KPI siap (struktur kolom KATEGORI, MIN_SCORE, MAX_SCORE, KETERANGAN).';
}
""".trimIndent()
    }
}
