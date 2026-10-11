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
        versionCode = 22
        versionName = "4.1"
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
            isMinifyEnabled = false
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
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
