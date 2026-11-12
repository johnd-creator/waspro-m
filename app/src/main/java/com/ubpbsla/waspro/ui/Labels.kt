package com.ubpbsla.waspro.ui

val labels = mapOf(
    "record_number" to "Nomor catatan", "display_number" to "Nomor transaksi", "hauling_number" to "Nomor angkut",
    "date" to "Tanggal", "transaction_date" to "Tanggal transaksi", "hauling_date" to "Tanggal angkut",
    "quantity" to "Jumlah", "unit" to "Satuan", "status" to "Status", "status_label" to "Status",
    "approval_status" to "Persetujuan", "operational_status_label" to "Kondisi operasional", "period_status" to "Status periode",
    "effective_status" to "Status efektif", "locked" to "Terkunci", "name" to "Nama", "email" to "Email",
    "source" to "Sumber / lokasi", "description" to "Deskripsi", "notes" to "Catatan", "note" to "Catatan",
    "rejection_reason" to "Alasan penolakan", "rejection_note" to "Alasan penolakan", "approval_notes" to "Catatan persetujuan",
    "approved_at" to "Disetujui pada", "submitted_at" to "Diajukan pada", "rejected_at" to "Ditolak pada",
    "created_at" to "Dibuat pada", "updated_at" to "Diperbarui pada", "expiry_date" to "Batas penyimpanan",
    "expiry_status" to "Status penyimpanan", "approved_hauled_quantity" to "Sudah diangkut", "remaining_quantity" to "Sisa",
    "reserved_quantity" to "Direservasi", "available_quantity" to "Tersedia", "waste_type" to "Jenis limbah",
    "category" to "Kategori", "characteristic" to "Karakteristik", "waste_record" to "Catatan limbah",
    "created_by" to "Pembuat", "created_by_user" to "Pembuat", "submitted_by" to "Pengaju", "submitted_by_user" to "Pengaju",
    "approved_by" to "Penyetuju", "approved_by_user" to "Penyetuju", "rejected_by_user" to "Penolak", "updated_by_user" to "Pengubah",
    "hauling_history" to "Riwayat pengangkutan", "haulings" to "Pengangkutan", "record" to "Catatan",
    "material_type" to "Material", "movement_type" to "Tipe transaksi", "period_label" to "Periode",
    "year" to "Tahun", "month" to "Bulan", "period_year" to "Tahun", "period_month" to "Bulan",
    "vendor" to "Vendor", "vendor_name" to "Vendor", "internal_destination" to "Tujuan internal", "purpose" to "Tujuan penggunaan",
    "document_number" to "Nomor dokumen", "document_date" to "Tanggal dokumen", "duplicate_warning" to "Peringatan duplikasi",
    "message" to "Pesan", "organization" to "Organisasi", "code" to "Kode", "role" to "Role", "permissions" to "Hak akses",
    "email_verified_at" to "Email terverifikasi", "is_active" to "Aktif", "is_hazardous" to "Berbahaya",
    "storage_period_days" to "Masa penyimpanan (hari)", "transport_cost" to "Biaya pengangkutan",
    "contact_person" to "Narahubung", "phone" to "Telepon", "address" to "Alamat",
    "license_number" to "Nomor izin", "license_expiry_date" to "Berlaku sampai", "waste" to "Limbah", "faba" to "FABA",
    "total_records" to "Total catatan", "draft_records" to "Draft", "pending_records" to "Menunggu persetujuan",
    "approved_records" to "Disetujui", "total_transportations" to "Total pengangkutan", "pending_transportations" to "Pengangkutan tertunda",
    "in_transit_transportations" to "Dalam perjalanan", "current_balance" to "Saldo sekarang",
    "total_production" to "Total produksi", "total_utilization" to "Total pemanfaatan", "pending_approvals" to "Periode menunggu",
    "pending_transaction_approvals" to "Transaksi menunggu", "negative_periods" to "Periode bersaldo negatif",
    "opening_balance" to "Saldo awal", "closing_balance" to "Saldo akhir", "production" to "Produksi", "utilization" to "Pemanfaatan",
    "adjustment_in" to "Penyesuaian masuk", "adjustment_out" to "Penyesuaian keluar", "balance" to "Saldo",
    "totals" to "Total", "months" to "Bulan", "rows" to "Rincian", "materials" to "Material", "fly_ash" to "Fly Ash",
    "bottom_ash" to "Bottom Ash", "recap" to "Rekap", "snapshot" to "Snapshot penutupan", "movements" to "Transaksi",
    "approval" to "Persetujuan periode", "approvals" to "Periode", "audit_logs" to "Riwayat aktivitas",
    "actor" to "Pelaku", "summary" to "Ringkasan", "action" to "Tindakan", "details" to "Rincian",
    "vendor_breakdown" to "Rincian vendor", "internal_destination_breakdown" to "Rincian tujuan internal",
    "purpose_breakdown" to "Rincian penggunaan", "capacity" to "Kapasitas TPS", "warning_threshold" to "Ambang warning",
    "critical_threshold" to "Ambang critical", "utilization_percentage" to "Pemakaian kapasitas (%)",
    "tps_capacity_summary" to "Kapasitas TPS", "stock_effect" to "Arah stok", "total" to "Total",
    "resolved_period" to "Periode laporan", "available_periods" to "Periode tersedia",
    "capacity_summary" to "Kapasitas TPS", "stock_card_summary" to "Ringkasan kartu stok",
    "search" to "Cari nomor / jenis / sumber", "vendor_id" to "Vendor", "set_at" to "Diatur pada", "set_by_user" to "Pengatur",
)
fun label(key: String) = labels[key] ?: key.replace('_', ' ').replaceFirstChar(Char::uppercase)

/** API decimal strings and JSON numbers use the same local display, never a missing-value zero. */
internal fun formatMetric(value: String): String? = value.toBigDecimalOrNull()?.let { number ->
    java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("id-ID")).apply {
        maximumFractionDigits = number.scale().coerceIn(0, 20)
    }.format(number)
}
val statuses = mapOf(
    "draft" to "Draft", "pending_approval" to "Menunggu persetujuan", "pending_review" to "Menunggu persetujuan", "submitted" to "Diajukan",
    "approved" to "Disetujui", "rejected" to "Ditolak", "cancelled" to "Dibatalkan",
    "production" to "Produksi", "workshop" to "Workshop", "reject" to "Reject", "disposal_pok" to "Disposal POK",
    "utilization_external" to "Pemanfaatan eksternal", "utilization_internal" to "Pemanfaatan internal",
    "adjustment_in" to "Penyesuaian masuk", "adjustment_out" to "Penyesuaian keluar",
    "fly_ash" to "Fly Ash", "bottom_ash" to "Bottom Ash", "true" to "Ya", "false" to "Tidak", "in" to "Masuk", "out" to "Keluar",
)
val actionLabels = mapOf("update" to "Edit", "delete" to "Hapus", "submit" to "Ajukan", "approve" to "Setujui", "reject" to "Tolak", "cancel" to "Batalkan", "return_to_draft" to "Kembalikan ke draft", "reopen" to "Buka kembali", "create_hauling" to "Ajukan pengangkutan")
