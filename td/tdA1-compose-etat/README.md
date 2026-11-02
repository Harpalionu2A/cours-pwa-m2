# TD A1 — Première app Compose (45 min)

Jour 4 — lundi 2 novembre 2026. Point de départ : `android/starter/` (smoke test passé). Un seul projet pour A1 → A7 : les TD suivants continuent dans ce dossier.

## Objectif

Une liste de notes et un formulaire dans le starter ; comprendre **où vit l'état** d'un écran Compose — et ce qui lui arrive à la rotation.

## Fonctionnalités attendues

- [ ] 1. `NotesListScreen` réécrit : un `OutlinedTextField` (le brouillon), un `Button` « Ajouter », une `LazyColumn` (les notes), une icône pour supprimer
- [ ] 2. `data/Note.kt` : `data class Note(val text: String, val createdAt: Long)` — en mémoire, sans Room
- [ ] 3. Un compteur « n notes » au-dessus de la liste (`plurals` dans `strings.xml`)
- [ ] 4. Le brouillon est **conservé à la rotation** (`rememberSaveable`)

Textes visibles dans `res/values/strings.xml`, pas dans le code.

## Validation

- Ajouter 3 notes : la liste se redessine à chaque ajout, le compteur suit
- Commencer une 4e note sans l'ajouter, **tourner le téléphone** : le brouillon est toujours là… et la liste est vide (voir l'échec)
- Android Studio → Running Devices → **Layout Inspector** : l'arbre `Scaffold → Column → Row → OutlinedTextField…` correspond au code

## L'échec à vivre

**Première version, volontairement fausse :**

```kotlin
val notes = remember { mutableListOf<Note>() }   // gardée entre recompositions, mais non observée
Button(onClick = { notes.add(Note(draft, System.currentTimeMillis())) }) { … }
```

Ajouter une note : **l'écran ne bouge pas**. La liste est gardée d'une recomposition à l'autre et contient bien la note (un `Log.d` le montre), mais Compose n'observe pas une `MutableList` : rien ne lui dit de recomposer.

**Deuxième version**, avec `remember { mutableStateListOf<Note>() }` et `var draft by remember { mutableStateOf("") }` : l'écran suit. Tourner le téléphone : **brouillon vide, liste vide**. L'Activity a été détruite et recréée ; `remember` ne survit qu'à la recomposition, pas à une nouvelle instance.

## La réparation

- Pour l'écran : `mutableStateListOf` (une liste **observée** : chaque `add`, `remove` déclenche la recomposition).
- Pour le brouillon : `rememberSaveable { mutableStateOf("") }` — la valeur est écrite dans le `Bundle` de l'Activity par l'OS, et rendue à la nouvelle instance.
- Pour la liste : `rememberSaveable` ne sait sauver que les types du `Bundle` (primitifs, `String`, `Parcelable`…). Deux voies : un `Saver` qui transforme la liste en quelque chose de sauvable (montré ci-dessous, suffisant pour le TD) — ou sortir l'état de l'écran, dans un **ViewModel** : c'est le TD A2.

## Aide

```kotlin
// NotesListScreen.kt — l'état, dans le composable (TD A1)
val notes = remember { mutableStateListOf<Note>() }      // observée ; perdue à la rotation
var draft by rememberSaveable { mutableStateOf("") }    // survit à la rotation

OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("Nouvelle note") })
Button(onClick = { notes.add(0, Note(draft.trim(), System.currentTimeMillis())); draft = "" },
       enabled = draft.isNotBlank()) { Text("Ajouter") }

Text(pluralStringResource(R.plurals.notes_count, notes.size, notes.size))

LazyColumn {
    items(notes) { note ->
        ListItem(headlineContent = { Text(note.text) },
                 trailingContent = { IconButton(onClick = { notes.remove(note) }) { Icon(Icons.Default.Delete, null) } })
    }
}
```

```xml
<!-- strings.xml -->
<plurals name="notes_count">
    <item quantity="one">%d note</item>
    <item quantity="other">%d notes</item>
</plurals>
```

```kotlin
// Bonus : un Saver pour la liste (les textes seulement, séparés par un caractère de contrôle)
val notes = rememberSaveable(saver = listSaver(
    save = { it.map { n -> "${n.createdAt}\u0000${n.text}" } },
    restore = { it.map { s -> val (t, x) = s.split("\u0000", limit = 2); Note(x, t.toLong()) }.toMutableStateList() },
)) { mutableStateListOf<Note>() }
```

Imports : `androidx.compose.runtime.*`, `androidx.compose.runtime.saveable.rememberSaveable`, `androidx.compose.foundation.lazy.LazyColumn`, `androidx.compose.foundation.lazy.items`, `androidx.compose.material3.*`, `androidx.compose.material.icons.Icons`, `androidx.compose.material.icons.filled.Delete`.

## Commandes

```bash
# Rotation sans toucher l'écran (émulateur ou téléphone, rotation auto activée)
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1      # paysage
adb shell settings put system user_rotation 0      # portrait

# Logcat en ligne de commande, filtré sur le tag du cours
adb logcat -s Carnet
```

**Corrigé :** `corrige/` → `android/etapes/A1/` (projet entier, `DIFF.md` liste les fichiers touchés).
