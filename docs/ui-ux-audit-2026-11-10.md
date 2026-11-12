# Audit UI/UX WASPRO Mobile — 10 November 2026

## Ruang lingkup

Aplikasi Android native Kotlin/Compose di Mnstr (`F:\waspro-m`). Audit ini melanjutkan perubahan desain lokal yang sudah ada, tanpa reset, commit, push, deploy, perubahan backend, atau penggunaan akun operasional. Playwright tidak digunakan untuk mengendalikan aplikasi native; verifikasi memakai Compose instrumentation dan emulator Android.

## Temuan dan perbaikan

| Prioritas | Temuan | Perbaikan |
| --- | --- | --- |
| Tinggi | Login baru dapat meninggalkan dashboard terus memuat karena reset state membatalkan request baru | Reset state lama dilakukan sebelum identity login dipublikasikan; regresi fresh login ditambahkan. |
| Tinggi | Edit terakhir dapat hilang jika form ditinggalkan sebelum debounce autosave | Semua jalur Back/Lanjutkan nanti mem-flush draft; pembukaan form berikutnya membaca versi DAO terbaru. |
| Tinggi | Penggantian lampiran terlalu besar dapat merusak file sebelumnya | File pengganti disimpan terpisah dan divalidasi sebelum mengganti referensi draft. |
| Tinggi | Respons filter lama dapat menimpa hasil filter baru; data lama tampil dengan filter baru | Identitas permintaan dan scope diperiksa; query baru membersihkan data lama, refresh query sama tetap memperlihatkan status pemuatan. |
| Tinggi | Back saat simpan sedang berjalan dapat mengubah halaman yang dipop callback | Back toolbar dan sistem memiliki guard busy yang sama. |
| Sedang | Tanggal dokumen eksternal yang tersembunyi dapat memblokir pemanfaatan internal | Definisi applicability yang sama dipakai untuk tampilan, tanda wajib, validasi, dan payload null. |
| Sedang | Retry inisialisasi form kehilangan konteks record/draft/hauling | Retry menyimpan parameter asli dan memeriksa scope akun. |
| Sedang | Form yang hanya dibuka lalu ditutup menghasilkan draft kosong | Form baru tanpa perubahan tidak dipersist; draft yang sudah ada dan retry terkunci tetap aman. |
| Sedang | CSV limbah tampak mengikuti filter periode, padahal backend mengekspor semua catatan yang diizinkan | Cakupan CSV dijelaskan per modul dan hanya filter yang didukung dikirim. |
| Sedang | Detail padat pada layar sempit/font besar | Pasangan label/nilai menjadi susunan vertikal, tidak dipotong. |
| Sedang | Navigasi ponsel melebar pada tablet | Navigation rail pada window >=840 dp; bottom navigation tetap untuk window kecil. |
| Sedang | Filter tahun/bulan invalid langsung dikirim | Validasi inline sebelum Terapkan. |
| Sedang | Switch tidak memiliki label aksesibilitas terkait | Seluruh baris menjadi toggle berlabel dan memiliki target sentuh yang cukup. |

## Konsistensi antar-screen

- Header, petunjuk tindakan, kartu, formulir berkelompok, status pemuatan/error/offline, dan empty state memakai komponen bersama.
- Semua modul operasional, rekap, master data, akun, draft, dan laporan memakai hierarki yang konsisten.
- Tombol simpan tetap terlihat di atas keyboard; lebar form/tombol dibatasi pada layar besar.
- Tanda wajib kondisional dan pesan error terhubung ke semantics aksesibilitas.
- Dialog approve/reject/delete menjelaskan konsekuensi dan dapat dibatalkan tanpa mutasi.
- Password memiliki kontrol tampil/sembunyikan. Lingkungan non-production ditandai.
- Status cache dan kegagalan tetap terlihat. Tindakan detail hanya tersedia setelah data fresh berhasil dimuat.

## Cakupan QA

Fixture `tools/ui_fixture.py` hanya bind ke loopback, memakai identitas/data berlabel DEMO, dan menolak mutasi operasional dengan HTTP 405. Fixture adalah alat rendering/navigasi, bukan bukti kontrak atau otorisasi backend.

- `ModernUiTest`: dashboard, navigasi berulang, list/detail, validasi, keyboard, draft/resume, laporan.
- `AllModulesUiTest`: seluruh modul yang terdaftar, FABA internal/eksternal, pengangkutan, periode, rekap, master, profil/password, persetujuan, empty/error/reset filter, date-picker Cancel, dialog reject/approve Cancel.
- `EditorStateRegressionTest`: login baru tanpa race pemuatan, autosave cepat, retry terkunci, penghapusan draft, lampiran oversized, respons terlambat, query gagal, form tanpa perubahan.
- `LoginScreenTest`, `OperationalCardsTest`, `DraftPersistenceTest`: komponen login, status/cache/angka nol, dan isolasi penyimpanan.
- `FormVisibilityTest`: conditional validation dan filter periode.

Hasil aktual dan screenshot dicatat di `.work/audit-nov10/`. Jangan menganggap fixture sebagai UAT produksi.

## Hasil akhir verifikasi

Audit dimulai 10 November dan verifikasi final selesai 11 November 2026 (UTC).

| Pemeriksaan | Hasil |
| --- | --- |
| Production debug assemble, lint, unit; development debug dan test APK | Berhasil (`login-fix-build.log`). |
| Unit test production | 15 lulus, 0 gagal. |
| Lint production | 0 error, 23 warning dependensi/versi tooling/target yang tidak dibersihkan dalam audit UI ini. |
| API36, 320 dp, font 130%, mode gelap | 21/21 instrumentation lulus, termasuk login baru, seluruh tour modul, dan delapan regresi state (`compact-release.log`). |
| API36, ponsel normal, build final | 1/1 alur utama dengan login baru lulus dan screenshot final diambil (`phone-release.log`). |
| API36, tablet 1280 dp | 8/8 tour UI lulus (`wide-verified.log`), sebelum perbaikan urutan login terakhir yang tidak mengubah layout. |
| API24, build final | 2/2 lulus: alur utama dengan login baru dan regresi identity publication (`api24-login-release.log`). |
| API24, komponen production | 5/5 login, kartu, dan persistence lulus pada pemeriksaan sebelumnya (`api24-tests.log`). |
| Whitespace patch | `git diff --check` lulus. |

`OperationalFlowTest` yang membutuhkan backend asli tidak dihitung sebagai lulus dan tidak dijalankan sebagai transaksi produksi. Seluruh 22 modul terdaftar ditelusuri melalui fixture dan komponen bersama; cakupan ini bukan pembuktian semua kombinasi peran, tenant, data, dan jaringan backend sebenarnya.

Screenshot aktual berada di `.work/audit-nov10/delivery/`; seluruh isi merupakan data sintetis yang ditandai DEMO. Perubahan lokal yang ada sebelum audit dipertahankan. Tidak ada commit, push, deploy, atau perubahan backend.

## Menjalankan demo aman

1. Jalankan emulator uji Android API 36 (atau perangkat uji), bukan perangkat operasional.
2. Jalankan `python tools/ui_fixture.py` dari root repo. Jika port 8798 sudah dipakai, periksa pemilik proses sebelum menghentikan apa pun.
3. Jalankan `adb -s <serial-emulator> reverse tcp:8798 tcp:8798`.
4. Build/install development dengan `-PdevelopmentApiUrl=http://127.0.0.1:8798/api/v1/`.
5. Jalankan `com.ubpbsla.waspro.dev/com.ubpbsla.waspro.MainActivity`. Gunakan identitas sintetis fixture `aditya@example.test` dan password demo `demo` jika layar login muncul. Ini bukan akun/kredensial WASPRO sebenarnya.
6. Tampilkan dashboard, catatan/detail, draft lokal, FABA, persetujuan (buka lalu Batal), master data, dan laporan. Pengiriman operasional sengaja ditolak fixture; jangan mempresentasikannya sebagai transaksi berhasil.

Contoh pengujian:

```powershell
.\gradlew.bat :app:assembleProductionDebug :app:lintProductionDebug :app:testProductionDebugUnitTest
.\gradlew.bat :app:connectedDevelopmentDebugAndroidTest '-PdevelopmentApiUrl=http://127.0.0.1:8798/api/v1/' '-Pandroid.testInstrumentationRunnerArguments.wasproVisualFixture=true' '-Pandroid.testInstrumentationRunnerArguments.wasproCapture=demo'
```

## Sebelum demo dengan transaksi sungguhan

- Lakukan UAT di backend staging disposable dengan operator dan dua supervisor, plus organisasi kedua untuk isolasi.
- Verifikasi create/submit/approve/reject, konflik versi, lampiran, download, serta peran/tenant dengan kontrak backend sebenarnya.
- Tes API24–36 tidak menggantikan uji perangkat fisik, TalkBack, koneksi lapangan, atau penandatanganan rilis.
- APK production debug dibangun untuk pemeriksaan, bukan bukti rilis siap distribusi. Tidak ada deployment dalam audit ini.
