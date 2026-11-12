# WASPRO Mobile

Aplikasi Android Kotlin untuk operasional limbah dan FABA. Menggunakan Compose Material 3, Navigation 3, ViewModel/StateFlow, Retrofit/OkHttp, Kotlin Serialization, Room, DataStore, dan Hilt.

## Lingkungan

Buka folder ini di Android Studio. Gunakan SDK 36, Build Tools 36.0.0 dan JDK bawaan Android Studio. Minimum perangkat Android 7.0 (API 24). Dependency dikunci dalam gradle/libs.versions.toml.

Variant production menggunakan https://waspro.ubpbsla.com/api/v1/. Variant development memakai http://10.0.2.2:8000/api/v1/ untuk emulator. URL development/staging dapat diganti lewat properti Gradle developmentApiUrl dan stagingApiUrl. Host staging bawaan merupakan konfigurasi awal yang perlu disesuaikan dengan server uji sebenarnya.

```powershell
.\gradlew.bat :app:assembleProductionDebug :app:lintProductionDebug :app:testProductionDebugUnitTest
.\gradlew.bat :app:connectedProductionDebugAndroidTest
.\gradlew.bat :app:installProductionDebug
adb shell am start -n com.ubpbsla.waspro/.MainActivity
```

APK tersedia di app/build/outputs/apk/production/debug/app-production-debug.apk.

## Modul

- Login, pemulihan sesi, beranda, profil, ganti password dan logout.
- Catatan limbah, pengajuan, penolakan/perbaikan, backlog dan riwayat pengangkutan.
- Produksi, pemanfaatan internal/eksternal, lampiran, penyesuaian, saldo awal dan kapasitas TPS.
- Persetujuan limbah, hauling, transaksi harian dan periode bulanan.
- Rekap bulanan/tahunan/vendor/saldo/kartu stok, CSV dan PDF/XLSX.
- Master kategori/karakteristik/jenis/vendor, serta direktori pengguna dan role sesuai izin.

Menu dan tindakan mengikuti permissions serta allowed_actions server. Operator melihat catatan limbah organisasi, sementara hauling mengikuti cakupan view_own backend. Approve sendiri ditolak backend. Pengajuan periode memakai izin submit dan peninjau periode harus berbeda dari pengajunya.

Draft lokal disimpan per environment, organisasi dan akun. Draft dikirim manual saat tersambung. Permintaan yang hasilnya belum pasti mempertahankan payload dan Idempotency-Key untuk retry. Edit memeriksa resource_version server sebelum pengiriman dan kembali di dalam transaksi backend. Token menggunakan enkripsi Android Keystore. Password tidak disimpan. Backup aplikasi dinonaktifkan.

## Backend

Sumber backend berada di F:\was-pro. Implementasi Android memerlukan API tambahan dalam repositori tersebut untuk master data, laporan, direktori pengguna/role, lampiran, multipart update dan permission bulanan yang telah diperbaiki. Lihat docs/api/mobile-implementation.md di backend untuk kontrak tambahan dan pengujian.

Sebelum UAT, pasang perubahan backend pada server staging dan siapkan operator serta dua supervisor pada satu organisasi. Akun organisasi kedua diperlukan untuk pengujian isolasi. Gunakan data uji.

Pengujian end-to-end perangkat memakai tests/Support/mobile-uat-fixture.php di backend. Script menolak berjalan pada database yang tidak melewati pengaman DisposableDatabase. Setelah menyiapkan database dan storage disposable sesuai TESTING.md backend, jalankan fixture dan server API lokal pada port 8799.

```powershell
.\gradlew.bat :app:connectedDevelopmentDebugAndroidTest '-PdevelopmentApiUrl=http://10.0.2.2:8799/api/v1/' '-Pandroid.testInstrumentationRunnerArguments.wasproFixture=true' '-Pandroid.testInstrumentationRunnerArguments.class=com.ubpbsla.waspro.OperationalFlowTest'
```

Alur ini menguji operator membuat/mengajukan limbah lalu supervisor menyetujui. Pada pengujian productionDebug biasa, test tersebut dilewati karena membutuhkan fixture lokal. Pengujian login UI dan persistensi draft berjalan tanpa server.

## Pemeriksaan visual lokal

`tools/ui_fixture.py` menyediakan data sintetis berlabel DEMO di loopback port 8798.
Fixture ini hanya untuk rendering, navigasi, validasi form, dan draft; bukan pengganti
pengujian kontrak/otorisasi backend. Tidak terhubung ke server WASPRO dan menolak
mutasi catatan. Gunakan emulator, bukan akun atau perangkat operasional.

```powershell
python tools/ui_fixture.py
adb reverse tcp:8798 tcp:8798
.\gradlew.bat :app:connectedDevelopmentDebugAndroidTest '-PdevelopmentApiUrl=http://127.0.0.1:8798/api/v1/' '-Pandroid.testInstrumentationRunnerArguments.wasproVisualFixture=true' '-Pandroid.testInstrumentationRunnerArguments.wasproCapture=modern' '-Pandroid.testInstrumentationRunnerArguments.class=com.ubpbsla.waspro.ModernUiTest'
```

Screenshot berasal dari aplikasi sebenarnya dan disimpan di `/data/local/tmp/waspro-modern-*.png`
pada emulator. Ulangi dengan ukuran layar/font berbeda dan mode gelap. Test hanya
membersihkan draft miliknya yang bertanda `Uji tampilan lokal` pada scope fixture
`development|ui-demo|ui-demo`; data akun lain tidak disentuh.

## Signing internal

Keystore release disediakan pemilik aplikasi dan disimpan di luar repositori. Sediakan environment variable pada proses build.

- WASPRO_KEYSTORE berisi lokasi absolut keystore.
- WASPRO_KEY_ALIAS berisi alias.
- WASPRO_STORE_PASSWORD dan WASPRO_KEY_PASSWORD berisi password.

```powershell
.\gradlew.bat :app:assembleProductionRelease
```

Gunakan keystore yang sama untuk pembaruan aplikasi berikutnya. CI verifikasi hanya mengunggah laporan pengujian. APK yang terhubung production didistribusikan lewat kanal internal. Signing dan UAT organisasi diperlukan sebelum rilis internal.

## Audit UI/UX lanjutan

Lihat [temuan, cakupan QA, dan panduan demo aman](docs/ui-ux-audit-2026-11-10.md). Pengujian native menggunakan Compose instrumentation; fixture DEMO tetap terpisah dari transaksi operasional.

## Polish visual lanjutan

Lihat [perubahan visual, rujukan desain, dan verifikasi](docs/ui-polish-2026-11-11.md).

## Panduan per role dan kasus

Buka **Lainnya → Panduan penggunaan** untuk 15 kasus operasional, filter role, pencarian, dan empat screenshot native DEMO yang tersedia di perangkat. Lihat [cakupan, sumber kontrak, screenshot, dan batasan mobile](docs/user-guide-mobile.md). Panduan tidak mengganti permission akun atau menambahkan fitur backend.
