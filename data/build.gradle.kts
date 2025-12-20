plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.hilt)
    kotlin("kapt")
}

val serializationVersion = libs.versions.serialization.get()

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

        javaCompileOptions {
            annotationProcessorOptions {
                argument(
                    "room.schemaLocation",
                    "$projectDir/schemas",
                )
                argument("room.incremental", "true")
                argument("room.expandProjection", "true")
            }
        }
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

kapt {
    val sqliteTmpDir = layout.buildDirectory.dir("tmp/sqlite").get().asFile
    sqliteTmpDir.mkdirs()
    javacOptions {
        option("-J-Dorg.sqlite.tmpdir=${sqliteTmpDir.absolutePath}")
        option("-J-Djava.io.tmpdir=${sqliteTmpDir.absolutePath}")
    }
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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    implementation(libs.androidx.security.crypto)
    kapt(libs.hilt.compiler)
    kapt(libs.androidx.room.compiler)
    kapt(libs.kotlinx.serialization.json)
    kapt("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
    kapt("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    detektPlugins(libs.detekt.formatting)
}

configurations.kapt {
    resolutionStrategy.force(
        "org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion",
    )
}
