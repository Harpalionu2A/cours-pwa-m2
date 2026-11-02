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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import corsica.universita.carnet.R
import corsica.universita.carnet.data.Note
import corsica.universita.carnet.ui.theme.CarnetTheme
import java.text.DateFormat
import java.util.Date

/**
 * L'écran « liste des notes » du TD A1 : un champ, un bouton, une liste.
 *
 * Où vit l'état, à ce stade : dans le composable lui-même.
 *  - `notes`  : `mutableStateListOf` → observé, chaque ajout redessine la liste… mais `remember`
 *               seul : la liste est PERDUE à la rotation (l'Activity est détruite et recréée).
 *  - `draft`  : `rememberSaveable` → sauvé dans le Bundle de l'Activity, SURVIT à la rotation.
 * La liste est confiée à un ViewModel au TD A2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen() {
    val notes = remember { mutableStateListOf<Note>() }
    var draft by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            NoteForm(
                draft = draft,
                onDraftChange = { draft = it },
                onAdd = {
                    notes.add(0, Note(text = draft.trim(), createdAt = System.currentTimeMillis()))
                    draft = ""
                },
            )
            Text(
                text = pluralStringResource(R.plurals.notes_count, notes.size, notes.size),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            NotesList(notes = notes, onDelete = { notes.remove(it) })
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
fun NotesList(notes: List<Note>, onDelete: (Note) -> Unit) {
    LazyColumn {
        items(notes) { note ->
            ListItem(
                headlineContent = { Text(note.text) },
                supportingContent = { Text(note.createdAt.toDateTime()) },
                trailingContent = {
                    IconButton(onClick = { onDelete(note) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
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
private fun NotesListScreenPreview() {
    CarnetTheme { NotesListScreen() }
}
