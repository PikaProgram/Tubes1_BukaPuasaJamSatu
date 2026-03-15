# Battlecode 2025 Scaffold - Java

This is the Battlecode 2025 Java scaffold, containing an `examplefuncsplayer`. Read https://play.battlecode.org/bc25java/quick_start !

# Penjelasan Singkat Algoritma Greedy

C. Bot "MyBotGweh" 
Strategi Greedy yang dipilih adalah Local Adaptif dan Priority Based, dengan Heurestik yang fokus Paint Area dan juga Attack low HP. Alasan memilih strategi greedy ini adalah karena setiap unit punya peran masing masing dimana efisiensi mereka sangat bergantung pada situasi dan juga ukuran map. Karena konsep 

### Project Structure

- `README.md`
    This file.
- `build.gradle`
    The Gradle build file used to build and run players.
- `src/`
    Player source code.
- `test/`
    Player test code.
- `client/`
    Contains the client. The proper executable can be found in this folder (don't move this!)
- `build/`
    Contains compiled player code and other artifacts of the build process. Can be safely ignored.
- `matches/`
    The output folder for match files.
- `maps/`
    The default folder for custom maps.
- `gradlew`, `gradlew.bat`
    The Unix (OS X/Linux) and Windows versions, respectively, of the Gradle wrapper. These are nifty scripts that you can execute in a terminal to run the Gradle build tasks of this project. If you aren't planning to do command line development, these can be safely ignored.
- `gradle/`
    Contains files used by the Gradle wrapper scripts. Can be safely ignored.

### How to get started

You are free to directly edit `examplefuncsplayer`.
However, we recommend you make a new bot by copying `examplefuncsplayer` to a new package under the `src` folder.

### Useful Commands

- `./gradlew build`
    Compiles your player
- `./gradlew run`
    Runs a game with the settings in gradle.properties
- `./gradlew update`
    Update configurations for the latest version -- run this often
- `./gradlew zipForSubmit`
    Create a submittable zip file
- `./gradlew tasks`
    See what else you can do!


### Configuration 

Look at `gradle.properties` for project-wide configuration.
Set teamA, teamB berdasarkan nama bot yang dipilih dari file src
ex: 
teamA: billbot
teamB: laliro
map: SmallDefault


### Langkah-langkah compile/Build
1.pastikan sudah menjadikan lokasi repo sebagai main directory
6.Lakukan konfigurasi bot yang dipilih melalui file gradle.properties
2.jalankan command ./gradlew build
3.jalankan command ./gradlew run, yang akan menghasilkan replay yang akan disimpan dalam folder matches
4.buka folder client, jalankan aplikasi Stima Battle Client.exe
5.dalam aplikasi, pilih "Queue" dan upload file replay dari folder matches sebelumnya.



If you are having any problems with the default client, please report to teh devs and
feel free to set the `compatibilityClient` configuration to `true` to download a different version of the client.
