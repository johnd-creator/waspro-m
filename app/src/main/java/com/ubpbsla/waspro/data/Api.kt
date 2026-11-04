package com.ubpbsla.waspro.data

import kotlinx.serialization.json.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import java.math.BigDecimal

val wireJson = Json { ignoreUnknownKeys = true; explicitNulls = false }
fun JsonElement.objectOrEmpty(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())
fun JsonObject.text(key: String): String = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
fun JsonObject.obj(key: String): JsonObject = get(key)?.objectOrEmpty() ?: JsonObject(emptyMap())
fun JsonObject.array(key: String): JsonArray = get(key) as? JsonArray ?: JsonArray(emptyList())
fun JsonObject.actions(): Set<String> = array("allowed_actions").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.toSet()
fun jsonObject(values: Map<String, String>): JsonObject = buildJsonObject {
    values.forEach { (key, value) ->
        if (value.isNotBlank()) put(key, if (value == "true" || value == "false") JsonPrimitive(value.toBoolean()) else JsonPrimitive(value))
    }
}

interface MobileApi {
    @Streaming @GET suspend fun get(@Url path: String, @Header("Authorization") token: String?, @QueryMap filters: Map<String, String>): Response<ResponseBody>
    @HTTP(method = "POST", hasBody = true) suspend fun post(@Url path: String, @Header("Authorization") token: String?, @Header("Idempotency-Key") key: String?, @Body body: RequestBody): Response<ResponseBody>
    @HTTP(method = "PUT", hasBody = true) suspend fun put(@Url path: String, @Header("Authorization") token: String?, @Header("Idempotency-Key") key: String?, @Body body: RequestBody): Response<ResponseBody>
    @HTTP(method = "PATCH", hasBody = true) suspend fun patch(@Url path: String, @Header("Authorization") token: String?, @Body body: RequestBody): Response<ResponseBody>
    @DELETE suspend fun delete(@Url path: String, @Header("Authorization") token: String?, @Header("Idempotency-Key") key: String?): Response<ResponseBody>
    @Multipart @POST suspend fun upload(@Url path: String, @Header("Authorization") token: String?, @Header("Idempotency-Key") key: String?, @Part fields: List<MultipartBody.Part>): Response<ResponseBody>
}

data class ApiFailure(val status: Int, override val message: String, val fields: Map<String, String> = emptyMap()) : Exception(message)
data class ApiPage(val data: JsonElement, val meta: JsonObject)

fun decodeResponse(status: Int, body: String): ApiPage {
    val envelope = runCatching { wireJson.parseToJsonElement(body).objectOrEmpty() }.getOrNull()
    if (status !in 200..299 || envelope?.get("success") != JsonPrimitive(true)) {
        val fields = envelope?.obj("errors")?.mapValues { (_, errors) ->
            (errors as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }?.joinToString("\n").orEmpty()
        }.orEmpty()
        throw ApiFailure(status, envelope?.text("message")?.takeIf(String::isNotBlank)
            ?: "Layanan belum dapat diakses. Coba kembali.", fields)
    }
    return ApiPage(envelope["data"] ?: JsonNull, envelope.obj("meta"))
}

data class UserContext(val raw: JsonObject) {
    val user = raw.obj("user")
    val id = user.text("id")
    val organization = raw.obj("organization")
    val permissions = raw.array("permissions").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.toSet()
    fun can(permission: String) = permission in permissions
    val canReview get() = permissions.any { it.endsWith(".approve") || it.endsWith(".reject") }
}

data class WasteRecordDto(val id: String, val number: String, val quantity: BigDecimal, val raw: JsonObject) {
    companion object {
        fun from(value: JsonObject) = WasteRecordDto(value.text("id"), value.text("record_number"), value.text("quantity").toBigDecimal(), value)
    }
}
data class HaulingBacklogDto(val id: String, val remaining: BigDecimal, val reserved: BigDecimal) {
    companion object {
        fun from(value: JsonObject) = HaulingBacklogDto(value.text("id"), value.text("remaining_quantity").toBigDecimal(), value.text("reserved_quantity").toBigDecimal())
    }
}
data class HaulingRequestDto(val id: String, val recordId: String, val status: String) {
    companion object {
        fun from(value: JsonObject) = HaulingRequestDto(value.text("id"), value.obj("waste_record").text("id"), value.text("status"))
    }
}
data class FabaMovementDto(val id: String, val material: String, val approvalStatus: String) {
    companion object {
        fun from(value: JsonObject) = FabaMovementDto(value.text("id"), value.text("material_type"), value.text("approval_status"))
    }
}

fun normalizedDecimal(input: String): String? {
    val cleaned = input.trim().replace(" ", "")
    val canonical = if (',' in cleaned) cleaned.replace(".", "").replace(',', '.') else cleaned
    return canonical.toBigDecimalOrNull()?.toPlainString()
}
