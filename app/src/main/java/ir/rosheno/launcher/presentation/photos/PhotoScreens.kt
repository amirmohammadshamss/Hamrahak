package ir.rosheno.launcher.presentation.photos

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.rosheno.launcher.R
import ir.rosheno.launcher.di.FeatureViewModelFactory
import ir.rosheno.launcher.presentation.common.resolve
import ir.rosheno.launcher.presentation.components.LauncherIcon
import ir.rosheno.launcher.presentation.theme.Green
import ir.rosheno.launcher.presentation.theme.Ink
import ir.rosheno.launcher.presentation.theme.Pale
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable private fun ReturnHome(onHome: () -> Unit) {
    val context = LocalContext.current
    Button(onClick = onHome, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        Text(context.getString(R.string.action_return_to_rosheno), fontSize = 22.sp)
    }
}

@Composable
fun CameraScreen(onHome: () -> Unit, onCaptured: (String) -> Unit, model: CameraViewModel = viewModel(factory = FeatureViewModelFactory)) {
    val context = LocalContext.current
    val state by model.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    var ready by remember { mutableStateOf(false) }
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
                model.ready()
            } catch (_: Exception) { model.unavailable() }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            active = false
            model.cancelPreparation()
            preview?.let { provider?.unbind(it, capture) }
        }
    }
    LaunchedEffect(state.preparedPath, ready) {
        if (!ready || !active) return@LaunchedEffect
        val path = model.takePreparedCapture() ?: return@LaunchedEffect
        try {
            capture.targetRotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
            capture.takePicture(ImageCapture.OutputFileOptions.Builder(File(path)).build(), ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                        if (model.captureSaved(path) && active) captured(path)
                    }
                    override fun onError(exception: ImageCaptureException) { model.captureFailed(path) }
                })
        } catch (_: Exception) { model.captureFailed(path) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReturnHome(onHome)
        Text(context.getString(R.string.camera_title), fontSize = 24.sp, color = Ink)
        if (!permission) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LauncherIcon("camera", Green)
                Text(context.getString(R.string.camera_permission_description), fontSize = 22.sp)
                Button(onClick = { permissionRequest.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                    Text(context.getString(R.string.action_camera_permission), fontSize = 20.sp)
                }
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth().background(colorResource(R.color.black), RoundedCornerShape(24.dp))) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                if (!ready && state.error == null) CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            state.error?.let { Text(it.resolve(context), fontSize = 18.sp, color = MaterialTheme.colorScheme.error) }
            Button(enabled = ready && !state.saving, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).testTag("camera-shutter"),
                onClick = model::requestCapture) {
                Text(if (state.saving) context.getString(R.string.status_saving_photo) else context.getString(R.string.action_take_photo), fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun PhotoLibraryScreen(onHome: () -> Unit, onCamera: () -> Unit, onPhoto: (String) -> Unit,
    model: PhotoLibraryViewModel = viewModel(factory = FeatureViewModelFactory)) {
    val context = LocalContext.current
    val state by model.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(model, owner) {
        model.refresh()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.refresh() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { onPhoto(it.toString()) } }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReturnHome(onHome)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(context.getString(R.string.photo_library_title), fontSize = 24.sp, color = Ink)
            if (state.loading) CircularProgressIndicator()
            else if (state.error == null && state.photos.isEmpty()) {
                LauncherIcon("photo", Green)
                Text(context.getString(R.string.photo_library_empty), fontSize = 22.sp)
            }
            state.error?.let { error ->
                Text(error.resolve(context), fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = model::refresh) { Text(context.getString(R.string.action_retry)) }
            }
            state.photos.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { path -> Surface(onClick = { onPhoto(path) }, shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f).aspectRatio(1f)) {
                    PhotoImage(path, ContentScale.Crop, Modifier.fillMaxSize(), context.getString(R.string.photo_saved_description))
                } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            } }
            Button(onClick = onCamera, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(context.getString(R.string.action_capture_new_photo), fontSize = 22.sp) }
            OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Text(context.getString(R.string.action_choose_device_photo), fontSize = 22.sp)
            }
        }
    }
}

@Composable
fun PhotoViewerScreen(source: String?, onHome: () -> Unit, onPhotos: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReturnHome(onHome)
        source?.let { PhotoImage(it, ContentScale.Fit, Modifier.weight(1f).fillMaxWidth(), context.getString(R.string.page_photo_view)) }
            ?: Text(context.getString(R.string.photo_not_selected), fontSize = 22.sp)
        OutlinedButton(onClick = onPhotos, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text(context.getString(R.string.photo_library_title), fontSize = 22.sp)
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
        if (failed) Text(context.getString(R.string.photo_unavailable), Modifier.padding(16.dp), fontSize = 20.sp)
    }
}
