plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.android)
    implementation(libs.gradle.roborazzi)
}

gradlePlugin {
    plugins {
        register("androidComposeApplication") {
            id = "com.wsr.k.a.row.compose.application"
            implementationClass = "plugins.AndroidComposeApplicationPlugin"
        }
        register("androidComposeLibrary") {
            id = "com.wsr.k.a.row.compose.library"
            implementationClass = "plugins.AndroidComposeLibraryPlugin"
        }
        register("composeMultiPlatform") {
            id = "com.wsr.k.a.row.compose.multiplatform"
            implementationClass = "plugins.ComposeMultiPlatformPlugin"
        }
        register("kotlinMultiPlatform") {
            id = "com.wsr.k.a.row.kotlin.multiplatform"
            implementationClass = "plugins.KotlinMultiPlatformPlugin"
        }
    }
}
