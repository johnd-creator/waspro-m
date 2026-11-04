# WASPRO Mobile

Buka folder ini di Android Studio dan jalankan Gradle Sync. Gunakan Android SDK Platform 36, Build Tools 36.0.0, serta JDK bawaan Android Studio. Untuk PowerShell, pastikan JAVA_HOME menunjuk ke JDK dan ANDROID_HOME ke SDK.

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
adb devices
.\gradlew.bat :app:installDebug
adb shell am start -n com.ubpbsla.waspro/.MainActivity
```

Pilih perangkat Android 7.0 (API 24) atau lebih baru. APK debug tersedia di app/build/outputs/apk/debug/app-debug.apk. Layar awal menampilkan nama aplikasi. Konfigurasi endpoint berada di app/build.gradle.kts. Buka folder proyek ini di Codex agar agen memakai AGENTS.md.
