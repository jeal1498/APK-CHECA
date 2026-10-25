plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.karen.miruta"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        versionCode = 75
        versionName = "9.5"
    }

    buildFeatures { buildConfig = true }

    // La firma del canal antiguo solo se configura localmente mediante variables de entorno.
    // No guardar contraseñas ni archivos de firma en el repositorio.
    val legacyKeystorePath = providers.environmentVariable("MIRUTA_KEYSTORE_PATH").orNull
    val legacyStorePassword = providers.environmentVariable("MIRUTA_STORE_PASSWORD").orNull
    val legacyKeyPassword = providers.environmentVariable("MIRUTA_KEY_PASSWORD").orNull
    val legacyKeyAlias = providers.environmentVariable("MIRUTA_KEY_ALIAS").orNull ?: "miruta"
    // Llave exclusiva de subida a Google Play; solo existe en el runner del workflow manual.
    val playKeystorePath = providers.environmentVariable("PLAY_UPLOAD_KEYSTORE_PATH").orNull
    val playStorePassword = providers.environmentVariable("PLAY_UPLOAD_STORE_PASSWORD").orNull
    val playKeyPassword = providers.environmentVariable("PLAY_UPLOAD_KEY_PASSWORD").orNull
    val playKeyAlias = providers.environmentVariable("PLAY_UPLOAD_KEY_ALIAS").orNull ?: "checa-upload"

    signingConfigs {
        if (!legacyKeystorePath.isNullOrBlank() &&
            !legacyStorePassword.isNullOrBlank() &&
            !legacyKeyPassword.isNullOrBlank() &&
            file(legacyKeystorePath).isFile
        ) {
            create("propia") {
                storeFile = file(legacyKeystorePath)
                storePassword = legacyStorePassword
                keyAlias = legacyKeyAlias
                keyPassword = legacyKeyPassword
            }
        }
        if (!playKeystorePath.isNullOrBlank() &&
            !playStorePassword.isNullOrBlank() &&
            !playKeyPassword.isNullOrBlank() &&
            file(playKeystorePath).isFile
        ) {
            create("playUpload") {
                storeFile = file(playKeystorePath)
                storePassword = playStorePassword
                keyAlias = playKeyAlias
                keyPassword = playKeyPassword
            }
        }
    }

    flavorDimensions += "canal"
    productFlavors {
        // Canal web (el de siempre): misma firma, mismo paquete, con auto-actualización.
        create("directo") {
            dimension = "canal"
            applicationId = "com.karen.miruta"
            signingConfig = signingConfigs.findByName("propia")
            buildConfigField("boolean", "ES_PLAY", "false")
        }
        // Google Play: se firma solo cuando el workflow manual proporciona la llave de subida.
        create("play") {
            dimension = "canal"
            applicationId = "com.jeal1498.checa"
            signingConfig = signingConfigs.findByName("playUpload")
            buildConfigField("boolean", "ES_PLAY", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

// build.yml no cambia: "gradle assembleRelease" compila solo el canal directo y deja el APK
// en la ruta de siempre (app/build/outputs/apk/release/app-release.apk).
afterEvaluate {
    tasks.named("assembleRelease") { setDependsOn(listOf("assembleDirectoRelease")) }
    tasks.named("assembleDirectoRelease") {
        doLast {
            val origen = File(project.layout.buildDirectory.get().asFile, "outputs/apk/directo/release")
                .listFiles { f -> f.extension == "apk" }?.firstOrNull()
                ?: error("No se encontró el APK del canal directo")
            val destino = File(project.layout.buildDirectory.get().asFile, "outputs/apk/release/app-release.apk")
            destino.parentFile.mkdirs()
            origen.copyTo(destino, overwrite = true)
        }
    }
}
