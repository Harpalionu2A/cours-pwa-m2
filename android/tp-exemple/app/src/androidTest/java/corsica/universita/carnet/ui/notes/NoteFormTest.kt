package corsica.universita.carnet.ui.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import corsica.universita.carnet.ui.theme.CarnetTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Un test Compose (Jour 6, chapitre 2) : le formulaire seul, dans un hôte de test, sans Activity
 * de l'app. S'exécute sur un appareil ou un émulateur : ./gradlew connectedDebugAndroidTest.
 */
class NoteFormTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun leBoutonAjouterEstInactifTantQueLeBrouillonEstVide() {
        var draft by mutableStateOf("")
        var added = 0
        composeRule.setContent {
            CarnetTheme { NoteForm(draft = draft, onDraftChange = { draft = it }, onAdd = { added++ }) }
        }

        composeRule.onNodeWithText("Ajouter").assertIsNotEnabled()

        composeRule.onNodeWithText("Nouvelle note").performTextInput("Chêne vert marqué")
        composeRule.onNodeWithText("Ajouter").performClick()

        assertEquals("Chêne vert marqué", draft)
        assertEquals(1, added)
    }
}
