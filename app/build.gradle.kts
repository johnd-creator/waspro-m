plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.compose)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
android {
    namespace = "com.ubpbsla.waspro"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.ubpbsla.waspro"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    flavorDimensions += "environment"
    productFlavors {
        create("development") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            buildConfigField("String", "API_BASE_URL", "\"" + providers.gradleProperty("developmentApiUrl").getOrElse("http://10.0.2.2:8000/api/v1/") + "\"")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            buildConfigField("String", "API_BASE_URL", "\"" + providers.gradleProperty("stagingApiUrl").getOrElse("https://staging.waspro.ubpbsla.com/api/v1/") + "\"")
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"https://waspro.ubpbsla.com/api/v1/\"")
        }
    }
    signingConfigs {
        create("internal") {
            val signingPath = providers.environmentVariable("WASPRO_KEYSTORE").orNull
            if (signingPath != null) {
                storeFile = file(signingPath)
                storePassword = providers.environmentVariable("WASPRO_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("WASPRO_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("WASPRO_KEY_PASSWORD").orNull
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("internal")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation3)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.serialization.json)
    implementation(libs.coroutines)
    coreLibraryDesugaring(libs.desugar)
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.android.test)
    androidTestImplementation(libs.test.runner)
    debugImplementation(libs.compose.test.manifest)
}
