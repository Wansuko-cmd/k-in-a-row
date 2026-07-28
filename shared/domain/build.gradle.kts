plugins {
    alias(buildLogic.plugins.kotlin.multiplatform)
}

kotlin {
    android {
        namespace = "com.wsr.k.a.row.shared.domain"
    }

    sourceSets {
        getByName("commonMain")  {
            dependencies {
                implementation(libs.kotlin.coroutine)
                implementation(libs.kotlin.datetime)
            }
        }
    }
}
