plugins {
    alias(buildLogic.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.wsr.k.a.row.shared.model"
        withHostTestBuilder { sourceSetTreeName = "test" }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.domain)

                implementation(libs.kotlin.coroutine)
                implementation(libs.kotlin.datetime)
                implementation(libs.kotlinx.serialization.core)

                implementation(libs.knist)
                implementation(libs.okio)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
