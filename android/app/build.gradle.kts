plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.calcke.qc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.calcke.qc"
        minSdk = 29
        targetSdk = 34
        // Every CI build gets a higher versionCode, so new APKs install over old ones.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
    }

    // A fixed personal key, so every build is signed the same way and updates keep your data.
    signingConfigs {
        create("personal") {
            storeFile = file("personal.keystore")
            storePassword = "quotecards"
            keyAlias = "quotecards"
            keyPassword = "quotecards"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("personal")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    // The web app lives in /web at the repo root and is bundled into the APK as-is.
    sourceSets["main"].assets.srcDirs("../../web")
}

dependencies {
    implementation("androidx.webkit:webkit:1.11.0")
    implementation("androidx.core:core-ktx:1.13.1")
}
