# Lyrics Auto Finder — Android 8.1 compatibility build

A lightweight Android app that reads the active music player's MediaSession metadata, looks up lyrics from LRCLIB, and displays synchronized lyrics when available.

## Android compatibility
- Minimum Android: 8.1 (API 27)
- Target SDK: 28
- Compile SDK: 33
- Java/Kotlin bytecode target: 1.8
- Android Gradle Plugin: 8.7.3
- Kotlin Gradle Plugin: 2.0.21
- Gradle wrapper: 8.9
- Debug APK signing: APK Signature Scheme v1 and v2
- No AndroidX runtime dependencies

## Build on Windows
1. Install Android Studio and Android SDK Platform 33 using SDK Manager.
2. Open the repository root folder in Android Studio.
3. Set **Gradle JDK** to Android Studio's bundled JDK 17 or newer (JDK 21 is suitable).
4. Allow Gradle Sync to finish.
5. Select **Build > Clean Project**, then **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

The project is a single-module Android application with its Gradle build file at the repository root.

## APK output
`build/outputs/apk/debug/app-debug.apk`

Install using ADB:
```powershell
adb install -r build/outputs/apk/debug/app-debug.apk
```

## How lyrics are detected
The app reads the active player's Android MediaSession metadata through a notification listener. The player must publish track metadata. The app does not identify songs by recording or listening to raw audio.

Lyrics are retrieved from LRCLIB over the internet. Synchronized highlighting depends on synchronized lyrics being available for the track.

## Android 8.1 head-unit note
The project targets API 27+ and is configured for legacy APK v1/v2 signing. This configuration does not guarantee that every vendor head-unit package installer will accept the APK. Test the built APK on the target device.
