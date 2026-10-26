package ir.rosheno.launcher

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable private fun ReturnHome(onHome: () -> Unit) {
    Button(onClick = onHome, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        Text("بازگشت به روشنو", fontSize = 22.sp)
    }
}

@Composable
fun CameraScreen(onHome: () -> Unit, onCaptured: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    var error by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var active by remember { mutableStateOf(true) }
    val captured by rememberUpdatedState(onCaptured)
    DisposableEffect(permission, owner) {
        var disposed = false
        active = true
        ready = false
        var provider: ProcessCameraProvider? = null
        var preview: Preview? = null
        val future = if (permission) ProcessCameraProvider.getInstance(context) else null
        future?.addListener({
            if (!disposed) try {
                provider = future.get()
                val selector = if (provider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                val livePreview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                preview = livePreview
                provider!!.bindToLifecycle(owner, selector, livePreview, capture)
                ready = true
            } catch (_: Exception) { error = "دوربین در دسترس نیست. می‌توانید به روشنو برگردید و دوباره تلاش کنید." }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            active = false
            preview?.let { provider?.unbind(it, capture) }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReturnHome(onHome)
        Text("دوربین", fontSize = 24.sp, color = Ink)
        if (!permission) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LauncherIcon("camera", Green)
                Text("برای عکس گرفتن، دسترسی به دوربین لازم است. عکس‌ها روی همین گوشی می‌مانند.", fontSize = 22.sp)
                Button(onClick = { permissionRequest.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                    Text("اجازهٔ استفاده از دوربین", fontSize = 20.sp)
                }
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black, RoundedCornerShape(24.dp))) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                if (!ready && error == null) CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            error?.let { Text(it, fontSize = 18.sp, color = MaterialTheme.colorScheme.error) }
            Button(enabled = ready && !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).testTag("camera-shutter"), onClick = {
                saving = true
                error = null
                val directory = context.filesDir.resolve("captures").apply { mkdirs() }
                val file = File(directory, "${UUID.randomUUID()}.jpg")
                capture.targetRotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
                capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                            saving = false
                            if (active) captured(file.absolutePath)
                        }
                        override fun onError(exception: ImageCaptureException) {
                            file.delete()
                            saving = false
                            if (active) error = "عکس ذخیره نشد. دوباره تلاش کنید."
                        }
                    })
            }) { Text(if (saving) "در حال ذخیرهٔ عکس…" else "عکس گرفتن", fontSize = 24.sp) }
        }
    }
}

@Composable
fun PhotoLibraryScreen(onHome: () -> Unit, onCamera: () -> Unit, onPhoto: (String) -> Unit) {
    val context = LocalContext.current
    val photos by produceState<List<File>?>(null) {
        value = withContext(Dispatchers.IO) {
            context.filesDir.resolve("captures").listFiles()?.filter { it.extension.equals("jpg", true) && it.length() > 0 }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { onPhoto(it.toString()) } }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReturnHome(onHome)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("عکس‌های من", fontSize = 24.sp, color = Ink)
            if (photos == null) CircularProgressIndicator()
            else if (photos!!.isEmpty()) {
                LauncherIcon("photo", Green)
                Text("عکس‌هایی که با روشنو می‌گیرید اینجا دیده می‌شوند.", fontSize = 22.sp)
            }
            photos.orEmpty().chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { file -> Surface(onClick = { onPhoto(file.absolutePath) }, shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f).aspectRatio(1f)) {
                    PhotoImage(file.absolutePath, ContentScale.Crop, Modifier.fillMaxSize(), "عکس ذخیره‌شده")
                } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            } }
            Button(onClick = onCamera, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("گرفتن عکس تازه", fontSize = 22.sp) }
            OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Text("انتخاب عکس از گوشی", fontSize = 22.sp)
            }
        }
    }
}

@Composable
fun PhotoViewerScreen(source: String?, onHome: () -> Unit, onPhotos: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReturnHome(onHome)
        source?.let { PhotoImage(it, ContentScale.Fit, Modifier.weight(1f).fillMaxWidth(), "نمایش عکس") }
            ?: Text("عکسی انتخاب نشده است.", fontSize = 22.sp)
        OutlinedButton(onClick = onPhotos, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text("عکس‌های من", fontSize = 22.sp)
        }
    }
}

@Composable
private fun PhotoImage(source: String, scale: ContentScale, modifier: Modifier, description: String) {
    val context = LocalContext.current
    var failed by remember(source) { mutableStateOf(false) }
    val image by produceState<ImageBitmap?>(null, source) {
        value = withContext(Dispatchers.IO) { runCatching {
            val uri = if (source.startsWith("/")) Uri.fromFile(File(source)) else Uri.parse(source)
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val limit = if (scale == ContentScale.Crop) 480f else 1600f
                val ratio = minOf(1f, limit / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize(maxOf(1, (info.size.width * ratio).toInt()), maxOf(1, (info.size.height * ratio).toInt()))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }.asImageBitmap()
        }.getOrNull() }
        failed = value == null
    }
    Box(modifier.background(Pale), contentAlignment = Alignment.Center) {
        image?.let { Image(it, contentDescription = description, modifier = Modifier.fillMaxSize(), contentScale = scale) }
        if (failed) Text("عکس در دسترس نیست؛ دوباره آن را انتخاب کنید.", Modifier.padding(16.dp), fontSize = 20.sp)
    }
}
