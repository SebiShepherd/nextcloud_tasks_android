plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.coroutines.core)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}
