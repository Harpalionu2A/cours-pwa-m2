package corsica.universita.carnet.ui.notes

import android.Manifest
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import corsica.universita.carnet.CarnetContainer
import corsica.universita.carnet.R
import corsica.universita.carnet.data.Note
import corsica.universita.carnet.ui.permissions.PermissionFlow
import corsica.universita.carnet.ui.permissions.PermissionState
import corsica.universita.carnet.ui.permissions.rememberPermissionFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * L'écran « détail d'une note » (TD A7, deuxième entrée de Navigation 3) : position et photo, chacune
 * derrière son flux de permission ; partage de la note (texte + photo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(id: Long, onBack: () -> Unit, onShare: (Note) -> Unit) {
    val viewModel: NoteDetailViewModel = viewModel(key = "note-$id", factory = NoteDetailViewModel.factory(id))
    val note by viewModel.note.collectAsStateWithLifecycle()
    val locating by viewModel.locating.collectAsStateWithLifecycle()
    val lastFix by viewModel.lastFix.collectAsStateWithLifecycle()

    // Précise OU approximative : l'utilisateur choisit dans le dialogue (Android 12+), l'app prend ce qu'on lui donne
    val location = rememberPermissionFlow(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        requireAll = false,
    )
    val camera = rememberPermissionFlow(Manifest.permission.CAMERA)
    var showCamera by rememberSaveable { mutableStateOf(false) }

    if (showCamera) {
        CameraCapture(
            photoStore = CarnetContainer.photoStore,
            onPhoto = { file -> viewModel.attachPhoto(file.path); showCamera = false },
            onClose = { showCamera = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.note_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    note?.let { n ->
                        IconButton(onClick = { onShare(n) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                        }
                        IconButton(onClick = { viewModel.delete(onBack) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val n = note
        if (n == null) {
            Text(stringResource(R.string.note_missing), modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            Text(n.text, style = MaterialTheme.typography.titleLarge)
            Text(n.createdAt.toDateTime(), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))

            // --- La position ---
            Text(stringResource(R.string.position), style = MaterialTheme.typography.titleMedium)
            if (n.lat != null && n.lng != null) {
                Text(String.format(Locale.FRANCE, "%.5f, %.5f", n.lat, n.lng))
                lastFix?.let { fix ->
                    Text(
                        stringResource(
                            if (fix.precise) R.string.fix_precise else R.string.fix_approx,
                            fix.accuracyMeters.toInt(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            PermissionAction(
                flow = location,
                label = stringResource(if (n.lat == null) R.string.add_position else R.string.refresh_position),
                rationale = stringResource(R.string.location_rationale),
                deniedForever = stringResource(R.string.location_denied_forever),
                busy = locating,
                onGranted = viewModel::locate,
            )
            Spacer(Modifier.height(16.dp))

            // --- La photo ---
            Text(stringResource(R.string.photo), style = MaterialTheme.typography.titleMedium)
            n.photoPath?.let { path -> NotePhoto(path) }
            PermissionAction(
                flow = camera,
                label = stringResource(if (n.photoPath == null) R.string.add_photo else R.string.retake_photo),
                rationale = stringResource(R.string.camera_rationale),
                deniedForever = stringResource(R.string.camera_denied_forever),
                busy = false,
                onGranted = { showCamera = true },
            )
        }
    }
}

/**
 * Un bouton derrière une permission, selon l'état de la machine :
 * NotAsked → demander · DeniedOnce → expliquer (rationale), puis demander · DeniedForever → les réglages ·
 * Granted → l'action. Le même composable sert à la position et à la photo.
 */
@Composable
fun PermissionAction(
    flow: PermissionFlow,
    label: String,
    rationale: String,
    deniedForever: String,
    busy: Boolean,
    onGranted: () -> Unit,
) {
    var showRationale by rememberSaveable { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        when (flow.state) {
            PermissionState.Granted -> Button(onClick = onGranted, enabled = !busy) { Text(label) }
            PermissionState.NotAsked -> OutlinedButton(onClick = flow::request) { Text(label) }
            PermissionState.DeniedOnce -> OutlinedButton(onClick = { showRationale = true }) { Text(label) }
            PermissionState.DeniedForever -> OutlinedButton(onClick = flow::openSettings) {
                Text(stringResource(R.string.open_settings))
            }
        }
        if (busy) {
            Spacer(Modifier.width(12.dp))
            CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
        }
    }
    if (flow.state == PermissionState.DeniedForever) {
        Text(deniedForever, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.why_permission)) },
            text = { Text(rationale) },
            confirmButton = {
                TextButton(onClick = { showRationale = false; flow.request() }) { Text(stringResource(R.string.continue_)) }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** La photo, décodée hors du fil principal, réduite (une photo de 12 Mpx ne tient pas en mémoire telle quelle). */
@Composable
fun NotePhoto(path: String) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = stringResource(R.string.photo),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
    }
}

private fun Long.toDateTime(): String =
    DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(Date(this))
