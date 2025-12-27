import java.util.Base64

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.hilt)
    alias(libs.plugins.playPublisher)
    alias(libs.plugins.composeCompiler)
    kotlin("kapt")
}

/**
 * Get version name from environment variable (set by CI) or git tag.
 * Falls back to default version if neither is available.
 */
fun getVersionName(): String {
    // First check if VERSION_NAME is set by CI
    val envVersion = System.getenv("VERSION_NAME")
    if (!envVersion.isNullOrBlank()) {
        return envVersion
    }

    // Try to get version from git tag
    try {
        val tagVersion =
            Runtime
                .getRuntime()
                .exec("git describe --tags --abbrev=0")
                .inputStream
                .bufferedReader()
                .readText()
                .trim()
                .removePrefix("v")

        if (tagVersion.isNotBlank()) {
            return tagVersion
        }
    } catch (e: Exception) {
        // Git command failed, use default
    }

    // Fallback to default version
    return "1.0.0"
}

/**
 * Get version code from environment variable or calculate from version name.
 * Version code format: MAJOR * 10000 + MINOR * 100 + PATCH
 * Example: 1.2.3 -> 10203
 */
fun getVersionCode(): Int {
    val versionName = getVersionName()

    // Parse version string (e.g., "1.2.3" or "1.2.3-beta")
    val versionParts = versionName.split("-")[0].split(".")

    return try {
        val major = versionParts.getOrNull(0)?.toIntOrNull() ?: 1
        val minor = versionParts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = versionParts.getOrNull(2)?.toIntOrNull() ?: 0

        major * 10000 + minor * 100 + patch
    } catch (e: Exception) {
        1 // Fallback version code
    }
}

android {
    namespace = "com.nextcloud.tasks"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nextcloud.tasks"
        minSdk = 26
        targetSdk = 36
        versionCode = getVersionCode()
        versionName = getVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["applicationId"] = "com.nextcloud.tasks.test"
    }

    androidResources {
        // Supported languages for resource filtering
        localeFilters += listOf("en", "de")
    }

    signingConfigs {
        create("release") {
            val keystoreBase64 = System.getenv("SIGNING_KEYSTORE_BASE64")
            if (!keystoreBase64.isNullOrBlank()) {
                val keystoreFile =
                    layout.buildDirectory
                        .file("keystore/release.jks")
                        .get()
                        .asFile
                keystoreFile.parentFile.mkdirs()
                keystoreFile.writeBytes(Base64.getDecoder().decode(keystoreBase64))
                storeFile = keystoreFile
                storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

kapt {
    correctErrorTypes = true
    useBuildCache = true
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.coroutines.android)
    implementation(libs.material)
    implementation(libs.timber)
    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    kapt(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(kotlin("test"))
    detektPlugins(libs.detekt.formatting)
}

play {
    // Only configure if service account JSON exists
    val serviceAccountPath = System.getenv("PLAY_SERVICE_ACCOUNT_JSON")
    if (!serviceAccountPath.isNullOrBlank() && file(serviceAccountPath).exists()) {
        serviceAccountCredentials.set(file(serviceAccountPath))
        track.set("internal")
        defaultToAppBundles.set(true)
        resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.AUTO)
    } else {
        // Disable Play Publisher when service account is not available
        enabled.set(false)
    }
}
