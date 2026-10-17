plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.karen.miruta"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.karen.miruta"
        minSdk = 26
        targetSdk = 34
        versionCode = 58
        versionName = "7.8"
    }

    signingConfigs {
        create("propia") {
            storeFile = file("miruta.jks")
            storePassword = "miruta123"
            keyAlias = "miruta"
            keyPassword = "miruta123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("propia")
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
}

dependencies {
    implementation("androidx.core:core:1.13.1")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // Iniciar sesión con Google (licencia ligada a la cuenta)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}
