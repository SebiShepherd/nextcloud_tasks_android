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

        // OAuth client credentials - override these in gradle.properties or via environment variables
        buildConfigField(
            "String",
            "OAUTH_CLIENT_ID",
            "\"${project.findProperty("OAUTH_CLIENT_ID") ?: System.getenv("OAUTH_CLIENT_ID") ?: "nextcloud-tasks-android"}\""
        )
        buildConfigField(
            "String",
            "OAUTH_CLIENT_SECRET",
            "\"${project.findProperty("OAUTH_CLIENT_SECRET") ?: System.getenv("OAUTH_CLIENT_SECRET") ?: "local-client-secret"}\""
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

dependencies {
    implementation(project(":domain"))
    implementation(libs.coroutines.android)
    implementation(libs.hilt.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    implementation(libs.timber)
    implementation(libs.androidx.security.crypto)
    kapt(libs.hilt.compiler)
    kapt(libs.moshi.codegen)
    detektPlugins(libs.detekt.formatting)
}
