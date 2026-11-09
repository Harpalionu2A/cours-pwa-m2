package corsica.universita.carnet.ui.notes

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import corsica.universita.carnet.R
import corsica.universita.carnet.data.Note
import corsica.universita.carnet.notif.Notifications
import corsica.universita.carnet.ui.theme.CarnetTheme
import java.text.DateFormat
import java.util.Date

/**
 * L'écran « liste des notes » (TD A2-A6) : l'état vient du ViewModel, l'écran ne fait que l'afficher
 * et lui renvoyer les événements. Rien n'est gardé dans le composable. Les actions qui sortent de
 * l'app (ouvrir la PWA, partager, copier, ouvrir les réglages) sont des événements remontés : l'écran
 * ne lance pas d'Intent lui-même. Seule exception : la demande de permission, qui est une API de l'écran.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory),
    onOpenPwa: () -> Unit,
    onShare: (Note) -> Unit,
    onCopyToken: (String) -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val pending by viewModel.pendingCount.collectAsStateWithLifecycle()
    val openNoteId by viewModel.openNoteId.collectAsStateWithLifecycle()
    val fcmToken by viewModel.fcmToken.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenPwa) {
                        Icon(Icons.Default.Language, contentDescription = stringResource(R.string.open_pwa))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            NoteForm(
                draft = draft,
                onDraftChange = viewModel::onDraftChange,
                onAdd = viewModel::addNote,
            )
            NotificationsRow(
                fcmToken = fcmToken,
                onRefreshToken = viewModel::refreshFcmToken,
                onCopyToken = onCopyToken,
                onOpenSettings = onOpenNotificationSettings,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = pluralStringResource(R.plurals.notes_count, notes.size, notes.size),
                    style = MaterialTheme.typography.labelLarge,
                )
                // TD A5 : l'outbox rendue visible — « en attente » tant que le réseau (ou l'OS) n'a pas laissé partir
                if (pending > 0) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = pluralStringResource(R.plurals.pending_count, pending, pending),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            NotesList(
                notes = notes,
                highlightedId = openNoteId,
                onPin = viewModel::togglePinned,
                onShare = onShare,
                onDelete = { viewModel.deleteNote(it.id) },
            )
        }
    }
}

/**
 * TD A6 : la permission POST_NOTIFICATIONS (Android 13+) et le jeton FCM. Demander, c'est une API de
 * l'écran (`rememberLauncherForActivityResult`) ; le résultat n'est pas un booléen définitif : refusée
 * deux fois, la permission ne sera plus jamais demandée — le launcher répond `false` sans dialogue —
 * et la seule issue est l'écran des réglages.
 */
@Composable
fun NotificationsRow(
    fcmToken: String?,
    onRefreshToken: () -> Unit,
    onCopyToken: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(Notifications.areEnabled(context)) }
    var deniedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        enabled = granted
        if (!granted) deniedOnce = true
    }
    LaunchedEffect(fcmToken) { if (fcmToken == null) onRefreshToken() }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        if (!enabled) {
            OutlinedButton(onClick = {
                when {
                    // Refusée une fois déjà : la demander encore ne rouvrira peut-être aucun dialogue
                    deniedOnce -> onOpenSettings()
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    // Avant Android 13 : pas de permission runtime, l'app est coupée dans les réglages
                    else -> onOpenSettings()
                }
            }) { Text(stringResource(if (deniedOnce) R.string.notif_settings else R.string.notif_enable)) }
        } else {
            Text(
                stringResource(R.string.notif_enabled),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = { fcmToken?.let(onCopyToken) ?: onRefreshToken() }) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(if (fcmToken == null) R.string.token_pending else R.string.copy_token))
        }
    }
}

/** Le formulaire : un champ de texte et un bouton. L'état est reçu, les événements remontent. */
@Composable
fun NoteForm(draft: String, onDraftChange: (String) -> Unit, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            label = { Text(stringResource(R.string.draft_hint)) },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = onAdd, enabled = draft.isNotBlank()) {
            Text(stringResource(R.string.add))
        }
    }
}

/** La liste, virtualisée : seules les lignes visibles sont composées. */
@Composable
fun NotesList(
    notes: List<Note>,
    highlightedId: Long? = null,
    onPin: (Note) -> Unit,
    onShare: (Note) -> Unit,
    onDelete: (Note) -> Unit,
) {
    LazyColumn {
        items(notes, key = { it.id }) { note ->
            ListItem(
                // TD A6 : la note visée par la notification est mise en évidence
                modifier = if (note.id == highlightedId) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier,
                headlineContent = { Text(note.text) },
                supportingContent = {
                    Text(if (note.synced) note.createdAt.toDateTime() else note.createdAt.toDateTime() + " · " + stringResource(R.string.pending))
                },
                trailingContent = {
                    Row {
                        IconButton(onClick = { onPin(note) }) {
                            Icon(
                                if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = stringResource(R.string.pin),
                                tint = if (note.pinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onShare(note) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                        }
                        IconButton(onClick = { onDelete(note) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
            )
            HorizontalDivider()
        }
    }
}

private fun Long.toDateTime(): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))

@Preview(showBackground = true)
@Composable
private fun NotesListPreview() {
    CarnetTheme {
        NotesList(
            notes = listOf(Note(1, "Source au pied du col, débit faible", 0L, pinned = true), Note(2, "Chêne vert marqué", 0L)),
            onPin = {},
            onShare = {},
            onDelete = {},
        )
    }
}
