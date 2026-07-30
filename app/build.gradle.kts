import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Local, developer-only signing config. Never committed (see .gitignore) — it exists so a
// contributor can point their own debug builds at the release cert and replace a release
// build in place with `adb install -r`. CI never reads this file; it sources the same four
// values from repo secrets instead (see .github/workflows/build.yml).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}
val hasLocalKeystoreProps = keystorePropsFile.exists()

fun signingValue(envName: String, propKey: String): String? =
    System.getenv(envName) ?: keystoreProps.getProperty(propKey)

android {
    namespace = "com.gios.lightocr"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.gios.lightocr"
        minSdk = 29
        targetSdk = 35
        // CI overwrites both from the workflow run number; see .github/workflows/build.yml
        versionCode = 1
        versionName = "1.0.0"

        // The LPIII is arm64 only.
        ndk { abiFilters += "arm64-v8a" }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingValue("RELEASE_STORE_FILE", "storeFile")
                ?: "../keystore/lightocr.jks"
            storeFile = file(storeFilePath)
            storePassword = signingValue("RELEASE_STORE_PASSWORD", "storePassword")
            keyAlias = signingValue("RELEASE_KEY_ALIAS", "keyAlias")
            keyPassword = signingValue("RELEASE_KEY_PASSWORD", "keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // Only when a developer has their own keystore.properties: point debug at the
            // same release signing config so a local debug install can replace a release
            // build in place instead of erroring with an install conflict. Without a local
            // keystore.properties (e.g. a fresh checkout, or check.yml in CI) debug keeps
            // the auto-generated debug keystore, which never needs any secret.
            if (hasLocalKeystoreProps) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Room, for the scan history.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // CameraX.
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // EXIF orientation for photos picked from the gallery.
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Thumbnail loading in the history list.
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ML Kit text recognition — the BUNDLED variant. The model ships inside the APK and
    // never touches Google Play Services at runtime, which is the only variant that works
    // on the LPIII. Do not swap this for com.google.android.gms:play-services-mlkit-* —
    // that variant downloads its model through Play Services, which this phone has none of.
    implementation("com.google.mlkit:text-recognition:16.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    testImplementation("junit:junit:4.13.2")
}
