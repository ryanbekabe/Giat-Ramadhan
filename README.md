# Aplikasi Giat Ramadhan

Aplikasi Android untuk membantu anak sekolah menjalani kegiatan Ramadhan 1447 H dengan cara yang menyenangkan, terstruktur, dan mudah dipantau setiap hari.

## Sekilas Fitur

- Checklist harian 1-30 Ramadhan.
- Tanggal Ramadhan dilengkapi hari dan tanggal Masehi.
- Checklist Sholat 5 waktu: Subuh, Dzuhur, Ashar, Maghrib, Isya.
- Status puasa harian: `Puasa` atau `Tidak Puasa`.
- Skor harian otomatis (0-100):
  - 5 sholat + puasa bernilai penuh 100.
  - `Tidak Puasa` tidak menambah nilai puasa.
- Ringkasan progres harian langsung di layar utama.
- Grafik kinerja Ramadhan (line chart):
  - Garis hijau: progres sholat per hari.
  - Garis oranye: progres puasa per hari.
- Badge/Achievement berdasarkan streak hari sempurna:
  - Pemula Konsisten (3 hari)
  - Terlatih (7 hari)
  - Hebat (14 hari)
  - Juara Ramadhan (30 hari)
- Animasi ringan + efek suara saat badge naik level.
- Catatan tadarus/mengaji per hari.
- Jadwal waktu sholat harian (Imsak, Subuh, Zuhur, Asar, Magrib, Isya) yang dibaca dari file CSV di `assets`.
- Informasi versi aplikasi saat ini: `20260217`.
- Tombol `Cek Update`:
  - Cek versi online dari `https://hanyajasa.com/apk/giatramadhan/versi.txt`
  - Jika versi lebih baru, unduh APK dari `https://hanyajasa.com/apk/giatramadhan/giatramadhan.apk`

## Penyimpanan Data

Data aktivitas harian disimpan lokal menggunakan `SharedPreferences`, meliputi:

- Checklist sholat per hari
- Status puasa per hari
- Catatan tadarus per hari
- Data ringkasan yang dihitung dari aktivitas harian

## Struktur Data Jadwal Sholat

- File sumber: `app/src/main/assets/Ramadhan1447H.csv`
- Keunggulan: jadwal bisa diperbarui cukup dengan mengganti file CSV tanpa edit kode Kotlin.

## Teknologi

- Kotlin
- Android View System (XML)
- Material Components
- MPAndroidChart
- SharedPreferences
- DownloadManager (fitur update APK)

## About

Apk MU version by hanyajasa.com - hanyajasa@gmail.com - Palangka Raya 2026
