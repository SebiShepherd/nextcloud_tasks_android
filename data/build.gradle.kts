plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.hilt)
    kotlin("kapt")
    alias(libs.plugins.ksp)
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
    ksp(libs.androidx.room.compiler)
    detektPlugins(libs.detekt.formatting)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

val roomSchemaDir = file("$projectDir/schemas")
tasks.withType<com.google.devtools.ksp.gradle.KspTask>().configureEach {
    doFirst {
        roomSchemaDir
            .takeIf { it.exists() }
            ?.listFiles { file -> file.extension == "json" && file.length() == 0L }
            ?.forEach { it.delete() }
    }
}
