package corsica.universita.carnet.ui.permissions

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val TAG = "Carnet"

/** Une permission runtime n'est pas un booléen : quatre états, et le dernier est sans retour depuis l'app. */
enum class PermissionState { NotAsked, Granted, DeniedOnce, DeniedForever }

/**
 * La machine à états d'une permission runtime (TD A7). Ce que l'OS expose :
 *  - `checkSelfPermission` : accordée ou non, maintenant ;
 *  - `shouldShowRequestPermissionRationale` : `true` après un premier refus (expliquer, puis redemander) ;
 *    `false` avant toute demande… et aussi après le refus définitif. D'où `askedBefore`, mémorisé.
 * Refusée deux fois sans explication, la permission ne sera plus jamais demandée : le launcher répond
 * `false` sans dialogue. La seule issue est l'écran des réglages de l'app.
 */
class PermissionFlow(
    private val activity: Activity,
    val permissions: List<String>,
    /** `false` : une seule suffit (localisation : précise OU approximative). */
    private val requireAll: Boolean,
    private val askedBefore: () -> Boolean,
    private val markAsked: () -> Unit,
) {
    var state by mutableStateOf(compute(afterResult = false))
        private set

    private var launcher: ActivityResultLauncher<Array<String>>? = null

    /** Recalculé à chaque retour au premier plan : l'utilisateur a pu révoquer dans les réglages. */
    fun refresh() {
        state = compute(afterResult = false)
    }

    /** Demander : un dialogue système, ou rien du tout si c'est déjà refusé pour de bon. */
    fun request() {
        markAsked()
        Log.d(TAG, "Permission demandée : $permissions (état avant : $state)")
        launcher?.launch(permissions.toTypedArray())
    }

    /** L'écran des réglages de Carnet : la seule issue après un refus définitif. */
    fun openSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)),
        )
    }

    internal fun attach(launcher: ActivityResultLauncher<Array<String>>) {
        this.launcher = launcher
    }

    internal fun onResult(result: Map<String, Boolean>) {
        state = compute(afterResult = true)
        Log.d(TAG, "Résultat : $result → $state")
    }

    private fun granted(): Boolean {
        val each = permissions.map {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
        return if (requireAll) each.all { it } else each.any { it }
    }

    private fun rationale(): Boolean =
        permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

    private fun compute(afterResult: Boolean): PermissionState = when {
        granted() -> PermissionState.Granted
        rationale() -> PermissionState.DeniedOnce          // refusée une fois : expliquer, puis redemander
        askedBefore() || afterResult -> PermissionState.DeniedForever   // déjà demandée, plus de rationale : fini
        else -> PermissionState.NotAsked
    }
}

/**
 * Le flux, dans un écran Compose : le launcher est rattaché, l'état est recalculé au retour au premier
 * plan (révocation dans les réglages), « déjà demandée » survit à la rotation et au process.
 */
@Composable
fun rememberPermissionFlow(vararg permissions: String, requireAll: Boolean = true): PermissionFlow {
    val activity = LocalActivity.current ?: error("PermissionFlow exige une Activity")
    var asked by rememberSaveable(permissions.joinToString()) { mutableStateOf(false) }
    val flow = remember(permissions.joinToString()) {
        PermissionFlow(activity, permissions.toList(), requireAll, askedBefore = { asked }, markAsked = { asked = true })
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        flow.onResult(it)
    }
    flow.attach(launcher)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, flow) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) flow.refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return flow
}
