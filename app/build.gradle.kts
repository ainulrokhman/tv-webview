plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.my.awebdev.tvbrowser"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.my.awebdev.tvbrowser"
        minSdk = 24
        // Perangkat tujuan adalah NexVidio Android 10 (API 29). targetSdk
        // disamakan ke 29 supaya perilaku service latar & izin mengikuti
        // aturan Android 10 yang benar-benar berjalan di box, bukan aturan
        // Android 14 yang tidak relevan untuk pemakaian pribadi ini.
        targetSdk = 29
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    // Server HTTP ringan untuk halaman remote + endpoint perintah
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    // Pembuat QR untuk panel koneksi TV (hanya encoder; pemindaian
    // dilakukan kamera HP, bukan aplikasi ini)
    implementation("com.google.zxing:core:3.5.3")
}
