# Lyrics Auto Finder — compatibility build

This revision makes two packaging changes for testing on older/vendor Android 8.1 head units:

- Explicitly sets `android:extractNativeLibs="true"` in the application manifest.
- Enables APK Signature Scheme v1 and v2 for the debug build. Android 8.1 supports v2; v1 is added as a compatibility fallback for vendor installers.

## Build on macOS

1. Open this folder in Android Studio.
2. Let Gradle sync. If prompted, use the Android SDK already installed on your Mac.
3. Select **Build → Clean Project**, then **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Use `app/build/outputs/apk/debug/app-debug.apk`.

Do not copy `local.properties` from another computer; Android Studio should generate it for the local SDK.

## Verify the resulting APK (optional)

In Terminal, run (adjust SDK path if needed):

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
APK="$HOME/Downloads/LyricsAutoFinder-AndroidStudio/app/build/outputs/apk/debug/app-debug.apk"
"$HOME/Library/Android/sdk/build-tools/35.0.0/apksigner" verify --verbose "$APK"
"$HOME/Library/Android/sdk/build-tools/35.0.0/aapt" dump badging "$APK" | head -8
```

Expected signature verification should show both v1 and v2 as `true`. The package should report `sdkVersion:'27'` and `targetSdkVersion:'28'`.

**Important:** This is a source/configuration patch, not a prebuilt or device-tested APK. The build must be produced on your Mac, and installation still needs to be tested on the head unit.
