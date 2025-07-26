package com.sheikyon.spectratron.ui.screens

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.Manifest
import android.graphics.Color
import android.util.Log
import android.util.Size
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class CameraViewModel : ViewModel() {
    val currentColorHex = mutableStateOf("#FFFFFF")

    val isLocked = mutableStateOf(false)

    val cameraError = mutableStateOf<String?>(null)

    val canUpdateColor = mutableStateOf(true)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val previewView = remember { PreviewView(context) }

    val startCamera = remember {
        {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {

                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(previewView.width, previewView.height))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            if (!viewModel.isLocked.value && viewModel.canUpdateColor.value) {
                                val hexColor = analyzeImageForColor(imageProxy)
                                if (hexColor != null) {
                                    ContextCompat.getMainExecutor(context).execute {
                                        viewModel.currentColorHex.value = hexColor
                                        viewModel.canUpdateColor.value = false

                                        lifecycleOwner.lifecycleScope.launch {
                                            delay(1000L)
                                            viewModel.canUpdateColor.value = true
                                        }
                                    }
                                }
                            }
                            imageProxy.close()
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                    viewModel.cameraError.value = null
                } catch (exc: Exception) {
                    Log.e("SpectraTron", "Fallo al vincular casos de uso de la cámara", exc)
                    viewModel.cameraError.value = "Error al iniciar cámara: ${exc.message}"
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    DisposableEffect(key1 = cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            startCamera()
        }
        onDispose {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            }, ContextCompat.getMainExecutor(context))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    previewView.apply {
                        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    }
                }
            )

            viewModel.cameraError.value?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = androidx.compose.ui.graphics.Color.Red,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f))
                        .padding(8.dp)
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.White,
                    radius = 8.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(androidx.compose.ui.graphics.Color(Color.parseColor(viewModel.currentColorHex.value)))
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = viewModel.currentColorHex.value,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(onClick = { viewModel.isLocked.value = !viewModel.isLocked.value }) {
                    Text(if (viewModel.isLocked.value) "Resume" else "Pause")
                }
            }

        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "SpectraTron necesita permiso de cámara para funcionar.",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (cameraPermissionState.status.shouldShowRationale) {
                    Text(
                        "Por favor, concede el permiso de cámara para usar esta aplicación.",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Conceder Permiso de Cámara")
                }
            }
        }
    }
}

fun analyzeImageForColor(image: ImageProxy): String? {
    try {
        val planes = image.planes
        val yBuffer: ByteBuffer = planes[0].buffer
        val uBuffer: ByteBuffer = planes[1].buffer
        val vBuffer: ByteBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        if (bitmap == null) {
            Log.e("SpectraTron", "Failed to decode bitmap from image bytes.")
            return null
        }

        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2

        if (centerX >= 0 && centerX < bitmap.width && centerY >= 0 && centerY < bitmap.height) {
            val pixel = bitmap.getPixel(centerX, centerY)
            val hexColor = String.format("#%06X", (0xFFFFFF and pixel))
            bitmap.recycle()
            return hexColor
        }
        bitmap.recycle()
        return null
    } catch (e: Exception) {
        Log.e("SpectraTron", "Error processing image for color: ${e.message}", e)
        return null
    }
}