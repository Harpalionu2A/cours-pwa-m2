package corsica.universita.carnet.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/** Ce qui part vers le serveur : un DTO, pas l'entité Room (le schéma du réseau n'est pas celui du disque). */
@Serializable
data class NoteDto(val id: Long, val text: String, val createdAt: Long)

/** Ce que l'écho HTTP renvoie (httpbin.org/post) : le corps reçu, dans `json`. Les autres champs sont ignorés. */
@Serializable
data class EchoResponse(val url: String = "", val json: NoteDto? = null)

/**
 * L'API de synchronisation (TD A5) : une interface, des annotations, Retrofit écrit le reste.
 * Base URL : `BuildConfig.SYNC_BASE_URL` (défaut `https://httpbin.org/`, surchargeable dans local.properties).
 */
interface SyncApi {
    @POST("post")
    suspend fun push(@Body body: NoteDto): EchoResponse
}
