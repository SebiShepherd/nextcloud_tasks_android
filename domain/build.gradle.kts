plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.coroutines.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.coroutines.test)
    detektPlugins(libs.detekt.formatting)
}

kotlin {
    jvmToolchain(17)
}
