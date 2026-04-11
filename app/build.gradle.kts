plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.cj.transporte"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cj.transporte"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // BOM do Compose (Atualizado para estabilidade)
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))

    // Bibliotecas Principais do Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // ADICIONADO: Para ícones como o de Lock (Cadeado)

    // Integração
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7") // ADICIONADO: Para suporte a collectAsStateWithLifecycle

    // Debug Preview
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Room
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // Firebase (CORRIGIDO: Removido -ktx que causava erro de referência)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2")) // Versão estável
    implementation("com.google.firebase:firebase-firestore") // Sem o -ktx
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-common-ktx") // Apenas para compatibilidade de base

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.github.DantSu:ESCPOS-ThermalPrinter-Android:3.3.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}