package plugins

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import plugins.ext.alias
import plugins.ext.buildLogic
import plugins.ext.getPlugin
import plugins.ext.getVersion
import plugins.ext.kotlinMultiplatform
import plugins.ext.libs

class ComposeMultiPlatformPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                alias(libs.getPlugin("kotlin"))
                alias(libs.getPlugin("android.multiplatform.library"))
                alias(libs.getPlugin("compose.compiler"))

                alias(libs.getPlugin("ktlint"))
            }

            kotlinMultiplatform {
                applyDefaultHierarchyTemplate()

                targets
                    .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                    .configureEach {
                        compileSdk = buildLogic.getVersion("android.compileSdk").toInt()
                        minSdk = buildLogic.getVersion("android.minSdk").toInt()
                    }

                // iOSビルド用
//                iosX64()
//                iosArm64()
//                iosSimulatorArm64()
            }
        }
    }
}