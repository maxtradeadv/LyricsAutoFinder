# Lyrics Auto Finder — Android 8.1 compatibility build

This is a compatibility build configuration for the supplied Lyrics Auto Finder source.

## Target
- Android 8.1 / API 27
- `minSdk = 27`
- `targetSdk = 28`
- `compileSdk = 33`
- Java bytecode target 1.8
- Gradle 7.5 / Android Gradle Plugin 7.4.2 / Kotlin plugin 1.8.22
- Debug APK signed with APK Signature Scheme v1 and v2
- No AndroidX dependencies; the app uses Android framework APIs only.

## Build on macOS
1. Install Android SDK Platform 33 and Build Tools (Android Studio > SDK Manager).
2. Use JDK 11 or newer (JDK 11 is sufficient for this toolchain).
3. Open this folder in Android Studio and wait for Gradle Sync.
4. Select Build > Clean Project.
5. Select Build > Build Bundle(s) / APK(s) > Build APK(s).

Output:
`app/build/outputs/apk/debug/app-debug.apk`

## Install/test
On the Android 8.1 emulator:
```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Check package metadata:
```sh
$ANDROID_HOME/build-tools/33.0.2/aapt dump badging app/build/outputs/apk/debug/app-debug.apk
```

## What this changes
Only the build toolchain and bytecode target are downgraded to a more legacy-friendly toolchain. App logic, notification listener, LRCLIB lookup, and Android 8.1 minimum remain unchanged. Manifest retains `android:extractNativeLibs="true"` and the notification listener service.

## Important limitations
- This is source code, not a prebuilt APK. Build it on the Mac to produce the APK.
- This does not prove or guarantee acceptance by the YT9217C/HW8227L vendor package installer. If it still reports “Problem parsing the package”, capture the exact installer error and test the APK transfer path/signature.
- Lyrics are fetched from LRCLIB over the internet. The app reads Android MediaSession metadata; it does not identify songs by listening to raw audio. The music player must publish media metadata.
