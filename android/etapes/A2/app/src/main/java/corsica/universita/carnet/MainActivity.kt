package corsica.universita.carnet

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LifecycleEventObserver
import corsica.universita.carnet.ui.CarnetNavigation
import corsica.universita.carnet.ui.theme.CarnetTheme

private const val TAG = "Carnet"

/** L'unique Activity de Carnet : un écran, lancé par l'OS sur l'intent MAIN / LAUNCHER. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // avant super.onCreate : l'écran de démarrage, puis Theme.Carnet.Main
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // TD A2 : observer le cycle de vie. Filtre Logcat : tag:Carnet
        // savedInstanceState est null au premier lancement, non null après une rotation ou une
        // mort du process : l'OS rend ce qu'il avait sauvé (Bundle).
        Log.d(TAG, "onCreate — instance ${System.identityHashCode(this)}, état restauré : ${savedInstanceState != null}")
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                Log.d(TAG, "Activity ${event.name} — instance ${System.identityHashCode(this)}")
            },
        )

        setContent {
            CarnetTheme {
                CarnetNavigation()
            }
        }
    }
}
