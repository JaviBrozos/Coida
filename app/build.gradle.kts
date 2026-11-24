import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * Configuración del asistente de voz (D-25). Vive en local.properties, que está
 * en .gitignore, para que la clave no llegue nunca al repositorio. Cambiar de
 * proveedor o de modelo es cambiar estos valores: el cliente habla el formato de
 * OpenAI. Sin clave, el asistente no aparece.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Las claves eran doado.ai.* antes del cambio de nombre (D-62): se siguen leyendo
// para que un local.properties sin tocar no apague el asistente sin avisar.
fun aiSetting(name: String, default: String = ""): String {
    val value = (localProperties.getProperty("coida.ai.$name")
        ?: localProperties.getProperty("doado.ai.$name", default)).trim()
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

/*
 * Firma de la versión que se sube a Google Play. La ruta del keystore y las
 * contraseñas viven en keystore.properties, en la raíz y fuera de git, igual que
 * el propio .jks. Claves: storeFile, storePassword, keyAlias y keyPassword.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    // Compilador de Compose. Kotlin en sí NO se declara aquí: desde AGP 9 viene
    // integrado en el plugin de Android (ver nota en CLAUDE.md).
    alias(libs.plugins.kotlin.compose)
}

android {
    // namespace = paquete raíz del código (com.coida.launcher, com.coida.data, ...).
    // Es distinto del applicationId a propósito: así el paquete de feature del
    // launcher puede llamarse "launcher" sin quedar como com.coida.launcher.launcher.
    namespace = "com.coida"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // Definitivo: una vez en Google Play no se puede cambiar (D-62).
        applicationId = "com.coida.launcher"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        // Empieza de cero con el applicationId nuevo: es otra app para Android y
        // para Play, así que no choca con la 1.4 de las pruebas (D-62).
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        if (!keystoreProperties.isEmpty) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // El asistente solo existe en depuración (D-63): la versión pública sale sin
        // él, sin su clave y sin los permisos de micrófono e internet, que se
        // declaran en src/debug/AndroidManifest.xml.
        debug {
            buildConfigField("boolean", "ASSISTANT_ENABLED", "true")
            // La bola roja de llamadas perdidas (D-66): su permiso, READ_CALL_LOG, va
            // en src/debug y src/familia, nunca en main.
            buildConfigField("boolean", "MISSED_CALLS_ENABLED", "true")
            buildConfigField("String", "AI_BASE_URL", aiSetting("baseUrl", "https://api.groq.com/openai/v1"))
            buildConfigField("String", "AI_STT_MODEL", aiSetting("sttModel", "whisper-large-v3"))
            buildConfigField("String", "AI_LLM_MODEL", aiSetting("llmModel", "openai/gpt-oss-120b"))
            // Solo para modelos que razonan (gpt-oss): "low" contesta antes. Vacío = no se envía.
            buildConfigField("String", "AI_REASONING_EFFORT", aiSetting("reasoningEffort", "low"))
            buildConfigField("String", "AI_API_KEY", aiSetting("apiKey"))
        }
        release {
            // Vacíos a propósito: ni la clave ni el proveedor llegan al .aab.
            buildConfigField("boolean", "ASSISTANT_ENABLED", "false")
            // Google Play no deja a un launcher leer el registro de llamadas (D-66).
            buildConfigField("boolean", "MISSED_CALLS_ENABLED", "false")
            buildConfigField("String", "AI_BASE_URL", "\"\"")
            buildConfigField("String", "AI_STT_MODEL", "\"\"")
            buildConfigField("String", "AI_LLM_MODEL", "\"\"")
            buildConfigField("String", "AI_REASONING_EFFORT", "\"\"")
            buildConfigField("String", "AI_API_KEY", "\"\"")

            // Sin keystore.properties se firma con la clave de depuración, solo para
            // probar R8 en el emulador. Google Play rechaza esos paquetes, así que
            // no hay riesgo de subir uno por error.
            signingConfig = if (keystoreProperties.isEmpty) {
                logger.warn("Coida: no hay keystore.properties; la release se firma con la clave de depuración y no vale para Google Play.")
                signingConfigs.getByName("debug")
            } else {
                signingConfigs.getByName("release")
            }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        // La que se manda a mano a la familia, fuera de Google Play (D-66): la release
        // tal cual (sin asistente, con R8 y la misma firma) más la bola roja de
        // llamadas perdidas, cuyo permiso declara src/familia/AndroidManifest.xml.
        // ./gradlew assembleFamilia
        create("familia") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            buildConfigField("boolean", "MISSED_CALLS_ENABLED", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        // Explícito para que no dependa del JDK con el que se compile.
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // El BOM alinea todas las versiones de Compose entre sí.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
}
