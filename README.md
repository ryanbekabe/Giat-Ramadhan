# Aplikasi Giat Ramadhan

Aplikasi Android untuk membantu anak sekolah menjalani kegiatan Ramadhan 1447 H dengan cara yang menyenangkan, terstruktur, dan mudah dipantau setiap hari.

## Sekilas Fitur

- Checklist harian 1-30 Ramadhan.
- Tanggal Ramadhan dilengkapi hari dan tanggal Masehi.
- Checklist Sholat 5 waktu: Subuh, Dzuhur, Ashar, Maghrib, Isya.
- Status puasa harian: `Puasa` atau `Tidak Puasa`.
- Skor harian otomatis (0-100): 5 sholat + puasa bernilai penuh 100.
- Ringkasan progres harian langsung di layar utama.
- Grafik kinerja Ramadhan (line chart) dengan sumbu hari lengkap `1-30`:
  - Garis hijau: progres sholat per hari.
  - Garis oranye: progres puasa per hari.
- Heatmap 30 hari untuk melihat performa ibadah bulanan secara cepat.
- Badge/Achievement berdasarkan streak hari sempurna:
  - Pemula Konsisten (3 hari)
  - Terlatih (7 hari)
  - Hebat (14 hari)
  - Juara Ramadhan (30 hari)
- Animasi ringan + efek suara saat badge naik level.
- Catatan tadarus/mengaji per hari.
- Jadwal waktu sholat harian (Imsak, Subuh, Zuhur, Asar, Magrib, Isya) yang dibaca dari file CSV di `assets`.
- Informasi versi aplikasi saat ini: `20260218`.
- Tombol `Cek Update`:
  - Cek versi online dari `https://hanyajasa.com/apk/giatramadhan/versi.txt`
  - Jika versi lebih baru, unduh APK dari `https://hanyajasa.com/apk/giatramadhan/giatramadhan.apk`
- Statistik startup otomatis:
  - Setelah aplikasi berjalan 5 detik dan internet aktif, aplikasi mengirim statistik perangkat Android dan versi aplikasi ke:
  - `https://hanyajasa.com/?StatistikGiatRamadhan=...`

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
- HttpURLConnection + ConnectivityManager (fitur statistik startup)

## Download APK

Unduh file APK terbaru melalui halaman rilis berikut:

https://github.com/ryanbekabe/Giat-Ramadhan/releases

## Donasi Pengembangan

Jika ingin mendukung pengembangan aplikasi ini, Anda dapat melakukan donasi melalui transfer QRIS dengan scan gambar berikut:

![QRIS Donasi](6170174619466271873.jpg)
```
BSI     :  9692999170
BCA     : 8600432053
BRI     : 4543-01-020754-53-0
Mandiri : 159-00-0068323-4
Muamalat : 6310042068
Dana : 082254205110
Jenius : 90110062490
Jago : 101396991206
Seabank : 901899706783
```

## About

Apk MU version by hanyajasa.com - hanyajasa@gmail.com - Palangka Raya 2026
