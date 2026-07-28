plugins {
    alias(buildLogic.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "com.wsr.k.a.row.shared.ui"
    }

    sourceSets {
        getByName("commonMain")  {
            dependencies {
                implementation(projects.shared.lib)

                implementation(libs.kotlin.coroutine)
                implementation(libs.kotlin.datetime)

                implementation(libs.bundles.compose)
            }
        }
    }
}
