package com.ubpbsla.waspro.features

/** Offline reference only. Roles describe responsibilities, never grant application permissions. */
data class GuideRole(val key: String, val title: String, val summary: String)
data class GuideCase(
    val id: String, val title: String, val group: String, val roles: Set<String>,
    val route: String, val prerequisite: String, val steps: List<String>,
    val result: String, val caution: String, val screenshot: String? = null,
)

object UserGuide {
    val roles = listOf(
        GuideRole("all", "Semua role", "Pilih tanggung jawab untuk melihat kasus yang relevan. Pilihan ini hanya menyaring panduan, bukan mengganti role akun."),
        GuideRole("operator", "Operator", "Mencatat limbah dan FABA, mengajukan pengangkutan serta periode, memperbaiki catatan, dan memeriksa rekap. Persetujuan dilakukan peninjau lain yang berwenang."),
        GuideRole("supervisor", "Supervisor", "Meninjau limbah, pengangkutan, transaksi dan periode FABA; menjaga master data serta rekap. Pembuat atau pengaju tidak boleh menyetujui pekerjaannya sendiri."),
        GuideRole("admin", "Admin", "Mengawasi operasional organisasi dan master data. Di mobile, Pengguna serta Role dan izin adalah direktori baca; perubahan akun/izin dilakukan di web oleh pihak berwenang."),
        GuideRole("super_admin", "Super Admin", "Mendampingi operasional pada organisasi akun aktif. Mobile tidak menyediakan pemilihan tenant, pengelolaan organisasi, atau administrasi platform; gunakan web untuk fungsi tersebut."),
    )
    private val everyone = setOf("operator", "supervisor", "admin", "super_admin")
    private val reviewers = setOf("supervisor", "admin", "super_admin")
    val cases = listOf(
        GuideCase("start", "Masuk dan periksa akses", "Mulai", everyone,
            "Masuk → Beranda → Lainnya", "Gunakan akun organisasi yang sudah disiapkan administrator.",
            listOf("Masukkan email dan password, lalu pilih Masuk. Jangan membagikan password atau token.",
                "Periksa nama organisasi di Beranda atau Lainnya sebelum mulai bekerja.",
                "Buka tab Limbah, FABA, Tinjau, atau Lainnya sesuai pekerjaan. Menu mengikuti izin server, bukan nama role saja.",
                "Jika organisasi kosong atau menu yang diperlukan tidak tersedia, hubungi administrator. Pilihan role pada panduan ini tidak mengubah akses."),
            "Ruang kerja sesuai akun dan organisasi aktif.",
            "Label DEV pada screenshot berarti aplikasi development dengan data DEMO; bukan bukti fitur sudah terpasang di server produksi."),
        GuideCase("waste-create", "Catat dan ajukan limbah", "Limbah", everyone,
            "Limbah → Catatan limbah", "Izin melihat/membuat limbah; pengajuan memerlukan izin submit. Jenis limbah harus tersedia.",
            listOf("Pilih Tambah catatan limbah. Isi tanggal, jenis limbah, jumlah, satuan, serta sumber/lokasi dan keterangan bila perlu.",
                "Periksa angka dan tanggal. Pilih Simpan ke server saat tersambung, lalu pastikan catatan tampil pada daftar/detail.",
                "Buka detail catatan draft dan pilih Ajukan jika tersedia. Baca dialog, lalu konfirmasi hanya setelah data benar.",
                "Muat ulang dan periksa status Menunggu persetujuan. Catatan memerlukan peninjau lain yang berwenang."),
            "Catatan tersimpan di server; pengajuan terpisah dari penyimpanan draft lokal.",
            "Operator dapat melihat catatan limbah organisasi sesuai izin backend. Hak edit tetap mengikuti pembuat, status, dan tindakan yang diizinkan server.", "form"),
        GuideCase("waste-rejected", "Perbaiki limbah yang ditolak", "Limbah", everyone,
            "Limbah → Catatan limbah → Detail", "Catatan ditolak dan server menyediakan Kembalikan ke draft/Edit untuk akun Anda.",
            listOf("Gunakan Filter status untuk menemukan catatan Ditolak, lalu baca alasan penolakan pada detail/Informasi tambahan.",
                "Pilih Kembalikan ke draft bila tersedia, lalu Edit. Perbaiki bagian yang diminta peninjau.",
                "Simpan ke server, buka kembali detail, dan Ajukan jika tindakan tersedia.",
                "Muat ulang untuk memastikan status terbaru. Jika tindakan tidak ada, minta administrator memeriksa izin/status, bukan membuat catatan ganda."),
            "Perbaikan dikirim kembali untuk peninjauan.", "Catatan yang terkunci atau sudah diproses tidak selalu dapat diubah.", "records"),
        GuideCase("hauling", "Ajukan pengangkutan dari sisa limbah", "Limbah", everyone,
            "Limbah → Pengangkutan → Catatan", "Limbah disetujui, ada sisa tersedia, dan akun memiliki izin membuat pengangkutan.",
            listOf("Pilih catatan pada daftar Pengangkutan. Periksa sisa, reservasi, dan riwayat angkut.",
                "Pilih Ajukan pengangkutan pada detail bila tersedia. Catatan asal terisi otomatis.",
                "Isi tanggal angkut, jumlah, dan catatan. Jangan melebihi jumlah tersedia setelah reservasi/pengajuan lain.",
                "Pilih Simpan ke server dan periksa riwayat/status pengangkutan. Batalkan hanya jika server menyediakan tindakan tersebut."),
            "Pengajuan pengangkutan tercatat dan menunggu alur persetujuan server.",
            "Form mobile saat ini belum menyediakan vendor, manifest/lampiran hauling, atau rencana angkut. Gunakan web bila prosedur organisasi memerlukan rincian itu. Cakupan view_own mengikuti backend."),
        GuideCase("faba-production", "Catat produksi FABA", "FABA", everyone,
            "FABA → Produksi FABA", "Izin input, material tersedia, dan periode tidak terkunci.",
            listOf("Pilih Tambah produksi FABA, lalu isi tanggal, material, jumlah, tipe entri dan catatan.",
                "Periksa jumlah dalam satuan modul serta tanggal operasional. Pilih Simpan ke server.",
                "Buka detail dan periksa status persetujuan. Transaksi FABA mengikuti alur server; tidak memiliki langkah Ajukan limbah yang sama.",
                "Gunakan Filter periode/material dan rekap untuk memeriksa hasil setelah peninjauan."),
            "Produksi tercatat dengan status dari server.", "Transaksi pada periode yang dikunci tidak dapat diperbaiki sebelum dibuka kembali oleh pihak berwenang."),
        GuideCase("faba-internal", "Catat pemanfaatan internal", "FABA", everyone,
            "FABA → Pemanfaatan FABA", "Izin membuat pemanfaatan dan tujuan internal tersedia.",
            listOf("Pilih Tambah pemanfaatan FABA dan jenis Pemanfaatan internal.",
                "Isi tanggal, material, jumlah, Tujuan internal, serta tujuan penggunaan/catatan yang relevan.",
                "Periksa saldo dan tujuan. Simpan ke server, kemudian periksa detail/status.",
                "Jika tujuan belum tersedia, minta penanggung jawab master data menyiapkannya di web."),
            "Pemanfaatan internal tercatat untuk peninjauan.", "Jangan mengisi vendor/dokumen eksternal untuk kasus internal; kolom mengikuti jenis pemanfaatan."),
        GuideCase("faba-external", "Catat pemanfaatan eksternal", "FABA", everyone,
            "FABA → Pemanfaatan FABA", "Izin membuat pemanfaatan, vendor tersedia, dan bukti dokumen siap.",
            listOf("Pilih Tambah pemanfaatan FABA dan jenis Pemanfaatan eksternal.",
                "Isi tanggal, material, jumlah, Vendor, Nomor dokumen, dan Tanggal dokumen.",
                "Pilih lampiran melalui file atau kamera sesuai pilihan form. Gunakan JPG, PNG, WEBP, atau PDF yang dapat dibaca.",
                "Periksa dokumen dan isian, lalu Simpan ke server. Buka detail untuk memeriksa status dan lampiran."),
            "Transaksi eksternal beserta lampirannya tercatat.", "Lampiran wajib untuk pemanfaatan eksternal. Buka/bagikan lampiran hanya kepada pihak yang berwenang; hindari menyebarkan data organisasi."),
        GuideCase("review", "Tinjau dan putuskan pengajuan", "Persetujuan", reviewers,
            "Tinjau → Jenis pengajuan → Detail", "Izin approve/reject; bukan pembuat atau pengaju sendiri, status masih menunggu, dan periode mengizinkan.",
            listOf("Pilih Persetujuan limbah, Persetujuan pengangkutan, atau Transaksi FABA pada tab Tinjau.",
                "Buka detail, muat ulang, dan periksa pembuat/pengaju, tanggal, jumlah, saldo/sisa, dokumen serta Informasi tambahan.",
                "Pilih Setujui bila sesuai atau Tolak dengan alasan yang jelas. Baca dialog sebelum konfirmasi; Batal tidak mengirim keputusan.",
                "Muat ulang dan pastikan status berubah. Jika tombol tidak tersedia, periksa izin/status dan gunakan peninjau lain untuk pengajuan sendiri."),
            "Keputusan tercatat atas nama peninjau yang bertindak.", "Jangan menyetujui transaksi sendiri, termasuk saat memakai role Admin/Super Admin. Screenshot hanya contoh limbah DEMO.", "detail"),
        GuideCase("period", "Ajukan dan tinjau periode bulanan", "Persetujuan", everyone,
            "FABA → Persetujuan bulanan → Periode", "Izin melihat periode; submit/approve/reject/reopen terpisah sesuai tanggung jawab.",
            listOf("Pilih tahun/periode dan periksa rekap serta transaksi yang masih perlu diselesaikan.",
                "Pengaju memilih Ajukan jika tersedia, lalu mengonfirmasi periode yang benar.",
                "Peninjau lain membuka periode yang sama melalui FABA atau Tinjau → Periode FABA, lalu memeriksa rekap sebelum Setujui/Tolak.",
                "Jika periode terkunci perlu koreksi, pemegang izin Buka kembali mengisi alasan. Setelah koreksi, ikuti pengajuan/peninjauan ulang yang tersedia."),
            "Status periode menunjukkan hasil proses bulanan.", "Operator dapat mengajukan bila memiliki izin submit, bukan menyetujui. Peninjau periode harus berbeda dari pengajunya."),
        GuideCase("adjust", "Koreksi saldo dan kapasitas TPS", "FABA", everyone,
            "FABA → Penyesuaian FABA / Saldo awal / Kapasitas TPS", "Penyesuaian memerlukan izin input; saldo awal/kapasitas hanya dengan izin pengaturan operasional.",
            listOf("Untuk koreksi transaksi, buka Penyesuaian FABA lalu isi tanggal, material, jumlah, jenis masuk/keluar dan alasan wajib.",
                "Simpan dan periksa status peninjauan. Jangan memakai penyesuaian sebagai pengganti proses koreksi yang diwajibkan organisasi.",
                "Untuk saldo awal, akun berwenang mengisi tahun, bulan, material, jumlah dan catatan pada Saldo awal.",
                "Untuk kapasitas, isi material, kapasitas ton dan ambang warning/critical pada Kapasitas TPS. Periksa kembali dashboard/rekap setelah tersimpan."),
            "Koreksi/pengaturan tercatat sesuai izin dan validasi server.", "Menu saldo awal/kapasitas tidak selalu tersedia pada Operator. Jangan mengubah parameter tanpa dasar operasional yang sah."),
        GuideCase("reports", "Baca rekap dan unduh laporan", "Laporan", everyone,
            "FABA → Rekap, atau Lainnya → Laporan", "Izin rekap/ekspor yang sesuai dan koneksi ke server.",
            listOf("Untuk membaca, pilih Rekap bulanan/tahunan/vendor, Saldo FABA, atau Kartu stok di FABA, lalu sesuaikan filter.",
                "Untuk laporan, buka Lainnya → Laporan. Pilih jenis, tahun/bulan yang relevan, material dan Filter tambahan.",
                "Pilih PDF atau XLSX lalu tombol unduh. Ekspor CSV tersedia pada bagian Ekspor data CSV di layar Laporan sesuai izin. Filter periode/material tidak berlaku untuk CSV limbah.",
                "Periksa nama/periode file sebelum membagikan lewat aplikasi lain. Pastikan penerimanya berwenang."),
            "Rekap atau file mengikuti filter serta akses akun.", "Data tersimpan/cache dapat tertinggal. Muat ulang sebelum memakai angka untuk keputusan; unduhan memerlukan endpoint backend yang telah dipasang.", "reports"),
        GuideCase("master", "Kelola master data mobile", "Administrasi", reviewers,
            "Lainnya → Master data", "Izin view/create/edit untuk kategori, karakteristik, jenis limbah atau vendor.",
            listOf("Buka kategori, karakteristik, jenis limbah atau vendor. Cari/periksa entri yang sudah ada agar tidak duplikat.",
                "Pilih Tambah atau buka detail → Edit jika tersedia. Isi kolom wajib dan referensi yang sesuai.",
                "Periksa status aktif serta data izin vendor bila relevan, lalu Simpan ke server.",
                "Muat ulang dan pastikan entri tersedia. Master tujuan internal/tujuan penggunaan serta modul administrasi lain dikelola lewat web."),
            "Master data yang didukung mobile tersimpan.", "Mobile tidak menyediakan CRUD untuk seluruh master web. Penghapusan master tidak ditawarkan oleh API mobile ini."),
        GuideCase("directory", "Periksa akun, role dan batas mobile", "Administrasi", reviewers,
            "Lainnya → Pengguna / Role dan izin", "Izin melihat pengguna atau role; organisasi aktif sudah benar.",
            listOf("Buka Pengguna untuk membaca direktori organisasi, atau Role dan izin untuk membaca rincian role.",
                "Untuk mengubah profil sendiri, buka Profil → Edit; nama dan email perlu diisi.",
                "Untuk mengganti password, buka Ganti password, isi password saat ini, baru, dan konfirmasi. Setelah berhasil, masuk kembali.",
                "Gunakan web yang disetujui organisasi untuk membuat akun, mengubah role/izin, mengelola organisasi, dan alat operasional yang belum mempunyai layar mobile."),
            "Informasi akses dipahami tanpa mengubah hak pengguna melalui panduan.", "Super Admin tidak dapat berganti tenant dari mobile. Panduan tidak menambahkan endpoint, permission, atau fitur web ke aplikasi."),
        GuideCase("draft", "Lanjutkan draft saat koneksi pulih", "Kendala", everyone,
            "Form → Lanjutkan nanti; Lainnya → Draft lokal", "Draft milik akun, organisasi, dan environment yang sama masih ada pada perangkat.",
            listOf("Saat mengisi form operasional, perubahan disimpan di perangkat. Pilih Lanjutkan nanti bila belum siap mengirim.",
                "Buka Lainnya → Draft lokal → Lanjutkan. Periksa kembali data/lampiran setelah koneksi pulih.",
                "Pilih Simpan ke server secara manual. Tidak ada pengiriman otomatis atau sinkronisasi background.",
                "Jika hasil kirim belum pasti, pertahankan draft yang sama dan ulangi sesuai pesan aplikasi. Jangan membuat duplikat atau menghapus draft sebelum status jelas."),
            "Draft terkirim hanya setelah penyimpanan ke server berhasil.", "Keluar akun menghapus draft dan lampiran lokal setelah konfirmasi. Jangan logout untuk memperbaiki jaringan bila masih ada draft penting."),
        GuideCase("troubleshoot", "Atasi menu hilang, data lama, dan konflik", "Kendala", everyone,
            "Layar terkait → Muat ulang / Filter", "Catat pesan kesalahan tanpa menyertakan password/token.",
            listOf("Data kosong: periksa filter, periode, organisasi, lalu Reset filter dan Muat ulang.",
                "Data tersimpan: periksa koneksi. Tindakan detail dibatasi saat membaca cache; muat ulang untuk status terkini.",
                "Menu/tombol hilang: cek izin, status catatan, pembuat/pengaju, dan kunci periode bersama administrator. Memilih role di panduan tidak membuka akses.",
                "Konflik data berubah: tinjau data server terbaru sebelum melanjutkan draft. Jangan menimpa atau mengirim berulang tanpa pemeriksaan.",
                "Layanan gagal: tunggu/kembalikan koneksi dan coba lagi dari pekerjaan yang sama. Jika berulang, sampaikan modul, waktu dan pesan kesalahan kepada administrator."),
            "Penyebab dipersempit tanpa mengubah data atau izin secara sembarangan.", "Panduan dan screenshot dapat dibaca tanpa request tambahan setelah masuk. Login, muat ulang, mutasi, serta ekspor tetap memerlukan layanan backend."),
    )

    fun initialRole(slug: String, name: String): String = roles.firstOrNull {
        it.key == slug || it.title.equals(name, ignoreCase = true)
    }?.key ?: "all"

    fun find(id: String): GuideCase? = cases.find { it.id == id }

    fun filter(role: String, query: String): List<GuideCase> {
        val terms = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        return cases.filter { item ->
            (role == "all" || role in item.roles) && terms.all { term ->
                listOf(item.title, item.group, item.route, item.prerequisite, item.result, item.caution)
                    .plus(item.steps).any { it.contains(term, ignoreCase = true) }
            }
        }
    }
}