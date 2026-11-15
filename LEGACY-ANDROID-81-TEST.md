# Android 8.1 compatibility build notes

This variant preserves the app's Android framework-only implementation and targets API 27 devices.

Compatibility choices:
- compileSdk 33 (build-time API set; not the minimum runtime version)
- minSdk 27 (Android 8.1)
- targetSdk 28
- Java/Kotlin bytecode target 1.8
- AGP 7.4.2 + Gradle 7.5 + Kotlin 1.8.22
- V1 + V2 debug APK signatures
- `android:extractNativeLibs="true"`

No attempt is made to rewrite app functionality or to lower minSdk to iGO's API 15. This app uses MediaSession APIs introduced after API 15, so copying iGO's minimum SDK would be incorrect.
