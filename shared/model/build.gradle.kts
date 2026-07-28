plugins {
    alias(buildLogic.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.wsr.k.a.row.shared.model"
    }

    sourceSets {
        getByName("commonMain")  {
            dependencies {
                implementation(projects.shared.domain)

                implementation(libs.kotlin.coroutine)
                implementation(libs.kotlin.datetime)
                implementation(libs.kotlinx.serialization.core)

                implementation(libs.knist)
            }
        }
    }
}
