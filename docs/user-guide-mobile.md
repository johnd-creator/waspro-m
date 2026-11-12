# Panduan penggunaan WASPRO-M

Panduan native tersedia pada **Lainnya → Panduan penggunaan**. Pilih Operator, Supervisor, Admin, Super Admin, atau Semua role, lalu cari kasus. Pilihan hanya menyaring materi: tidak mengganti akun, tenant, permission, atau tindakan yang tersedia.

## Cakupan

15 kasus: masuk/periksa akses; catat/ajukan limbah; perbaikan ditolak; pengangkutan; produksi; pemanfaatan internal; pemanfaatan eksternal; review; periode bulanan; penyesuaian/saldo awal/kapasitas; rekap/ekspor; master data; direktori/akun; draft lokal; kendala akses/cache/konflik.

Setiap kasus memuat jalur menu, prasyarat, langkah bernomor, hasil yang diharapkan, serta perhatian/batasan. Pencarian menyaring judul, langkah, prasyarat, jalur, dan catatan dalam role terpilih. Tidak ada request jaringan atau transaksi dari layar panduan. Akses aplikasi tetap memerlukan sesi pengguna yang valid.

## Per role

- **Operator:** input limbah/FABA, pengajuan angkut dan periode, perbaikan, draft, rekap/laporan sesuai izin. Bukan peninjau sendiri.
- **Supervisor:** pekerjaan operator serta review, master data, dan pengaturan operasional jika diizinkan. Pembuat/pengaju tidak dapat menyetujui sendiri.
- **Admin:** operasional organisasi dan master mobile. Direktori pengguna/role hanya baca; pengelolaan akun/izin melalui web.
- **Super Admin:** mobile mengikuti organisasi akun aktif. Tidak ada pemilih tenant, pengelolaan organisasi, atau administrasi platform di mobile.

Nama role bukan pemeriksaan otorisasi. Aplikasi memakai permissions dan allowed_actions aktual dari server. Daftar kasus tidak menyembunyikan informasi umum tentang alur kerja lintas peran.

## Screenshot aktual yang disertakan

Empat PNG disalin tanpa modifikasi dari hasil pemeriksaan visual native di `.work/polish-nov11/delivery/`. Screenshot berasal dari emulator development dengan fixture loopback `tools/ui_fixture.py`, berisi data sintetis DEMO dan email `example.test`. Bukan mockup, akun nyata, atau bukti deployment produksi. Gambar disimpan pada drawable-nodpi agar tersedia offline. Tombol Buka screenshot menampilkan gambar yang bisa digulir; Tutup dan Back mengembalikan kasus semula.

| Kasus | Screenshot | Isi |
| --- | --- | --- |
| Catat dan ajukan limbah | [Form](../app/src/main/res/drawable-nodpi/guide_waste_form.png) | Kolom tanggal, jenis, jumlah, satuan, sumber, simpan |
| Perbaiki limbah ditolak | [Daftar](../app/src/main/res/drawable-nodpi/guide_waste_records.png) | Filter, muat ulang, status. Contoh daftar, bukan screenshot transaksi ditolak |
| Tinjau pengajuan | [Detail](../app/src/main/res/drawable-nodpi/guide_waste_review.png) | Identitas pengaju DEMO, status, tombol Setujui/Tolak |
| Rekap dan laporan | [Laporan](../app/src/main/res/drawable-nodpi/guide_reports.png) | Jenis, periode, material, filter, PDF/XLSX |

## Kontrak yang diverifikasi

Sumber mobile: `features/Modules.kt`, `features/WasproViewModel.kt`, `data/Api.kt`, `ui/WasproApp.kt`. Sumber backend (dibaca tanpa diubah): `docs/api/mobile-implementation.md`, `docs/api/kotlin-readiness.md`, `routes/api.php`, `database/seeders/RolesSeeder.php`, `database/seeders/RolePermissionsSeeder.php`, `app/Http/Controllers/Api/WasteHaulingController.php`, dan `app/Http/Requests/WasteManagement/WasteHaulingRequest.php` pada F:\was-pro.

Batas yang disebutkan secara eksplisit:

- Role/izin pengguna hanya dapat dibaca pada mobile. Master mobile terbatas kategori, karakteristik, jenis limbah dan vendor.
- Metadata/vendor/manifest/lampiran hauling dan rencana angkut sudah memiliki unsur backend, tetapi belum tersedia pada form mobile. Panduan mengarahkan ke web jika prosedur memerlukannya.
- Tidak ada sinkronisasi background atau kirim otomatis. Draft milik environment, organisasi dan akun; logout menghapus draft setelah konfirmasi.
- Produksi/pemanfaatan/penyesuaian tidak disamakan dengan langkah Ajukan pada limbah.
- Persetujuan memakai pemisahan pembuat/pengaju dan peninjau, termasuk role admin; periode memerlukan izin submit/approve/reject/reopen masing-masing.
- Ekspor CSV berada pada layar Laporan. Filter periode/material laporan tidak berlaku untuk CSV limbah.
- Screenshot/guidance bukan bukti backend sudah dipasang di produksi dan tidak menggantikan UAT staging, tenant isolation atau pengujian transaksi nyata.

## Pemeliharaan

Katalog: `features/UserGuide.kt`; tampilan: `ui/UserGuideScreen.kt`; pintu masuk/navigasi: `ui/WasproApp.kt`. Perbarui panduan serta screenshot jika label/menu berubah. Semua kasus menggunakan ID stabil. Screenshot yang didukung dipetakan eksplisit dan diberi deskripsi aksesibilitas.

Pengujian baru: `UserGuideTest` memeriksa kelengkapan, role, pencarian, fallback dan batasan; `UserGuideUiTest` memeriksa filter/reset, pembukaan kasus, screenshot close/Back, dan kasus tidak dikenal tanpa backend.
## Hasil verifikasi (12 November 2026, WIB)

- `:app:assembleProductionDebug :app:lintProductionDebug :app:testProductionDebugUnitTest`: lulus. Unit test 20/20, termasuk 5 kasus katalog baru. Lint 0 error, 23 warning dependency/tooling yang sudah ada.
- `:app:connectedProductionDebugAndroidTest`: 26 skenario terdaftar, 8 lulus dan 18 dilewati sesuai syarat fixture; tidak ada kegagalan. Termasuk tiga tes UI panduan murni lokal. Gunakan hitungan XML, karena ringkasan console Gradle menghitung skipped dua kali.
- Development DEMO normal: 4/4 lulus untuk `UserGuideNavigationTest` dan `UserGuideUiTest`.
- Development DEMO 320 dp, font 130%, mode gelap: 4/4 lulus untuk dua kelas yang sama. Ukuran, density, font dan mode malam emulator dikembalikan ke baseline setelah tes.
- Pencarian/role, kondisi kosong/reset, masuk kasus, kembali ke daftar dengan query tetap, pindah tab, buka/tutup/Back screenshot, serta ID kasus tak dikenal diuji. Satu kegagalan awal karena timing/focus Back dialog diperbaiki dan uji ulang lulus.
- Screenshot dari aplikasi final (bukan harness) diperiksa secara visual pada ukuran normal dan sempit. Caption dan langkah tetap dapat digulir; judul app bar dipendekkan pada layar sempit seperti halaman lain.
- `git diff --check`: lulus.

Log dan bukti lokal berada pada `.work/guide-nov11/`: `production-final.log`, `development-guide.log`, `compact-guide.log`, `mobile-index.png`, `mobile-case.png`, `compact-index.png`, `compact-case.png`. Folder `.work` tidak dikirim ke Git. APK debug tersedia pada jalur output Gradle standar. Pengujian ini tidak mengklaim seluruh modul/peran telah UAT, perangkat fisik telah diuji, atau release telah didistribusikan.