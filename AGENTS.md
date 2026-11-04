# WASPRO Mobile

Kembangkan klien Android native dalam Kotlin di workspace ini. Gunakan Gradle Wrapper dari root proyek. Plugin Android menyediakan dukungan Kotlin bawaan.

## Backend

Mulai dari `F:\was-pro\docs\api\kotlin-readiness.md` dan `openapi.json` di folder yang sama untuk merancang DTO. Cek batas cakupan spesifikasi pada README backend sebelum mengandalkannya.

Gunakan BuildConfig.API_BASE_URL untuk URL dasar. Periksa kontrak di F:\was-pro\docs\api, rute di F:\was-pro\routes\api.php, serta controller dan middleware terkait sebelum menambah integrasi. API menggunakan prefix /api/v1/. Periksa cakupan tenant dan hak akses untuk setiap operasi. Gunakan fixture atau server uji untuk pengujian. Simpan kredensial dan token di luar source, log, dan BuildConfig.

## Verifikasi

Jalankan .\gradlew.bat :app:assembleDebug :app:lintDebug setelah mengubah aplikasi. Jalankan :app:testDebugUnitTest saat menambahkan logika yang diuji. Untuk pengujian UI, pilih emulator atau perangkat uji melalui Android Studio.
