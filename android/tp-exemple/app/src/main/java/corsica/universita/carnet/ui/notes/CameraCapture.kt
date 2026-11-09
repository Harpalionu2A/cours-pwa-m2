package corsica.universita.carnet.ui.notes

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import corsica.universita.carnet.R
import corsica.universita.carnet.data.camera.PhotoStore
import java.io.File

private const val TAG = "Carnet"

/**
 * TD A7 : la caméra en pipeline (CameraX) — un aperçu (`Preview`) et une capture (`ImageCapture`), liés au
 * cycle de vie de l'écran : l'OS coupe la caméra quand l'écran n'est plus visible, et la rend ensuite.
 * (Le raccourci `ActivityResultContracts.TakePicture()` ouvre l'app caméra du système, sans permission ;
 * ici l'app possède le pipeline — et donc la permission CAMERA.)
 */
@Composable
fun CameraCapture(photoStore: PhotoStore, onPhoto: (File) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
        )
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(24.dp),
        ) {
            OutlinedButton(onClick = onClose) { Text(stringResource(R.string.cancel)) }
            Button(onClick = {
                val file = photoStore.newFile()
                imageCapture.takePicture(
                    ImageCapture.OutputFileOptions.Builder(file).build(),
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Log.d(TAG, "Photo enregistrée : ${file.path}")
                            onPhoto(file)
                        }

                        override fun onError(e: ImageCaptureException) {
                            Log.d(TAG, "Photo impossible : $e")
                        }
                    },
                )
            }) { Text(stringResource(R.string.take_photo)) }
        }
    }

    // L'écran disparaît (photo prise, annulation, retour) : la caméra est rendue à l'OS
    DisposableEffect(Unit) {
        onDispose { if (providerFuture.isDone) providerFuture.get().unbindAll() }
    }
}
