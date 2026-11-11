import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.android)
}

// Las API keys viven en local.properties (ignorado por git) y llegan al código vía BuildConfig
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
fun claveLocal(nombre: String): String = propiedadesLocales.getProperty(nombre, "")

android {
    namespace = "com.tecsup.agroscan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tecsup.agroscan"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "ROBOFLOW_API_KEY", "\"${claveLocal("ROBOFLOW_API_KEY")}\"")
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"${claveLocal("OPENWEATHER_API_KEY")}\"")
        // URL base de la plataforma web (p. ej. https://mi-api.up.railway.app/); vacía = sin sincronización
        buildConfigField("String", "WEB_API_URL", "\"${claveLocal("WEB_API_URL")}\"")
    }

    buildTypes {
        release {
            // R8 elimina y optimiza código (Compose corre mucho más fluido en release que en debug)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Firma de depuración para poder instalar y probar el APK release; cambiar por la de publicación
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        // El modelo TFLite se mapea en memoria desde assets: no debe comprimirse
        noCompress += "tflite"
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.osmdroid.android)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.graphics.path)
    implementation(libs.androidx.compose.ui)
    implementation("androidx.compose.runtime:runtime")
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // LiteRT (TFLite) para el modelo YOLO11 local
    implementation(libs.litert)

    // Jetpack Glance (App Widgets)
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.glance:glance-material3:1.2.0")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}