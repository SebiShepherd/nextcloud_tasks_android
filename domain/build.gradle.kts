plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.coroutines.core)
    testImplementation(kotlin("test"))
    detektPlugins(libs.detekt.formatting)
}

kotlin {
    jvmToolchain(17)
}
