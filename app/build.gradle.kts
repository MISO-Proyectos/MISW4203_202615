import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// URL del backend: se lee de local.properties (git lo ignora) para no editar archivos del
// repo al usar un celular físico. Si no está, se usa la del emulador.
val localProperties = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val baseUrl: String = (localProperties.getProperty("vinilos.baseUrl")?.trim()
    ?.takeIf { it.isNotEmpty() } ?: "http://10.0.2.2:3000/")
    // Retrofit exige que termine en /
    .let { if (it.endsWith("/")) it else "$it/" }

android {
    namespace = "co.edu.uniandes.misw4203.vinilos"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "co.edu.uniandes.misw4203.vinilos"

        // Versión mínima compatible con la aplicación
        minSdk = 21

        // Versión de Android contra la cual se prueba la aplicación
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        // IMPORTANTE: la URL siempre debe terminar en / (ya se arregla arriba)
        buildConfigField(
            "String",
            "BASE_URL",
            "\"$baseUrl\""
        )
    }

    buildFeatures {
        // Genera clases como ActivityMainBinding y FragmentHomeBinding
        viewBinding = true

        // Permite utilizar BuildConfig.BASE_URL
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Dependencias creadas por Android Studio
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.9")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // HU01: lista de álbumes y carga de carátulas
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // HU03: deslizar hacia abajo para recargar la lista
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // Solo en debug: detecta fugas de memoria. No va en release (gasta CPU y memoria al perfilar)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}