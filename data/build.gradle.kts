import org.jetbrains.kotlin.gradle.tasks.KaptTask

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.hilt)
    kotlin("kapt")
}

android {
    namespace = "com.nextcloud.tasks.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        buildConfigField(
            "String",
            "DEFAULT_NEXTCLOUD_BASE_URL",
            "\"https://nextcloud.example.com/\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kapt {
    correctErrorTypes = true
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<KaptTask>().configureEach {
    val sqliteTmpDir = layout.buildDirectory.dir("tmp/sqlite").get().asFile
    sqliteTmpDir.mkdirs()
    jvmArgs("-Dorg.sqlite.tmpdir=${sqliteTmpDir.absolutePath}")
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.coroutines.android)
    implementation(libs.hilt.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.timber)
    implementation(libs.androidx.security.crypto)
    kapt(libs.hilt.compiler)
    kapt(libs.androidx.room.compiler)
    detektPlugins(libs.detekt.formatting)
}
