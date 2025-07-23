plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // If you plan to use Room or other annotation processors, you might need this:
    // alias(libs.plugins.ksp) // Ensure this is also defined in your libs.versions.toml and project build.gradle.kts
}

android {
    namespace = "com.sheikyon.spectratron"
    compileSdk = 35 // ¡Esto ya lo cambiaste, genial!

    defaultConfig {
        applicationId = "com.sheikyon.spectratron"
        minSdk = 29
        targetSdk = 35 // También sube el targetSdk a 35 para que coincida con compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11" // Ejemplo, verifica con tu libs.versions.toml
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) // Esto trae muchas dependencias de Compose
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // *******************************************************************
    // ¡¡¡NUEVAS DEPENDENCIAS QUE NECESITAS AÑADIR!!!
    // *******************************************************************

    // CameraX (Ajusta la versión a la más reciente y estable)
    // POR QUÉ: Son las herramientas principales para interactuar con la cámara del móvil.
    // CÓMO: Cada línea trae una parte específica de CameraX (el núcleo, la conexión con la cámara2,
    //       la integración con el ciclo de vida de la app y la vista previa).
    val cameraXVersion = "1.3.3" // Revisa la documentación oficial para la última estable
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion") // Para PreviewView

    // Kotlin Coroutines (para tareas en segundo plano)
    // POR QUÉ: Permiten que tu app haga cosas "en secreto" (como analizar la imagen)
    //          sin congelar la pantalla o hacer que el móvil se sienta lento.
    // CÓMO: Proporcionan una forma moderna y eficiente de manejar el trabajo asíncrono.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0") // O la última versión estable
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0") // Para usar Coroutines en Android

    // ViewModel y Lifecycle para Compose (para el "cerebro" de tu pantalla)
    // POR QUÉ: Permiten que el 'CameraViewModel' que definimos funcione correctamente
    //          y se integre con Jetpack Compose.
    // CÓMO: Son librerías de Jetpack que facilitan la gestión del estado de la UI.
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0") // O la última versión estable
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0") // O la última versión estable

    // Permisos (Google Accompanist Permissions)
    // POR QUÉ: Simplifica mucho la tarea de pedir permisos al usuario (como el de la cámara)
    //          y de saber si ya los ha dado o si hay que darle una explicación.
    // CÓMO: Proporciona funciones @Composable que manejan la lógica de permisos por ti.
    val accompanistVersion = "0.34.0" // Revisa la última versión estable en Maven Central (search.maven.org)
    implementation("com.google.accompanist:accompanist-permissions:$accompanistVersion")

    // *******************************************************************
    // FIN DE LAS NUEVAS DEPENDENCIAS
    // *******************************************************************

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}