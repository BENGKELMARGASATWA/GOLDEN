package com.example.server

/**
 * Web PC Realtime Dashboard Template
 *
 * Modern Real-time Audit Armada Monitoring Center for PC Desktop & Web browsers.
 * Displays:
 * 1. Riwayat Audit (Daftar Riwayat Audit Lengkap + Foto & Pencarian)
 * 2. Resume Audit (Matriks Resume Armada 14 Unit dengan Sticky Column)
 * 3. Bobot Resume (Tabel Bobot & Nilai Temuan Akumulasi)
 * 4. Rekap Nilai (Rekap Nilai Sub Group & Evaluasi Grade KPI A/B/C/D/E)
 * 5. Rekap Temuan (Sebaran Temuan Operasional vs Service dengan Ambang Kritis > 50%)
 */
object WebDashboardHtml {

    fun generate(
        embeddedAuditsJson: String? = null,
        embeddedSpecJson: String? = null,
        embeddedBobotJson: String? = null,
        embeddedPoolsJson: String? = null,
        embeddedSamplesJson: String? = null,
        embeddedGradesJson: String? = null,
        embeddedPeriodesJson: String? = null,
        embeddedVehiclesJson: String? = null,
        embeddedUsersJson: String? = null
    ): String {
        val safeAudits = if (!embeddedAuditsJson.isNullOrBlank()) embeddedAuditsJson else "null"
        val safeSpec = if (!embeddedSpecJson.isNullOrBlank()) embeddedSpecJson else "null"
        val safeBobot = if (!embeddedBobotJson.isNullOrBlank()) embeddedBobotJson else "null"
        val safePools = if (!embeddedPoolsJson.isNullOrBlank()) embeddedPoolsJson else "null"
        val safeSamples = if (!embeddedSamplesJson.isNullOrBlank()) embeddedSamplesJson else "null"
        val safeGrades = if (!embeddedGradesJson.isNullOrBlank()) embeddedGradesJson else "null"
        val safePeriodes = if (!embeddedPeriodesJson.isNullOrBlank()) embeddedPeriodesJson else "null"
        val safeVehicles = if (!embeddedVehiclesJson.isNullOrBlank()) embeddedVehiclesJson else "null"
        val safeUsers = if (!embeddedUsersJson.isNullOrBlank()) embeddedUsersJson else "null"

        return """
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AUDIT ARMADA BLUEBIRD - REALTIME PC COMMAND CENTER</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
${getCss()}
    </style>
</head>
<body>
${getHtmlBody()}
    <script>
${getScript(safeAudits, safeSpec, safeBobot, safePools, safeSamples, safeGrades, safePeriodes, safeVehicles, safeUsers)}
    </script>
</body>
</html>
        """.trimIndent()
    }

    private fun getCss(): String {
        return """
        :root {
            --primary: #0284c7;
            --primary-dark: #0369a1;
            --primary-light: #e0f2fe;
            --primary-focus: rgba(2, 132, 199, 0.15);
            --navy-900: #0f172a;
            --navy-800: #1e293b;
            --navy-700: #334155;
            --slate-50: #f8fafc;
            --slate-100: #f1f5f9;
            --slate-200: #e2e8f0;
            --slate-300: #cbd5e1;
            --slate-400: #94a3b8;
            --slate-500: #64748b;
            --slate-600: #475569;
            --slate-700: #334155;
            --slate-800: #1e293b;
            --slate-900: #0f172a;
            --success: #10b981;
            --success-dark: #059669;
            --success-light: #ecfdf5;
            --warning: #f59e0b;
            --warning-dark: #d97706;
            --warning-light: #fef3c7;
            --danger: #ef4444;
            --danger-dark: #dc2626;
            --danger-light: #fee2e2;
            --teal: #0d9488;
            --teal-light: #ccfbf1;
            --indigo: #6366f1;
            --indigo-light: #e0e7ff;
            --card-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
            --card-shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.07), 0 4px 6px -4px rgba(0, 0, 0, 0.05);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Plus Jakarta Sans', system-ui, -apple-system, sans-serif;
            background-color: #f1f5f9;
            color: var(--slate-800);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            line-height: 1.5;
        }

        /* Top Executive Header */
        header {
            background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0369a1 100%);
            color: #ffffff;
            padding: 14px 28px;
            box-shadow: 0 4px 12px rgba(15, 23, 42, 0.15);
            position: sticky;
            top: 0;
            z-index: 100;
        }

        .header-content {
            max-width: 1720px;
            margin: 0 auto;
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 16px;
        }

        .brand-box {
            display: flex;
            align-items: center;
            gap: 14px;
        }

        .brand-icon {
            width: 44px;
            height: 44px;
            background: linear-gradient(135deg, #0284c7 0%, #38bdf8 100%);
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
            box-shadow: 0 4px 10px rgba(2, 132, 199, 0.35);
        }

        .brand-title {
            font-size: 17px;
            font-weight: 800;
            letter-spacing: 0.5px;
            color: #ffffff;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .brand-subtitle {
            font-size: 12px;
            color: #94a3b8;
            display: flex;
            align-items: center;
            gap: 10px;
            margin-top: 2px;
        }

        .live-badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: rgba(16, 185, 129, 0.15);
            border: 1px solid rgba(16, 185, 129, 0.4);
            color: #34d399;
            padding: 2px 8px;
            border-radius: 9999px;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 0.4px;
        }

        .pulse-dot {
            width: 7px;
            height: 7px;
            background-color: #34d399;
            border-radius: 50%;
            animation: pulse-glow 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
        }

        @keyframes pulse-glow {
            0%, 100% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.4; transform: scale(1.3); }
        }

        .header-actions {
            display: flex;
            align-items: center;
            gap: 8px;
            flex-wrap: wrap;
        }

        .btn {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 8px 14px;
            font-size: 12px;
            font-weight: 600;
            border-radius: 8px;
            border: none;
            cursor: pointer;
            text-decoration: none;
            transition: all 0.18s ease;
            white-space: nowrap;
        }

        .btn-outline-white {
            background: rgba(255, 255, 255, 0.08);
            color: #ffffff;
            border: 1px solid rgba(255, 255, 255, 0.2);
        }

        .btn-outline-white:hover {
            background: rgba(255, 255, 255, 0.18);
            border-color: rgba(255, 255, 255, 0.4);
        }

        .btn-primary {
            background: #0284c7;
            color: #ffffff;
        }

        .btn-primary:hover {
            background: #0369a1;
        }

        .btn-white {
            background: #ffffff;
            color: var(--navy-900);
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }

        .btn-white:hover {
            background: #f8fafc;
            color: var(--primary);
        }

        .btn-success {
            background: #10b981;
            color: #ffffff;
            font-weight: 700;
        }

        .btn-success:hover {
            background: #059669;
        }

        /* Container Layout */
        .container {
            max-width: 1720px;
            width: 100%;
            margin: 0 auto;
            padding: 20px 24px 60px;
            display: flex;
            flex-direction: column;
            gap: 18px;
        }

        /* Top Metric Cards Strip */
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
            gap: 14px;
        }

        .stat-card {
            background: #ffffff;
            border-radius: 12px;
            padding: 16px 20px;
            box-shadow: var(--card-shadow);
            border: 1px solid var(--slate-200);
            position: relative;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            transition: transform 0.15s ease, box-shadow 0.15s ease;
        }

        .stat-card:hover {
            transform: translateY(-2px);
            box-shadow: var(--card-shadow-lg);
        }

        .stat-card::before {
            content: '';
            position: absolute;
            left: 0;
            top: 0;
            bottom: 0;
            width: 4px;
            background: var(--primary);
        }

        .stat-card.stat-standar::before { background: var(--success); }
        .stat-card.stat-temuan::before { background: var(--danger); }
        .stat-card.stat-poin::before { background: var(--warning); }
        .stat-card.stat-kpi::before { background: var(--teal); }

        .stat-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 6px;
        }

        .stat-label {
            font-size: 12px;
            font-weight: 700;
            color: var(--slate-500);
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .stat-icon {
            font-size: 18px;
            opacity: 0.85;
        }

        .stat-value {
            font-size: 26px;
            font-weight: 800;
            color: var(--slate-900);
            line-height: 1.2;
        }

        .stat-sub {
            font-size: 11.5px;
            color: var(--slate-500);
            margin-top: 4px;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        /* Toolbar Card (Tabs + Global Filters) */
        .toolbar-card {
            background: #ffffff;
            border-radius: 14px;
            padding: 14px 20px;
            box-shadow: var(--card-shadow);
            border: 1px solid var(--slate-200);
            display: flex;
            flex-direction: column;
            gap: 14px;
        }

        .tabs-header-bar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 12px;
            border-bottom: 1px solid var(--slate-200);
            padding-bottom: 12px;
        }

        .tabs {
            display: flex;
            align-items: center;
            gap: 6px;
            overflow-x: auto;
            padding-bottom: 2px;
        }

        .tab-btn {
            background: var(--slate-100);
            border: 1px solid var(--slate-200);
            padding: 9px 18px;
            border-radius: 10px;
            font-size: 13px;
            font-weight: 700;
            color: var(--slate-600);
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 8px;
            transition: all 0.15s ease;
            white-space: nowrap;
        }

        .tab-btn:hover {
            background: var(--slate-200);
            color: var(--slate-900);
        }

        .tab-btn.active {
            background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
            color: #ffffff;
            border-color: #0284c7;
            box-shadow: 0 4px 10px rgba(2, 132, 199, 0.25);
        }

        .tab-badge {
            background: rgba(0, 0, 0, 0.08);
            padding: 2px 7px;
            border-radius: 9999px;
            font-size: 11px;
            font-weight: 800;
        }

        .tab-btn.active .tab-badge {
            background: rgba(255, 255, 255, 0.25);
            color: #ffffff;
        }

        .filter-controls {
            display: flex;
            align-items: center;
            gap: 10px;
            flex-wrap: wrap;
        }

        .filter-group {
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .filter-label {
            font-size: 12px;
            font-weight: 700;
            color: var(--slate-600);
            white-space: nowrap;
        }

        .select-input, .text-input {
            padding: 7px 12px;
            font-size: 12.5px;
            font-weight: 600;
            color: var(--slate-800);
            background-color: #ffffff;
            border: 1px solid var(--slate-300);
            border-radius: 8px;
            outline: none;
            transition: border-color 0.15s ease, box-shadow 0.15s ease;
            font-family: inherit;
        }

        .select-input:focus, .text-input:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px var(--primary-focus);
        }

        .text-input {
            min-width: 260px;
        }

        /* View Container Cards */
        .view-content {
            display: none;
        }

        .view-content.active {
            display: block;
        }

        .table-card {
            background: #ffffff;
            border-radius: 14px;
            box-shadow: var(--card-shadow);
            border: 1px solid var(--slate-200);
            overflow: hidden;
        }

        .table-card-header {
            padding: 16px 22px;
            background: #ffffff;
            border-bottom: 1px solid var(--slate-200);
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 12px;
        }

        .table-title-box {
            display: flex;
            flex-direction: column;
            gap: 2px;
        }

        .table-title {
            font-size: 15px;
            font-weight: 800;
            color: var(--slate-900);
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .table-subtitle {
            font-size: 12px;
            color: var(--slate-500);
        }

        .table-card-actions {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        /* General Table Styling */
        .table-responsive {
            width: 100%;
            overflow-x: auto;
            max-height: calc(100vh - 280px);
            position: relative;
        }

        table.data-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 12px;
            text-align: left;
        }

        table.data-table th {
            background: #f8fafc;
            color: var(--slate-700);
            font-weight: 700;
            padding: 10px 14px;
            border-bottom: 2px solid var(--slate-200);
            white-space: nowrap;
            position: sticky;
            top: 0;
            z-index: 10;
        }

        table.data-table td {
            padding: 10px 14px;
            border-bottom: 1px solid var(--slate-200);
            color: var(--slate-800);
            vertical-align: middle;
        }

        table.data-table tr:hover td {
            background-color: #f8fafc;
        }

        /* Badges */
        .badge {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 3px 8px;
            border-radius: 6px;
            font-size: 11px;
            font-weight: 700;
            line-height: 1.2;
            white-space: nowrap;
        }

        .badge-standar {
            background: var(--success-light);
            color: var(--success-dark);
            border: 1px solid #a7f3d0;
        }

        .badge-temuan {
            background: var(--danger-light);
            color: var(--danger-dark);
            border: 1px solid #fecaca;
        }

        .badge-op {
            background: #f0fdf4;
            color: #166534;
            border: 1px solid #bbf7d0;
        }

        .badge-svc {
            background: #eff6ff;
            color: #1e40af;
            border: 1px solid #bfdbfe;
        }

        .badge-gray {
            background: var(--slate-100);
            color: var(--slate-600);
            border: 1px solid var(--slate-200);
        }

        .badge-primary {
            background: var(--primary-light);
            color: var(--primary-dark);
            border: 1px solid #bae6fd;
        }

        /* Sticky Left Column for Matrix */
        .matrix-scroll-wrapper {
            width: 100%;
            overflow-x: auto;
            max-height: calc(100vh - 280px);
            position: relative;
        }

        table.matrix-table {
            border-collapse: separate;
            border-spacing: 0;
            width: max-content;
            min-width: 100%;
            font-size: 11.5px;
        }

        table.matrix-table th, table.matrix-table td {
            border-bottom: 1px solid var(--slate-200);
            border-right: 1px solid var(--slate-200);
            padding: 7px 10px;
            text-align: center;
            vertical-align: middle;
        }

        /* Freeze Leftmost Column */
        table.matrix-table .sticky-col {
            position: sticky;
            left: 0;
            z-index: 20;
            background: #ffffff;
            width: 320px;
            min-width: 320px;
            max-width: 320px;
            text-align: left !important;
            font-weight: 600;
            box-shadow: 4px 0 6px -2px rgba(0, 0, 0, 0.06);
        }

        table.matrix-table th.sticky-col {
            z-index: 30;
            background: #f1f5f9;
        }

        table.matrix-table tr.header-row th {
            position: sticky;
            top: 0;
            background: #f8fafc;
            z-index: 15;
            font-weight: 700;
            color: var(--slate-800);
        }

        table.matrix-table tr.header-row th.sticky-col {
            z-index: 35;
            background: #e2e8f0;
        }

        table.matrix-table tr.section-row td {
            background: #0f172a;
            color: #ffffff;
            font-weight: 800;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            text-align: left;
            padding: 9px 12px;
            font-size: 12px;
        }

        table.matrix-table tr.section-row td.sticky-col {
            background: #0f172a !important;
            color: #ffffff !important;
        }

        table.matrix-table tr.group-row td {
            background: #f1f5f9;
            color: var(--navy-800);
            font-weight: 700;
            text-align: left;
            padding: 7px 12px;
            font-size: 11.5px;
        }

        table.matrix-table tr.group-row td.sticky-col {
            background: #f1f5f9 !important;
            color: var(--navy-800) !important;
        }

        .cell-zero {
            color: #10b981;
            font-weight: 700;
        }

        .cell-finding {
            background-color: #fee2e2 !important;
            color: #b91c1c !important;
            font-weight: 800;
        }

        .cell-danger-50 {
            background-color: #fee2e2 !important;
            color: #b91c1c !important;
            font-weight: 800;
        }

        /* Rekap Nilai KPI Showcase Card */
        .kpi-showcase {
            background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
            border: 1px solid var(--slate-200);
            border-radius: 14px;
            padding: 20px;
            margin-bottom: 16px;
            display: grid;
            grid-template-columns: 280px 1fr;
            gap: 20px;
            align-items: center;
        }

        @media (max-width: 992px) {
            .kpi-showcase {
                grid-template-columns: 1fr;
            }
        }

        .kpi-grade-banner {
            background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
            color: #ffffff;
            border-radius: 12px;
            padding: 22px;
            text-align: center;
            box-shadow: 0 6px 16px rgba(15, 23, 42, 0.15);
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
        }

        .grade-letter {
            font-size: 54px;
            font-weight: 900;
            line-height: 1;
            color: #38bdf8;
            text-shadow: 0 2px 10px rgba(56, 189, 248, 0.4);
            margin: 6px 0;
        }

        .kpi-score-badge {
            background: rgba(255, 255, 255, 0.1);
            padding: 4px 12px;
            border-radius: 9999px;
            font-size: 14px;
            font-weight: 800;
            color: #ffffff;
            margin-top: 4px;
        }

        .kpi-section-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 12px;
        }

        .kpi-sub-card {
            background: #ffffff;
            border: 1px solid var(--slate-200);
            border-radius: 10px;
            padding: 14px;
            box-shadow: var(--card-shadow);
        }

        .kpi-sub-label {
            font-size: 11px;
            font-weight: 700;
            color: var(--slate-500);
            text-transform: uppercase;
        }

        .kpi-sub-value {
            font-size: 20px;
            font-weight: 800;
            color: var(--slate-900);
            margin-top: 4px;
        }

        .kpi-formula-bar {
            background: #f8fafc;
            border-left: 4px solid var(--primary);
            padding: 10px 14px;
            border-radius: 0 8px 8px 0;
            font-size: 12px;
            color: var(--slate-600);
            margin-top: 10px;
            font-family: monospace;
        }

        /* Modal Dialog */
        .modal-backdrop {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(15, 23, 42, 0.6);
            backdrop-filter: blur(4px);
            z-index: 1000;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }

        .modal-backdrop.active {
            display: flex;
        }

        .modal-card {
            background: #ffffff;
            border-radius: 16px;
            width: 100%;
            max-width: 900px;
            max-height: 90vh;
            display: flex;
            flex-direction: column;
            box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2);
            overflow: hidden;
        }

        .modal-header {
            padding: 18px 24px;
            background: #f8fafc;
            border-bottom: 1px solid var(--slate-200);
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .modal-title {
            font-size: 16px;
            font-weight: 800;
            color: var(--slate-900);
        }

        .modal-close {
            background: none;
            border: none;
            font-size: 20px;
            color: var(--slate-400);
            cursor: pointer;
            padding: 4px 8px;
            border-radius: 6px;
        }

        .modal-close:hover {
            color: var(--slate-800);
            background: var(--slate-200);
        }

        .modal-body {
            padding: 20px 24px;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            gap: 18px;
        }

        .photo-gallery-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
            gap: 14px;
        }

        .photo-thumb-card {
            background: #f8fafc;
            border: 1px solid var(--slate-200);
            border-radius: 10px;
            overflow: hidden;
            cursor: pointer;
            transition: transform 0.15s ease, box-shadow 0.15s ease;
        }

        .photo-thumb-card:hover {
            transform: translateY(-2px);
            box-shadow: var(--card-shadow-lg);
        }

        .photo-thumb-img {
            width: 100%;
            height: 140px;
            object-fit: cover;
            background: #e2e8f0;
        }

        .photo-thumb-info {
            padding: 8px 10px;
            font-size: 11px;
            font-weight: 600;
            color: var(--slate-700);
        }

        /* Lightbox Zoom Modal */
        .zoom-modal {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.92);
            z-index: 2000;
            flex-direction: column;
        }

        .zoom-modal.active {
            display: flex;
        }

        .zoom-toolbar {
            padding: 12px 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            color: #ffffff;
            background: rgba(0, 0, 0, 0.5);
        }

        .zoom-stage {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
            position: relative;
            cursor: grab;
        }

        .zoom-stage.grabbing {
            cursor: grabbing;
        }

        .zoom-img {
            max-width: 90%;
            max-height: 85vh;
            object-fit: contain;
            transition: transform 0.1s ease-out;
            user-select: none;
        }

        /* Print styles */
        @media print {
            header, .toolbar-card, .btn, .modal-backdrop, .zoom-modal {
                display: none !important;
            }
            body {
                background: #ffffff;
                color: #000000;
            }
            .container {
                padding: 0;
                max-width: 100%;
            }
            .view-content {
                display: block !important;
                page-break-after: always;
            }
            .table-card {
                box-shadow: none;
                border: 1px solid #000000;
            }
            .matrix-scroll-wrapper, .table-responsive {
                max-height: none !important;
                overflow: visible !important;
            }
        }

        /* ================= INPUT AUDIT FORM STYLING ================= */
        .form-section-card {
            background: #ffffff;
            border-radius: 12px;
            padding: 24px;
            box-shadow: var(--card-shadow);
            border: 1px solid var(--slate-200);
            margin-bottom: 24px;
        }

        .form-section-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding-bottom: 14px;
            margin-bottom: 18px;
            border-bottom: 1.5px solid var(--slate-100);
            flex-wrap: wrap;
            gap: 12px;
        }

        .form-section-title {
            font-size: 13.5px;
            font-weight: 800;
            color: var(--navy-900);
            display: flex;
            align-items: center;
            gap: 8px;
            text-transform: uppercase;
            letter-spacing: 0.3px;
        }

        .form-grid-2 {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 16px;
        }

        .form-grid-3 {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 16px;
        }

        .form-grid-4 {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 14px;
        }

        @media (max-width: 900px) {
            .form-grid-2, .form-grid-3, .form-grid-4 {
                grid-template-columns: 1fr;
            }
        }

        .form-group {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        .form-label {
            font-size: 12px;
            font-weight: 700;
            color: var(--slate-700);
        }

        .form-label span.req {
            color: var(--danger);
        }

        .form-control {
            padding: 9px 13px;
            border-radius: 8px;
            border: 1px solid var(--slate-300);
            font-size: 13.5px;
            font-family: inherit;
            color: var(--slate-800);
            background: #fff;
            transition: all 0.2s;
            outline: none;
        }

        .form-control:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px var(--primary-focus);
        }

        .kpi-section-banner {
            background: linear-gradient(135deg, #0f172a, #334155);
            color: white;
            padding: 10px 16px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 800;
            margin-top: 18px;
            margin-bottom: 12px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .kpi-item-row {
            background: #f8fafc;
            border: 1px solid var(--slate-200);
            border-radius: 10px;
            padding: 14px 16px;
            margin-bottom: 10px;
            display: flex;
            flex-direction: column;
            gap: 10px;
            transition: all 0.2s;
        }

        .kpi-item-row.has-finding {
            background: #fff1f2;
            border-color: #fecdd3;
        }

        .kpi-item-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 8px;
        }

        .kpi-item-title {
            font-size: 13.5px;
            font-weight: 700;
            color: var(--navy-900);
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .score-pill-group {
            display: inline-flex;
            background: var(--slate-200);
            padding: 3px;
            border-radius: 8px;
            gap: 2px;
        }

        .score-pill {
            border: none;
            background: transparent;
            padding: 5px 13px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 700;
            color: var(--slate-700);
            cursor: pointer;
            transition: all 0.15s;
        }

        .score-pill:hover {
            background: rgba(255,255,255,0.6);
        }

        .score-pill.active-zero {
            background: var(--success);
            color: white;
            box-shadow: 0 1px 3px rgba(16, 185, 129, 0.4);
        }

        .score-pill.active-finding {
            background: var(--danger);
            color: white;
            box-shadow: 0 1px 3px rgba(239, 68, 68, 0.4);
        }

        .kpi-finding-box {
            display: flex;
            gap: 12px;
            align-items: center;
            flex-wrap: wrap;
            padding-top: 10px;
            border-top: 1px dashed var(--slate-200);
        }

        .photo-upload-btn {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 6px 12px;
            background: white;
            border: 1px solid var(--slate-300);
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
            color: var(--slate-700);
            cursor: pointer;
            transition: all 0.2s;
        }

        .photo-upload-btn:hover {
            border-color: var(--primary);
            color: var(--primary);
        }

        .photo-thumbnail-box {
            position: relative;
            display: inline-block;
            border-radius: 6px;
            overflow: hidden;
            border: 1.5px solid var(--slate-300);
        }

        .photo-thumbnail-box img {
            width: 52px;
            height: 52px;
            object-fit: cover;
            display: block;
        }

        .photo-thumbnail-box .remove-btn {
            position: absolute;
            top: 2px;
            right: 2px;
            background: rgba(0,0,0,0.65);
            color: white;
            border: none;
            border-radius: 50%;
            width: 18px;
            height: 18px;
            font-size: 10px;
            line-height: 18px;
            text-align: center;
            cursor: pointer;
        }

        .photo-thumbnail-box .remove-btn:hover {
            background: rgba(220, 38, 38, 0.9);
        }

        .std-photo-card {
            background: #f8fafc;
            border: 1.5px dashed var(--slate-300);
            border-radius: 8px;
            padding: 12px;
            display: flex;
            flex-direction: column;
            align-items: center;
            text-align: center;
            gap: 8px;
        }

        .std-photo-card.has-photo {
            border-style: solid;
            border-color: var(--primary);
            background: #f0f9ff;
        }

        .std-photo-preview {
            width: 100%;
            height: 90px;
            object-fit: cover;
            border-radius: 6px;
        }

        .sticky-score-bar {
            position: sticky;
            bottom: 0;
            z-index: 90;
            background: rgba(15, 23, 42, 0.96);
            backdrop-filter: blur(8px);
            color: white;
            padding: 14px 24px;
            border-radius: 12px;
            margin-top: 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            box-shadow: 0 -6px 20px rgba(0,0,0,0.25);
            flex-wrap: wrap;
            gap: 14px;
        }
        }
        """.trimIndent()
    }

    private fun getHtmlBody(): String {
        return """
    <!-- Top Executive Header -->
    <header>
        <div class="header-content">
            <div class="brand-box">
                <div class="brand-icon">🚗</div>
                <div>
                    <div class="brand-title">
                        <span>AUDIT ARMADA BLUEBIRD</span>
                        <span style="font-size: 12px; font-weight: 600; opacity: 0.8;">• REALTIME COMMAND CENTER</span>
                    </div>
                    <div class="brand-subtitle">
                        <span>Monitoring Terpadu & Analisis Kinerja Armada</span>
                        <div class="live-badge">
                            <div class="pulse-dot"></div>
                            <span>REALTIME AKTIF</span>
                        </div>
                    </div>
                </div>
            </div>

            <div class="header-actions">
                <button class="btn btn-outline-white" onclick="fetchData()">
                    🔄 Sinkron Sekarang
                </button>
                <button class="btn btn-outline-white" onclick="window.print()">
                    🖨️ Cetak PDF
                </button>
                <button class="btn btn-success" onclick="exportActiveTabExcel()">
                    📊 Download Excel (.xls)
                </button>
                <button class="btn btn-outline-white" onclick="exportAllWorkbookExcel()">
                    📑 Buku Kerja Excel (Semua Sheet)
                </button>
                <button class="btn btn-white" onclick="exportActiveTabCsv()">
                    📥 Ekspor CSV
                </button>
            </div>
        </div>
    </header>

    <!-- Main Container -->
    <div class="container">

        <!-- Top Metric Cards Strip -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-header">
                    <span class="stat-label">Total Armada Diaudit</span>
                    <span class="stat-icon">🚗</span>
                </div>
                <div class="stat-value" id="stat-total-armada">0</div>
                <div class="stat-sub" id="stat-sub-armada">Memuat data audit...</div>
            </div>

            <div class="stat-card stat-standar">
                <div class="stat-header">
                    <span class="stat-label">Armada Standar (0 Poin)</span>
                    <span class="stat-icon">✅</span>
                </div>
                <div class="stat-value" id="stat-standar">0</div>
                <div class="stat-sub" id="stat-sub-standar">Kondisi Prima</div>
            </div>

            <div class="stat-card stat-temuan">
                <div class="stat-header">
                    <span class="stat-label">Armada Temuan (>0 Poin)</span>
                    <span class="stat-icon">⚠️</span>
                </div>
                <div class="stat-value" id="stat-temuan">0</div>
                <div class="stat-sub" id="stat-sub-temuan">Unit Perbaikan</div>
            </div>

            <div class="stat-card stat-poin">
                <div class="stat-header">
                    <span class="stat-label">Total Poin Temuan</span>
                    <span class="stat-icon">📊</span>
                </div>
                <div class="stat-value" id="stat-total-poin">0</div>
                <div class="stat-sub">Akumulasi Seluruh Unit Terfilter</div>
            </div>

            <div class="stat-card stat-kpi">
                <div class="stat-header">
                    <span class="stat-label">KPI CUCIAN</span>
                    <span class="stat-icon">🧼</span>
                </div>
                <div class="stat-value" id="stat-grade-kpi">-</div>
                <div class="stat-sub" id="stat-nilai-kpi">Nilai KPI: -</div>
            </div>
        </div>

        <!-- Toolbar: Modern Tabs & Global Filters -->
        <div class="toolbar-card">
            <div class="tabs-header-bar">
                <div class="tabs">
                    <button class="tab-btn active" id="tab-btn-riwayat" onclick="switchTab('riwayat')">
                        🚗 Riwayat Audit
                        <span class="tab-badge" id="badge-count-riwayat">0</span>
                    </button>
                    <button class="tab-btn" id="tab-btn-resume" onclick="switchTab('resume')">
                        📋 Resume Audit (Matriks)
                        <span class="tab-badge" id="badge-count-resume">0</span>
                    </button>
                    <button class="tab-btn" id="tab-btn-bobot" onclick="switchTab('bobot')">
                        ⚖️ Bobot Resume
                    </button>
                    <button class="tab-btn" id="tab-btn-rekap-nilai" onclick="switchTab('rekap-nilai')">
                        📊 Rekap Nilai
                    </button>
                    <button class="tab-btn" id="tab-btn-rekap-temuan" onclick="switchTab('rekap-temuan')">
                        ⚠️ Rekap Temuan
                    </button>
                    <button class="tab-btn" id="tab-btn-input" onclick="switchTab('input')" style="background: rgba(14, 165, 233, 0.12); border: 1.5px solid rgba(14, 165, 233, 0.4); color: var(--navy-900); font-weight: 700;">
                        ✍️ Input Data Audit
                    </button>
                </div>

                <div style="font-size: 11.5px; color: var(--slate-500); display: flex; align-items: center; gap: 6px;">
                    <span id="last-updated-text">● Terhubung</span>
                </div>
            </div>

            <div class="filter-controls">
                <div class="filter-group">
                    <label class="filter-label">Periode:</label>
                    <select id="filter-periode" class="select-input" onchange="applyFilters()">
                        <option value="ALL">Semua Periode</option>
                    </select>
                </div>

                <div class="filter-group">
                    <label class="filter-label">Pool:</label>
                    <select id="filter-pool" class="select-input" onchange="applyFilters()">
                        <option value="ALL">Semua Pool</option>
                    </select>
                </div>

                <div class="filter-group">
                    <label class="filter-label">Sample:</label>
                    <select id="filter-sample" class="select-input" onchange="applyFilters()">
                        <option value="ALL">Semua Sample</option>
                        <option value="OPERASIONAL">Operasional</option>
                        <option value="SERVICE">Service</option>
                    </select>
                </div>

                <div class="filter-group">
                    <label class="filter-label">Status:</label>
                    <select id="filter-status" class="select-input" onchange="applyFilters()">
                        <option value="ALL">Semua Status</option>
                        <option value="STANDAR">Standar (0 Poin)</option>
                        <option value="TEMUAN">Temuan (>0 Poin)</option>
                    </select>
                </div>

                <div class="filter-group" style="flex: 1; min-width: 240px;">
                    <input type="text" id="filter-search" class="text-input" style="width: 100%;" placeholder="Cari No Mobil, Plat, Petugas, Checklist..." oninput="applyFilters()">
                </div>
            </div>
        </div>

        <!-- TAB 1: Riwayat Audit -->
        <div id="view-riwayat" class="view-content active">
            <div class="table-card">
                <div class="table-card-header">
                    <div class="table-title-box">
                        <div class="table-title">
                            <span>🚗 DAFTAR RIWAYAT AUDIT ARMADA</span>
                            <span class="badge badge-primary" id="riwayat-total-badge">0 Data</span>
                        </div>
                        <div class="table-subtitle">Data riwayat inspeksi lengkap, status kelayakan, catatan, dan foto dokumentasi temuan</div>
                    </div>
                    <div class="table-card-actions">
                        <button class="btn btn-success" onclick="exportRiwayatExcel()">
                            📊 Unduh Excel Riwayat (.xls)
                        </button>
                        <button class="btn btn-primary" onclick="exportRiwayatCsv()">
                            📥 Unduh CSV Riwayat
                        </button>
                    </div>
                </div>

                <div class="table-responsive">
                    <table class="data-table" id="riwayat-table">
                        <thead>
                            <tr>
                                <th style="width: 45px; text-align: center;">No</th>
                                <th>Tanggal & Waktu</th>
                                <th>Periode</th>
                                <th>Pool</th>
                                <th>No Mobil</th>
                                <th>Plat Nomor</th>
                                <th>Auditor</th>
                                <th>Sample</th>
                                <th>Paket Service</th>
                                <th style="text-align: center;">Total Poin</th>
                                <th style="text-align: center;">Status</th>
                                <th style="text-align: center;">Foto</th>
                                <th style="text-align: center;">Aksi</th>
                            </tr>
                        </thead>
                        <tbody id="riwayat-table-body">
                            <!-- Populated via JS -->
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- TAB 2: Resume Audit (Matriks 14 Unit) -->
        <div id="view-resume" class="view-content">
            <div class="table-card">
                <div class="table-card-header">
                    <div class="table-title-box">
                        <div class="table-title">
                            <span>📋 FORM TABEL RESUME MATRIKS ARMADA</span>
                            <span class="badge badge-primary" id="resume-units-count">0 Unit</span>
                        </div>
                        <div class="table-subtitle">Sesuai standar form Bluebird: Kolom DATA CEK ALL di sisi kiri (Sticky), penilaian unit armada di kanan (Maks 14 Mobil)</div>
                    </div>
                    <div class="table-card-actions">
                        <button class="btn btn-success" onclick="exportResumeMatrixExcel()">
                            📊 Unduh Excel Resume (.xls)
                        </button>
                        <button class="btn btn-primary" onclick="exportResumeMatrixCsv()">
                            📥 Unduh CSV Resume
                        </button>
                    </div>
                </div>

                <div class="matrix-scroll-wrapper">
                    <table class="matrix-table" id="resume-matrix-table">
                        <!-- Populated via JS -->
                    </table>
                </div>
            </div>
        </div>

        <!-- TAB 3: Bobot Resume -->
        <div id="view-bobot" class="view-content">
            <div class="table-card">
                <div class="table-card-header">
                    <div class="table-title-box">
                        <div class="table-title">
                            <span>⚖️ TABEL BOBOT RESUME AUDIT ARMADA</span>
                            <span class="badge badge-primary" id="bobot-summary-badge">0 Item Terdaftar</span>
                        </div>
                        <div class="table-subtitle">Akumulasi temuan poin, bobot pengali, dan perhitungan nilai temuan per item pemeriksaan</div>
                    </div>
                    <div class="table-card-actions">
                        <button class="btn btn-success" onclick="exportBobotExcel()">
                            📊 Unduh Excel Bobot (.xls)
                        </button>
                        <button class="btn btn-primary" onclick="exportBobotCsv()">
                            📥 Unduh CSV Bobot
                        </button>
                    </div>
                </div>

                <div class="table-responsive">
                    <table class="data-table" id="bobot-table">
                        <thead>
                            <tr>
                                <th style="width: 50px; text-align: center;">No</th>
                                <th>Tabel Input / Item Pemeriksaan</th>
                                <th>Seksi & Sub Group</th>
                                <th style="text-align: center;">Bobot</th>
                                <th style="text-align: center;">Temuan (Poin)</th>
                                <th style="text-align: center;">Point Max</th>
                                <th style="text-align: center;">Nilai Temuan (Bobot × Point)</th>
                                <th style="text-align: center;">Jumlah Sample</th>
                            </tr>
                        </thead>
                        <tbody id="bobot-table-body">
                            <!-- Populated via JS -->
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- TAB 4: Rekap Nilai & Grade KPI -->
        <div id="view-rekap-nilai" class="view-content">
            <!-- Grand KPI Showcase Banner -->
            <div class="kpi-showcase">
                <div class="kpi-grade-banner">
                    <span style="font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; color: #94a3b8;">KPI CUCIAN</span>
                    <div class="grade-letter" id="showcase-grade-letter" style="font-size: 32px;">-</div>
                    <div class="kpi-score-badge" id="showcase-score-badge">Nilai KPI: -</div>
                    <div style="font-size: 12px; color: #e2e8f0; margin-top: 6px;" id="showcase-grade-desc">Cucian - 20 (Desimal 2 Digit)</div>
                </div>

                <div>
                    <div class="kpi-section-cards">
                        <div class="kpi-sub-card">
                            <div class="kpi-sub-label">Seksi Exterior (/5)</div>
                            <div class="kpi-sub-value" id="kpi-exterior-pct">0.00</div>
                        </div>
                        <div class="kpi-sub-card">
                            <div class="kpi-sub-label">Seksi Interior (/5)</div>
                            <div class="kpi-sub-value" id="kpi-interior-pct">0.00</div>
                        </div>
                        <div class="kpi-sub-card">
                            <div class="kpi-sub-label">Seksi Cucian (/3)</div>
                            <div class="kpi-sub-value" id="kpi-cucian-pct">0.00</div>
                        </div>
                        <div class="kpi-sub-card">
                            <div class="kpi-sub-label">Ketentuan Sample</div>
                            <div class="kpi-sub-value" id="kpi-sample-info">0 Unit</div>
                        </div>
                    </div>

                    <div class="kpi-formula-bar">
                        Rumus Nilai KPI: (((Seksi Exterior + Seksi Interior) / 2) - 20) | KPI CUCIAN: Cucian - 20 (Desimal 2 Digit)
                    </div>
                </div>
            </div>

            <!-- Table Rekap Nilai Sub Group -->
            <div class="table-card">
                <div class="table-card-header">
                    <div class="table-title-box">
                        <div class="table-title">
                            <span>📊 REKAPITULASI NILAI SUB GROUP & BOBOT TEMUAN</span>
                        </div>
                        <div class="table-subtitle">Rincian perhitungan bobot, point max, total point temuan, dan persentase nilai per sub group</div>
                    </div>
                    <div class="table-card-actions">
                        <button class="btn btn-success" onclick="exportRekapNilaiExcel()">
                            📊 Unduh Excel Rekap Nilai (.xls)
                        </button>
                        <button class="btn btn-primary" onclick="exportRekapNilaiCsv()">
                            📥 Unduh CSV Rekap Nilai
                        </button>
                    </div>
                </div>

                <div class="table-responsive">
                    <table class="data-table" id="rekap-nilai-table">
                        <thead>
                            <tr>
                                <th style="width: 45px; text-align: center;">No</th>
                                <th>Seksi & Sub Group</th>
                                <th style="text-align: center;">Total Bobot Temuan</th>
                                <th style="text-align: center;">Nilai Sub Group</th>
                                <th style="text-align: center;">Status Pemeriksaan</th>
                            </tr>
                        </thead>
                        <tbody id="rekap-nilai-table-body">
                            <!-- Populated via JS -->
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- TAB 5: Rekap Temuan (Operasional vs Service) -->
        <div id="view-rekap-temuan" class="view-content">
            <div class="table-card">
                <div class="table-card-header">
                    <div class="table-title-box">
                        <div class="table-title">
                            <span>⚠️ REKAP TEMUAN AUDIT ARMADA (OPERASIONAL & SERVICE)</span>
                            <span class="badge badge-temuan" id="rekap-temuan-critical-badge">0 Temuan Kritis</span>
                        </div>
                        <div class="table-subtitle">Rumus: Jumlah unit temuan / (Sample Operasional + Service). Berwarna merah jika di atas 50%</div>
                    </div>
                    <div class="table-card-actions">
                        <button class="btn btn-success" onclick="exportRekapTemuanExcel()">
                            📊 Unduh Excel Rekap Temuan (.xls)
                        </button>
                        <button class="btn btn-primary" onclick="exportRekapTemuanCsv()">
                            📥 Unduh CSV Rekap Temuan
                        </button>
                    </div>
                </div>

                <div class="matrix-scroll-wrapper">
                    <table class="matrix-table" id="rekap-temuan-table">
                        <!-- Populated via JS -->
                    </table>
                </div>
            </div>
        </div>

        <!-- TAB 6: Form Input Data Audit (Desktop PC) -->
        <div id="view-input" class="view-content">
            <div class="form-section-card">
                <div class="form-section-header">
                    <div>
                        <div class="form-section-title">
                            <span>✍️ FORMULIR INPUT DATA AUDIT ARMADA (PC DESKTOP)</span>
                            <span class="badge badge-standar" style="background:#e0f2fe; color:#0369a1; border-color:#bae6fd;">ONLINE PC INPUT</span>
                        </div>
                        <div style="font-size: 12px; color: var(--slate-500); margin-top: 4px;">
                            Masukkan data audit armada langsung dari browser PC. Data tersimpan ke aplikasi dan tersinkronisasi otomatis secara realtime.
                        </div>
                    </div>
                    <div style="display:flex; gap:8px;">
                        <button type="button" class="btn btn-secondary" onclick="resetAuditForm()">
                            ↺ Reset Form
                        </button>
                        <button type="button" class="btn btn-primary" id="btn-submit-top" onclick="submitAuditForm()" style="background:#0284c7; font-weight:700;">
                            💾 Simpan Audit
                        </button>
                    </div>
                </div>

                <form id="audit-form" onsubmit="event.preventDefault(); submitAuditForm();">
                    <!-- SECTION 1: IDENTITAS PETUGAS & PERIODE -->
                    <div class="form-section-title" style="margin-top: 6px; margin-bottom: 12px;">
                        <span>👤 1. Identitas Petugas & Periode Audit</span>
                    </div>
                    <div class="form-grid-4">
                        <div class="form-group">
                            <label class="form-label">Tanggal Audit <span class="req">*</span></label>
                            <input type="date" id="input-tanggal" class="form-control" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Periode Audit <span class="req">*</span></label>
                            <select id="input-periode" class="form-control" required>
                                <option value="">Pilih Periode...</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">NIP Petugas</label>
                            <input type="text" id="input-nip" list="users-nip-list" class="form-control" placeholder="Contoh: 10293" oninput="onNipInput(this.value)">
                            <datalist id="users-nip-list"></datalist>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Nama Petugas / Auditor <span class="req">*</span></label>
                            <input type="text" id="input-nama-petugas" list="users-name-list" class="form-control" placeholder="Nama Lengkap Auditor" required>
                            <datalist id="users-name-list"></datalist>
                        </div>
                    </div>

                    <!-- SECTION 2: DATA ARMADA KENDARAAN -->
                    <div class="form-section-title" style="margin-top: 24px; margin-bottom: 12px;">
                        <span>🚗 2. Data Armada Kendaraan</span>
                    </div>
                    <div class="form-grid-3">
                        <div class="form-group">
                            <label class="form-label">Nomor Mobil (Pintu) <span class="req">*</span></label>
                            <input type="text" id="input-no-mobil" list="vehicles-list" class="form-control" placeholder="Ketik No Mobil (misal: 1001)" required oninput="onVehicleInput(this.value)" style="font-weight:700; color:var(--primary-dark);">
                            <datalist id="vehicles-list"></datalist>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Plat Nomer</label>
                            <input type="text" id="input-plat" class="form-control" placeholder="B 1234 AUT">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Pool Armada <span class="req">*</span></label>
                            <select id="input-pool" class="form-control" required>
                                <option value="">Pilih Pool...</option>
                            </select>
                        </div>
                    </div>

                    <div class="form-grid-3" style="margin-top: 12px;">
                        <div class="form-group">
                            <label class="form-label">Tipe Armada</label>
                            <input type="text" id="input-tipe" class="form-control" placeholder="Avanza / Transmover / Alvva">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Warna</label>
                            <input type="text" id="input-warna" class="form-control" placeholder="Biru / Putih / Silver">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Tahun Pembuatan</label>
                            <input type="text" id="input-tahun" class="form-control" placeholder="2022">
                        </div>
                    </div>

                    <!-- SECTION 3: JENIS SAMPEL & PARAMETER SERVICE -->
                    <div class="form-section-title" style="margin-top: 24px; margin-bottom: 12px;">
                        <span>📋 3. Jenis Sampel & Parameter</span>
                    </div>
                    <div class="form-grid-3">
                        <div class="form-group">
                            <label class="form-label">Jenis Sampel <span class="req">*</span></label>
                            <select id="input-jenis-sample" class="form-control" onchange="onJenisSampleChange()" required>
                                <option value="OPERASIONAL" selected>OPERASIONAL (Armada Siap Operasi)</option>
                                <option value="SERVICE">SERVICE (Armada Pasca Service Bengkel)</option>
                            </select>
                        </div>
                        <div class="form-group" id="group-tgl-service" style="display:none;">
                            <label class="form-label">Tanggal Service</label>
                            <input type="date" id="input-tgl-service" class="form-control">
                        </div>
                        <div class="form-group" id="group-paket-service" style="display:none;">
                            <label class="form-label">Paket Service</label>
                            <select id="input-paket-service" class="form-control">
                                <option value="PM 1">PM 1 (5.000 KM)</option>
                                <option value="PM 2">PM 2 (10.000 KM)</option>
                                <option value="PM 3">PM 3 (20.000 KM)</option>
                                <option value="PM 4">PM 4 (40.000 KM)</option>
                                <option value="PM 5">PM 5 (80.000 KM)</option>
                                <option value="GENERAL REPAIR">GENERAL REPAIR</option>
                                <option value="BODY REPAIR">BODY REPAIR</option>
                            </select>
                        </div>
                    </div>

                    <!-- SECTION 4: FOTO STANDAR KENDARAAN (7 SUDUT) -->
                    <div class="form-section-title" style="margin-top: 24px; margin-bottom: 8px;">
                        <span>📸 4. Foto Standar Armada (7 Sudut)</span>
                    </div>
                    <div style="font-size:12px; color:var(--slate-500); margin-bottom:12px;">
                        Lampirkan foto kondisi standar kendaraan dari PC (opsional).
                    </div>
                    <div class="form-grid-4" id="std-photos-container">
                        <!-- Populated by JS -->
                    </div>

                    <!-- SECTION 5: PENILAIAN KPI & TEMUAN INSPEKSI -->
                    <div class="form-section-title" style="margin-top: 28px; margin-bottom: 8px;">
                        <span>🔍 5. Checklist Penilaian KPI & Temuan Inspeksi</span>
                    </div>
                    <div style="font-size:12px; color:var(--slate-500); margin-bottom:14px;">
                        Klik tombol skor untuk setiap item (0 = Standar/Baik, &gt;0 = Temuan). Bila terdapat temuan, masukkan catatan temuan dan lampirkan foto bukti temuan.
                    </div>

                    <div id="kpi-checklist-container">
                        <!-- Injected via JS from kpiSpec -->
                    </div>

                    <!-- STICKY SCORE BAR & ACTION BUTTONS -->
                    <div class="sticky-score-bar">
                        <div style="display:flex; align-items:center; gap:20px; flex-wrap:wrap;">
                            <div>
                                <div style="font-size:11px; text-transform:uppercase; letter-spacing:0.5px; opacity:0.8;">Total Poin Temuan</div>
                                <div style="font-size:24px; font-weight:800; color:#38bdf8;" id="sticky-total-poin">0 Poin</div>
                            </div>
                            <div>
                                <div style="font-size:11px; text-transform:uppercase; letter-spacing:0.5px; opacity:0.8;">Status Kelulusan</div>
                                <div id="sticky-status-badge" style="font-size:13px; font-weight:700; color:#34d399;">✅ STANDAR (LULUS)</div>
                            </div>
                            <div>
                                <div style="font-size:11px; text-transform:uppercase; letter-spacing:0.5px; opacity:0.8;">Jumlah Item Temuan</div>
                                <div style="font-size:15px; font-weight:700;" id="sticky-count-temuan">0 item</div>
                            </div>
                        </div>

                        <div style="display:flex; align-items:center; gap:10px;">
                            <button type="button" class="btn btn-secondary" onclick="resetAuditForm()" style="background:rgba(255,255,255,0.15); border:none; color:white;">
                                ↺ Reset
                            </button>
                            <button type="submit" class="btn btn-primary" id="btn-submit-bottom" style="background:#0284c7; padding:10px 24px; font-weight:700; font-size:14px; box-shadow:0 4px 12px rgba(2,132,199,0.5);">
                                💾 Simpan & Kirim Audit
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>

    </div>

    <!-- Modal Detail Audit & Galeri Foto -->
    <div class="modal-backdrop" id="detail-modal">
        <div class="modal-card">
            <div class="modal-header">
                <div class="modal-title" id="modal-title">Rincian Detail Audit</div>
                <button class="modal-close" onclick="closeModal()">&times;</button>
            </div>
            <div class="modal-body" id="modal-body">
                <!-- Injected via JS -->
            </div>
        </div>
    </div>

    <!-- Lightbox Zoom Modal -->
    <div class="zoom-modal" id="zoom-modal">
        <div class="zoom-toolbar">
            <div id="zoom-caption" style="font-size: 13px; font-weight: 600;">Foto Temuan</div>
            <div style="display: flex; align-items: center; gap: 8px;">
                <button class="btn btn-outline-white" onclick="applyZoom(0.25)">🔍+ Zoom In</button>
                <button class="btn btn-outline-white" onclick="applyZoom(-0.25)">🔍- Zoom Out</button>
                <button class="btn btn-outline-white" onclick="resetZoom()">↺ Reset</button>
                <button class="btn btn-primary" onclick="downloadCurrentZoomPhoto()">💾 Simpan Foto</button>
                <button class="btn btn-outline-white" onclick="closeZoomModal()">&times; Tutup</button>
            </div>
        </div>
        <div class="zoom-stage" id="zoom-stage">
            <img id="zoom-img" class="zoom-img" src="" alt="Foto Zoom">
        </div>
    </div>
        """.trimIndent()
    }

    private fun getScript(
        safeAudits: String,
        safeSpec: String,
        safeBobot: String,
        safePools: String,
        safeSamples: String,
        safeGrades: String,
        safePeriodes: String,
        safeVehicles: String = "null",
        safeUsers: String = "null"
    ): String {
        return """
        // Initial embedded data from server
        const EMBEDDED_AUDITS = $safeAudits;
        const EMBEDDED_SPEC = $safeSpec;
        const EMBEDDED_BOBOT = $safeBobot;
        const EMBEDDED_POOLS = $safePools;
        const EMBEDDED_SAMPLES = $safeSamples;
        const EMBEDDED_GRADES = $safeGrades;
        const EMBEDDED_PERIODES = $safePeriodes;
        const EMBEDDED_VEHICLES = $safeVehicles;
        const EMBEDDED_USERS = $safeUsers;

        let allAudits = Array.isArray(EMBEDDED_AUDITS) ? EMBEDDED_AUDITS : [];
        let filteredAudits = [];
        let kpiSpec = Array.isArray(EMBEDDED_SPEC) ? EMBEDDED_SPEC : [];
        let allBobots = Array.isArray(EMBEDDED_BOBOT) ? EMBEDDED_BOBOT : [];
        let masterPools = Array.isArray(EMBEDDED_POOLS) ? EMBEDDED_POOLS : [];
        let masterSamples = Array.isArray(EMBEDDED_SAMPLES) ? EMBEDDED_SAMPLES : [];
        let masterGrades = Array.isArray(EMBEDDED_GRADES) ? EMBEDDED_GRADES : [];
        let masterPeriodes = Array.isArray(EMBEDDED_PERIODES) ? EMBEDDED_PERIODES : [];
        let masterVehicles = Array.isArray(EMBEDDED_VEHICLES) ? EMBEDDED_VEHICLES : [];
        let masterUsers = Array.isArray(EMBEDDED_USERS) ? EMBEDDED_USERS : [];

        let bobotMap = {};
        let activeTab = 'riwayat';
        let checklistRendered = false;

        // Form state for Input Audit tab
        let formScores = {};
        let formNotes = {};
        let formPhotos = {};
        let stdPhotos = {};
        let stdPhotoNotes = {};

        // Initialize Bobot lookup map
        function updateBobotMap() {
            bobotMap = {};
            if (Array.isArray(allBobots)) {
                allBobots.forEach(b => {
                    if (b.itemId) {
                        bobotMap[b.itemId] = (typeof b.bobot === 'number') ? b.bobot : (parseFloat(b.bobot) || 1.0);
                    }
                });
            }
        }

        // Initialize application and real-time polling
        async function init() {
            updateBobotMap();
            if (allAudits.length > 0) {
                updateFiltersDropdown();
                applyFilters();
            }
            await fetchKpiSpec();
            initAuditForm();
            await fetchData();

            // Real-time polling every 3.5 seconds when connected to HTTP server
            if (window.location.protocol.startsWith('http')) {
                setInterval(fetchData, 3500);
            }
        }

        async function fetchKpiSpec() {
            if (!window.location.protocol.startsWith('http')) return;
            try {
                const res = await fetch('/api/spec');
                if (res.ok) {
                    kpiSpec = await res.json();
                }
            } catch (err) {
                console.warn('Gagal memuat KPI spec:', err);
            }
        }

        async function fetchData() {
            if (!window.location.protocol.startsWith('http')) {
                const now = new Date();
                const timeStr = now.toLocaleTimeString('id-ID');
                const updateEl = document.getElementById('last-updated-text');
                if (updateEl) updateEl.textContent = 'Mode Offline Standalone • ' + timeStr;
                return;
            }

            try {
                const [resAudits, resBobot, resPools, resSamples, resGrades, resPeriodes, resVehicles, resUsers] = await Promise.all([
                    fetch('/api/audits').catch(() => null),
                    fetch('/api/bobot').catch(() => null),
                    fetch('/api/pools').catch(() => null),
                    fetch('/api/samples').catch(() => null),
                    fetch('/api/grades').catch(() => null),
                    fetch('/api/periodes').catch(() => null),
                    fetch('/api/vehicles').catch(() => null),
                    fetch('/api/users').catch(() => null)
                ]);

                if (resAudits && resAudits.ok) {
                    const rawList = await resAudits.json();
                    const seen = new Set();
                    allAudits = rawList.filter(a => {
                        const idKey = String(a.kodeUnik || a.auditUuid || a.cloudId || a.id || '').trim();
                        if (idKey) {
                            if (seen.has(idKey)) return false;
                            seen.add(idKey);
                        }
                        return true;
                    });
                }

                if (resBobot && resBobot.ok) {
                    allBobots = await resBobot.json();
                    updateBobotMap();
                }

                if (resPools && resPools.ok) {
                    masterPools = await resPools.json();
                }

                if (resSamples && resSamples.ok) {
                    masterSamples = await resSamples.json();
                }

                if (resGrades && resGrades.ok) {
                    masterGrades = await resGrades.json();
                }

                if (resPeriodes && resPeriodes.ok) {
                    masterPeriodes = await resPeriodes.json();
                }

                if (resVehicles && resVehicles.ok) {
                    masterVehicles = await resVehicles.json();
                }

                if (resUsers && resUsers.ok) {
                    masterUsers = await resUsers.json();
                }

                updateFiltersDropdown();
                populateFormMasterData();
                applyFilters();

                const now = new Date();
                const timeStr = now.toLocaleTimeString('id-ID');
                const updateEl = document.getElementById('last-updated-text');
                if (updateEl) {
                    updateEl.innerHTML = '<span style="color: #10b981; font-weight: 800;">● Realtime Online</span> • ' + timeStr;
                }
            } catch (err) {
                console.warn('Sinkronisasi data tertunda:', err);
            }
        }

        function updateFiltersDropdown() {
            // Periode Dropdown
            const periodeSelect = document.getElementById('filter-periode');
            const currentPeriode = periodeSelect.value;
            const periodesFromAudits = allAudits.map(a => a.periode).filter(Boolean);
            const periodesFromMaster = masterPeriodes.map(p => p.periode).filter(Boolean);
            const periodes = Array.from(new Set([...periodesFromAudits, ...periodesFromMaster])).sort();

            periodeSelect.innerHTML = '<option value="ALL">Semua Periode</option>';
            periodes.forEach(p => {
                const opt = document.createElement('option');
                opt.value = p;
                opt.textContent = p;
                if (p === currentPeriode) opt.selected = true;
                periodeSelect.appendChild(opt);
            });

            // Pool Dropdown
            const poolSelect = document.getElementById('filter-pool');
            const currentPool = poolSelect.value;
            const poolsFromAudits = allAudits.map(a => a.pool).filter(Boolean);
            const poolsFromMaster = masterPools.map(p => p.kodePool).filter(Boolean);
            const pools = Array.from(new Set([...poolsFromAudits, ...poolsFromMaster])).sort();

            poolSelect.innerHTML = '<option value="ALL">Semua Pool</option>';
            pools.forEach(p => {
                const opt = document.createElement('option');
                opt.value = p;
                opt.textContent = p;
                if (p === currentPool) opt.selected = true;
                poolSelect.appendChild(opt);
            });
        }

        // Tab Navigation
        function switchTab(tabId) {
            activeTab = tabId;
            document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
            document.querySelectorAll('.view-content').forEach(view => view.classList.remove('active'));

            const btn = document.getElementById('tab-btn-' + tabId);
            const view = document.getElementById('view-' + tabId);
            if (btn) btn.classList.add('active');
            if (view) view.classList.add('active');

            renderActiveTab();
        }

        function renderActiveTab() {
            if (activeTab === 'riwayat') renderRiwayatTable();
            else if (activeTab === 'resume') renderResumeMatrixTable();
            else if (activeTab === 'bobot') renderBobotTable();
            else if (activeTab === 'rekap-nilai') renderRekapNilaiTable();
            else if (activeTab === 'rekap-temuan') renderRekapTemuanTable();
            else if (activeTab === 'input') {
                populateFormMasterData();
                if (!checklistRendered && kpiSpec.length > 0) {
                    renderKpiChecklistForm();
                }
            }
        }

        // Filter Application
        function applyFilters() {
            const periode = document.getElementById('filter-periode').value;
            const pool = document.getElementById('filter-pool').value;
            const sample = document.getElementById('filter-sample').value;
            const status = document.getElementById('filter-status').value;
            const search = (document.getElementById('filter-search').value || '').trim().toLowerCase();

            // First level: Filter by Periode & Pool
            let base = allAudits.filter(a => {
                if (periode !== 'ALL' && a.periode !== periode) return false;
                if (pool !== 'ALL' && a.pool !== pool) return false;
                return true;
            });

            // Deduplicate & keep latest per car and period (Matching Android business logic)
            base = deduplicateAndKeepLatest(base);

            // Second level: Filter by Sample, Status, Search
            filteredAudits = base.filter(a => {
                if (sample !== 'ALL' && (a.jenisSample || '').toUpperCase() !== sample) return false;
                
                const isStandar = (a.totalPoinTemuan === 0);
                if (status === 'STANDAR' && !isStandar) return false;
                if (status === 'TEMUAN' && isStandar) return false;

                if (search) {
                    const noMobil = (a.noMobil || '').toLowerCase();
                    const plat = (a.platNomer || '').toLowerCase();
                    const auditor = (a.namaPetugas || '').toLowerCase();
                    const poolCode = (a.pool || '').toLowerCase();
                    const matched = noMobil.includes(search) || plat.includes(search) || auditor.includes(search) || poolCode.includes(search);
                    if (!matched) return false;
                }

                return true;
            });

            updateStatCards(base);
            renderActiveTab();
        }

        function deduplicateAndKeepLatest(audits) {
            // Group by noMobil + periode, sort by timestamp desc, take latest
            const map = new Map();
            audits.forEach(a => {
                const key = ((a.noMobil || '') + '_' + (a.periode || '')).toUpperCase();
                const existing = map.get(key);
                if (!existing || (a.timestamp || 0) > (existing.timestamp || 0)) {
                    map.set(key, a);
                }
            });
            return Array.from(map.values()).sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
        }

        function parseAuditScores(audit) {
            if (!audit.kpiScoresJson) return {};
            try {
                return JSON.parse(audit.kpiScoresJson);
            } catch (e) {
                return {};
            }
        }

        // Top KPI Stat Cards Update
        function updateStatCards(baseAudits) {
            const total = filteredAudits.length;
            const standar = filteredAudits.filter(a => a.totalPoinTemuan === 0).length;
            const temuan = total - standar;
            const totalPoin = filteredAudits.reduce((acc, a) => acc + (a.totalPoinTemuan || 0), 0);

            const opCount = filteredAudits.filter(a => (a.jenisSample || '').toUpperCase() === 'OPERASIONAL').length;
            const svcCount = total - opCount;

            document.getElementById('stat-total-armada').textContent = total;
            document.getElementById('stat-sub-armada').textContent = opCount + ' Operasional • ' + svcCount + ' Service';

            document.getElementById('stat-standar').textContent = standar;
            const standarPct = total > 0 ? ((standar / total) * 100).toFixed(1) : '0';
            document.getElementById('stat-sub-standar').textContent = standarPct + '% Armada Kondisi Prima';

            document.getElementById('stat-temuan').textContent = temuan;
            const temuanPct = total > 0 ? ((temuan / total) * 100).toFixed(1) : '0';
            document.getElementById('stat-sub-temuan').textContent = temuanPct + '% Perlu Perbaikan';

            document.getElementById('stat-total-poin').textContent = totalPoin;

            // Badges
            document.getElementById('badge-count-riwayat').textContent = total;
            document.getElementById('badge-count-resume').textContent = Math.min(total, 14);
            document.getElementById('riwayat-total-badge').textContent = total + ' Data';
            document.getElementById('resume-units-count').textContent = Math.min(total, 14) + ' / 14 Unit';

            // Calculate live KPI & Grade
            const kpiResult = computeKpiGrade(baseAudits);
            document.getElementById('stat-grade-kpi').textContent = kpiResult.kpiCucian.toFixed(2);
            document.getElementById('stat-nilai-kpi').textContent = 'Nilai KPI: ' + kpiResult.nilaiKpi.toFixed(2);
        }

        // KPI & Grade Calculation
        function computeKpiGrade(baseAudits) {
            if (!kpiSpec || kpiSpec.length === 0) {
                return { nilaiKpi: 0, gradeLetter: '-', gradeDesc: '-', extPct: 0, intPct: 0, cucPct: 0, kpiCucian: 0, targetSample: 10 };
            }

            const sampleCount = baseAudits.length;
            const currentPool = document.getElementById('filter-pool').value;
            let targetSample = 10;
            if (currentPool !== 'ALL') {
                const poolObj = masterPools.find(p => (p.kodePool || '').toUpperCase() === currentPool.toUpperCase());
                const area = poolObj ? (poolObj.statusArea || 'JABODETABEK') : 'JABODETABEK';
                const sObj = masterSamples.find(s => (s.statusArea || '').toUpperCase() === area.toUpperCase());
                targetSample = sObj ? sObj.jumlahSample : 10;
            } else {
                targetSample = 10;
            }

            const parsedScores = baseAudits.map(a => parseAuditScores(a));

            let extGroupPcts = [];
            let intGroupPcts = [];
            let cucGroupPcts = [];

            kpiSpec.forEach(sec => {
                const secTitle = (sec.title || '').toUpperCase();
                (sec.groups || []).forEach(grp => {
                    let totalBobotGroupTemuan = 0;
                    (grp.items || []).forEach(item => {
                        const b = bobotMap[item.id] || item.bobot || 1.0;
                        const pMax = item.maxPoints || 2;
                        const totalMax = pMax * sampleCount;
                        const earned = parsedScores.reduce((sum, sc) => sum + (sc[item.id] || 0), 0);
                        const finding = Math.max(0, totalMax - earned);
                        const bobotFinding = finding * b;
                        totalBobotGroupTemuan += bobotFinding;
                    });

                    const groupPct = targetSample > 0 ? (totalBobotGroupTemuan / targetSample) : 0;
                    if (secTitle.includes('EXTERIOR')) extGroupPcts.push(groupPct);
                    else if (secTitle.includes('INTERIOR')) intGroupPcts.push(groupPct);
                    else if (secTitle.includes('CUCIAN')) cucGroupPcts.push(groupPct);
                });
            });

            const sumExt = extGroupPcts.reduce((a, b) => a + b, 0);
            const sumInt = intGroupPcts.reduce((a, b) => a + b, 0);
            const sumCuc = cucGroupPcts.reduce((a, b) => a + b, 0);

            const extPct = extGroupPcts.length > 0 ? (sumExt / 5.0) : 0;
            const intPct = intGroupPcts.length > 0 ? (sumInt / 5.0) : 0;
            const cucPct = cucGroupPcts.length > 0 ? (sumCuc / 3.0) : 0;

            let nilaiKpi = ((extPct + intPct) / 2.0) - 20.0;
            if (nilaiKpi < 0) nilaiKpi = 0;
            if (nilaiKpi > 80) nilaiKpi = 80;

            let kpiCucian = cucPct - 20.0;
            if (kpiCucian < 0) kpiCucian = 0;
            if (kpiCucian > 80) kpiCucian = 80;

            let gradeLetter = 'E';
            let gradeDesc = 'Perlu Perbaikan / Kritis';

            if (masterGrades && masterGrades.length > 0) {
                const rounded = Math.round(nilaiKpi * 100) / 100;
                const match = masterGrades.find(g => rounded >= (g.minScore - 0.01) && rounded <= (g.maxScore + 0.01));
                if (match) {
                    gradeLetter = match.kategori;
                    gradeDesc = match.keterangan;
                } else if (rounded >= 70) {
                    gradeLetter = 'A';
                    gradeDesc = 'Sangat Baik (Grade A)';
                } else if (rounded <= 29.99) {
                    gradeLetter = 'E';
                    gradeDesc = 'Kritis / Perlu Perbaikan (Grade E)';
                }
            } else {
                if (nilaiKpi >= 70) { gradeLetter = 'A'; gradeDesc = 'Sangat Baik (Grade A)'; }
                else if (nilaiKpi >= 60) { gradeLetter = 'B'; gradeDesc = 'Baik (Grade B)'; }
                else if (nilaiKpi >= 50) { gradeLetter = 'C'; gradeDesc = 'Cukup (Grade C)'; }
                else if (nilaiKpi >= 30) { gradeLetter = 'D'; gradeDesc = 'Kurang (Grade D)'; }
                else { gradeLetter = 'E'; gradeDesc = 'Kritis / Perlu Perbaikan (Grade E)'; }
            }

            return {
                nilaiKpi: nilaiKpi,
                gradeLetter: gradeLetter,
                gradeDesc: gradeDesc,
                extPct: extPct,
                intPct: intPct,
                cucPct: cucPct,
                kpiCucian: kpiCucian,
                targetSample: targetSample
            };
        }

        // TAB 1: RENDER RIWAYAT AUDIT
        function renderRiwayatTable() {
            const tbody = document.getElementById('riwayat-table-body');
            if (!tbody) return;

            if (filteredAudits.length === 0) {
                tbody.innerHTML = '<tr><td colspan="13" style="text-align: center; padding: 40px; color: var(--slate-400);">Tidak ada data audit yang sesuai dengan filter.</td></tr>';
                return;
            }

            let html = '';
            filteredAudits.forEach((a, index) => {
                const dateStr = a.tanggal || (a.timestamp ? new Date(a.timestamp).toLocaleDateString('id-ID') : '-');
                const isStandar = (a.totalPoinTemuan === 0);
                const statusBadge = isStandar
                    ? '<span class="badge badge-standar">STANDAR</span>'
                    : '<span class="badge badge-temuan">TEMUAN</span>';

                const sampleType = (a.jenisSample || 'OPERASIONAL').toUpperCase();
                const sampleBadge = sampleType === 'SERVICE'
                    ? '<span class="badge badge-svc">SERVICE</span>'
                    : '<span class="badge badge-op">OPERASIONAL</span>';

                const poinClass = isStandar ? 'cell-zero' : 'badge badge-temuan';
                const photosCount = getAuditPhotos(a).length;
                const photoBadge = photosCount > 0
                    ? '<button class="btn btn-outline-white" style="background: var(--primary-light); color: var(--primary-dark); padding: 3px 8px; font-size: 11px;" onclick="viewAuditDetail(' + index + ')">📷 ' + photosCount + ' Foto</button>'
                    : '<span style="color: var(--slate-400); font-size: 11px;">-</span>';

                html += '<tr>' +
                    '<td style="text-align: center; font-weight: 700; color: var(--slate-500);">' + (index + 1) + '</td>' +
                    '<td style="white-space: nowrap;">' + dateStr + '</td>' +
                    '<td><span class="badge badge-gray">' + (a.periode || '-') + '</span></td>' +
                    '<td><strong>' + (a.pool || '-') + '</strong></td>' +
                    '<td><strong style="color: var(--primary); font-size: 13px;">' + (a.noMobil || '-') + '</strong></td>' +
                    '<td>' + (a.platNomer || '-') + '</td>' +
                    '<td>' + (a.namaPetugas || '-') + '</td>' +
                    '<td>' + sampleBadge + '</td>' +
                    '<td>' + (a.paketService || '-') + '</td>' +
                    '<td style="text-align: center;"><span class="' + poinClass + '">' + (a.totalPoinTemuan || 0) + '</span></td>' +
                    '<td style="text-align: center;">' + statusBadge + '</td>' +
                    '<td style="text-align: center;">' + photoBadge + '</td>' +
                    '<td style="text-align: center;">' +
                        '<button class="btn btn-primary" style="padding: 4px 8px; font-size: 11px;" onclick="viewAuditDetail(' + index + ')">🔍 Detail</button>' +
                    '</td>' +
                '</tr>';
            });

            tbody.innerHTML = html;
        }

        // TAB 2: RENDER RESUME MATRIX TABLE (MAKS 14 MOBIL)
        function renderResumeMatrixTable() {
            const table = document.getElementById('resume-matrix-table');
            if (!table) return;

            const matrixAudits = filteredAudits.slice(0, 14);
            if (matrixAudits.length === 0) {
                table.innerHTML = '<tr><td style="padding: 40px; text-align: center; color: var(--slate-400);">Tidak ada data unit mobil untuk ditampilkan pada matriks resume.</td></tr>';
                return;
            }

            let html = '';

            // Header Row 1: Unit Columns Header
            html += '<tr class="header-row">';
            html += '<th class="sticky-col" style="vertical-align: bottom;">DATA CEK ALL / CHECKLIST</th>';
            matrixAudits.forEach((a, idx) => {
                html += '<th style="min-width: 110px;">';
                html += '<div style="font-size: 10px; color: var(--slate-500); font-weight: 700;">UNIT #' + (idx + 1) + '</div>';
                html += '<div style="font-size: 14px; font-weight: 800; color: var(--primary); margin: 2px 0;">' + (a.noMobil || '-') + '</div>';
                html += '<div style="font-size: 11px; color: var(--slate-600);">' + (a.platNomer || '-') + '</div>';
                html += '<div style="font-size: 10px; color: var(--slate-500); margin-top: 2px;">' + (a.pool || '-') + ' • ' + (a.jenisSample || 'OP') + '</div>';
                html += '<button class="btn btn-outline-white" style="margin-top: 4px; padding: 2px 6px; font-size: 10px; background: var(--slate-100); color: var(--slate-700);" onclick="viewAuditDetailByMobil(\'' + a.noMobil + '\')">📷 Foto</button>';
                html += '</th>';
            });
            html += '</tr>';

            // Row 2: Total Poin Temuan
            html += '<tr style="background: #ffffff; font-weight: 800;">';
            html += '<td class="sticky-col">TOTAL POIN TEMUAN</td>';
            matrixAudits.forEach(a => {
                const pt = a.totalPoinTemuan || 0;
                html += '<td><span class="' + (pt === 0 ? 'cell-zero' : 'badge badge-temuan') + '">' + pt + '</span></td>';
            });
            html += '</tr>';

            // Parsed scores per audit
            const matrixScores = matrixAudits.map(a => parseAuditScores(a));

            // Section -> Group -> Items rows
            kpiSpec.forEach(sec => {
                // Section Title Row
                html += '<tr class="section-row">';
                html += '<td class="sticky-col">' + (sec.title || '') + '</td>';
                matrixAudits.forEach(() => { html += '<td></td>'; });
                html += '</tr>';

                (sec.groups || []).forEach(grp => {
                    // Group Title Row
                    html += '<tr class="group-row">';
                    html += '<td class="sticky-col">• ' + (grp.title || '') + '</td>';
                    matrixAudits.forEach(() => { html += '<td></td>'; });
                    html += '</tr>';

                    (grp.items || []).forEach(item => {
                        html += '<tr>';
                        html += '<td class="sticky-col" style="padding-left: 20px;">' + (item.title || item.name || '') + ' <span style="font-size: 10px; color: var(--slate-400);">(' + (item.maxPoints || 2) + ' Poin)</span></td>';

                        matrixAudits.forEach((a, aIdx) => {
                            const sc = matrixScores[aIdx][item.id] !== undefined ? matrixScores[aIdx][item.id] : 0;
                            if (sc === 0) {
                                html += '<td><span class="cell-zero">0</span></td>';
                            } else {
                                html += '<td class="cell-finding">' + sc + '</td>';
                            }
                        });
                        html += '</tr>';
                    });
                });
            });

            table.innerHTML = html;
        }

        // TAB 3: RENDER BOBOT RESUME TABLE
        function renderBobotTable() {
            const tbody = document.getElementById('bobot-table-body');
            if (!tbody) return;

            const allItems = getAllSpecItems();
            const sampleCount = filteredAudits.length;
            const parsedScores = filteredAudits.map(a => parseAuditScores(a));

            let html = '';
            let totalAccumulatedTemuan = 0;
            let totalAccumulatedNilai = 0;

            allItems.forEach((item, index) => {
                const b = bobotMap[item.id] || item.bobot || 1.0;
                const pMax = item.maxPoints || 2;
                const totalMax = pMax * sampleCount;
                const earned = parsedScores.reduce((sum, sc) => sum + (sc[item.id] || 0), 0);
                const finding = Math.max(0, totalMax - earned);
                const nilaiTemuan = finding * b;

                totalAccumulatedTemuan += finding;
                totalAccumulatedNilai += nilaiTemuan;

                const hasFinding = finding > 0;
                const rowStyle = hasFinding ? 'background-color: #fffbeb;' : '';

                html += '<tr style="' + rowStyle + '">' +
                    '<td style="text-align: center; font-weight: 700; color: var(--slate-500);">' + (index + 1) + '</td>' +
                    '<td><strong>' + (item.title || item.name || '') + '</strong></td>' +
                    '<td><span class="badge badge-gray">' + (item.sectionTitle || '') + ' • ' + (item.groupTitle || '') + '</span></td>' +
                    '<td style="text-align: center;"><span class="badge badge-primary">' + b.toFixed(1) + '</span></td>' +
                    '<td style="text-align: center;"><span class="' + (hasFinding ? 'badge badge-temuan' : 'cell-zero') + '">' + finding + '</span></td>' +
                    '<td style="text-align: center;">' + pMax + '</td>' +
                    '<td style="text-align: center; font-weight: 800; color: ' + (hasFinding ? 'var(--danger-dark)' : 'var(--slate-700)') + ';">' + nilaiTemuan.toFixed(1) + '</td>' +
                    '<td style="text-align: center;">' + sampleCount + '</td>' +
                '</tr>';
            });

            document.getElementById('bobot-summary-badge').textContent = allItems.length + ' Item • ' + totalAccumulatedTemuan + ' Poin Temuan';
            tbody.innerHTML = html;
        }

        // TAB 4: RENDER REKAP NILAI TABLE
        function renderRekapNilaiTable() {
            const tbody = document.getElementById('rekap-nilai-table-body');
            if (!tbody) return;

            const kpiResult = computeKpiGrade(filteredAudits);

            // Update Showcase UI
            document.getElementById('showcase-grade-letter').textContent = kpiResult.kpiCucian.toFixed(2);
            document.getElementById('showcase-score-badge').textContent = 'Nilai KPI: ' + kpiResult.nilaiKpi.toFixed(2);
            document.getElementById('showcase-grade-desc').textContent = 'KPI CUCIAN: ' + kpiResult.kpiCucian.toFixed(2) + ' (Cucian - 20)';

            document.getElementById('kpi-exterior-pct').textContent = kpiResult.extPct.toFixed(2);
            document.getElementById('kpi-interior-pct').textContent = kpiResult.intPct.toFixed(2);
            document.getElementById('kpi-cucian-pct').textContent = kpiResult.cucPct.toFixed(2);
            document.getElementById('kpi-sample-info').textContent = filteredAudits.length + ' / ' + kpiResult.targetSample + ' Unit';

            // Sub Group Table
            let html = '';
            let groupIndex = 1;
            const sampleCount = filteredAudits.length;
            const parsedScores = filteredAudits.map(a => parseAuditScores(a));

            kpiSpec.forEach(sec => {
                (sec.groups || []).forEach(grp => {
                    let totalBobotGroup = 0;
                    (grp.items || []).forEach(item => {
                        const b = bobotMap[item.id] || item.bobot || 1.0;
                        const pMax = item.maxPoints || 2;
                        const totalMax = pMax * sampleCount;
                        const earned = parsedScores.reduce((sum, sc) => sum + (sc[item.id] || 0), 0);
                        const finding = Math.max(0, totalMax - earned);
                        totalBobotGroup += (finding * b);
                    });

                    const groupPct = kpiResult.targetSample > 0 ? (totalBobotGroup / kpiResult.targetSample) : 0;

                    html += '<tr>' +
                        '<td style="text-align: center; font-weight: 700; color: var(--slate-500);">' + (groupIndex++) + '</td>' +
                        '<td>' +
                            '<strong>' + (grp.title || '') + '</strong>' +
                            '<div style="font-size: 11px; color: var(--slate-500);">' + (sec.title || '') + ' (' + (grp.items || []).length + ' Item Checklist)</div>' +
                        '</td>' +
                        '<td style="text-align: center; font-weight: 700;">' + totalBobotGroup.toFixed(1) + '</td>' +
                        '<td style="text-align: center; font-weight: 800; color: var(--primary-dark);">' + groupPct.toFixed(2) + '</td>' +
                        '<td style="text-align: center;"><span class="badge badge-standar">TERVERIFIKASI</span></td>' +
                    '</tr>';
                });
            });

            tbody.innerHTML = html;
        }

        // TAB 5: RENDER REKAP TEMUAN TABLE (OPERASIONAL VS SERVICE)
        function renderRekapTemuanTable() {
            const table = document.getElementById('rekap-temuan-table');
            if (!table) return;

            const allItems = getAllSpecItems();
            const currentPool = document.getElementById('filter-pool').value;

            // Determine displayed pools
            let displayedPools = [];
            if (currentPool !== 'ALL') {
                displayedPools = [currentPool];
            } else {
                const poolsFromAudits = Array.from(new Set(filteredAudits.map(a => a.pool).filter(Boolean))).sort();
                displayedPools = poolsFromAudits.length > 0 ? poolsFromAudits : ['ALL'];
            }

            // Prepare pool audit stats
            const poolData = {};
            displayedPools.forEach(p => {
                const pAudits = p === 'ALL' ? filteredAudits : filteredAudits.filter(a => a.pool === p);
                const opAudits = pAudits.filter(a => (a.jenisSample || '').toUpperCase() === 'OPERASIONAL');
                const svcAudits = pAudits.filter(a => (a.jenisSample || '').toUpperCase() === 'SERVICE');

                poolData[p] = {
                    opAudits: opAudits,
                    svcAudits: svcAudits,
                    opScores: opAudits.map(a => parseAuditScores(a)),
                    svcScores: svcAudits.map(a => parseAuditScores(a)),
                    totalSample: opAudits.length + svcAudits.length
                };
            });

            let criticalCount = 0;
            let html = '';

            // Header Row 1: Main Columns
            html += '<tr class="header-row">';
            html += '<th class="sticky-col" rowspan="2" style="vertical-align: middle;">DATA INPUT (TITIK PEMERIKSAAN)</th>';
            displayedPools.forEach(p => {
                html += '<th colspan="3" style="text-align: center; background: #e2e8f0; border-bottom: 1px solid var(--slate-300); font-weight: 800;">POOL: ' + p + '</th>';
            });
            html += '</tr>';

            // Header Row 2: Sub-columns (Operasional, Service, Total)
            html += '<tr class="header-row">';
            displayedPools.forEach(p => {
                const tot = poolData[p].totalSample;
                const opTot = poolData[p].opAudits.length;
                const svcTot = poolData[p].svcAudits.length;
                html += '<th style="min-width: 90px; font-size: 11px;">OPERASIONAL<br><span style="font-size: 10px; font-weight: 500; color: var(--slate-500);">(' + opTot + ' Smp)</span></th>';
                html += '<th style="min-width: 90px; font-size: 11px;">SERVICE<br><span style="font-size: 10px; font-weight: 500; color: var(--slate-500);">(' + svcTot + ' Smp)</span></th>';
                html += '<th style="min-width: 90px; font-size: 11px; background: #f1f5f9;">TOTAL %<br><span style="font-size: 10px; font-weight: 500; color: var(--slate-500);">(' + tot + ' Smp)</span></th>';
            });
            html += '</tr>';

            // Rows per checklist item grouped by Section
            kpiSpec.forEach(sec => {
                html += '<tr class="section-row">';
                html += '<td class="sticky-col">' + (sec.title || '') + '</td>';
                displayedPools.forEach(() => { html += '<td colspan="3"></td>'; });
                html += '</tr>';

                (sec.groups || []).forEach(grp => {
                    html += '<tr class="group-row">';
                    html += '<td class="sticky-col">• ' + (grp.title || '') + '</td>';
                    displayedPools.forEach(() => { html += '<td colspan="3"></td>'; });
                    html += '</tr>';

                    (grp.items || []).forEach(item => {
                        html += '<tr>';
                        html += '<td class="sticky-col" style="padding-left: 20px;">' + (item.title || item.name || '') + '</td>';

                        displayedPools.forEach(p => {
                            const pInfo = poolData[p];
                            const totSample = pInfo.totalSample;
                            const opSample = pInfo.opAudits.length;
                            const svcSample = pInfo.svcAudits.length;

                            const opFindingCars = pInfo.opScores.filter(sc => (sc[item.id] || 0) > 0).length;
                            const svcFindingCars = pInfo.svcScores.filter(sc => (sc[item.id] || 0) > 0).length;
                            const totalFindingCars = opFindingCars + svcFindingCars;

                            const opPct = opSample > 0 ? ((opFindingCars / opSample) * 100) : 0;
                            const svcPct = svcSample > 0 ? ((svcFindingCars / svcSample) * 100) : 0;
                            const totPct = totSample > 0 ? ((totalFindingCars / totSample) * 100) : 0;

                            const isOpCritical = opPct > 50;
                            const isSvcCritical = svcPct > 50;
                            const isTotCritical = totPct > 50;

                            if (isOpCritical || isSvcCritical || isTotCritical) {
                                criticalCount++;
                            }

                            const opCellClass = isOpCritical ? 'cell-danger-50' : (opFindingCars > 0 ? '' : 'cell-zero');
                            const svcCellClass = isSvcCritical ? 'cell-danger-50' : (svcFindingCars > 0 ? '' : 'cell-zero');
                            const totCellClass = isTotCritical ? 'cell-danger-50' : (totalFindingCars > 0 ? '' : 'cell-zero');

                            html += '<td class="' + opCellClass + '" style="text-align: center;">' + opFindingCars + ' (' + opPct.toFixed(1) + '%)</td>';
                            html += '<td class="' + svcCellClass + '" style="text-align: center;">' + svcFindingCars + ' (' + svcPct.toFixed(1) + '%)</td>';
                            html += '<td class="' + totCellClass + '" style="text-align: center; font-weight: 700;">' + totalFindingCars + ' (' + totPct.toFixed(1) + '%)</td>';
                        });

                        html += '</tr>';
                    });
                });
            });

            document.getElementById('rekap-temuan-critical-badge').textContent = criticalCount + ' Titik Kritis (>50%)';
            table.innerHTML = html;
        }

        // Helper: Extract all checklist items in linear list
        function getAllSpecItems() {
            const items = [];
            kpiSpec.forEach(sec => {
                (sec.groups || []).forEach(grp => {
                    (grp.items || []).forEach(item => {
                        items.push({
                            id: item.id,
                            title: item.title || item.name || '',
                            name: item.title || item.name || '',
                            maxPoints: item.maxPoints || 2,
                            bobot: item.bobot || 1.0,
                            sectionTitle: sec.title || '',
                            groupTitle: grp.title || ''
                        });
                    });
                });
            });
            return items;
        }

        // Helper: Get photo list for an audit
        function getAuditPhotos(audit) {
            const photos = [];
            if (audit.photos && typeof audit.photos === 'object') {
                Object.keys(audit.photos).forEach(key => {
                    const photoObj = audit.photos[key];
                    if (photoObj) {
                        const url = photoObj.base64 ? photoObj.base64 : ('/api/photo?auditId=' + (audit.kodeUnik || audit.id) + '&key=' + encodeURIComponent(key));
                        photos.push({
                            key: key,
                            label: photoObj.label || key,
                            keterangan: photoObj.keterangan || '',
                            url: url
                        });
                    }
                });
            }
            return photos;
        }

        // Audit Detail & Photo Modal
        function viewAuditDetail(index) {
            const audit = filteredAudits[index];
            if (!audit) return;
            showAuditModal(audit);
        }

        function viewAuditDetailByMobil(noMobil) {
            const audit = filteredAudits.find(a => (a.noMobil || '').toUpperCase() === (noMobil || '').toUpperCase());
            if (!audit) return;
            showAuditModal(audit);
        }

        function showAuditModal(audit) {
            document.getElementById('modal-title').innerHTML = 'Rincian Audit Armada: <strong>' + (audit.noMobil || '-') + '</strong> (' + (audit.platNomer || '-') + ')';

            const photos = getAuditPhotos(audit);
            const isStandar = (audit.totalPoinTemuan === 0);
            const scores = parseAuditScores(audit);

            let findingsHtml = '';
            const allItems = getAllSpecItems();
            allItems.forEach(it => {
                const sc = scores[it.id] || 0;
                if (sc > 0) {
                    findingsHtml += '<div style="background: #fee2e2; border-left: 3px solid #dc2626; padding: 6px 12px; border-radius: 4px; font-size: 12px; margin-bottom: 6px; display: flex; justify-content: space-between;">' +
                        '<span><strong>' + it.title + '</strong> <span style="font-size: 11px; color: #475569;">(' + it.sectionTitle + ' • ' + it.groupTitle + ')</span></span>' +
                        '<span class="badge badge-temuan">' + sc + ' Poin</span>' +
                    '</div>';
                }
            });

            if (!findingsHtml) {
                findingsHtml = '<div style="background: #ecfdf5; border-left: 3px solid #10b981; padding: 8px 12px; border-radius: 4px; font-size: 12px; color: #065f46; font-weight: 600;">' +
                    '✅ Kondisi Sempurna: Seluruh item checklist memenuhi standar keindahan (0 Poin Temuan).' +
                '</div>';
            }

            let photosHtml = '';
            if (photos.length > 0) {
                photosHtml = '<div class="photo-gallery-grid">';
                photos.forEach(p => {
                    photosHtml += '<div class="photo-thumb-card" onclick="openZoomModal(\'' + p.url + '\', \'' + (p.label || '') + '\')">' +
                        '<img src="' + p.url + '" class="photo-thumb-img" alt="' + p.label + '" loading="lazy">' +
                        '<div class="photo-thumb-info">' +
                            '<div><strong>' + (p.label || 'Foto') + '</strong></div>' +
                            (p.keterangan ? '<div style="font-size: 10.5px; color: var(--slate-500);">' + p.keterangan + '</div>' : '') +
                        '</div>' +
                    '</div>';
                });
                photosHtml += '</div>';
            } else {
                photosHtml = '<div style="color: var(--slate-400); font-size: 12px;">Tidak ada dokumentasi foto yang diunggah untuk unit ini.</div>';
            }

            const bodyHtml = 
                '<div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; background: #f8fafc; padding: 14px; border-radius: 10px; border: 1px solid var(--slate-200);">' +
                    '<div><span style="font-size: 11px; color: var(--slate-500);">Pool</span><div><strong>' + (audit.pool || '-') + '</strong></div></div>' +
                    '<div><span style="font-size: 11px; color: var(--slate-500);">Periode</span><div><strong>' + (audit.periode || '-') + '</strong></div></div>' +
                    '<div><span style="font-size: 11px; color: var(--slate-500);">Petugas / Auditor</span><div><strong>' + (audit.namaPetugas || '-') + '</strong></div></div>' +
                    '<div><span style="font-size: 11px; color: var(--slate-500);">Sample</span><div><strong>' + (audit.jenisSample || 'OPERASIONAL') + '</strong></div></div>' +
                    '<div><span style="font-size: 11px; color: var(--slate-500);">Paket Service</span><div><strong>' + (audit.paketService || '-') + '</strong></div></div>' +
                    '<div><span style="font-size: 11px; color: var(--slate-500);">Status Kelayakan</span><div>' + (isStandar ? '<span class="badge badge-standar">STANDAR</span>' : '<span class="badge badge-temuan">TEMUAN (' + audit.totalPoinTemuan + ' Poin)</span>') + '</div></div>' +
                '</div>' +

                '<div>' +
                    '<h4 style="font-size: 13px; font-weight: 800; color: var(--slate-800); margin-bottom: 8px;">Daftar Temuan Item:</h4>' +
                    findingsHtml +
                '</div>' +

                '<div>' +
                    '<h4 style="font-size: 13px; font-weight: 800; color: var(--slate-800); margin-bottom: 8px;">Dokumentasi Foto Temuan (' + photos.length + '):</h4>' +
                    photosHtml +
                '</div>';

            document.getElementById('modal-body').innerHTML = bodyHtml;
            document.getElementById('detail-modal').classList.add('active');
        }

        function closeModal() {
            document.getElementById('detail-modal').classList.remove('active');
        }

        // Lightbox Zoom & Pan
        let currentZoomLevel = 1.0;
        let zoomTranslateX = 0;
        let zoomTranslateY = 0;
        let isDraggingZoom = false;
        let dragStartX = 0;
        let dragStartY = 0;
        let currentZoomUrl = '';

        function openZoomModal(url, caption) {
            currentZoomUrl = url;
            const modal = document.getElementById('zoom-modal');
            const img = document.getElementById('zoom-img');
            document.getElementById('zoom-caption').textContent = caption || 'Foto Temuan';
            img.src = url;
            resetZoom();
            modal.classList.add('active');
        }

        function closeZoomModal() {
            document.getElementById('zoom-modal').classList.remove('active');
        }

        function applyZoom(delta) {
            currentZoomLevel = Math.max(0.5, Math.min(4.0, currentZoomLevel + delta));
            updateZoomTransform();
        }

        function resetZoom() {
            currentZoomLevel = 1.0;
            zoomTranslateX = 0;
            zoomTranslateY = 0;
            updateZoomTransform();
        }

        function updateZoomTransform() {
            const img = document.getElementById('zoom-img');
            img.style.transform = 'translate(' + zoomTranslateX + 'px, ' + zoomTranslateY + 'px) scale(' + currentZoomLevel + ')';
        }

        function downloadCurrentZoomPhoto() {
            if (!currentZoomUrl) return;
            const a = document.createElement('a');
            a.href = currentZoomUrl;
            a.download = 'Foto_Temuan_Audit.jpg';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
        }

        function setupZoomInteractions() {
            const stage = document.getElementById('zoom-stage');
            if (!stage) return;

            stage.addEventListener('wheel', (e) => {
                e.preventDefault();
                applyZoom(e.deltaY < 0 ? 0.25 : -0.25);
            }, { passive: false });

            stage.addEventListener('mousedown', (e) => {
                if (currentZoomLevel > 1) {
                    isDraggingZoom = true;
                    dragStartX = e.clientX - zoomTranslateX;
                    dragStartY = e.clientY - zoomTranslateY;
                    stage.classList.add('grabbing');
                }
            });

            window.addEventListener('mousemove', (e) => {
                if (isDraggingZoom) {
                    zoomTranslateX = e.clientX - dragStartX;
                    zoomTranslateY = e.clientY - dragStartY;
                    updateZoomTransform();
                }
            });

            window.addEventListener('mouseup', () => {
                isDraggingZoom = false;
                stage.classList.remove('grabbing');
            });

            document.addEventListener('keydown', (e) => {
                if (e.key === 'Escape') {
                    closeZoomModal();
                    closeModal();
                }
            });
        }

        // CSV Export Functions
        function exportActiveTabCsv() {
            if (activeTab === 'riwayat') exportRiwayatCsv();
            else if (activeTab === 'resume') exportResumeMatrixCsv();
            else if (activeTab === 'bobot') exportBobotCsv();
            else if (activeTab === 'rekap-nilai') exportRekapNilaiCsv();
            else if (activeTab === 'rekap-temuan') exportRekapTemuanCsv();
        }

        function downloadCsvFile(content, fileName) {
            const blob = new Blob(['\uFEFF' + content], { type: 'text/csv;charset=utf-8;' });
            const link = document.createElement('a');
            const url = URL.createObjectURL(blob);
            link.setAttribute('href', url);
            link.setAttribute('download', fileName);
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }

        function exportRiwayatCsv() {
            let csv = 'No,Tanggal,Periode,Pool,No Mobil,Plat Nomor,Auditor,Sample,Paket Service,Total Poin,Status Kelayakan\n';
            filteredAudits.forEach((a, idx) => {
                csv += [
                    idx + 1,
                    '"' + (a.tanggal || '') + '"',
                    '"' + (a.periode || '') + '"',
                    '"' + (a.pool || '') + '"',
                    '"' + (a.noMobil || '') + '"',
                    '"' + (a.platNomer || '') + '"',
                    '"' + (a.namaPetugas || '') + '"',
                    '"' + (a.jenisSample || '') + '"',
                    '"' + (a.paketService || '') + '"',
                    a.totalPoinTemuan || 0,
                    (a.totalPoinTemuan === 0 ? 'STANDAR' : 'TEMUAN')
                ].join(',') + '\n';
            });
            downloadCsvFile(csv, 'Riwayat_Audit_Armada.csv');
        }

        function exportResumeMatrixCsv() {
            const matrixAudits = filteredAudits.slice(0, 14);
            let csv = 'CHECKLIST ITEM';
            matrixAudits.forEach(a => { csv += ',"' + a.noMobil + ' (' + (a.platNomer || '') + ')"'; });
            csv += '\n';

            csv += 'TOTAL POIN TEMUAN';
            matrixAudits.forEach(a => { csv += ',' + (a.totalPoinTemuan || 0); });
            csv += '\n';

            const scores = matrixAudits.map(a => parseAuditScores(a));
            getAllSpecItems().forEach(it => {
                csv += '"' + it.sectionTitle + ' - ' + it.title + '"';
                matrixAudits.forEach((a, idx) => {
                    csv += ',' + (scores[idx][it.id] || 0);
                });
                csv += '\n';
            });

            downloadCsvFile(csv, 'Resume_Matriks_14_Armada.csv');
        }

        function exportBobotCsv() {
            let csv = 'No,Item Pemeriksaan,Seksi,Sub Group,Bobot,Temuan Poin,Point Max,Nilai Temuan,Jumlah Sample\n';
            const sampleCount = filteredAudits.length;
            const parsedScores = filteredAudits.map(a => parseAuditScores(a));

            getAllSpecItems().forEach((it, idx) => {
                const b = bobotMap[it.id] || it.bobot || 1.0;
                const pMax = it.maxPoints || 2;
                const totalMax = pMax * sampleCount;
                const earned = parsedScores.reduce((sum, sc) => sum + (sc[it.id] || 0), 0);
                const finding = Math.max(0, totalMax - earned);
                const nilaiTemuan = finding * b;

                csv += [
                    idx + 1,
                    '"' + it.title + '"',
                    '"' + it.sectionTitle + '"',
                    '"' + it.groupTitle + '"',
                    b,
                    finding,
                    pMax,
                    nilaiTemuan.toFixed(2),
                    sampleCount
                ].join(',') + '\n';
            });

            downloadCsvFile(csv, 'Bobot_Resume_Audit.csv');
        }

        function exportRekapNilaiCsv() {
            let csv = 'No,Seksi,Sub Group,Total Bobot Temuan,Nilai\n';
            const kpiResult = computeKpiGrade(filteredAudits);
            const sampleCount = filteredAudits.length;
            const parsedScores = filteredAudits.map(a => parseAuditScores(a));
            let idx = 1;

            kpiSpec.forEach(sec => {
                (sec.groups || []).forEach(grp => {
                    let totalBobotGroup = 0;
                    (grp.items || []).forEach(it => {
                        const b = bobotMap[it.id] || it.bobot || 1.0;
                        const pMax = it.maxPoints || 2;
                        const totalMax = pMax * sampleCount;
                        const earned = parsedScores.reduce((sum, sc) => sum + (sc[it.id] || 0), 0);
                        const finding = Math.max(0, totalMax - earned);
                        totalBobotGroup += (finding * b);
                    });
                    const pct = kpiResult.targetSample > 0 ? (totalBobotGroup / kpiResult.targetSample) : 0;
                    csv += [idx++, '"' + sec.title + '"', '"' + grp.title + '"', totalBobotGroup.toFixed(2), pct.toFixed(2)].join(',') + '\n';
                });
            });

            csv += '\nKPI CUCIAN:,' + kpiResult.kpiCucian.toFixed(2) + '\n';
            csv += 'Evaluasi Grade KPI:,' + kpiResult.gradeLetter + '\n';
            csv += 'Nilai KPI Final:,' + kpiResult.nilaiKpi.toFixed(2) + '\n';
            downloadCsvFile(csv, 'Rekap_Nilai_Sub_Group.csv');
        }

        function exportRekapTemuanCsv() {
            let csv = 'SEKSI,GROUP,DATA INPUT,OPERASIONAL (UNIT),OPERASIONAL (%),SERVICE (UNIT),SERVICE (%),TOTAL (UNIT),TOTAL (%)\n';
            const allItems = getAllSpecItems();
            const totSample = filteredAudits.length;
            const opAudits = filteredAudits.filter(a => (a.jenisSample || '').toUpperCase() === 'OPERASIONAL');
            const svcAudits = filteredAudits.filter(a => (a.jenisSample || '').toUpperCase() === 'SERVICE');
            const opScores = opAudits.map(a => parseAuditScores(a));
            const svcScores = svcAudits.map(a => parseAuditScores(a));
            const opSample = opAudits.length;
            const svcSample = svcAudits.length;

            allItems.forEach(it => {
                const opUnits = opScores.filter(sc => (sc[it.id] || 0) > 0).length;
                const svcUnits = svcScores.filter(sc => (sc[it.id] || 0) > 0).length;
                const totUnits = opUnits + svcUnits;

                const opPct = opSample > 0 ? ((opUnits / opSample) * 100).toFixed(1) : 0;
                const svcPct = svcSample > 0 ? ((svcUnits / svcSample) * 100).toFixed(1) : 0;
                const totPct = totSample > 0 ? ((totUnits / totSample) * 100).toFixed(1) : 0;

                const escSec = '"' + (it.sectionTitle || '').replace(/"/g, '""') + '"';
                const escGrp = '"' + (it.groupTitle || '').replace(/"/g, '""') + '"';
                const escItem = '"' + (it.title || '').replace(/"/g, '""') + '"';

                csv += [escSec, escGrp, escItem, opUnits, opPct + '%', svcUnits, svcPct + '%', totUnits, totPct + '%'].join(',') + '\n';
            });

            downloadCsvFile(csv, 'Rekap_Temuan_Audit.csv');
        }

        // ==========================================
        // EXCEL DOWNLOAD / EXPORT FUNCTIONS (.XLS)
        // ==========================================

        function downloadExcelFile(htmlTableContent, fileName) {
            const excelTemplate = 
                '<html xmlns:o="urn:schemas-microsoft-com:office:office" ' +
                'xmlns:x="urn:schemas-microsoft-com:office:excel" ' +
                'xmlns="http://www.w3.org/TR/REC-html40">' +
                '<head>' +
                '<meta http-equiv="content-type" content="application/vnd.ms-excel; charset=UTF-8">' +
                '<!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet>' +
                '<x:Name>Laporan</x:Name>' +
                '<x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions>' +
                '</x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->' +
                '<style>' +
                'table { border-collapse: collapse; font-family: Calibri, Arial, sans-serif; font-size: 11pt; } ' +
                'th { background-color: #0284c7; color: #ffffff; font-weight: bold; border: 0.5pt solid #94a3b8; padding: 7px 10px; text-align: center; } ' +
                'td { border: 0.5pt solid #cbd5e1; padding: 6px 8px; vertical-align: middle; } ' +
                '.text { mso-number-format:"\\@"; } ' +
                '.num { mso-number-format:"0"; text-align: right; } ' +
                '.dec { mso-number-format:"0\\.00"; text-align: right; } ' +
                '.pct { mso-number-format:"0\\.00%"; text-align: right; } ' +
                '.danger { background-color: #fee2e2; color: #b91c1c; font-weight: bold; } ' +
                '.success { background-color: #ecfdf5; color: #047857; font-weight: bold; } ' +
                '.section { background-color: #0f172a; color: #ffffff; font-weight: bold; text-align: left; } ' +
                '.group { background-color: #f1f5f9; color: #1e293b; font-weight: bold; text-align: left; } ' +
                '.title-header { font-size: 14pt; font-weight: bold; color: #0f172a; text-align: left; } ' +
                '.meta-row { font-size: 10pt; color: #475569; } ' +
                '</style>' +
                '</head>' +
                '<body>' + htmlTableContent + '</body></html>';

            const blob = new Blob(['\uFEFF' + excelTemplate], { type: 'application/vnd.ms-excel;charset=utf-8;' });
            const link = document.createElement('a');
            const url = URL.createObjectURL(blob);
            link.setAttribute('href', url);
            const actualName = fileName.endsWith('.xls') ? fileName : (fileName + '.xls');
            link.setAttribute('download', actualName);
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }

        function exportActiveTabExcel() {
            if (activeTab === 'riwayat') exportRiwayatExcel();
            else if (activeTab === 'resume') exportResumeMatrixExcel();
            else if (activeTab === 'bobot') exportBobotExcel();
            else if (activeTab === 'rekap-nilai') exportRekapNilaiExcel();
            else if (activeTab === 'rekap-temuan') exportRekapTemuanExcel();
        }

        function exportRiwayatExcel() {
            let html = '<table>';
            html += '<tr><td colspan="11" class="title-header">LAPORAN RIWAYAT AUDIT ARMADA BLUEBIRD</td></tr>';
            html += '<tr><td colspan="11" class="meta-row">Tanggal Unduh: ' + new Date().toLocaleString('id-ID') + ' | Total: ' + filteredAudits.length + ' Unit</td></tr>';
            html += '<tr><td colspan="11"></td></tr>';
            html += '<tr>' +
                '<th>No</th>' +
                '<th>Tanggal & Waktu</th>' +
                '<th>Periode</th>' +
                '<th>Pool</th>' +
                '<th>No Mobil</th>' +
                '<th>Plat Nomor</th>' +
                '<th>Auditor</th>' +
                '<th>Sample</th>' +
                '<th>Paket Service</th>' +
                '<th>Total Poin Temuan</th>' +
                '<th>Status Kelayakan</th>' +
            '</tr>';

            filteredAudits.forEach((a, idx) => {
                const isStandar = (a.totalPoinTemuan === 0);
                const statusClass = isStandar ? 'success' : 'danger';
                html += '<tr>' +
                    '<td class="num">' + (idx + 1) + '</td>' +
                    '<td class="text">' + (a.tanggal || '') + '</td>' +
                    '<td class="text">' + (a.periode || '') + '</td>' +
                    '<td class="text"><strong>' + (a.pool || '') + '</strong></td>' +
                    '<td class="text"><strong>' + (a.noMobil || '') + '</strong></td>' +
                    '<td class="text">' + (a.platNomer || '') + '</td>' +
                    '<td class="text">' + (a.namaPetugas || '') + '</td>' +
                    '<td class="text">' + (a.jenisSample || '') + '</td>' +
                    '<td class="text">' + (a.paketService || '') + '</td>' +
                    '<td class="num ' + (isStandar ? '' : 'danger') + '">' + (a.totalPoinTemuan || 0) + '</td>' +
                    '<td class="' + statusClass + '" style="text-align: center;">' + (isStandar ? 'STANDAR' : 'TEMUAN') + '</td>' +
                '</tr>';
            });

            html += '</table>';
            downloadExcelFile(html, 'Riwayat_Audit_Armada.xls');
        }

        function exportResumeMatrixExcel() {
            const matrixAudits = filteredAudits.slice(0, 14);
            let html = '<table>';
            html += '<tr><td colspan="' + (matrixAudits.length + 1) + '" class="title-header">FORM RESUME AUDIT ARMADA (MATRIKS 14 UNIT)</td></tr>';
            html += '<tr><td colspan="' + (matrixAudits.length + 1) + '" class="meta-row">Tanggal Unduh: ' + new Date().toLocaleString('id-ID') + '</td></tr>';
            html += '<tr><td colspan="' + (matrixAudits.length + 1) + '"></td></tr>';

            // Headers with Unit info
            html += '<tr>';
            html += '<th>DATA CEK ALL / CHECKLIST</th>';
            matrixAudits.forEach((a, idx) => {
                html += '<th>UNIT #' + (idx + 1) + '<br>' + (a.noMobil || '-') + '<br>(' + (a.platNomer || '-') + ')<br>' + (a.pool || '-') + ' • ' + (a.jenisSample || 'OP') + '</th>';
            });
            html += '</tr>';

            // Total Poin Temuan
            html += '<tr><td style="font-weight: bold; background: #ffffff;">TOTAL POIN TEMUAN</td>';
            matrixAudits.forEach(a => {
                const pt = a.totalPoinTemuan || 0;
                html += '<td class="num ' + (pt === 0 ? '' : 'danger') + '" style="text-align: center; font-weight: bold;">' + pt + '</td>';
            });
            html += '</tr>';

            const scores = matrixAudits.map(a => parseAuditScores(a));

            kpiSpec.forEach(sec => {
                html += '<tr>';
                html += '<td class="section" colspan="' + (matrixAudits.length + 1) + '">' + (sec.title || '') + '</td>';
                html += '</tr>';

                (sec.groups || []).forEach(grp => {
                    html += '<tr>';
                    html += '<td class="group" colspan="' + (matrixAudits.length + 1) + '">• ' + (grp.title || '') + '</td>';
                    html += '</tr>';

                    (grp.items || []).forEach(item => {
                        html += '<tr>';
                        html += '<td style="padding-left: 20px;">' + (item.title || item.name || '') + ' (' + (item.maxPoints || 2) + ' Poin)</td>';
                        matrixAudits.forEach((a, aIdx) => {
                            const sc = scores[aIdx][item.id] !== undefined ? scores[aIdx][item.id] : 0;
                            if (sc === 0) {
                                html += '<td class="num" style="color: #059669; text-align: center;">0</td>';
                            } else {
                                html += '<td class="num danger" style="text-align: center;">' + sc + '</td>';
                            }
                        });
                        html += '</tr>';
                    });
                });
            });

            html += '</table>';
            downloadExcelFile(html, 'Resume_Matriks_14_Armada.xls');
        }

        function exportBobotExcel() {
            let html = '<table>';
            html += '<tr><td colspan="8" class="title-header">TABEL BOBOT RESUME AUDIT ARMADA</td></tr>';
            html += '<tr><td colspan="8" class="meta-row">Tanggal Unduh: ' + new Date().toLocaleString('id-ID') + ' | Jumlah Sample: ' + filteredAudits.length + ' Mobil</td></tr>';
            html += '<tr><td colspan="8"></td></tr>';
            html += '<tr>' +
                '<th>No</th>' +
                '<th>Item Pemeriksaan</th>' +
                '<th>Seksi & Sub Group</th>' +
                '<th>Bobot</th>' +
                '<th>Temuan (Poin)</th>' +
                '<th>Point Max</th>' +
                '<th>Nilai Temuan (Bobot × Poin)</th>' +
                '<th>Jumlah Sample</th>' +
            '</tr>';

            const sampleCount = filteredAudits.length;
            const parsedScores = filteredAudits.map(a => parseAuditScores(a));
            let totalFindingAccum = 0;
            let totalNilaiAccum = 0;

            getAllSpecItems().forEach((it, idx) => {
                const b = bobotMap[it.id] || it.bobot || 1.0;
                const pMax = it.maxPoints || 2;
                const totalMax = pMax * sampleCount;
                const earned = parsedScores.reduce((sum, sc) => sum + (sc[it.id] || 0), 0);
                const finding = Math.max(0, totalMax - earned);
                const nilaiTemuan = finding * b;

                totalFindingAccum += finding;
                totalNilaiAccum += nilaiTemuan;

                const hasFinding = finding > 0;
                html += '<tr>' +
                    '<td class="num">' + (idx + 1) + '</td>' +
                    '<td class="text"><strong>' + it.title + '</strong></td>' +
                    '<td class="text">' + it.sectionTitle + ' • ' + it.groupTitle + '</td>' +
                    '<td class="dec" style="text-align: center;">' + b.toFixed(1) + '</td>' +
                    '<td class="num ' + (hasFinding ? 'danger' : '') + '" style="text-align: center;">' + finding + '</td>' +
                    '<td class="num" style="text-align: center;">' + pMax + '</td>' +
                    '<td class="dec ' + (hasFinding ? 'danger' : '') + '" style="text-align: center;">' + nilaiTemuan.toFixed(2) + '</td>' +
                    '<td class="num" style="text-align: center;">' + sampleCount + '</td>' +
                '</tr>';
            });

            html += '<tr style="font-weight: bold; background: #f8fafc;">' +
                '<td colspan="4" style="text-align: right;">TOTAL AKUMULASI:</td>' +
                '<td class="num danger" style="text-align: center;">' + totalFindingAccum + '</td>' +
                '<td></td>' +
                '<td class="dec danger" style="text-align: center;">' + totalNilaiAccum.toFixed(2) + '</td>' +
                '<td class="num" style="text-align: center;">' + sampleCount + '</td>' +
            '</tr>';

            html += '</table>';
            downloadExcelFile(html, 'Bobot_Resume_Audit.xls');
        }

        function exportRekapNilaiExcel() {
            const kpiResult = computeKpiGrade(filteredAudits);
            const sampleCount = filteredAudits.length;
            const parsedScores = filteredAudits.map(a => parseAuditScores(a));

            let html = '<table>';
            html += '<tr><td colspan="5" class="title-header">REKAPITULASI NILAI SUB GROUP & EVALUASI GRADE KPI</td></tr>';
            html += '<tr><td colspan="5" class="meta-row">Tanggal Unduh: ' + new Date().toLocaleString('id-ID') + '</td></tr>';
            html += '<tr><td colspan="5"></td></tr>';

            // KPI Showcase Box
            html += '<tr><th colspan="5" style="background: #0f172a; text-align: left;">RINGKASAN KPI</th></tr>';
            html += '<tr><td><strong>KPI CUCIAN:</strong></td><td colspan="4" style="font-size: 14pt; font-weight: bold; color: #7c3aed;">' + kpiResult.kpiCucian.toFixed(2) + ' (Cucian - 20)</td></tr>';
            html += '<tr><td><strong>NILAI KPI FINAL:</strong></td><td colspan="4" style="font-size: 13pt; font-weight: bold;">' + kpiResult.nilaiKpi.toFixed(2) + '</td></tr>';
            html += '<tr><td><strong>EVALUASI GRADE KPI:</strong></td><td colspan="4" style="font-size: 11pt; color: #0284c7;">GRADE ' + kpiResult.gradeLetter + ' (' + kpiResult.gradeDesc + ')</td></tr>';
            html += '<tr><td>Seksi Exterior (/5):</td><td colspan="4">' + kpiResult.extPct.toFixed(2) + '</td></tr>';
            html += '<tr><td>Seksi Interior (/5):</td><td colspan="4">' + kpiResult.intPct.toFixed(2) + '</td></tr>';
            html += '<tr><td>Seksi Cucian (/3):</td><td colspan="4">' + kpiResult.cucPct.toFixed(2) + '</td></tr>';
            html += '<tr><td>Ketentuan Sample:</td><td colspan="4">' + sampleCount + ' / ' + kpiResult.targetSample + ' Unit</td></tr>';
            html += '<tr><td>Rumus Nilai KPI:</td><td colspan="4">(((Seksi Exterior + Seksi Interior) / 2) - 20)</td></tr>';
            html += '<tr><td colspan="5"></td></tr>';

            // Sub Group Table
            html += '<tr>' +
                '<th>No</th>' +
                '<th>Seksi & Sub Group</th>' +
                '<th>Jumlah Item</th>' +
                '<th>Total Bobot Temuan</th>' +
                '<th>Nilai Sub Group</th>' +
            '</tr>';

            let grpIdx = 1;
            kpiSpec.forEach(sec => {
                (sec.groups || []).forEach(grp => {
                    let totalBobotGroup = 0;
                    (grp.items || []).forEach(it => {
                        const b = bobotMap[it.id] || it.bobot || 1.0;
                        const pMax = it.maxPoints || 2;
                        const totalMax = pMax * sampleCount;
                        const earned = parsedScores.reduce((sum, sc) => sum + (sc[it.id] || 0), 0);
                        const finding = Math.max(0, totalMax - earned);
                        totalBobotGroup += (finding * b);
                    });
                    const pct = kpiResult.targetSample > 0 ? (totalBobotGroup / kpiResult.targetSample) : 0;
                    html += '<tr>' +
                        '<td class="num">' + (grpIdx++) + '</td>' +
                        '<td class="text"><strong>' + grp.title + '</strong> (' + sec.title + ')</td>' +
                        '<td class="num" style="text-align: center;">' + (grp.items || []).length + '</td>' +
                        '<td class="dec" style="text-align: center;">' + totalBobotGroup.toFixed(2) + '</td>' +
                        '<td class="pct" style="text-align: center; font-weight: bold; color: #0369a1;">' + pct.toFixed(2) + '</td>' +
                    '</tr>';
                });
            });

            html += '</table>';
            downloadExcelFile(html, 'Rekap_Nilai_Sub_Group.xls');
        }

        function exportRekapTemuanExcel() {
            let html = '<table>';
            html += '<tr><td colspan="7" class="title-header">REKAP TEMUAN AUDIT ARMADA (OPERASIONAL & SERVICE)</td></tr>';
            html += '<tr><td colspan="7" class="meta-row">Tanggal Unduh: ' + new Date().toLocaleString('id-ID') + ' | Kritis jika > 50%</td></tr>';
            html += '<tr><td colspan="7"></td></tr>';
            html += '<tr>' +
                '<th>Titik Pemeriksaan / Checklist</th>' +
                '<th>Seksi & Grup</th>' +
                '<th>Operasional (Unit)</th>' +
                '<th>Operasional (%)</th>' +
                '<th>Service (Unit)</th>' +
                '<th>Service (%)</th>' +
                '<th>Total Gabungan (%)</th>' +
            '</tr>';

            const allItems = getAllSpecItems();
            const totSample = filteredAudits.length;
            const opAudits = filteredAudits.filter(a => (a.jenisSample || '').toUpperCase() === 'OPERASIONAL');
            const svcAudits = filteredAudits.filter(a => (a.jenisSample || '').toUpperCase() === 'SERVICE');
            const opScores = opAudits.map(a => parseAuditScores(a));
            const svcScores = svcAudits.map(a => parseAuditScores(a));
            const opSample = opAudits.length;
            const svcSample = svcAudits.length;

            allItems.forEach(it => {
                const opUnits = opScores.filter(sc => (sc[it.id] || 0) > 0).length;
                const svcUnits = svcScores.filter(sc => (sc[it.id] || 0) > 0).length;
                const totUnits = opUnits + svcUnits;

                const opPct = opSample > 0 ? ((opUnits / opSample) * 100) : 0;
                const svcPct = svcSample > 0 ? ((svcUnits / svcSample) * 100) : 0;
                const totPct = totSample > 0 ? ((totUnits / totSample) * 100) : 0;

                const isOpCritical = opPct > 50;
                const isSvcCritical = svcPct > 50;
                const isTotCritical = totPct > 50;

                html += '<tr>' +
                    '<td class="text"><strong>' + it.title + '</strong></td>' +
                    '<td class="text">' + it.sectionTitle + ' • ' + it.groupTitle + '</td>' +
                    '<td class="num ' + (isOpCritical ? 'danger' : '') + '" style="text-align: center;">' + opUnits + '</td>' +
                    '<td class="pct ' + (isOpCritical ? 'danger' : '') + '" style="text-align: center;">' + opPct.toFixed(1) + ' %</td>' +
                    '<td class="num ' + (isSvcCritical ? 'danger' : '') + '" style="text-align: center;">' + svcUnits + '</td>' +
                    '<td class="pct ' + (isSvcCritical ? 'danger' : '') + '" style="text-align: center;">' + svcPct.toFixed(1) + ' %</td>' +
                    '<td class="pct ' + (isTotCritical ? 'danger' : '') + '" style="text-align: center; font-weight: bold;">' + totPct.toFixed(1) + ' %</td>' +
                '</tr>';
            });

            html += '</table>';
            downloadExcelFile(html, 'Rekap_Temuan_Audit.xls');
        }

        function exportAllWorkbookExcel() {
            // Generates an Excel XML Spreadsheet with all 5 distinct sheets
            let xml = '<?xml version="1.0" encoding="UTF-8"?>\n' +
                '<?mso-application progid="Excel.Sheet"?>\n' +
                '<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"\n' +
                ' xmlns:o="urn:schemas-microsoft-com:office:office"\n' +
                ' xmlns:x="urn:schemas-microsoft-com:office:excel"\n' +
                ' xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"\n' +
                ' xmlns:html="http://www.w3.org/TR/REC-html40">\n' +
                '<Styles>\n' +
                ' <Style ss:ID="Default" ss:Name="Normal"><Alignment ss:Vertical="Center"/><Font ss:FontName="Calibri" ss:Size="11"/></Style>\n' +
                ' <Style ss:ID="Header"><Alignment ss:Horizontal="Center" ss:Vertical="Center"/><Font ss:FontName="Calibri" ss:Size="11" ss:Color="#FFFFFF" ss:Bold="1"/><Interior ss:Color="#0284C7" ss:Pattern="Solid"/></Style>\n' +
                ' <Style ss:ID="Title"><Font ss:FontName="Calibri" ss:Size="14" ss:Color="#0F172A" ss:Bold="1"/></Style>\n' +
                ' <Style ss:ID="Section"><Font ss:FontName="Calibri" ss:Size="11" ss:Color="#FFFFFF" ss:Bold="1"/><Interior ss:Color="#0F172A" ss:Pattern="Solid"/></Style>\n' +
                ' <Style ss:ID="Group"><Font ss:FontName="Calibri" ss:Size="11" ss:Color="#1E293B" ss:Bold="1"/><Interior ss:Color="#F1F5F9" ss:Pattern="Solid"/></Style>\n' +
                ' <Style ss:ID="Danger"><Alignment ss:Horizontal="Center"/><Font ss:FontName="Calibri" ss:Size="11" ss:Color="#B91C1C" ss:Bold="1"/><Interior ss:Color="#FEE2E2" ss:Pattern="Solid"/></Style>\n' +
                ' <Style ss:ID="Success"><Alignment ss:Horizontal="Center"/><Font ss:FontName="Calibri" ss:Size="11" ss:Color="#047857" ss:Bold="1"/><Interior ss:Color="#ECFDF5" ss:Pattern="Solid"/></Style>\n' +
                ' <Style ss:ID="NumCenter"><Alignment ss:Horizontal="Center"/><NumberFormat ss:Format="0"/></Style>\n' +
                ' <Style ss:ID="DecCenter"><Alignment ss:Horizontal="Center"/><NumberFormat ss:Format="0.00"/></Style>\n' +
                '</Styles>\n';

            // Sheet 1: Riwayat Audit
            xml += '<Worksheet ss:Name="Riwayat Audit"><Table>\n';
            xml += '<Row><Cell ss:StyleID="Title"><Data ss:Type="String">RIWAYAT AUDIT ARMADA BLUEBIRD</Data></Cell></Row>\n';
            xml += '<Row><Cell><Data ss:Type="String">Total Data: ' + filteredAudits.length + '</Data></Cell></Row>\n';
            xml += '<Row ss:StyleID="Header">\n' +
                '<Cell><Data ss:Type="String">No</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Tanggal</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Periode</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Pool</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">No Mobil</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Plat Nomor</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Auditor</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Sample</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Paket Service</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Poin Temuan</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Status</Data></Cell>\n' +
                '</Row>\n';

            filteredAudits.forEach((a, idx) => {
                const isStd = (a.totalPoinTemuan === 0);
                xml += '<Row>\n' +
                    '<Cell ss:StyleID="NumCenter"><Data ss:Type="Number">' + (idx + 1) + '</Data></Cell>\n' +
                    '<Cell><Data ss:Type="String">' + (a.tanggal || '') + '</Data></Cell>\n' +
                    '<Cell><Data ss:Type="String">' + (a.periode || '') + '</Data></Cell>\n' +
                    '<Cell><Data ss:Type="String">' + (a.pool || '') + '</Data></Cell>\n' +
                    '<Cell><Data ss:Type="String">' + (a.noMobil || '') + '</Data></Cell>\n' +
                    '<Cell><Data ss:Type="String">' + (a.platNomer || '') + '</Data></Cell>\n' +
                    '<Cell><Data ss:Type="String">' + (a.namaPetugas || '') + '</Data></Cell>\n' +
                    '<Cell><Data ss:Type="String">' + (a.jenisSample || '') + '</Data></Cell>\n' +
                    '<Cell><Data ss:Type="String">' + (a.paketService || '') + '</Data></Cell>\n' +
                    '<Cell ss:StyleID="' + (isStd ? 'NumCenter' : 'Danger') + '"><Data ss:Type="Number">' + (a.totalPoinTemuan || 0) + '</Data></Cell>\n' +
                    '<Cell ss:StyleID="' + (isStd ? 'Success' : 'Danger') + '"><Data ss:Type="String">' + (isStd ? 'STANDAR' : 'TEMUAN') + '</Data></Cell>\n' +
                    '</Row>\n';
            });
            xml += '</Table></Worksheet>\n';

            // Sheet 2: Bobot Resume
            xml += '<Worksheet ss:Name="Bobot Resume"><Table>\n';
            xml += '<Row><Cell ss:StyleID="Title"><Data ss:Type="String">BOBOT RESUME AUDIT</Data></Cell></Row>\n';
            xml += '<Row ss:StyleID="Header">\n' +
                '<Cell><Data ss:Type="String">No</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Item Pemeriksaan</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Seksi</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Sub Group</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Bobot</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Temuan (Poin)</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Point Max</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Nilai Temuan</Data></Cell>\n' +
                '</Row>\n';

            const sampleCount = filteredAudits.length;
            const parsedScores = filteredAudits.map(a => parseAuditScores(a));
            getAllSpecItems().forEach((it, idx) => {
                const b = bobotMap[it.id] || it.bobot || 1.0;
                const pMax = it.maxPoints || 2;
                const totalMax = pMax * sampleCount;
                const earned = parsedScores.reduce((sum, sc) => sum + (sc[it.id] || 0), 0);
                const finding = Math.max(0, totalMax - earned);
                const nilaiTemuan = finding * b;

                xml += '<Row>\n' +
                    '<Cell ss:StyleID="NumCenter"><Data ss:Type="Number">' + (idx + 1) + '</Data></Cell>\n' +
                    '<Cell><Data ss:Type="String">' + it.title + '</Data></Cell>\n' +
                    '<Cell><Data ss:Type="String">' + it.sectionTitle + '</Data></Cell>\n' +
                    '<Cell><Data ss:Type="String">' + it.groupTitle + '</Data></Cell>\n' +
                    '<Cell ss:StyleID="DecCenter"><Data ss:Type="Number">' + b + '</Data></Cell>\n' +
                    '<Cell ss:StyleID="' + (finding > 0 ? 'Danger' : 'NumCenter') + '"><Data ss:Type="Number">' + finding + '</Data></Cell>\n' +
                    '<Cell ss:StyleID="NumCenter"><Data ss:Type="Number">' + pMax + '</Data></Cell>\n' +
                    '<Cell ss:StyleID="DecCenter"><Data ss:Type="Number">' + nilaiTemuan.toFixed(2) + '</Data></Cell>\n' +
                    '</Row>\n';
            });
            xml += '</Table></Worksheet>\n';

            // Sheet 3: Rekap Nilai
            const kpiResult = computeKpiGrade(filteredAudits);
            xml += '<Worksheet ss:Name="Rekap Nilai"><Table>\n';
            xml += '<Row><Cell ss:StyleID="Title"><Data ss:Type="String">REKAP NILAI SUB GROUP & KPI CUCIAN</Data></Cell></Row>\n';
            xml += '<Row><Cell><Data ss:Type="String">KPI CUCIAN: ' + kpiResult.kpiCucian.toFixed(2) + ' (Cucian - 20)</Data></Cell></Row>\n';
            xml += '<Row><Cell><Data ss:Type="String">Nilai KPI: ' + kpiResult.nilaiKpi.toFixed(2) + '</Data></Cell></Row>\n';
            xml += '<Row ss:StyleID="Header">\n' +
                '<Cell><Data ss:Type="String">No</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Seksi</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Sub Group</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Total Bobot Temuan</Data></Cell>\n' +
                '<Cell><Data ss:Type="String">Nilai Sub Group</Data></Cell>\n' +
                '</Row>\n';

            let grpIdx = 1;
            kpiSpec.forEach(sec => {
                (sec.groups || []).forEach(grp => {
                    let totalBobotGroup = 0;
                    (grp.items || []).forEach(it => {
                        const b = bobotMap[it.id] || it.bobot || 1.0;
                        const pMax = it.maxPoints || 2;
                        const totalMax = pMax * sampleCount;
                        const earned = parsedScores.reduce((sum, sc) => sum + (sc[it.id] || 0), 0);
                        const finding = Math.max(0, totalMax - earned);
                        totalBobotGroup += (finding * b);
                    });
                    const pct = kpiResult.targetSample > 0 ? (totalBobotGroup / kpiResult.targetSample) : 0;
                    xml += '<Row>\n' +
                        '<Cell ss:StyleID="NumCenter"><Data ss:Type="Number">' + (grpIdx++) + '</Data></Cell>\n' +
                        '<Cell><Data ss:Type="String">' + sec.title + '</Data></Cell>\n' +
                        '<Cell><Data ss:Type="String">' + grp.title + '</Data></Cell>\n' +
                        '<Cell ss:StyleID="DecCenter"><Data ss:Type="Number">' + totalBobotGroup.toFixed(2) + '</Data></Cell>\n' +
                        '<Cell ss:StyleID="DecCenter"><Data ss:Type="String">' + pct.toFixed(2) + '</Data></Cell>\n' +
                        '</Row>\n';
                });
            });
            xml += '</Table></Worksheet>\n';

            xml += '</Workbook>';

            const blob = new Blob(['\uFEFF' + xml], { type: 'application/vnd.ms-excel;charset=utf-8;' });
            const link = document.createElement('a');
            const url = URL.createObjectURL(blob);
            link.setAttribute('href', url);
            link.setAttribute('download', 'Buku_Kerja_Audit_Armada_Lengkap.xls');
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }

        /* ================= INPUT AUDIT FORM CONTROLLER ================= */
        const STD_PHOTO_SLOTS = [
            { key: 'foto_depan', label: '1. Foto Depan' },
            { key: 'foto_belakang', label: '2. Foto Belakang' },
            { key: 'foto_kanan', label: '3. Samping Kanan' },
            { key: 'foto_kiri', label: '4. Samping Kiri' },
            { key: 'foto_interior', label: '5. Interior Depan' },
            { key: 'foto_bagasi', label: '6. Bagasi Belakang' },
            { key: 'foto_mesin', label: '7. Ruang Mesin' }
        ];

        function initAuditForm() {
            // Set default date to today
            const today = new Date().toISOString().split('T')[0];
            const dateInput = document.getElementById('input-tanggal');
            if (dateInput && !dateInput.value) dateInput.value = today;

            const tglServiceInput = document.getElementById('input-tgl-service');
            if (tglServiceInput && !tglServiceInput.value) tglServiceInput.value = today;

            renderStdPhotoSlots();
            populateFormMasterData();
            if (kpiSpec && kpiSpec.length > 0) {
                renderKpiChecklistForm();
            }
        }

        function renderStdPhotoSlots() {
            const container = document.getElementById('std-photos-container');
            if (!container) return;
            container.innerHTML = '';

            STD_PHOTO_SLOTS.forEach(slot => {
                const card = document.createElement('div');
                card.className = 'std-photo-card' + (stdPhotos[slot.key] ? ' has-photo' : '');
                card.id = 'std-card-' + slot.key;

                if (stdPhotos[slot.key]) {
                    card.innerHTML = 
                        '<div style="font-size:11.5px; font-weight:700; color:var(--slate-800);">' + slot.label + '</div>' +
                        '<img src="' + stdPhotos[slot.key] + '" class="std-photo-preview" alt="' + slot.label + '">' +
                        '<button type="button" class="btn btn-outline-danger" style="padding:4px 8px; font-size:11px;" onclick="removeStdPhoto(\'' + slot.key + '\')">🗑️ Hapus</button>';
                } else {
                    card.innerHTML = 
                        '<div style="font-size:11.5px; font-weight:700; color:var(--slate-700);">' + slot.label + '</div>' +
                        '<div style="font-size:24px; opacity:0.6;">📷</div>' +
                        '<input type="file" id="file-' + slot.key + '" accept="image/*" style="display:none;" onchange="onStdPhotoSelect(\'' + slot.key + '\', this)">' +
                        '<button type="button" class="photo-upload-btn" onclick="document.getElementById(\'file-' + slot.key + '\').click()">Pilih Foto</button>';
                }
                container.appendChild(card);
            });
        }

        function populateFormMasterData() {
            // Populate Periode dropdown
            const periodeSel = document.getElementById('input-periode');
            if (periodeSel && masterPeriodes.length > 0 && periodeSel.options.length <= 1) {
                const cur = periodeSel.value;
                masterPeriodes.forEach(p => {
                    const opt = document.createElement('option');
                    opt.value = p.periode;
                    opt.textContent = p.periode + (p.keterangan ? ' - ' + p.keterangan : '');
                    if (p.periode === cur) opt.selected = true;
                    periodeSel.appendChild(opt);
                });
                if (!periodeSel.value && masterPeriodes.length > 0) {
                    periodeSel.value = masterPeriodes[masterPeriodes.length - 1].periode;
                }
            }

            // Populate Pool dropdown
            const poolSel = document.getElementById('input-pool');
            if (poolSel && masterPools.length > 0 && poolSel.options.length <= 1) {
                const cur = poolSel.value;
                masterPools.forEach(p => {
                    const opt = document.createElement('option');
                    opt.value = p.kodePool;
                    opt.textContent = p.kodePool + (p.namaPool ? ' (' + p.namaPool + ')' : '');
                    if (p.kodePool === cur) opt.selected = true;
                    poolSel.appendChild(opt);
                });
                if (!poolSel.value && masterPools.length > 0) {
                    poolSel.value = masterPools[0].kodePool;
                }
            }

            // Datalist Vehicles
            const vList = document.getElementById('vehicles-list');
            if (vList && masterVehicles.length > 0) {
                vList.innerHTML = '';
                masterVehicles.forEach(v => {
                    const opt = document.createElement('option');
                    opt.value = v.noMobil;
                    opt.textContent = v.noMobil + ' • ' + (v.platNomer || '') + ' • ' + (v.pool || '') + ' • ' + (v.tipe || '');
                    vList.appendChild(opt);
                });
            }

            // Datalist Users
            const uNipList = document.getElementById('users-nip-list');
            const uNameList = document.getElementById('users-name-list');
            if (uNipList && masterUsers.length > 0) {
                uNipList.innerHTML = '';
                uNameList.innerHTML = '';
                masterUsers.forEach(u => {
                    const opt1 = document.createElement('option');
                    opt1.value = u.nip;
                    opt1.textContent = u.namaPetugas + ' (' + (u.levelAkses || 'User') + ')';
                    uNipList.appendChild(opt1);

                    const opt2 = document.createElement('option');
                    opt2.value = u.namaPetugas;
                    opt2.textContent = 'NIP: ' + u.nip;
                    uNameList.appendChild(opt2);
                });
            }
        }

        function onVehicleInput(val) {
            if (!val) return;
            const query = val.trim().toLowerCase();
            const found = masterVehicles.find(v => (v.noMobil && v.noMobil.toLowerCase() === query));
            if (found) {
                if (found.platNomer) document.getElementById('input-plat').value = found.platNomer;
                if (found.tipe) document.getElementById('input-tipe').value = found.tipe;
                if (found.warna) document.getElementById('input-warna').value = found.warna;
                if (found.tahun) document.getElementById('input-tahun').value = found.tahun;
                if (found.pool) document.getElementById('input-pool').value = found.pool;
            }
        }

        function onNipInput(val) {
            if (!val) return;
            const query = val.trim().toLowerCase();
            const found = masterUsers.find(u => (u.nip && u.nip.toLowerCase() === query));
            if (found) {
                if (found.namaPetugas) document.getElementById('input-nama-petugas').value = found.namaPetugas;
            }
        }

        function onJenisSampleChange() {
            const val = document.getElementById('input-jenis-sample').value;
            const grpTgl = document.getElementById('group-tgl-service');
            const grpPkt = document.getElementById('group-paket-service');
            if (val === 'SERVICE') {
                if (grpTgl) grpTgl.style.display = 'flex';
                if (grpPkt) grpPkt.style.display = 'flex';
            } else {
                if (grpTgl) grpTgl.style.display = 'none';
                if (grpPkt) grpPkt.style.display = 'none';
            }
            renderKpiChecklistForm();
        }

        function findItemSpec(itemId) {
            if (!kpiSpec) return null;
            for (let s = 0; s < kpiSpec.length; s++) {
                const grps = kpiSpec[s].groups || [];
                for (let g = 0; g < grps.length; g++) {
                    const itms = grps[g].items || [];
                    for (let i = 0; i < itms.length; i++) {
                        if (itms[i].id === itemId) return itms[i];
                    }
                }
            }
            return null;
        }

        function renderKpiChecklistForm() {
            const container = document.getElementById('kpi-checklist-container');
            if (!container || !kpiSpec || kpiSpec.length === 0) return;

            container.innerHTML = '';
            checklistRendered = true;

            const sampleType = (document.getElementById('input-jenis-sample')?.value || 'OPERASIONAL').toUpperCase();
            const isOperasional = sampleType === 'OPERASIONAL';

            kpiSpec.forEach((sec, sIdx) => {
                const visibleGroups = (sec.groups || []).filter(grp => {
                    const isHidden = isOperasional && (
                        grp.id === 'INT_EXS_SERVICE' ||
                        grp.id === 'CUC_LENGKAP' ||
                        (grp.title && (grp.title.toUpperCase().includes('EXS SERVICE') || grp.title.toUpperCase().includes('CUCI LENGKAP')))
                    );
                    if (isHidden) {
                        (grp.items || []).forEach(it => {
                            formScores[it.id] = 0;
                            delete formNotes[it.id];
                            delete formPhotos[it.id];
                            for (let s = 0; s < 10; s++) {
                                delete formPhotos[it.id + '_' + s];
                                delete formNotes[it.id + '_' + s];
                            }
                        });
                        return false;
                    }
                    return true;
                });

                if (visibleGroups.length === 0) return;

                const secBanner = document.createElement('div');
                secBanner.className = 'kpi-section-banner';
                secBanner.innerHTML = '<span>' + sec.title + '</span><span style="font-size:11px; opacity:0.8; font-weight:600;">Bagian ' + (sIdx + 1) + '</span>';
                container.appendChild(secBanner);

                visibleGroups.forEach(grp => {
                    const grpTitle = document.createElement('div');
                    grpTitle.style.cssText = 'font-size:12.5px; font-weight:800; color:var(--slate-600); margin: 12px 4px 6px; text-transform:uppercase;';
                    grpTitle.textContent = '• ' + grp.title;
                    container.appendChild(grpTitle);

                    (grp.items || []).forEach(it => {
                        if (typeof formScores[it.id] === 'undefined') {
                            formScores[it.id] = 0;
                        }

                        const maxPts = it.maxPoints || 2;
                        const bVal = bobotMap[it.id] || it.bobot || 1.0;
                        const isFinding = formScores[it.id] > 0;

                        const row = document.createElement('div');
                        row.className = 'kpi-item-row' + (isFinding ? ' has-finding' : '');
                        row.id = 'kpi-row-' + it.id;

                        // Build pill buttons
                        let pillsHtml = '';
                        for (let p = 0; p <= maxPts; p++) {
                            const isSelected = formScores[it.id] === p;
                            const pillClass = isSelected ? (p === 0 ? 'score-pill active-zero' : 'score-pill active-finding') : 'score-pill';
                            pillsHtml += '<button type="button" class="' + pillClass + '" onclick="setKpiScore(\'' + it.id + '\', ' + p + ')">' + p + '</button>';
                        }

                        row.innerHTML = 
                            '<div class="kpi-item-header">' +
                                '<div class="kpi-item-title">' +
                                    '<span style="font-weight:800; color:var(--primary); font-size:12px;">[' + it.id + ']</span>' +
                                    '<span>' + it.name + '</span>' +
                                    '<span style="font-size:11px; background:var(--slate-200); padding:2px 6px; border-radius:4px; font-weight:700; color:var(--slate-700);">Bobot: ' + bVal + '</span>' +
                                '</div>' +
                                '<div class="score-pill-group" id="score-pills-' + it.id + '">' +
                                    pillsHtml +
                                '</div>' +
                            '</div>' +
                            '<div class="kpi-finding-box" id="finding-box-' + it.id + '" style="' + (isFinding ? 'display:flex; flex-direction:column; gap:8px;' : 'display:none;') + '">' +
                            '</div>';
                        container.appendChild(row);

                        if (isFinding) {
                            renderFindingSlots(it.id);
                        }
                    });
                });
            });

            updateFormScoreSummary();
        }

        function renderFindingSlots(itemId) {
            const fBox = document.getElementById('finding-box-' + itemId);
            if (!fBox) return;

            const score = formScores[itemId] || 0;
            if (score <= 0) {
                fBox.style.display = 'none';
                fBox.innerHTML = '';
                return;
            }

            fBox.style.display = 'flex';
            fBox.style.flexDirection = 'column';
            fBox.style.gap = '8px';
            fBox.innerHTML = '';

            const itemSpec = findItemSpec(itemId);

            for (let s = 0; s < score; s++) {
                const slotKey = itemId + '_' + s;
                const photoSrc = formPhotos[slotKey] || '';
                const noteVal = formNotes[slotKey] || '';
                const labelText = (itemSpec && itemSpec.photoLabels && itemSpec.photoLabels[s]) ? itemSpec.photoLabels[s] : ('Foto Point ' + (s + 1));

                const slotRow = document.createElement('div');
                slotRow.style.cssText = 'background:' + (photoSrc ? '#f0fdf4' : '#fffbeb') + '; border:1.5px solid ' + (photoSrc ? '#16a34a' : '#f59e0b') + '; border-radius:8px; padding:10px; display:flex; flex-direction:column; gap:8px;';
                
                let thumbHtml = '';
                if (photoSrc) {
                    thumbHtml = '<div class="photo-thumbnail-box" style="position:relative; width:60px; height:60px;">' +
                        '<img src="' + photoSrc + '" alt="Point ' + (s + 1) + '" style="width:100%; height:100%; object-fit:cover; border-radius:6px; border:1px solid #16a34a;">' +
                        '<button type="button" class="remove-btn" title="Hapus foto point ini (point berikutnya akan bergeser)" onclick="removeKpiPhotoSlot(\'' + itemId + '\', ' + s + ')">&times;</button>' +
                    '</div>';
                }

                let deleteBtnHtml = '';
                if (photoSrc) {
                    deleteBtnHtml = '<button type="button" class="btn btn-secondary" onclick="removeKpiPhotoSlot(\'' + itemId + '\', ' + s + ')" style="padding:5px 10px; font-size:11px; color:#ef4444; border-color:#fca5a5; background:#fff;">🗑️ Hapus Foto Point ' + (s + 1) + '</button>';
                }

                slotRow.innerHTML = 
                    '<div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:4px;">' +
                        '<span style="font-weight:700; font-size:12px; color:' + (photoSrc ? '#15803d' : '#b45309') + ';">' +
                            (photoSrc ? '✅' : '📷') + ' Point ' + (s + 1) + ': ' + labelText +
                        '</span>' +
                        '<span style="font-size:10px; font-weight:700; padding:2px 6px; border-radius:4px; background:' + (photoSrc ? '#dcfce7' : '#fef3c7') + '; color:' + (photoSrc ? '#15803d' : '#b45309') + ';">' +
                            (photoSrc ? 'SUDAH DIUPLOAD' : 'BELUM DIUPLOAD') +
                        '</span>' +
                    '</div>' +
                    '<div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">' +
                        thumbHtml +
                        '<input type="file" id="photo-input-' + slotKey + '" accept="image/*" style="display:none;" onchange="onSlotPhotoSelect(\'' + itemId + '\', ' + s + ', this)">' +
                        '<button type="button" class="photo-upload-btn" onclick="document.getElementById(\'photo-input-' + slotKey + '\').click()" style="padding:5px 12px; font-size:11.5px;">' +
                            '📷 ' + (photoSrc ? 'Ganti Foto Point ' + (s + 1) : 'Upload Foto Point ' + (s + 1)) +
                        '</button>' +
                        deleteBtnHtml +
                    '</div>' +
                    '<div>' +
                        '<input type="text" class="form-control" placeholder="Keterangan foto point ' + (s + 1) + ' (opsional)..." id="note-' + slotKey + '" value="' + noteVal + '" oninput="onSlotNoteInput(\'' + itemId + '\', ' + s + ', this.value)" style="font-size:12px;">' +
                    '</div>';
                fBox.appendChild(slotRow);
            }
        }

        function setKpiScore(itemId, score) {
            const prevScore = formScores[itemId] || 0;
            formScores[itemId] = score;

            // Jika skor diturunkan, bersihkan slot berlebih
            if (score < prevScore) {
                for (let s = score; s < 10; s++) {
                    delete formPhotos[itemId + '_' + s];
                    delete formNotes[itemId + '_' + s];
                }
                if (score === 0) {
                    delete formPhotos[itemId];
                    delete formNotes[itemId];
                }
            }

            // Update row style
            const row = document.getElementById('kpi-row-' + itemId);
            if (row) {
                if (score > 0) row.classList.add('has-finding');
                else row.classList.remove('has-finding');
            }

            // Update pill styles
            const pillsGroup = document.getElementById('score-pills-' + itemId);
            if (pillsGroup) {
                const buttons = pillsGroup.querySelectorAll('button');
                buttons.forEach((btn, idx) => {
                    btn.className = 'score-pill';
                    if (idx === score) {
                        btn.className = score === 0 ? 'score-pill active-zero' : 'score-pill active-finding';
                    }
                });
            }

            renderFindingSlots(itemId);
            updateFormScoreSummary();
        }

        function onSlotNoteInput(itemId, slotIdx, val) {
            const slotKey = itemId + '_' + slotIdx;
            formNotes[slotKey] = val.toUpperCase();
            formNotes[itemId] = val.toUpperCase();
        }

        function onSlotPhotoSelect(itemId, slotIdx, input) {
            if (!input.files || input.files.length === 0) return;
            const file = input.files[0];
            const reader = new FileReader();
            reader.onload = (e) => {
                const slotKey = itemId + '_' + slotIdx;
                formPhotos[slotKey] = e.target.result;
                if (!formPhotos[itemId]) {
                    formPhotos[itemId] = e.target.result;
                }
                renderFindingSlots(itemId);
            };
            reader.readAsDataURL(file);
        }

        function removeKpiPhotoSlot(itemId, slotIdx) {
            const score = formScores[itemId] || 0;
            // Geser foto dan catatan dari point berikutnya (i + 1) ke point i
            // Contoh: point 1, 2, 3 -> hapus photo 2 -> point 3 otomatis pindah ke point 2
            for (let i = slotIdx; i < score - 1; i++) {
                const currKey = itemId + '_' + i;
                const nextKey = itemId + '_' + (i + 1);
                if (formPhotos[nextKey]) {
                    formPhotos[currKey] = formPhotos[nextKey];
                } else {
                    delete formPhotos[currKey];
                }
                if (formNotes[nextKey]) {
                    formNotes[currKey] = formNotes[nextKey];
                } else {
                    delete formNotes[currKey];
                }
            }
            // Bersihkan slot paling akhir
            const lastKey = itemId + '_' + (score - 1);
            delete formPhotos[lastKey];
            delete formNotes[lastKey];

            if (formPhotos[itemId + '_0']) {
                formPhotos[itemId] = formPhotos[itemId + '_0'];
            } else {
                delete formPhotos[itemId];
            }

            renderFindingSlots(itemId);
        }

        function onKpiNoteInput(itemId, val) {
            formNotes[itemId] = val.toUpperCase();
        }

        function onKpiPhotoSelect(itemId, input) {
            onSlotPhotoSelect(itemId, 0, input);
        }

        function removeKpiPhoto(itemId) {
            removeKpiPhotoSlot(itemId, 0);
        }

        function onStdPhotoSelect(key, input) {
            if (!input.files || input.files.length === 0) return;
            const file = input.files[0];
            const reader = new FileReader();
            reader.onload = (e) => {
                stdPhotos[key] = e.target.result;
                renderStdPhotoSlots();
            };
            reader.readAsDataURL(file);
        }

        function removeStdPhoto(key) {
            delete stdPhotos[key];
            renderStdPhotoSlots();
        }

        function updateFormScoreSummary() {
            let total = 0;
            let countFindings = 0;
            for (const k in formScores) {
                const s = formScores[k] || 0;
                total += s;
                if (s > 0) countFindings++;
            }

            const totalEl = document.getElementById('sticky-total-poin');
            const statusEl = document.getElementById('sticky-status-badge');
            const countEl = document.getElementById('sticky-count-temuan');

            if (totalEl) totalEl.textContent = total + ' Poin';
            if (countEl) countEl.textContent = countFindings + ' item';

            if (statusEl) {
                if (total === 0) {
                    statusEl.innerHTML = '✅ STANDAR (LULUS)';
                    statusEl.style.color = '#34d399';
                } else {
                    statusEl.innerHTML = '⚠️ TEMUAN (' + total + ' Poin)';
                    statusEl.style.color = '#f87171';
                }
            }
        }

        function resetAuditForm() {
            if (!confirm('Apakah Anda yakin ingin mereset formulir audit ini?')) return;
            document.getElementById('audit-form').reset();
            formScores = {};
            formNotes = {};
            formPhotos = {};
            stdPhotos = {};
            initAuditForm();
        }

        async function submitAuditForm() {
            const noMobil = (document.getElementById('input-no-mobil').value || '').trim();
            if (!noMobil) {
                alert('Peringatan: Nomor Mobil wajib diisi!');
                document.getElementById('input-no-mobil').focus();
                return;
            }

            const namaPetugas = (document.getElementById('input-nama-petugas').value || '').trim();
            if (!namaPetugas) {
                alert('Peringatan: Nama Petugas / Auditor wajib diisi!');
                document.getElementById('input-nama-petugas').focus();
                return;
            }

            const tanggalVal = document.getElementById('input-tanggal').value;
            if (!tanggalVal) {
                alert('Peringatan: Tanggal Audit wajib diisi!');
                return;
            }

            const periode = document.getElementById('input-periode').value;
            if (!periode) {
                alert('Peringatan: Periode Audit wajib dipilih!');
                return;
            }

            const pool = document.getElementById('input-pool').value;
            if (!pool) {
                alert('Peringatan: Pool Armada wajib dipilih!');
                return;
            }

            // Date to dd/MM/yyyy
            const [y, m, d] = tanggalVal.split('-');
            const formattedTgl = d + '/' + m + '/' + y;

            const jenisSample = document.getElementById('input-jenis-sample').value;
            let tglServiceFmt = '';
            if (jenisSample === 'SERVICE') {
                const ts = document.getElementById('input-tgl-service').value;
                if (ts) {
                    const [sy, sm, sd] = ts.split('-');
                    tglServiceFmt = sd + '/' + sm + '/' + sy;
                }
            }

            const payload = {
                noMobil: noMobil,
                platNomer: document.getElementById('input-plat').value.trim(),
                pool: pool,
                tipe: document.getElementById('input-tipe').value.trim(),
                warna: document.getElementById('input-warna').value.trim(),
                tahun: document.getElementById('input-tahun').value.trim(),
                tanggal: formattedTgl,
                periode: periode,
                nip: document.getElementById('input-nip').value.trim(),
                namaPetugas: namaPetugas,
                jenisSample: jenisSample,
                tanggalService: tglServiceFmt,
                sampleTanggal: tglServiceFmt,
                paketService: jenisSample === 'SERVICE' ? document.getElementById('input-paket-service').value : '',
                kpiScores: formScores,
                kpiNotes: formNotes,
                kpiPhotos: { ...formPhotos, ...stdPhotos }
            };

            const btnTop = document.getElementById('btn-submit-top');
            const btnBottom = document.getElementById('btn-submit-bottom');
            if (btnTop) { btnTop.disabled = true; btnTop.textContent = '⏳ Menyimpan...'; }
            if (btnBottom) { btnBottom.disabled = true; btnBottom.textContent = '⏳ Menyimpan data audit...'; }

            try {
                const res = await fetch('/api/audit', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                const resData = await res.json();
                if (res.ok && resData.success) {
                    alert('✅ Sukses! Data audit armada ' + noMobil + ' berhasil disimpan ke sistem!');
                    // Reset form
                    document.getElementById('audit-form').reset();
                    formScores = {};
                    formNotes = {};
                    formPhotos = {};
                    stdPhotos = {};
                    initAuditForm();

                    // Refresh data from server
                    await fetchData();

                    // Switch to Riwayat tab to view the newly saved audit
                    switchTab('riwayat');
                } else {
                    alert('Gagal menyimpan audit: ' + (resData.message || 'Terjadi kesalahan pada server'));
                }
            } catch (err) {
                console.error('Error submitting audit:', err);
                alert('Gagal mengirim data audit ke server: ' + err.message);
            } finally {
                if (btnTop) { btnTop.disabled = false; btnTop.textContent = '💾 Simpan Audit'; }
                if (btnBottom) { btnBottom.disabled = false; btnBottom.textContent = '💾 Simpan & Kirim Audit'; }
            }
        }

        // Start lifecycle on load
        window.addEventListener('DOMContentLoaded', () => {
            init();
            setupZoomInteractions();
        });
        """.trimIndent()
    }
}
