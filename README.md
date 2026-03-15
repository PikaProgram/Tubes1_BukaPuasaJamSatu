# BukaPuasaJamSatu

Repository ini memiliki 3 bot (satu bot utama dan 2 alternatif) yang digunakan dalam permainan Battlecode 2025

## Penjelasan Singkat Algoritma Greedy

- Bot "MyBotGweh" (Bot utama)\
Strategi Greedy yang dipilih adalah Local Adaptif dan Priority Based, dengan Heurestik yang fokus Paint Area dan juga Attack low HP. Alasan memilih strategi greedy ini adalah karena setiap unit punya peran masing masing dimana efisiensi mereka sangat bergantung pada situasi dan juga ukuran map. Karena konsep
- Bot "Laliro"\
Pendekatan dengan menggunakan algoritma greedy berbasis prioritas berdasarkan sumber daya dengan pemindaian lokal. Aksi tiap robot sudah memiliki prioritas yang terstruktur, tetapi dapat berubah berdasarkan hal-hal di sekitar robot.
- Bot "billbot"\
Setiap tipe unit dalam bot menggunakan pendekatan greedy yang sama. Meskipun strateginya konsisten di seluruh program, heuristik spesifiknya berbeda untuk Soldier, Mopper, Splasher, dan Tower.




### Configuration 

Set teamA, teamB berdasarkan nama bot yang dipilih dari file src\
ex: \
teamA: billbot\
teamB: laliro\
map: SmallDefault


### Langkah-langkah compile/Build
1. Pastikan sudah menjadikan lokasi repo sebagai main directory
2. Lakukan konfigurasi bot yang dipilih melalui file gradle.properties
3. Jalankan command ./gradlew build
4. Jalankan command ./gradlew run, yang akan menghasilkan replay yang akan disimpan dalam folder matches
5.Buka folder client, jalankan aplikasi Stima Battle Client.exe
6. Dalam aplikasi, pilih "Queue" dan upload file replay dari folder matches sebelumnya.

### Credits
- 13524121 (Billy Ontoseno Irawan)
- 13524125 (Muhammad Rafi Akbar)
- 13524142 (Rasyad Satyatma)
