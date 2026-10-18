# Aurora Launcher

A modern, animation-heavy Android home screen (Kotlin + Jetpack Compose).

## Features
- Animated aurora background that drifts and shifts as you swipe pages
- Icons pop in with a springy stagger; press-bounce on every icon
- 3D page-turn swipe between home pages, stretchy page dots
- Live clock card with rolling digits
- Frosted-glass dock (phone, messages, browser, camera - your defaults)
- Swipe up anywhere: app drawer with live search (drag down to close)
- App icons are re-shaped into one consistent rounded square
- Tap an icon: it zooms open from its own position
- Long-press an icon: App info / Uninstall
- Home button returns to page 1 and closes the drawer

## Get the APK (no Android Studio needed)
1. Create a free GitHub account and a new empty repository.
2. Upload everything in this folder (including the hidden `.github` folder) to the repository's `main` branch.
3. Open the repository's **Actions** tab. The "Build APK" workflow starts by itself (about 5 minutes).
4. Open the finished run, download the **AuroraLauncher-apk** artifact, unzip it, and you get `app-release.apk`.

## Install on your phone
1. Copy `app-release.apk` to the phone and open it. Allow "install unknown apps" when asked.
2. Press the Home button and choose **Aurora** -> **Always**.
   (Or: Settings -> Apps -> Default apps -> Home app -> Aurora.)
3. To go back to your old launcher, choose it in the same setting.

## Build with Android Studio instead
Open this folder in Android Studio, let it sync, then Build -> Build APK(s).
