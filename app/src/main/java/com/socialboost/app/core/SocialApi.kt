package com.socialboost.app.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.socialboost.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(val code: Int, explanation: String) : IOException(explanation)

/**
 * Uses the existing SocialBoost FastAPI routes, not a second authentication or a mock server.
 * All requests are on Dispatchers.IO. Backend controls data visibility and workspace isolation.
 */
class SocialApi(private val context: Context, private val session: SecureSession) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun request(
        path: String,
        method: String = "GET",
        data: JSONObject? = null,
        withAuth: Boolean = true,
    ): Any? = withContext(Dispatchers.IO) {
        require(path.startsWith("/") && !path.startsWith("//")) { "Path must be API-relative" }
        val body = if (method == "GET" || method == "DELETE") null
            else (data ?: JSONObject()).toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val builder = Request.Builder()
            .url(BuildConfig.API_BASE_URL + path)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-store")
            .method(method, body)
        if (withAuth) {
            val token = session.token() ?: throw ApiException(401, "Sign in to continue.")
            builder.header("Authorization", "Bearer " + token)
        }
        execute(builder.build())
    }

    suspend fun upload(path: String, uri: Uri): Any? = withContext(Dispatchers.IO) {
        val token = session.token() ?: throw ApiException(401, "Sign in to continue.")
        val resolver = context.contentResolver
        val name = getFileName(uri)
        val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else -1L
        } ?: -1L
        if (size > 200L * 1024 * 1024) throw IOException("Maximum upload size is 200 MB.")
        val streamBody = object : RequestBody() {
            override fun contentType() = ("application/octet-stream").toMediaTypeOrNull()
            override fun contentLength(): Long = size
            override fun writeTo(sink: BufferedSink) {
                resolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        sink.write(buffer, 0, count)
                    }
                } ?: throw IOException("Cannot open selected file.")
            }
        }
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", name, streamBody).build()
        val request = Request.Builder().url(BuildConfig.API_BASE_URL + path)
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .post(multipart).build()
        execute(request)
    }

    private fun getFileName(uri: Uri): String {
        val raw = context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        return (raw ?: "upload.mp4").substringAfterLast('/').replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun execute(request: Request): Any? {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = try {
                    val value = JSONObject(body).opt("detail")
                    when (value) {
                        is String -> value
                        is JSONArray -> value.toString()
                        else -> "Request failed (" + response.code + ")."
                    }
                } catch (_: Exception) {
                    "Request failed (" + response.code + ")."
                }
                throw ApiException(response.code, detail)
            }
            if (body.isBlank()) return null
            return JSONTokener(body).nextValue()
        }
    }
}

fun Any?.asObject(): JSONObject = this as? JSONObject ?: JSONObject()
fun Any?.asRows(): List<JSONObject> {
    val array = this as? JSONArray ?: return emptyList()
    return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
}
fun JSONObject.value(key: String, default: String = "Unavailable"): String {
    if (!has(key) || isNull(key)) return default
    val raw = opt(key)
    if (raw is JSONObject || raw is JSONArray) return raw.toString()
    return raw?.toString()?.takeIf { it.isNotBlank() } ?: default
}
fun JSONObject.numberLabel(key: String): String {
    if (!has(key) || isNull(key)) return "Unavailable"
    val n = opt(key)
    return if (n is Number) java.text.NumberFormat.getNumberInstance().format(n) else value(key)
}
