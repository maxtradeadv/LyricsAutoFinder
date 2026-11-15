plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.maxsave.lyricsautofinder"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.maxsave.lyricsautofinder"
        minSdk = 27
        targetSdk = 28
        versionCode = 4
        versionName = "2.2-android81-compat"
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Intentionally framework-only: no AndroidX runtime components.
}
