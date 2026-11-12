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
        // targetSdk dinaikkan ke 34 (Android 14) supaya aplikasi juga
        // berperilaku benar di Android terbaru (HP penguji dan perangkat
        // masa depan): foreground service wajib bertipe khusus dan
        // notifikasi butuh izin. Box NexVidio Android 10 tidak terpengaruh
        // karena yang menentukan bisa/tidaknya dipasang adalah minSdk.
        targetSdk = 34
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
