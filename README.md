# TV Browser Remote — WebView TV yang Diremote dari HP

Browser WebView layar penuh untuk Android TV box (perangkat tujuan:
NexVidio, Android 10, RAM 2 GB), dikendalikan dari browser HP lewat WiFi/LAN
yang sama. Tidak memakai Shizuku, ADB, root, maupun layanan aksesibilitas —
yang dikendalikan hanya aplikasi ini sendiri.

## Cara kerja

- Aplikasi box = satu WebView + server HTTP lokal (NanoHTTPD, port 8765)
  yang hidup sebagai foreground service dan menyala lagi setelah booting.
- Halaman awal aplikasi menampilkan blok QR (alamat + **PIN 4 digit**) dan
  link server; blok itu hanya tampil di halaman awal dan tidak hilang
  sendiri. Di bawahnya ada form: tempel link untuk membukanya, atau ketik
  kata untuk mencarinya.
- Pindai QR itu dengan kamera HP (langsung terhubung), atau buka alamatnya
  di browser HP dan masukkan PIN sekali (tersimpan di HP).
- Dari HP kamu bisa:
  - **Tempel link apa pun** (http/https) atau ketik kata pencarian
    (otomatis dicari lewat DuckDuckGo HTML yang ringan).
  - Navigasi: Kembali, Maju, Muat Ulang, Stop, Beranda.
  - D-pad untuk pindah fokus di dalam halaman, tombol OK, spasi
    (putar/jeda di banyak pemutar web).
  - **Touchpad**: ketuk = klik pada posisi relatif yang sama di TV,
    geser vertikal = gulir halaman. Tombol Gulir ↑/↓ dan Zoom +/−.
  - **Ketik dari HP** ke kolom yang sedang fokus di halaman TV.
  - Ganti **User-Agent** Bawaan/Desktop (halaman dimuat ulang).
  - **Favorit**: simpan halaman yang sedang terbuka, buka lagi sekali
    ketuk, hapus. Tersimpan di box.
  - Video HTML5 bisa **layar penuh** (tombol fullscreen di pemutar web),
    layar TV tidak tidur selama aplikasi di depan, cookie/login tersimpan.

## Batasan yang disengaja / perlu diketahui

- Remote hanya bekerja **di dalam aplikasi ini**. Aplikasi lain di box
  (YouTube native, Vidio, dst.) tidak ikut terkendali — itulah tukarannya
  membuang Shizuku.
- Layanan DRM resmi (Netflix, Vidio, Disney+, dll.) sering memblokir
  WebView atau menurunkan kualitas; untuk itu tetap pakai aplikasi
  bawaannya. Situs video HTML5 biasa umumnya berjalan.
- Link non-web (intent://, market://, tel:, unduhan berkas) **ditahan**
  dan tidak dijalankan — sesuai keputusan desain Tahap 4.
- Keamanan: server HTTP tanpa enkripsi + PIN 4 digit. Hanya untuk
  jaringan rumah sendiri; jangan pernah diekspos ke internet.
- `targetSdk` 34 (Android 14) supaya perilaku service/izin juga benar di
  Android terbaru; box Android 10 tetap bisa memasang karena `minSdk` 24.
  Foreground service dideklarasikan bertipe `specialUse` di manifest.

## Build (di Android Studio, laptop)

Build utama lewat **GitHub Actions** (workflow *Build APK*: push ke
branch `done` memicu build, APK diunduh dari artifact). Untuk build lokal
di Android Studio, wrapper Gradle versi 8.7 dikonfigurasi tetapi
`gradle-wrapper.jar` belum ada. Cara termudah:

1. Buka Android Studio → *New Project* → *Empty Views Activity* (Kotlin),
   tutup lagi. Salin folder `app/` dan file `settings.gradle.kts`,
   `build.gradle.kts`, `gradle.properties` dari proyek ini menimpa proyek
   kosong itu (nama package harus tetap `id.my.awebdev.tvbrowser`), lalu
   *Sync* dan *Run/Build APK*.
   — Atau bila `gradle` 8.7 tersedia: `gradle wrapper --gradle-version 8.7`
   sekali di folder ini, setelah itu `./gradlew assembleDebug`.
2. APK hasil build ada di `app/build/outputs/apk/debug/app-debug.apk`.

Ketergantungan: AndroidX core-ktx 1.13.1, appcompat 1.7.0,
NanoHTTPD 2.3.1. AGP 8.5.2, Kotlin 2.0.20, compileSdk 34, minSdk 24.

## Pasang & pakai di NexVidio

1. Salin APK ke flashdisk → pasang lewat file manager box (izinkan
   instalasi dari sumber tidak dikenal bila diminta), atau pasang lewat
   ADB bila tersedia.
2. Buka **TV Browser Remote**. Halaman awal menampilkan QR + link + PIN.
3. HP dan box satu jaringan (disarankan box lewat **LAN kabel**).
   Buka alamat itu di browser HP, masukkan PIN.
4. Tempel link pertama dari HP — halaman harus tampil di TV.
5. Uji sesuai rencana: situs ringan, situs video (coba fullscreen),
   situs yang butuh login, dan halaman favorit.

Bila ada error saat build pertama di Android Studio, kirim pesan errornya
— perbaikan dilakukan dari sana.

## Struktur singkat

- `MainActivity.kt` — WebView + implementasi `BrowserController`
  (satu-satunya pemilik WebView).
- `BrowserController.kt` — kontrak perintah remote ↔ WebView.
- `RemoteServer.kt` — HTTP server: halaman remote, `/cmd`, `/state`,
  `/favorites`, semua ber-PIN.
- `ServerService.kt` — foreground service: PIN, alamat IP, notifikasi.
- `BootReceiver.kt` — server menyala setelah booting.
- `FavoritesStore.kt` — favorit di SharedPreferences.
- `assets/remote.html` — halaman remote HP (satu file, tanpa build).
- `assets/home.html` — halaman awal lokal di TV (form link/keyword yang
  dikirim lewat skema `tvbrowser://` dan diputuskan `UrlResolver`).
