plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val appVersion = (findProperty("appVersion") as String?) ?: "1.0.0"
val appVersionCode = ((findProperty("appVersionCode") as String?) ?: "1").toInt()

// Assinatura opcional via variáveis de ambiente (nada de segredos no repositório).
// Sem elas, o APK de release é assinado com a chave de debug gerada automaticamente,
// o que permite instalar o APK diretamente no aparelho.
val keystorePath: String? = System.getenv("HAO_KEYSTORE_PATH")

android {
    namespace = "com.haoalert.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.haoalert.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion
    }

    signingConfigs {
        if (!keystorePath.isNullOrBlank()) {
            create("haoRelease") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("HAO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("HAO_KEY_ALIAS")
                keyPassword = System.getenv("HAO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (!keystorePath.isNullOrBlank()) {
                signingConfigs.getByName("haoRelease")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.3")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Apenas para a rede de segurança que confere se os alarmes continuam agendados.
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
