import sys

with open('app/src/main/java/com/example/data/remote/GoogleAppsScriptHub.kt') as f:
    code = f.read()

# 1. Imports
if 'import com.example.data.model.AuditDatabaseSchema' not in code:
    code = code.replace(
        'package com.example.data.remote\n',
        'package com.example.data.remote\n\nimport com.example.data.model.AuditDatabaseSchema\nimport com.example.data.model.KpiAuditSpec\n'
    )

# 2. Add inputAuditHeadersJs & kpiItemIdsJs to getGoogleScriptCode
needle_func = 'fun getGoogleScriptCode(sheetId: String = SPREADSHEET_ID, folderId: String = DRIVE_FOLDER_ID): String {\n'
repl_func = '''fun getGoogleScriptCode(sheetId: String = SPREADSHEET_ID, folderId: String = DRIVE_FOLDER_ID): String {
        val inputAuditHeadersJs = AuditDatabaseSchema.INPUT_AUDIT_COLUMNS.joinToString(", ") { "\\\"$it\\\"" }
        val kpiItemIdsJs = KpiAuditSpec.getAllItems().joinToString(", ") { "\\\"${it.id}\\\"" }
'''
code = code.replace(needle_func, repl_func, 1)

# 3. Add INPUT_AUDIT_COLUMNS & KPI_ITEM_IDS in JS header
needle_const = '''const SHEET_ID = '$sheetId';
const FOLDER_ID = '$folderId';'''
repl_const = '''const SHEET_ID = '$sheetId';
const FOLDER_ID = '$folderId';
const INPUT_AUDIT_COLUMNS = [ $inputAuditHeadersJs ];
const KPI_ITEM_IDS = [ $kpiItemIdsJs ];'''
code = code.replace(needle_const, repl_const, 1)

# 4. Replace setupSpreadsheet part 7 and saveAuditRecord + deleteAuditRecord
start_target = code.find('  // 7. DATA_AUDIT')
end_target = code.find('function saveMasterKendaraan(v) {')
assert start_target != -1 and end_target != -1, f'Indices not found: {start_target}, {end_target}'

new_section = '''  // 7. INPUT_AUDIT (A1 s/d NC1 = 367 Kolom)
  let sheetAudit = findSheetFlexible(ss, 'INPUT_AUDIT', 'INPUT AUDIT', 'Input Audit', 'DATA_AUDIT', 'DATA AUDIT', 'Data Audit', 'Audit') || ss.insertSheet('INPUT_AUDIT');
  if (sheetAudit.getLastRow() === 0) {
    sheetAudit.appendRow(INPUT_AUDIT_COLUMNS);
  } else if (sheetAudit.getLastColumn() < INPUT_AUDIT_COLUMNS.length) {
    sheetAudit.getRange(1, 1, 1, INPUT_AUDIT_COLUMNS.length).setValues([INPUT_AUDIT_COLUMNS]);
  }
}

/**
 * Menyimpan / Memperbarui Record Audit di INPUT_AUDIT (A1 s/d NC1)
 * Baris lama di-update secara in-place jika ID atau No Kendaraan + Periode cocok.
 */
function saveAuditRecord(audit) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'INPUT_AUDIT', 'INPUT AUDIT', 'Input Audit', 'DATA_AUDIT', 'DATA AUDIT', 'Data Audit', 'Audit') || ss.insertSheet('INPUT_AUDIT');
  let folder = null;
  try {
    if (FOLDER_ID && String(FOLDER_ID).trim() !== '') {
      folder = DriveApp.getFolderById(String(FOLDER_ID).trim());
    }
  } catch (fe) {
    folder = null;
  }

  // Pastikan header A1 s/d NC1 tersedia
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(INPUT_AUDIT_COLUMNS);
  } else if (sheet.getLastColumn() < INPUT_AUDIT_COLUMNS.length) {
    sheet.getRange(1, 1, 1, INPUT_AUDIT_COLUMNS.length).setValues([INPUT_AUDIT_COLUMNS]);
  }

  const lastRow = sheet.getLastRow();
  let existingRowIndex = -1;
  let existingRowValues = null;

  const targetId = (audit.id !== undefined && audit.id !== null) ? String(audit.id).trim() : '';
  const targetNoMobil = (audit.noKendaraan || audit.noMobil) ? String(audit.noKendaraan || audit.noMobil).trim().toUpperCase() : '';
  const targetPeriode = audit.periode ? String(audit.periode).trim().toUpperCase() : '';
  const targetTanggal = audit.tanggal ? String(audit.tanggal).trim() : '';

  if (lastRow > 1) {
    const dataRange = sheet.getRange(2, 1, lastRow - 1, Math.min(sheet.getLastColumn(), 15)).getValues();

    if (targetId && targetId !== '0') {
      for (let i = 0; i < dataRange.length; i++) {
        const rowId = String(dataRange[i][0]).trim();
        if (rowId === targetId) {
          existingRowIndex = i + 2;
          break;
        }
      }
    }

    if (existingRowIndex === -1 && targetNoMobil && targetPeriode && targetTanggal) {
      for (let i = 0; i < dataRange.length; i++) {
        const rowNoMobil = String(dataRange[i][6]).trim().toUpperCase();
        const rowPeriode = String(dataRange[i][5]).trim().toUpperCase();
        const rowTanggal = String(dataRange[i][1]).trim();
        if (rowNoMobil === targetNoMobil && rowPeriode === targetPeriode && rowTanggal === targetTanggal) {
          existingRowIndex = i + 2;
          break;
        }
      }
    }

    if (existingRowIndex === -1 && (audit.isEdit || targetId) && targetNoMobil && targetPeriode) {
      for (let i = 0; i < dataRange.length; i++) {
        const rowNoMobil = String(dataRange[i][6]).trim().toUpperCase();
        const rowPeriode = String(dataRange[i][5]).trim().toUpperCase();
        if (rowNoMobil === targetNoMobil && rowPeriode === targetPeriode) {
          existingRowIndex = i + 2;
          break;
        }
      }
    }
  }

  if (existingRowIndex > 0) {
    existingRowValues = sheet.getRange(existingRowIndex, 1, 1, Math.max(sheet.getLastColumn(), INPUT_AUDIT_COLUMNS.length)).getValues()[0];
  }

  function uploadImage(base64Data, filename) {
    if (!base64Data || String(base64Data).trim().length < 50) return '';
    if (String(base64Data).startsWith('http')) return base64Data;
    try {
      const decoded = Utilities.base64Decode(String(base64Data).replace(/^data:image\\/[a-z]+;base64,/, ''));
      const blob = Utilities.newBlob(decoded, 'image/jpeg', filename);
      const file = folder ? folder.createFile(blob) : DriveApp.createFile(blob);
      try {
        file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      } catch (shareErr) {}
      return file.getUrl();
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

  const recordId = existingRowIndex > 0 ? (existingRowValues[0] || targetId || (existingRowIndex - 1)) : (targetId && targetId !== '0' ? targetId : lastRow);
  const originalTimestamp = existingRowIndex > 0 ? (existingRowValues[2] || audit.timestamp || new Date().toLocaleString('id-ID')) : (audit.timestamp || new Date().toLocaleString('id-ID'));
  const timestamp = String(originalTimestamp);
  const tanggal = audit.tanggal || (existingRowIndex > 0 ? existingRowValues[1] : Utilities.formatDate(new Date(), 'GMT+7', 'dd/MM/yyyy'));

  const rowData = new Array(INPUT_AUDIT_COLUMNS.length);
  for (let ci = 0; ci < rowData.length; ci++) rowData[ci] = '';

  rowData[0] = recordId;
  rowData[1] = tanggal;
  rowData[2] = timestamp;
  rowData[3] = audit.nip || (existingRowIndex > 0 ? existingRowValues[3] : '');
  rowData[4] = audit.namaPetugas || audit.namaAuditor || (existingRowIndex > 0 ? existingRowValues[4] : '');
  rowData[5] = audit.periode || (existingRowIndex > 0 ? existingRowValues[5] : '');
  rowData[6] = targetNoMobil || (existingRowIndex > 0 ? existingRowValues[6] : '');
  rowData[7] = audit.platNomer || (existingRowIndex > 0 ? existingRowValues[7] : '');
  rowData[8] = audit.tipe || (existingRowIndex > 0 ? existingRowValues[8] : '');
  rowData[9] = audit.warna || (existingRowIndex > 0 ? existingRowValues[9] : '');
  rowData[10] = audit.tahun || (existingRowIndex > 0 ? existingRowValues[10] : '');
  rowData[11] = audit.pool || (existingRowIndex > 0 ? existingRowValues[11] : '');

  for (let vi = 0; vi < vKeys.length; vi++) {
    const vk = vKeys[vi];
    const colIdx = 12 + vi;
    const existingVal = (existingRowValues && existingRowValues.length > colIdx) ? existingRowValues[colIdx] : '';
    const finalVal = uploadedUrls[vk] || photosMap[vk] || kpiPhotos[vk] || audit['photo' + (vi + 1)] || existingVal || '';
    rowData[colIdx] = finalVal;
  }

  rowData[19] = audit.jenisSample || (existingRowIndex > 0 ? existingRowValues[19] : '');
  rowData[20] = audit.tanggalService || (existingRowIndex > 0 ? existingRowValues[20] : '');
  rowData[21] = audit.paketService || audit.service || (existingRowIndex > 0 ? existingRowValues[21] : '');
  rowData[22] = (audit.totalPoinTemuan !== undefined && audit.totalPoinTemuan !== null) ? audit.totalPoinTemuan : (existingRowIndex > 0 ? existingRowValues[22] : 0);

  let kpiItemIdx = -1;
  let photoCounter = 0;

  for (let ci = 23; ci < 366; ci++) {
    const colName = INPUT_AUDIT_COLUMNS[ci];
    const existingVal = (existingRowValues && existingRowValues.length > ci) ? existingRowValues[ci] : '';

    if (colName.endsWith('_POINT')) {
      kpiItemIdx++;
      photoCounter = 0;
      const curItemId = KPI_ITEM_IDS[kpiItemIdx];
      const pt = (kpiScores[curItemId] !== undefined && kpiScores[curItemId] !== null) ? kpiScores[curItemId] : (existingVal !== '' ? existingVal : 0);
      rowData[ci] = pt;
    } else if (colName.startsWith('FOTO_') || colName.startsWith('PHOTO_')) {
      const curItemId = KPI_ITEM_IDS[kpiItemIdx];
      const slotKey = curItemId + '_' + photoCounter;
      const url = uploadedUrls[slotKey] || photosMap[slotKey] || kpiPhotos[slotKey] || existingVal || '';
      rowData[ci] = url;
    } else if (colName.startsWith('CATATAN_') || colName.startsWith('CATATN_')) {
      const curItemId = KPI_ITEM_IDS[kpiItemIdx];
      const slotKey = curItemId + '_' + photoCounter;
      const note = (kpiNotes[slotKey] !== undefined && kpiNotes[slotKey] !== null) ? kpiNotes[slotKey] : (existingVal !== '' ? existingVal : '');
      rowData[ci] = note;
      photoCounter++;
    }
  }

  const rawPayload = {
    kpiScores: kpiScores,
    kpiNotes: kpiNotes,
    kpiPhotos: kpiPhotos,
    rincian: audit.rincianPoinTemuan || ''
  };
  rowData[366] = JSON.stringify(rawPayload);

  if (existingRowIndex > 0) {
    sheet.getRange(existingRowIndex, 1, 1, rowData.length).setValues([rowData]);
    return { id: recordId, timestamp: timestamp, mode: 'updated', row: existingRowIndex };
  } else {
    sheet.appendRow(rowData);
    return { id: recordId, timestamp: timestamp, mode: 'inserted', row: sheet.getLastRow() };
  }
}

/**
 * Hapus Data Audit dari INPUT_AUDIT
 */
function deleteAuditRecord(id, noMobil, periode) {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = findSheetFlexible(ss, 'INPUT_AUDIT', 'INPUT AUDIT', 'Input Audit', 'DATA_AUDIT', 'DATA AUDIT', 'Data Audit', 'Audit');
  if (!sheet) return { deleted: false, message: 'Sheet INPUT_AUDIT tidak ditemukan' };
  const lastRow = sheet.getLastRow();
  if (lastRow <= 1) return { deleted: false, message: 'Data kosong' };

  const targetId = id ? String(id).trim() : '';
  const targetNoMobil = noMobil ? String(noMobil).trim().toUpperCase() : '';
  const targetPeriode = periode ? String(periode).trim().toUpperCase() : '';

  const dataRange = sheet.getRange(2, 1, lastRow - 1, Math.min(sheet.getLastColumn(), 15)).getValues();
  for (let i = 0; i < dataRange.length; i++) {
    const rowId = String(dataRange[i][0]).trim();
    const rowNoMobil = String(dataRange[i][6]).trim().toUpperCase();
    const rowPeriodeNew = (dataRange[i].length > 5) ? String(dataRange[i][5]).trim().toUpperCase() : '';
    const rowPeriodeOld = (dataRange[i].length > 3) ? String(dataRange[i][3]).trim().toUpperCase() : '';

    if ((targetId && rowId === targetId) || (targetNoMobil && rowNoMobil === targetNoMobil && targetPeriode && (rowPeriodeNew === targetPeriode || rowPeriodeOld === targetPeriode))) {
      sheet.deleteRow(i + 2);
      return { deleted: true, row: i + 2 };
    }
  }
  return { deleted: false, message: 'Data tidak ditemukan' };
}

'''

code = code[:start_target] + new_section + code[end_target:]

code = code.replace(
    "findSheetFlexible(ss, 'DATA_AUDIT', 'DATA AUDIT', 'Data Audit', 'Audit', 'Audits', 'Hasil Audit')",
    "findSheetFlexible(ss, 'INPUT_AUDIT', 'INPUT AUDIT', 'Input Audit', 'DATA_AUDIT', 'DATA AUDIT', 'Data Audit', 'Audit', 'Audits', 'Hasil Audit')"
)

with open('app/src/main/java/com/example/data/remote/GoogleAppsScriptHub.kt', 'w') as f:
    f.write(code)

print('GoogleAppsScriptHub.kt updated successfully!')
