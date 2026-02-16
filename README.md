# Aplikasi Giat Ramadhan

Aplikasi Android untuk membantu anak sekolah menjalani kegiatan Ramadhan 1447 H dengan cara yang lebih menyenangkan, terstruktur, dan terpantau setiap hari.

## Fitur Utama

- Checklist harian tanggal 1 sampai 30 Ramadhan.
- Checklist Sholat 5 waktu:
  - Subuh
  - Dzuhur
  - Ashar
  - Maghrib
  - Isya
- Status puasa harian:
  - Puasa
  - Tidak Puasa
- Skor harian otomatis (0-100):
  - Nilai penuh 100 jika semua kegiatan terpenuhi.
  - Status "Tidak Puasa" tidak menambah nilai puasa.
- Ringkasan progres harian langsung di layar utama.
- Grafik kinerja Ramadhan (line chart):
  - Garis hijau: progres sholat per hari.
  - Garis oranye: status puasa per hari.
- Badge/Achievement berdasarkan konsistensi hari sempurna berturut-turut:
  - Pemula Konsisten (3 hari)
  - Terlatih (7 hari)
  - Hebat (14 hari)
  - Juara Ramadhan (30 hari)
- Animasi ringan + efek suara singkat saat badge naik level.
- Catatan tadarus/mengaji per hari (contoh: "1 Ramadhan membaca Al Baqarah ayat 255").

## Penyimpanan Data

Semua data disimpan secara lokal di perangkat menggunakan `SharedPreferences`, termasuk:

- Checklist sholat per hari
- Status puasa per hari
- Skor/achievement yang dihitung dari data harian
- Catatan tadarus per hari

Data tersimpan otomatis saat pengguna mengubah checklist, status puasa, atau isi catatan.

## Teknologi

- Kotlin
- Android View System (XML)
- Material Components
- MPAndroidChart (grafik)
