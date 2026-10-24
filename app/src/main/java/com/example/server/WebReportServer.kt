package com.example.server

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import android.util.Base64
import android.util.Log
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.DataAuditEntity
import com.example.data.model.KpiAuditSpec
import com.example.util.PhotoDownloadHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IpInterfaceInfo(
    val ip: String,
    val interfaceName: String,
    val label: String,
    val isRecommended: Boolean,
    val isEmulator: Boolean
)

class WebReportServer(
    private val context: Context,
    initialPort: Int = 8080
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val database = AppDatabase.getInstance(context)
    private val auditDao = database.dataAuditDao()
    private val bobotDao = database.masterBobotDao()
    private val poolDao = database.masterPoolDao()
    private val sampleDao = database.masterSampleDao()
    private val gradeDao = database.masterGradeKpiDao()
    private val periodeDao = database.masterPeriodeDao()
    private val kendaraanDao = database.masterKendaraanDao()
    private val userDao = database.userDao()

    var currentPort: Int = initialPort
        private set

    var isRunning: Boolean = false
        private set

    var lastError: String? = null
        private set

    var activeConnectionsCount: Int = 0
        private set

    var selectedIpOverride: String? = null

    fun getAllAvailableIps(): List<IpInterfaceInfo> {
        val list = mutableListOf<IpInterfaceInfo>()
        try {
            // Check WifiManager safely
            var wifiIp: String? = null
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                if (wifiManager != null && wifiManager.isWifiEnabled) {
                    @Suppress("DEPRECATION")
                    val ipInt = wifiManager.connectionInfo?.ipAddress ?: 0
                    if (ipInt != 0) {
                        @Suppress("DEPRECATION")
                        val ip = Formatter.formatIpAddress(ipInt)
                        if (ip != "0.0.0.0" && ip.isNotBlank()) {
                            wifiIp = ip
                            list.add(
                                IpInterfaceInfo(
                                    ip = ip,
                                    interfaceName = "wlan",
                                    label = "Wi-Fi Terhubung ($ip)",
                                    isRecommended = true,
                                    isEmulator = false
                                )
                            )
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.w("WebReportServer", "Permission ACCESS_WIFI_STATE not granted or restricted: ${e.message}")
            } catch (e: Exception) {
                Log.w("WebReportServer", "Unable to inspect WifiManager: ${e.message}")
            }

            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val ifName = networkInterface.name.lowercase(Locale.getDefault())
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val hostAddress = address.hostAddress
                        if (hostAddress != null && hostAddress.isNotBlank()) {
                            if (hostAddress == wifiIp) continue // avoid duplicates

                            val isEmulator = hostAddress == "10.0.2.15" || hostAddress.startsWith("10.0.2.")
                            val isHotspot = hostAddress == "192.168.43.1" || ifName.contains("ap") || hostAddress.startsWith("192.168.43.")
                            val isUsb = ifName.contains("rndis") || hostAddress.startsWith("192.168.42.")

                            val label = when {
                                isEmulator -> "Emulator Cloud Android Studio ($hostAddress)"
                                isHotspot -> "Hotspot HP / Tethering ($hostAddress) ★ Paling Handal"
                                isUsb -> "USB Tethering ($hostAddress)"
                                ifName.contains("wlan") -> "Wi-Fi ($hostAddress)"
                                else -> "Jaringan $ifName ($hostAddress)"
                            }

                            list.add(
                                IpInterfaceInfo(
                                    ip = hostAddress,
                                    interfaceName = ifName,
                                    label = label,
                                    isRecommended = isHotspot || (!isEmulator && hostAddress.startsWith("192.168.")),
                                    isEmulator = isEmulator
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WebReportServer", "Error finding IP addresses", e)
        }

        if (list.isEmpty()) {
            list.add(
                IpInterfaceInfo(
                    ip = "127.0.0.1",
                    interfaceName = "lo",
                    label = "Localhost Internal (127.0.0.1)",
                    isRecommended = false,
                    isEmulator = false
                )
            )
        }

        return list
    }

    fun isCurrentIpEmulator(): Boolean {
        val current = getLocalIpAddress()
        return current == "10.0.2.15" || current.startsWith("10.0.2.")
    }

    fun getLocalIpAddress(): String {
        selectedIpOverride?.let { if (it.isNotBlank()) return it }
        val ips = getAllAvailableIps()
        // Priority: Hotspot 192.168.43.x -> Wi-Fi -> First non-emulator -> Any
        val hotspot = ips.find { it.ip.startsWith("192.168.43.") }
        if (hotspot != null) return hotspot.ip

        val wifi = ips.find { !it.isEmulator && it.ip.startsWith("192.168.") }
        if (wifi != null) return wifi.ip

        val nonEmulator = ips.find { !it.isEmulator && it.ip != "127.0.0.1" }
        if (nonEmulator != null) return nonEmulator.ip

        return ips.firstOrNull()?.ip ?: "127.0.0.1"
    }

    fun getServerUrl(): String {
        val ip = getLocalIpAddress()
        return "http://$ip:$currentPort"
    }

    @Synchronized
    fun changePort(newPort: Int, onStarted: ((String) -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        val wasRunning = isRunning
        stop()
        currentPort = newPort
        if (wasRunning) {
            start(onStarted, onError)
        } else {
            onStarted?.invoke(getServerUrl())
        }
    }

    @Synchronized
    fun start(onStarted: ((String) -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        if (isRunning) {
            onStarted?.invoke(getServerUrl())
            return
        }

        try {
            serverSocket = ServerSocket(currentPort)
            isRunning = true
            lastError = null

            val url = getServerUrl()
            Log.i("WebReportServer", "Web Report Server started at $url")

            serverJob = scope.launch {
                while (isActive && isRunning) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        launch {
                            handleClientSocket(socket)
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.w("WebReportServer", "Socket accept exception: ${e.message}")
                        }
                    }
                }
            }

            onStarted?.invoke(url)
        } catch (e: Exception) {
            isRunning = false
            lastError = e.message
            Log.e("WebReportServer", "Failed to start server", e)
            onError?.invoke(e.message ?: "Gagal memulai server pada port $currentPort")
        }
    }

    @Synchronized
    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.w("WebReportServer", "Error closing server socket: ${e.message}")
        }
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        Log.i("WebReportServer", "Web Report Server stopped")
    }

    suspend fun getAuditsJsonString(embedBase64Photos: Boolean = false): String = withContext(Dispatchers.IO) {
        val rawAudits = auditDao.getAllAuditsList()
        val audits = com.example.util.AuditFilterUtils.filterUniqueAudits(rawAudits)
        val jsonArray = JSONArray()

        for (a in audits) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("timestamp", a.timestamp)
            obj.put("tanggal", a.tanggal)
            obj.put("periode", a.periode)
            obj.put("nip", a.nip)
            obj.put("namaPetugas", a.namaPetugas)
            obj.put("noMobil", a.noMobil)
            obj.put("platNomer", a.platNomer)
            obj.put("tipe", a.tipe)
            obj.put("warna", a.warna)
            obj.put("tahun", a.tahun)
            obj.put("tanggalService", a.tanggalService)
            obj.put("service", a.service)
            obj.put("pool", a.pool)
            obj.put("totalPoinTemuan", a.totalPoinTemuan)
            obj.put("jenisSample", a.jenisSample)
            obj.put("sampleTanggal", a.sampleTanggal)
            obj.put("paketService", a.paketService)
            obj.put("kpiScoresJson", a.kpiScoresJson)
            obj.put("kpiNotesJson", a.kpiNotesJson)

            val photos = PhotoDownloadHelper.extractPhotosFromAudit(a)
            val photosArray = JSONArray()
            for (p in photos) {
                val pObj = JSONObject()
                pObj.put("slotIndex", p.slotIndex)
                pObj.put("title", p.title)
                pObj.put("note", p.note)
                pObj.put("itemKey", p.itemKey)
                if (embedBase64Photos && p.photoPath.isNotBlank()) {
                    val bytes = loadPhotoBytes(p.photoPath)
                    if (bytes != null && bytes.isNotEmpty()) {
                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        pObj.put("url", "data:image/jpeg;base64,$b64")
                    } else {
                        pObj.put("url", "/api/photo?auditId=${a.id}&key=${p.itemKey}")
                    }
                } else {
                    pObj.put("url", "/api/photo?auditId=${a.id}&key=${p.itemKey}")
                }
                photosArray.put(pObj)
            }
            obj.put("photos", photosArray)
            obj.put("photoCount", photos.size)

            jsonArray.put(obj)
        }
        jsonArray.toString()
    }

    fun getKpiSpecJsonString(): String {
        val sectionsArray = JSONArray()
        for (sec in KpiAuditSpec.sections) {
            val secObj = JSONObject()
            secObj.put("id", sec.id)
            secObj.put("title", sec.title)
            val groupsArray = JSONArray()
            for (grp in sec.groups) {
                val grpObj = JSONObject()
                grpObj.put("id", grp.id)
                grpObj.put("title", grp.title)
                val itemsArray = JSONArray()
                for (it in grp.items) {
                    val itObj = JSONObject()
                    itObj.put("id", it.id)
                    itObj.put("name", it.name)
                    itObj.put("maxPoints", it.maxPoints)
                    itemsArray.put(itObj)
                }
                grpObj.put("items", itemsArray)
                groupsArray.put(grpObj)
            }
            secObj.put("groups", groupsArray)
            sectionsArray.put(secObj)
        }
        return sectionsArray.toString()
    }

    suspend fun getBobotJsonString(): String = withContext(Dispatchers.IO) {
        val list = bobotDao.getAllBobot()
        val jsonArray = JSONArray()
        for (b in list) {
            val obj = JSONObject()
            obj.put("itemId", b.itemId)
            obj.put("itemName", b.itemName)
            obj.put("sectionTitle", b.sectionTitle)
            obj.put("groupTitle", b.groupTitle)
            obj.put("pointMax", b.pointMax)
            obj.put("bobot", b.bobot)
            jsonArray.put(obj)
        }
        jsonArray.toString()
    }

    suspend fun getPoolsJsonString(): String = withContext(Dispatchers.IO) {
        val list = poolDao.getAllPoolsList()
        val jsonArray = JSONArray()
        for (p in list) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("kodePool", p.kodePool)
            obj.put("namaPool", p.namaPool)
            obj.put("statusArea", p.statusArea)
            jsonArray.put(obj)
        }
        jsonArray.toString()
    }

    suspend fun getSamplesJsonString(): String = withContext(Dispatchers.IO) {
        val list = sampleDao.getAllSamples()
        val jsonArray = JSONArray()
        for (s in list) {
            val obj = JSONObject()
            obj.put("statusArea", s.statusArea)
            obj.put("jumlahSample", s.jumlahSample)
            obj.put("keterangan", s.keterangan)
            jsonArray.put(obj)
        }
        jsonArray.toString()
    }

    suspend fun getGradesJsonString(): String = withContext(Dispatchers.IO) {
        val list = gradeDao.getAllGradeKpi()
        val jsonArray = JSONArray()
        for (g in list) {
            val obj = JSONObject()
            obj.put("kategori", g.kategori)
            obj.put("minScore", g.minScore)
            obj.put("maxScore", g.maxScore)
            obj.put("keterangan", g.keterangan)
            jsonArray.put(obj)
        }
        jsonArray.toString()
    }

    suspend fun getPeriodesJsonString(): String = withContext(Dispatchers.IO) {
        val list = periodeDao.getAllPeriodeList()
        val jsonArray = JSONArray()
        for (p in list) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("periode", p.periode)
            obj.put("keterangan", p.keterangan)
            jsonArray.put(obj)
        }
        jsonArray.toString()
    }

    suspend fun getVehiclesJsonString(): String = withContext(Dispatchers.IO) {
        val list = kendaraanDao.getAllKendaraanList()
        val jsonArray = JSONArray()
        for (v in list) {
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("noMobil", v.noMobil)
            obj.put("platNomer", v.platNomer)
            obj.put("tipe", v.tipe)
            obj.put("warna", v.warna)
            obj.put("tahun", v.tahun)
            obj.put("pool", v.pool)
            jsonArray.put(obj)
        }
        jsonArray.toString()
    }

    suspend fun getUsersJsonString(): String = withContext(Dispatchers.IO) {
        val list = userDao.getAllUsersList()
        val jsonArray = JSONArray()
        for (u in list) {
            val obj = JSONObject()
            obj.put("nip", u.nip)
            obj.put("namaPetugas", u.namaPetugas)
            obj.put("username", u.username)
            obj.put("levelAkses", u.levelAkses)
            obj.put("kodePool", u.kodePool)
            jsonArray.put(obj)
        }
        jsonArray.toString()
    }

    suspend fun generateStandaloneHtmlFile(): File = withContext(Dispatchers.IO) {
        val auditsJson = getAuditsJsonString(embedBase64Photos = true)
        val specJson = getKpiSpecJsonString()
        val bobotJson = getBobotJsonString()
        val poolsJson = getPoolsJsonString()
        val samplesJson = getSamplesJsonString()
        val gradesJson = getGradesJsonString()
        val periodesJson = getPeriodesJsonString()
        val vehiclesJson = getVehiclesJsonString()
        val usersJson = getUsersJsonString()
        val htmlContent = WebDashboardHtml.generate(
            embeddedAuditsJson = auditsJson,
            embeddedSpecJson = specJson,
            embeddedBobotJson = bobotJson,
            embeddedPoolsJson = poolsJson,
            embeddedSamplesJson = samplesJson,
            embeddedGradesJson = gradesJson,
            embeddedPeriodesJson = periodesJson,
            embeddedVehiclesJson = vehiclesJson,
            embeddedUsersJson = usersJson
        )

        val dir = File(context.cacheDir, "web_reports")
        if (!dir.exists()) dir.mkdirs()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val file = File(dir, "Dashboard_Audit_Armada_$dateStr.html")
        file.writeText(htmlContent, Charsets.UTF_8)
        file
    }

    suspend fun generateStandaloneExcelFile(): File = withContext(Dispatchers.IO) {
        val excelContent = generateExcelContent()
        val dir = File(context.cacheDir, "web_reports")
        if (!dir.exists()) dir.mkdirs()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val file = File(dir, "Laporan_Audit_Armada_$dateStr.xls")
        file.writeText(excelContent, Charsets.UTF_8)
        file
    }

    private suspend fun generateExcelContent(): String {
        val audits = auditDao.getAllAuditsList()
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        return buildString {
            append("""
                <html xmlns:o="urn:schemas-microsoft-com:office:office" 
                      xmlns:x="urn:schemas-microsoft-com:office:excel" 
                      xmlns="http://www.w3.org/TR/REC-html40">
                <head>
                <meta http-equiv="content-type" content="application/vnd.ms-excel; charset=UTF-8">
                <!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet>
                <x:Name>Riwayat Audit</x:Name>
                <x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions>
                </x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->
                <style>
                table { border-collapse: collapse; font-family: Calibri, Arial, sans-serif; font-size: 11pt; }
                th { background-color: #0284c7; color: #ffffff; font-weight: bold; border: 0.5pt solid #94a3b8; padding: 7px 10px; text-align: center; }
                td { border: 0.5pt solid #cbd5e1; padding: 6px 8px; vertical-align: middle; }
                .text { mso-number-format:"\@"; }
                .num { mso-number-format:"0"; text-align: right; }
                .danger { background-color: #fee2e2; color: #b91c1c; font-weight: bold; text-align: center; }
                .success { background-color: #ecfdf5; color: #047857; font-weight: bold; text-align: center; }
                .title-header { font-size: 14pt; font-weight: bold; color: #0f172a; }
                .meta-row { font-size: 10pt; color: #475569; }
                </style>
                </head>
                <body>
                <table>
                <tr><td colspan="11" class="title-header">LAPORAN AUDIT KEINDAHAN ARMADA BLUEBIRD</td></tr>
                <tr><td colspan="11" class="meta-row">Tanggal Unduh: $dateStr | Total Audit: ${audits.size} Unit</td></tr>
                <tr><td colspan="11"></td></tr>
                <tr>
                    <th>No</th>
                    <th>Tanggal & Waktu</th>
                    <th>Periode</th>
                    <th>Pool</th>
                    <th>No Mobil</th>
                    <th>Plat Nomor</th>
                    <th>Auditor</th>
                    <th>Sample</th>
                    <th>Paket Service</th>
                    <th>Total Poin Temuan</th>
                    <th>Status Kelayakan</th>
                </tr>
            """.trimIndent())
            append("\n")

            audits.forEachIndexed { index, a ->
                val isStandar = (a.totalPoinTemuan == 0)
                val statusClass = if (isStandar) "success" else "danger"
                val statusText = if (isStandar) "STANDAR" else "TEMUAN"
                append("""
                    <tr>
                        <td class="num">${index + 1}</td>
                        <td class="text">${a.tanggal}</td>
                        <td class="text">${a.periode}</td>
                        <td class="text"><b>${a.pool}</b></td>
                        <td class="text"><b>${a.noMobil}</b></td>
                        <td class="text">${a.platNomer}</td>
                        <td class="text">${a.namaPetugas}</td>
                        <td class="text">${a.jenisSample}</td>
                        <td class="text">${a.paketService}</td>
                        <td class="num ${if (isStandar) "" else "danger"}">${a.totalPoinTemuan}</td>
                        <td class="$statusClass">$statusText</td>
                    </tr>
                """.trimIndent())
                append("\n")
            }

            append("""
                </table>
                </body>
                </html>
            """.trimIndent())
        }
    }

    private suspend fun handleClientSocket(socket: Socket) = withContext(Dispatchers.IO) {
        activeConnectionsCount++
        try {
            socket.soTimeout = 15000
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = socket.getOutputStream()

            val requestLine = input.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val fullPath = parts[1]

            // Parse path and query parameters
            val questionMarkIdx = fullPath.indexOf('?')
            val path = if (questionMarkIdx != -1) fullPath.substring(0, questionMarkIdx) else fullPath
            val queryString = if (questionMarkIdx != -1) fullPath.substring(questionMarkIdx + 1) else ""
            val queryParams = parseQuery(queryString)

            // Read remaining headers until empty line & track Content-Length
            var headerLine: String?
            var contentLength = 0
            while (true) {
                headerLine = input.readLine()
                if (headerLine.isNullOrEmpty()) break
                if (headerLine.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            // Handle CORS OPTIONS preflight
            if (method.equals("OPTIONS", ignoreCase = true)) {
                sendCorsPreflight(output)
                return@withContext
            }

            // Read request body if present (POST / PUT)
            var requestBody = ""
            if (contentLength > 0 && (method.equals("POST", ignoreCase = true) || method.equals("PUT", ignoreCase = true))) {
                val charBuf = CharArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val count = input.read(charBuf, readTotal, contentLength - readTotal)
                    if (count == -1) break
                    readTotal += count
                }
                requestBody = String(charBuf, 0, readTotal)
            }

            when {
                path == "/" || path == "/index.html" -> {
                    serveHtmlDashboard(output)
                }
                method.equals("POST", ignoreCase = true) && (path == "/api/audit" || path == "/api/submit-audit") -> {
                    handleSaveAuditJson(requestBody, output)
                }
                path == "/api/audits" -> {
                    serveAuditsJson(output)
                }
                path == "/api/vehicles" -> {
                    serveVehiclesJson(output)
                }
                path == "/api/users" -> {
                    serveUsersJson(output)
                }
                path == "/api/spec" -> {
                    serveKpiSpecJson(output)
                }
                path == "/api/bobot" -> {
                    serveBobotJson(output)
                }
                path == "/api/pools" -> {
                    servePoolsJson(output)
                }
                path == "/api/samples" -> {
                    serveSamplesJson(output)
                }
                path == "/api/grades" -> {
                    serveGradesJson(output)
                }
                path == "/api/periodes" -> {
                    servePeriodesJson(output)
                }
                path == "/api/photo" -> {
                    servePhoto(output, queryParams)
                }
                path == "/export.csv" || path == "/api/export" -> {
                    serveExportCsv(output)
                }
                path == "/export.xls" || path == "/api/export-excel" -> {
                    serveExportExcel(output)
                }
                path == "/api/status" -> {
                    serveStatusJson(output)
                }
                else -> {
                    sendNotFound(output)
                }
            }
        } catch (t: Throwable) {
            Log.w("WebReportServer", "Error handling client: ${t.message}")
        } finally {
            activeConnectionsCount = (activeConnectionsCount - 1).coerceAtLeast(0)
            try {
                socket.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        for (param in query.split("&")) {
            val pair = param.split("=", limit = 2)
            if (pair.isNotEmpty()) {
                val key = URLDecoder.decode(pair[0], "UTF-8")
                val value = if (pair.size > 1) URLDecoder.decode(pair[1], "UTF-8") else ""
                result[key] = value
            }
        }
        return result
    }

    private suspend fun serveHtmlDashboard(output: OutputStream) {
        val auditsJson = getAuditsJsonString(embedBase64Photos = false)
        val specJson = getKpiSpecJsonString()
        val bobotJson = getBobotJsonString()
        val poolsJson = getPoolsJsonString()
        val samplesJson = getSamplesJsonString()
        val gradesJson = getGradesJsonString()
        val periodesJson = getPeriodesJsonString()
        val vehiclesJson = getVehiclesJsonString()
        val usersJson = getUsersJsonString()
        val html = WebDashboardHtml.generate(
            embeddedAuditsJson = auditsJson,
            embeddedSpecJson = specJson,
            embeddedBobotJson = bobotJson,
            embeddedPoolsJson = poolsJson,
            embeddedSamplesJson = samplesJson,
            embeddedGradesJson = gradesJson,
            embeddedPeriodesJson = periodesJson,
            embeddedVehiclesJson = vehiclesJson,
            embeddedUsersJson = usersJson
        )
        val bytes = html.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n" +
                "Cache-Control: no-cache\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private suspend fun servePoolsJson(output: OutputStream) {
        val jsonStr = getPoolsJsonString()
        val bytes = jsonStr.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private suspend fun serveSamplesJson(output: OutputStream) {
        val jsonStr = getSamplesJsonString()
        val bytes = jsonStr.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private suspend fun serveGradesJson(output: OutputStream) {
        val jsonStr = getGradesJsonString()
        val bytes = jsonStr.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private suspend fun servePeriodesJson(output: OutputStream) {
        val jsonStr = getPeriodesJsonString()
        val bytes = jsonStr.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private suspend fun serveBobotJson(output: OutputStream) {
        val jsonStr = getBobotJsonString()
        val bytes = jsonStr.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private suspend fun serveAuditsJson(output: OutputStream) {
        val jsonStr = getAuditsJsonString(embedBase64Photos = false)
        val bytes = jsonStr.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveKpiSpecJson(output: OutputStream) {
        val jsonStr = getKpiSpecJsonString()
        val bytes = jsonStr.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private suspend fun serveVehiclesJson(output: OutputStream) {
        val jsonStr = getVehiclesJsonString()
        sendJsonResponse(output, 200, jsonStr)
    }

    private suspend fun serveUsersJson(output: OutputStream) {
        val jsonStr = getUsersJsonString()
        sendJsonResponse(output, 200, jsonStr)
    }

    private fun sendCorsPreflight(output: OutputStream) {
        val headers = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, Authorization\r\n" +
                "Access-Control-Max-Age: 86400\r\n" +
                "Connection: close\r\n\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun sendJsonResponse(output: OutputStream, statusCode: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val statusText = if (statusCode == 200) "OK" else if (statusCode == 400) "Bad Request" else "Internal Server Error"
        val headers = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun saveBase64Image(dataUri: String, prefix: String): String {
        return try {
            val base64Data = if (dataUri.contains(",")) dataUri.substringAfter(",") else dataUri
            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val photosDir = File(context.filesDir, "audit_photos")
            if (!photosDir.exists()) photosDir.mkdirs()
            val file = File(photosDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { it.write(imageBytes) }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("WebReportServer", "Failed to save base64 image", e)
            dataUri
        }
    }

    private suspend fun handleSaveAuditJson(body: String, output: OutputStream) = withContext(Dispatchers.IO) {
        try {
            if (body.isBlank()) {
                sendJsonResponse(output, 400, JSONObject().put("success", false).put("message", "Body kosong").toString())
                return@withContext
            }
            val json = JSONObject(body)
            val noMobil = json.optString("noMobil").trim()
            if (noMobil.isBlank()) {
                sendJsonResponse(output, 400, JSONObject().put("success", false).put("message", "Nomor Mobil wajib diisi").toString())
                return@withContext
            }

            val tanggal = json.optString("tanggal").trim().ifBlank {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            }
            val periode = json.optString("periode").trim()
            val nip = json.optString("nip").trim()
            val namaPetugas = json.optString("namaPetugas").trim().ifBlank { "Petugas PC" }
            val platNomer = json.optString("platNomer").trim().ifBlank { "B-$noMobil-AUT" }
            val tipe = json.optString("tipe").trim().ifBlank { "Armada Standar" }
            val warna = json.optString("warna").trim().ifBlank { "Standar" }
            val tahun = json.optString("tahun").trim()
            val pool = json.optString("pool").trim().ifBlank { "SKT" }
            val jenisSample = json.optString("jenisSample").trim().ifBlank { "OPERASIONAL" }
            val tanggalService = json.optString("tanggalService").trim()
            val sampleTanggal = json.optString("sampleTanggal").trim().ifBlank { tanggalService }
            val paketService = json.optString("paketService").trim()

            // Scores
            val scoresObj = json.optJSONObject("kpiScores") ?: JSONObject()
            val completeScores = mutableMapOf<String, Int>()
            val keys = scoresObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                completeScores[k] = scoresObj.optInt(k, 0)
            }

            val isOperasional = jenisSample.equals("OPERASIONAL", ignoreCase = true)
            val hiddenItemIds = if (isOperasional) {
                setOf("pelumas_engsel", "kondisi_ruang_mesin", "kondisi_kap_mesin_dalam", "kondisi_tutup_bbm", "cuc_ruang_mesin", "cuc_kebersihan_engsel")
            } else emptySet()

            if (isOperasional) {
                hiddenItemIds.forEach { itId ->
                    completeScores[itId] = 0
                }
            }

            // Notes
            val notesObj = json.optJSONObject("kpiNotes") ?: JSONObject()
            val cleanKpiNotes = mutableMapOf<String, String>()
            val nKeys = notesObj.keys()
            while (nKeys.hasNext()) {
                val k = nKeys.next()
                val v = notesObj.optString(k).trim()
                if (v.isNotBlank()) cleanKpiNotes[k] = v
            }

            // Photos
            val photosObj = json.optJSONObject("kpiPhotos") ?: JSONObject()
            val cleanKpiPhotos = mutableMapOf<String, String>()
            val pKeys = photosObj.keys()
            while (pKeys.hasNext()) {
                val k = pKeys.next()
                val v = photosObj.optString(k).trim()
                if (v.isNotBlank()) {
                    if (v.startsWith("data:image")) {
                        val savedPath = saveBase64Image(v, "audit_${noMobil}_$k")
                        cleanKpiPhotos[k] = savedPath
                    } else {
                        cleanKpiPhotos[k] = v
                    }
                }
            }

            // Flatten 20 photos & notes for Google Sheets backward compatibility
            val flatPhotos = Array(20) { "" }
            val flatNotes = Array(20) { "" }

            val stdCarPhotoKeys = listOf(
                "foto_depan" to "Foto Depan",
                "foto_belakang" to "Foto Belakang",
                "foto_kanan" to "Foto Samping Kanan",
                "foto_kiri" to "Foto Samping Kiri",
                "foto_interior" to "Foto Interior Depan",
                "foto_bagasi" to "Foto Bagasi",
                "foto_mesin" to "Foto Ruang Mesin"
            )
            for (idx in stdCarPhotoKeys.indices) {
                val (k, label) = stdCarPhotoKeys[idx]
                val path = cleanKpiPhotos[k]
                if (!path.isNullOrBlank()) {
                    flatPhotos[idx] = path
                    flatNotes[idx] = cleanKpiNotes[k].orEmpty().ifBlank { label }
                }
            }

            var findingIdx = 7
            for ((itemKey, score) in completeScores) {
                if (score > 0 && findingIdx < 20) {
                    for (p in 0 until score) {
                        val subKey = "${itemKey}_$p"
                        val pPath = cleanKpiPhotos[subKey] ?: cleanKpiPhotos[itemKey]
                        if (!pPath.isNullOrBlank() && findingIdx < 20) {
                            flatPhotos[findingIdx] = pPath
                            flatNotes[findingIdx] = cleanKpiNotes[subKey] ?: cleanKpiNotes[itemKey] ?: itemKey
                            findingIdx++
                        }
                    }
                }
            }

            val totalPoin = completeScores.values.sum()
            val effectiveKode = com.example.util.UniqueCodeGenerator.generateAuditCode()
            val currentTimestamp = System.currentTimeMillis()

            val newAudit = DataAuditEntity(
                cloudId = 0L,
                auditUuid = effectiveKode,
                kodeUnik = effectiveKode,
                tanggal = tanggal,
                timestamp = currentTimestamp,
                periode = periode,
                nip = nip,
                namaPetugas = namaPetugas,
                noMobil = noMobil,
                platNomer = platNomer,
                tipe = tipe,
                warna = warna,
                tahun = tahun,
                tanggalService = if (jenisSample.equals("SERVICE", ignoreCase = true)) tanggalService else "",
                service = if (jenisSample.equals("SERVICE", ignoreCase = true)) paketService else "",
                pool = pool,
                totalPoinTemuan = totalPoin,
                jenisSample = jenisSample,
                sampleTanggal = if (jenisSample.equals("SERVICE", ignoreCase = true)) sampleTanggal else "",
                paketService = if (jenisSample.equals("SERVICE", ignoreCase = true)) paketService else "",
                kpiScoresJson = JSONObject(completeScores as Map<*, *>).toString(),
                kpiPhotosJson = JSONObject(cleanKpiPhotos as Map<*, *>).toString(),
                kpiNotesJson = JSONObject(cleanKpiNotes as Map<*, *>).toString(),
                photo1 = flatPhotos[0], catatanPhoto1 = flatNotes[0],
                photo2 = flatPhotos[1], catatanPhoto2 = flatNotes[1],
                photo3 = flatPhotos[2], catatanPhoto3 = flatNotes[2],
                photo4 = flatPhotos[3], catatanPhoto4 = flatNotes[3],
                photo5 = flatPhotos[4], catatanPhoto5 = flatNotes[4],
                photo6 = flatPhotos[5], catatanPhoto6 = flatNotes[5],
                photo7 = flatPhotos[6], catatanPhoto7 = flatNotes[6],
                photo8 = flatPhotos[7], catatanPhoto8 = flatNotes[7],
                photo9 = flatPhotos[8], catatanPhoto9 = flatNotes[8],
                photo10 = flatPhotos[9], catatanPhoto10 = flatNotes[9],
                photo11 = flatPhotos[10], catatanPhoto11 = flatNotes[10],
                photo12 = flatPhotos[11], catatanPhoto12 = flatNotes[11],
                photo13 = flatPhotos[12], catatanPhoto13 = flatNotes[12],
                photo14 = flatPhotos[13], catatanPhoto14 = flatNotes[13],
                photo15 = flatPhotos[14], catatanPhoto15 = flatNotes[14],
                photo16 = flatPhotos[15], catatanPhoto16 = flatNotes[15],
                photo17 = flatPhotos[16], catatanPhoto17 = flatNotes[17],
                photo18 = flatPhotos[17], catatanPhoto18 = flatNotes[17],
                photo19 = flatPhotos[18], catatanPhoto19 = flatNotes[18],
                photo20 = flatPhotos[19], catatanPhoto20 = flatNotes[19],
                isSynced = false
            )

            val insertedId = auditDao.insertAudit(newAudit)

            // Trigger background sync to Google Sheets
            scope.launch {
                try {
                    val repo = com.example.data.repository.AuditRepository(context, database)
                    repo.syncAllPendingAudits()
                } catch (e: Exception) {
                    Log.w("WebReportServer", "Background sync after web input error: ${e.message}")
                }
            }

            val resObj = JSONObject()
            resObj.put("success", true)
            resObj.put("message", "Data audit armada $noMobil berhasil disimpan!")
            resObj.put("id", insertedId)
            resObj.put("kodeUnik", effectiveKode)
            resObj.put("noMobil", noMobil)
            resObj.put("totalPoinTemuan", totalPoin)
            resObj.put("status", if (totalPoin == 0) "STANDAR" else "TEMUAN")

            sendJsonResponse(output, 200, resObj.toString())
        } catch (e: Exception) {
            Log.e("WebReportServer", "Error saving audit from web", e)
            sendJsonResponse(output, 500, JSONObject().put("success", false).put("message", "Gagal menyimpan audit: ${e.message}").toString())
        }
    }

    private suspend fun servePhoto(output: OutputStream, params: Map<String, String>) {
        val auditId = params["auditId"]?.toLongOrNull() ?: 0L
        val key = params["key"].orEmpty()

        var imageBytes: ByteArray? = null

        if (auditId > 0L) {
            val audit = auditDao.getAuditById(auditId)
            if (audit != null) {
                val photos = PhotoDownloadHelper.extractPhotosFromAudit(audit)
                val target = photos.find { it.itemKey == key } ?: photos.firstOrNull()
                if (target != null && target.photoPath.isNotBlank()) {
                    imageBytes = loadPhotoBytes(target.photoPath)
                }
            }
        }

        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val headers = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: image/jpeg\r\n" +
                    "Content-Length: ${imageBytes.size}\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Cache-Control: public, max-age=3600\r\n" +
                    "Connection: close\r\n" +
                    "\r\n"
            output.write(headers.toByteArray(Charsets.UTF_8))
            output.write(imageBytes)
        } else {
            // Serve friendly SVG placeholder
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="240" height="180" viewBox="0 0 240 180" fill="#f1f5f9">
                    <rect width="240" height="180" fill="#f8fafc" stroke="#cbd5e1" stroke-width="2"/>
                    <path d="M90 65 L100 50 L140 50 L150 65 L180 65 A10 10 0 0 1 190 75 L190 135 A10 10 0 0 1 180 145 L60 145 A10 10 0 0 1 50 135 L50 75 A10 10 0 0 1 60 65 Z" fill="#94a3b8"/>
                    <circle cx="120" cy="105" r="25" fill="#f8fafc"/>
                    <text x="120" y="165" font-family="sans-serif" font-size="12" fill="#64748b" text-anchor="middle">Foto Tidak Tersedia</text>
                </svg>
            """.trimIndent()
            val svgBytes = svg.toByteArray(Charsets.UTF_8)
            val headers = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: image/svg+xml\r\n" +
                    "Content-Length: ${svgBytes.size}\r\n" +
                    "Connection: close\r\n" +
                    "\r\n"
            output.write(headers.toByteArray(Charsets.UTF_8))
            output.write(svgBytes)
        }
        output.flush()
    }

    private fun loadPhotoBytes(path: String): ByteArray? {
        return try {
            if (path.startsWith("data:image")) {
                val base64 = path.substringAfter(",")
                Base64.decode(base64, Base64.DEFAULT)
            } else if (path.startsWith("/")) {
                val file = File(path)
                if (file.exists()) {
                    file.readBytes()
                } else null
            } else {
                null
            }
        } catch (t: Throwable) {
            Log.w("WebReportServer", "Error reading photo: ${t.message}")
            null
        }
    }

    private suspend fun serveExportCsv(output: OutputStream) {
        val audits = auditDao.getAllAuditsList()
        val csv = buildString {
            append("No,Tanggal,Periode,Pool,No Mobil,Plat Nomor,Petugas,Sample,Paket Service,Total Poin Temuan,Status Kelayakan\n")
            audits.forEachIndexed { index, a ->
                val status = if (a.totalPoinTemuan == 0) "STANDAR" else "TEMUAN"
                val row = listOf(
                    (index + 1).toString(),
                    "\"${a.tanggal}\"",
                    "\"${a.periode}\"",
                    "\"${a.pool}\"",
                    "\"${a.noMobil}\"",
                    "\"${a.platNomer}\"",
                    "\"${a.namaPetugas}\"",
                    "\"${a.jenisSample}\"",
                    "\"${a.paketService}\"",
                    a.totalPoinTemuan.toString(),
                    status
                ).joinToString(",")
                append(row)
                append("\n")
            }
        }

        val bytes = csv.toByteArray(Charsets.UTF_8)
        val filename = "Laporan_Audit_Armada_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/csv; charset=UTF-8\r\n" +
                "Content-Disposition: attachment; filename=\"$filename\"\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private suspend fun serveExportExcel(output: OutputStream) {
        val excelContent = generateExcelContent()
        val bytes = excelContent.toByteArray(Charsets.UTF_8)
        val filename = "Laporan_Audit_Armada_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.xls"
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/vnd.ms-excel; charset=UTF-8\r\n" +
                "Content-Disposition: attachment; filename=\"$filename\"\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveStatusJson(output: OutputStream) {
        val obj = JSONObject()
        obj.put("status", "running")
        obj.put("ip", getLocalIpAddress())
        obj.put("port", currentPort)
        obj.put("timestamp", System.currentTimeMillis())
        val bytes = obj.toString().toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun sendNotFound(output: OutputStream) {
        val msg = "404 Not Found"
        val bytes = msg.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }
}
