package com.ubpbsla.waspro.data

import android.content.Context
import com.ubpbsla.waspro.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.time.Instant
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class LoadedData(val page: ApiPage, val cachedAt: Long? = null)

@Singleton
class WasproRepository @Inject constructor(
    val api: MobileApi,
    val database: LocalStore,
    private val sessions: SessionStore,
    @param:ApplicationContext private val context: Context,
) {
    val identity = MutableStateFlow<UserContext?>(null)
    private var token: String? = null
    private var expires: String = ""
    private var sessionGeneration = 0
    val scope: String get() = listOf(BuildConfig.FLAVOR, identity.value?.organization?.text("id"), identity.value?.id).joinToString("|")
    val draftDirectory: File get() {
        val scopeKey = MessageDigest.getInstance("SHA-256").digest(scope.toByteArray()).joinToString("") { "%02x".format(it) }
        return context.filesDir.resolve("drafts").resolve(scopeKey)
    }
    private val bearer get() = token?.let { "Bearer $it" }

    suspend fun restore() {
        val saved = sessions.read() ?: return
        token = saved.text("token")
        expires = saved.text("expires_at")
        if (runCatching { Instant.parse(expires).isBefore(Instant.now()) }.getOrDefault(true)) {
            expire()
            return
        }
        try { refreshIdentity() }
        catch (failure: IOException) {
            identity.value = UserContext(saved.obj("context"))
        }
    }
    suspend fun login(email: String, password: String) {
        val page = mutate("auth/login", "POST", jsonObject(mapOf(
            "email" to email.trim(), "password" to password,
            "device_name" to "WASPRO Android ${android.os.Build.MODEL}",
        )))
        val session = page.data.jsonObject
        require(session.text("token").isNotBlank()) { "Respons sesi tidak lengkap." }
        token = session.text("token")
        sessionGeneration++
        expires = session.text("expires_at")
        identity.value = UserContext(session.obj("context"))
        sessions.save(session)
    }
    suspend fun refreshIdentity() {
        val updated = request("me").data.jsonObject
        identity.value = UserContext(updated)
        sessions.save(buildJsonObject {
            put("token", token)
            put("expires_at", expires)
            put("context", updated)
        })
    }
    private suspend fun expire() {
        sessionGeneration++
        token = null
        identity.value = null
        sessions.clear()
    }
    suspend fun logout() {
        sessionGeneration++
        val oldScope = scope
        val oldDraftDirectory = draftDirectory
        requestLogout()
        database.dao().clearCache(oldScope)
        database.dao().clearDrafts(oldScope)
        withContext(Dispatchers.IO) { oldDraftDirectory.deleteRecursively() }
        expire()
    }
    private suspend fun requestLogout() {
        // Keep credentials until server confirms revocation, unless it already rejects the token.
        try { mutate("auth/logout", "POST", JsonObject(emptyMap())) }
        catch (failure: ApiFailure) { if (failure.status != 401) throw failure }
    }
    suspend fun request(path: String, filters: Map<String, String> = emptyMap()): ApiPage {
        val credentials = bearer
        val response = api.get(path, credentials, filters)
        if (response.code() == 401 && credentials == bearer) expire()
        return withContext(Dispatchers.IO) {
            decodeResponse(response.code(), (response.body() ?: response.errorBody())?.string().orEmpty())
        }
    }
    suspend fun load(path: String, filters: Map<String, String> = emptyMap()): LoadedData {
        val requestScope = scope
        val generation = sessionGeneration
        val cacheKey = path + "?" + filters.toSortedMap().entries.joinToString("&")
        try {
            val page = request(path, filters)
            if (generation != sessionGeneration) throw CancellationException("Sesi berubah")
            database.dao().cache(CachedResponse(requestScope, cacheKey, buildJsonObject {
                put("data", page.data); put("meta", page.meta)
            }.toString(), System.currentTimeMillis()))
            return LoadedData(page)
        } catch (failure: IOException) {
            val cached = database.dao().cached(requestScope, cacheKey) ?: throw failure
            val body = wireJson.parseToJsonElement(cached.payload).jsonObject
            return LoadedData(ApiPage(body.getValue("data"), body.obj("meta")), cached.savedAt)
        }
    }
    suspend fun mutate(path: String, method: String, data: JsonObject, key: String? = null, file: File? = null): ApiPage {
        val body = data.toString().toRequestBody("application/json".toMediaType())
        val response = when {
            file != null -> {
                val fields = data.map { (name, value) ->
                    MultipartBody.Part.createFormData(name, if (value == JsonNull) "" else value.jsonPrimitive.content)
                }.toMutableList()
                if (method == "PUT") fields += MultipartBody.Part.createFormData("_method", "PUT")
                val mime = when (file.extension.lowercase()) {
                    "pdf" -> "application/pdf"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> "image/jpeg"
                }
                fields += MultipartBody.Part.createFormData("attachment", file.name, file.asRequestBody(mime.toMediaType()))
                api.upload(path, bearer, key, fields)
            }
            method == "PUT" -> api.put(path, bearer, key, body)
            method == "PATCH" -> api.patch(path, bearer, body)
            method == "DELETE" -> api.delete(path, bearer, key)
            else -> api.post(path, bearer, key, body)
        }
        if (response.code() == 401) expire()
        val page = decodeResponse(response.code(), (response.body() ?: response.errorBody())?.string().orEmpty())
        if (path == "profile/password") expire()
        return page
    }
    suspend fun download(path: String, filters: Map<String, String>, name: String): File {
        val response = api.get(path, bearer, filters)
        if (response.code() == 401) expire()
        if (!response.isSuccessful) decodeResponse(response.code(), response.errorBody()?.string().orEmpty())
        val output = context.cacheDir.resolve("exports").apply { mkdirs() }.resolve(name)
        withContext(Dispatchers.IO) {
            response.body()!!.byteStream().use { input -> output.outputStream().use { input.copyTo(it) } }
        }
        return output
    }
}
