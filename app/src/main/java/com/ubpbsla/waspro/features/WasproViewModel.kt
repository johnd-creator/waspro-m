package com.ubpbsla.waspro.features

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ubpbsla.waspro.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ScreenData(
    val data: JsonElement = JsonNull, val meta: JsonObject = JsonObject(emptyMap()),
    val loading: Boolean = false, val failure: String? = null, val cachedAt: Long? = null,
    val loadedAt: Long = System.currentTimeMillis(),
)
data class EditorState(
    val module: String, val draft: Draft?, val values: Map<String, String>,
    val errors: Map<String, String> = emptyMap(), val existingAttachment: Boolean = false,
    val saved: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WasproViewModel @Inject constructor(
    val repository: WasproRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val user = repository.identity.asStateFlow()
    val starting = MutableStateFlow(true)
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val screens = MutableStateFlow<Map<String, ScreenData>>(emptyMap())
    val editor = MutableStateFlow<EditorState?>(null)
    val conflict = MutableStateFlow<JsonObject?>(null)
    val lookups = MutableStateFlow(JsonObject(emptyMap()))
    val drafts = user.flatMapLatest { identity ->
        if (identity == null) flowOf(emptyList()) else repository.database.dao().drafts(repository.scope)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val draftMutex = Mutex()
    private var saving: Job? = null

    init {
        viewModelScope.launch {
            try { repository.restore() }
            catch (failure: Exception) { message.value = readable(failure) }
            finally { starting.value = false }
        }
    }
    private fun readable(failure: Exception) = when (failure) {
        is ApiFailure -> failure.message
        is java.io.IOException -> "Koneksi terputus. Periksa jaringan lalu coba kembali."
        else -> failure.message ?: "Terjadi kesalahan. Coba kembali."
    }
    private fun runOperation(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try { block() }
            catch (failure: CancellationException) { throw failure }
            catch (failure: Exception) {
                message.value = readable(failure)
                if (failure is ApiFailure) {
                    editor.value = editor.value?.copy(errors = failure.fields)
                    if (failure.status == 403) runCatching { repository.refreshIdentity() }
                }
            } finally { busy.value = false }
        }
    }
    fun login(email: String, password: String) = runOperation {
        repository.login(email, password)
        screens.value = emptyMap()
        lookups.value = JsonObject(emptyMap())
        editor.value = null
    }
    fun logout() = runOperation {
        saving?.cancelAndJoin()
        persistEditor()
        draftMutex.withLock { repository.logout() }
        lookups.value = JsonObject(emptyMap())
        screens.value = emptyMap()
        editor.value = null
    }
    fun load(key: String, path: String, filters: Map<String, String> = emptyMap(), append: Boolean = false) {
        screens.value = screens.value + (key to (screens.value[key] ?: ScreenData()).copy(loading = true, failure = null))
        viewModelScope.launch {
            val identityId = user.value?.id
            try {
                val result = repository.load(path, filters)
                if (user.value?.id != identityId) return@launch
                val previous = screens.value[key]?.data as? JsonArray
                val items = result.page.data as? JsonArray
                val data = if (append && previous != null && items != null) JsonArray((previous + items).distinctBy {
                    it.objectOrEmpty().text("id").ifBlank { it.toString() }
                }) else result.page.data
                screens.value = screens.value + (key to ScreenData(data, result.page.meta, cachedAt = result.cachedAt))
            } catch (failure: CancellationException) { throw failure }
            catch (failure: Exception) {
                screens.value = screens.value + (key to (screens.value[key] ?: ScreenData()).copy(loading = false, failure = readable(failure)))
                if (failure is ApiFailure && failure.status == 403) runCatching { repository.refreshIdentity() }
            }
        }
    }
    private suspend fun refreshLookups() {
        val requestScope = repository.scope
        val base = repository.load("lookups").page.data.objectOrEmpty()
        val faba = if (user.value?.permissions?.any { it.startsWith("faba_") } == true)
            repository.load("faba/options").page.data.objectOrEmpty() else JsonObject(emptyMap())
        val movementTypes = listOf("production_movement_types", "utilization_movement_types", "adjustment_movement_types")
            .flatMap { faba.array(it) }.distinct()
        if (requestScope != repository.scope) throw CancellationException("Konteks organisasi berubah")
        lookups.value = JsonObject(base + faba + ("movement_types" to JsonArray(movementTypes)))
    }
    fun loadOptions() {
        viewModelScope.launch {
            try { refreshLookups() }
            catch (failure: CancellationException) { throw failure }
            catch (failure: Exception) { message.value = readable(failure) }
        }
    }
    fun openEditor(module: ModuleSpec, record: JsonObject? = null, savedDraft: Draft? = null, initial: Map<String, String> = emptyMap()) {
        saving?.cancel()
        editor.value = null
        conflict.value = null
        runOperation {
            if (module.fields.any { it.lookup != null }) refreshLookups()
            val values = savedDraft?.let { wireJson.parseToJsonElement(it.payload).jsonObject.mapValues { entry -> entry.value.jsonPrimitive.content } }
                ?: module.fields.associate { field ->
                    field.key to (record?.text(field.key)?.takeIf(String::isNotBlank) ?: when {
                        field.kind == FieldKind.Boolean -> "true"
                        field.key in listOf("date", "transaction_date", "hauling_date") -> LocalDate.now().toString()
                        field.key == "year" -> LocalDate.now().year.toString()
                        field.key == "month" -> LocalDate.now().monthValue.toString()
                        field.key == "unit" && module.key in listOf("production", "utilization", "adjustments") -> "ton"
                        else -> ""
                    })
                } + initial + mapOf("_has_attachment" to (savedDraft?.let { wireJson.parseToJsonElement(it.payload).jsonObject.text("_has_attachment") }
                    ?: (record?.text("attachment_path")?.isNotBlank() == true).toString()))
            val persistent = module !in listOf(Modules.password, Modules.profile)
            val draft = savedDraft ?: if (persistent) Draft(
                repository.scope, UUID.randomUUID().toString(), module.key, record?.text("id")?.takeIf(String::isNotBlank),
                jsonObject(values).toString(), null, UUID.randomUUID().toString(),
                record?.text("resource_version")?.takeIf(String::isNotBlank), System.currentTimeMillis(),
            ) else null
            editor.value = EditorState(module.key, draft, values, existingAttachment = values["_has_attachment"] == "true")
        }
    }
    fun change(key: String, value: String) {
        val current = editor.value ?: return
        if (current.draft?.submittedPayload != null) return
        editor.value = current.copy(values = current.values + (key to value), errors = current.errors - key, saved = false)
        saving?.cancel()
        saving = viewModelScope.launch { delay(250); persistEditor() }
    }
    suspend fun persistEditor() = draftMutex.withLock {
        val current = editor.value ?: return@withLock
        val draft = current.draft ?: return@withLock
        val updated = draft.copy(payload = jsonObject(current.values).toString(), updatedAt = System.currentTimeMillis())
        repository.database.dao().save(updated)
        if (editor.value == current) editor.value = current.copy(draft = updated, saved = true)
    }
    fun attach(uri: Uri) = runOperation {
        val current = editor.value ?: return@runOperation
        val draft = current.draft ?: return@runOperation
        val mime = context.contentResolver.getType(uri)
        val extension = mapOf("application/pdf" to "pdf", "image/jpeg" to "jpg", "image/png" to "png", "image/webp" to "webp")[mime]
            ?: throw IllegalArgumentException("Pilih JPG, PNG, WEBP atau PDF.")
        val folder = repository.draftDirectory.apply { mkdirs() }
        val file = folder.resolve("${draft.id}.$extension")
        try {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0
                    var count = input.read(buffer)
                    while (count != -1) {
                        total += count
                        require(total <= 5 * 1024 * 1024) { "Ukuran lampiran maksimal 5 MB." }
                        output.write(buffer, 0, count)
                        count = input.read(buffer)
                    }
                }
            }
        } catch (failure: Exception) { file.delete(); throw failure }
        current.draft.attachment?.takeIf { it != file.path }?.let { File(it).delete() }
        editor.value = current.copy(draft = draft.copy(attachment = file.path), errors = current.errors - "attachment")
        persistEditor()
    }
    fun submitEditor(onSuccess: (JsonObject) -> Unit) = runOperation {
        saving?.cancel()
        val current = editor.value ?: return@runOperation
        val module = Modules.find(current.module)
        val errors = validateForm(module, current.values, current.draft?.attachment != null || current.existingAttachment)
        if (errors.isNotEmpty()) {
            editor.value = current.copy(errors = errors)
            return@runOperation
        }
        persistEditor()
        repository.refreshIdentity()
        val actor = user.value ?: return@runOperation
        if (current.draft?.recordId == null && module.createPermission != null) require(actor.can(module.createPermission)) { "Izin input sudah berubah." }
        var draft = editor.value?.draft
        if (draft?.recordId != null && draft.submittedPayload == null) {
            val latest = repository.request("${module.path}/${draft.recordId}").data.jsonObject
            require("update" in latest.actions()) { "Data sudah tidak dapat diubah. Muat ulang detail." }
            if (draft.sourceVersion != null && latest.text("resource_version") != draft.sourceVersion) {
                conflict.value = latest
                throw ApiFailure(409, "Data berubah di server. Tinjau data terbaru sebelum mengirim draft.")
            }
        }
        val canonical = current.values.toMutableMap()
        module.fields.filter { it.kind == FieldKind.Decimal || it.kind == FieldKind.Integer }.forEach { field ->
            canonical[field.key]?.takeIf(String::isNotBlank)?.let { canonical[field.key] = normalizedDecimal(it)!! }
        }
        val payload = buildJsonObject {
            module.fields.forEach { field ->
                val value = canonical[field.key].orEmpty()
                put(field.key, when {
                    value.isBlank() -> JsonNull
                    field.kind == FieldKind.Boolean -> JsonPrimitive(value.toBoolean())
                    else -> JsonPrimitive(value)
                })
            }
            draft?.sourceVersion?.let { put("_source_version", it) }
        }
        if (draft != null && draft.submittedPayload == null) {
            val hash = draft.attachment?.let { fileHash(File(it)) }
            draft = draft.copy(submittedPayload = payload.toString(), submittedAttachmentHash = hash)
            repository.database.dao().save(draft)
            editor.value = editor.value?.copy(draft = draft)
        }
        val file = draft?.attachment?.let(::File)
        if (file != null) require(fileHash(file) == draft.submittedAttachmentHash) { "Lampiran berubah. Periksa draft." }
        val path = module.path + (draft?.recordId?.let { "/$it" } ?: "")
        val method = when {
            module == Modules.profile -> "PATCH"
            module == Modules.password || draft?.recordId != null -> "PUT"
            else -> "POST"
        }
        try {
            val result = repository.mutate(path, method, draft?.submittedPayload?.let { wireJson.parseToJsonElement(it).jsonObject } ?: payload, draft?.requestKey, file)
            if (module == Modules.profile) repository.refreshIdentity()
            draft?.let {
                repository.database.dao().deleteDraft(it.scope, it.id)
                it.attachment?.let { path -> File(path).delete() }
            }
            editor.value = null
            screens.value = emptyMap()
            message.value = "Data berhasil disimpan."
            onSuccess(result.data.objectOrEmpty())
        } catch (failure: ApiFailure) {
            if (failure.status in listOf(403, 409, 422)) {
                draft?.let {
                    val unlocked = it.copy(submittedPayload = null, submittedAttachmentHash = null)
                    repository.database.dao().save(unlocked)
                    editor.value = editor.value?.copy(draft = unlocked)
                }
            }
            if (failure.status == 409 && draft?.recordId != null) {
                conflict.value = runCatching { repository.request(path).data.objectOrEmpty() }.getOrNull()
            }
            throw failure
        }
    }
    fun acceptLatestVersion() = runOperation {
        val latest = conflict.value ?: return@runOperation
        require("update" in latest.actions()) { "Data server sudah tidak dapat diubah." }
        val current = editor.value ?: return@runOperation
        val draft = current.draft ?: return@runOperation
        val rebased = draft.copy(sourceVersion = latest.text("resource_version"), submittedPayload = null, submittedAttachmentHash = null)
        editor.value = current.copy(draft = rebased)
        repository.database.dao().save(rebased)
        conflict.value = null
    }
    fun removeDraft(draft: Draft) = runOperation {
        repository.database.dao().deleteDraft(draft.scope, draft.id)
        draft.attachment?.let { File(it).delete() }
    }
    fun action(path: String, action: String, fields: Map<String, String>, requestKey: String, completed: () -> Unit) = runOperation {
        repository.refreshIdentity()
        repository.mutate(path, if (action == "delete") "DELETE" else "POST", jsonObject(fields), requestKey)
        screens.value = emptyMap()
        message.value = "Tindakan berhasil diproses."
        completed()
    }
    fun download(path: String, filters: Map<String, String>, name: String, open: (File) -> Unit) = runOperation {
        repository.refreshIdentity()
        open(repository.download(path, filters, name))
    }
    private fun fileHash(file: File): String = MessageDigest.getInstance("SHA-256").let { digest ->
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var count = input.read(buffer)
            while (count != -1) { digest.update(buffer, 0, count); count = input.read(buffer) }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}
