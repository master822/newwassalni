package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.util.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.PrimaryGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * High-performance Cache and Loader for Chat Images.
 * Seamlessly handles:
 * - Base64 Data URIs (data:image/jpeg;base64,...)
 * - Raw Base64 strings
 * - Content URIs (content://...)
 * - Local file paths (file:// or /storage/...)
 * - Remote HTTP/HTTPS URLs
 */
object ChatImageLoader {
    private const val TAG = "ChatImageLoader"

    // Memory cache for decoded Bitmaps to guarantee 60 FPS scrolling and instant previews
    private val bitmapCache = object : LruCache<String, Bitmap>(50) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }
    }

    private fun cacheKey(source: String): String {
        return if (source.length > 80) {
            "${source.take(40)}_${source.hashCode()}_${source.length}"
        } else {
            source
        }
    }

    fun getCached(source: String?): Bitmap? {
        if (source.isNullOrBlank()) return null
        return bitmapCache.get(cacheKey(source))
    }

    suspend fun loadBitmap(context: Context, source: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (source.isNullOrBlank()) return@withContext null
        val key = cacheKey(source)
        bitmapCache.get(key)?.let { return@withContext it }

        val trimmed = source.trim()
        val bmp: Bitmap? = try {
            when {
                // 1. Data URI with base64 (e.g. data:image/jpeg;base64,...)
                trimmed.startsWith("data:image", ignoreCase = true) || trimmed.contains(";base64,") -> {
                    val base64Part = trimmed.substringAfter(";base64,").trim()
                    val decodedBytes = Base64.decode(base64Part, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                }

                // 2. Android Content URI (e.g. content://media/external/images/media/...)
                trimmed.startsWith("content://") -> {
                    val uri = Uri.parse(trimmed)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        decodeScaledBitmapFromStream(inputStream, 1280)
                    }
                }

                // 3. Local File URI or absolute path
                trimmed.startsWith("file://") || trimmed.startsWith("/") -> {
                    val path = trimmed.removePrefix("file://")
                    val file = File(path)
                    if (file.exists() && file.length() > 0) {
                        decodeScaledBitmapFromFile(file, 1280)
                    } else null
                }

                // 4. Raw Base64 string without prefix
                trimmed.length > 60 && !trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true) -> {
                    try {
                        val decodedBytes = Base64.decode(trimmed, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    } catch (_: Exception) {
                        null
                    }
                }

                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode image: ${e.message}")
            null
        }

        if (bmp != null) {
            bitmapCache.put(key, bmp)
        }
        bmp
    }

    /**
     * Compress an image from any URI/File to a compact, transmission-ready Base64 JPEG data URI.
     */
    fun compressUriToBase64(context: Context, uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        val trimmed = uriString.trim()
        if (trimmed.startsWith("data:image", ignoreCase = true) || trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }

        return try {
            val bitmap = when {
                trimmed.startsWith("content://") || trimmed.startsWith("file://") -> {
                    val uri = Uri.parse(trimmed)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        decodeScaledBitmapFromStream(inputStream, 1280)
                    }
                }
                else -> {
                    val file = File(trimmed)
                    if (file.exists()) {
                        decodeScaledBitmapFromFile(file, 1280)
                    } else null
                }
            } ?: return trimmed

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            val bytes = baos.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress image to base64: ${e.message}")
            uriString
        }
    }

    private fun decodeScaledBitmapFromStream(inputStream: InputStream, maxDim: Int): Bitmap? {
        val original = BitmapFactory.decodeStream(inputStream) ?: return null
        return scaleDownBitmap(original, maxDim)
    }

    private fun decodeScaledBitmapFromFile(file: File, maxDim: Int): Bitmap? {
        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return scaleDownBitmap(original, maxDim)
    }

    private fun scaleDownBitmap(original: Bitmap, maxDim: Int): Bitmap {
        val width = original.width
        val height = original.height
        if (width <= maxDim && height <= maxDim) return original

        val ratio = width.toFloat() / height.toFloat()
        val (newW, newH) = if (ratio > 1f) {
            maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
        }
        return Bitmap.createScaledBitmap(original, newW, newH, true)
    }
}

/**
 * Universal Composable for reliably displaying chat images across all devices and formats.
 */
@Composable
fun ChatImageView(
    imageSource: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var bitmap by remember(imageSource) { mutableStateOf(ChatImageLoader.getCached(imageSource)) }
    var isLoading by remember(imageSource) {
        val isHttp = imageSource?.startsWith("http://", ignoreCase = true) == true ||
                     imageSource?.startsWith("https://", ignoreCase = true) == true
        mutableStateOf(bitmap == null && !imageSource.isNullOrBlank() && !isHttp)
    }
    var hasError by remember(imageSource) { mutableStateOf(false) }

    val isHttp = remember(imageSource) {
        imageSource?.startsWith("http://", ignoreCase = true) == true ||
        imageSource?.startsWith("https://", ignoreCase = true) == true
    }

    LaunchedEffect(imageSource) {
        if (imageSource.isNullOrBlank()) {
            bitmap = null
            isLoading = false
            hasError = false
            return@LaunchedEffect
        }
        if (isHttp) {
            isLoading = false
            return@LaunchedEffect
        }
        if (bitmap == null) {
            isLoading = true
            hasError = false
            val loaded = ChatImageLoader.loadBitmap(context, imageSource)
            bitmap = loaded
            isLoading = false
            hasError = (loaded == null)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            isHttp -> {
                AsyncImage(
                    model = imageSource,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PrimaryGreen,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            hasError -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrokenImage,
                            contentDescription = "تعذر تحميل الصورة",
                            tint = Color.Gray.copy(alpha = 0.7f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "صورة مرفقة",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full-screen interactive Photo Viewer with Pinch-to-Zoom, Double-Tap Zoom, and Pan gestures.
 */
@Composable
fun FullscreenPhotoViewerDialog(
    imageSource: String?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    if (imageSource.isNullOrBlank()) return

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            val maxOffsetX = 800f * (scale - 1f)
            val maxOffsetY = 1200f * (scale - 1f)
            offset = Offset(
                x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
            )
        } else {
            offset = Offset.Zero
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
        ) {
            // Top Header Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "عرض الصورة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                if (onDelete != null) {
                    IconButton(
                        onClick = {
                            onDismiss()
                            onDelete()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "حذف الصورة",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.size(42.dp))
                }
            }

            // Central Zoomable Image View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 68.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(state = transformableState),
                contentAlignment = Alignment.Center
            ) {
                ChatImageView(
                    imageSource = imageSource,
                    contentDescription = "صورة بالحجم الكامل",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom Tip & Reset Zoom Bar
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (scale > 1f) "انقر نقراً مزدوجاً لإعادة التكبير للحجم الأصلي" else "يمكنك التكبير باللمس بإصبعين أو النقر المزدوج",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
