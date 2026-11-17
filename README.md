# Calorie Counter

Aplikasi Android sederhana untuk mencatat kalori harian. Cukup ketik angka kalori, tanpa perlu memilih makanan.

## Fitur

- Catat kalori **masuk** atau **terbakar** (olahraga) lewat satu bottom sheet
- Catatan opsional per entri
- Dashboard harian: kalori masuk, terbakar, netto, dan progress terhadap target
- Target kalori harian yang bisa diubah (default 2000 kkal)
- Riwayat 30 hari terakhir
- Konfirmasi sebelum menghapus entri
- Data tersimpan lokal di perangkat (offline)

## Tech Stack

- Kotlin + Jetpack Compose (Material 3)
- Room (via KSP) untuk database lokal
- DataStore untuk menyimpan target harian
- MVVM (ViewModel + Repository)
- Navigation Compose
- Gradle 9.5, AGP 9.3.2, min SDK 26, target SDK 36

## Struktur

```
app/src/main/java/com/fahriza/caloriecounter/
├── data/         FoodEntry, FoodDao, AppDatabase
├── repository/   FoodRepository
├── viewmodel/    CalorieViewModel
├── ui/screens/   HomeScreen, HistoryScreen
├── ui/theme/     Theme
└── MainActivity.kt
```

## Build

Prasyarat: Android SDK (platform 36.1) dan akses internet untuk mengunduh JDK 21 lewat toolchain Gradle.

Buat `local.properties`:

```
sdk.dir=/path/ke/Android/Sdk
```

Debug APK:

```bash
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Release APK (butuh keystore, lihat bagian Signing):

```bash
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

## Signing

`app/build.gradle.kts` membaca keystore `calorie-counter.keystore` di root project. Buat keystore sendiri:

```bash
keytool -genkeypair -keystore calorie-counter.keystore -alias caloriecounter \
  -keyalg RSA -keysize 2048 -validity 10000
```

Lalu sesuaikan password di `signingConfigs` pada `app/build.gradle.kts`. Jangan commit keystore dan password ke repository publik.

## Instalasi di HP

1. Salin APK ke HP
2. Aktifkan "Install aplikasi dari sumber tidak dikenal"
3. Buka file APK dan tap Install

Atau via adb:

```bash
adb install app/build/outputs/apk/release/app-release.apk
```
