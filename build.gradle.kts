import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.tasks.Delete
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.playPublisher) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<KtlintExtension> {
        android.set(true)
        filter {
            exclude("**/generated/**")
        }
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(files(rootProject.file("config/detekt/detekt.yml")))
        autoCorrect = true
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
