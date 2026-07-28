plugins {
    alias(buildLogic.plugins.kotlin.multiplatform)
}

kotlin {
    android {
        namespace = "com.wsr.k.a.row.shared.model"
    }

    sourceSets {
        getByName("commonMain")  {
            dependencies {
                implementation(libs.kotlin.coroutine)
                implementation(libs.kotlin.datetime)

                implementation(projects.shared.domain)
            }
        }
    }
}
