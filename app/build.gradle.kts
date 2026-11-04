plugins {
    id("com.android.application") version "9.2.1"
}

android {
    namespace = "com.ubpbsla.waspro"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ubpbsla.waspro"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE_URL", "\"https://waspro.ubpbsla.com/api/v1/\"")
    }

    buildFeatures {
        buildConfig = true
    }
}
