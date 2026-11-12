package com.ubpbsla.waspro.features

import com.ubpbsla.waspro.data.UserContext
import com.ubpbsla.waspro.data.normalizedDecimal
import java.time.LocalDate

enum class FieldKind { Text, Decimal, Integer, Date, Choice, Boolean, Password }
data class InputField(
    val key: String, val label: String, val required: Boolean = false,
    val kind: FieldKind = FieldKind.Text, val choices: List<String> = emptyList(), val lookup: String? = null,
)
data class ModuleSpec(
    val key: String, val title: String, val path: String, val permission: String,
    val fields: List<InputField> = emptyList(), val createPermission: String? = null,
    val paged: Boolean = false, val filters: List<String> = emptyList(),
)
private fun date(key: String = "transaction_date", label: String = "Tanggal") = InputField(key, label, true, FieldKind.Date)
private fun quantity() = InputField("quantity", "Jumlah", true, FieldKind.Decimal)
private fun choice(key: String, label: String, lookup: String, required: Boolean = true) =
    InputField(key, label, required, FieldKind.Choice, lookup = lookup)
private val fabaFields = listOf(date(), choice("material_type", "Material", "materials"), quantity(), InputField("note", "Catatan"))
private val masterFields = listOf(InputField("name", "Nama", true), InputField("code", "Kode", true), InputField("description", "Deskripsi"), InputField("is_active", "Aktif", kind = FieldKind.Boolean))
private val periodFilters = listOf("year", "month", "material_type", "movement_type")

object Modules {
    val waste = ModuleSpec("waste", "Catatan limbah", "waste-records", "waste_records.view_own", listOf(
        date("date"), choice("waste_type_id", "Jenis limbah", "waste_types"), quantity(),
        InputField("unit", "Satuan", true), InputField("source", "Sumber / lokasi"),
        InputField("description", "Deskripsi"), InputField("notes", "Catatan"),
    ), "waste_records.create", true, listOf("search", "status"))
    val hauling = ModuleSpec("hauling", "Pengangkutan", "waste-haulings", "waste_hauling.view_own",
        listOf(InputField("waste_record_id", "Catatan limbah", true), date("hauling_date", "Tanggal angkut"), quantity(), InputField("notes", "Catatan")),
        "waste_hauling.create")
    val production = ModuleSpec("production", "Produksi FABA", "faba/production", "faba_production.view",
        fabaFields + choice("movement_type", "Tipe entri", "production_movement_types"), "faba_production.create", true, periodFilters)
    val utilization = ModuleSpec("utilization", "Pemanfaatan FABA", "faba/utilization", "faba_utilization.view",
        fabaFields + listOf(choice("movement_type", "Jenis pemanfaatan", "utilization_movement_types"),
            choice("vendor_id", "Vendor", "vendors", false),
            choice("internal_destination_id", "Tujuan internal", "internal_destinations", false),
            choice("purpose_id", "Tujuan penggunaan", "purposes", false),
            InputField("document_number", "Nomor dokumen"),
            InputField("document_date", "Tanggal dokumen", kind = FieldKind.Date)),
        "faba_utilization.create", true, periodFilters)
    val adjustments = ModuleSpec("adjustments", "Penyesuaian FABA", "faba/adjustments", "faba_adjustments.view",
        fabaFields.filter { it.key != "note" } + listOf(choice("movement_type", "Jenis penyesuaian", "adjustment_movement_types"), InputField("note", "Alasan penyesuaian", true)),
        "faba_adjustments.create", true, periodFilters)
    val periods = ModuleSpec("periods", "Persetujuan bulanan", "faba/approvals", "faba_approvals.view", filters = listOf("year"))
    val history = ModuleSpec("approval-history", "Riwayat FABA", "faba/approvals/history", "faba_approvals.view")
    val profile = ModuleSpec("profile", "Profil", "profile", "", listOf(InputField("name", "Nama", true), InputField("email", "Email", true)))
    val password = ModuleSpec("password", "Ganti password", "profile/password", "", listOf(
        InputField("current_password", "Password saat ini", true, FieldKind.Password),
        InputField("password", "Password baru", true, FieldKind.Password),
        InputField("password_confirmation", "Konfirmasi password baru", true, FieldKind.Password)))
    val opening = ModuleSpec("opening", "Saldo awal", "faba/recaps/opening-balance", "faba_opening_balance.manage",
        listOf(InputField("year", "Tahun", true, FieldKind.Integer), InputField("month", "Bulan", true, FieldKind.Integer),
            choice("material_type", "Material", "materials"), quantity(), InputField("note", "Catatan")), "faba_opening_balance.manage")
    val capacity = ModuleSpec("capacity", "Kapasitas TPS", "faba/recaps/tps-capacity", "faba_opening_balance.manage",
        listOf(choice("material_type", "Material", "materials"), InputField("capacity", "Kapasitas ton", true, FieldKind.Decimal),
            InputField("warning_threshold", "Ambang warning (%)", true, FieldKind.Decimal),
            InputField("critical_threshold", "Ambang critical (%)", true, FieldKind.Decimal)), "faba_opening_balance.manage")
    val categories = ModuleSpec("categories", "Kategori limbah", "master-data/categories", "waste_categories.view", masterFields, "waste_categories.create", true)
    val characteristics = ModuleSpec("characteristics", "Karakteristik limbah", "master-data/characteristics", "waste_characteristics.view",
        masterFields + InputField("is_hazardous", "Berbahaya", kind = FieldKind.Boolean), "waste_characteristics.create", true)
    val types = ModuleSpec("types", "Jenis limbah", "master-data/types", "waste_types.view", masterFields + listOf(
        choice("category_id", "Kategori", "waste_categories"), choice("characteristic_id", "Karakteristik", "waste_characteristics"),
        InputField("storage_period_days", "Masa penyimpanan (hari)", true, FieldKind.Integer),
        InputField("transport_cost", "Biaya pengangkutan", true, FieldKind.Decimal)), "waste_types.create", true)
    val vendors = ModuleSpec("vendors", "Vendor", "master-data/vendors", "vendors.view", masterFields + listOf(
        InputField("contact_person", "Narahubung"), InputField("phone", "Telepon"), InputField("email", "Email"),
        InputField("address", "Alamat"), InputField("license_number", "Nomor izin"), InputField("license_expiry_date", "Berlaku sampai", kind = FieldKind.Date)),
        "vendors.create", true)
    val recaps = listOf("monthly" to "Rekap bulanan", "yearly" to "Rekap tahunan", "vendors" to "Rekap vendor", "balance" to "Saldo FABA", "stock-card" to "Kartu stok").map {
        ModuleSpec("recap-${it.first}", it.second, "faba/recaps/${it.first}", "faba_recaps.view", filters = listOf("year", "month", "material_type", "vendor_id"))
    }
    val all = listOf(waste, hauling, production, utilization, adjustments, periods, history, profile, password, opening, capacity, categories, characteristics, types, vendors) +
        recaps + listOf(ModuleSpec("users", "Pengguna", "users", "users.view"), ModuleSpec("roles", "Role dan izin", "roles", "roles.view"))
    fun find(key: String) = all.first { it.key == key }
    fun visible(module: ModuleSpec, user: UserContext): Boolean = module.permission.isBlank() || user.can(module.permission) ||
        (module == waste && user.can("waste_records.view_all")) || (module == hauling && user.can("waste_hauling.view_all"))
}

/** One definition drives visible fields, required labels, validation and submitted payload. */
fun isApplicableField(module: ModuleSpec, field: InputField, values: Map<String, String>): Boolean =
    module != Modules.utilization || when (field.key) {
        "vendor_id", "document_number", "document_date" -> values["movement_type"] == "utilization_external"
        "internal_destination_id" -> values["movement_type"] != "utilization_external"
        else -> true
    }

fun effectiveField(module: ModuleSpec, field: InputField, values: Map<String, String>): InputField =
    if (module == Modules.utilization && field.key in listOf("vendor_id", "document_number", "document_date", "internal_destination_id"))
        field.copy(required = isApplicableField(module, field, values)) else field

fun validateForm(module: ModuleSpec, values: Map<String, String>, hasAttachment: Boolean): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    module.fields.filter { isApplicableField(module, it, values) }.map { effectiveField(module, it, values) }.forEach { field ->
        val value = values[field.key].orEmpty().trim()
        if (field.required && value.isBlank()) errors[field.key] = "${field.label} wajib diisi."
        if (value.isNotBlank() && field.kind in listOf(FieldKind.Decimal, FieldKind.Integer)) {
            val number = normalizedDecimal(value)?.toBigDecimalOrNull()
            if (number == null || (field.kind == FieldKind.Integer && number.stripTrailingZeros().scale() > 0)) errors[field.key] = "Masukkan angka yang valid."
            else if (number.signum() < 0 || (field.key == "quantity" && module != Modules.waste && module != Modules.opening && number.signum() == 0)) errors[field.key] = "Jumlah tidak valid."
        }
        if (value.isNotBlank() && field.kind == FieldKind.Date) {
            val day = runCatching { LocalDate.parse(value) }.getOrNull()
            if (day == null) errors[field.key] = "Gunakan tanggal YYYY-MM-DD."
            else if (field.key != "hauling_date" && field.key != "license_expiry_date" && day > LocalDate.now()) errors[field.key] = "Tanggal tidak boleh melebihi hari ini."
        }
    }
    if (module == Modules.utilization) {
        val external = values["movement_type"] == "utilization_external"
        val needed = if (external) listOf("vendor_id", "document_number", "document_date") else listOf("internal_destination_id")
        needed.filter { values[it].isNullOrBlank() }.forEach { errors[it] = "Wajib diisi." }
        if (external && !hasAttachment) errors["attachment"] = "Lampiran wajib diunggah."
    }
    if (module == Modules.password && values["password"] != values["password_confirmation"]) errors["password_confirmation"] = "Konfirmasi password tidak sesuai."
    return errors
}

/** Optional filters remain optional; populated periods must be meaningful before a request. */
fun validatePeriodFilters(values: Map<String, String>): Map<String, String> = buildMap {
    values["year"]?.takeIf(String::isNotBlank)?.let { if (it.toIntOrNull() !in 2000..2100) put("year", "Gunakan tahun 2000–2100.") }
    values["month"]?.takeIf(String::isNotBlank)?.let { if (it.toIntOrNull() !in 1..12) put("month", "Gunakan bulan 1–12.") }
}
