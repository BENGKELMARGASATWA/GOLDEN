package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBlueDark
import com.example.ui.theme.HighDensityBlueLight
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Teal600
import com.example.util.ImageCompressor
import com.example.util.WatermarkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class MarkShapeMode {
    SELECT,
    FREEHAND,
    CIRCLE,
    RECTANGLE,
    ARROW,
    TEXT
}

enum class ShapeHandle {
    NONE,
    BODY,
    TL,
    TR,
    BL,
    BR,
    ARROW_START,
    ARROW_END
}

data class DrawnStroke(
    val path: Path? = null,
    val shapeMode: MarkShapeMode = MarkShapeMode.FREEHAND,
    val startOffset: Offset = Offset.Zero,
    val endOffset: Offset = Offset.Zero,
    val points: List<Offset> = emptyList(), // Normalized coordinates 0f..1f
    val text: String = "",
    val color: Color,
    val strokeWidth: Float // Normalized stroke relative to min dimension
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoMarkupEditorDialog(
    photoPath: String,
    title: String = "Tandai Kerusakan / Catatan Foto",
    vehicleNumber: String = "",
    auditorName: String = "",
    sample: String = "OPERASIONAL",
    sampleTanggal: String? = null,
    paketService: String? = null,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Watermark State (No Mobil & Auditor yang di-input - permanen & tidak dapat diedit manual)
    val currentVehicleNumber = vehicleNumber.trim()
    val currentAuditorName = auditorName.trim()
    val rawSample = sample.trim().ifBlank { "OPERASIONAL" }
    val currentSample = buildString {
        append(rawSample.uppercase())
        if (rawSample.equals("SERVICE", ignoreCase = true)) {
            val tgl = sampleTanggal?.trim().orEmpty()
            val paket = paketService?.trim().orEmpty()
            if (tgl.isNotBlank()) append(" - [$tgl]")
            if (paket.isNotBlank()) append(" - [${paket.uppercase()}]")
        }
    }
    val showWatermark = currentVehicleNumber.isNotBlank()

    // Drawing Tool States
    var selectedColor by remember { mutableStateOf(Color(0xFFEF4444)) } // Default Bright Red for markings
    var strokeSize by remember { mutableFloatStateOf(6f) } // Pixel stroke on screen
    var shapeMode by remember { mutableStateOf(MarkShapeMode.FREEHAND) }

    val strokes = remember { mutableStateListOf<DrawnStroke>() }

    // Text & Shape Manipulation States
    var showTextInputDialog by remember { mutableStateOf(false) }
    var textToInsert by remember { mutableStateOf("") }
    var currentTextNormSize by remember { mutableFloatStateOf(0.024f) } // Default Medium size
    var targetTextPosition by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var selectedStrokeIndex by remember { mutableStateOf<Int?>(null) }
    var activeHandle by remember { mutableStateOf(ShapeHandle.NONE) }
    var isDraggingStroke by remember { mutableStateOf(false) }
    var editingStrokeIndex by remember { mutableStateOf<Int?>(null) }

    // Temporary active stroke during drag
    var currentPoints = remember { mutableStateListOf<Offset>() }
    var dragStartOffset by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentOffset by remember { mutableStateOf<Offset?>(null) }

    // Load original bitmap on background IO
    LaunchedEffect(photoPath) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                loadedBitmap?.recycle()
                loadedBitmap = null
                val file = File(photoPath)
                if (file.exists()) {
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

                    val maxDim = 1024
                    val h = boundsOptions.outHeight
                    val w = boundsOptions.outWidth
                    var sampleSize = 1
                    if (h > maxDim || w > maxDim) {
                        val maxSide = maxOf(h, w)
                        while ((maxSide / sampleSize) > maxDim) {
                            sampleSize *= 2
                        }
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inMutable = true
                    }
                    var bmp = try {
                        BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    } catch (oom: OutOfMemoryError) {
                        System.gc()
                        decodeOptions.inSampleSize = sampleSize * 2
                        BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    }

                    // EXIF Orientation check
                    val exif = try { ExifInterface(file.absolutePath) } catch (e: Exception) { null }
                    val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: ExifInterface.ORIENTATION_NORMAL
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    if (!matrix.isIdentity && bmp != null) {
                        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                        if (rotated != bmp) {
                            bmp.recycle()
                            bmp = rotated
                        }
                    }
                    loadedBitmap = bmp
                }
            } catch (t: Throwable) {
                if (t is OutOfMemoryError) {
                    System.gc()
                }
                t.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            try {
                loadedBitmap?.recycle()
                loadedBitmap = null
            } catch (_: Exception) {}
            System.gc()
        }
    }

    Dialog(
        onDismissRequest = {
            try {
                loadedBitmap?.recycle()
                loadedBitmap = null
            } catch (_: Exception) {}
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val performSave: () -> Unit = {
            val bmp = loadedBitmap
            if (bmp == null) {
                onDismiss()
            } else if (strokes.isEmpty()) {
                Toast.makeText(context, "Tidak ada coretan/tulisan yang ditambahkan", Toast.LENGTH_SHORT).show()
                onDismiss()
            } else {
                isSaving = true
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val resultBitmap = try {
                            Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
                        } catch (oom: OutOfMemoryError) {
                            System.gc()
                            Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.RGB_565)
                        }
                        val canvas = android.graphics.Canvas(resultBitmap)

                        // Draw original image
                        canvas.drawBitmap(bmp, 0f, 0f, null)

                        val w = bmp.width.toFloat()
                        val h = bmp.height.toFloat()
                        val minDim = minOf(w, h)

                        // Render each stroke on full resolution bitmap
                        strokes.forEach { stroke ->
                            val paint = Paint().apply {
                                color = stroke.color.toArgb()
                                strokeWidth = stroke.strokeWidth * minDim
                                style = Paint.Style.STROKE
                                strokeCap = Paint.Cap.ROUND
                                strokeJoin = Paint.Join.ROUND
                                isAntiAlias = true
                            }

                            when (stroke.shapeMode) {
                                MarkShapeMode.FREEHAND -> {
                                    if (stroke.points.size > 1) {
                                        val path = AndroidPath().apply {
                                            val f = stroke.points.first()
                                            moveTo(f.x * w, f.y * h)
                                            for (i in 1 until stroke.points.size) {
                                                val p = stroke.points[i]
                                                lineTo(p.x * w, p.y * h)
                                            }
                                        }
                                        canvas.drawPath(path, paint)
                                    }
                                }
                                MarkShapeMode.CIRCLE -> {
                                    val start = stroke.startOffset
                                    val end = stroke.endOffset
                                    val left = minOf(start.x, end.x) * w
                                    val top = minOf(start.y, end.y) * h
                                    val right = maxOf(start.x, end.x) * w
                                    val bottom = maxOf(start.y, end.y) * h
                                    canvas.drawOval(RectF(left, top, right, bottom), paint)
                                }
                                MarkShapeMode.RECTANGLE -> {
                                    val start = stroke.startOffset
                                    val end = stroke.endOffset
                                    val left = minOf(start.x, end.x) * w
                                    val top = minOf(start.y, end.y) * h
                                    val right = maxOf(start.x, end.x) * w
                                    val bottom = maxOf(start.y, end.y) * h
                                    canvas.drawRect(RectF(left, top, right, bottom), paint)
                                }
                                MarkShapeMode.ARROW -> {
                                    val start = stroke.startOffset
                                    val end = stroke.endOffset
                                    val sX = start.x * w
                                    val sY = start.y * h
                                    val eX = end.x * w
                                    val eY = end.y * h

                                    canvas.drawLine(sX, sY, eX, eY, paint)
                                    // Arrowhead
                                    val angle = atan2((eY - sY).toDouble(), (eX - sX).toDouble())
                                    val headLength = (stroke.strokeWidth * minDim) * 3.5f
                                    val headAngle = Math.toRadians(30.0)

                                    val x1 = eX - headLength * cos(angle - headAngle).toFloat()
                                    val y1 = eY - headLength * sin(angle - headAngle).toFloat()
                                    val x2 = eX - headLength * cos(angle + headAngle).toFloat()
                                    val y2 = eY - headLength * sin(angle + headAngle).toFloat()

                                    canvas.drawLine(eX, eY, x1, y1, paint)
                                    canvas.drawLine(eX, eY, x2, y2, paint)
                                }
                                MarkShapeMode.TEXT -> {
                                    val text = stroke.text
                                    if (text.isNotBlank()) {
                                        val fontSize = (stroke.strokeWidth * minDim * 3.5f).coerceIn(20f, 360f)
                                        val textPaint = Paint().apply {
                                            color = stroke.color.toArgb()
                                            textSize = fontSize
                                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                            isAntiAlias = true
                                        }

                                        val textBounds = android.graphics.Rect()
                                        textPaint.getTextBounds(text, 0, text.length, textBounds)

                                        val x = stroke.startOffset.x * w
                                        val y = stroke.startOffset.y * h

                                        val padX = fontSize * 0.4f
                                        val padY = fontSize * 0.3f
                                        val bgRect = RectF(
                                            x - padX,
                                            y - textBounds.height() - padY,
                                            x + textBounds.width() + padX,
                                            y + padY
                                        )

                                        val bgPaint = Paint().apply {
                                            color = android.graphics.Color.argb(220, 15, 23, 42) // Solid dark pill background
                                            style = Paint.Style.FILL
                                            isAntiAlias = true
                                        }
                                        val borderPaint = Paint().apply {
                                            color = stroke.color.toArgb()
                                            style = Paint.Style.STROKE
                                            strokeWidth = (stroke.strokeWidth * minDim * 0.35f).coerceIn(2.5f, 12f)
                                            isAntiAlias = true
                                        }

                                        val corner = fontSize * 0.35f
                                        canvas.drawRoundRect(bgRect, corner, corner, bgPaint)
                                        canvas.drawRoundRect(bgRect, corner, corner, borderPaint)
                                        canvas.drawText(text, x, y, textPaint)
                                    }
                                }
                                MarkShapeMode.SELECT -> {}
                            }
                        }

                        // Catatan: Foto yang dimuat sudah memiliki watermark tunggal permanen dari ImageCompressor.
                        // Tidak menggambar ulang watermark di sini agar watermark pada foto tetap 1 dan tidak menumpuk.

                        // Save result bitmap to file
                        val destinationDir = File(context.filesDir, "audit_photos").apply { if (!exists()) mkdirs() }
                        val markedFile = File(destinationDir, "marked_${System.currentTimeMillis()}.jpg")
                        java.io.FileOutputStream(markedFile).use { out ->
                            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                            out.flush()
                        }
                        resultBitmap.recycle()

                        withContext(Dispatchers.Main) {
                            try {
                                loadedBitmap?.recycle()
                                loadedBitmap = null
                            } catch (_: Exception) {}
                            isSaving = false
                            Toast.makeText(context, "Marking foto berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            onSave(markedFile.absolutePath)
                        }
                    } catch (t: Throwable) {
                        if (t is OutOfMemoryError) {
                            System.gc()
                        }
                        withContext(Dispatchers.Main) {
                            isSaving = false
                            Toast.makeText(context, "Gagal menyimpan foto bertanda: ${t.localizedMessage ?: "Memori penuh"}", Toast.LENGTH_LONG).show()
                        }
                        t.printStackTrace()
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .safeDrawingPadding(),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Top Header Row with Title and Quick Save Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Editor Mark Foto",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (currentVehicleNumber.isNotBlank()) {
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFF38BDF8))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsCar,
                                                contentDescription = null,
                                                tint = Color(0xFFFACC15),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = currentVehicleNumber.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFFFACC15),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        }
                                    }
                                }
                                if (currentAuditorName.isNotBlank()) {
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFF94A3B8))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = currentAuditorName.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, Color(0xFF4ADE80))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = currentSample.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF4ADE80),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Top Bar Save Button (Always visible on any screen)
                    Button(
                        onClick = performSave,
                        enabled = !isSaving && loadedBitmap != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HighDensityBlue,
                            disabledContainerColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Simpan",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Toolbar Controls: Colors, Stroke Size, Shapes, Undo, Reset (Adaptive wrapping for all screen sizes)
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Row 1: Shapes (Pilih, Pena, Teks, Lingkar, Kotak, Panah) & Undo (Batal) & Reset in adaptive FlowRow
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Tool: Select / Move (Pilih & Geser)
                            ToolButton(
                                icon = Icons.Default.OpenWith,
                                label = "Pilih",
                                isSelected = shapeMode == MarkShapeMode.SELECT,
                                onClick = {
                                    shapeMode = MarkShapeMode.SELECT
                                }
                            )
                            // Tool: Freehand Pen
                            ToolButton(
                                icon = Icons.Default.Brush,
                                label = "Pena",
                                isSelected = shapeMode == MarkShapeMode.FREEHAND,
                                onClick = {
                                    shapeMode = MarkShapeMode.FREEHAND
                                    selectedStrokeIndex = null
                                }
                            )
                            // Tool: Text / Tulisan
                            ToolButton(
                                icon = Icons.Default.TextFields,
                                label = "Teks",
                                isSelected = shapeMode == MarkShapeMode.TEXT,
                                onClick = {
                                    shapeMode = MarkShapeMode.TEXT
                                    editingStrokeIndex = null
                                    textToInsert = ""
                                    targetTextPosition = Offset(0.5f, 0.5f)
                                    showTextInputDialog = true
                                }
                            )
                            // Tool: Circle / Lingkaran
                            ToolButton(
                                icon = Icons.Default.Circle,
                                label = "Lingkaran",
                                isSelected = shapeMode == MarkShapeMode.CIRCLE,
                                onClick = {
                                    shapeMode = MarkShapeMode.CIRCLE
                                    selectedStrokeIndex = null
                                }
                            )
                            // Tool: Rectangle
                            ToolButton(
                                icon = Icons.Default.CropSquare,
                                label = "Kotak",
                                isSelected = shapeMode == MarkShapeMode.RECTANGLE,
                                onClick = {
                                    shapeMode = MarkShapeMode.RECTANGLE
                                    selectedStrokeIndex = null
                                }
                            )
                            // Tool: Arrow
                            ToolButton(
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                label = "Panah",
                                isSelected = shapeMode == MarkShapeMode.ARROW,
                                onClick = {
                                    shapeMode = MarkShapeMode.ARROW
                                    selectedStrokeIndex = null
                                }
                            )

                            // Tool: Undo (Batal)
                            ToolButton(
                                icon = Icons.AutoMirrored.Filled.Undo,
                                label = "Batal",
                                isSelected = false,
                                enabled = strokes.isNotEmpty(),
                                onClick = {
                                    if (strokes.isNotEmpty()) {
                                        strokes.removeAt(strokes.lastIndex)
                                        if (selectedStrokeIndex != null && selectedStrokeIndex !in strokes.indices) {
                                            selectedStrokeIndex = null
                                        }
                                    }
                                }
                            )

                            // Tool: Clear All (Reset)
                            ToolButton(
                                icon = Icons.Default.DeleteSweep,
                                label = "Reset",
                                isSelected = false,
                                enabled = strokes.isNotEmpty(),
                                onClick = {
                                    strokes.clear()
                                    selectedStrokeIndex = null
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: Color Palette & Dynamic Size Controls in adaptive FlowRow
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Colors: Red, Yellow, Green, Cyan, White, Black
                            val colors = listOf(
                                Color(0xFFEF4444), // Red (Marking)
                                Color(0xFFFACC15), // Yellow
                                Color(0xFF22C55E), // Green
                                Color(0xFF06B6D4), // Cyan
                                Color(0xFFFFFFFF), // White
                                Color(0xFF000000)  // Black
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                colors.forEach { color ->
                                    val isSelected = selectedColor == color
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color(0xFF64748B),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                selectedColor = color
                                                // If any stroke is selected, update its color too
                                                if (selectedStrokeIndex != null && selectedStrokeIndex in strokes.indices) {
                                                    val cur = strokes[selectedStrokeIndex!!]
                                                    strokes[selectedStrokeIndex!!] = cur.copy(color = color)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (color == Color(0xFFFFFFFF) || color == Color(0xFFFACC15)) Color.Black else Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Dynamic Right Controls: If Teks is selected, show Text Sizing; otherwise show Stroke Thickness
                            if (shapeMode == MarkShapeMode.TEXT || (selectedStrokeIndex != null && selectedStrokeIndex in strokes.indices && strokes[selectedStrokeIndex!!].shapeMode == MarkShapeMode.TEXT)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ukuran:",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 10.5.sp)
                                    )
                                    val textSizes = listOf(
                                        Pair("S", 0.015f),
                                        Pair("M", 0.024f),
                                        Pair("L", 0.038f),
                                        Pair("XL", 0.056f)
                                    )
                                    textSizes.forEach { (lbl, normVal) ->
                                        ThicknessButton(
                                            size = normVal,
                                            label = lbl,
                                            isSelected = kotlin.math.abs(currentTextNormSize - normVal) < 0.005f
                                        ) {
                                            currentTextNormSize = normVal
                                            if (selectedStrokeIndex != null && selectedStrokeIndex in strokes.indices) {
                                                val cur = strokes[selectedStrokeIndex!!]
                                                if (cur.shapeMode == MarkShapeMode.TEXT) {
                                                    strokes[selectedStrokeIndex!!] = cur.copy(strokeWidth = normVal)
                                                }
                                            }
                                        }
                                    }

                                    // Stepper Perkecil Teks (A−)
                                    ThicknessButton(
                                        size = 0f,
                                        label = "A−",
                                        isSelected = false
                                    ) {
                                        val newSize = (currentTextNormSize * 0.82f).coerceIn(0.010f, 0.075f)
                                        currentTextNormSize = newSize
                                        if (selectedStrokeIndex != null && selectedStrokeIndex in strokes.indices) {
                                            val cur = strokes[selectedStrokeIndex!!]
                                            if (cur.shapeMode == MarkShapeMode.TEXT) {
                                                strokes[selectedStrokeIndex!!] = cur.copy(strokeWidth = newSize)
                                            }
                                        }
                                    }

                                    // Stepper Perbesar Teks (A+)
                                    ThicknessButton(
                                        size = 0f,
                                        label = "A+",
                                        isSelected = false
                                    ) {
                                        val newSize = (currentTextNormSize * 1.22f).coerceIn(0.010f, 0.075f)
                                        currentTextNormSize = newSize
                                        if (selectedStrokeIndex != null && selectedStrokeIndex in strokes.indices) {
                                            val cur = strokes[selectedStrokeIndex!!]
                                            if (cur.shapeMode == MarkShapeMode.TEXT) {
                                                strokes[selectedStrokeIndex!!] = cur.copy(strokeWidth = newSize)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ThicknessButton(size = 4f, label = "Tipis", isSelected = strokeSize == 4f) {
                                        strokeSize = 4f
                                        if (selectedStrokeIndex != null && selectedStrokeIndex in strokes.indices) {
                                            val cur = strokes[selectedStrokeIndex!!]
                                            if (cur.shapeMode != MarkShapeMode.TEXT) {
                                                strokes[selectedStrokeIndex!!] = cur.copy(strokeWidth = 0.005f)
                                            }
                                        }
                                    }
                                    ThicknessButton(size = 7f, label = "Sedang", isSelected = strokeSize == 7f) {
                                        strokeSize = 7f
                                        if (selectedStrokeIndex != null && selectedStrokeIndex in strokes.indices) {
                                            val cur = strokes[selectedStrokeIndex!!]
                                            if (cur.shapeMode != MarkShapeMode.TEXT) {
                                                strokes[selectedStrokeIndex!!] = cur.copy(strokeWidth = 0.009f)
                                            }
                                        }
                                    }
                                    ThicknessButton(size = 12f, label = "Tebal", isSelected = strokeSize == 12f) {
                                        strokeSize = 12f
                                        if (selectedStrokeIndex != null && selectedStrokeIndex in strokes.indices) {
                                            val cur = strokes[selectedStrokeIndex!!]
                                            if (cur.shapeMode != MarkShapeMode.TEXT) {
                                                strokes[selectedStrokeIndex!!] = cur.copy(strokeWidth = 0.016f)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Text & Shape Banner: Guidance, Position, and Size Controls
                if (selectedStrokeIndex != null && selectedStrokeIndex in strokes.indices) {
                    val activeStroke = strokes[selectedStrokeIndex!!]
                    if (activeStroke.shapeMode == MarkShapeMode.TEXT) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                // Line 1: Header (Label, Drag hint, Edit, Delete, Close)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenWith,
                                            contentDescription = "Geser",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "Teks: \"${activeStroke.text}\"",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Sentuh & geser tulisan di foto untuk pindah posisi",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 9.5.sp
                                                )
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Edit text button
                                        Surface(
                                            color = Color(0xFF334155),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.clickable {
                                                editingStrokeIndex = selectedStrokeIndex
                                                textToInsert = activeStroke.text
                                                currentTextNormSize = activeStroke.strokeWidth
                                                selectedColor = activeStroke.color
                                                showTextInputDialog = true
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Ubah",
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "Ubah",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF38BDF8),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp
                                                    )
                                                )
                                            }
                                        }

                                        // Delete text button
                                        IconButton(
                                            onClick = {
                                                strokes.removeAt(selectedStrokeIndex!!)
                                                selectedStrokeIndex = null
                                            },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Teks",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        // Deselect button
                                        IconButton(
                                            onClick = { selectedStrokeIndex = null },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Selesai",
                                                tint = Slate400,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = Color(0xFF1E293B))
                                Spacer(modifier = Modifier.height(6.dp))

                                // Line 2: Size Controls (Perbesar & Perkecil Teks)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Presets: S, M, L, XL
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Ukuran:",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF94A3B8),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        )

                                        val sizes = listOf(
                                            Pair("S", 0.015f),
                                            Pair("M", 0.024f),
                                            Pair("L", 0.038f),
                                            Pair("XL", 0.056f)
                                        )
                                        sizes.forEach { (label, normVal) ->
                                            val isCurrent = kotlin.math.abs(activeStroke.strokeWidth - normVal) < 0.005f
                                            Surface(
                                                color = if (isCurrent) HighDensityBlue else Color(0xFF1E293B),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, if (isCurrent) Color(0xFF38BDF8) else Color(0xFF334155)),
                                                modifier = Modifier.clickable {
                                                    val updated = activeStroke.copy(strokeWidth = normVal)
                                                    strokes[selectedStrokeIndex!!] = updated
                                                    currentTextNormSize = normVal
                                                }
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isCurrent) Color.White else Color(0xFFCBD5E1),
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 10.5.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Steppers: A− (Kecilkan) and A+ (Besarkan)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // A- (Perkecil)
                                        Surface(
                                            color = Color(0xFF1E293B),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.clickable {
                                                val newSize = (activeStroke.strokeWidth * 0.82f).coerceIn(0.010f, 0.075f)
                                                strokes[selectedStrokeIndex!!] = activeStroke.copy(strokeWidth = newSize)
                                                currentTextNormSize = newSize
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "A−",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Kecilkan",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 9.5.sp
                                                    )
                                                )
                                            }
                                        }

                                        // A+ (Perbesar)
                                        Surface(
                                            color = Color(0xFF1E293B),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.clickable {
                                                val newSize = (activeStroke.strokeWidth * 1.22f).coerceIn(0.010f, 0.075f)
                                                strokes[selectedStrokeIndex!!] = activeStroke.copy(strokeWidth = newSize)
                                                currentTextNormSize = newSize
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "A+",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Besarkan",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 9.5.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (activeStroke.shapeMode in listOf(MarkShapeMode.CIRCLE, MarkShapeMode.RECTANGLE, MarkShapeMode.ARROW)) {
                        val shapeLabel = when (activeStroke.shapeMode) {
                            MarkShapeMode.CIRCLE -> "Lingkaran"
                            MarkShapeMode.RECTANGLE -> "Kotak"
                            MarkShapeMode.ARROW -> "Panah"
                            else -> "Bentuk"
                        }
                        val shapeIcon = when (activeStroke.shapeMode) {
                            MarkShapeMode.CIRCLE -> Icons.Default.Circle
                            MarkShapeMode.RECTANGLE -> Icons.Default.CropSquare
                            MarkShapeMode.ARROW -> Icons.AutoMirrored.Filled.ArrowForward
                            else -> Icons.Default.OpenWith
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                // Header: Shape Info, Drag/Resize tips, Delete, Close
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = shapeIcon,
                                            contentDescription = shapeLabel,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "Mark $shapeLabel Terpilih",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp
                                                )
                                            )
                                            Text(
                                                text = "Tarik titik sudut untuk perbesar/kecil • Geser badan untuk pindah",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 9.5.sp
                                                )
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Delete shape button
                                        IconButton(
                                            onClick = {
                                                strokes.removeAt(selectedStrokeIndex!!)
                                                selectedStrokeIndex = null
                                            },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Mark",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        // Deselect button
                                        IconButton(
                                            onClick = { selectedStrokeIndex = null },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Selesai",
                                                tint = Slate400,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = Color(0xFF1E293B))
                                Spacer(modifier = Modifier.height(6.dp))

                                // Controls: Manual Scale Steppers (Kecilkan / Besarkan) & Thickness quick presets
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Manual Steppers
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Ukuran:",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF94A3B8),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        )

                                        // Perkecil button
                                        Surface(
                                            color = Color(0xFF1E293B),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.clickable {
                                                strokes[selectedStrokeIndex!!] = scaleShape(activeStroke, 0.85f)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "−",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Kecilkan",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFFCBD5E1),
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }

                                        // Besarkan button
                                        Surface(
                                            color = Color(0xFF1E293B),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.clickable {
                                                strokes[selectedStrokeIndex!!] = scaleShape(activeStroke, 1.18f)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "+",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Besarkan",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFFCBD5E1),
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Thickness Presets for Selected Shape
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Tebal:",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.5.sp
                                            )
                                        )
                                        ThicknessButton(size = 0.005f, label = "Tipis", isSelected = kotlin.math.abs(activeStroke.strokeWidth - 0.005f) < 0.002f) {
                                            strokes[selectedStrokeIndex!!] = activeStroke.copy(strokeWidth = 0.005f)
                                        }
                                        ThicknessButton(size = 0.009f, label = "Sedang", isSelected = kotlin.math.abs(activeStroke.strokeWidth - 0.009f) < 0.002f) {
                                            strokes[selectedStrokeIndex!!] = activeStroke.copy(strokeWidth = 0.009f)
                                        }
                                        ThicknessButton(size = 0.016f, label = "Tebal", isSelected = kotlin.math.abs(activeStroke.strokeWidth - 0.016f) < 0.003f) {
                                            strokes[selectedStrokeIndex!!] = activeStroke.copy(strokeWidth = 0.016f)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Canvas Area (Photo Drawing Area)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF000000))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Memuat foto...", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (loadedBitmap != null) {
                        val bmp = loadedBitmap!!
                        val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
                        val aspectRatio = bmp.width.toFloat() / bmp.height.toFloat()

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Calculate display width and height maintaining aspect ratio
                            val containerWidth = constraints.maxWidth.toFloat()
                            val containerHeight = constraints.maxHeight.toFloat()
                            val containerAspect = containerWidth / containerHeight

                            val canvasWidth: Float
                            val canvasHeight: Float
                            if (aspectRatio > containerAspect) {
                                canvasWidth = containerWidth
                                canvasHeight = containerWidth / aspectRatio
                            } else {
                                canvasHeight = containerHeight
                                canvasWidth = containerHeight * aspectRatio
                            }

                            // Interactive Canvas
                            Canvas(
                                modifier = Modifier
                                    .size(
                                        width = (canvasWidth / (context.resources.displayMetrics.density)).dp,
                                        height = (canvasHeight / (context.resources.displayMetrics.density)).dp
                                    )
                                    .clipToBounds()
                                    .pointerInput(shapeMode, selectedColor, strokeSize, strokes.size, selectedStrokeIndex) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val cW = size.width.toFloat()
                                                val cH = size.height.toFloat()

                                                // 1. If a stroke is already selected, check if touching its handles first
                                                if (selectedStrokeIndex != null && selectedStrokeIndex in strokes.indices) {
                                                    val handle = hitTestSelectedHandle(
                                                        touchOffset = offset,
                                                        stroke = strokes[selectedStrokeIndex!!],
                                                        canvasWidth = cW,
                                                        canvasHeight = cH
                                                    )
                                                    if (handle != ShapeHandle.NONE) {
                                                        activeHandle = handle
                                                        isDraggingStroke = true
                                                        dragStartOffset = null
                                                        dragCurrentOffset = null
                                                        currentPoints.clear()
                                                        return@detectDragGestures
                                                    }
                                                }

                                                // 2. Otherwise, check if touching any stroke on canvas
                                                val hitIndex = findHitStrokeIndex(
                                                    touchOffset = offset,
                                                    canvasWidth = cW,
                                                    canvasHeight = cH,
                                                    strokes = strokes
                                                )

                                                if (hitIndex != null) {
                                                    selectedStrokeIndex = hitIndex
                                                    activeHandle = ShapeHandle.BODY
                                                    isDraggingStroke = true
                                                    dragStartOffset = null
                                                    dragCurrentOffset = null
                                                    currentPoints.clear()
                                                } else {
                                                    // Touched empty space
                                                    isDraggingStroke = false
                                                    activeHandle = ShapeHandle.NONE
                                                    if (shapeMode == MarkShapeMode.SELECT) {
                                                        selectedStrokeIndex = null
                                                        return@detectDragGestures
                                                    }
                                                    if (shapeMode != MarkShapeMode.TEXT) {
                                                        selectedStrokeIndex = null
                                                    }

                                                    val normX = (offset.x / size.width).coerceIn(0f, 1f)
                                                    val normY = (offset.y / size.height).coerceIn(0f, 1f)
                                                    val normOffset = Offset(normX, normY)

                                                    dragStartOffset = normOffset
                                                    dragCurrentOffset = normOffset
                                                    currentPoints.clear()
                                                    currentPoints.add(normOffset)
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val cW = size.width.toFloat()
                                                val cH = size.height.toFloat()

                                                if (isDraggingStroke && selectedStrokeIndex != null && selectedStrokeIndex in strokes.indices) {
                                                    val idx = selectedStrokeIndex!!
                                                    val cur = strokes[idx]
                                                    val dxNorm = dragAmount.x / cW
                                                    val dyNorm = dragAmount.y / cH

                                                    when (activeHandle) {
                                                        ShapeHandle.BODY -> {
                                                            when (cur.shapeMode) {
                                                                MarkShapeMode.TEXT -> {
                                                                    val newX = (cur.startOffset.x + dxNorm).coerceIn(0.02f, 0.98f)
                                                                    val newY = (cur.startOffset.y + dyNorm).coerceIn(0.04f, 0.98f)
                                                                    strokes[idx] = cur.copy(startOffset = Offset(newX, newY))
                                                                }
                                                                MarkShapeMode.CIRCLE, MarkShapeMode.RECTANGLE, MarkShapeMode.ARROW -> {
                                                                    val newStartX = (cur.startOffset.x + dxNorm).coerceIn(0f, 1f)
                                                                    val newStartY = (cur.startOffset.y + dyNorm).coerceIn(0f, 1f)
                                                                    val newEndX = (cur.endOffset.x + dxNorm).coerceIn(0f, 1f)
                                                                    val newEndY = (cur.endOffset.y + dyNorm).coerceIn(0f, 1f)
                                                                    strokes[idx] = cur.copy(
                                                                        startOffset = Offset(newStartX, newStartY),
                                                                        endOffset = Offset(newEndX, newEndY)
                                                                    )
                                                                }
                                                                MarkShapeMode.FREEHAND -> {
                                                                    val moved = cur.points.map { pt ->
                                                                        Offset((pt.x + dxNorm).coerceIn(0f, 1f), (pt.y + dyNorm).coerceIn(0f, 1f))
                                                                    }
                                                                    strokes[idx] = cur.copy(points = moved)
                                                                }
                                                                else -> {}
                                                            }
                                                        }
                                                        ShapeHandle.TL -> {
                                                            val l = minOf(cur.startOffset.x, cur.endOffset.x) + dxNorm
                                                            val t = minOf(cur.startOffset.y, cur.endOffset.y) + dyNorm
                                                            val r = maxOf(cur.startOffset.x, cur.endOffset.x)
                                                            val b = maxOf(cur.startOffset.y, cur.endOffset.y)
                                                            strokes[idx] = cur.copy(
                                                                startOffset = Offset(minOf(l, r - 0.02f).coerceIn(0f, 1f), minOf(t, b - 0.02f).coerceIn(0f, 1f)),
                                                                endOffset = Offset(r.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
                                                            )
                                                        }
                                                        ShapeHandle.TR -> {
                                                            val l = minOf(cur.startOffset.x, cur.endOffset.x)
                                                            val t = minOf(cur.startOffset.y, cur.endOffset.y) + dyNorm
                                                            val r = maxOf(cur.startOffset.x, cur.endOffset.x) + dxNorm
                                                            val b = maxOf(cur.startOffset.y, cur.endOffset.y)
                                                            strokes[idx] = cur.copy(
                                                                startOffset = Offset(l.coerceIn(0f, 1f), minOf(t, b - 0.02f).coerceIn(0f, 1f)),
                                                                endOffset = Offset(maxOf(r, l + 0.02f).coerceIn(0f, 1f), b.coerceIn(0f, 1f))
                                                            )
                                                        }
                                                        ShapeHandle.BL -> {
                                                            val l = minOf(cur.startOffset.x, cur.endOffset.x) + dxNorm
                                                            val t = minOf(cur.startOffset.y, cur.endOffset.y)
                                                            val r = maxOf(cur.startOffset.x, cur.endOffset.x)
                                                            val b = maxOf(cur.startOffset.y, cur.endOffset.y) + dyNorm
                                                            strokes[idx] = cur.copy(
                                                                startOffset = Offset(minOf(l, r - 0.02f).coerceIn(0f, 1f), t.coerceIn(0f, 1f)),
                                                                endOffset = Offset(r.coerceIn(0f, 1f), maxOf(b, t + 0.02f).coerceIn(0f, 1f))
                                                            )
                                                        }
                                                        ShapeHandle.BR -> {
                                                            val l = minOf(cur.startOffset.x, cur.endOffset.x)
                                                            val t = minOf(cur.startOffset.y, cur.endOffset.y)
                                                            val r = maxOf(cur.startOffset.x, cur.endOffset.x) + dxNorm
                                                            val b = maxOf(cur.startOffset.y, cur.endOffset.y) + dyNorm
                                                            strokes[idx] = cur.copy(
                                                                startOffset = Offset(l.coerceIn(0f, 1f), t.coerceIn(0f, 1f)),
                                                                endOffset = Offset(maxOf(r, l + 0.02f).coerceIn(0f, 1f), maxOf(b, t + 0.02f).coerceIn(0f, 1f))
                                                            )
                                                        }
                                                        ShapeHandle.ARROW_START -> {
                                                            val newStartX = (cur.startOffset.x + dxNorm).coerceIn(0f, 1f)
                                                            val newStartY = (cur.startOffset.y + dyNorm).coerceIn(0f, 1f)
                                                            strokes[idx] = cur.copy(startOffset = Offset(newStartX, newStartY))
                                                        }
                                                        ShapeHandle.ARROW_END -> {
                                                            val newEndX = (cur.endOffset.x + dxNorm).coerceIn(0f, 1f)
                                                            val newEndY = (cur.endOffset.y + dyNorm).coerceIn(0f, 1f)
                                                            strokes[idx] = cur.copy(endOffset = Offset(newEndX, newEndY))
                                                        }
                                                        else -> {}
                                                    }
                                                } else if (shapeMode != MarkShapeMode.SELECT) {
                                                    val newOffset = change.position
                                                    val normX = (newOffset.x / size.width).coerceIn(0f, 1f)
                                                    val normY = (newOffset.y / size.height).coerceIn(0f, 1f)
                                                    val normOffset = Offset(normX, normY)

                                                    dragCurrentOffset = normOffset
                                                    if (shapeMode == MarkShapeMode.FREEHAND) {
                                                        currentPoints.add(normOffset)
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                if (isDraggingStroke) {
                                                    isDraggingStroke = false
                                                    activeHandle = ShapeHandle.NONE
                                                } else if (shapeMode != MarkShapeMode.SELECT) {
                                                    val start = dragStartOffset
                                                    val end = dragCurrentOffset
                                                    val minDim = minOf(size.width, size.height)
                                                    val normStroke = strokeSize / minDim

                                                    if (start != null) {
                                                        if (shapeMode == MarkShapeMode.TEXT) {
                                                            targetTextPosition = start
                                                            editingStrokeIndex = null
                                                            textToInsert = ""
                                                            showTextInputDialog = true
                                                        } else if (end != null) {
                                                            when (shapeMode) {
                                                                MarkShapeMode.FREEHAND -> {
                                                                    if (currentPoints.isNotEmpty()) {
                                                                        strokes.add(
                                                                            DrawnStroke(
                                                                                shapeMode = MarkShapeMode.FREEHAND,
                                                                                points = currentPoints.toList(),
                                                                                color = selectedColor,
                                                                                strokeWidth = normStroke
                                                                            )
                                                                        )
                                                                        selectedStrokeIndex = strokes.lastIndex
                                                                    }
                                                                }
                                                                MarkShapeMode.CIRCLE, MarkShapeMode.RECTANGLE, MarkShapeMode.ARROW -> {
                                                                    strokes.add(
                                                                        DrawnStroke(
                                                                            shapeMode = shapeMode,
                                                                            startOffset = start,
                                                                            endOffset = end,
                                                                            color = selectedColor,
                                                                            strokeWidth = normStroke
                                                                        )
                                                                    )
                                                                    selectedStrokeIndex = strokes.lastIndex
                                                                }
                                                                MarkShapeMode.SELECT, MarkShapeMode.TEXT -> {}
                                                            }
                                                        }
                                                    }
                                                    currentPoints.clear()
                                                    dragStartOffset = null
                                                    dragCurrentOffset = null
                                                }
                                            },
                                            onDragCancel = {
                                                isDraggingStroke = false
                                                activeHandle = ShapeHandle.NONE
                                                currentPoints.clear()
                                                dragStartOffset = null
                                                dragCurrentOffset = null
                                            }
                                        )
                                    }
                            ) {
                                // 1. Draw background photo
                                drawImage(
                                    image = imageBitmap,
                                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                                )

                                val minDim = minOf(size.width, size.height)

                                // 2. Draw completed strokes with selection highlighting for active shapes & text
                                strokes.forEachIndexed { index, stroke ->
                                    val pxStroke = stroke.strokeWidth * minDim
                                    val isSelected = (selectedStrokeIndex == index)
                                    drawCustomShape(stroke, size, pxStroke, isSelected)
                                }

                                // 3. Draw active stroke in progress
                                val activeStart = dragStartOffset
                                val activeEnd = dragCurrentOffset
                                if (activeStart != null && activeEnd != null) {
                                    val activeStrokePx = strokeSize
                                    when (shapeMode) {
                                        MarkShapeMode.FREEHAND -> {
                                            if (currentPoints.size > 1) {
                                                val path = Path().apply {
                                                    val first = currentPoints.first()
                                                    moveTo(first.x * size.width, first.y * size.height)
                                                    for (i in 1 until currentPoints.size) {
                                                        val pt = currentPoints[i]
                                                        lineTo(pt.x * size.width, pt.y * size.height)
                                                    }
                                                }
                                                drawPath(
                                                    path = path,
                                                    color = selectedColor,
                                                    style = Stroke(
                                                        width = activeStrokePx,
                                                        cap = StrokeCap.Round,
                                                        join = StrokeJoin.Round
                                                    )
                                                )
                                            }
                                        }
                                        MarkShapeMode.CIRCLE -> {
                                            val startPx = Offset(activeStart.x * size.width, activeStart.y * size.height)
                                            val endPx = Offset(activeEnd.x * size.width, activeEnd.y * size.height)
                                            val left = minOf(startPx.x, endPx.x)
                                            val top = minOf(startPx.y, endPx.y)
                                            val right = maxOf(startPx.x, endPx.x)
                                            val bottom = maxOf(startPx.y, endPx.y)

                                            drawOval(
                                                color = selectedColor,
                                                topLeft = Offset(left, top),
                                                size = Size(maxOf(1f, right - left), maxOf(1f, bottom - top)),
                                                style = Stroke(width = activeStrokePx, cap = StrokeCap.Round)
                                            )
                                        }
                                        MarkShapeMode.RECTANGLE -> {
                                            val startPx = Offset(activeStart.x * size.width, activeStart.y * size.height)
                                            val endPx = Offset(activeEnd.x * size.width, activeEnd.y * size.height)
                                            val left = minOf(startPx.x, endPx.x)
                                            val top = minOf(startPx.y, endPx.y)
                                            val right = maxOf(startPx.x, endPx.x)
                                            val bottom = maxOf(startPx.y, endPx.y)

                                            drawRect(
                                                color = selectedColor,
                                                topLeft = Offset(left, top),
                                                size = Size(maxOf(1f, right - left), maxOf(1f, bottom - top)),
                                                style = Stroke(width = activeStrokePx, cap = StrokeCap.Round)
                                            )
                                        }
                                        MarkShapeMode.ARROW -> {
                                            val startPx = Offset(activeStart.x * size.width, activeStart.y * size.height)
                                            val endPx = Offset(activeEnd.x * size.width, activeEnd.y * size.height)
                                            drawArrow(startPx, endPx, selectedColor, activeStrokePx)
                                        }
                                        MarkShapeMode.TEXT -> {
                                            // Tap location indicator for text insertion
                                            val tapPx = Offset(activeStart.x * size.width, activeStart.y * size.height)
                                            drawCircle(
                                                color = selectedColor,
                                                radius = 12f,
                                                center = tapPx,
                                                style = Stroke(width = 3f)
                                            )
                                        }
                                        MarkShapeMode.SELECT -> {}
                                    }
                                }

                                // Catatan: Foto sudah memiliki watermark tunggal dari penyimpanan awal sehingga tidak digambar ulang agar tidak menumpuk.
                            }
                        }
                    } else {
                        Text("Gagal memuat foto", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Action Buttons: Batal & Simpan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Batal",
                            color = Color(0xFFCBD5E1),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        )
                    }

                    Button(
                        onClick = performSave,
                        enabled = !isSaving && loadedBitmap != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HighDensityBlue,
                            disabledContainerColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Simpan Mark Foto",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Popup Input Dialog for Adding Text Annotation
        if (showTextInputDialog) {
            Dialog(
                onDismissRequest = { showTextInputDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                val presets = listOf("BARET", "PENYOK", "RETAK", "PECAH", "KOTOR", "BOCOR", "LECET", "KARAT", "SOBEK", "OK")

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TextFields, contentDescription = null, tint = HighDensityBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (editingStrokeIndex != null) "Ubah Tulisan pada Foto" else "Beri Tulisan pada Foto",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingStrokeIndex = null
                                    showTextInputDialog = false
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400, modifier = Modifier.size(18.dp))
                            }
                        }

                        Text(
                            text = "Pilih label cepat atau ketik keterangan:",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 11.5.sp)
                        )

                        // Quick preset chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(presets) { preset ->
                                Surface(
                                    color = if (textToInsert == preset) HighDensityBlue else Color(0xFF334155),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (textToInsert == preset) Color.White else Color(0xFF475569)),
                                    modifier = Modifier.clickable { textToInsert = preset }
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = textToInsert,
                            onValueChange = { textToInsert = it.uppercase() },
                            placeholder = { Text("Ketik tulisan/catatan...", color = Slate400, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = HighDensityBlue,
                                unfocusedBorderColor = Color(0xFF475569),
                                cursorColor = HighDensityBlue
                            )
                        )

                        // Pilihan Ukuran Tulisan di Dialog
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ukuran Tulisan:",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 11.5.sp)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val sizeChoices = listOf(
                                    Triple("Kecil (S)", 0.015f, "S"),
                                    Triple("Sedang (M)", 0.024f, "M"),
                                    Triple("Besar (L)", 0.038f, "L"),
                                    Triple("Ekstra (XL)", 0.056f, "XL")
                                )
                                sizeChoices.forEach { (label, normVal, shortKey) ->
                                    val isChosen = kotlin.math.abs(currentTextNormSize - normVal) < 0.005f
                                    Surface(
                                        color = if (isChosen) HighDensityBlue else Color(0xFF334155),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isChosen) Color(0xFF38BDF8) else Color(0xFF475569)),
                                        modifier = Modifier.clickable { currentTextNormSize = normVal }
                                    ) {
                                        Text(
                                            text = shortKey,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Tip: Tulisan dapat langsung digeser di foto ke posisi yang diinginkan.",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editingStrokeIndex = null
                                    showTextInputDialog = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569)),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text("Batal", color = Slate400, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (textToInsert.isNotBlank()) {
                                        if (editingStrokeIndex != null && editingStrokeIndex!! in strokes.indices) {
                                            val existing = strokes[editingStrokeIndex!!]
                                            strokes[editingStrokeIndex!!] = existing.copy(
                                                text = textToInsert.trim(),
                                                color = selectedColor,
                                                strokeWidth = currentTextNormSize
                                            )
                                            selectedStrokeIndex = editingStrokeIndex
                                        } else {
                                            strokes.add(
                                                DrawnStroke(
                                                    shapeMode = MarkShapeMode.TEXT,
                                                    startOffset = targetTextPosition,
                                                    text = textToInsert.trim(),
                                                    color = selectedColor,
                                                    strokeWidth = currentTextNormSize
                                                )
                                            )
                                            selectedStrokeIndex = strokes.lastIndex
                                        }
                                        editingStrokeIndex = null
                                        textToInsert = ""
                                        showTextInputDialog = false
                                    }
                                },
                                enabled = textToInsert.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HighDensityBlue,
                                    disabledContainerColor = Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.3f).height(40.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (editingStrokeIndex != null) "Simpan Perubahan" else "Pasang Teks",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun scaleShape(stroke: DrawnStroke, factor: Float): DrawnStroke {
    return when (stroke.shapeMode) {
        MarkShapeMode.CIRCLE, MarkShapeMode.RECTANGLE -> {
            val cx = (stroke.startOffset.x + stroke.endOffset.x) / 2f
            val cy = (stroke.startOffset.y + stroke.endOffset.y) / 2f
            val halfW = (kotlin.math.abs(stroke.endOffset.x - stroke.startOffset.x) / 2f) * factor
            val halfH = (kotlin.math.abs(stroke.endOffset.y - stroke.startOffset.y) / 2f) * factor
            val clampedHalfW = halfW.coerceIn(0.015f, 0.48f)
            val clampedHalfH = halfH.coerceIn(0.015f, 0.48f)

            val left = (cx - clampedHalfW).coerceIn(0.01f, 0.99f)
            val top = (cy - clampedHalfH).coerceIn(0.01f, 0.99f)
            val right = (cx + clampedHalfW).coerceIn(0.01f, 0.99f)
            val bottom = (cy + clampedHalfH).coerceIn(0.01f, 0.99f)

            stroke.copy(
                startOffset = Offset(left, top),
                endOffset = Offset(right, bottom)
            )
        }
        MarkShapeMode.ARROW -> {
            val cx = (stroke.startOffset.x + stroke.endOffset.x) / 2f
            val cy = (stroke.startOffset.y + stroke.endOffset.y) / 2f
            val dx = (stroke.endOffset.x - stroke.startOffset.x) * factor
            val dy = (stroke.endOffset.y - stroke.startOffset.y) * factor
            val newStartX = (cx - dx / 2f).coerceIn(0.01f, 0.99f)
            val newStartY = (cy - dy / 2f).coerceIn(0.01f, 0.99f)
            val newEndX = (cx + dx / 2f).coerceIn(0.01f, 0.99f)
            val newEndY = (cy + dy / 2f).coerceIn(0.01f, 0.99f)

            stroke.copy(
                startOffset = Offset(newStartX, newStartY),
                endOffset = Offset(newEndX, newEndY)
            )
        }
        MarkShapeMode.TEXT -> {
            val newWidth = (stroke.strokeWidth * factor).coerceIn(0.012f, 0.08f)
            stroke.copy(strokeWidth = newWidth)
        }
        MarkShapeMode.FREEHAND -> {
            if (stroke.points.isEmpty()) return stroke
            var minX = stroke.points.minOf { it.x }
            var maxX = stroke.points.maxOf { it.x }
            var minY = stroke.points.minOf { it.y }
            var maxY = stroke.points.maxOf { it.y }
            val cx = (minX + maxX) / 2f
            val cy = (minY + maxY) / 2f
            val scaledPoints = stroke.points.map { pt ->
                Offset(
                    (cx + (pt.x - cx) * factor).coerceIn(0.01f, 0.99f),
                    (cy + (pt.y - cy) * factor).coerceIn(0.01f, 0.99f)
                )
            }
            stroke.copy(points = scaledPoints)
        }
        else -> stroke
    }
}

private fun hitTestSelectedHandle(
    touchOffset: Offset,
    stroke: DrawnStroke,
    canvasWidth: Float,
    canvasHeight: Float,
    hitRadiusPx: Float = 42f
): ShapeHandle {
    when (stroke.shapeMode) {
        MarkShapeMode.CIRCLE, MarkShapeMode.RECTANGLE -> {
            val l = minOf(stroke.startOffset.x, stroke.endOffset.x) * canvasWidth
            val t = minOf(stroke.startOffset.y, stroke.endOffset.y) * canvasHeight
            val r = maxOf(stroke.startOffset.x, stroke.endOffset.x) * canvasWidth
            val b = maxOf(stroke.startOffset.y, stroke.endOffset.y) * canvasHeight

            val hitR2 = hitRadiusPx * hitRadiusPx
            fun distSq(p1: Offset, p2: Offset): Float {
                val dx = p1.x - p2.x
                val dy = p1.y - p2.y
                return dx * dx + dy * dy
            }

            if (distSq(touchOffset, Offset(l, t)) <= hitR2) return ShapeHandle.TL
            if (distSq(touchOffset, Offset(r, t)) <= hitR2) return ShapeHandle.TR
            if (distSq(touchOffset, Offset(l, b)) <= hitR2) return ShapeHandle.BL
            if (distSq(touchOffset, Offset(r, b)) <= hitR2) return ShapeHandle.BR
        }
        MarkShapeMode.ARROW -> {
            val startPx = Offset(stroke.startOffset.x * canvasWidth, stroke.startOffset.y * canvasHeight)
            val endPx = Offset(stroke.endOffset.x * canvasWidth, stroke.endOffset.y * canvasHeight)
            val hitR2 = hitRadiusPx * hitRadiusPx

            fun distSq(p1: Offset, p2: Offset): Float {
                val dx = p1.x - p2.x
                val dy = p1.y - p2.y
                return dx * dx + dy * dy
            }

            if (distSq(touchOffset, startPx) <= hitR2) return ShapeHandle.ARROW_START
            if (distSq(touchOffset, endPx) <= hitR2) return ShapeHandle.ARROW_END
        }
        else -> {}
    }
    return ShapeHandle.NONE
}

private fun distToSegmentSq(p: Offset, v: Offset, w: Offset): Float {
    val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
    if (l2 == 0f) return (p.x - v.x) * (p.x - v.x) + (p.y - v.y) * (p.y - v.y)
    val t = (((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2).coerceIn(0f, 1f)
    val projX = v.x + t * (w.x - v.x)
    val projY = v.y + t * (w.y - v.y)
    return (p.x - projX) * (p.x - projX) + (p.y - projY) * (p.y - projY)
}

private fun findHitStrokeIndex(
    touchOffset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    strokes: List<DrawnStroke>,
    extraPaddingPx: Float = 36f
): Int? {
    for (i in strokes.indices.reversed()) {
        val stroke = strokes[i]
        when (stroke.shapeMode) {
            MarkShapeMode.TEXT -> {
                val rect = getTextBoundsRect(stroke, canvasWidth, canvasHeight) ?: continue
                if (touchOffset.x >= rect.left - extraPaddingPx &&
                    touchOffset.x <= rect.right + extraPaddingPx &&
                    touchOffset.y >= rect.top - extraPaddingPx &&
                    touchOffset.y <= rect.bottom + extraPaddingPx
                ) {
                    return i
                }
            }
            MarkShapeMode.CIRCLE -> {
                val l = minOf(stroke.startOffset.x, stroke.endOffset.x) * canvasWidth
                val t = minOf(stroke.startOffset.y, stroke.endOffset.y) * canvasHeight
                val r = maxOf(stroke.startOffset.x, stroke.endOffset.x) * canvasWidth
                val b = maxOf(stroke.startOffset.y, stroke.endOffset.y) * canvasHeight

                val cx = (l + r) / 2f
                val cy = (t + b) / 2f
                val rx = maxOf(12f, (r - l) / 2f + extraPaddingPx)
                val ry = maxOf(12f, (b - t) / 2f + extraPaddingPx)
                val dx = (touchOffset.x - cx) / rx
                val dy = (touchOffset.y - cy) / ry
                if ((dx * dx + dy * dy) <= 1.15f) {
                    return i
                }
            }
            MarkShapeMode.RECTANGLE -> {
                val l = minOf(stroke.startOffset.x, stroke.endOffset.x) * canvasWidth
                val t = minOf(stroke.startOffset.y, stroke.endOffset.y) * canvasHeight
                val r = maxOf(stroke.startOffset.x, stroke.endOffset.x) * canvasWidth
                val b = maxOf(stroke.startOffset.y, stroke.endOffset.y) * canvasHeight

                if (touchOffset.x >= l - extraPaddingPx &&
                    touchOffset.x <= r + extraPaddingPx &&
                    touchOffset.y >= t - extraPaddingPx &&
                    touchOffset.y <= b + extraPaddingPx
                ) {
                    return i
                }
            }
            MarkShapeMode.ARROW -> {
                val startPx = Offset(stroke.startOffset.x * canvasWidth, stroke.startOffset.y * canvasHeight)
                val endPx = Offset(stroke.endOffset.x * canvasWidth, stroke.endOffset.y * canvasHeight)
                val hitThreshold = maxOf(extraPaddingPx, 32f)
                if (distToSegmentSq(touchOffset, startPx, endPx) <= hitThreshold * hitThreshold) {
                    return i
                }
            }
            MarkShapeMode.FREEHAND -> {
                if (stroke.points.size > 1) {
                    val hitThreshold = maxOf(extraPaddingPx, 28f)
                    val hitR2 = hitThreshold * hitThreshold
                    for (j in 0 until stroke.points.size - 1) {
                        val p1 = Offset(stroke.points[j].x * canvasWidth, stroke.points[j].y * canvasHeight)
                        val p2 = Offset(stroke.points[j + 1].x * canvasWidth, stroke.points[j + 1].y * canvasHeight)
                        if (distToSegmentSq(touchOffset, p1, p2) <= hitR2) {
                            return i
                        }
                    }
                }
            }
            else -> {}
        }
    }
    return null
}

private fun getTextBoundsRect(stroke: DrawnStroke, canvasWidth: Float, canvasHeight: Float): RectF? {
    if (stroke.shapeMode != MarkShapeMode.TEXT || stroke.text.isBlank()) return null
    val minDim = minOf(canvasWidth, canvasHeight)
    val fontSize = (stroke.strokeWidth * minDim * 3.5f).coerceIn(14f, 160f)
    val textPaint = Paint().apply {
        textSize = fontSize
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textBounds = android.graphics.Rect()
    textPaint.getTextBounds(stroke.text, 0, stroke.text.length, textBounds)

    val x = stroke.startOffset.x * canvasWidth
    val y = stroke.startOffset.y * canvasHeight

    val padX = fontSize * 0.45f
    val padY = fontSize * 0.35f

    return RectF(
        x - padX,
        y - textBounds.height() - padY,
        x + textBounds.width() + padX,
        y + padY
    )
}

private fun findHitTextStrokeIndex(
    touchOffset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    strokes: List<DrawnStroke>,
    extraPaddingPx: Float = 45f
): Int? {
    for (i in strokes.indices.reversed()) {
        val rect = getTextBoundsRect(strokes[i], canvasWidth, canvasHeight) ?: continue
        if (touchOffset.x >= rect.left - extraPaddingPx &&
            touchOffset.x <= rect.right + extraPaddingPx &&
            touchOffset.y >= rect.top - extraPaddingPx &&
            touchOffset.y <= rect.bottom + extraPaddingPx
        ) {
            return i
        }
    }
    return null
}

private fun DrawScope.drawCustomShape(
    stroke: DrawnStroke,
    size: Size,
    strokePx: Float,
    isSelected: Boolean = false
) {
    val minDim = minOf(size.width, size.height)
    when (stroke.shapeMode) {
        MarkShapeMode.FREEHAND -> {
            if (stroke.points.size > 1) {
                val path = Path().apply {
                    val first = stroke.points.first()
                    moveTo(first.x * size.width, first.y * size.height)
                    for (i in 1 until stroke.points.size) {
                        val pt = stroke.points[i]
                        lineTo(pt.x * size.width, pt.y * size.height)
                    }
                }
                drawPath(
                    path = path,
                    color = stroke.color,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                if (isSelected) {
                    val minX = stroke.points.minOf { it.x } * size.width
                    val maxX = stroke.points.maxOf { it.x } * size.width
                    val minY = stroke.points.minOf { it.y } * size.height
                    val maxY = stroke.points.maxOf { it.y } * size.height
                    drawRoundRect(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(minX - 6f, minY - 6f),
                        size = Size(maxX - minX + 12f, maxY - minY + 12f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
                    )
                }
            }
        }
        MarkShapeMode.CIRCLE -> {
            val startPx = Offset(stroke.startOffset.x * size.width, stroke.startOffset.y * size.height)
            val endPx = Offset(stroke.endOffset.x * size.width, stroke.endOffset.y * size.height)
            val left = minOf(startPx.x, endPx.x)
            val top = minOf(startPx.y, endPx.y)
            val right = maxOf(startPx.x, endPx.x)
            val bottom = maxOf(startPx.y, endPx.y)
            val width = maxOf(1f, right - left)
            val height = maxOf(1f, bottom - top)

            drawOval(
                color = stroke.color,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (isSelected) {
                // Draw selection bounding box
                drawRoundRect(
                    color = Color(0xFF38BDF8),
                    topLeft = Offset(left - 6f, top - 6f),
                    size = Size(width + 12f, height + 12f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
                )

                // 4 Corner resize handles
                val handleRadius = 9f
                val cornerPoints = listOf(
                    Offset(left, top),
                    Offset(right, top),
                    Offset(left, bottom),
                    Offset(right, bottom)
                )
                cornerPoints.forEach { pt ->
                    drawCircle(color = Color.White, radius = handleRadius, center = pt)
                    drawCircle(color = HighDensityBlueDark, radius = handleRadius, center = pt, style = Stroke(width = 2.5f))
                }
            }
        }
        MarkShapeMode.RECTANGLE -> {
            val startPx = Offset(stroke.startOffset.x * size.width, stroke.startOffset.y * size.height)
            val endPx = Offset(stroke.endOffset.x * size.width, stroke.endOffset.y * size.height)
            val left = minOf(startPx.x, endPx.x)
            val top = minOf(startPx.y, endPx.y)
            val right = maxOf(startPx.x, endPx.x)
            val bottom = maxOf(startPx.y, endPx.y)
            val width = maxOf(1f, right - left)
            val height = maxOf(1f, bottom - top)

            drawRect(
                color = stroke.color,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (isSelected) {
                // Draw selection bounding outline
                drawRoundRect(
                    color = HighDensityBlue,
                    topLeft = Offset(left - 6f, top - 6f),
                    size = Size(width + 12f, height + 12f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
                )

                // 4 Corner resize handles
                val handleRadius = 9f
                val cornerPoints = listOf(
                    Offset(left, top),
                    Offset(right, top),
                    Offset(left, bottom),
                    Offset(right, bottom)
                )
                cornerPoints.forEach { pt ->
                    drawCircle(color = Color.White, radius = handleRadius, center = pt)
                    drawCircle(color = HighDensityBlueDark, radius = handleRadius, center = pt, style = Stroke(width = 2.5f))
                }
            }
        }
        MarkShapeMode.ARROW -> {
            val startPx = Offset(stroke.startOffset.x * size.width, stroke.startOffset.y * size.height)
            val endPx = Offset(stroke.endOffset.x * size.width, stroke.endOffset.y * size.height)
            drawArrow(startPx, endPx, stroke.color, strokePx)

            if (isSelected) {
                val handleRadius = 9f
                // Tail handle (start)
                drawCircle(color = Color.White, radius = handleRadius, center = startPx)
                drawCircle(color = HighDensityBlueDark, radius = handleRadius, center = startPx, style = Stroke(width = 2.5f))

                // Head handle (end)
                drawCircle(color = Color.White, radius = handleRadius, center = endPx)
                drawCircle(color = HighDensityBlueDark, radius = handleRadius, center = endPx, style = Stroke(width = 2.5f))
            }
        }
        MarkShapeMode.TEXT -> {
            val text = stroke.text
            if (text.isNotBlank()) {
                val fontSize = (stroke.strokeWidth * minDim * 3.5f).coerceIn(14f, 160f)
                val textPaint = Paint().apply {
                    color = stroke.color.toArgb()
                    textSize = fontSize
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val textBounds = android.graphics.Rect()
                textPaint.getTextBounds(text, 0, text.length, textBounds)

                val x = stroke.startOffset.x * size.width
                val y = stroke.startOffset.y * size.height

                val padX = fontSize * 0.45f
                val padY = fontSize * 0.35f
                val bgRect = RectF(
                    x - padX,
                    y - textBounds.height() - padY,
                    x + textBounds.width() + padX,
                    y + padY
                )

                val bgPaint = Paint().apply {
                    color = android.graphics.Color.argb(225, 15, 23, 42)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val borderPaint = Paint().apply {
                    color = stroke.color.toArgb()
                    style = Paint.Style.STROKE
                    strokeWidth = (stroke.strokeWidth * minDim * 0.35f).coerceIn(2.5f, 10f)
                    isAntiAlias = true
                }

                val corner = fontSize * 0.35f
                drawContext.canvas.nativeCanvas.drawRoundRect(bgRect, corner, corner, bgPaint)
                drawContext.canvas.nativeCanvas.drawRoundRect(bgRect, corner, corner, borderPaint)
                drawContext.canvas.nativeCanvas.drawText(text, x, y, textPaint)

                // If selected / actively dragged, render cyan halo and drag handles
                if (isSelected) {
                    val selHighlightPaint = Paint().apply {
                        color = android.graphics.Color.parseColor("#38BDF8")
                        style = Paint.Style.STROKE
                        strokeWidth = 3.5f
                        isAntiAlias = true
                    }
                    val selRect = RectF(
                        bgRect.left - 4f,
                        bgRect.top - 4f,
                        bgRect.right + 4f,
                        bgRect.bottom + 4f
                    )
                    val selCorner = corner + 4f
                    drawContext.canvas.nativeCanvas.drawRoundRect(selRect, selCorner, selCorner, selHighlightPaint)

                    val dotPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val dotBorderPaint = Paint().apply {
                        color = android.graphics.Color.parseColor("#0284C7")
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                        isAntiAlias = true
                    }
                    val dotR = 5f
                    listOf(
                        Offset(selRect.left, selRect.top),
                        Offset(selRect.right, selRect.top),
                        Offset(selRect.left, selRect.bottom),
                        Offset(selRect.right, selRect.bottom)
                    ).forEach { pt ->
                        drawContext.canvas.nativeCanvas.drawCircle(pt.x, pt.y, dotR, dotPaint)
                        drawContext.canvas.nativeCanvas.drawCircle(pt.x, pt.y, dotR, dotBorderPaint)
                    }
                }
            }
        }
        else -> {}
    }
}

private fun DrawScope.drawArrow(start: Offset, end: Offset, color: Color, strokeWidth: Float) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val headLength = strokeWidth * 3.5f
    val headAngle = Math.toRadians(30.0)

    val x1 = end.x - headLength * cos(angle - headAngle).toFloat()
    val y1 = end.y - headLength * sin(angle - headAngle).toFloat()
    val x2 = end.x - headLength * cos(angle + headAngle).toFloat()
    val y2 = end.y - headLength * sin(angle + headAngle).toFloat()

    drawLine(
        color = color,
        start = end,
        end = Offset(x1, y1),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = end,
        end = Offset(x2, y2),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        color = when {
            isSelected -> HighDensityBlue
            !enabled -> Color(0xFF1E293B)
            else -> Color(0xFF334155)
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF475569)),
        modifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) Color.White else Color(0xFF64748B),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(3.5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (enabled) Color.White else Color(0xFF64748B),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun ThicknessButton(
    size: Float,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) HighDensityBlue else Color(0xFF334155),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = Color.White,
                fontSize = 10.sp
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}
