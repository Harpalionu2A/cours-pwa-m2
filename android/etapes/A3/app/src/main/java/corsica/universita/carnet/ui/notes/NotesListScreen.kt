package corsica.universita.carnet.ui.notes

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import corsica.universita.carnet.R
import corsica.universita.carnet.data.Note
import corsica.universita.carnet.ui.theme.CarnetTheme
import java.text.DateFormat
import java.util.Date

/**
 * L'écran « liste des notes » (TD A2-A3) : l'état vient du ViewModel, l'écran ne fait que l'afficher
 * et lui renvoyer les événements. Rien n'est gardé dans le composable. Les deux actions qui
 * sortent de l'app (ouvrir la PWA, partager) sont des événements remontés : l'écran ne lance pas
 * d'Intent lui-même.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel = viewModel(),
    onOpenPwa: () -> Unit,
    onShare: (Note) -> Unit,
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()

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
            Text(
                text = pluralStringResource(R.plurals.notes_count, notes.size, notes.size),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            NotesList(notes = notes, onShare = onShare, onDelete = { viewModel.deleteNote(it.id) })
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
fun NotesList(notes: List<Note>, onShare: (Note) -> Unit, onDelete: (Note) -> Unit) {
    LazyColumn {
        items(notes, key = { it.id }) { note ->
            ListItem(
                headlineContent = { Text(note.text) },
                supportingContent = { Text(note.createdAt.toDateTime()) },
                trailingContent = {
                    Row {
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
            notes = listOf(Note(1, "Source au pied du col, débit faible", 0L), Note(2, "Chêne vert marqué", 0L)),
            onShare = {},
            onDelete = {},
        )
    }
}
