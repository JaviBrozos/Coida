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

fun aiSetting(name: String, default: String = ""): String {
    val value = localProperties.getProperty("doado.ai.$name", default).trim()
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

plugins {
    alias(libs.plugins.android.application)
    // Compilador de Compose. Kotlin en sí NO se declara aquí: desde AGP 9 viene
    // integrado en el plugin de Android (ver nota en CLAUDE.md).
    alias(libs.plugins.kotlin.compose)
}

android {
    // namespace = paquete raíz del código (com.doado.launcher, com.doado.data, ...).
    // Es distinto del applicationId a propósito: así el paquete de feature del
    // launcher puede llamarse "launcher" sin quedar como com.doado.launcher.launcher.
    namespace = "com.doado"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.doado.launcher"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 4
        versionName = "1.4"

        buildConfigField("String", "AI_BASE_URL", aiSetting("baseUrl", "https://api.groq.com/openai/v1"))
        buildConfigField("String", "AI_STT_MODEL", aiSetting("sttModel", "whisper-large-v3"))
        buildConfigField("String", "AI_LLM_MODEL", aiSetting("llmModel", "openai/gpt-oss-120b"))
        // Solo para modelos que razonan (gpt-oss): "low" contesta antes. Vacío = no se envía.
        buildConfigField("String", "AI_REASONING_EFFORT", aiSetting("reasoningEffort", "low"))
        buildConfigField("String", "AI_API_KEY", aiSetting("apiKey"))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
