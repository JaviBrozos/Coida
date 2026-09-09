// Build script raíz: solo declara los plugins que usarán los módulos.
// El único módulo real es :app (ver settings.gradle.kts).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
