# StreamWave Logo Integration Plan

This plan details the implementation steps to integrate the new branding asset `streamwavelogo.png` professionally throughout the application, replacing all legacy branding and icon placeholders.

## Proposed Changes

We will copy the source image `app/src/streamwavelogo.png` to the Android drawable resources directory, integrate it into a new Splash screen, add it to main headers (Home, Sports, Library), place it in empty/error states (Search, Connection Timeouts), and configure it as the launcher icon (adaptive and legacy mipmaps).

---

### Resources Configuration

#### [NEW] [streamwavelogo.png](file:///c:/workspace/cineverse%20%281%29/app/src/main/res/drawable/streamwavelogo.png)
- Copy the logo from `app/src/streamwavelogo.png` into `app/src/main/res/drawable/streamwavelogo.png` so it can be referenced in layout resources and Jetpack Compose.

#### [MODIFY] [ic_launcher_background.xml](file:///c:/workspace/cineverse%20%281%29/app/src/main/res/drawable/ic_launcher_background.xml)
- Change background path to a solid dark grey (`#121212`) color instead of the Android green grid pattern.

#### [MODIFY] [ic_launcher_foreground.xml](file:///c:/workspace/cineverse%20%281%29/app/src/main/res/drawable/ic_launcher_foreground.xml)
- Overwrite this file with a `layer-list` containing `@drawable/streamwavelogo` centered at `72dp` size to serve as the foreground of the adaptive launcher icon.

#### [DELETE/NEW] Legacy Mipmaps
- Run a PowerShell script to clean up legacy `ic_launcher.webp` and `ic_launcher_round.webp` files from `mipmap-*` folders.
- Generate legacy PNG versions of `ic_launcher.png` and `ic_launcher_round.png` resized correctly for each density:
  - `mipmap-mdpi` (48x48)
  - `mipmap-hdpi` (72x72)
  - `mipmap-xhdpi` (96x96)
  - `mipmap-xxhdpi` (144x144)
  - `mipmap-xxxhdpi` (192x192)

---

### App Navigation & Splash Screen

#### [NEW] [SplashScreen.kt](file:///c:/workspace/cineverse%20%281%29/app/src/main/java/com/example/ui/screens/SplashScreen.kt)
- Create a brand new splash screen with a sleek black background, smooth Scale/Fade-In animations for `streamwavelogo.png`, subtitle text, and a automatic 2-second transition timer.

#### [MODIFY] [AppNavigation.kt](file:///c:/workspace/cineverse%20%281%29/app/src/main/java/com/example/ui/navigation/AppNavigation.kt)
- Define `Screen.Splash = "splash"` in the `Screen` object.
- Change the `startDestination` of `NavHost` to `Screen.Splash`.
- Add a new `composable` route for `Screen.Splash` that navigates to `Screen.TabsShell` when done, clearing the backstack.

---

### Top Bar Branding & Headers

#### [MODIFY] [HomeScreen.kt](file:///c:/workspace/cineverse%20%281%29/app/src/main/java/com/example/ui/screens/HomeScreen.kt)
- Add a beautiful top bar row as the first item in the LazyColumn displaying the logo image (36.dp, 1:1) and the application title "StreamWave" in bold black letter spacing.
- Add the branding logo centered above the "Connection Timed Out" empty state text.

#### [MODIFY] [LibraryScreen.kt](file:///c:/workspace/cineverse%20%281%29/app/src/main/java/com/example/ui/screens/LibraryScreen.kt)
- Replace the text-based `"My CineVerse"` header with a horizontal `Row` rendering the logo image (32.dp, 1:1) and the text `"My StreamWave"`.

#### [MODIFY] [SportsHomeScreen.kt](file:///c:/workspace/cineverse%20%281%29/app/src/main/java/com/example/sports/ui/SportsHomeScreen.kt)
- Add the logo (28.dp, 1:1) next to the "Sports" text inside the top bar header.

---

### Empty States & Error Screens

#### [MODIFY] [MovieTabScreen.kt](file:///c:/workspace/cineverse%20%281%29/app/src/main/java/com/example/ui/screens/movie/MovieTabScreen.kt)
- Add the logo image (80.dp, 1:1) above the "Connection Timed Out" retry section.

#### [MODIFY] [SearchScreen.kt](file:///c:/workspace/cineverse%20%281%29/app/src/main/java/com/example/ui/screens/SearchScreen.kt)
- Add the logo image (60.dp, 1:1) above the "Search Failed" text.
- Add the logo image (60.dp, 1:1) above the "No results found" text.

---

## Verification Plan

### Automated Verification
- Compile and build the project using Gradle command:
  ```powershell
  ./gradlew assembleDebug
  ```

### Manual Verification
- Verify that resources contain `streamwavelogo.png` and compilation succeeds without resource conflicts.
- Confirm adaptive icon XMLs parse correctly.
