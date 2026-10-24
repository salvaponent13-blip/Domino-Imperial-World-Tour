import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.dominograndprix"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.horts.dominoimperial"
        minSdk = 24
        targetSdk = 36
        versionCode = 20
        versionName = "2.5.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ------------------------------------------------------------------------------------
        // AdMob: ÚNICO punto de configuración de los IDs de anuncios.
        // IDs reales de producción vinculados a la app oficial en AdMob.
        // ------------------------------------------------------------------------------------
        val admobAppId = "ca-app-pub-8601373307349680~4429473161"
        val admobBannerId = "ca-app-pub-8601373307349680/5563425588"
        val admobInterstitialId = "ca-app-pub-8601373307349680/9311098905"
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")
    }

    signingConfigs {
        create("release") {
            val sf = keystoreProperties.getProperty("storeFile")
            if (sf != null) {
                storeFile = file(sf)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // IDs de PRUEBA oficiales de Google en desarrollo: probar con los
            // reales genera impresiones propias (tráfico inválido).
            val testAppId = "ca-app-pub-3940256099942544~3347511713"
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "ADMOB_APP_ID", "\"$testAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        }
        release {
            // NOTA SOBRE R8 / MINIFICACION (leer antes de activar):
            //
            // Las reglas de app/proguard-rules.pro ya estan escritas y cubren
            // kotlinx.serialization (que es lo unico reflectivo que usa la app: la
            // persistencia de la carrera). Pero mientras no se pueda EJECUTAR el APK
            // minificado en un dispositivo real, activar R8 es un riesgo a ciegas:
            // los tests unitarios corren sobre codigo sin ofuscar y no dicen nada al
            // respecto.
            //
            // Para activarlo con seguridad:
            //   1. Poner isMinifyEnabled = true
            //   2. ./gradlew :app:connectedDebugAndroidTest  (instala en un dispositivo
            //      o emulador y ejecuta CareerProgressInstrumentedTest, que comprueba
            //      que la persistencia sobrevive a la ofuscacion)
            //   3. Verificar ademas que la app abre y suena con normalidad
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      buildConfig = true
      aidl = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

/**
 * Propaga propiedades del sistema al JVM de los tests, para poder ampliar la muestra o
 * activar mediciones exploratorias sin tocar codigo:
 *   ./gradlew test -Dbenchmark.full=true      # benchmark de IA a escala completa (4800 rondas)
 *   ./gradlew test -Dbenchmark.tuning=true    # ajuste exploratorio de pesos
 * Sin propiedades, el banco juega 2000 rondas (lo que corre en CI).
 */
tasks.withType<Test>().configureEach {
    systemProperty("benchmark.full", providers.systemProperty("benchmark.full").getOrElse("false"))
    systemProperty("benchmark.tuning", providers.systemProperty("benchmark.tuning").getOrElse("false"))
}

dependencies {
    // Google UMP: consentimiento RGPD para anuncios en el EEE/Reino Unido.
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Persistencia del progreso de la carrera (JSON en SharedPreferences).
  // La versión la gestiona el BOM de kotlinx.serialization que ya arrastra androidx.lifecycle.
  implementation(libs.kotlinx.serialization.json)

  // Publicidad AdMob
  implementation(libs.play.services.ads)

  // Google Play Billing (IAP quitar publicidad)
  implementation(libs.google.billing)
}



